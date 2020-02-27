package val.db.sql;

import org.jooq.*;
import val.Constants;
import val.Transaction;
import val.*;
import val.db.BlockDb;
import val.db.DbIterator;
import val.db.store.BlockchainStore;
import val.schema.tables.records.BlockRecord;
import val.services.TimeService;
import val.util.Convert;

import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static val.schema.Tables.*;

public class SqlBlockchainStore implements BlockchainStore {

    private final TransactionDb transactionDb = ValNetwork.getDbs().getTransactionDb();
    private final BlockDb blockDb = ValNetwork.getDbs().getBlockDb();
    private final HashMap<String, String> internalStore;

    public SqlBlockchainStore(TimeService timeService) {

        internalStore = new HashMap<>();
        // System.out.printf("start get cycle data\n");
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        Runnable getTnxRateAndPayload = () -> {
            synchronized (internalStore) {
                try {
                    int curTime = timeService.getEpochTime();
                    String transactionRate = getTransactionRate(curTime, Constants.TRANSACTION_RATE_CYCLE);
                    String middlePayload = getMiddlePayload(curTime, Constants.MIDDLE_PAYLOAD_CYCLE);
                    System.out.printf("get cycle data: ts:[%s], transactionRate:[%s], middlePayload:[%s]\n", curTime, transactionRate, middlePayload);
                    internalStore.put(Constants.MIDDLE_PAYLOAD, middlePayload);
                    internalStore.put(Constants.TRANSACTION_RATE, transactionRate);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        scheduler.scheduleWithFixedDelay(getTnxRateAndPayload, 0, 4, TimeUnit.MINUTES);
    }

    @Override
    public String getTransactionRate() {
        synchronized (internalStore) {
            return internalStore.getOrDefault(Constants.TRANSACTION_RATE, "0");
        }
    }

    @Override
    public String getMiddlePayload() {
        synchronized (internalStore) {
            return internalStore.getOrDefault(Constants.MIDDLE_PAYLOAD, "0");
        }
    }

    @Override
    public List<Long> getBlockByLikeId(String blockId) {
        List<Long> allId = new ArrayList();
        try (DSLContext ctx = Db.getDSLContext()) {
            //System.out.printf("getblockbylikeid:%s\n", blockId);
            Result<Record> results = ctx
                    .fetch("select id from block where cast(id as unsigned) like '" + blockId + "%'");
            for (Record record : results) {
                Long block = (Long) record.getValue("id");
                if (block != 0) {
                    allId.add(block);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return allId;

    }

    @Override
    public DbIterator<Block> getBlocks(int from, int to) {
        try (DSLContext ctx = Db.getDSLContext()) {
            int blockchainHeight = ValNetwork.getBlockchain().getHeight();
            return
                    getBlocks(
                            ctx,
                            ctx.selectFrom(BLOCK).where(
                                    BLOCK.HEIGHT.between(to > 0 ? blockchainHeight - to : 0).and(blockchainHeight - Math.max(from, 0))
                            ).orderBy(BLOCK.HEIGHT.desc()).fetchResultSet()
                    );
        } catch (Exception e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public DbIterator<Block> getBlockLists(int page, int limit, int height, long blockID, long account, long pool) {
        try (DSLContext ctx = Db.getDSLContext()) {
            ArrayList<Condition> conditions = new ArrayList<>();
            if (height >= 0 && height < Integer.MAX_VALUE) {
                conditions.add(BLOCK.HEIGHT.eq(height));
            }
            if (blockID != 0) {
                conditions.add(BLOCK.ID.eq(blockID));
            }
            if (account != 0) {
                conditions.add(BLOCK.GENERATOR_ID.eq(account));
            }
            if (pool != 0) {
                conditions.add(BLOCK.POOL_ID.eq(pool));
            }
            SelectQuery selectQuery = ctx.selectFrom(BLOCK).where(conditions).orderBy(BLOCK.HEIGHT.desc()).getQuery();
            DbUtils.applyPages(selectQuery, page, limit);

            return getBlocks(ctx, selectQuery.fetchResultSet());
        } catch (Exception e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public int getBlockCount(int height, long blockID, long account, long pool) {
        DSLContext ctx = Db.getDSLContext();
        ArrayList<Condition> conditions = new ArrayList<>();
        if (height >= 0 && height < Integer.MAX_VALUE) {
            conditions.add(BLOCK.HEIGHT.eq(height));
        }
        if (blockID != 0) {
            conditions.add(BLOCK.ID.eq(blockID));
        }
        if (account != 0) {
            conditions.add(BLOCK.GENERATOR_ID.eq(account));
        }
        if (pool != 0) {
            conditions.add(BLOCK.POOL_ID.eq(pool));
        }
        return ctx.selectCount().from(BLOCK).where(conditions).fetchOne(0, int.class);
    }

    @Override
    public DbIterator<Block> getBlocks(Account account, int timestamp, int from, int to) {
        try (DSLContext ctx = Db.getDSLContext()) {
            int blockchainHeight = ValNetwork.getBlockchain().getHeight();
            SelectConditionStep query = ctx.selectFrom(BLOCK).where(BLOCK.GENERATOR_ID.eq(account.getId()));
            if (timestamp > 0) {
                query = query.and(BLOCK.TIMESTAMP.ge(timestamp));
            }
            // DbUtils.limitsClause(from, to)))
            return
                    getBlocks(
                            ctx,
                            query.orderBy(BLOCK.HEIGHT.desc()).fetchResultSet()
                    );
        } catch (Exception e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public int getAccountBlockCount(long accountId, long poolId, int startTime, int endTime) {
        try (DSLContext ctx = Db.getDSLContext()) {
            ArrayList<Condition> conditions = new ArrayList<>();
            conditions.add(BLOCK.GENERATOR_ID.eq(accountId));
            if (startTime > 0 && endTime > 0) {
                conditions.add(BLOCK.TIMESTAMP.gt(startTime).and(BLOCK.TIMESTAMP.le(endTime)));
            }
            if (poolId != 0) {
                conditions.add(BLOCK.POOL_ID.eq(poolId));
            }
            return ctx.selectCount().from(BLOCK).where(conditions).fetchOne(0, int.class);
        } catch (Exception e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public int getPoolBlockCount(long accountId, int startTime, int endTime) {
        try (DSLContext ctx = Db.getDSLContext()) {
            ArrayList<Condition> conditions = new ArrayList<>();
            conditions.add(BLOCK.POOL_ID.eq(accountId));
            if (startTime > 0 && endTime > 0) {
                conditions.add(BLOCK.TIMESTAMP.gt(startTime).and(BLOCK.TIMESTAMP.le(endTime)));
            }
            return ctx.selectCount().from(BLOCK).where(conditions).fetchOne(0, int.class);
        } catch (Exception e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public int getAccountBlockCount(byte[] publicKey) {
        try (DSLContext ctx = Db.getDSLContext()) {
            return ctx.selectCount().from(BLOCK).where(BLOCK.GENERATOR_PUBLIC_KEY.eq(publicKey)).fetchOne(0, int.class);
        } catch (Exception e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public long getLatestBlockReward(long accountId) {
        try (DSLContext ctx = Db.getDSLContext()) {
            return ctx.select(BLOCK.FORGE_REWARD).from(BLOCK).where(BLOCK.POOL_ID.eq(accountId)).orderBy(BLOCK.HEIGHT.desc()).limit(1).fetchOne(0, long.class);
        } catch (Exception e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public DbIterator<Block> getBlocks(DSLContext ctx, ResultSet rs) {
        return new val.db.sql.DbIterator(ctx, rs, blockDb::loadBlock);
    }

    @Override
    public List<Long> getBlockIdsAfter(long blockId, int limit) {
        if (limit > 1440) {
            throw new IllegalArgumentException("Can't get more than 1440 blocks at a time");
        }

        try (DSLContext ctx = Db.getDSLContext()) {
            return
                    ctx.selectFrom(BLOCK).where(
                            BLOCK.HEIGHT.gt(ctx.select(BLOCK.HEIGHT).from(BLOCK).where(BLOCK.ID.eq(blockId)))
                    ).orderBy(BLOCK.HEIGHT.asc()).limit(limit).fetch(BLOCK.ID, Long.class);
        } catch (Exception e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public List<Block> getBlocksAfter(long blockId, int limit) {
        if (limit > 1440) {
            throw new IllegalArgumentException("Can't get more than 1440 blocks at a time");
        }

        try (DSLContext ctx = Db.getDSLContext()) {
            List<Block> blocksAfter = new ArrayList<>();
            try (Cursor<BlockRecord> cursor = ctx.selectFrom(BLOCK).where(BLOCK.HEIGHT.gt(ctx.select(BLOCK.HEIGHT).from(BLOCK).where(BLOCK.ID.eq(blockId)))).orderBy(BLOCK.HEIGHT.asc()).limit(limit).fetchLazy()) {
                while (cursor.hasNext()) {
                    blocksAfter.add(blockDb.loadBlock(cursor.fetchNext()));
                }
            }
            return blocksAfter;
        } catch (Exception e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public int getTransactionCount() {
        DSLContext ctx = Db.getDSLContext();
        return ctx.selectCount().from(TRANSACTION).fetchOne(0, int.class);
    }

    @Override
    public int getTransactionCount(long accountId, int countType) {
        DSLContext ctx = Db.getDSLContext();
        ArrayList<Condition> conditions = new ArrayList<>();

        SelectQuery selectQuery = null;

        if (countType == 0) {//transaction out
            conditions.add(TRANSACTION.SENDER_ID.eq(accountId));
            selectQuery = ctx.selectFrom(TRANSACTION).where(conditions).getQuery();

        } else if (countType == 1) {//transaction in
            conditions.add(TRANSACTION.RECIPIENT_ID.eq(accountId));
            selectQuery = ctx.selectFrom(TRANSACTION).where(conditions).getQuery();
        } else {// transaction total
            ArrayList<Condition> conditionsTnx = new ArrayList<>();
            conditions.add(TRANSACTION.RECIPIENT_ID.eq(accountId));
            conditions.add(TRANSACTION.SENDER_ID.ne(accountId));
            conditionsTnx.add(TRANSACTION.SENDER_ID.eq(accountId));
            selectQuery = ctx.selectFrom(TRANSACTION).where(conditions)
                    .unionAll(ctx.selectFrom(TRANSACTION).where(conditionsTnx)).getQuery();
        }
        return ctx.selectCount().from(selectQuery).fetchOne(0, int.class);
    }

    @Override
    public DbIterator<Transaction> getAllTransactions() {
        DSLContext ctx = Db.getDSLContext();
        return getTransactions(
                ctx,
                ctx.selectFrom(TRANSACTION).orderBy(TRANSACTION.DB_ID.asc()).fetchResultSet()
        );
    }


    @Override
    public DbIterator<Transaction> getTransactions(Account account, int numberOfConfirmations, byte type, byte subtype,
                                                   int blockTimestamp, int from, int to) {
        int height = numberOfConfirmations > 0 ? ValNetwork.getBlockchain().getHeight() - numberOfConfirmations : Integer.MAX_VALUE;
        if (height < 0) {
            throw new IllegalArgumentException("Number of confirmations required " + numberOfConfirmations
                    + " exceeds current blockchain height " + ValNetwork.getBlockchain().getHeight());
        }
        DSLContext ctx = Db.getDSLContext();
        ArrayList<Condition> conditions = new ArrayList<>();
        if (blockTimestamp > 0) {
            conditions.add(TRANSACTION.BLOCK_TIMESTAMP.ge(blockTimestamp));
        }
        if (type >= 0) {
            conditions.add(TRANSACTION.TYPE.eq(type));
            if (subtype >= 0) {
                conditions.add(TRANSACTION.SUBTYPE.eq(subtype));
            }
        }
        if (height < Integer.MAX_VALUE) {
            conditions.add(TRANSACTION.HEIGHT.le(height));
        }
        SelectQuery selectQuery = ctx.selectFrom(TRANSACTION).where(conditions).and(
                TRANSACTION.RECIPIENT_ID.eq(account.getId()).and(
                        TRANSACTION.SENDER_ID.ne(account.getId())
                )
        ).unionAll(
                ctx.selectFrom(TRANSACTION).where(conditions).and(
                        TRANSACTION.SENDER_ID.eq(account.getId())
                )
        )
                .orderBy(TRANSACTION.BLOCK_TIMESTAMP.desc(), TRANSACTION.ID.desc()).getQuery();
        DbUtils.applyLimits(selectQuery, from, to);

        return getTransactions(
                ctx,
                selectQuery.fetchResultSet()
        );
    }

    public DbIterator<Transaction> getTransactionLists(Account account, byte type, byte subtype,
                                                       long transactionId, String transactionFullHash, long blockID, int height, int page, int limit) {
        try (DSLContext ctx = Db.getDSLContext()) {

            ArrayList<Condition> conditions = new ArrayList<>();
            ArrayList<Condition> conditionsTnx = new ArrayList<>();
            if (height >= 0 && height < Integer.MAX_VALUE) {
                conditions.add(TRANSACTION.HEIGHT.eq(height));
                conditionsTnx.add(TRANSACTION.HEIGHT.eq(height));
            }
            if (blockID != 0) {
                conditions.add(TRANSACTION.BLOCK_ID.eq(blockID));
                conditionsTnx.add(TRANSACTION.BLOCK_ID.eq(blockID));
            }
            if (type >= 0) {
                conditions.add(TRANSACTION.TYPE.eq(type));
                conditionsTnx.add(TRANSACTION.TYPE.eq(type));
                if (subtype >= 0) {
                    conditions.add(TRANSACTION.SUBTYPE.eq(subtype));
                    conditionsTnx.add(TRANSACTION.SUBTYPE.eq(subtype));
                }
            }
            if (transactionId != 0) {
                conditions.add(TRANSACTION.ID.eq(transactionId));
                conditionsTnx.add(TRANSACTION.ID.eq(transactionId));
            }
            if (transactionFullHash != null) {
                conditions.add(TRANSACTION.FULL_HASH.eq(Convert.parseHexString(transactionFullHash)));
                conditionsTnx.add(TRANSACTION.FULL_HASH.eq(Convert.parseHexString(transactionFullHash)));
            }
            SelectQuery selectQuery = null;
            if (account != null) {
                conditions.add(TRANSACTION.RECIPIENT_ID.eq(account.getId()));
                conditions.add(TRANSACTION.SENDER_ID.ne(account.getId()));
                conditionsTnx.add(TRANSACTION.SENDER_ID.eq(account.getId()));
                selectQuery = ctx.selectFrom(TRANSACTION).where(conditions)
                        //.and(TRANSACTION.RECIPIENT_ID.eq(account.getId()).and(TRANSACTION.SENDER_ID.ne(account.getId())))
                        .unionAll(ctx.selectFrom(TRANSACTION).where(conditionsTnx))   /*.and(TRANSACTION.SENDER_ID.eq(account.getId())))*/
                        .orderBy(TRANSACTION.BLOCK_TIMESTAMP.desc(), TRANSACTION.ID.desc()).getQuery();
            } else {
                selectQuery = ctx.selectFrom(TRANSACTION).where(conditions)
                        .orderBy(TRANSACTION.BLOCK_TIMESTAMP.desc(), TRANSACTION.ID.desc()).getQuery();
            }

            DbUtils.applyPages(selectQuery, page, limit);

            return getTransactions(ctx, selectQuery.fetchResultSet());
        } catch (Exception e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public int getTransactionCount(Account account, byte type, byte subtype,
                                   long transactionId, String transactionFullHash, long blockID, int height) {

        try (DSLContext ctx = Db.getDSLContext()) {

            ArrayList<Condition> conditions = new ArrayList<>();
            ArrayList<Condition> conditionsTnx = new ArrayList<>();
            if (height >= 0 && height < Integer.MAX_VALUE) {
                conditions.add(TRANSACTION.HEIGHT.eq(height));
                conditionsTnx.add(TRANSACTION.HEIGHT.eq(height));
            }
            if (blockID != 0) {
                conditions.add(TRANSACTION.BLOCK_ID.eq(blockID));
                conditionsTnx.add(TRANSACTION.BLOCK_ID.eq(blockID));
            }
            if (type >= 0) {
                conditions.add(TRANSACTION.TYPE.eq(type));
                conditionsTnx.add(TRANSACTION.TYPE.eq(type));
                if (subtype >= 0) {
                    conditions.add(TRANSACTION.SUBTYPE.eq(subtype));
                    conditionsTnx.add(TRANSACTION.SUBTYPE.eq(subtype));
                }
            }
            if (transactionId != 0) {
                conditions.add(TRANSACTION.ID.eq(transactionId));
                conditionsTnx.add(TRANSACTION.ID.eq(transactionId));
            }
            if (transactionFullHash != null) {
                conditions.add(TRANSACTION.FULL_HASH.eq(Convert.parseHexString(transactionFullHash)));
                conditionsTnx.add(TRANSACTION.FULL_HASH.eq(Convert.parseHexString(transactionFullHash)));
            }
            SelectQuery selectQuery = null;
            if (account != null) {
                conditions.add(TRANSACTION.RECIPIENT_ID.eq(account.getId()));
                conditions.add(TRANSACTION.SENDER_ID.ne(account.getId()));
                conditionsTnx.add(TRANSACTION.SENDER_ID.eq(account.getId()));
                selectQuery = ctx.selectFrom(TRANSACTION).where(conditions)
                        //.and(TRANSACTION.RECIPIENT_ID.eq(account.getId()).and(TRANSACTION.SENDER_ID.ne(account.getId())))
                        .unionAll(ctx.selectFrom(TRANSACTION).where(conditionsTnx)).getQuery();
            } else {
                selectQuery = ctx.selectFrom(TRANSACTION).where(conditions).getQuery();
            }


            return ctx.selectCount().from(selectQuery).fetchOne(0, int.class);
        } catch (Exception e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public DbIterator<Transaction> getTransactions(DSLContext ctx, ResultSet rs) {
        return new val.db.sql.DbIterator(ctx, rs, transactionDb::loadTransaction);
    }

    @Override
    public boolean addBlock(Block block) {
        DSLContext ctx = Db.getDSLContext();
        blockDb.saveBlock(ctx, block);
        return true;
    }

    public void scan(int height) {
    }

    @Override
    public DbIterator<Block> getLatestBlocks(int amountBlocks) {
        final int latestBlockHeight = blockDb.findLastBlock().getHeight();

        final int firstLatestBlockHeight = Math.max(0, latestBlockHeight - amountBlocks);

        try (DSLContext ctx = Db.getDSLContext()) {
            return
                    getBlocks(
                            ctx,
                            ctx.selectFrom(BLOCK).where(
                                    BLOCK.HEIGHT.between(firstLatestBlockHeight).and(latestBlockHeight)
                            ).orderBy(BLOCK.HEIGHT.asc()).fetchResultSet()
                    );
        } catch (Exception e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public int getBlockchainAddressCount() {
        try (DSLContext ctx = Db.getDSLContext()) {
            return ctx.selectCount().from(ACCOUNT).where(ACCOUNT.ID.ne(0L).and(ACCOUNT.LATEST.eq(true))).fetchOne(0, int.class);
        } catch (Exception e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public String getTransactionRate(int curTime, int dur) {
        int stTime = curTime - dur;
        String transactionRate = "1.00";
        //System.out.printf("getTransactionRate, stTime:[%s], curTime:[%s], dur:[%s]\n", stTime,curTime,dur);
        try (DSLContext ctx = Db.getDSLContext()) {
            int transactionCount = ctx.selectCount().from(TRANSACTION).where(
                    TRANSACTION.BLOCK_TIMESTAMP.greaterOrEqual(stTime).and(TRANSACTION.BLOCK_TIMESTAMP.lessThan(curTime))
            ).fetchOne(0, int.class);
            int blockCount = ctx.selectCount().from(BLOCK).where(
                    BLOCK.TIMESTAMP.greaterOrEqual(stTime).and(BLOCK.TIMESTAMP.lessThan(curTime))
            ).fetchOne(0, int.class);
            //System.out.printf("getTransactionRate, transactionCount:[%s], blockCount:[%s]\n", transactionCount,blockCount);
            if (blockCount > 0) {
                DecimalFormat df = new DecimalFormat("0.00");//格式化小数
                transactionRate = df.format((float) transactionCount / (float) blockCount);
            }
            return transactionRate;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public String getMiddlePayload(int curTime, int dur) {
        int stTime = curTime - dur;
        String middlePayload = "0";
        //System.out.printf("getMiddlePayload, stTime:[%s], curTime:[%s], dur:[%s]\n", stTime,curTime,dur);
        try (DSLContext ctx = Db.getDSLContext()) {
            Result<Record1<Integer>> results = ctx.select(BLOCK.PAYLOAD_LENGTH).from(BLOCK).where(
                    BLOCK.TIMESTAMP.greaterOrEqual(stTime).and(BLOCK.TIMESTAMP.lessThan(curTime).and(BLOCK.PAYLOAD_LENGTH.gt(0)))).getQuery().fetch();
            if (results.size() > 0) {
                Collections.sort(results);
                middlePayload = String.valueOf(results.get(results.size() / 2).getValue(BLOCK.PAYLOAD_LENGTH));
            }
//			for (Record1<Integer> record:results){
//				System.out.printf("%s\t",record.getValue(BLOCK.PAYLOAD_LENGTH));	
//			}
            return middlePayload;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.toString(), e);
        }
    }


}
