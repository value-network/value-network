package val;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import val.ValException.ValidationException;
import val.db.store.Dbs;
import val.db.store.Stores;
import val.fluxcapacitor.FeatureToggle;
import val.peer.Peer;
import val.peer.Peers;
import val.props.PropertyService;
import val.props.Props;
import val.services.AccountService;
import val.services.TimeService;
import val.services.TransactionService;
import val.unconfirmedtransactions.UnconfirmedTransactionStore;
import val.util.JSON;
import val.util.Listener;
import val.util.Listeners;
import val.util.ThreadPool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static val.http.common.ResultFields.UNCONFIRMED_TRANSACTIONS_RESPONSE;

public class TransactionProcessorImpl implements TransactionProcessor {

    private static final Logger logger = LoggerFactory.getLogger(TransactionProcessorImpl.class);

    private final boolean testUnconfirmedTransactions;

    private final Object unconfirmedTransactionsSyncObj = new Object();

    private final Listeners<List<? extends Transaction>, Event> transactionListeners = new Listeners<>();

    private final EconomicClustering economicClustering;

    private final Stores stores;
    private final TimeService timeService;
    private final TransactionService transactionService;
    private final Dbs dbs;
    private final Blockchain blockchain;
    private final AccountService accountService;
    private final UnconfirmedTransactionStore unconfirmedTransactionStore;
    private final Function<Peer, List<Transaction>> foodDispenser;
    private final BiConsumer<Peer, List<Transaction>> doneFeedingLog;

    public TransactionProcessorImpl(PropertyService propertyService,
                                    EconomicClustering economicClustering, Blockchain blockchain, Stores stores, TimeService timeService, Dbs dbs, AccountService accountService,
                                    TransactionService transactionService, ThreadPool threadPool) {
        this.economicClustering = economicClustering;
        this.blockchain = blockchain;
        this.timeService = timeService;

        this.stores = stores;
        this.dbs = dbs;

        this.accountService = accountService;
        this.transactionService = transactionService;

        this.testUnconfirmedTransactions = propertyService.getBoolean(Props.BRS_TEST_UNCONFIRMED_TRANSACTIONS);
        this.unconfirmedTransactionStore = stores.getUnconfirmedTransactionStore();

        this.foodDispenser = (unconfirmedTransactionStore::getAllFor);
        this.doneFeedingLog = (unconfirmedTransactionStore::markFingerPrintsOf);

        Runnable getUnconfirmedTransactions = () -> {
            try {
                try {
                    synchronized (unconfirmedTransactionsSyncObj) {
                        Peer peer = Peers.getAnyPeer(Peer.State.CONNECTED);
                        if (peer == null) {
                            return;
                        }
                        JsonObject response = Peers.readUnconfirmedTransactionsNonBlocking(peer).get();
                        if (response == null) {
                            return;
                        }

                        JsonArray transactionsData = JSON.getAsJsonArray(response.get(UNCONFIRMED_TRANSACTIONS_RESPONSE));

                        if (transactionsData == null) {
                            return;
                        }
                        try {
                            List<Transaction> addedTransactions = processPeerTransactions(transactionsData, peer);
                            Peers.feedingTime(peer, foodDispenser, doneFeedingLog);

                            if (!addedTransactions.isEmpty()) {
                                List<Peer> activePrioPlusExtra = Peers.getAllActivePriorityPlusSomeExtraPeers();
                                activePrioPlusExtra.remove(peer);

                                List<CompletableFuture<?>> expectedResults = new ArrayList<>();

                                for (Peer otherPeer : activePrioPlusExtra) {
                                    CompletableFuture<JsonObject> unconfirmedTransactionsResult = Peers.readUnconfirmedTransactionsNonBlocking(otherPeer);

                                    unconfirmedTransactionsResult.whenComplete((jsonObject, throwable) -> {
                                        try {
                                            processPeerTransactions(transactionsData, otherPeer);
                                            Peers.feedingTime(otherPeer, foodDispenser, doneFeedingLog);
                                        } catch (ValidationException | RuntimeException e) {
                                            peer.blacklist(e, "pulled invalid data using getUnconfirmedTransactions");
                                        }
                                    });

                                    expectedResults.add(unconfirmedTransactionsResult);
                                }

                                CompletableFuture.allOf(expectedResults.toArray(new CompletableFuture[0])).join();
                            }
                        } catch (ValidationException | RuntimeException e) {
                            peer.blacklist(e, "pulled invalid data using getUnconfirmedTransactions");
                        }
                    }
                } catch (Exception e) {
                    logger.debug("Error processing unconfirmed transactions", e);
                }

            } catch (Exception t) {
                logger.info("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString(), t);
                System.exit(1);
            }
        };
        threadPool.scheduleThread("PullUnconfirmedTransactions", getUnconfirmedTransactions, 5);
    }

    @Override
    public boolean addListener(Listener<List<? extends Transaction>> listener, Event eventType) {
        return transactionListeners.addListener(listener, eventType);
    }

    @Override
    public boolean removeListener(Listener<List<? extends Transaction>> listener, Event eventType) {
        return transactionListeners.removeListener(listener, eventType);
    }

    void notifyListeners(List<? extends Transaction> transactions, Event eventType) {
        transactionListeners.notify(transactions, eventType);
    }

    public Object getUnconfirmedTransactionsSyncObj() {
        return unconfirmedTransactionsSyncObj;
    }

    @Override
    public List<Transaction> getAllUnconfirmedTransactions() {
        return unconfirmedTransactionStore.getAll();
    }

    @Override
    public int getAmountUnconfirmedTransactions() {
        return unconfirmedTransactionStore.getAmount();
    }

    @Override
    public List<Transaction> getAllUnconfirmedTransactionsFor(Peer peer) {
        return unconfirmedTransactionStore.getAllFor(peer);
    }

    @Override
    public void markFingerPrintsOf(Peer peer, List<Transaction> transactions) {
        unconfirmedTransactionStore.markFingerPrintsOf(peer, transactions);
    }

    @Override
    public Transaction getUnconfirmedTransaction(long transactionId) {
        return unconfirmedTransactionStore.get(transactionId);
    }

    @Override
    public Transaction.Builder newTransactionBuilder(byte[] senderPublicKey, long amountNQT, long feeNQT, short deadline, Attachment attachment) {
        byte version = (byte) getTransactionVersion(blockchain.getHeight());
        int timestamp = timeService.getEpochTime();
        Transaction.Builder builder = new Transaction.Builder(version, senderPublicKey, amountNQT, feeNQT, timestamp,
                deadline, (Attachment.AbstractAttachment) attachment);
        if (version > 0) {
            Block ecBlock = this.economicClustering.getECBlock(timestamp);
            builder.ecBlockHeight(ecBlock.getHeight());
            builder.ecBlockId(ecBlock.getId());
        }
        return builder;
    }

    @Override
    public Transaction.Builder newTransactionBuilderWithTimestamp(byte[] senderPublicKey, long amountNQT, long feeNQT, short deadline, Attachment attachment, int timestamp) {
        byte version = (byte) getTransactionVersion(blockchain.getHeight());
        // int timestamp = timeService.getEpochTime();
        Transaction.Builder builder = new Transaction.Builder(version, senderPublicKey, amountNQT, feeNQT, timestamp,
                deadline, (Attachment.AbstractAttachment) attachment);
        if (version > 0) {
            Block ecBlock = this.economicClustering.getECBlock(timestamp);
            builder.ecBlockHeight(ecBlock.getHeight());
            builder.ecBlockId(ecBlock.getId());
        }
        return builder;
    }

    @Override
    public Integer broadcast(Transaction transaction) throws ValException.ValidationException {
        if (!transaction.verifySignature()) {
            throw new ValException.NotValidException("Transaction signature verification failed");
        }
        List<Transaction> processedTransactions;
        if (dbs.getTransactionDb().hasTransaction(transaction.getId())) {
            logger.info("Transaction " + transaction.getStringId() + " already in blockchain, will not broadcast again");
            return null;
        }

        if (unconfirmedTransactionStore.exists(transaction.getId())) {
            logger.info("Transaction " + transaction.getStringId() + " already in unconfirmed pool, will not broadcast again");
            return null;
        }
//    System.out.printf("transactions: %s", transaction.toString());
        processedTransactions = processTransactions(Collections.singleton(transaction), null);

        if (!processedTransactions.isEmpty()) {
            return broadcastToPeers(true);
        } else {
            logger.debug("Could not accept new transaction " + transaction.getStringId());
            throw new ValException.NotValidException("Invalid transaction " + transaction.getStringId());
        }
    }

    @Override
    public void processPeerTransactions(JsonObject request, Peer peer) throws ValException.ValidationException {
        JsonArray transactionsData = JSON.getAsJsonArray(request.get("transactions"));
        List<Transaction> processedTransactions = processPeerTransactions(transactionsData, peer);

        if (!processedTransactions.isEmpty()) {
            broadcastToPeers(false);
        } else {
        }
    }

    @Override
    public Transaction parseTransaction(byte[] bytes) throws ValException.ValidationException {
        return Transaction.parseTransaction(bytes);
    }

    @Override
    public Transaction parseTransaction(JsonObject transactionData) throws ValException.NotValidException {
        return Transaction.parseTransaction(transactionData, blockchain.getHeight());
    }

    @Override
    public void clearUnconfirmedTransactions() {
        synchronized (unconfirmedTransactionsSyncObj) {
            List<Transaction> removed;
            try {
                stores.beginTransaction();
                removed = unconfirmedTransactionStore.getAll();
                accountService.flushAccountTable();
                unconfirmedTransactionStore.clear();
                stores.commitTransaction();
            } catch (Exception e) {
                logger.error(e.toString(), e);
                stores.rollbackTransaction();
                throw e;
            } finally {
                stores.endTransaction();
            }

            transactionListeners.notify(removed, Event.REMOVED_UNCONFIRMED_TRANSACTIONS);
        }
    }

    void requeueAllUnconfirmedTransactions() {
        synchronized (unconfirmedTransactionsSyncObj) {
            unconfirmedTransactionStore.resetAccountBalances();
        }
    }

    int getTransactionVersion(int previousBlockHeight) {
        //return Volume.getFluxCapacitor().isActive(FeatureToggle.DIGITAL_GOODS_STORE, previousBlockHeight) ? 1 : 0;
        return ValNetwork.getFluxCapacitor().isActive(FeatureToggle.DIGITAL_GOODS_STORE, previousBlockHeight) ? 0 : 1;
    }

    // Watch: This is not really clean
    void processLater(Collection<Transaction> transactions) {
        for (Transaction transaction : transactions) {
            try {
                unconfirmedTransactionStore.put(transaction, null);
            } catch (ValException.ValidationException e) {
                logger.debug("Discarding invalid transaction in for later processing: " + JSON.toJsonString(transaction.getJsonObject()), e);
            }
        }
    }

    private List<Transaction> processPeerTransactions(JsonArray transactionsData, Peer peer) throws ValException.ValidationException {
        if (blockchain.getLastBlock().getTimestamp() < timeService.getEpochTime() - 60 * 1440 && !testUnconfirmedTransactions) {
            return new ArrayList<>();
        }
        if (blockchain.getHeight() <= Constants.NQT_BLOCK) {
            return new ArrayList<>();
        }
        List<Transaction> transactions = new ArrayList<>();
        for (JsonElement transactionData : transactionsData) {
            try {
                Transaction transaction = parseTransaction(JSON.getAsJsonObject(transactionData));
                logger.debug("parse transaction: %s", transaction.getId());
                transactionService.validate(transaction);
                if (!this.economicClustering.verifyFork(transaction)) {
          /*if(Volume.getBlockchain().getHeight() >= Constants.EC_CHANGE_BLOCK_1) {
            throw new VolumeException.NotValidException("Transaction from wrong fork");
            }*/
                    continue;
                }
                transactions.add(transaction);
            } catch (ValException.NotCurrentlyValidException ignore) {
                ignore.printStackTrace();
            } catch (ValException.NotValidException e) {
                logger.debug("Invalid transaction from peer: " + JSON.toJsonString(transactionData));
                throw e;
            }
        }
        return processTransactions(transactions, peer);
    }

    private List<Transaction> processTransactions(Collection<Transaction> transactions, Peer peer) throws ValException.ValidationException {
        synchronized (unconfirmedTransactionsSyncObj) {
            if (transactions.isEmpty()) {
                return Collections.emptyList();
            }

            List<Transaction> addedUnconfirmedTransactions = new ArrayList<>();

            for (Transaction transaction : transactions) {

                try {
                    int curTime = timeService.getEpochTime();
                    if (transaction.getTimestamp() > curTime + 15 || transaction.getExpiration() < curTime
                            || transaction.getDeadline() > 1440) {
//        	  System.out.printf("time err:transaction.getTimestamp(): %s, curTime: %s, transaction.getExpiration():%s , transaction.getDeadline() : %s \n",transaction.getTimestamp(),
                        // curTime, transaction.getExpiration(), transaction.getDeadline());
                        continue;
                    }

                    try {
                        stores.beginTransaction();
                        if (blockchain.getHeight() < Constants.NQT_BLOCK) {
//            	System.out.printf("blockchain.getHeight():%s\n",blockchain.getHeight());
                            break; // not ready to process transactions
                        }

                        if (dbs.getTransactionDb().hasTransaction(transaction.getId()) || unconfirmedTransactionStore.exists(transaction.getId())) {
                            stores.commitTransaction();
                            unconfirmedTransactionStore.markFingerPrintsOf(peer, Collections.singletonList(transaction));
                            continue;
                        }

                        if (!(transaction.verifySignature() && transactionService.verifyPublicKey(transaction))) {
                            if (accountService.getAccount(transaction.getSenderId()) != null) {
                                logger.debug("Transaction " + JSON.toJsonString(transaction.getJsonObject()) + " failed to verify");
                            }
                            stores.commitTransaction();
                            continue;
                        }

                        if (unconfirmedTransactionStore.put(transaction, peer)) {
                            addedUnconfirmedTransactions.add(transaction);
                        }

                        stores.commitTransaction();
                    } catch (Exception e) {
                        e.printStackTrace();
                        stores.rollbackTransaction();
                        throw e;
                    } finally {
                        stores.endTransaction();
                    }
                } catch (RuntimeException e) {
                    logger.info("Error processing transaction", e);
                }
            }

            if (!addedUnconfirmedTransactions.isEmpty()) {
                transactionListeners.notify(addedUnconfirmedTransactions, Event.ADDED_UNCONFIRMED_TRANSACTIONS);
            }

            return addedUnconfirmedTransactions;
        }
    }

    private int broadcastToPeers(boolean toAll) {
        List<? extends Peer> peersToSendTo = toAll ? Peers.getActivePeers().stream().limit(100).collect(Collectors.toList()) : Peers.getAllActivePriorityPlusSomeExtraPeers();

        logger.trace("Queueing up {} Peers for feeding", peersToSendTo.size());

        for (Peer p : peersToSendTo) {
            Peers.feedingTime(p, foodDispenser, doneFeedingLog);
        }

        return peersToSendTo.size();
    }

    public void revalidateUnconfirmedTransactions() {
        final List<Transaction> invalidTransactions = new ArrayList<>();

        for (Transaction t : unconfirmedTransactionStore.getAll()) {
            try {
                this.transactionService.validate(t);
            } catch (ValidationException e) {
                invalidTransactions.add(t);
            }
        }

        for (Transaction t : invalidTransactions) {
            unconfirmedTransactionStore.remove(t);
        }
    }

    public void removeForgedTransactions(List<Transaction> transactions) {
        this.unconfirmedTransactionStore.removeForgedTransactions(transactions);
    }
}
