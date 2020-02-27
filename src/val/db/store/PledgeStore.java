package val.db.store;

import val.Account;
import val.db.DbIterator;
import val.db.VersionedEntityTable;
import val.db.sql.DbKey;

/**
 * Interface for Database operations related to Accounts
 */
public interface PledgeStore {

    VersionedEntityTable<Account.Pledges> getPledgesTable();

    DbKey.LongKeyFactory<Account.Pledges> getPledgeKeyFactory();

    Account.Pledges getAccountPledge(long accountId);

    long getMinerTotalReward(long accountId);

    Account.Pledges getAccountPledge(long accountId, int height);

    Account.Pledges getAccountPledge(long accountId, long recipientId);

    Account.Pledges getAccountPledgeOne(long accountId, long totalPledged);

    DbIterator<Account.Pledges> getPoolAllMinerPledge(long accountId);

    DbIterator<Account.Pledges> getPoolAllMinerPledge(long accountId, int page, int limit);

    int getPoolAllMinerPledgeCount(long accountId);

    DbIterator<Account.Pledges> getPledges(long accountId, int page, int limit);

    int getPledgesCount(long accountId);

    long getBlockchainPledged();

}
