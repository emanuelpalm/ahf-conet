package eu.arrowhead.proto.cosys.datasharing;

import eu.arrowhead.client.library.ArrowheadService;
import eu.arrowhead.client.library.config.ApplicationInitListener;
import eu.arrowhead.client.library.util.ClientCommonConstants;
import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.core.CoreSystem;
import eu.arrowhead.common.dto.shared.ServiceRegistryRequestDTO;
import eu.arrowhead.common.dto.shared.ServiceSecurityType;
import eu.arrowhead.common.dto.shared.SystemRequestDTO;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.proto.cosys.datasharing.controller.DataConsumerController;
import eu.arrowhead.proto.cosys.datasharing.database.AcceptedItem;
import eu.arrowhead.proto.cosys.datasharing.security.ConsumerSecurityConfig;
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
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;

@Component
public class DataConsumerListener extends ApplicationInitListener {

    @Autowired
    protected ArrowheadService arrowheadService;

    @Autowired
    private ConsumerSecurityConfig consumerSecurityConfig;

    @Autowired
    private ApplicationContext applicationContext;

    @Value(ClientCommonConstants.$CLIENT_SYSTEM_NAME)
    private String mySystemName;

    @Value(ClientCommonConstants.$CLIENT_SERVER_ADDRESS_WD)
    private String mySystemAddress;

    @Value(ClientCommonConstants.$CLIENT_SERVER_PORT_WD)
    private Integer mySystemPort;

    @Value(ClientCommonConstants.$TOKEN_SECURITY_FILTER_ENABLED_WD)
    private boolean tokenSecurityFilterEnabled;

    @Value(CommonConstants.$SERVER_SSL_ENABLED_WD)
    private boolean sslEnabled;

    @Bean(DataConsumerConstants.ACCEPTED_ITEM_LIST)
    public ArrayList<AcceptedItem> getAcceptedItems() {
        return new ArrayList<>();
    }

    @Bean(DataConsumerConstants.SUB_TASK)
    public DataConsumerCLI getCLI() {
        return new DataConsumerCLI();
    }

    private final Logger logger = LogManager.getLogger(DataConsumerController.class);

    @Override
    protected void customInit(final ContextRefreshedEvent event) {
        //Configurator.setRootLevel(Level.DEBUG);

        checkCoreSystemReachability(CoreSystem.SERVICE_REGISTRY);

        if (sslEnabled && tokenSecurityFilterEnabled) {
            checkCoreSystemReachability(CoreSystem.AUTHORIZATION);

            arrowheadService.updateCoreServiceURIs(CoreSystem.AUTHORIZATION);

            setTokenSecurityFilter();
        }

        checkCoreSystemReachability(CoreSystem.ORCHESTRATOR);
        arrowheadService.updateCoreServiceURIs(CoreSystem.ORCHESTRATOR);

        // register reject and accept relay services.
        final ServiceRegistryRequestDTO rejectRelayRequest = createServiceRegistryRequest(DataConsumerConstants.REJECT_NAME, DataConsumerConstants.REJECT_URI, HttpMethod.POST);
        arrowheadService.forceRegisterServiceToServiceRegistry(rejectRelayRequest);

        final ServiceRegistryRequestDTO acceptRelayRequest = createServiceRegistryRequest(DataConsumerConstants.ACCEPT_NAME, DataConsumerConstants.ACCEPT_URI, HttpMethod.POST);
        arrowheadService.forceRegisterServiceToServiceRegistry(acceptRelayRequest);

        final DataConsumerCLI cliThread = applicationContext.getBean(DataConsumerConstants.SUB_TASK, DataConsumerCLI.class);
        applicationContext.getAutowireCapableBeanFactory().autowireBean(cliThread);
        cliThread.start();
    }

    // helpers

    private void setTokenSecurityFilter() {
        final PublicKey authorizationPublicKey = arrowheadService.queryAuthorizationPublicKey();
        if (authorizationPublicKey == null) {
            throw new ArrowheadException("Authorization public key is null");
        }

        KeyStore keystore = null;
        try {
            keystore = KeyStore.getInstance(sslProperties.getKeyStoreType());
            keystore.load(sslProperties.getKeyStore().getInputStream(), sslProperties.getKeyStorePassword().toCharArray());
        } catch (IOException | CertificateException | NoSuchAlgorithmException | KeyStoreException e) {
            e.printStackTrace();
        }

        final PrivateKey consumerPrivateKey = Utilities.getPrivateKey(keystore, sslProperties.getKeyPassword());
        consumerSecurityConfig.getTokenSecurityFilter().setAuthorizationPublicKey(authorizationPublicKey);
        consumerSecurityConfig.getTokenSecurityFilter().setMyPrivateKey(consumerPrivateKey);
    }

    private ServiceRegistryRequestDTO createServiceRegistryRequest(final String serviceDefinition, final String serviceUri, final HttpMethod httpMethod) {
        final ServiceRegistryRequestDTO serviceRegistryRequest = new ServiceRegistryRequestDTO();
        serviceRegistryRequest.setServiceDefinition(serviceDefinition);
        final SystemRequestDTO systemRequest = new SystemRequestDTO();
        systemRequest.setSystemName(mySystemName);
        systemRequest.setAddress(mySystemAddress);
        systemRequest.setPort(mySystemPort);

        if (tokenSecurityFilterEnabled) {
            systemRequest.setAuthenticationInfo(Base64.getEncoder().encodeToString(arrowheadService.getMyPublicKey().getEncoded()));
            serviceRegistryRequest.setSecure(ServiceSecurityType.TOKEN);
            serviceRegistryRequest.setInterfaces(List.of(DataConsumerConstants.INTERFACE_SECURE));
        } else if (sslEnabled) {
            systemRequest.setAuthenticationInfo(Base64.getEncoder().encodeToString(arrowheadService.getMyPublicKey().getEncoded()));
            serviceRegistryRequest.setSecure(ServiceSecurityType.CERTIFICATE);
            serviceRegistryRequest.setInterfaces(List.of(DataConsumerConstants.INTERFACE_SECURE));
        } else {
            serviceRegistryRequest.setSecure(ServiceSecurityType.NOT_SECURE);
            serviceRegistryRequest.setInterfaces(List.of(DataConsumerConstants.INTERFACE_INSECURE));
        }
        serviceRegistryRequest.setProviderSystem(systemRequest);
        serviceRegistryRequest.setServiceUri(serviceUri);
        serviceRegistryRequest.setMetadata(new HashMap<>());
        serviceRegistryRequest.getMetadata().put(DataConsumerConstants.HTTP_METHOD, httpMethod.name());
        return serviceRegistryRequest;
    }

}
