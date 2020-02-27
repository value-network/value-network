package val.db.store;

import org.jooq.Record6;
import org.jooq.Result;
import val.Account;

/**
 * Interface for Database operations related to Accounts
 */
public interface PoolMinerStore {

    //VersionedEntityTable<Account.PoolMiner> getPoolMinerTable();

    //DbKey.LongKeyFactory<Account.PoolMiner> getPoolMinerKeyFactory();

    Result<Record6<Integer, Long, Long, Integer, Long, Long>> getGrantPoolMiners(long poolId, int page, int limit);

    int getGrantPoolMinerCount(long poolId);

    void savePoolMiner(Account.PoolMiner poolMiner);

    void revokePoolMiner(long accountId, long poolId, int status, long mTime);

    Account.PoolMiner getGrantPoolMiner(long accountId, long poolId);


}
