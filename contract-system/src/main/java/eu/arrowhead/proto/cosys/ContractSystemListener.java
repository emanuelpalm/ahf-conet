package eu.arrowhead.proto.cosys;

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
import eu.arrowhead.proto.cosys.database.DbItem;
import eu.arrowhead.proto.cosys.security.ContractSecurityConfig;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
public class ContractSystemListener extends ApplicationInitListener {

    @Autowired
    private ArrowheadService arrowheadService;

    @Autowired
    private ContractSecurityConfig contractSecurityConfig;

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

    @Bean(ContractSystemConstants.OFFER_LIST)
    public ArrayList<DbItem> getOfferList() {
        return new ArrayList<>();
    }

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

        // Register all the services
        // Offer
        final ServiceRegistryRequestDTO offerServiceRequest = createServiceRegistryRequest(ContractSystemConstants.OFFER_NAME, ContractSystemConstants.OFFER_URI, HttpMethod.POST);
        arrowheadService.forceRegisterServiceToServiceRegistry(offerServiceRequest);

        // Reject
        final ServiceRegistryRequestDTO rejectServiceRequest = createServiceRegistryRequest(ContractSystemConstants.REJECT_NAME, ContractSystemConstants.REJECT_URI, HttpMethod.POST);
        arrowheadService.forceRegisterServiceToServiceRegistry(rejectServiceRequest);

        // Accept
        final ServiceRegistryRequestDTO acceptServiceRequest = createServiceRegistryRequest(ContractSystemConstants.ACCEPT_NAME, ContractSystemConstants.ACCEPT_URI, HttpMethod.POST);
        arrowheadService.forceRegisterServiceToServiceRegistry(acceptServiceRequest);

        if (arrowheadService.echoCoreSystem(CoreSystem.EVENT_HANDLER)) {
            arrowheadService.updateCoreServiceURIs(CoreSystem.EVENT_HANDLER);
        }

    }

    private void setTokenSecurityFilter() {
        final PublicKey authorizationPublicKey = arrowheadService.queryAuthorizationPublicKey();
        if (authorizationPublicKey == null) {
            throw new ArrowheadException("Authorization public key is null");
        }

        KeyStore keystore = null;
        try {
            keystore = KeyStore.getInstance(sslProperties.getKeyStoreType());
            keystore.load(sslProperties.getKeyStore().getInputStream(), sslProperties.getKeyStorePassword().toCharArray());
        } catch (KeyStoreException | IOException | CertificateException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        final PrivateKey providerPrivateKey = Utilities.getPrivateKey(keystore, sslProperties.getKeyPassword());
        contractSecurityConfig.getTokenSecurityFilter().setAuthorizationPublicKey(authorizationPublicKey);
        contractSecurityConfig.getTokenSecurityFilter().setMyPrivateKey(providerPrivateKey);
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
            serviceRegistryRequest.setInterfaces(List.of(ContractSystemConstants.INTERFACE_SECURE));
        } else if (sslEnabled) {
            systemRequest.setAuthenticationInfo(Base64.getEncoder().encodeToString(arrowheadService.getMyPublicKey().getEncoded()));
            serviceRegistryRequest.setSecure(ServiceSecurityType.CERTIFICATE);
            serviceRegistryRequest.setInterfaces(List.of(ContractSystemConstants.INTERFACE_SECURE));
        } else {
            serviceRegistryRequest.setSecure(ServiceSecurityType.NOT_SECURE);
            serviceRegistryRequest.setInterfaces(List.of(ContractSystemConstants.INTERFACE_INSECURE));
        }
        serviceRegistryRequest.setProviderSystem(systemRequest);
        serviceRegistryRequest.setServiceUri(serviceUri);
        serviceRegistryRequest.setMetadata(new HashMap<>());
        serviceRegistryRequest.getMetadata().put(ContractSystemConstants.HTTP_METHOD, httpMethod.name());
        return serviceRegistryRequest;
    }
}
