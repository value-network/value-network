package val.db.sql;

import org.jooq.DSLContext;
import org.jooq.SortField;
import val.Alias;
import val.ValNetwork;
import val.db.DbIterator;
import val.db.DbKey;
import val.db.VersionedEntityTable;
import val.db.store.AliasStore;
import val.db.store.DerivedTableManager;
import val.schema.Tables;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static val.schema.Tables.ALIAS;
import static val.schema.Tables.ALIAS_OFFER;

public class SqlAliasStore implements AliasStore {

    private static final val.db.sql.DbKey.LongKeyFactory<Alias.Offer> offerDbKeyFactory = new val.db.sql.DbKey.LongKeyFactory<Alias.Offer>("id") {
        @Override
        public DbKey newKey(Alias.Offer offer) {
            return offer.dbKey;
        }
    };
    private static final DbKey.LongKeyFactory<Alias> aliasDbKeyFactory = new val.db.sql.DbKey.LongKeyFactory<Alias>("id") {

        @Override
        public DbKey newKey(Alias alias) {
            return alias.dbKey;
        }
    };
    private final VersionedEntityTable<Alias.Offer> offerTable;
    private final VersionedEntityTable<Alias> aliasTable;

    public SqlAliasStore(DerivedTableManager derivedTableManager) {
        offerTable = new VersionedEntitySqlTable<Alias.Offer>("alias_offer", ALIAS_OFFER, offerDbKeyFactory, derivedTableManager) {
            @Override
            protected Alias.Offer load(DSLContext ctx, ResultSet rs) throws SQLException {
                return new SqlOffer(rs);
            }

            @Override
            protected void save(DSLContext ctx, Alias.Offer offer) {
                saveOffer(offer);
            }
        };

        aliasTable = new VersionedEntitySqlTable<Alias>("alias", Tables.ALIAS, aliasDbKeyFactory, derivedTableManager) {
            @Override
            protected Alias load(DSLContext ctx, ResultSet rs) throws SQLException {
                return new SqlAlias(rs);
            }

            @Override
            protected void save(DSLContext ctx, Alias alias) {
                saveAlias(ctx, alias);
            }

            @Override
            protected List<SortField> defaultSort() {
                List<SortField> sort = new ArrayList<>();
                sort.add(tableClass.field("alias_name_lower", String.class).asc());
                return sort;
            }
        };
    }

    @Override
    public DbKey.LongKeyFactory<Alias.Offer> getOfferDbKeyFactory() {
        return offerDbKeyFactory;
    }

    @Override
    public DbKey.LongKeyFactory<Alias> getAliasDbKeyFactory() {
        return aliasDbKeyFactory;
    }

    @Override
    public VersionedEntityTable<Alias> getAliasTable() {
        return aliasTable;
    }

    private void saveOffer(Alias.Offer offer) {
        try (DSLContext ctx = Db.getDSLContext()) {
            ctx.insertInto(
                    ALIAS_OFFER,
                    ALIAS_OFFER.ID, ALIAS_OFFER.PRICE, ALIAS_OFFER.BUYER_ID, ALIAS_OFFER.HEIGHT
            ).values(
                    offer.getId(), offer.getPriceNQT(), (offer.getBuyerId() == 0 ? null : offer.getBuyerId()), ValNetwork.getBlockchain().getHeight()
            ).execute();
        }
    }

    @Override
    public VersionedEntityTable<Alias.Offer> getOfferTable() {
        return offerTable;
    }

    private void saveAlias(DSLContext ctx, Alias alias) {
        ctx.insertInto(ALIAS).
                set(ALIAS.ID, alias.getId()).
                set(ALIAS.ACCOUNT_ID, alias.getAccountId()).
                set(ALIAS.ALIAS_NAME, alias.getAliasName()).
                set(ALIAS.ALIAS_NAME_LOWER, alias.getAliasName().toLowerCase(Locale.ENGLISH)).
                set(ALIAS.ALIAS_URI, alias.getAliasURI()).
                set(ALIAS.TIMESTAMP, alias.getTimestamp()).
                set(ALIAS.HEIGHT, ValNetwork.getBlockchain().getHeight()).execute();
    }

    @Override
    public DbIterator<Alias> getAliasesByOwner(long accountId, int from, int to) {
        return aliasTable.getManyBy(Tables.ALIAS.ACCOUNT_ID.eq(accountId), from, to);
    }

    @Override
    public Alias getAlias(String aliasName) {
        return aliasTable.getBy(Tables.ALIAS.ALIAS_NAME_LOWER.eq(aliasName.toLowerCase(Locale.ENGLISH)));
    }

    private class SqlOffer extends Alias.Offer {
        private SqlOffer(ResultSet rs) throws SQLException {
            super(rs.getLong("id"), rs.getLong("price"), rs.getLong("buyer_id"), offerDbKeyFactory.newKey(rs.getLong("id")));
        }
    }

    private class SqlAlias extends Alias {
        private SqlAlias(ResultSet rs) throws SQLException {
            super(
                    rs.getLong("id"),
                    rs.getLong("account_id"),
                    rs.getString("alias_name"),
                    rs.getString("alias_uri"),
                    rs.getInt("timestamp"),
                    aliasDbKeyFactory.newKey(rs.getLong("id"))
            );
        }
    }

}
