package val.db.sql;

import org.jooq.DSLContext;
import org.jooq.DeleteQuery;
import org.jooq.SelectQuery;
import org.jooq.impl.TableImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import val.Block;
import val.ValNetwork;
import val.ValException;
import val.db.BlockDb;
import val.schema.Tables;
import val.schema.tables.records.BlockRecord;

import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static val.schema.Tables.BLOCK;

public class SqlBlockDb implements BlockDb {

    private static final Logger logger = LoggerFactory.getLogger(BlockDb.class);

    public Block findBlock(long blockId) {
        try (DSLContext ctx = Db.getDSLContext()) {
            BlockRecord r = ctx.selectFrom(BLOCK).where(BLOCK.ID.eq(blockId)).fetchAny();
            return r == null ? null : loadBlock(r);
        } catch (ValException.ValidationException e) {
            throw new RuntimeException("Block already in database, id = " + blockId + ", does not pass validation!", e);
        }
    }

    public boolean hasBlock(long blockId) {
        DSLContext ctx = Db.getDSLContext();

        boolean ret = ctx.fetchExists(ctx.selectOne().from(BLOCK).where(BLOCK.ID.eq(blockId)));
        // logger.info("SQL hashBlock:{}, ret:{}", blockId, ret);
        return ret;
    }

    public long findBlockIdAtHeight(int height) {
        DSLContext ctx = Db.getDSLContext();
        Long id = ctx.select(BLOCK.ID).from(BLOCK).where(BLOCK.HEIGHT.eq(height)).fetchOne(BLOCK.ID);
        if (id == null) {
            throw new RuntimeException("Block at height " + height + " not found in database!");
        }
        return id;
    }

    public Block findBlockAtHeight(int height) {
        try (DSLContext ctx = Db.getDSLContext()) {
            Block block = loadBlock(ctx.selectFrom(BLOCK).where(BLOCK.HEIGHT.eq(height)).fetchAny());
            if (block == null) {
                throw new RuntimeException("Block at height " + height + " not found in database!");
            }
            return block;
        } catch (ValException.ValidationException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public Block findLastBlock() {
        try (DSLContext ctx = Db.getDSLContext()) {
            return loadBlock(ctx.selectFrom(BLOCK).orderBy(BLOCK.DB_ID.desc()).limit(1).fetchAny());
        } catch (ValException.ValidationException e) {
            throw new RuntimeException("Last block already in database does not pass validation!", e);
        }
    }

    public Block findLastBlock(int timestamp) {
        try (DSLContext ctx = Db.getDSLContext()) {
            return loadBlock(ctx.selectFrom(BLOCK).where(BLOCK.TIMESTAMP.lessOrEqual(timestamp)).orderBy(BLOCK.DB_ID.desc()).limit(1).fetchAny());
        } catch (ValException.ValidationException e) {
            throw new RuntimeException("Block already in database at timestamp " + timestamp + " does not pass validation!", e);
        }
    }

    public Block loadBlock(DSLContext ctx, ResultSet rs)
            throws ValException.ValidationException {
        try {
            int version = rs.getInt("version");
            int timestamp = rs.getInt("timestamp");
            long previousBlockId = rs.getLong("previous_block_id");
            long totalAmountNQT = rs.getLong("total_amount");
            long totalFeeNQT = rs.getLong("total_fee");
            int payloadLength = rs.getInt("payload_length");
            byte[] generatorPublicKey = rs.getBytes("generator_public_key");
            byte[] previousBlockHash = rs.getBytes("previous_block_hash");
            BigInteger cumulativeDifficulty = new BigInteger(rs.getBytes("cumulative_difficulty"));
            long baseTarget = rs.getLong("base_target");
            long nextBlockId = rs.getLong("next_block_id");
            int height = rs.getInt("height");
            byte[] generationSignature = rs.getBytes("generation_signature");
            byte[] blockSignature = rs.getBytes("block_signature");
            byte[] payloadHash = rs.getBytes("payload_hash");
            long id = rs.getLong("id");
            long nonce = rs.getLong("nonce");
            byte[] blockATs = rs.getBytes("ats");
            long forgeReward = rs.getLong("forge_reward");
            long poolId = rs.getLong("pool_id");

            Block block = new Block(version, timestamp, previousBlockId, totalAmountNQT, totalFeeNQT,
                    payloadLength, payloadHash, generatorPublicKey, generationSignature, blockSignature,
                    previousBlockHash, cumulativeDifficulty, baseTarget, nextBlockId, height, id, nonce,
                    blockATs);
            block.setForgeReward(forgeReward);
            block.setPoolId(poolId);
            return block;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public Block loadBlock(BlockRecord r) throws ValException.ValidationException {
        int version = r.getVersion();
        int timestamp = r.getTimestamp();
        long previousBlockId = Optional.ofNullable(r.getPreviousBlockId()).orElse(0L);
        long totalAmountNQT = r.getTotalAmount();
        long totalFeeNQT = r.getTotalFee();
        int payloadLength = r.getPayloadLength();
        byte[] generatorPublicKey = r.getGeneratorPublicKey();
        byte[] previousBlockHash = r.getPreviousBlockHash();
        BigInteger cumulativeDifficulty = new BigInteger(r.getCumulativeDifficulty());
        long baseTarget = r.getBaseTarget();
        long nextBlockId = Optional.ofNullable(r.getNextBlockId()).orElse(0L);
        int height = r.getHeight();
        byte[] generationSignature = r.getGenerationSignature();
        byte[] blockSignature = r.getBlockSignature();
        byte[] payloadHash = r.getPayloadHash();
        long id = r.getId();
        long nonce = r.getNonce();
        byte[] blockATs = r.getAts();
        long forgeReward = r.getForgeReward();
        long poolId = r.getPoolId();

        Block block = new Block(version, timestamp, previousBlockId, totalAmountNQT, totalFeeNQT, payloadLength, payloadHash,
                generatorPublicKey, generationSignature, blockSignature, previousBlockHash,
                cumulativeDifficulty, baseTarget, nextBlockId, height, id, nonce, blockATs);
        block.setForgeReward(forgeReward);
        block.setPoolId(poolId);
        return block;
    }

    public void saveBlock(DSLContext ctx, Block block) {
        ctx.insertInto(BLOCK, BLOCK.ID, BLOCK.VERSION, BLOCK.TIMESTAMP, BLOCK.PREVIOUS_BLOCK_ID,
                BLOCK.TOTAL_AMOUNT, BLOCK.TOTAL_FEE, BLOCK.PAYLOAD_LENGTH, BLOCK.GENERATOR_PUBLIC_KEY,
                BLOCK.PREVIOUS_BLOCK_HASH, BLOCK.CUMULATIVE_DIFFICULTY, BLOCK.BASE_TARGET, BLOCK.HEIGHT,
                BLOCK.GENERATION_SIGNATURE, BLOCK.BLOCK_SIGNATURE, BLOCK.PAYLOAD_HASH, BLOCK.GENERATOR_ID,
                BLOCK.NONCE, BLOCK.POOL_ID, BLOCK.FORGE_REWARD, BLOCK.ATS)
                .values(block.getId(), block.getVersion(), block.getTimestamp(),
                        block.getPreviousBlockId() == 0 ? null : block.getPreviousBlockId(),
                        block.getTotalAmountNQT(), block.getTotalFeeNQT(), block.getPayloadLength(),
                        block.getGeneratorPublicKey(), block.getPreviousBlockHash(),
                        block.getCumulativeDifficulty().toByteArray(), block.getBaseTarget(), block.getHeight(),
                        block.getGenerationSignature(), block.getBlockSignature(), block.getPayloadHash(),
                        block.getGeneratorId(), block.getNonce(), block.getPoolId(), block.getForgeReward(), block.getBlockATs())
                .execute();

        ValNetwork.getDbs().getTransactionDb().saveTransactions(block.getTransactions());

        if (block.getPreviousBlockId() != 0) {
            ctx.update(BLOCK).set(BLOCK.NEXT_BLOCK_ID, block.getId())
                    .where(BLOCK.ID.eq(block.getPreviousBlockId())).execute();
        }
    }

    // relying on cascade triggers in the database to delete the transactions for all deleted blocks
    @Override
    public void deleteBlocksFrom(long blockId) {
        if (!Db.isInTransaction()) {
            try {
                Db.beginTransaction();
                deleteBlocksFrom(blockId);
                Db.commitTransaction();
            } catch (Exception e) {
                Db.rollbackTransaction();
                throw e;
            } finally {
                Db.endTransaction();
            }
            return;
        }
        DSLContext ctx = Db.getDSLContext();
        SelectQuery blockHeightQuery = ctx.selectQuery();
        blockHeightQuery.addFrom(BLOCK);
        blockHeightQuery.addSelect(BLOCK.field("height", Integer.class));
        blockHeightQuery.addConditions(BLOCK.field("id", Long.class).eq(blockId));
        Integer blockHeight = (Integer) ctx.fetchValue(blockHeightQuery.fetchResultSet());

        if (blockHeight != null) {
            DeleteQuery deleteQuery = ctx.deleteQuery(BLOCK);
            deleteQuery.addConditions(BLOCK.field("height", Integer.class).ge(blockHeight));
            deleteQuery.execute();
        }
    }

    public void deleteAll(boolean force) {
        if (!Db.isInTransaction()) {
            try {
                Db.beginTransaction();
                deleteAll(force);
                Db.commitTransaction();
            } catch (Exception e) {
                Db.rollbackTransaction();
                throw e;
            } // FIXME: nally {
            Db.endTransaction();
            return;
        }
        logger.info("Deleting blockchain...");
        DSLContext ctx = Db.getDSLContext();
        List<TableImpl> tables = new ArrayList<>(Arrays.asList(Tables.ACCOUNT,
                Tables.ACCOUNT_ASSET, Tables.ALIAS, Tables.ALIAS_OFFER,
                Tables.ASK_ORDER, Tables.ASSET, Tables.ASSET_TRANSFER,
                Tables.AT, Tables.AT_STATE, Tables.BID_ORDER,
                Tables.BLOCK, Tables.ESCROW, Tables.ESCROW_DECISION,
                Tables.GOODS, Tables.PEER, Tables.PURCHASE,
                Tables.PURCHASE_FEEDBACK, Tables.PURCHASE_PUBLIC_FEEDBACK,
                Tables.REWARD_RECIP_ASSIGN, Tables.SUBSCRIPTION,
                Tables.TRADE, Tables.TRANSACTION,
                Tables.UNCONFIRMED_TRANSACTION));
        for (TableImpl table : tables) {
            try {
                ctx.truncate(table).execute();
            } catch (org.jooq.exception.DataAccessException e) {
                if (force) {
                    logger.trace("exception during truncate {0}", table, e);
                } else {
                    throw e;
                }
            }
        }
    }
}
