package eu.arrowhead.proto.cosys.publisher.service;

import eu.arrowhead.client.library.ArrowheadService;
import eu.arrowhead.client.library.util.ClientCommonConstants;
import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.dto.shared.EventPublishRequestDTO;
import eu.arrowhead.common.dto.shared.SystemRequestDTO;
import eu.arrowhead.proto.cosys.ContractSystemListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import eu.arrowhead.proto.cosys.publisher.event.PresetEventType;

import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.HashMap;

@Service
public class PublisherService {

    @Value(ClientCommonConstants.$CLIENT_SYSTEM_NAME)
    private String mySystemName;

    @Value(ClientCommonConstants.$CLIENT_SERVER_ADDRESS_WD)
    private String mySystemAddress;

    @Value(ClientCommonConstants.$CLIENT_SERVER_PORT_WD)
    private Integer mySystemPort;

    @Value(CommonConstants.$SERVER_SSL_ENABLED_WD)
    private boolean sslEnabled;

    @Autowired
    private ArrowheadService arrowheadService;

    private final Logger logger = LogManager.getLogger(ContractSystemListener.class);

    // https://github.com/arrowhead-f/sos-examples-spring/blob/master/demo-car-with-events/demo-car-provider-with-publishing/src/main/java/eu/arrowhead/client/skeleton/publisher/service/PublisherService.java
    public void publish(final PresetEventType eventType, final HashMap<String, String> metadata,final String payload) {
        logger.info("Publishing event with type: " + eventType.toString());
        final EventPublishRequestDTO request = getPublishRequest(eventType, metadata, payload);
        arrowheadService.publishToEventHandler(request);
    }

    private EventPublishRequestDTO getPublishRequest(final PresetEventType eventType, final HashMap<String, String> metadata, final String payload) {
        final String timeStamp = Utilities.convertZonedDateTimeToUTCString(ZonedDateTime.now());

        final EventPublishRequestDTO publishRequestDTO = new EventPublishRequestDTO(eventType.getEventTypeName(), getSource(), metadata, payload, timeStamp);

        return publishRequestDTO;
    }

    private SystemRequestDTO getSource() {
        final SystemRequestDTO source = new SystemRequestDTO();
        source.setSystemName(mySystemName);
        source.setAddress(mySystemAddress);
        source.setPort(mySystemPort);

        if (sslEnabled) {
            source.setAuthenticationInfo(Base64.getEncoder().encodeToString(arrowheadService.getMyPublicKey().getEncoded()));
        }

        return source;
    }
}
