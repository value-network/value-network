package val.fluxcapacitor;

import val.Blockchain;
import val.props.PropertyService;

public class FluxCapacitorImpl implements FluxCapacitor {

    private final Blockchain blockchain;

    private final FeatureCapacitor featureCapacitor;
    private final TypeCapacitor<Integer> intCapacitor;

    public FluxCapacitorImpl(Blockchain blockchain, PropertyService propertyService) {
        this.blockchain = blockchain;

        final HistorianImpl historian = new HistorianImpl(propertyService);

        featureCapacitor = new FeatureCapacitor(historian);
        intCapacitor = new IntCapacitor(historian);
    }

    @Override
    public boolean isActive(FeatureToggle featureToggle) {
        return isActive(featureToggle, blockchain.getHeight());
    }

    @Override
    public boolean isActive(FeatureToggle featureToggle, int height) {
        return featureCapacitor.getValueAtHeight(featureToggle.getFlux(), height);
    }

    @Override
    public Integer getInt(FluxInt fluxInt) {
        return this.getInt(fluxInt, blockchain.getHeight());
    }

    @Override
    public Integer getInt(FluxInt fluxInt, int height) {
        return intCapacitor.getValueAtHeight(fluxInt.getFlux(), height);
    }

    @Override
    public Integer getStartingHeight(FeatureToggle featureToggle) {
        return featureCapacitor.getStartingHeight(featureToggle.getFlux());
    }

}
