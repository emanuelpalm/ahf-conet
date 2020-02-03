package eu.arrowhead.proto.cosys.datasharing.controller;
import eu.arrowhead.common.dto.shared.EventDTO;
import eu.arrowhead.proto.cosys.datasharing.DataProducerListener;
import eu.arrowhead.proto.cosys.datasharing.DataProducerConstants;
import eu.arrowhead.proto.cosys.datasharing.database.InMemoryDb;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.concurrent.ConcurrentLinkedQueue;


@RestController
@RequestMapping(DataProducerConstants.PROVIDER_URI)
public class DataProducerController {

    private final Logger logger = LogManager.getLogger(DataProducerListener.class);

    @Resource(name = DataProducerConstants.NOTIFICATION_QUEUE)
    private ConcurrentLinkedQueue<EventDTO> notificationQueue;

    @Resource(name = DataProducerConstants.IN_MEMORY_DB)
    private InMemoryDb inMemoryDb;

    @PostMapping(path = DataProducerConstants.GET_DATA_URI, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody public String directRequest(@RequestParam(name = "identifying-hash" , required = true) String randomIdentifier) {
        logger.info("Got a direct request for data");
        JSONObject returnObject = new JSONObject();

        if (inMemoryDb.getOfferMap().containsKey(randomIdentifier)) {
            String hashValue = inMemoryDb.getOfferMap().remove(randomIdentifier);
            String data = inMemoryDb.getValueFromKey(hashValue);
            returnObject.put("data", data);
            returnObject.put("status", "OK");
        } else {
            returnObject.put("data", "Identifier not found");
            returnObject.put("status", "ERROR");
        }

        return returnObject.toString();

    }

    @PostMapping(path = DataProducerConstants.REQUEST_RECEIVED_NOTIFICATION_URI)
    public void receieveEventRequestRecieved(@RequestBody final EventDTO event) {
        logger.info("Received an offer");
        if (event.getEventType() != null) {
           notificationQueue.add(event);
        }
    }

    @GetMapping(path = "/echo")
    @ResponseBody public String echo() {
        return "echo";
    }

    @GetMapping(path = "/get-all")
    @ResponseBody public String test() {
        return inMemoryDb.getInMemoryMap().toString();
    }

}
