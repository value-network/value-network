package val.db.store;

import val.Subscription;
import val.db.DbIterator;
import val.db.DbKey;
import val.db.VersionedEntityTable;

public interface SubscriptionStore {

    DbKey.LongKeyFactory<Subscription> getSubscriptionDbKeyFactory();

    VersionedEntityTable<Subscription> getSubscriptionTable();

    DbIterator<Subscription> getSubscriptionsByParticipant(Long accountId);

    DbIterator<Subscription> getIdSubscriptions(Long accountId);

    DbIterator<Subscription> getSubscriptionsToId(Long accountId);

    DbIterator<Subscription> getUpdateSubscriptions(int timestamp);
}
