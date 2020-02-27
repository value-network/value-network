package val.db.store;

import val.Alias;
import val.db.DbIterator;
import val.db.DbKey;
import val.db.VersionedEntityTable;

public interface AliasStore {
    DbKey.LongKeyFactory<Alias> getAliasDbKeyFactory();

    DbKey.LongKeyFactory<Alias.Offer> getOfferDbKeyFactory();

    VersionedEntityTable<Alias> getAliasTable();

    VersionedEntityTable<Alias.Offer> getOfferTable();

    DbIterator<Alias> getAliasesByOwner(long accountId, int from, int to);

    Alias getAlias(String aliasName);
}
