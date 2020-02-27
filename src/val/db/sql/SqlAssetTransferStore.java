package val.db.sql;

import org.jooq.DSLContext;
import org.jooq.SelectQuery;
import val.AssetTransfer;
import val.db.DbIterator;
import val.db.DbKey;
import val.db.store.AssetTransferStore;
import val.db.store.DerivedTableManager;
import val.schema.Tables;

import java.sql.ResultSet;
import java.sql.SQLException;

import static val.schema.Tables.ASSET_TRANSFER;

public class SqlAssetTransferStore implements AssetTransferStore {

    private static final DbKey.LongKeyFactory<AssetTransfer> transferDbKeyFactory = new val.db.sql.DbKey.LongKeyFactory<AssetTransfer>("id") {

        @Override
        public DbKey newKey(AssetTransfer assetTransfer) {
            return assetTransfer.dbKey;
        }
    };
    private final EntitySqlTable<AssetTransfer> assetTransferTable;

    public SqlAssetTransferStore(DerivedTableManager derivedTableManager) {
        assetTransferTable = new EntitySqlTable<AssetTransfer>("asset_transfer", Tables.ASSET_TRANSFER, transferDbKeyFactory, derivedTableManager) {

            @Override
            protected AssetTransfer load(DSLContext ctx, ResultSet rs) throws SQLException {
                return new SqlAssetTransfer(rs);
            }

            @Override
            protected void save(DSLContext ctx, AssetTransfer assetTransfer) {
                saveAssetTransfer(assetTransfer);
            }
        };
    }

    private void saveAssetTransfer(AssetTransfer assetTransfer) {
        try (DSLContext ctx = Db.getDSLContext()) {
            ctx.insertInto(
                    ASSET_TRANSFER,
                    ASSET_TRANSFER.ID, ASSET_TRANSFER.ASSET_ID, ASSET_TRANSFER.SENDER_ID, ASSET_TRANSFER.RECIPIENT_ID,
                    ASSET_TRANSFER.QUANTITY, ASSET_TRANSFER.TIMESTAMP, ASSET_TRANSFER.HEIGHT
            ).values(
                    assetTransfer.getId(), assetTransfer.getAssetId(), assetTransfer.getSenderId(), assetTransfer.getRecipientId(),
                    assetTransfer.getQuantityQNT(), assetTransfer.getTimestamp(), assetTransfer.getHeight()
            ).execute();
        }
    }


    @Override
    public EntitySqlTable<AssetTransfer> getAssetTransferTable() {
        return assetTransferTable;
    }

    @Override
    public DbKey.LongKeyFactory<AssetTransfer> getTransferDbKeyFactory() {
        return transferDbKeyFactory;
    }

    @Override
    public DbIterator<AssetTransfer> getAssetTransfers(long assetId, int from, int to) {
        return getAssetTransferTable().getManyBy(ASSET_TRANSFER.ASSET_ID.eq(assetId), from, to);
    }

    @Override
    public DbIterator<AssetTransfer> getAccountAssetTransfers(long accountId, int from, int to) {
        DSLContext ctx = Db.getDSLContext();

        SelectQuery selectQuery = ctx
                .selectFrom(ASSET_TRANSFER).where(
                        ASSET_TRANSFER.SENDER_ID.eq(accountId)
                )
                .unionAll(
                        ctx.selectFrom(ASSET_TRANSFER).where(
                                ASSET_TRANSFER.RECIPIENT_ID.eq(accountId).and(ASSET_TRANSFER.SENDER_ID.ne(accountId))
                        )
                )
                .orderBy(ASSET_TRANSFER.HEIGHT.desc())
                .getQuery();
        DbUtils.applyLimits(selectQuery, from, to);

        return getAssetTransferTable().getManyBy(ctx, selectQuery, false);
    }

    @Override
    public DbIterator<AssetTransfer> getAccountAssetTransfers(long accountId, long assetId, int from, int to) {
        DSLContext ctx = Db.getDSLContext();

        SelectQuery selectQuery = ctx
                .selectFrom(ASSET_TRANSFER).where(
                        ASSET_TRANSFER.SENDER_ID.eq(accountId).and(ASSET_TRANSFER.ASSET_ID.eq(assetId))
                )
                .unionAll(
                        ctx.selectFrom(ASSET_TRANSFER).where(
                                ASSET_TRANSFER.RECIPIENT_ID.eq(accountId)).and(
                                ASSET_TRANSFER.SENDER_ID.ne(accountId)
                        ).and(ASSET_TRANSFER.ASSET_ID.eq(assetId))
                )
                .orderBy(ASSET_TRANSFER.HEIGHT.desc())
                .getQuery();
        DbUtils.applyLimits(selectQuery, from, to);

        return getAssetTransferTable().getManyBy(ctx, selectQuery, false);
    }

    @Override
    public int getTransferCount(long assetId) {
        DSLContext ctx = Db.getDSLContext();
        return ctx.fetchCount(ctx.selectFrom(ASSET_TRANSFER).where(ASSET_TRANSFER.ASSET_ID.eq(assetId)));
    }

    class SqlAssetTransfer extends AssetTransfer {

        SqlAssetTransfer(ResultSet rs) throws SQLException {
            super(rs.getLong("id"),
                    transferDbKeyFactory.newKey(rs.getLong("id")),
                    rs.getLong("asset_id"),
                    rs.getInt("height"),
                    rs.getLong("sender_id"),
                    rs.getLong("recipient_id"),
                    rs.getLong("quantity"),
                    rs.getInt("timestamp")
            );
        }
    }


}
