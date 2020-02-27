package val;

import val.db.DbIterator;

import java.util.List;

public interface Blockchain {

    Block getLastBlock();

    void setLastBlock(Block blockImpl);

    Block getLastBlock(int timestamp);

    int getHeight();

    Block getBlock(long blockImplId);

    Block getBlockAtHeight(int height);

    boolean hasBlock(long blockImplId);

    DbIterator<Block> getBlocks(int from, int to);

    DbIterator<Block> getBlockLists(int page, int limit, int height, long blockID, long account, long pool);

    int getBlockCount(int height, long blockID, long account, long pool);

    DbIterator<Block> getBlocks(Account account, int timestamp);

    DbIterator<Block> getBlocks(Account account, int timestamp, int from, int to);

    int getAccountBlockCount(long accountId, long poolId, int startTime, int endTime);

    int getAccountBlockCount(byte[] publicKey);

    int getPoolBlockCount(long accountId, int startTime, int endTime);

    long getLatestBlockReward(long accountId);

    int getBlockchainAddressCount();

    List<Long> getBlockIdsAfter(long blockImplId, int limit);

    List<? extends Block> getBlocksAfter(long blockImplId, int limit);

    long getBlockIdAtHeight(int height);

    Transaction getTransaction(long transactionId);

    Transaction getTransactionByFullHash(String fullHash); // TODO add byte[] method

    boolean hasTransaction(long transactionId);

    boolean hasTransactionByFullHash(String fullHash); // TODO add byte[] method

    int getTransactionCount();

    int getTransactionCount(long accountId, int countType);

    DbIterator<Transaction> getAllTransactions();

    DbIterator<Transaction> getTransactions(Account account, byte type, byte subtype, int blockImplTimestamp);

    DbIterator<Transaction> getTransactions(Account account, int numberOfConfirmations, byte type, byte subtype, int blockImplTimestamp, int from, int to);

    DbIterator<Transaction> getTransactionLists(Account account, byte type, byte subtype, long transactionId, String transactionFullHash, long blockID, int height, int page, int limit);

    int getTransactionCount(Account account, byte type, byte subtype, long transactionId, String transactionFullHash, long blockID, int height);

    String getTransactionRate();

    String getMiddlePayload();

    List<Transaction> getTransactionByLikeFullHash(String fullHash);

    List<Long> getBlockByLikeId(String blockId);

    List<Long> getTransactionByLikeId(String transactionId);
}
