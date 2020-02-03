package eu.arrowhead.proto.cosys.datasharing.database;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

public class InMemoryDb {
    private int threshold;
    private HashMap<String, String> inMemoryMap;
    private HashMap<String, String> offerMap;

    public InMemoryDb() {
        inMemoryMap = new HashMap<>();
        offerMap = new HashMap<>();

        // loading random data into the hashtable for testing
        String[] fruits = new String[]{"Orange", "Apple", "Pear", "Strawberry"};

        for (String fruit : fruits) {
            try {
                this.putIntoHashTable(this.hashKey(fruit), fruit);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void putIntoHashTable(String key, String value) {
        this.inMemoryMap.put(key, value);
    }

    // Easier way to change hash function
    private String hashKey(String key) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] hashInBytes = md.digest(key.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hashInBytes.length; i++) {
            sb.append(Integer.toString((hashInBytes[i] & 0xff) + 0x100, 16).substring(1));
        }
        String s = sb.toString();
        return s;
    }


    public synchronized void setThreshold(int newThreshold) {
        threshold = newThreshold;
    }

    public synchronized String getValueFromKey(String key) {
        return inMemoryMap.get(key);
    }

    public synchronized HashMap<String, String> getOfferMap() {
        return offerMap;
    }

    public synchronized  HashMap<String, String> getInMemoryMap() {
        return inMemoryMap;
    }
}
