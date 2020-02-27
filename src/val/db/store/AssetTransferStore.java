package val.db.store;

import val.AssetTransfer;
import val.db.DbIterator;
import val.db.DbKey;
import val.db.sql.EntitySqlTable;

public interface AssetTransferStore {
    DbKey.LongKeyFactory<AssetTransfer> getTransferDbKeyFactory();

    EntitySqlTable<AssetTransfer> getAssetTransferTable();

    DbIterator<AssetTransfer> getAssetTransfers(long assetId, int from, int to);

    DbIterator<AssetTransfer> getAccountAssetTransfers(long accountId, int from, int to);

    DbIterator<AssetTransfer> getAccountAssetTransfers(long accountId, long assetId, int from, int to);

    int getTransferCount(long assetId);
}
