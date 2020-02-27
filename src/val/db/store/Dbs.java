package val.db.store;

import val.TransactionDb;
import val.db.BlockDb;
import val.db.PeerDb;

public interface Dbs {

    BlockDb getBlockDb();

    TransactionDb getTransactionDb();

    PeerDb getPeerDb();

}
