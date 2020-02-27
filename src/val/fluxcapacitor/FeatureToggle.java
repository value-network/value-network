package val.fluxcapacitor;

public enum FeatureToggle {

    REWARD_RECIPIENT_ENABLE(new FluxHistory<>(false, new FluxHistory.Element<>(HistoricalMoments.REWARD_RECIPIENT_ENABLE, true))),
    DIGITAL_GOODS_STORE(new FluxHistory<>(false, new FluxHistory.Element<>(HistoricalMoments.DIGITAL_GOODS_STORE_BLOCK, true))),
    AUTOMATED_TRANSACTION_BLOCK(new FluxHistory<>(false, new FluxHistory.Element<>(HistoricalMoments.AUTOMATED_TRANSACTION_BLOCK, true))),
    AT_FIX_BLOCK_2(new FluxHistory<>(false, new FluxHistory.Element<>(HistoricalMoments.AT_FIX_BLOCK_2, true))),
    AT_FIX_BLOCK_3(new FluxHistory<>(false, new FluxHistory.Element<>(HistoricalMoments.AT_FIX_BLOCK_3, true))),
    AT_FIX_BLOCK_4(new FluxHistory<>(false, new FluxHistory.Element<>(HistoricalMoments.AT_FIX_BLOCK_4, true))),
    POC2(new FluxHistory<>(false, new FluxHistory.Element<>(HistoricalMoments.POC2, true))),
    PRE_DYMAXION(new FluxHistory<>(false, new FluxHistory.Element<>(HistoricalMoments.PRE_DYMAXION, true))),
    DYMAXION(new FluxHistory<>(false, new FluxHistory.Element<>(HistoricalMoments.DYMAXION, true)));

    private final FluxHistory<Boolean> flux;

    FeatureToggle(FluxHistory<Boolean> flux) {
        this.flux = flux;
    }

    public FluxHistory<Boolean> getFlux() {
        return flux;
    }

}
