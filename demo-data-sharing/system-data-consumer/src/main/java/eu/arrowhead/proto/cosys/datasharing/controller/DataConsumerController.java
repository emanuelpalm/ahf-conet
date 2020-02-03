package eu.arrowhead.proto.cosys.datasharing.controller;

import eu.arrowhead.proto.cosys.datasharing.DataConsumerConstants;
import eu.arrowhead.proto.cosys.datasharing.database.AcceptedItem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;

@RestController
public class DataConsumerController {
    @Autowired
    private ArrayList<AcceptedItem> db;

    private final Logger logger = LogManager.getLogger(DataConsumerController.class);

    @PostMapping(path = DataConsumerConstants.REJECT_URI, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody public void rejectEndpoint(@RequestParam(name = DataConsumerConstants.IDENTIFYING_HASH) String hash) {
        logger.info("The offer with identifying hash: " + hash +  " was rejected.");
    }

    @PostMapping(path = DataConsumerConstants.ACCEPT_URI, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody public void acceptEndpoint(@RequestParam(name = DataConsumerConstants.IDENTIFYING_HASH) String hash,
                                                @RequestParam(name = DataConsumerConstants.PRODUCER_NAME) String producerName,
                                                @RequestParam(name = DataConsumerConstants.PRODUCER_ADDRESS) String producerAddress,
                                                @RequestParam(name = DataConsumerConstants.PRODUCER_PORT) String producerPort,
                                                @RequestParam(name = DataConsumerConstants.SERVICE_URI) String serviceUri) {
        logger.info("The offer with identifying hash: " + hash + " was accepted by: " + producerName + " with address: " + producerAddress + ":" + producerPort);
        db.add(new AcceptedItem(hash, producerName, producerAddress, producerPort, serviceUri));
    }

}
