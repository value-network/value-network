package val.peer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bitlet.weupnp.GatewayDevice;
import org.bitlet.weupnp.GatewayDiscover;
import org.bitlet.weupnp.PortMappingEntry;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.FilterMapping;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.DoSFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import val.*;
import val.props.PropertyService;
import val.props.Props;
import val.services.AccountService;
import val.services.TimeService;
import val.util.JSON;
import val.util.Listener;
import val.util.Listeners;
import val.util.ThreadPool;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static val.props.Props.P2P_ENABLE_TX_REBROADCAST;
import static val.props.Props.P2P_SEND_TO_LIMIT;
import static val.util.JSON.prepareRequest;

public final class Peers {

    static final int LOGGING_MASK_EXCEPTIONS = 1;
    static final int LOGGING_MASK_NON200_RESPONSES = 2;
    static final int LOGGING_MASK_200_RESPONSES = 4;
    static final int DEFAULT_PEER_PORT = 8123;
    static final int TESTNET_PEER_PORT = 7123;
    private static final Logger logger = LoggerFactory.getLogger(Peers.class);
    private static final Random r = new Random();
    private static final Listeners<Peer, Event> listeners = new Listeners<>();
    private static final ConcurrentMap<String, PeerImpl> peers = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, String> announcedAddresses = new ConcurrentHashMap<>();
    private static final Collection<PeerImpl> allPeers = Collections.unmodifiableCollection(peers.values());
    private static final ExecutorService sendBlocksToPeersService = Executors.newCachedThreadPool();
    private static final ExecutorService blocksSendingService = Executors.newFixedThreadPool(10);
    private static final JsonElement getUnconfirmedTransactionsRequest;
    private static final ExecutorService utReceivingService = Executors.newCachedThreadPool();
    private static final ExecutorService utSendingService = Executors.newCachedThreadPool();
    private static final List<Peer> processingQueue = new ArrayList<>();
    private static final List<Peer> beingProcessed = new ArrayList<>();
    static int communicationLoggingMask;
    static Set<String> wellKnownPeers;
    static Set<String> knownBlacklistedPeers;
    static Set<String> rebroadcastPeers;
    static int connectTimeout;
    static int readTimeout;
    static int blacklistingPeriod;
    private static final Runnable peerUnBlacklistingThread = () -> {

        try {
            try {

                long curTime = System.currentTimeMillis();
                for (PeerImpl peer : peers.values()) {
                    peer.updateBlacklistedStatus(curTime);
                }

            } catch (Exception e) {
                logger.debug("Error un-blacklisting peer", e);
            }
        } catch (Exception t) {
            logger.info("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString(), t);
            System.exit(1);
        }

    };
    static boolean getMorePeers;
    static JsonElement myPeerInfoRequest;
    static JsonElement myPeerInfoRequestBackwardsCompatible;
    static JsonElement myPeerInfoResponse;
    static JsonElement myPeerInfoResponseBackwardsCompatible;
    private static int connectWellKnownFirst;
    private static boolean connectWellKnownFinished;
    private static String myPlatform;
    private static String myAddress;
    private static int myPeerServerPort;
    private static boolean useUpnp;
    private static boolean shareMyAddress;
    private static int maxNumberOfConnectedPublicPeers;
    private static int sendToPeersLimit;
    private static boolean usePeersDb;
    private static boolean savePeers;
    private static int getMorePeersThreshold;
    private static final Runnable getMorePeersThread = new Runnable() {

        private final JsonElement getPeersRequest;
        private final AtomicBoolean addedNewPeer = new AtomicBoolean(false);

        {
            JsonObject request = new JsonObject();
            request.addProperty("requestType", "getPeers");
            getPeersRequest = prepareRequest(request);
        }

        {
            Peers.addListener(peer -> addedNewPeer.set(true), Event.NEW_PEER);
        }

        @Override
        public void run() {

            try {
                try {
                    /* We do not want more peers if above Threshold but we need enough to
                     * connect to selected number of peers
                     */
                    if (peers.size() >= getMorePeersThreshold && peers.size() > maxNumberOfConnectedPublicPeers) {
                        return;
                    }

                    Peer peer = getAnyPeer(Peer.State.CONNECTED);
                    if (peer == null) {
                        return;
                    }
                    JsonObject response = peer.send(getPeersRequest);
                    if (response == null) {
                        return;
                    }
                    JsonArray peers = JSON.getAsJsonArray(response.get("peers"));
                    Set<String> addedAddresses = new HashSet<>();
                    if (peers != null) {
                        for (JsonElement announcedAddress : peers) {
                            if (addPeer(JSON.getAsString(announcedAddress)) != null) {
                                addedAddresses.add(JSON.getAsString(announcedAddress));
                            }
                        }
                        if (savePeers && addedNewPeer.get()) {
                            addedNewPeer.set(false);
                        }
                    }

                    JsonArray myPeers = new JsonArray();
                    for (Peer myPeer : Peers.getAllPeers()) {
                        if (!myPeer.isBlacklisted() && myPeer.getAnnouncedAddress() != null
                                && myPeer.getState() == Peer.State.CONNECTED && myPeer.shareAddress()
                                && !addedAddresses.contains(myPeer.getAnnouncedAddress())
                                && !myPeer.getAnnouncedAddress().equals(peer.getAnnouncedAddress())
                                && myPeer.isHigherOrEqualVersionThan(Constants.MIN_VERSION)
                        ) {
                            myPeers.add(myPeer.getAnnouncedAddress());
                        }
                    }
                    //executor shutdown?
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }

                    if (myPeers.size() > 0) {
                        JsonObject request = new JsonObject();
                        request.addProperty("requestType", "addPeers");
                        request.add("peers", myPeers);
                        peer.send(prepareRequest(request));
                    }

                } catch (Exception e) {
                    logger.debug("Error requesting peers from a peer", e);
                }
            } catch (Exception t) {
                logger.info("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString(), t);
                System.exit(1);
            }

        }

    };
    private static String dumpPeersVersion;
    private static int lastSavedPeers;
    private static TimeService timeService;
    private static final Runnable peerConnectingThread = new Runnable() {
        @Override
        public void run() {
            try {
                int numConnectedPeers = getNumberOfConnectedPublicPeers();
                /*
                 * aggressive connection with while loop.
                 * if we have connected to our target amount we can exit loop.
                 * if peers size is equal or below connected value we have nothing to connect to
                 */
                while (!Thread.currentThread().isInterrupted() && ThreadPool.running.get()
                        && numConnectedPeers < maxNumberOfConnectedPublicPeers && peers.size() > numConnectedPeers) {
                    PeerImpl peer = (PeerImpl) getAnyPeer(ThreadLocalRandom.current().nextInt(2) == 0 ? Peer.State.NON_CONNECTED : Peer.State.DISCONNECTED);
                    if (peer != null) {
                        peer.connect(timeService.getEpochTime());
                        /*
                         * remove non connected peer. if peer is blacklisted, keep it to maintain blacklist time.
                         * Peers should never be removed if total peers are below our target to prevent total erase of peers
                         * if we loose Internet connection
                         */

                        if (!peer.isHigherOrEqualVersionThan(Constants.MIN_VERSION)
                                || (peer.getState() != Peer.State.CONNECTED && !peer.isBlacklisted() && peers.size() > maxNumberOfConnectedPublicPeers)) {
                            removePeer(peer);
                        } else {
                            numConnectedPeers++;
                        }
                    }

                    try {
                        int i = 1;
                        while (i++ < 100) {
                            Thread.sleep(10);
                        }
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }

                int now = timeService.getEpochTime();
                for (PeerImpl peer : peers.values()) {
                    if (peer.getState() == Peer.State.CONNECTED && now - peer.getLastUpdated() > 3600) {
                        peer.connect(timeService.getEpochTime());
                        if (!peer.isHigherOrEqualVersionThan(Constants.MIN_VERSION) ||
                                (peer.getState() != Peer.State.CONNECTED && !peer.isBlacklisted() && peers.size() > maxNumberOfConnectedPublicPeers)) {
                            removePeer(peer);
                        }
                    }
                }

                if (lastSavedPeers != peers.size()) {
                    lastSavedPeers = peers.size();
                    updateSavedPeers();
                }

            } catch (Exception e) {
                logger.debug("Error connecting to peer", e);
            }
        }

        private void updateSavedPeers() {
            Set<String> oldPeers = new HashSet<>(ValNetwork.getDbs().getPeerDb().loadPeers());
            Set<String> currentPeers = new HashSet<>();
            for (Peer peer : Peers.peers.values()) {
                if (peer.getAnnouncedAddress() != null
                        && !peer.isBlacklisted()
                        && !peer.isWellKnown()
                        && peer.isHigherOrEqualVersionThan(Constants.MIN_VERSION)) {
                    currentPeers.add(peer.getAnnouncedAddress());
                }
            }
            Set<String> toDelete = new HashSet<>(oldPeers);
            toDelete.removeAll(currentPeers);
            try {
                ValNetwork.getStores().beginTransaction();
                ValNetwork.getDbs().getPeerDb().deletePeers(toDelete);
                //   logger.debug("Deleted " + toDelete.size() + " peers from the peers database");
                currentPeers.removeAll(oldPeers);
                ValNetwork.getDbs().getPeerDb().addPeers(currentPeers);
                //   logger.debug("Added " + currentPeers.size() + " peers to the peers database");
                ValNetwork.getStores().commitTransaction();
            } catch (Exception e) {
                ValNetwork.getStores().rollbackTransaction();
                throw e;
            } finally {
                ValNetwork.getStores().endTransaction();
            }
        }


    };
    private static PropertyService propertyService;

    static {
        JsonObject request = new JsonObject();
        request.addProperty("requestType", "getUnconfirmedTransactions");
        getUnconfirmedTransactionsRequest = prepareRequest(request);
    }

    private Peers() {
    } // never

    public static boolean isSupportedUserAgent(String header) {
        if (header == null || header.isEmpty() || !header.trim().startsWith("VLM/")) {
            return false;
        } else {
            try {
                return PeerImpl.isHigherOrEqualVersion(Constants.MIN_VERSION, Version.parse(header.trim().substring("VLM/".length())));
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
    }

    public static void init(TimeService timeService, AccountService accountService, Blockchain blockchain, TransactionProcessor transactionProcessor,
                            BlockchainProcessor blockchainProcessor, PropertyService propertyService, ThreadPool threadPool) {
        Peers.timeService = timeService;
        Peers.propertyService = propertyService;

        myPlatform = propertyService.getString(Props.P2P_MY_PLATFORM);
        if (propertyService.getString(Props.P2P_MY_ADDRESS) != null
                && propertyService.getString(Props.P2P_MY_ADDRESS).trim().isEmpty()
                && Init.gateway != null) {
            String externalIPAddress = null;
            try {
                externalIPAddress = Init.gateway.getExternalIPAddress();
            } catch (IOException | SAXException e) {
                logger.info("Can't get gateways IP adress");
            }
            myAddress = externalIPAddress;
        } else {
            myAddress = propertyService.getString(Props.P2P_MY_ADDRESS);
        }

        if (myAddress != null && myAddress.endsWith(":" + TESTNET_PEER_PORT) && !ValNetwork.getPropertyService().getBoolean(Props.DEV_TESTNET)) {
            throw new RuntimeException("Port " + TESTNET_PEER_PORT + " should only be used for testnet!!!");
        }
        myPeerServerPort = propertyService.getInt(Props.P2P_PORT);
        if (myPeerServerPort == TESTNET_PEER_PORT && !ValNetwork.getPropertyService().getBoolean(Props.DEV_TESTNET)) {
            throw new RuntimeException("Port " + TESTNET_PEER_PORT + " should only be used for testnet!!!");
        }
        useUpnp = propertyService.getBoolean(Props.P2P_UPNP);
        shareMyAddress = propertyService.getBoolean(Props.P2P_SHARE_MY_ADDRESS) && !ValNetwork.getPropertyService().getBoolean(Props.DEV_OFFLINE);

        JsonObject json = new JsonObject();
        if (myAddress != null && !myAddress.isEmpty()) {
            try {
                URI uri = new URI("http://" + myAddress.trim());
                String host = uri.getHost();
                int port = uri.getPort();
                if (!ValNetwork.getPropertyService().getBoolean(Props.DEV_TESTNET)) {
                    if (port >= 0) {
                        json.addProperty("announcedAddress", myAddress);
                    } else {
                        json.addProperty("announcedAddress", host + (myPeerServerPort != DEFAULT_PEER_PORT ? ":" + myPeerServerPort : ""));
                    }
                } else {
                    json.addProperty("announcedAddress", host);
                }
            } catch (URISyntaxException e) {
                logger.info("Your announce address is invalid: " + myAddress);
                throw new RuntimeException(e.toString(), e);
            }
        }

        json.addProperty("application", ValNetwork.APPLICATION);
        json.addProperty("version", ValNetwork.VERSION.toBackwardsCompatibleString());
        json.addProperty("platform", Peers.myPlatform);
        json.addProperty("shareAddress", Peers.shareMyAddress);
        //json.addProperty("blockHeight", Volume.getBlockchain() != null && Volume.getBlockchain().getLastBlock() != null ? Volume.getBlockchain().getLastBlock().getHeight() : 0);
        logger.debug("My peer info:\n" + JSON.toJsonString(json));
        myPeerInfoResponseBackwardsCompatible = JSON.cloneJson(json);
        json.addProperty("requestType", "getInfo");
        myPeerInfoRequestBackwardsCompatible = prepareRequest(JSON.getAsJsonObject(JSON.cloneJson(json)));
        json.addProperty("version", ValNetwork.VERSION.toString());
        myPeerInfoResponse = json;
        myPeerInfoRequest = prepareRequest(json);


        if (propertyService.getBoolean(P2P_ENABLE_TX_REBROADCAST)) {
            rebroadcastPeers = Collections
                    .unmodifiableSet(new HashSet<>(propertyService.getStringList(ValNetwork.getPropertyService().getBoolean(Props.DEV_TESTNET) ? Props.DEV_P2P_REBROADCAST_TO : Props.P2P_REBROADCAST_TO)));
        } else {
            rebroadcastPeers = Collections.emptySet();
        }

        List<String> wellKnownPeersList = propertyService.getStringList(ValNetwork.getPropertyService().getBoolean(Props.DEV_TESTNET) ? Props.DEV_P2P_BOOTSTRAP_PEERS : Props.P2P_BOOTSTRAP_PEERS);

        for (String rePeer : rebroadcastPeers) {
            if (!wellKnownPeersList.contains(rePeer)) {
                wellKnownPeersList.add(rePeer);
            }
        }
        if (wellKnownPeersList.isEmpty() || ValNetwork.getPropertyService().getBoolean(Props.DEV_OFFLINE)) {
            wellKnownPeers = Collections.emptySet();
        } else {
            wellKnownPeers = Collections.unmodifiableSet(new HashSet<>(wellKnownPeersList));
        }

        connectWellKnownFirst = propertyService.getInt(Props.P2P_NUM_BOOTSTRAP_CONNECTIONS);
        connectWellKnownFinished = (connectWellKnownFirst == 0);
        logger.info("wellKnownPeersList is {}, connectWellKnownFirst:{}", wellKnownPeersList.toString(), connectWellKnownFirst);

        List<String> knownBlacklistedPeersList = propertyService.getStringList(Props.P2P_BLACKLISTED_PEERS);
        if (knownBlacklistedPeersList.isEmpty()) {
            knownBlacklistedPeers = Collections.emptySet();
        } else {
            knownBlacklistedPeers = Collections.unmodifiableSet(new HashSet<>(knownBlacklistedPeersList));
        }

        maxNumberOfConnectedPublicPeers = propertyService.getInt(Props.P2P_MAX_CONNECTIONS);
        connectTimeout = propertyService.getInt(Props.P2P_TIMEOUT_CONNECT_MS);
        readTimeout = propertyService.getInt(Props.P2P_TIMEOUT_READ_MS);

        blacklistingPeriod = propertyService.getInt(Props.P2P_BLACKLISTING_TIME_MS);
        communicationLoggingMask = propertyService.getInt(Props.BRS_COMMUNICATION_LOGGING_MASK);
        sendToPeersLimit = propertyService.getInt(P2P_SEND_TO_LIMIT);
        usePeersDb = propertyService.getBoolean(Props.P2P_USE_PEERS_DB) && !ValNetwork.getPropertyService().getBoolean(Props.DEV_OFFLINE);
        savePeers = usePeersDb && propertyService.getBoolean(Props.P2P_SAVE_PEERS);
        getMorePeers = propertyService.getBoolean(Props.P2P_GET_MORE_PEERS);
        getMorePeersThreshold = propertyService.getInt(Props.P2P_GET_MORE_PEERS_THRESHOLD);
        dumpPeersVersion = propertyService.getString(Props.DEV_DUMP_PEERS_VERSION);

        final List<Future<String>> unresolvedPeers = Collections.synchronizedList(new ArrayList<>());

        threadPool.runBeforeStart(new Runnable() {

            private void loadPeers(Collection<String> addresses) {
                for (final String address : addresses) {
                    Future<String> unresolvedAddress = sendBlocksToPeersService.submit(() -> {
                        Peer peer = Peers.addPeer(address);
                        return peer == null ? address : null;
                    });
                    unresolvedPeers.add(unresolvedAddress);
                }
            }

            @Override
            public void run() {
                if (!wellKnownPeers.isEmpty()) {
                    loadPeers(wellKnownPeers);
                }
                if (usePeersDb) {
                    logger.debug("Loading known peers from the database...");
                    loadPeers(ValNetwork.getDbs().getPeerDb().loadPeers());
                }
                lastSavedPeers = peers.size();
            }
        }, false);

        threadPool.runAfterStart(() -> {
            for (Future<String> unresolvedPeer : unresolvedPeers) {
                try {
                    String badAddress = unresolvedPeer.get(5, TimeUnit.SECONDS);
                    if (badAddress != null) {
                        logger.debug("Failed to resolve peer address: " + badAddress);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    logger.debug("Failed to add peer", e);
                } catch (TimeoutException e) {
                }
            }
            logger.debug("Known peers: " + peers.size());
        });

        Init.init(timeService, accountService, blockchain, transactionProcessor, blockchainProcessor, propertyService, threadPool);

        if (!ValNetwork.getPropertyService().getBoolean(Props.DEV_OFFLINE)) {
            threadPool.scheduleThread("PeerConnecting", Peers.peerConnectingThread, 5);
            threadPool.scheduleThread("PeerUnBlacklisting", Peers.peerUnBlacklistingThread, 1);
            if (Peers.getMorePeers) {
                threadPool.scheduleThread("GetMorePeers", Peers.getMorePeersThread, 5);
            }
        }

    }

    public static void shutdown(ThreadPool threadPool) {
        if (Init.peerServer != null) {
            try {
                Init.peerServer.stop();
            } catch (Exception e) {
                logger.info("Failed to stop peer server", e);
            }
        }
        if (Init.gateway != null) {
            try {
                Init.gateway.deletePortMapping(Init.port, "TCP");
            } catch (Exception e) {
                logger.info("Failed to remove UPNP rule from gateway", e);
            }
        }
        if (dumpPeersVersion != null) {
            StringBuilder buf = new StringBuilder();
            for (Map.Entry<String, String> entry : announcedAddresses.entrySet()) {
                Peer peer = peers.get(entry.getValue());
                if (peer != null && peer.getState() == Peer.State.CONNECTED && peer.shareAddress() && !peer.isBlacklisted()
                        && peer.getVersion() != null
                        && peer.getVersion().toString().startsWith(dumpPeersVersion)) {
                    buf.append("('").append(entry.getKey()).append("'), ");
                }
            }
            logger.info(buf.toString());
        }

        threadPool.shutdownExecutor(sendBlocksToPeersService);
        // threadPool.shutdownExecutor(blocksSendingService);
    }

    private static boolean addListener(Listener<Peer> listener, Event eventType) {
        return Peers.listeners.addListener(listener, eventType);
    }

    public static boolean removeListener(Listener<Peer> listener, Event eventType) {
        return Peers.listeners.removeListener(listener, eventType);
    }

    static void notifyListeners(Peer peer, Event eventType) {
        Peers.listeners.notify(peer, eventType);
    }

    public static Collection<? extends Peer> getAllPeers() {
        return allPeers;
    }

    public static List<? extends Peer> getActivePeers() {
        List<PeerImpl> activePeers = new ArrayList<>();
        for (PeerImpl peer : peers.values()) {
            if (peer.getState() != Peer.State.NON_CONNECTED) {
                activePeers.add(peer);
            }
        }
        return activePeers;
    }

    public static Collection<? extends Peer> getPeers(Peer.State state) {
        List<PeerImpl> peerList = new ArrayList<>();
        for (PeerImpl peer : peers.values()) {
            if (peer.getState() == state) {
                peerList.add(peer);
            }
        }
        return peerList;
    }

    public static Peer getPeer(String peerAddress) {
        return peers.get(peerAddress);
    }

    public static Peer addPeer(String announcedAddress) {
        if (announcedAddress == null) {
            return null;
        }
        announcedAddress = announcedAddress.trim();

        // logger.info("load peer:{}", announcedAddress);
        Peer peer;
        if ((peer = peers.get(announcedAddress)) != null) {
            return peer;
        }
        String address;
        if ((address = announcedAddresses.get(announcedAddress)) != null && (peer = peers.get(address)) != null) {
            return peer;
        }
        try {
            URI uri = new URI("http://" + announcedAddress);
            String host = uri.getHost();
            if ((peer = peers.get(host)) != null) {
                return peer;
            }
            InetAddress inetAddress = InetAddress.getByName(host);
            return addPeer(inetAddress.getHostAddress(), announcedAddress);
        } catch (URISyntaxException | UnknownHostException e) {
            //logger.debug("Invalid peer address: " + announcedAddress + ", " + e.toString());
            return null;
        }
    }

    static PeerImpl addPeer(final String address, final String announcedAddress) {
        // logger.info("addPeer, address:{}, announcedAddress:{}", address, announcedAddress);
        //re-add the [] to ipv6 addresses lost in getHostAddress() above
        String clean_address = address;
        if (clean_address.split(":").length > 2) {
            clean_address = "[" + clean_address + "]";
        }
        PeerImpl peer;
        if ((peer = peers.get(clean_address)) != null) {
            return peer;
        }
        String peerAddress = normalizeHostAndPort(clean_address);
        if (peerAddress == null) {
            return null;
        }
        if ((peer = peers.get(peerAddress)) != null) {
            return peer;
        }

        String announcedPeerAddress = address.equals(announcedAddress) ? peerAddress : normalizeHostAndPort(announcedAddress);

        if (Peers.myAddress != null && !Peers.myAddress.isEmpty() && Peers.myAddress.equalsIgnoreCase(announcedPeerAddress)) {
            return null;
        }

        peer = new PeerImpl(peerAddress, announcedPeerAddress);
        if (ValNetwork.getPropertyService().getBoolean(Props.DEV_TESTNET) && peer.getPort() > 0 && peer.getPort() != TESTNET_PEER_PORT) {
            logger.debug("Peer " + peerAddress + " on testnet is not using port " + TESTNET_PEER_PORT + ", ignoring");
            return null;
        }
        peers.put(peerAddress, peer);
        if (announcedAddress != null) {
            updateAddress(peer);
        }
        listeners.notify(peer, Event.NEW_PEER);
        return peer;
    }

    static PeerImpl removePeer(PeerImpl peer) {
        if (peer.getAnnouncedAddress() != null) {
            announcedAddresses.remove(peer.getAnnouncedAddress());
        }
        return peers.remove(peer.getPeerAddress());
    }

    static void updateAddress(PeerImpl peer) {
        String oldAddress = announcedAddresses.put(peer.getAnnouncedAddress(), peer.getPeerAddress());
        if (oldAddress != null && !peer.getPeerAddress().equals(oldAddress)) {
            //logger.debug("Peer " + peer.getAnnouncedAddress() + " has changed address from " + oldAddress
            //        + " to " + peer.getPeerAddress());
            Peer oldPeer = peers.remove(oldAddress);
            if (oldPeer != null) {
                Peers.notifyListeners(oldPeer, Peers.Event.REMOVE);
            }
        }
    }

    public static void sendToSomePeers(Block block) {
        JsonObject request = block.getJsonObject();
        request.addProperty("requestType", "processBlock");

        blocksSendingService.submit(() -> {
            final JsonElement jsonRequest = prepareRequest(request);

            int successful = 0;
            List<Future<JsonObject>> expectedResponses = new ArrayList<>();
            for (final Peer peer : peers.values()) {

                if (peerEligibleForSending(peer, false)) {
                    Future<JsonObject> futureResponse = sendBlocksToPeersService.submit(() -> peer.send(jsonRequest));
                    expectedResponses.add(futureResponse);
                }
                if (expectedResponses.size() >= Peers.sendToPeersLimit - successful) {
                    for (Future<JsonObject> future : expectedResponses) {
                        try {
                            JsonObject response = future.get();
                            if (response != null && response.get("error") == null) {
                                successful += 1;
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } catch (ExecutionException e) {
                            logger.debug("Error in sendToSomePeers", e);
                        }

                    }
                    expectedResponses.clear();
                }
                if (successful >= Peers.sendToPeersLimit) {
                    return;
                }
            }
        });
    }

    public static CompletableFuture<JsonObject> readUnconfirmedTransactionsNonBlocking(Peer peer) {
        return CompletableFuture.supplyAsync(() -> peer.send(getUnconfirmedTransactionsRequest), utReceivingService);
    }

    public synchronized static void feedingTime(Peer peer, Function<Peer, List<Transaction>> foodDispenser, BiConsumer<Peer, List<Transaction>> doneFeedingLog) {
        if (!beingProcessed.contains(peer)) {
            beingProcessed.add(peer);
            CompletableFuture.runAsync(() -> feedPeer(peer, foodDispenser, doneFeedingLog), utSendingService);
        } else if (!processingQueue.contains(peer)) {
            processingQueue.add(peer);
        }
    }

    private static void feedPeer(Peer peer, Function<Peer, List<Transaction>> foodDispenser, BiConsumer<Peer, List<Transaction>> doneFeedingLog) {
        List<Transaction> transactionsToSend = foodDispenser.apply(peer);

        if (!transactionsToSend.isEmpty()) {
            logger.trace("Feeding {} {} transactions", peer.getPeerAddress(), transactionsToSend.size());
            JsonObject response = peer.send(sendUnconfirmedTransactionsRequest(transactionsToSend));

            if (response != null && response.get("error") == null) {
                doneFeedingLog.accept(peer, transactionsToSend);
            } else {
                logger.warn("Error feeding {} transactions: {} error: {}", peer.getPeerAddress(), transactionsToSend.stream().map(Transaction::getId).collect(Collectors.toList()), response);
            }
        } else {
            logger.trace("No need to feed {}", peer.getPeerAddress());
        }

        beingProcessed.remove(peer);

        if (processingQueue.contains(peer)) {
            processingQueue.remove(peer);
            beingProcessed.add(peer);
            feedPeer(peer, foodDispenser, doneFeedingLog);
        }
    }

    private static JsonElement sendUnconfirmedTransactionsRequest(List<Transaction> transactions) {
        JsonObject request = new JsonObject();
        JsonArray transactionsData = new JsonArray();

        for (Transaction transaction : transactions) {
            transactionsData.add(transaction.getJsonObject());
        }

        request.addProperty("requestType", "processTransactions");
        request.add("transactions", transactionsData);

        return prepareRequest(request);
    }

    private static boolean peerEligibleForSending(Peer peer, boolean sendSameBRSclass) {
        return peer.isHigherOrEqualVersionThan(Constants.MIN_VERSION)
                && (!sendSameBRSclass || peer.isAtLeastMyVersion())
                && !peer.isBlacklisted()
                && peer.getState() == Peer.State.CONNECTED
                && peer.getAnnouncedAddress() != null;
    }

    public static Peer getAnyPeer(Peer.State state) {

        if (!connectWellKnownFinished) {
            int wellKnownConnected = 0;
            for (Peer peer : peers.values()) {
                if (peer.isWellKnown() && peer.getState() == Peer.State.CONNECTED) {
                    wellKnownConnected++;
                }
            }
            if (wellKnownConnected >= connectWellKnownFirst) {
                connectWellKnownFinished = true;
                logger.info("Finished connecting to " + connectWellKnownFirst + " well known peers.");
                logger.info("You can open your Vol Wallet in your favorite browser with: http://127.0.0.1:8125 or http://localhost:8125");
            }
        }

        List<Peer> selectedPeers = new ArrayList<>();
        for (Peer peer : peers.values()) {
            if (!peer.isBlacklisted() && peer.getState() == state && peer.shareAddress()
                    && (connectWellKnownFinished || peer.getState() == Peer.State.CONNECTED || peer.isWellKnown())) {
                selectedPeers.add(peer);
            }
        }

        if (!selectedPeers.isEmpty()) {
            return selectedPeers.get(r.nextInt(selectedPeers.size()));
        }
        return null;
    }

    public static List<Peer> getAllActivePriorityPlusSomeExtraPeers() {
        final List<Peer> peersActivePriorityPlusSomeExtraPeers = new ArrayList<>();
        int amountExtrasLeft = propertyService.getInt(P2P_SEND_TO_LIMIT);

        for (Peer peer : peers.values()) {
            if (peerEligibleForSending(peer, true)) {
                if (peer.isRebroadcastTarget()) {
                    peersActivePriorityPlusSomeExtraPeers.add(peer);
                } else if (amountExtrasLeft > 0) {
                    peersActivePriorityPlusSomeExtraPeers.add(peer);
                    amountExtrasLeft--;
                }
            }
        }

        return peersActivePriorityPlusSomeExtraPeers;
    }

    static String normalizeHostAndPort(String address) {
        try {
            if (address == null) {
                return null;
            }
            URI uri = new URI("http://" + address.trim());
            String host = uri.getHost();
            if (host == null || host.equals("")) {
                return null;
            }
            InetAddress inetAddress = InetAddress.getByName(host);
            if (inetAddress.isAnyLocalAddress() || inetAddress.isLoopbackAddress() ||
                    inetAddress.isLinkLocalAddress()) {
                return null;
            }
            int port = uri.getPort();
            return port == -1 ? host : host + ':' + port;
        } catch (URISyntaxException | UnknownHostException e) {
            return null;
        }
    }

    private static int getNumberOfConnectedPublicPeers() {
        int numberOfConnectedPeers = 0;
        for (Peer peer : peers.values()) {
            if (peer.getState() == Peer.State.CONNECTED) {
                numberOfConnectedPeers++;
            }
        }
        return numberOfConnectedPeers;
    }

    public enum Event {
        BLACKLIST, UNBLACKLIST, DEACTIVATE, REMOVE,
        DOWNLOADED_VAL, UPLOADED_VAL, WEIGHT,
        ADDED_ACTIVE_PEER, CHANGED_ACTIVE_PEER,
        NEW_PEER
    }

    private static class Init {

        private static Server peerServer;
        private static GatewayDevice gateway;
        private static Integer port;

        private Init() {
        }

        static void init(TimeService timeService, AccountService accountService, Blockchain blockchain, TransactionProcessor transactionProcessor,
                         BlockchainProcessor blockchainProcessor, PropertyService propertyService, ThreadPool threadPool) {
            if (Peers.shareMyAddress) {
                if (useUpnp) {
                    port = propertyService.getInt(Props.P2P_PORT);
                    GatewayDiscover gatewayDiscover = new GatewayDiscover();
                    gatewayDiscover.setTimeout(2000);
                    try {
                        gatewayDiscover.discover();
                    } catch (IOException | SAXException | ParserConfigurationException e) {
                    }
                    logger.trace("Looking for Gateway Devices");
                    gateway = gatewayDiscover.getValidGateway();

                    Runnable GwDiscover = () -> {
                        if (gateway != null) {
                            GatewayDevice.setHttpReadTimeout(2000);
                            try {
                                InetAddress localAddress = gateway.getLocalAddress();
                                String externalIPAddress = gateway.getExternalIPAddress();
                                logger.info("Attempting to map {0}:{1} -> {2}:{3} on Gateway {0} ({1})",
                                        externalIPAddress, port, localAddress, port, gateway.getModelName(), gateway.getModelDescription());
                                PortMappingEntry portMapping = new PortMappingEntry();
                                if (gateway.getSpecificPortMappingEntry(port, "TCP", portMapping)) {
                                    logger.info("Port was already mapped. Aborting test.");
                                } else {
                                    if (gateway.addPortMapping(port, port, localAddress.getHostAddress(), "TCP", "vol")) {
                                        logger.info("UPnP Mapping successful");
                                    } else {
                                        logger.warn("UPnP Mapping was denied!");
                                    }
                                }
                            } catch (IOException | SAXException e) {
                                logger.error("Can't start UPnP", e);
                            }
                        }
                    };
                    if (gateway != null) {
                        new Thread(GwDiscover).start();
                    } else {
                        logger.warn("Tried to establish UPnP, but it was denied by the network.");
                    }
                }

                peerServer = new Server();
                ServerConnector connector = new ServerConnector(peerServer);
                port = ValNetwork.getPropertyService().getBoolean(Props.DEV_TESTNET) ? TESTNET_PEER_PORT : Peers.myPeerServerPort;
                connector.setPort(port);
                final String host = propertyService.getString(Props.P2P_LISTEN);
                connector.setHost(host);
                connector.setIdleTimeout(propertyService.getInt(Props.P2P_TIMEOUT_IDLE_MS));
                connector.setReuseAddress(true);
                peerServer.addConnector(connector);

                ServletHolder peerServletHolder = new ServletHolder(new PeerServlet(timeService, accountService, blockchain,
                        transactionProcessor, blockchainProcessor));
                boolean isGzipEnabled = propertyService.getBoolean(Props.JETTY_P2P_GZIP_FILTER);
                peerServletHolder.setInitParameter("isGzipEnabled", Boolean.toString(isGzipEnabled));

                ServletHandler peerHandler = new ServletHandler();
                peerHandler.addServletWithMapping(peerServletHolder, "/*");

                if (propertyService.getBoolean(Props.JETTY_P2P_DOS_FILTER)) {
                    FilterHolder dosFilterHolder = peerHandler.addFilterWithMapping(DoSFilter.class, "/*", FilterMapping.DEFAULT);
                    dosFilterHolder.setInitParameter("maxRequestsPerSec", propertyService.getString(Props.JETTY_P2P_DOS_FILTER_MAX_REQUESTS_PER_SEC));
                    dosFilterHolder.setInitParameter("throttledRequests", propertyService.getString(Props.JETTY_P2P_DOS_FILTER_THROTTLED_REQUESTS));
                    dosFilterHolder.setInitParameter("delayMs", propertyService.getString(Props.JETTY_P2P_DOS_FILTER_DELAY_MS));
                    dosFilterHolder.setInitParameter("maxWaitMs", propertyService.getString(Props.JETTY_P2P_DOS_FILTER_MAX_WAIT_MS));
                    dosFilterHolder.setInitParameter("maxRequestMs", propertyService.getString(Props.JETTY_P2P_DOS_FILTER_MAX_REQUEST_MS));
                    dosFilterHolder.setInitParameter("maxthrottleMs", propertyService.getString(Props.JETTY_P2P_DOS_FILTER_THROTTLE_MS));
                    dosFilterHolder.setInitParameter("maxIdleTrackerMs", propertyService.getString(Props.JETTY_P2P_DOS_FILTER_MAX_IDLE_TRACKER_MS));
                    dosFilterHolder.setInitParameter("trackSessions", propertyService.getString(Props.JETTY_P2P_DOS_FILTER_TRACK_SESSIONS));
                    dosFilterHolder.setInitParameter("insertHeaders", propertyService.getString(Props.JETTY_P2P_DOS_FILTER_INSERT_HEADERS));
                    dosFilterHolder.setInitParameter("remotePort", propertyService.getString(Props.JETTY_P2P_DOS_FILTER_REMOTE_PORT));
                    dosFilterHolder.setInitParameter("ipWhitelist", propertyService.getString(Props.JETTY_P2P_DOS_FILTER_IP_WHITELIST));
                    dosFilterHolder.setInitParameter("managedAttr", propertyService.getString(Props.JETTY_P2P_DOS_FILTER_MANAGED_ATTR));
                    dosFilterHolder.setAsyncSupported(true);
                }

                if (isGzipEnabled) {
                    GzipHandler gzipHandler = new GzipHandler();
                    gzipHandler.setIncludedMethods(propertyService.getString(Props.JETTY_P2P_GZIP_FILTER_METHODS));
                    gzipHandler.setInflateBufferSize(propertyService.getInt(Props.JETTY_P2P_GZIP_FILTER_BUFFER_SIZE));
                    gzipHandler.setMinGzipSize(propertyService.getInt(Props.JETTY_P2P_GZIP_FILTER_MIN_GZIP_SIZE));
                    gzipHandler.setIncludedMimeTypes("text/plain");
                    gzipHandler.setHandler(peerHandler);
                    peerServer.setHandler(gzipHandler);
                } else {
                    peerServer.setHandler(peerHandler);
                }
                peerServer.setStopAtShutdown(true);
                threadPool.runBeforeStart(() -> {
                    try {
                        peerServer.start();
                        logger.info("Started peer networking server at " + host + ":" + port);
                    } catch (Exception e) {
                        logger.error("Failed to start peer networking server", e);
                        throw new RuntimeException(e.toString(), e);
                    }
                }, true);
            } else {
                peerServer = null;
                gateway = null;
                port = null;
                logger.info("shareMyAddress is disabled, will not start peer networking server");
            }
        }

    }

}
