package val.db.sql;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.SelectQuery;
import org.jooq.SortField;
import org.jooq.impl.TableImpl;
import val.Order;
import val.ValNetwork;
import val.db.DbIterator;
import val.db.DbKey;
import val.db.VersionedEntityTable;
import val.db.store.DerivedTableManager;
import val.db.store.OrderStore;
import val.schema.Tables;
import val.schema.tables.records.AskOrderRecord;
import val.schema.tables.records.BidOrderRecord;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SqlOrderStore implements OrderStore {
    private final val.db.sql.DbKey.LongKeyFactory<Order.Ask> askOrderDbKeyFactory = new val.db.sql.DbKey.LongKeyFactory<Order.Ask>("id") {

        @Override
        public DbKey newKey(Order.Ask ask) {
            return ask.dbKey;
        }

    };
    private final VersionedEntityTable<Order.Ask> askOrderTable;
    private final val.db.sql.DbKey.LongKeyFactory<Order.Bid> bidOrderDbKeyFactory = new val.db.sql.DbKey.LongKeyFactory<Order.Bid>("id") {

        @Override
        public DbKey newKey(Order.Bid bid) {
            return bid.dbKey;
        }

    };
    private final VersionedEntityTable<Order.Bid> bidOrderTable;
    public SqlOrderStore(DerivedTableManager derivedTableManager) {
        askOrderTable = new VersionedEntitySqlTable<Order.Ask>("ask_order", Tables.ASK_ORDER, askOrderDbKeyFactory, derivedTableManager) {
            @Override
            protected Order.Ask load(DSLContext ctx, ResultSet rs) throws SQLException {
                return new SqlAsk(rs);
            }

            @Override
            protected void save(DSLContext ctx, Order.Ask ask) {
                saveAsk(ctx, Tables.ASK_ORDER, ask);
            }

            @Override
            protected List<SortField> defaultSort() {
                List<SortField> sort = new ArrayList<>();
                sort.add(tableClass.field("creation_height", Integer.class).desc());
                return sort;
            }
        };

        bidOrderTable = new VersionedEntitySqlTable<Order.Bid>("bid_order", Tables.BID_ORDER, bidOrderDbKeyFactory, derivedTableManager) {

            @Override
            protected Order.Bid load(DSLContext ctx, ResultSet rs) throws SQLException {
                return new SqlBid(rs);
            }

            @Override
            protected void save(DSLContext ctx, Order.Bid bid) {
                saveBid(ctx, Tables.BID_ORDER, bid);
            }

            @Override
            protected List<SortField> defaultSort() {
                List<SortField> sort = new ArrayList<>();
                sort.add(tableClass.field("creation_height", Integer.class).desc());
                return sort;
            }

        };

    }

    @Override
    public VersionedEntityTable<Order.Bid> getBidOrderTable() {
        return bidOrderTable;
    }

    @Override
    public DbIterator<Order.Ask> getAskOrdersByAccountAsset(final long accountId, final long assetId, int from, int to) {
        return askOrderTable.getManyBy(
                Tables.ASK_ORDER.ACCOUNT_ID.eq(accountId).and(
                        Tables.ASK_ORDER.ASSET_ID.eq(assetId)
                ),
                from,
                to
        );
    }

    @Override
    public DbIterator<Order.Ask> getSortedAsks(long assetId, int from, int to) {
        List<SortField> sort = new ArrayList<>();
        sort.add(Tables.ASK_ORDER.field("price", Long.class).asc());
        sort.add(Tables.ASK_ORDER.field("creation_height", Integer.class).asc());
        sort.add(Tables.ASK_ORDER.field("id", Long.class).asc());
        return askOrderTable.getManyBy(Tables.ASK_ORDER.ASSET_ID.eq(assetId), from, to, sort);
    }

    @Override
    public Order.Ask getNextOrder(long assetId) {
        DSLContext ctx = Db.getDSLContext();
        SelectQuery query = ctx.selectFrom(Tables.ASK_ORDER).where(
                Tables.ASK_ORDER.ASSET_ID.eq(assetId).and(Tables.ASK_ORDER.LATEST.isTrue())
        ).orderBy(
                Tables.ASK_ORDER.PRICE.asc(),
                Tables.ASK_ORDER.CREATION_HEIGHT.asc(),
                Tables.ASK_ORDER.ID.asc()
        ).limit(1).getQuery();
        try (DbIterator<Order.Ask> askOrders = askOrderTable.getManyBy(ctx, query, true)) {
            return askOrders.hasNext() ? askOrders.next() : null;
        }
    }

    @Override
    public DbIterator<Order.Ask> getAll(int from, int to) {
        return askOrderTable.getAll(from, to);
    }

    @Override
    public DbIterator<Order.Ask> getAskOrdersByAccount(long accountId, int from, int to) {
        return askOrderTable.getManyBy(Tables.ASK_ORDER.ACCOUNT_ID.eq(accountId), from, to);
    }

    @Override
    public DbIterator<Order.Ask> getAskOrdersByAsset(long assetId, int from, int to) {
        return askOrderTable.getManyBy(Tables.ASK_ORDER.ASSET_ID.eq(assetId), from, to);
    }

    private void saveAsk(DSLContext ctx, TableImpl table, Order.Ask ask) {
        AskOrderRecord askOrderRecord = ctx.newRecord(Tables.ASK_ORDER);
        askOrderRecord.setId(ask.getId());
        askOrderRecord.setAccountId(ask.getAccountId());
        askOrderRecord.setAssetId(ask.getAssetId());
        askOrderRecord.setPrice(ask.getPriceNQT());
        askOrderRecord.setQuantity(ask.getQuantityQNT());
        askOrderRecord.setCreationHeight(ask.getHeight());
        askOrderRecord.setHeight(ValNetwork.getBlockchain().getHeight());
        askOrderRecord.setLatest(true);
        DbUtils.mergeInto(
                ctx, askOrderRecord, table,
                (new Field[]{askOrderRecord.field("id"), askOrderRecord.field("height")})
        );
    }

    @Override
    public val.db.sql.DbKey.LongKeyFactory<Order.Ask> getAskOrderDbKeyFactory() {
        return askOrderDbKeyFactory;
    }

    @Override
    public VersionedEntityTable<Order.Ask> getAskOrderTable() {
        return askOrderTable;
    }

    @Override
    public val.db.sql.DbKey.LongKeyFactory<Order.Bid> getBidOrderDbKeyFactory() {
        return bidOrderDbKeyFactory;
    }

    @Override
    public DbIterator<Order.Bid> getBidOrdersByAccount(long accountId, int from, int to) {
        return bidOrderTable.getManyBy(Tables.BID_ORDER.ACCOUNT_ID.eq(accountId), from, to);
    }

    @Override
    public DbIterator<Order.Bid> getBidOrdersByAsset(long assetId, int from, int to) {
        return bidOrderTable.getManyBy(Tables.BID_ORDER.ASSET_ID.eq(assetId), from, to);
    }

    @Override
    public DbIterator<Order.Bid> getBidOrdersByAccountAsset(final long accountId, final long assetId, int from, int to) {
        return bidOrderTable.getManyBy(
                Tables.BID_ORDER.ACCOUNT_ID.eq(accountId).and(
                        Tables.BID_ORDER.ASSET_ID.eq(assetId)
                ),
                from,
                to
        );
    }

    @Override
    public DbIterator<Order.Bid> getSortedBids(long assetId, int from, int to) {
        List<SortField> sort = new ArrayList<>();
        sort.add(Tables.BID_ORDER.field("price", Long.class).desc());
        sort.add(Tables.BID_ORDER.field("creation_height", Integer.class).asc());
        sort.add(Tables.BID_ORDER.field("id", Long.class).asc());
        return bidOrderTable.getManyBy(Tables.BID_ORDER.ASSET_ID.eq(assetId), from, to, sort);
    }

    @Override
    public Order.Bid getNextBid(long assetId) {
        DSLContext ctx = Db.getDSLContext();
        SelectQuery query = ctx.selectFrom(Tables.BID_ORDER).where(
                Tables.BID_ORDER.ASSET_ID.eq(assetId).and(Tables.BID_ORDER.LATEST.isTrue())
        ).orderBy(
                Tables.BID_ORDER.PRICE.desc(),
                Tables.BID_ORDER.CREATION_HEIGHT.asc(),
                Tables.BID_ORDER.ID.asc()
        ).limit(1).getQuery();
        try (DbIterator<Order.Bid> bidOrders = bidOrderTable.getManyBy(ctx, query, true)) {
            return bidOrders.hasNext() ? bidOrders.next() : null;
        }
    }

    private void saveBid(DSLContext ctx, TableImpl table, Order.Bid bid) {
        BidOrderRecord bidOrderRecord = ctx.newRecord(Tables.BID_ORDER);
        bidOrderRecord.setId(bid.getId());
        bidOrderRecord.setAccountId(bid.getAccountId());
        bidOrderRecord.setAssetId(bid.getAssetId());
        bidOrderRecord.setPrice(bid.getPriceNQT());
        bidOrderRecord.setQuantity(bid.getQuantityQNT());
        bidOrderRecord.setCreationHeight(bid.getHeight());
        bidOrderRecord.setHeight(ValNetwork.getBlockchain().getHeight());
        bidOrderRecord.setLatest(true);
        DbUtils.mergeInto(
                ctx, bidOrderRecord, table,
                (new Field[]{bidOrderRecord.field("id"), bidOrderRecord.field("height")})
        );
    }

    class SqlAsk extends Order.Ask {
        private SqlAsk(ResultSet rs) throws SQLException {
            super(
                    rs.getLong("id"),
                    rs.getLong("account_id"),
                    rs.getLong("asset_id"),
                    rs.getLong("price"),
                    rs.getInt("creation_height"),
                    rs.getLong("quantity"),
                    askOrderDbKeyFactory.newKey(rs.getLong("id"))
            );
        }
    }

    class SqlBid extends Order.Bid {
        private SqlBid(ResultSet rs) throws SQLException {
            super(
                    rs.getLong("id"),
                    rs.getLong("account_id"),
                    rs.getLong("asset_id"),
                    rs.getLong("price"),
                    rs.getInt("creation_height"),
                    rs.getLong("quantity"),
                    bidOrderDbKeyFactory.newKey(rs.getLong("id"))
            );
        }


    }

}
