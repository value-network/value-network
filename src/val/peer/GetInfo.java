package val.peer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import val.services.TimeService;
import val.util.JSON;

final class GetInfo extends PeerServlet.PeerRequestHandler {

    private final TimeService timeService;

    GetInfo(TimeService timeService) {
        this.timeService = timeService;
    }

    @Override
    JsonElement processRequest(JsonObject request, Peer peer) {
        PeerImpl peerImpl = (PeerImpl) peer;
        String announcedAddress = JSON.getAsString(request.get("announcedAddress"));
        if (announcedAddress != null && !(announcedAddress = announcedAddress.trim()).isEmpty()) {
            if (peerImpl.getAnnouncedAddress() != null && !announcedAddress.equals(peerImpl.getAnnouncedAddress())) {
                // force verification of changed announced address
                peerImpl.setState(Peer.State.NON_CONNECTED);
            }
            peerImpl.setAnnouncedAddress(announcedAddress);
        }
        String application = JSON.getAsString(request.get("application"));
        if (application == null) {
            application = "?";
        }
        peerImpl.setApplication(application.trim());

        String version = JSON.getAsString(request.get("version"));
        if (version == null) {
            version = "?";
        }
        peerImpl.setVersion(version.trim());

        String platform = JSON.getAsString(request.get("platform"));
        if (platform == null) {
            platform = "?";
        }
        peerImpl.setPlatform(platform.trim());

        peerImpl.setShareAddress(Boolean.TRUE.equals(JSON.getAsBoolean(request.get("shareAddress"))));
        peerImpl.setLastUpdated(timeService.getEpochTime());

        //peerImpl.setState(Peer.State.CONNECTED);
        Peers.notifyListeners(peerImpl, Peers.Event.ADDED_ACTIVE_PEER);

        return peerImpl.getVersion().backwardsCompatibilityNeeded() ? Peers.myPeerInfoResponseBackwardsCompatible : Peers.myPeerInfoResponse;

    }

}
