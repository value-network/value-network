package val.db.store;

import val.Trade;
import val.db.DbIterator;
import val.db.DbKey;
import val.db.sql.EntitySqlTable;

public interface TradeStore {
    DbIterator<Trade> getAllTrades(int from, int to);

    DbIterator<Trade> getAssetTrades(long assetId, int from, int to);

    DbIterator<Trade> getAccountTrades(long accountId, int from, int to);

    DbIterator<Trade> getAccountAssetTrades(long accountId, long assetId, int from, int to);

    int getTradeCount(long assetId);

    DbKey.LinkKeyFactory<Trade> getTradeDbKeyFactory();

    EntitySqlTable<Trade> getTradeTable();
}
