package val.feesuggestions;

import org.apache.commons.collections.buffer.CircularFifoBuffer;
import val.Block;
import val.BlockchainProcessor;
import val.Constants;
import val.db.store.BlockchainStore;

public class FeeSuggestionCalculator {

    private final CircularFifoBuffer latestBlocks;

    private final BlockchainStore blockchainStore;

    private FeeSuggestion feeSuggestion;

    public FeeSuggestionCalculator(BlockchainProcessor blockchainProcessor, BlockchainStore blockchainStore, int historyLength) {
        this.latestBlocks = new CircularFifoBuffer(historyLength);

        this.blockchainStore = blockchainStore;

        blockchainProcessor.addListener(this::newBlockApplied, BlockchainProcessor.Event.AFTER_BLOCK_APPLY);
    }

    public FeeSuggestion giveFeeSuggestion() {
        if (latestBlocks.isEmpty()) {
            fillInitialHistory();
            recalculateSuggestion();
        }

        return feeSuggestion;
    }

    private void newBlockApplied(Block block) {
        if (latestBlocks.isEmpty()) {
            fillInitialHistory();
        }

        this.latestBlocks.add(block);
        recalculateSuggestion();
    }

    private void fillInitialHistory() {
        blockchainStore.getLatestBlocks(latestBlocks.maxSize()).forEachRemaining(latestBlocks::add);
    }

    private void recalculateSuggestion() {
        int lowestAmountTransactionsNearHistory = latestBlocks.stream().mapToInt(b -> ((Block) b).getTransactions().size()).min().orElse(1);
        int averageAmountTransactionsNearHistory = (int) Math.ceil(latestBlocks.stream().mapToInt(b -> ((Block) b).getTransactions().size()).average().getAsDouble());
        int highestAmountTransactionsNearHistory = latestBlocks.stream().mapToInt(b -> ((Block) b).getTransactions().size()).max().orElse(1);

        long cheapFee = (1 + lowestAmountTransactionsNearHistory) * Constants.FEE_QUANT;
        long standardFee = (1 + averageAmountTransactionsNearHistory) * Constants.FEE_QUANT;
        long priorityFee = (1 + highestAmountTransactionsNearHistory) * Constants.FEE_QUANT;

        feeSuggestion = new FeeSuggestion(cheapFee, standardFee, priorityFee);
    }
}
