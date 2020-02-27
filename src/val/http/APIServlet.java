package val.http;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import val.*;
import val.assetexchange.AssetExchange;
import val.deeplink.DeeplinkQRCodeGenerator;
import val.feesuggestions.FeeSuggestionCalculator;
import val.props.PropertyService;
import val.props.Props;
import val.services.*;
import val.util.JSON;
import val.util.Subnet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.net.InetAddress;
import java.util.*;

public final class APIServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(APIServlet.class);
    public static Map<String, APIRequestHandler> apiRequestHandlers;
    private static boolean acceptSurplusParams;
    private static boolean enforcePost;
    private static Map<String, PrimitiveRequestHandler> primitiveRequestHandlers;

    public APIServlet(TransactionProcessor transactionProcessor, Blockchain blockchain, BlockchainProcessor blockchainProcessor, ParameterService parameterService,
                      AccountService accountService, AliasService aliasService, AssetExchange assetExchange,
                      EscrowService escrowService, DGSGoodsStoreService digitalGoodsStoreService,
                      SubscriptionService subscriptionService, ATService atService, TimeService timeService, EconomicClustering economicClustering, TransactionService transactionService,
                      BlockService blockService, Generator generator, PropertyService propertyService, APITransactionManager apiTransactionManager, FeeSuggestionCalculator feeSuggestionCalculator,
                      GlobalParameterService globalParameterService, DeeplinkQRCodeGenerator deeplinkQRCodeGenerator) {

        enforcePost = propertyService.getBoolean(Props.API_SERVER_ENFORCE_POST);
        acceptSurplusParams = propertyService.getBoolean(Props.API_ACCEPT_SURPLUS_PARAMS);

        final Map<String, APIRequestHandler> map = new HashMap<>();
        final Map<String, PrimitiveRequestHandler> primitiveMap = new HashMap<>();

//    map.put("broadcastTransaction", new BroadcastTransaction(transactionProcessor, parameterService, transactionService));
        map.put("calculateFullHash", new CalculateFullHash());
        // map.put("cancelAskOrder", new CancelAskOrder(parameterService, blockchain, assetExchange, apiTransactionManager));
        // map.put("cancelBidOrder", new CancelBidOrder(parameterService, blockchain, assetExchange, apiTransactionManager));
        // map.put("decryptFrom", new DecryptFrom(parameterService));
        // map.put("dgsListing", new DGSListing(parameterService, blockchain, apiTransactionManager));
        // map.put("dgsDelisting", new DGSDelisting(parameterService, blockchain, apiTransactionManager));
        // map.put("dgsDelivery", new DGSDelivery(parameterService, blockchain, accountService, apiTransactionManager));
        // map.put("dgsFeedback", new DGSFeedback(parameterService, blockchain, accountService, apiTransactionManager));
        // map.put("dgsPriceChange", new DGSPriceChange(parameterService, blockchain, apiTransactionManager));
        // map.put("dgsPurchase", new DGSPurchase(parameterService, blockchain, accountService, timeService, apiTransactionManager));
        // map.put("dgsQuantityChange", new DGSQuantityChange(parameterService, blockchain, apiTransactionManager));
        // map.put("dgsRefund", new DGSRefund(parameterService, blockchain, accountService, apiTransactionManager));
        // map.put("encryptTo", new EncryptTo(parameterService, accountService));
        map.put("generateToken", new GenerateToken(timeService));
        // map.put("getAccount", new GetAccount(parameterService, accountService));
        // map.put("getAccountsWithName", new GetAccountsWithName(accountService));
        // map.put("getAccountBlockIds", new GetAccountBlockIds(parameterService, blockchain));
        // map.put("getAccountBlocks", new GetAccountBlocks(blockchain, parameterService, blockService));
        // map.put("getAccountId", new GetAccountId());
//    map.put("getAccountPublicKey", new GetAccountPublicKey(parameterService));
        // map.put("getAccountTransactionIds", new GetAccountTransactionIds(parameterService, blockchain));
        // map.put("getAccountTransactions", new GetAccountTransactions(parameterService, blockchain));
        // map.put("getAccountLessors", new GetAccountLessors(parameterService, blockchain));
        // map.put("sellAlias", new SellAlias(parameterService, blockchain, apiTransactionManager));
        // map.put("buyAlias", new BuyAlias(parameterService, blockchain, aliasService, apiTransactionManager));
        // map.put("getAlias", new GetAlias(parameterService, aliasService));
        // map.put("getAliases", new GetAliases(parameterService, aliasService));
        // map.put("getAllAssets", new GetAllAssets(assetExchange));
        // map.put("getAsset", new GetAsset(parameterService, assetExchange));
        // map.put("getAssets", new GetAssets(assetExchange));
        // map.put("getAssetIds", new GetAssetIds(assetExchange));
        // map.put("getAssetsByIssuer", new GetAssetsByIssuer(parameterService, assetExchange));
        // map.put("getAssetAccounts", new GetAssetAccounts(parameterService, assetExchange));
        // map.put("getBalance", new GetBalance(parameterService));
        // map.put("getBlock", new GetBlock(blockchain, blockService));
        // map.put("getBlockId", new GetBlockId(blockchain));
        // map.put("getBlocks", new GetBlocks(blockchain, blockService));
//    map.put("getBlockchainStatus", new GetBlockchainStatus(blockchainProcessor, blockchain, timeService));
        // map.put("getConstants", GetConstants.instance);
        // map.put("getDGSGoods", new GetDGSGoods(digitalGoodsStoreService));
        // map.put("getDGSGood", new GetDGSGood(parameterService));
        // map.put("getDGSPurchases", new GetDGSPurchases(digitalGoodsStoreService));
        // map.put("getDGSPurchase", new GetDGSPurchase(parameterService));
        // map.put("getDGSPendingPurchases", new GetDGSPendingPurchases(digitalGoodsStoreService));
        // map.put("getECBlock", new GetECBlock(blockchain, timeService, economicClustering));
        // map.put("getMyInfo", GetMyInfo.instance);
//    map.put("getPeer", GetPeer.instance);
        map.put("getMyPeerInfo", new GetMyPeerInfo(blockchainProcessor, transactionProcessor));
//    map.put("getPeers", GetPeers.instance);
        map.put("getState", new GetState(blockchain, assetExchange, accountService, escrowService, aliasService, timeService, generator));
        map.put("getTime", new GetTime(timeService));
        // map.put("getTrades", new GetTrades(parameterService, assetExchange));
        // map.put("getAllTrades", new GetAllTrades(assetExchange));
        // map.put("getAssetTransfers", new GetAssetTransfers(parameterService, accountService, assetExchange));
        // map.put("getTransaction", new GetTransaction(transactionProcessor, blockchain));
        // map.put("getTransactionBytes", new GetTransactionBytes(blockchain, transactionProcessor));
        // map.put("getUnconfirmedTransactionIds", new GetUnconfirmedTransactionIds(transactionProcessor));
//    map.put("getUnconfirmedTransactions", new GetUnconfirmedTransactions(transactionProcessor));
        // map.put("getAccountCurrentAskOrderIds", new GetAccountCurrentAskOrderIds(parameterService, assetExchange));
        // map.put("getAccountCurrentBidOrderIds", new GetAccountCurrentBidOrderIds(parameterService, assetExchange));
        // map.put("getAccountCurrentAskOrders", new GetAccountCurrentAskOrders(parameterService, assetExchange));
        // map.put("getAccountCurrentBidOrders", new GetAccountCurrentBidOrders(parameterService, assetExchange));
        // map.put("getAllOpenAskOrders", new GetAllOpenAskOrders(assetExchange));
        // map.put("getAllOpenBidOrders", new GetAllOpenBidOrders(assetExchange));
        // map.put("getAskOrder", new GetAskOrder(assetExchange));
        // map.put("getAskOrderIds", new GetAskOrderIds(parameterService, assetExchange));
        // map.put("getAskOrders", new GetAskOrders(parameterService, assetExchange));
        // map.put("getBidOrder", new GetBidOrder(assetExchange));
        // map.put("getBidOrderIds", new GetBidOrderIds(parameterService, assetExchange));
        // map.put("getBidOrders", new GetBidOrders(parameterService, assetExchange));
        // map.put("suggestFee", new SuggestFee(feeSuggestionCalculator));
        // map.put("issueAsset", new IssueAsset(parameterService, blockchain, apiTransactionManager));
        map.put("longConvert", LongConvert.instance);
        map.put("parseTransaction", new ParseTransaction(parameterService, transactionService));
        // map.put("placeAskOrder", new PlaceAskOrder(parameterService, blockchain, apiTransactionManager, accountService));
        // map.put("placeBidOrder", new PlaceBidOrder(parameterService, blockchain, apiTransactionManager));
        // map.put("rsConvert", RSConvert.instance);
        // map.put("readMessage", new ReadMessage(blockchain, accountService));
        // map.put("sendMessage", new SendMessage(parameterService, apiTransactionManager));
//    map.put("sendMoney", new SendMoney(parameterService, apiTransactionManager));
//    map.put("sendMoneyMulti", new SendMoneyMulti(parameterService, blockchain, apiTransactionManager));
//    map.put("sendMoneyMultiSame", new SendMoneyMultiSame(parameterService, blockchain, apiTransactionManager));
//    map.put("setAccountInfo", new SetAccountInfo(parameterService, blockchain, apiTransactionManager));
        // map.put("setAlias", new SetAlias(parameterService, blockchain, aliasService, apiTransactionManager));
        map.put("signTransaction", new SignTransaction(parameterService, transactionService));
        // map.put("transferAsset", new TransferAsset(parameterService, blockchain, apiTransactionManager, accountService));
        map.put("getMiningInfo", new GetMiningInfo(blockchain));
        map.put("submitNonce", new SubmitNonce(accountService, blockchain, generator));
        // map.put("getRewardRecipient", new GetRewardRecipient(parameterService, blockchain, accountService));
        // map.put("setRewardRecipient", new SetRewardRecipient(parameterService, blockchain, accountService, apiTransactionManager));
        // map.put("getAccountsWithRewardRecipient", new GetAccountsWithRewardRecipient(parameterService, accountService));
        // map.put("sendMoneyEscrow", new SendMoneyEscrow(parameterService, blockchain, apiTransactionManager));
        // map.put("escrowSign", new EscrowSign(parameterService, blockchain, escrowService, apiTransactionManager));
        // map.put("getEscrowTransaction", new GetEscrowTransaction(escrowService));
        // map.put("getAccountEscrowTransactions", new GetAccountEscrowTransactions(parameterService, escrowService));
        // map.put("sendMoneySubscription", new SendMoneySubscription(parameterService, blockchain, apiTransactionManager));
        // map.put("subscriptionCancel", new SubscriptionCancel(parameterService, subscriptionService, blockchain, apiTransactionManager));
        // map.put("getSubscription", new GetSubscription(subscriptionService));
        // map.put("getAccountSubscriptions", new GetAccountSubscriptions(parameterService, subscriptionService));
        // map.put("getSubscriptionsToAccount", new GetSubscriptionsToAccount(parameterService, subscriptionService));
        // map.put("createATProgram", new CreateATProgram(parameterService, blockchain, apiTransactionManager));
        // map.put("getAT", new GetAT(parameterService, accountService));
        // map.put("getATDetails", new GetATDetails(parameterService, accountService));
        // map.put("getATIds", new GetATIds(atService));
        // map.put("getATLong", GetATLong.instance);
        // map.put("getAccountATs", new GetAccountATs(parameterService, atService, accountService));

        // new api
        map.put("search", new Search(blockchain, accountService));
        map.put("getBlockList", new GetBlockList(blockchain, blockService)); //getBlocks
        map.put("getTransactionList", new GetTransactionList(transactionProcessor, parameterService, blockchain));//getTransaction
        map.put("getAccountList", new GetAccountList(parameterService, accountService, blockchain));//getAccount
        map.put("getMinePoolList", new GetMinePoolList(parameterService, accountService, blockchain, timeService));// get mine pool list info
        map.put("getMinerList", new GetMinerList(parameterService, accountService, blockchain, timeService));// get miner info lists in the mine pool id, only for node info
        map.put("getAccountTransactionCount", new GetAccountTransactionCount(parameterService, blockchain));
        map.put("getBlockchainPledged", new GetBlockchainPledged(accountService));
        map.put("getBlockchainAddress", new GetBlockchainAddressCount(blockchain));
        map.put("getPledges", new GetPledges(accountService));
        //map.put("grantPoolMiner", new GrantPoolMiner(parameterService, accountService, timeService));
        //map.put("revokePoolMiner", new RevokePoolMiner(parameterService, accountService, timeService));
        //map.put("getPoolMinerList", new GetPoolMinerList(accountService,blockchain,timeService));//only for pool miner managerment

        map.put("setPledges", new SetPledges(parameterService, blockchain, accountService, timeService, apiTransactionManager));
        map.put("setUnpledges", new SetUnpledges(parameterService, blockchain, accountService, timeService, apiTransactionManager));
        //map.put("withdrawPledges", new WithdrawPledges(parameterService, blockchain, accountService, timeService, apiTransactionManager));
        map.put("setGlobalParameter", new SetGlobalParameter(parameterService, blockchain, apiTransactionManager));
        map.put("getGlobalParameter", new GetGlobalParameter(timeService, blockchain, globalParameterService));

        map.put("sendMoney", new SendMoney(parameterService, apiTransactionManager));
        map.put("sendMoneyMulti", new SendMoneyMulti(parameterService, blockchain, apiTransactionManager));
        map.put("sendMoneyMultiSame", new SendMoneyMultiSame(parameterService, blockchain, apiTransactionManager));
        map.put("getAccountPublicKey", new GetAccountPublicKey(parameterService));
        map.put("getPeer", GetPeer.instance);
        map.put("getPeers", GetPeers.instance);
        map.put("getBlockchainStatus", new GetBlockchainStatus(blockchainProcessor, blockchain, timeService, blockService));
        map.put("setAccountInfo", new SetAccountInfo(parameterService, blockchain, apiTransactionManager));
        map.put("broadcastTransaction", new BroadcastTransaction(transactionProcessor, parameterService, transactionService));
        map.put("getUnconfirmedTransactions", new GetUnconfirmedTransactions(transactionProcessor));

        primitiveMap.put("generateSendTransactionQRCode", new GenerateDeeplinkQRCode(deeplinkQRCodeGenerator));

        if (API.enableDebugAPI) {
            //map.put("clearUnconfirmedTransactions", new ClearUnconfirmedTransactions(transactionProcessor));
            //map.put("fullReset", new FullReset(blockchainProcessor));
            //map.put("popOff", new PopOff(blockchainProcessor, blockchain, blockService));
            // map.put("scan", new Scan(blockchainProcessor, blockchain));
            //map.put("getGlobalParameterStoreCache", new GetGlobalParameterStoreCache());
        }

        apiRequestHandlers = Collections.unmodifiableMap(map);
        primitiveRequestHandlers = Collections.unmodifiableMap(primitiveMap);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        try {
            process(req, resp);
        } catch (Exception e) { // We don't want to send exception information to client...
            resp.setStatus(500);
            logger.warn("Error handling GET request", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        try {
            process(req, resp);
        } catch (Exception e) { // We don't want to send exception information to client...
            resp.setStatus(500);
            logger.warn("Error handling GET request", e);
        }
    }

    private void process(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, private");
        resp.setHeader("Pragma", "no-cache");
        resp.setDateHeader("Expires", 0);
        // System.out.printf("API.allowOrigin:%s\n",API.allowOrigin);
        if ("*".equals(API.allowOrigin)) {
            //System.out.println("set allow origin success!");
            resp.setHeader("Access-Control-Allow-Origin", "*");
            resp.setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, X-Key, Authorization, X-Real-IP");
            resp.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS, POST, PUT, DELETE");
        }

        JsonElement response = JSON.emptyJSON;

        try {

            long startTime = System.currentTimeMillis();

            if (API.allowedBotHosts != null) {
                InetAddress remoteAddress = InetAddress.getByName(req.getRemoteHost());
                boolean allowed = false;
                for (Subnet allowedSubnet : API.allowedBotHosts) {
                    if (allowedSubnet.isInNet(remoteAddress)) {
                        allowed = true;
                        break;
                    }
                }
                if (!allowed) {
                    response = JSONResponses.ERROR_NOT_ALLOWED;
                    return;
                }
            }

            String requestType = req.getParameter("requestType");
//      System.out.printf("requestType: %s\n", requestType);
            if (requestType == null) {
                response = JSONResponses.ERROR_INCORRECT_REQUEST;
                return;
            }

            APIRequestHandler apiRequestHandler = apiRequestHandlers.get(requestType);
            if (apiRequestHandler == null) {
                final PrimitiveRequestHandler primitiveRequestHandler = primitiveRequestHandlers.get(req.getParameter("requestType"));

                if (primitiveRequestHandler != null) {
                    primitiveRequestHandler.processRequest(req, resp);
                } else {
                    response = JSONResponses.ERROR_INCORRECT_REQUEST;
                }
                //System.out.print("apiRequestHandler is null \n");
                return;
            }

            if (enforcePost && apiRequestHandler.requirePost() && !"POST".equals(req.getMethod())) {
                response = JSONResponses.POST_REQUIRED;
                return;
            }

            try {
                if (apiRequestHandler.startDbTransaction()) {
                    ValNetwork.getStores().beginTransaction();
                }
                apiRequestHandler.validateRequest(req);
                response = apiRequestHandler.processRequest(req);
            } catch (ParameterException e) {
                response = e.getErrorResponse();
            } catch (ValException | RuntimeException e) {
                logger.debug("Error processing API request", e);
                e.printStackTrace();
                response = JSONResponses.ERROR_INCORRECT_REQUEST;
            } finally {
                if (apiRequestHandler.startDbTransaction()) {
                    ValNetwork.getStores().endTransaction();
                }
            }

            if (response instanceof JsonObject) {
                JSON.getAsJsonObject(response).addProperty("requestProcessingTime", System.currentTimeMillis() - startTime);
            }

        } finally {
            if (resp.getContentType() == null || resp.getContentType().isEmpty()) {
                resp.setContentType("text/plain; charset=UTF-8");
                try (Writer writer = resp.getWriter()) {
                    JSON.writeTo(response, writer);
                }
            }
        }

    }

    abstract static class APIRequestHandler {

        private final List<String> parameters;
        private final Set<APITag> apiTags;

        APIRequestHandler(APITag[] apiTags, String... parameters) {
            this.parameters = Collections.unmodifiableList(Arrays.asList(parameters));
            this.apiTags = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(apiTags)));
        }

        final List<String> getParameters() {
            return parameters;
        }

        final Set<APITag> getAPITags() {
            return apiTags;
        }

        abstract JsonElement processRequest(HttpServletRequest request) throws ValException;

        final void validateRequest(HttpServletRequest req) throws ParameterException {
            if (acceptSurplusParams) {
                return;  // do not validate params if we're told to accept all that's comming our way
            }
            for (String parameter : req.getParameterMap().keySet()) {
                // _ is a parameter used in eg. jquery to avoid caching queries
                if (!this.parameters.contains(parameter) && !parameter.equals("_") && !parameter.equals("requestType"))
                    throw new ParameterException(JSONResponses.incorrectUnkown(parameter));
            }
        }

        boolean requirePost() {
            return false;
        }

        boolean startDbTransaction() {
            return false;
        }

    }

    abstract static class PrimitiveRequestHandler {

        protected abstract void processRequest(HttpServletRequest req, HttpServletResponse resp);

        void addErrorMessage(HttpServletResponse resp, JsonElement msg) throws IOException {
            try (Writer writer = resp.getWriter()) {
                resp.setContentType("text/plain; charset=UTF-8");
                resp.setStatus(500);
                JSON.writeTo(msg, writer);
            }
        }

    }

}
