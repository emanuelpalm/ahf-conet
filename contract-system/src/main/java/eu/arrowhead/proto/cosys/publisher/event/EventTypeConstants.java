package eu.arrowhead.proto.cosys.publisher.event;

public class EventTypeConstants {

    //=================================================================================================
    // members

    public static final String EVENT_TYPE_START_INIT = "START_INIT";
    public static final String EVENT_TYPE_START_RUN = "START_RUN";
    public static final String EVENT_TYPE_REQUEST_RECEIVED = "REQUEST_RECEIVED";
    public static final String EVENT_TYPE_REQUEST_RECEIVED_METADATA_REQUEST_TYPE = "REQUEST_TYPE";
    public static final String EVENT_TYPE_PUBLISHER_DESTROYED = "PUBLISHER_DESTROYED";
    //public static final String EVENT_TYPE_PUBLISH_OFFER = "PUBLISH_OFFER";

    //=================================================================================================
    // assistant methods

    //-------------------------------------------------------------------------------------------------
    private EventTypeConstants () {
        throw new UnsupportedOperationException();
    }
}
