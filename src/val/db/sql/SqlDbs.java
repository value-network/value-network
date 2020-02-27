package val.db.sql;

import val.TransactionDb;
import val.db.BlockDb;
import val.db.PeerDb;
import val.db.store.Dbs;

public class SqlDbs implements Dbs {

    private final BlockDb blockDb;
    private final TransactionDb transactionDb;
    private final PeerDb peerDb;


    public SqlDbs() {
        this.blockDb = new SqlBlockDb();
        this.transactionDb = new SqlTransactionDb();
        this.peerDb = new SqlPeerDb();
    }

    @Override
    public BlockDb getBlockDb() {
        return blockDb;
    }

    @Override
    public TransactionDb getTransactionDb() {
        return transactionDb;
    }

    @Override
    public PeerDb getPeerDb() {
        return peerDb;
    }

}
