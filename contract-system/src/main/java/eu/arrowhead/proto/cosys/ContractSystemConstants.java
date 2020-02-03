package eu.arrowhead.proto.cosys;

import org.springframework.http.HttpMethod;

public class ContractSystemConstants {

    public static final String BASE_PACKAGE = "eu.arrowhead";
    public static final String HTTP_METHOD = "http-method";
    public static final String INTERFACE_SECURE = "HTTPS-SECURE-JSON";
    public static final String INTERFACE_INSECURE = "HTTP-INSECURE-JSON";

    public static final String OFFER_AMOUNT = "offer-amount";
    public static final String RANDOM_HASH = "random-hash";
    public static final String REJECT_URI = "/reject";
    public static final String OFFER_URI = "/offer";
    public static final String ACCEPT_URI = "/accept";
    public static final String OFFER_LIST = "offer-list";
    public static final String IDENTIFYING_HASH = "ident-hash";
    public static final String REQUEST_PORT = "request-port";
    public static final String REQUEST_ADDRESS = "request-address";
    public static final String PRODUCER_NAME = "producer-name";
    public static final String PRODUCER_ADDRESS = "producer-address";
    public static final String PRODUCER_PORT = "producer-port";
    public static final String OFFER_NAME = "offer-contract";
    public static final String REJECT_NAME = "reject-contract";
    public static final String ACCEPT_NAME = "accept-contract";
    public static final String DATA_HASH = "data-hash";
    public static final String SERVICE_URI = "service-uri";
}
