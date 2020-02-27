package val.db.store;

import val.Asset;
import val.db.DbIterator;
import val.db.DbKey;
import val.db.sql.EntitySqlTable;

public interface AssetStore {
    DbKey.LongKeyFactory<Asset> getAssetDbKeyFactory();

    EntitySqlTable<Asset> getAssetTable();

    DbIterator<Asset> getAssetsIssuedBy(long accountId, int from, int to);
}
