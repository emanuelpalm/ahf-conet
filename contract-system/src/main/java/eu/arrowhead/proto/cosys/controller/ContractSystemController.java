package eu.arrowhead.proto.cosys.controller;

import eu.arrowhead.client.library.ArrowheadService;
import eu.arrowhead.common.SSLProperties;
import eu.arrowhead.common.dto.shared.*;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.proto.cosys.ContractSystemListener;
import eu.arrowhead.proto.cosys.ContractSystemConstants;
import eu.arrowhead.proto.cosys.database.DbItem;
import eu.arrowhead.proto.cosys.dto.EmptyDTO;
import eu.arrowhead.proto.cosys.publisher.event.PresetEventType;
import eu.arrowhead.proto.cosys.publisher.service.PublisherService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;

@RestController
public class ContractSystemController {

    private final Logger logger = LogManager.getLogger(ContractSystemListener.class);

    @Autowired
    protected SSLProperties sslProperties;

    @Autowired
    protected ArrowheadService arrowheadService;

    @Autowired
    private PublisherService publisherService;

    @Autowired
    private ArrayList<DbItem> db;

    // TODO: add information about the requesting system to be able to relay the answer
    @PostMapping(path = ContractSystemConstants.OFFER_URI, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody public void offerEndpoint(@RequestParam(name = ContractSystemConstants.OFFER_AMOUNT) Integer offerAmount,
                                            @RequestParam(name = ContractSystemConstants.IDENTIFYING_HASH) String randomHash,
                                            @RequestParam(name = ContractSystemConstants.DATA_HASH) String hash,
                                            @RequestParam(name = ContractSystemConstants.REQUEST_ADDRESS) String requestAddress,
                                            @RequestParam(name = ContractSystemConstants.REQUEST_PORT) String requestPort)
    {

        logger.info("Received a offer POST request");
        // save it in a db (and who made the offer)
        db.add(new DbItem(randomHash, offerAmount, requestAddress, requestPort));
        // publish the offer through the eventhandler

        // arguments: eventType, metadata, payload
        HashMap<String, String> tempMap = new HashMap<String, String>();

        tempMap.put("randomHash", randomHash);
        tempMap.put("offer", Integer.toString(offerAmount));
        tempMap.put("hash", hash);

        // payload hardcoded should probably be something
        publisherService.publish(PresetEventType.REQUEST_RECEIVED, tempMap, "1");
    }

    @PostMapping(path = ContractSystemConstants.REJECT_URI, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody public void rejectEndpoint(@RequestParam(name = ContractSystemConstants.RANDOM_HASH) String randomHash) {
        logger.info("Received a reject POST request");
        // find the request
        DbItem tempItem = getItem(randomHash);

        if (tempItem != null) {
            // relay this to the requesting system
            relayRejectRequest(tempItem);
        }
    }

    @PostMapping(path = ContractSystemConstants.ACCEPT_URI, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody public void acceptEndpoint(@RequestParam(name = ContractSystemConstants.RANDOM_HASH) String randomHash,
                                             @RequestParam(name = ContractSystemConstants.PRODUCER_NAME) String producerName,
                                             @RequestParam(name = ContractSystemConstants.PRODUCER_ADDRESS) String producerAddress,
                                             @RequestParam(name = ContractSystemConstants.PRODUCER_PORT) String producerPort,
                                             @RequestParam(name = ContractSystemConstants.SERVICE_URI) String serviceUri) {
        // find the request
        logger.info("Received a accept POST request");
        DbItem tempItem = getItem(randomHash);

        if (tempItem != null) {
            // relay this to the requesting system
            relayAcceptRequest(tempItem, producerName, producerAddress, producerPort, serviceUri);
        }
    }

    @GetMapping(path = "/echo")
    @ResponseBody public String echo() {
        return "echo";
    }

    public DbItem getItem(String randomHash) {
        for (int i = 0; i < db.size(); i++) {
            if (db.get(i).getRandomHash().equals(randomHash)) {
                return db.get(i);
            }
        }
        return null;
    }

    public void relayRejectRequest(DbItem dbItem) {

        OrchestrationResultDTO orchestrationResult = getService("reject-relay");
        validateOrchestrationResult(orchestrationResult, "reject-relay");

        final String token = orchestrationResult.getAuthorizationTokens() == null ? null : orchestrationResult.getAuthorizationTokens().get(getInterface());

        arrowheadService.consumeServiceHTTP(EmptyDTO.class,
                HttpMethod.POST,
                dbItem.getRequestAddress(),
                Integer.parseInt(dbItem.getRequestPort()),
                orchestrationResult.getServiceUri(),
                getInterface(),
                token,
                "",
                "ident-hash", dbItem.getRandomHash()
                );
    }

    public void relayAcceptRequest(DbItem dbItem, String producerName, String producerAddress, String producerPort, String serviceUri) {

        OrchestrationResultDTO orchestrationResult = getService("accept-relay");
        validateOrchestrationResult(orchestrationResult, "accept-relay");

        final String token = orchestrationResult.getAuthorizationTokens() == null ? null : orchestrationResult.getAuthorizationTokens().get(getInterface());

        arrowheadService.consumeServiceHTTP(EmptyDTO.class,
                HttpMethod.valueOf(orchestrationResult.getMetadata().get(ContractSystemConstants.HTTP_METHOD)),
                dbItem.getRequestAddress(),
                Integer.parseInt(dbItem.getRequestPort()),
                orchestrationResult.getServiceUri(),
                getInterface(),
                token, //
                "",
                "ident-hash", dbItem.getRandomHash(),
                "producer-name", producerName,
                "producer-address", producerAddress,
                "producer-port", producerPort,
                "service-uri", serviceUri
        );
    }

    private String getInterface() {
        return sslProperties.isSslEnabled() ? ContractSystemConstants.INTERFACE_SECURE : ContractSystemConstants.INTERFACE_INSECURE;
    }

    private void validateOrchestrationResult(final OrchestrationResultDTO orchestrationResult, final String serviceDefinition) {
        if (!orchestrationResult.getService().getServiceDefinition().equalsIgnoreCase(serviceDefinition)) {
            throw new InvalidParameterException("Requested and orchestrated service definition do not match");
        }

        boolean hasValidInterface = false;
        for (final ServiceInterfaceResponseDTO serviceInterface : orchestrationResult.getInterfaces()) {
            if (serviceInterface.getInterfaceName().equalsIgnoreCase(getInterface())) {
                hasValidInterface = true;
                break;
            }
        }
        if (!hasValidInterface) {
            throw new InvalidParameterException("Requested and orchestrated interface do not match");
        }
    }

    public OrchestrationResultDTO getService(String serviceDefinition) {

        // find the endpoint
        final ServiceQueryFormDTO serviceQueryForm = new ServiceQueryFormDTO.Builder(serviceDefinition)
                .interfaces(getInterface())
                .build();

        final OrchestrationFormRequestDTO.Builder orchestrationFormBuilder = arrowheadService.getOrchestrationFormBuilder();
        final OrchestrationFormRequestDTO orchestrationFormRequest = orchestrationFormBuilder.requestedService(serviceQueryForm)
                .flag(OrchestrationFlags.Flag.MATCHMAKING, true)
                .flag(OrchestrationFlags.Flag.OVERRIDE_STORE, true)
                .build();

        final OrchestrationResponseDTO orchestrationResponse = arrowheadService.proceedOrchestration(orchestrationFormRequest);

        if (orchestrationResponse == null) {
            logger.info("No orchestration response received");
            // TODO: throw error
        } else if (orchestrationResponse.getResponse().isEmpty()) {
            logger.info("No provider found during the orchestration");
            // TODO: throw error
        }

        return orchestrationResponse.getResponse().get(0);
    }

}
