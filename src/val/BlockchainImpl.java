package val;

import val.db.BlockDb;
import val.db.DbIterator;
import val.db.store.BlockchainStore;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.StampedLock;

public class BlockchainImpl implements Blockchain {

    private final TransactionDb transactionDb;
    private final BlockDb blockDb;
    private final BlockchainStore blockchainStore;

    private final StampedLock bcsl;
    private final AtomicReference<Block> lastBlock = new AtomicReference<>();

    BlockchainImpl(TransactionDb transactionDb, BlockDb blockDb, BlockchainStore blockchainStore) {
        this.transactionDb = transactionDb;
        this.blockDb = blockDb;
        this.blockchainStore = blockchainStore;
        this.bcsl = new StampedLock();
    }

    @Override
    public Block getLastBlock() {
        long stamp = bcsl.tryOptimisticRead();
        Block retBlock = lastBlock.get();
        if (!bcsl.validate(stamp)) {
            stamp = bcsl.readLock();
            try {
                retBlock = lastBlock.get();
            } finally {
                bcsl.unlockRead(stamp);
            }
        }
        return retBlock;
    }

    @Override
    public void setLastBlock(Block block) {
        long stamp = bcsl.writeLock();
        try {
            lastBlock.set(block);
        } finally {
            bcsl.unlockWrite(stamp);
        }
    }

    void setLastBlock(Block previousBlock, Block block) {
        long stamp = bcsl.writeLock();
        try {
            if (!lastBlock.compareAndSet(previousBlock, block)) {
                throw new IllegalStateException("Last block is no longer previous block");
            }
        } finally {
            bcsl.unlockWrite(stamp);
        }
    }

    @Override
    public int getHeight() {
        long stamp = bcsl.tryOptimisticRead();
        Block last = lastBlock.get();
        if (!bcsl.validate(stamp)) {
            stamp = bcsl.readLock();
            try {
                last = lastBlock.get();
            } finally {
                bcsl.unlockRead(stamp);
            }
        }
        return last == null ? 0 : last.getHeight();
    }

    @Override
    public Block getLastBlock(int timestamp) {
        Block block = getSafelastBlock();
        if (timestamp >= block.getTimestamp()) {
            return block;
        }
        return blockDb.findLastBlock(timestamp);
    }

    @Override
    public Block getBlock(long blockId) {
        Block block = getSafelastBlock();
        if (block.getId() == blockId) {
            return block;
        }
        return blockDb.findBlock(blockId);
    }

    private Block getSafelastBlock() {
        long stamp = bcsl.tryOptimisticRead();
        Block block = lastBlock.get();
        if (!bcsl.validate(stamp)) {
            stamp = bcsl.readLock();
            try {
                block = lastBlock.get();
            } finally {
                bcsl.unlockRead(stamp);
            }
        }
        return block;
    }

    @Override
    public boolean hasBlock(long blockId) {
        return getSafelastBlock().getId() == blockId || blockDb.hasBlock(blockId);
    }

    @Override
    public DbIterator<Block> getBlocks(int from, int to) {
        return blockchainStore.getBlocks(from, to);
    }

    @Override
    public DbIterator<Block> getBlockLists(int page, int limit, int height, long blockID, long account, long pool) {
        return blockchainStore.getBlockLists(page, limit, height, blockID, account, pool);
    }

    @Override
    public int getBlockCount(int height, long blockID, long account, long pool) {
        return blockchainStore.getBlockCount(height, blockID, account, pool);
    }

    @Override
    public DbIterator<Block> getBlocks(Account account, int timestamp) {
        return getBlocks(account, timestamp, 0, -1);
    }

    @Override
    public DbIterator<Block> getBlocks(Account account, int timestamp, int from, int to) {
        return blockchainStore.getBlocks(account, timestamp, from, to);
    }

    @Override
    public int getAccountBlockCount(long accountId, long poolId, int startTime, int endTime) {
        return blockchainStore.getAccountBlockCount(accountId, poolId, startTime, endTime);
    }

    @Override
    public int getAccountBlockCount(byte[] publicKey) {
        return blockchainStore.getAccountBlockCount(publicKey);
    }

    @Override
    public int getPoolBlockCount(long accountId, int startTime, int endTime) {
        return blockchainStore.getPoolBlockCount(accountId, startTime, endTime);
    }

    @Override
    public long getLatestBlockReward(long accountId) {
        return blockchainStore.getLatestBlockReward(accountId);
    }

    @Override
    public List<Long> getBlockIdsAfter(long blockId, int limit) {
        return blockchainStore.getBlockIdsAfter(blockId, limit);
    }

    @Override
    public List<Block> getBlocksAfter(long blockId, int limit) {
        return blockchainStore.getBlocksAfter(blockId, limit);
    }

    @Override
    public long getBlockIdAtHeight(int height) {
        Block block = getSafelastBlock();
        if (height > block.getHeight()) {
            throw new IllegalArgumentException("Invalid height " + height + ", current blockchain is at " + block.getHeight());
        }
        if (height == block.getHeight()) {
            return block.getId();
        }
        return blockDb.findBlockIdAtHeight(height);
    }

    @Override
    public Block getBlockAtHeight(int height) {
        Block block = getSafelastBlock();
        if (height > block.getHeight()) {
            throw new IllegalArgumentException("Invalid height " + height + ", current blockchain is at " + block.getHeight());
        }
        if (height == block.getHeight()) {
            return block;
        }
        return blockDb.findBlockAtHeight(height);
    }

    @Override
    public Transaction getTransaction(long transactionId) {
        return transactionDb.findTransaction(transactionId);
    }

    @Override
    public Transaction getTransactionByFullHash(String fullHash) {
        return transactionDb.findTransactionByFullHash(fullHash);
    }

    @Override
    public List<Transaction> getTransactionByLikeFullHash(String fullHash) {
        return transactionDb.findTransactionByLikeFullHash(fullHash);
    }

    @Override
    public boolean hasTransaction(long transactionId) {
        return transactionDb.hasTransaction(transactionId);
    }

    @Override
    public boolean hasTransactionByFullHash(String fullHash) {
        return transactionDb.hasTransactionByFullHash(fullHash);
    }

    @Override
    public int getTransactionCount() {
        return blockchainStore.getTransactionCount();
    }

    @Override
    public int getTransactionCount(long accountId, int countType) {
        return blockchainStore.getTransactionCount(accountId, countType);
    }

    @Override
    public DbIterator<Transaction> getAllTransactions() {
        return blockchainStore.getAllTransactions();
    }

    @Override
    public DbIterator<Transaction> getTransactions(Account account, byte type, byte subtype, int blockTimestamp) {
        return getTransactions(account, 0, type, subtype, blockTimestamp, 0, -1);
    }

    @Override
    public DbIterator<Transaction> getTransactions(Account account, int numberOfConfirmations, byte type, byte subtype,
                                                   int blockTimestamp, int from, int to) {
        return blockchainStore.getTransactions(account, numberOfConfirmations, type, subtype, blockTimestamp, from, to);
    }

    @Override
    public DbIterator<Transaction> getTransactionLists(Account account, byte type, byte subtype, long transactionId,
                                                       String transactionFullHash, long blockID, int height, int page, int limit) {

        return blockchainStore.getTransactionLists(account, type, subtype, transactionId, transactionFullHash, blockID, height, page, limit);
    }

    @Override
    public int getTransactionCount(Account account, byte type, byte subtype, long transactionId,
                                   String transactionFullHash, long blockID, int height) {
        return blockchainStore.getTransactionCount(account, type, subtype, transactionId, transactionFullHash, blockID, height);
    }

    @Override
    public int getBlockchainAddressCount() {
        return blockchainStore.getBlockchainAddressCount();
    }

    @Override
    public String getTransactionRate() {
        return blockchainStore.getTransactionRate();
    }

    @Override
    public String getMiddlePayload() {
        return blockchainStore.getMiddlePayload();
    }

    @Override
    public List<Long> getBlockByLikeId(String blockId) {
        return blockchainStore.getBlockByLikeId(blockId);
    }

    @Override
    public List<Long> getTransactionByLikeId(String transactionId) {
        return transactionDb.getTransactionByLikeId(transactionId);
    }
}
