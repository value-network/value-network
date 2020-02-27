package val.db;

import org.jooq.DSLContext;
import val.Block;
import val.ValException;
import val.schema.tables.records.BlockRecord;

import java.sql.ResultSet;

public interface BlockDb {
    Block findBlock(long blockId);

    boolean hasBlock(long blockId);

    long findBlockIdAtHeight(int height);

    Block findBlockAtHeight(int height);

    Block findLastBlock();

    Block findLastBlock(int timestamp);

    Block loadBlock(DSLContext ctx, ResultSet rs) throws ValException.ValidationException;

    Block loadBlock(BlockRecord r) throws ValException.ValidationException;

    void saveBlock(DSLContext ctx, Block block);

    // relying on cascade triggers in the database to delete the transactions for all deleted blocks
    void deleteBlocksFrom(long blockId);

    void deleteAll(boolean force);
}
