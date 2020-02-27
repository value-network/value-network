package val.db.store;

import val.Escrow;
import val.Transaction;
import val.db.DbIterator;
import val.db.DbKey;
import val.db.VersionedEntityTable;

import java.util.Collection;
import java.util.List;

public interface EscrowStore {

    DbKey.LongKeyFactory<Escrow> getEscrowDbKeyFactory();

    VersionedEntityTable<Escrow> getEscrowTable();

    val.db.sql.DbKey.LinkKeyFactory<Escrow.Decision> getDecisionDbKeyFactory();

    VersionedEntityTable<Escrow.Decision> getDecisionTable();

    Collection<Escrow> getEscrowTransactionsByParticipant(Long accountId);

    List<Transaction> getResultTransactions();

    DbIterator<Escrow.Decision> getDecisions(Long id);
}
