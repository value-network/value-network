package val.db.store;

import org.jooq.DSLContext;
import val.Account;
import val.Block;
import val.Transaction;
import val.db.DbIterator;

import java.sql.ResultSet;
import java.util.List;

/**
 * Store for both BlockchainImpl and BlockchainProcessorImpl
 */

public interface BlockchainStore {


    DbIterator<Block> getBlocks(int from, int to);

    DbIterator<Block> getBlockLists(int page, int limit, int height, long blockID, long account, long pool);

    int getBlockCount(int height, long blockID, long account, long pool);

    DbIterator<Block> getBlocks(Account account, int timestamp, int from, int to);

    int getAccountBlockCount(long accountId, long poolId, int startTime, int endTime);

    int getPoolBlockCount(long accountId, int startTime, int endTime);

    int getAccountBlockCount(byte[] publicKey);

    long getLatestBlockReward(long accountId);

    DbIterator<Block> getBlocks(DSLContext ctx, ResultSet rs);

    List<Long> getBlockIdsAfter(long blockId, int limit);

    List<Block> getBlocksAfter(long blockId, int limit);

    int getTransactionCount();

    int getTransactionCount(long accountId, int countType);

    DbIterator<Transaction> getAllTransactions();

    DbIterator<Transaction> getTransactions(Account account, int numberOfConfirmations, byte type, byte subtype,
                                            int blockTimestamp, int from, int to);

    DbIterator<Transaction> getTransactionLists(Account account, byte type, byte subtype,
                                                long transactionId, String transactionFullHash, long blockID, int height, int page, int limit);

    int getTransactionCount(Account account, byte type, byte subtype,
                            long transactionId, String transactionFullHash, long blockID, int height);

    DbIterator<Transaction> getTransactions(DSLContext ctx, ResultSet rs);

    boolean addBlock(Block block);

    void scan(int height);

    DbIterator<Block> getLatestBlocks(int amountBlocks);

    int getBlockchainAddressCount();

    String getTransactionRate(int curTime, int dur);

    String getMiddlePayload(int curTime, int dur);

    String getTransactionRate();

    String getMiddlePayload();

    List<Long> getBlockByLikeId(String blockId);
}
