package eu.arrowhead.proto.cosys.datasharing;

import eu.arrowhead.client.library.ArrowheadService;
import eu.arrowhead.client.library.util.ClientCommonConstants;
import eu.arrowhead.common.SSLProperties;
import eu.arrowhead.common.dto.shared.*;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.proto.cosys.datasharing.database.AcceptedItem;
import eu.arrowhead.proto.cosys.datasharing.dto.EmptyDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.Scanner;
import java.util.UUID;

public class DataConsumerCLI extends Thread {
    private boolean running = false;

    @Autowired
    protected SSLProperties sslProperties;

    @Autowired
    protected ArrowheadService arrowheadService;

    @Value(ClientCommonConstants.$CLIENT_SERVER_ADDRESS_WD)
    private String mySystemAddress;

    @Value(ClientCommonConstants.$CLIENT_SERVER_PORT_WD)
    private Integer mySystemPort;

    @Autowired
    private ArrayList<AcceptedItem> db;

    private final Logger logger = LogManager.getLogger(DataConsumerCLI.class);

    @Override
    public void run() {
        Scanner in = new Scanner(System.in);
        String text = "";

        while(!text.equalsIgnoreCase("exit")) {
            logger.info("Enter command: ");

            text = in.nextLine();

            String[] split = text.split(" ");

            try {
                if (split.length == 3 && split[0].equalsIgnoreCase("1")) {
                    requestData(split[1], Integer.parseInt(split[2]));
                } else if (split.length == 2 && split[0].equalsIgnoreCase("2")) {
                    logger.info(getData(split[1]));
                } else if (split[0].equalsIgnoreCase("exit")) {
                    logger.info("exiting..");
                } else {
                    logger.warn("enter valid string");
                }
            } catch(Exception e) {
                logger.warn("something went wrong");
                logger.warn(e.toString());
            }

        }
    }

    public void requestData(String dataHash, Integer offerAmount) {

        String randomIdentifier = getRandomIdentifier();

        OrchestrationResultDTO orchestrationResult = getService("offer-contract");
        validateOrchestrationResult(orchestrationResult, "offer-contract");

        final String token = orchestrationResult.getAuthorizationTokens() == null ? null : orchestrationResult.getAuthorizationTokens().get(getInterface());

        arrowheadService.consumeServiceHTTP(EmptyDTO.class,
                HttpMethod.valueOf(orchestrationResult.getMetadata().get(DataConsumerConstants.HTTP_METHOD)),
                orchestrationResult.getProvider().getAddress(),
                orchestrationResult.getProvider().getPort(),
                orchestrationResult.getServiceUri(),
                getInterface(),
                token,
                "",
                "ident-hash", randomIdentifier,
                "offer-amount", Integer.toString(offerAmount),
                "data-hash", dataHash,
                "request-address", mySystemAddress,
                "request-port", Integer.toString(mySystemPort)
        );
    }

   public String getData(String identifyingHash) {
        for (int i = 0; i < db.size(); i++) {
            if (db.get(i).getIdentifyingHash().equals(identifyingHash)) {
                logger.info("Trying to send a request to: " + db.get(i).getProducerAddress() + ":" + db.get(i).getProducerPort());

                OrchestrationResultDTO orchestrationResult = getService(db.get(i).getServiceUri());

                final String token = orchestrationResult.getAuthorizationTokens() == null ? null : orchestrationResult.getAuthorizationTokens().get(getInterface());

                JSONObject a = arrowheadService.consumeServiceHTTP(JSONObject.class,
                        HttpMethod.POST,
                        db.get(i).getProducerAddress(),
                        Integer.parseInt(db.get(i).getProducerPort()),
                        "/get-data",
                        getInterface(),
                        token,
                        "",
                        "identifying-hash", identifyingHash);

                if (a.containsKey("data") && a.containsKey("status")) {
                    if (a.get("status").equals("ERROR")) {
                        return "The hash: " + identifyingHash + " was not accepted";
                    } else {
                        return "The data requested: " + a.get("data").toString();
                    }
                }
            }
        }
        return "The hash: " + identifyingHash + " is not used";
    }

    // HELPERS
    public String getRandomIdentifier() {
        return UUID.randomUUID().toString().replace("-", "");
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
                .flag(OrchestrationFlags.Flag.ENABLE_INTER_CLOUD, true)
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

    private String getInterface() {
        return sslProperties.isSslEnabled() ? DataConsumerConstants.INTERFACE_SECURE : DataConsumerConstants.INTERFACE_INSECURE;
    }

}
