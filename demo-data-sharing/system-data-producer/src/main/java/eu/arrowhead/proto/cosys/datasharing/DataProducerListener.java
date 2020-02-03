package eu.arrowhead.proto.cosys.datasharing;

import eu.arrowhead.client.library.ArrowheadService;
import eu.arrowhead.client.library.config.ApplicationInitListener;
import eu.arrowhead.client.library.util.ClientCommonConstants;
import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.core.CoreSystem;
import eu.arrowhead.common.dto.shared.EventDTO;
import eu.arrowhead.common.dto.shared.ServiceRegistryRequestDTO;
import eu.arrowhead.common.dto.shared.ServiceSecurityType;
import eu.arrowhead.common.dto.shared.SystemRequestDTO;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.proto.cosys.datasharing.controller.DataProducerSubThread;
import eu.arrowhead.proto.cosys.datasharing.database.InMemoryDb;
import eu.arrowhead.proto.cosys.datasharing.security.SubscriberSecurityConfig;
import eu.arrowhead.proto.cosys.datasharing.utils.ConfigEventProperties;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;


@Component
public class DataProducerListener extends ApplicationInitListener {

    @Autowired
    private ArrowheadService arrowheadService;

    @Autowired
    private SubscriberSecurityConfig subscriberSecurityConfig;

    @Value(ClientCommonConstants.$TOKEN_SECURITY_FILTER_ENABLED_WD)
    private boolean tokenSecurityFilterEnabled;

    @Value(CommonConstants.$SERVER_SSL_ENABLED_WD)
    private boolean sslEnabled;

    @Value(ClientCommonConstants.$CLIENT_SYSTEM_NAME)
    private String mySystemName;

    @Value(ClientCommonConstants.$CLIENT_SERVER_ADDRESS_WD)
    private String mySystemAddress;

    @Value(ClientCommonConstants.$CLIENT_SERVER_PORT_WD)
    private Integer mySystemPort;

    @Autowired
    private ConfigEventProperties configEventProperties;

    @Autowired
    private ApplicationContext applicationContext;

    private final Logger logger = LogManager.getLogger(DataProducerListener.class);

    @Bean( DataProducerConstants.NOTIFICATION_QUEUE )
    public ConcurrentLinkedQueue<EventDTO> getNotificationQueue() {
        return new ConcurrentLinkedQueue<>();
    }

    @Bean( DataProducerConstants.IN_MEMORY_DB )
    public InMemoryDb getInMemoryDb() {
        return new InMemoryDb();
    }

    @Bean ( DataProducerConstants.SUB_TASK )
    public DataProducerSubThread getProducerThread() {
        return new DataProducerSubThread();
    }

    @Override
    protected void customInit(final ContextRefreshedEvent event) {
        //Configurator.setRootLevel(Level.DEBUG);

        checkCoreSystemReachability(CoreSystem.SERVICE_REGISTRY);

        if (sslEnabled && tokenSecurityFilterEnabled) {
            checkCoreSystemReachability(CoreSystem.AUTHORIZATION);
            arrowheadService.updateCoreServiceURIs(CoreSystem.AUTHORIZATION);
            setTokenSecurityFilter();

            setNotificationFilter();
        }

        if (arrowheadService.echoCoreSystem(CoreSystem.EVENT_HANDLER)) {
            arrowheadService.updateCoreServiceURIs(CoreSystem.EVENT_HANDLER);
            subscribeToPresetEvents();
        }

        // Orchestrator setup
        checkCoreSystemReachability(CoreSystem.ORCHESTRATOR);
        arrowheadService.updateCoreServiceURIs(CoreSystem.ORCHESTRATOR);

        // Create the subtask
        final DataProducerSubThread subtask = applicationContext.getBean(DataProducerConstants.SUB_TASK, DataProducerSubThread.class);
        applicationContext.getAutowireCapableBeanFactory().autowireBean(subtask);
        subtask.start();

        // register direct connection by consumer to get the data
        final ServiceRegistryRequestDTO getDataRequest = createServiceRegistryRequest(DataProducerConstants.GET_DATA_NAME, DataProducerConstants.GET_DATA_URI, HttpMethod.POST);
        arrowheadService.forceRegisterServiceToServiceRegistry(getDataRequest);

   }

    private void setTokenSecurityFilter() {
        if(!tokenSecurityFilterEnabled || !sslEnabled) {
            logger.info("TokenSecurityFilter in not active");
        } else {
            final PublicKey authorizationPublicKey = arrowheadService.queryAuthorizationPublicKey();
            if (authorizationPublicKey == null) {
                throw new ArrowheadException("Authorization public key is null");
            }

            KeyStore keystore;
            try {
                keystore = KeyStore.getInstance(sslProperties.getKeyStoreType());
                keystore.load(sslProperties.getKeyStore().getInputStream(), sslProperties.getKeyStorePassword().toCharArray());
            } catch ( final KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException ex) {
                throw new ArrowheadException(ex.getMessage());
            }
            final PrivateKey subscriberPrivateKey = Utilities.getPrivateKey(keystore, sslProperties.getKeyPassword());

            final Map<String, String> eventTypeMap = configEventProperties.getEventTypeURIMap();

            subscriberSecurityConfig.getTokenSecurityFilter().setEventTypeMap(eventTypeMap);
            subscriberSecurityConfig.getTokenSecurityFilter().setAuthorizationPublicKey(authorizationPublicKey);
            subscriberSecurityConfig.getTokenSecurityFilter().setMyPrivateKey(subscriberPrivateKey);
        }
    }

   private ServiceRegistryRequestDTO createServiceRegistryRequest(final String serviceDefinition, final String serviceUri, HttpMethod httpMethod) {
        final ServiceRegistryRequestDTO serviceRegistryRequest = new ServiceRegistryRequestDTO();
        serviceRegistryRequest.setServiceDefinition(serviceDefinition);

        final SystemRequestDTO systemRequest = new SystemRequestDTO();
        systemRequest.setSystemName(mySystemName);
        systemRequest.setAddress(mySystemAddress);
        systemRequest.setPort(mySystemPort);

        if (tokenSecurityFilterEnabled) {
            systemRequest.setAuthenticationInfo(Base64.getEncoder().encodeToString(arrowheadService.getMyPublicKey().getEncoded()));
            serviceRegistryRequest.setSecure(ServiceSecurityType.TOKEN);
            serviceRegistryRequest.setInterfaces(List.of(DataProducerConstants.INTERFACE_SECURE));
        } else if (sslEnabled) {
            systemRequest.setAuthenticationInfo(Base64.getEncoder().encodeToString(arrowheadService.getMyPublicKey().getEncoded()));
            serviceRegistryRequest.setSecure(ServiceSecurityType.CERTIFICATE);
            serviceRegistryRequest.setInterfaces(List.of(DataProducerConstants.INTERFACE_SECURE));
        } else {
            serviceRegistryRequest.setSecure(ServiceSecurityType.NOT_SECURE);
            serviceRegistryRequest.setInterfaces(List.of(DataProducerConstants.INTERFACE_INSECURE));
        }

        serviceRegistryRequest.setProviderSystem(systemRequest);
        serviceRegistryRequest.setServiceUri(serviceUri);
        serviceRegistryRequest.setMetadata(new HashMap<>());
        serviceRegistryRequest.getMetadata().put(DataProducerConstants.HTTP_METHOD, httpMethod.name());
        return serviceRegistryRequest;

   }

    private void setNotificationFilter() {
        logger.info( "setNotificationFilter started..." );

        final Map<String, String> eventTypeMap = configEventProperties.getEventTypeURIMap();

        subscriberSecurityConfig.getNotificationFilter().setEventTypeMap(eventTypeMap);
        subscriberSecurityConfig.getNotificationFilter().setServerCN(arrowheadService.getServerCN());

    }

    private void subscribeToPresetEvents() {

        final Map<String, String> eventTypeMap = configEventProperties.getEventTypeURIMap();

        if( eventTypeMap == null) {

            logger.info("No preset events to subscribe.");

        } else {

            final SystemRequestDTO subscriber = new SystemRequestDTO();
            subscriber.setSystemName( mySystemName);
            subscriber.setAddress(mySystemAddress);
            subscriber.setPort(mySystemPort);

            if (sslEnabled) {

                subscriber.setAuthenticationInfo( Base64.getEncoder().encodeToString( arrowheadService.getMyPublicKey().getEncoded()) );

            }
            for (final String eventType  : eventTypeMap.keySet()) {

                try {

                    arrowheadService.unsubscribeFromEventHandler(eventType, mySystemName, mySystemAddress, mySystemPort);

                } catch (final Exception ex) {

                    logger.debug("Exception happend in subscription initalization " + ex);
                }

                try {

                    arrowheadService.subscribeToEventHandler( SubscriberUtilities.createSubscriptionRequestDTO( eventType, subscriber, eventTypeMap.get( eventType ) ) );

                } catch ( final InvalidParameterException ex) {

                    if( ex.getMessage().contains( "Subscription violates uniqueConstraint rules" )) {

                        logger.debug("Subscription is already in DB");

                    } else {

                        logger.debug(ex.getMessage());
                        logger.debug(ex);
                    }

                } catch ( final Exception ex) {

                    logger.debug("Could not subscribe to EventType: " + eventType );
                }
            }

        }
    }
}