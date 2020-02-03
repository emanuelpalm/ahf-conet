package eu.arrowhead.proto.cosys.datasharing;

public class DataConsumerConstants {
    public static final String BASE_PACKAGE = "eu.arrowhead.proto.cosys.datasharing";
    public static final String INTERFACE_INSECURE = "HTTP-INSECURE-JSON";
    public static final String INTERFACE_SECURE = "HTTPS-SECURE-JSON";
    public static final String HTTP_METHOD = "http-method";
    public static final String IDENTIFYING_HASH = "ident-hash";

    public static final String PRODUCER_PORT = "producer-port";
    public static final String PRODUCER_ADDRESS = "producer-address";
    public static final String PRODUCER_NAME = "producer-name";

    public static final String REJECT_NAME = "reject-relay";
    public static final String REJECT_URI = "/rejectRelay";

    public static final String ACCEPT_NAME = "accept-relay";
    public static final String ACCEPT_URI = "/acceptRelay";
    public static final String ACCEPTED_ITEM_LIST = "accepted-items-list";
    public static final String SERVICE_URI = "service-uri";
    public static final String SUB_TASK = "cli-thread";
}
