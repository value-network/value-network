package val.db.sql;

import org.jooq.DSLContext;
import val.Asset;
import val.ValNetwork;
import val.db.DbIterator;
import val.db.DbKey;
import val.db.store.AssetStore;
import val.db.store.DerivedTableManager;
import val.schema.Tables;

import java.sql.ResultSet;
import java.sql.SQLException;

import static val.schema.tables.Asset.ASSET;

public class SqlAssetStore implements AssetStore {

    private final DbKey.LongKeyFactory<Asset> assetDbKeyFactory = new val.db.sql.DbKey.LongKeyFactory<Asset>("id") {

        @Override
        public DbKey newKey(Asset asset) {
            return asset.dbKey;
        }

    };
    private final EntitySqlTable<Asset> assetTable;

    public SqlAssetStore(DerivedTableManager derivedTableManager) {
        assetTable = new EntitySqlTable<Asset>("asset", Tables.ASSET, assetDbKeyFactory, derivedTableManager) {

            @Override
            protected Asset load(DSLContext ctx, ResultSet rs) throws SQLException {
                return new SqlAsset(rs);
            }

            @Override
            protected void save(DSLContext ctx, Asset asset) {
                saveAsset(ctx, asset);
            }
        };
    }

    private void saveAsset(DSLContext ctx, Asset asset) {
        ctx.insertInto(ASSET).
                set(ASSET.ID, asset.getId()).
                set(ASSET.ACCOUNT_ID, asset.getAccountId()).
                set(ASSET.NAME, asset.getName()).
                set(ASSET.DESCRIPTION, asset.getDescription()).
                set(ASSET.QUANTITY, asset.getQuantityQNT()).
                set(ASSET.DECIMALS, asset.getDecimals()).
                set(ASSET.HEIGHT, ValNetwork.getBlockchain().getHeight()).execute();
    }

    @Override
    public DbKey.LongKeyFactory<Asset> getAssetDbKeyFactory() {
        return assetDbKeyFactory;
    }

    @Override
    public EntitySqlTable<Asset> getAssetTable() {
        return assetTable;
    }

    @Override
    public DbIterator<Asset> getAssetsIssuedBy(long accountId, int from, int to) {
        return assetTable.getManyBy(ASSET.ACCOUNT_ID.eq(accountId), from, to);
    }

    private class SqlAsset extends Asset {

        private SqlAsset(ResultSet rs) throws SQLException {
            super(rs.getLong("id"),
                    assetDbKeyFactory.newKey(rs.getLong("id")),
                    rs.getLong("account_id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getLong("quantity"),
                    rs.getByte("decimals")
            );
        }
    }
}
