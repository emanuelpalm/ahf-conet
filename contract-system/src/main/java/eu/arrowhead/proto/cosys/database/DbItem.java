package eu.arrowhead.proto.cosys.database;


import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;

public class DbItem {

    private String requestName;
    private String randomHash;
    private Integer offerAmount;
    private String requestAddress;
    private String requestPort;

    public DbItem(String randomHash, Integer offerAmount, String requestAddress, String requestPort) {
        this.randomHash = randomHash;
        this.offerAmount = offerAmount;
        this.requestAddress = requestAddress;
        this.requestPort = requestPort;
    }

    public Integer getOfferAmount() {
        return offerAmount;
    }

    public void setOfferAmount(Integer offerAmount) {
        this.offerAmount = offerAmount;
    }

    public String getRandomHash() {
        return randomHash;
    }

    public void setRandomHash(String randomHash) {
        this.randomHash = randomHash;
    }


    public String getRequestName() {
        return requestName;
    }

    public void setRequestName(String requestName) {
        this.requestName = requestName;
    }

    public String getRequestAddress() {
        return requestAddress;
    }

    public void setRequestAddress(String requestAddress) {
        this.requestAddress = requestAddress;
    }

    public String getRequestPort() {
        return requestPort;
    }

    public void setRequestPort(String requestPort) {
        this.requestPort = requestPort;
    }
}
