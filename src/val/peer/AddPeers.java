package val.peer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import val.util.JSON;

final class AddPeers extends PeerServlet.PeerRequestHandler {

    static final AddPeers instance = new AddPeers();

    private AddPeers() {
    }

    @Override
    JsonElement processRequest(JsonObject request, Peer peer) {
        JsonArray peers = JSON.getAsJsonArray(request.get("peers"));
        if (peers != null && Peers.getMorePeers) {
            for (JsonElement announcedAddress : peers) {
                Peers.addPeer(JSON.getAsString(announcedAddress));
            }
        }
        return JSON.emptyJSON;
    }

}
