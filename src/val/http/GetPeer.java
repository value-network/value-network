package val.http;

import com.google.gson.JsonElement;
import val.peer.Peer;
import val.peer.Peers;

import javax.servlet.http.HttpServletRequest;

import static val.http.JSONResponses.MISSING_PEER;
import static val.http.JSONResponses.UNKNOWN_PEER;
import static val.http.common.Parameters.PEER_PARAMETER;

final class GetPeer extends APIServlet.APIRequestHandler {

    static final GetPeer instance = new GetPeer();

    private GetPeer() {
        super(new APITag[]{APITag.INFO}, PEER_PARAMETER);
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) {

        String peerAddress = req.getParameter(PEER_PARAMETER);
        if (peerAddress == null) {
            return MISSING_PEER;
        }

        Peer peer = Peers.getPeer(peerAddress);
        if (peer == null) {
            return UNKNOWN_PEER;
        }

        return JSONData.peer(peer);

    }

}
