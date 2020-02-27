package val.services.impl;

import val.Alias;
import val.Attachment;
import val.Transaction;
import val.db.DbIterator;
import val.db.DbKey;
import val.db.VersionedEntityTable;
import val.db.store.AliasStore;
import val.services.AliasService;

public class AliasServiceImpl implements AliasService {

    private final AliasStore aliasStore;
    private final VersionedEntityTable<Alias> aliasTable;
    private final DbKey.LongKeyFactory<Alias> aliasDbKeyFactory;
    private final VersionedEntityTable<Alias.Offer> offerTable;
    private final DbKey.LongKeyFactory<Alias.Offer> offerDbKeyFactory;

    public AliasServiceImpl(AliasStore aliasStore) {
        this.aliasStore = aliasStore;
        this.aliasTable = aliasStore.getAliasTable();
        this.aliasDbKeyFactory = aliasStore.getAliasDbKeyFactory();
        this.offerTable = aliasStore.getOfferTable();
        this.offerDbKeyFactory = aliasStore.getOfferDbKeyFactory();
    }

    public Alias getAlias(String aliasName) {
        return aliasStore.getAlias(aliasName);
    }

    public Alias getAlias(long id) {
        return aliasTable.get(aliasDbKeyFactory.newKey(id));
    }

    @Override
    public Alias.Offer getOffer(Alias alias) {
        return offerTable.get(offerDbKeyFactory.newKey(alias.getId()));
    }

    @Override
    public long getAliasCount() {
        return aliasTable.getCount();
    }

    @Override
    public DbIterator<Alias> getAliasesByOwner(long accountId, int from, int to) {
        return aliasStore.getAliasesByOwner(accountId, from, to);
    }

    @Override
    public void addOrUpdateAlias(Transaction transaction, Attachment.MessagingAliasAssignment attachment) {
        Alias alias = getAlias(attachment.getAliasName());
        if (alias == null) {
            DbKey aliasDBId = aliasDbKeyFactory.newKey(transaction.getId());
            alias = new Alias(transaction.getId(), aliasDBId, transaction, attachment);
        } else {
            alias.setAccountId(transaction.getSenderId());
            alias.setAliasURI(attachment.getAliasURI());
            alias.setTimestamp(transaction.getBlockTimestamp());
        }
        aliasTable.insert(alias);
    }

    @Override
    public void sellAlias(Transaction transaction, Attachment.MessagingAliasSell attachment) {
        final String aliasName = attachment.getAliasName();
        final long priceNQT = attachment.getPriceNQT();
        final long buyerId = transaction.getRecipientId();
        if (priceNQT > 0) {
            Alias alias = getAlias(aliasName);
            Alias.Offer offer = getOffer(alias);
            if (offer == null) {
                DbKey dbKey = offerDbKeyFactory.newKey(alias.getId());
                offerTable.insert(new Alias.Offer(dbKey, alias.getId(), priceNQT, buyerId));
            } else {
                offer.setPriceNQT(priceNQT);
                offer.setBuyerId(buyerId);
                offerTable.insert(offer);
            }
        } else {
            changeOwner(buyerId, aliasName, transaction.getBlockTimestamp());
        }
    }

    @Override
    public void changeOwner(long newOwnerId, String aliasName, int timestamp) {
        final Alias alias = getAlias(aliasName);
        alias.setAccountId(newOwnerId);
        alias.setTimestamp(timestamp);
        aliasTable.insert(alias);

        final Alias.Offer offer = getOffer(alias);
        offerTable.delete(offer);
    }
}
