package val.peer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import val.Blockchain;
import val.BlockchainProcessor;
import val.Constants;
import val.TransactionProcessor;
import val.services.AccountService;
import val.services.TimeService;
import val.util.CountingInputStream;
import val.util.CountingOutputStream;
import val.util.JSON;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class PeerServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(PeerServlet.class);
    private static final JsonElement UNSUPPORTED_REQUEST_TYPE;
    private static final JsonElement UNSUPPORTED_PROTOCOL;

    static {
        final JsonObject response = new JsonObject();
        response.addProperty("error", "Unsupported request type!");
        UNSUPPORTED_REQUEST_TYPE = response;
    }

    static {
        final JsonObject response = new JsonObject();
        response.addProperty("error", "Unsupported protocol!");
        UNSUPPORTED_PROTOCOL = response;
    }

    private final Map<String, PeerRequestHandler> peerRequestHandlers;

    public PeerServlet(TimeService timeService, AccountService accountService,
                       Blockchain blockchain,
                       TransactionProcessor transactionProcessor,
                       BlockchainProcessor blockchainProcessor) {
        final Map<String, PeerRequestHandler> map = new HashMap<>();
        map.put("addPeers", AddPeers.instance);
        map.put("getCumulativeDifficulty", new GetCumulativeDifficulty(blockchain));
        map.put("getInfo", new GetInfo(timeService));
        map.put("getMilestoneBlockIds", new GetMilestoneBlockIds(blockchain));
        map.put("getNextBlockIds", new GetNextBlockIds(blockchain));
        map.put("getBlocksFromHeight", new GetBlocksFromHeight(blockchain));
        map.put("getNextBlocks", new GetNextBlocks(blockchain));
        map.put("getPeers", GetPeers.instance);
        map.put("getUnconfirmedTransactions", new GetUnconfirmedTransactions(transactionProcessor));
        map.put("processBlock", new ProcessBlock(blockchain, blockchainProcessor));
        map.put("processTransactions", new ProcessTransactions(transactionProcessor));
        map.put("getAccountBalance", new GetAccountBalance(accountService));
        map.put("getAccountRecentTransactions", new GetAccountRecentTransactions(accountService, blockchain));
        peerRequestHandlers = Collections.unmodifiableMap(map);
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        try {

            // logger.info("peerServlet get remote request:[{}], UA:[{}]", req.getRemoteAddr(), req.getHeader("User-Agent"));
            if (!Peers.isSupportedUserAgent(req.getHeader("User-Agent"))) {
                // logger.info("peerServlet get remote request:[{}], UA Check failed!!!", req.getRemoteAddr());
                return;
            }
            process(req, resp);
        } catch (Exception e) { // We don't want to send exception information to client...
            resp.setStatus(500);
            logger.warn("Error handling peer request", e);
        }
    }

    private void process(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        PeerImpl peer = null;
        JsonElement response;

        ExtendedProcessRequest extendedProcessRequest = null;
        // logger.info("peerServlet get remote request:[{}]", req.getRemoteAddr());

        String requestType = "unknown";
        try {
            peer = Peers.addPeer(req.getRemoteAddr(), null);
            if (peer == null || peer.isBlacklisted()) {
                return;
            }

            JsonObject request;
            CountingInputStream cis = new CountingInputStream(req.getInputStream());
            try (Reader reader = new InputStreamReader(cis, StandardCharsets.UTF_8)) {
                request = JSON.getAsJsonObject(JSON.parse(reader));
            }

            // logger.info("peerServlet get remote request:[{}], req body:[{}]", req.getRemoteAddr(), request.toString());

            if (request == null) {
                return;
            }

            if (peer.isState(Peer.State.DISCONNECTED)) {
                peer.setState(Peer.State.CONNECTED);
                if (peer.getAnnouncedAddress() != null) {
                    Peers.updateAddress(peer);
                }
            }
            peer.updateDownloadedVolume(cis.getCount());

            // logger.info("peerServlet get remote peer [{}] request:{}", peer.getAnnouncedAddress(), request.toString());

            if (request.get(Constants.PROTOCOL) != null && JSON.getAsString(request.get(Constants.PROTOCOL)).equals("B1")) {
                requestType = "" + JSON.getAsString(request.get("requestType"));
                PeerRequestHandler peerRequestHandler = peerRequestHandlers.get(JSON.getAsString(request.get("requestType")));
                if (peerRequestHandler != null) {
                    if (peerRequestHandler instanceof ExtendedPeerRequestHandler) {
                        extendedProcessRequest = ((ExtendedPeerRequestHandler) peerRequestHandler).extendedProcessRequest(request, peer);
                        response = extendedProcessRequest.response;
                    } else {
                        response = peerRequestHandler.processRequest(request, peer);
                    }
                } else {
                    response = UNSUPPORTED_REQUEST_TYPE;
                }
            } else {
                logger.debug("Unsupported protocol " + JSON.getAsString(request.get(Constants.PROTOCOL)));
                response = UNSUPPORTED_PROTOCOL;
            }

            // logger.info("peerServlet get remote peer [{}] request:{}, response:{}", peer.getAnnouncedAddress(), request.toString(), response.toString());

        } catch (RuntimeException e) {
            logger.debug("Error processing POST request", e);
            JsonObject json = new JsonObject();
            json.addProperty("error", e.toString());
            response = json;
        }

        resp.setContentType("text/plain; charset=UTF-8");
        try {
            long byteCount;

            CountingOutputStream cos = new CountingOutputStream(resp.getOutputStream());
            try (Writer writer = new OutputStreamWriter(cos, StandardCharsets.UTF_8)) {
                JSON.writeTo(response, writer);
            }
            byteCount = cos.getCount();
            if (peer != null) {
                peer.updateUploadedVolume(byteCount);
            }
        } catch (Exception e) {
            if (peer != null) {
                peer.blacklist(e, "can't respond to requestType=" + requestType);
            }
            return;
        }

        if (extendedProcessRequest != null) {
            extendedProcessRequest.afterRequestHook.run();
        }
    }

    interface RequestLifecycleHook {
        void run();
    }

    abstract static class PeerRequestHandler {
        abstract JsonElement processRequest(JsonObject request, Peer peer);
    }

    abstract static class ExtendedPeerRequestHandler extends PeerRequestHandler {
        JsonElement processRequest(JsonObject request, Peer peer) {
            return null;
        }

        abstract ExtendedProcessRequest extendedProcessRequest(JsonObject request, Peer peer);
    }

    static class ExtendedProcessRequest {
        final JsonElement response;
        final RequestLifecycleHook afterRequestHook;

        public ExtendedProcessRequest(JsonElement response, RequestLifecycleHook afterRequestHook) {
            this.response = response;
            this.afterRequestHook = afterRequestHook;
        }
    }

}
