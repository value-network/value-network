package val.assetexchange;

import val.*;
import val.db.DbIterator;
import val.util.Listener;

public interface AssetExchange {

    DbIterator<Asset> getAllAssets(int from, int to);

    Asset getAsset(long assetId);

    int getTradeCount(long assetId);

    int getTransferCount(long id);

    int getAssetAccountsCount(long id);

    void addTradeListener(Listener<Trade> listener, Trade.Event trade);

    Order.Ask getAskOrder(long orderId);

    void addAsset(Transaction transaction, Attachment.ColoredCoinsAssetIssuance attachment);

    void addAssetTransfer(Transaction transaction, Attachment.ColoredCoinsAssetTransfer attachment);

    void addAskOrder(Transaction transaction, Attachment.ColoredCoinsAskOrderPlacement attachment);

    void addBidOrder(Transaction transaction, Attachment.ColoredCoinsBidOrderPlacement attachment);

    void removeAskOrder(long orderId);

    Order.Bid getBidOrder(long orderId);

    void removeBidOrder(long orderId);

    DbIterator<Trade> getAllTrades(int i, int i1);

    DbIterator<Trade> getTrades(long assetId, int from, int to);

    DbIterator<Trade> getAccountTrades(long accountId, int from, int to);

    DbIterator<Trade> getAccountAssetTrades(long accountId, long assetId, int from, int to);

    DbIterator<Account.AccountAsset> getAccountAssetsOverview(long accountId, int height, int from, int to);

    DbIterator<Asset> getAssetsIssuedBy(long accountId, int from, int to);

    DbIterator<AssetTransfer> getAssetTransfers(long assetId, int from, int to);

    DbIterator<AssetTransfer> getAccountAssetTransfers(long id, long id1, int from, int to);

    int getAssetsCount();

    int getAskCount();

    int getBidCount();

    int getTradesCount();

    int getAssetTransferCount();

    DbIterator<Order.Ask> getAskOrdersByAccount(long accountId, int from, int to);

    DbIterator<Order.Ask> getAskOrdersByAccountAsset(long accountId, long assetId, int from, int to);

    DbIterator<Order.Bid> getBidOrdersByAccount(long accountId, int from, int to);

    DbIterator<Order.Bid> getBidOrdersByAccountAsset(long accountId, long assetId, int from, int to);

    DbIterator<Order.Ask> getAllAskOrders(int from, int to);

    DbIterator<Order.Bid> getAllBidOrders(int from, int to);

    DbIterator<Order.Ask> getSortedAskOrders(long assetId, int from, int to);

    DbIterator<Order.Bid> getSortedBidOrders(long assetId, int from, int to);

}
