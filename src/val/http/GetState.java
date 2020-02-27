package val.http;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import val.*;
import val.assetexchange.AssetExchange;
import val.db.DbIterator;
import val.peer.Peer;
import val.peer.Peers;
import val.services.AccountService;
import val.services.AliasService;
import val.services.EscrowService;
import val.services.TimeService;

import javax.servlet.http.HttpServletRequest;

import static val.http.common.Parameters.INCLUDE_COUNTS_PARAMETER;
import static val.http.common.ResultFields.TIME_RESPONSE;

final class GetState extends APIServlet.APIRequestHandler {

    private final Blockchain blockchain;
    private final AssetExchange assetExchange;
    private final AccountService accountService;
    private final EscrowService escrowService;
    private final AliasService aliasService;
    private final TimeService timeService;
    private final Generator generator;

    GetState(Blockchain blockchain, AssetExchange assetExchange, AccountService accountService, EscrowService escrowService,
             AliasService aliasService, TimeService timeService, Generator generator) {
        super(new APITag[]{APITag.INFO}, INCLUDE_COUNTS_PARAMETER);
        this.blockchain = blockchain;
        this.assetExchange = assetExchange;
        this.accountService = accountService;
        this.escrowService = escrowService;
        this.aliasService = aliasService;
        this.timeService = timeService;
        this.generator = generator;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) {

        JsonObject response = new JsonObject();

        response.addProperty("application", ValNetwork.APPLICATION);
        response.addProperty("version", ValNetwork.VERSION.toString());
        response.addProperty(TIME_RESPONSE, timeService.getEpochTime());
        response.addProperty("lastBlock", blockchain.getLastBlock().getStringId());
        response.addProperty("cumulativeDifficulty", blockchain.getLastBlock().getCumulativeDifficulty().toString());


        long totalEffectiveBalance = 0;
        try (DbIterator<Account> accounts = accountService.getAllAccounts(0, -1)) {
            while (accounts.hasNext()) {
                long effectiveBalanceBURST = accounts.next().getBalanceNQT();
                if (effectiveBalanceBURST > 0) {
                    totalEffectiveBalance += effectiveBalanceBURST;
                }
            }
        }
        try (DbIterator<Escrow> escrows = escrowService.getAllEscrowTransactions()) {
            while (escrows.hasNext()) {
                totalEffectiveBalance += escrows.next().getAmountNQT();
            }
        }
        response.addProperty("totalEffectiveBalanceNXT", totalEffectiveBalance / Constants.ONE_COIN);


        if (!"false".equalsIgnoreCase(req.getParameter("includeCounts"))) {
            response.addProperty("numberOfBlocks", blockchain.getHeight() + 1);
            response.addProperty("numberOfTransactions", blockchain.getTransactionCount());
            response.addProperty("numberOfAccounts", accountService.getCount());
            response.addProperty("numberOfAssets", assetExchange.getAssetsCount());
            int askCount = assetExchange.getAskCount();
            int bidCount = assetExchange.getBidCount();
            response.addProperty("numberOfOrders", askCount + bidCount);
            response.addProperty("numberOfAskOrders", askCount);
            response.addProperty("numberOfBidOrders", bidCount);
            response.addProperty("numberOfTrades", assetExchange.getTradesCount());
            response.addProperty("numberOfTransfers", assetExchange.getAssetTransferCount());
            response.addProperty("numberOfAliases", aliasService.getAliasCount());
            //response.addProperty("numberOfPolls", Poll.getCount());
            //response.addProperty("numberOfVotes", Vote.getCount());
        }
        response.addProperty("numberOfPeers", Peers.getAllPeers().size());
        response.addProperty("numberOfUnlockedAccounts", generator.getAllGenerators().size());
        Peer lastBlockchainFeeder = ValNetwork.getBlockchainProcessor().getLastBlockchainFeeder();
        response.addProperty("lastBlockchainFeeder", lastBlockchainFeeder == null ? null : lastBlockchainFeeder.getAnnouncedAddress());
        response.addProperty("lastBlockchainFeederHeight", ValNetwork.getBlockchainProcessor().getLastBlockchainFeederHeight());
        response.addProperty("isScanning", ValNetwork.getBlockchainProcessor().isScanning());
        response.addProperty("availableProcessors", Runtime.getRuntime().availableProcessors());
        response.addProperty("maxMemory", Runtime.getRuntime().maxMemory());
        response.addProperty("totalMemory", Runtime.getRuntime().totalMemory());
        response.addProperty("freeMemory", Runtime.getRuntime().freeMemory());

        return response;
    }

}
