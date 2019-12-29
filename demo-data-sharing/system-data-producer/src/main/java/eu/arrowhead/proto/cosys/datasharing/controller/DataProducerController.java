package eu.arrowhead.proto.cosys.datasharing.controller;

import eu.arrowhead.common.dto.shared.EventDTO;
import eu.arrowhead.proto.cosys.datasharing.DataProviderConstants;
import eu.arrowhead.proto.cosys.datasharing.database.InMemoryDb;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.concurrent.ConcurrentLinkedQueue;


@RestController
@RequestMapping(DataProviderConstants.PROVIDER_URI)
public class DataProducerController {

    @Resource(name = DataProviderConstants.NOTIFICATION_QUEUE)
    private ConcurrentLinkedQueue<EventDTO> notificationQueue;

    @Resource(name = DataProviderConstants.IN_MEMORY_DB)
    private InMemoryDb inMemoryDb;

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody public String directRequest(@RequestParam(name = "randomIdentifier" , required = true) final String randomIdentifier) {
        if (inMemoryDb.getOfferMap().containsKey(randomIdentifier)) {
            String hashValue = inMemoryDb.getOfferMap().remove(randomIdentifier);
            String tempValue = inMemoryDb.getValueFromKey(hashValue);
            return tempValue;
        }

        return "Identifier not found";
    }

    @PostMapping(path = DataProviderConstants.REQUEST_RECEIVED_NOTIFICATION_URI)
    public void receieveEventRequestRecieved(@RequestBody final EventDTO event) {

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