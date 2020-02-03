package eu.arrowhead.proto.cosys.datasharing.database;

public class AcceptedItem {
    private String identifyingHash;
    private String producerName;
    private String producerAddress;
    private String producerPort;
    private String serviceUri;

    public AcceptedItem(String identifyingHash, String producerName, String producerAddress, String producerPort, String serviceUri) {
        this.identifyingHash = identifyingHash;
        this.producerAddress = producerAddress;
        this.producerName = producerName;
        this.producerPort = producerPort;
        this.serviceUri = serviceUri;
    }


    public String getIdentifyingHash() {
        return identifyingHash;
    }

    public void setIdentifyingHash(String identifyingHash) {
        this.identifyingHash = identifyingHash;
    }

    public String getProducerName() {
        return producerName;
    }

    public void setProducerName(String producerName) {
        this.producerName = producerName;
    }

    public String getProducerAddress() {
        return producerAddress;
    }

    public void setProducerAddress(String producerAddress) {
        this.producerAddress = producerAddress;
    }

    public String getProducerPort() {
        return producerPort;
    }

    public void setProducerPort(String producerPort) {
        this.producerPort = producerPort;
    }

    public String getServiceUri() {
        return serviceUri;
    }

    public void setServiceUri(String serviceUri) {
        this.serviceUri = serviceUri;
    }
}
