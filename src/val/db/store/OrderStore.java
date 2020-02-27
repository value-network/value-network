package val.db.store;

import val.Order;
import val.db.DbIterator;
import val.db.DbKey;
import val.db.VersionedEntityTable;

public interface OrderStore {
    VersionedEntityTable<Order.Bid> getBidOrderTable();

    DbIterator<Order.Ask> getAskOrdersByAccountAsset(long accountId, long assetId, int from, int to);

    DbIterator<Order.Ask> getSortedAsks(long assetId, int from, int to);

    Order.Ask getNextOrder(long assetId);

    DbIterator<Order.Ask> getAll(int from, int to);

    DbIterator<Order.Ask> getAskOrdersByAccount(long accountId, int from, int to);

    DbIterator<Order.Ask> getAskOrdersByAsset(long assetId, int from, int to);

    DbKey.LongKeyFactory<Order.Ask> getAskOrderDbKeyFactory();

    VersionedEntityTable<Order.Ask> getAskOrderTable();

    DbKey.LongKeyFactory<Order.Bid> getBidOrderDbKeyFactory();

    DbIterator<Order.Bid> getBidOrdersByAccount(long accountId, int from, int to);

    DbIterator<Order.Bid> getBidOrdersByAsset(long assetId, int from, int to);

    DbIterator<Order.Bid> getBidOrdersByAccountAsset(long accountId, long assetId, int from, int to);

    DbIterator<Order.Bid> getSortedBids(long assetId, int from, int to);

    Order.Bid getNextBid(long assetId);
}
