package val.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import val.peer.Peer;
import val.peer.Peers;
import val.util.Convert;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

import static val.http.common.Parameters.ACTIVE_PARAMETER;
import static val.http.common.Parameters.STATE_PARAMETER;

final class GetPeers extends APIServlet.APIRequestHandler {

    static final GetPeers instance = new GetPeers();

    private GetPeers() {
        super(new APITag[]{APITag.INFO}, ACTIVE_PARAMETER, STATE_PARAMETER);
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) {

        boolean active = "true".equalsIgnoreCase(req.getParameter(ACTIVE_PARAMETER));
        String stateValue = Convert.emptyToNull(req.getParameter(STATE_PARAMETER));

        JsonArray peers = new JsonArray();
        List<Peer> peerArr = new ArrayList<Peer>();
        for (Peer peer : active ? Peers.getActivePeers() : stateValue != null ? Peers.getPeers(Peer.State.valueOf(stateValue)) : Peers.getAllPeers()) {
            //peers.add(peer.getPeerAddress());
            peerArr.add(peer);
        }
        peerArr.sort((o1, o2) -> Integer.compare(o2.getBlockHeight(), o1.getBlockHeight()));
        for (Peer peer : peerArr) {
            peers.add(peer.getPeerAddress());
        }

        JsonObject response = new JsonObject();
        response.add("peers", peers);
        return response;
    }

}
