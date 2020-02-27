package val.peer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import val.Version;

public interface Peer extends Comparable<Peer> {

    String getPeerAddress();

    String getAnnouncedAddress();

    State getState();

    Version getVersion();

    String getApplication();

    String getPlatform();

    String getSoftware();

    int getBlockHeight();

    boolean shareAddress();

    boolean isWellKnown();

    boolean isRebroadcastTarget();

    boolean isBlacklisted();

    boolean isAtLeastMyVersion();

    boolean isHigherOrEqualVersionThan(Version version);

    void blacklist(Exception cause, String description);

    void blacklist(String description);

    void blacklist();

    void unBlacklist();

    void remove();

    long getDownloadedVolume();

    long getUploadedVolume();

    int getLastUpdated();

    JsonObject send(JsonElement request);

    // Long getLastUnconfirmedTransactionTimestamp();

    // void setLastUnconfirmedTransactionTimestamp(Long lastUnconfirmedTransactionTimestamp);

    enum State {
        NON_CONNECTED, CONNECTED, DISCONNECTED
    }

}
