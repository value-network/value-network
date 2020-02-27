package val.fluxcapacitor;

import val.props.Prop;
import val.props.Props;

enum HistoricalMoments {

    REWARD_RECIPIENT_ENABLE(0, 0, Props.DEV_REWARD_RECIPIENT_ENABLE_BLOCK_HEIGHT),
    //DIGITAL_GOODS_STORE_BLOCK(11800, 1440, Props.DEV_DIGITAL_GOODS_STORE_BLOCK_HEIGHT),// transaction version, message tnx, alias tnx, good tnx, ecblock
    DIGITAL_GOODS_STORE_BLOCK(-1, 0, Props.DEV_DIGITAL_GOODS_STORE_BLOCK_HEIGHT),
    AUTOMATED_TRANSACTION_BLOCK(49200, 1440, Props.DEV_AUTOMATED_TRANSACTION_BLOCK_HEIGHT),
    AT_FIX_BLOCK_2(67000, 2880, Props.DEV_AT_FIX_BLOCK_2_BLOCK_HEIGHT),
    AT_FIX_BLOCK_3(92000, 4320, Props.DEV_AT_FIX_BLOCK_3_BLOCK_HEIGHT),
    AT_FIX_BLOCK_4(255000, 5760, Props.DEV_AT_FIX_BLOCK_4_BLOCK_HEIGHT),
    //PRE_DYMAXION(500000, 71666, Props.DEV_PRE_DYMAXION_BLOCK_HEIGHT),//Multi Out Payments,Multi Same Out Payments, min fee
    PRE_DYMAXION(0, 0, Props.DEV_PRE_DYMAXION_BLOCK_HEIGHT),
    POC2(502000, 71670, Props.DEV_POC2_BLOCK_HEIGHT),
    DYMAXION(Integer.MAX_VALUE, Integer.MAX_VALUE, Props.DEV_DYMAXION_BLOCK_HEIGHT); // TBD :-)//no use

    final int momentProductionNet;
    final int momentTestNet;
    final Prop overridingProperty;

    HistoricalMoments(int momentProductionNet, int momentTestNet, Prop overridingProperty) {
        this.momentProductionNet = momentProductionNet;
        this.momentTestNet = momentTestNet;
        this.overridingProperty = overridingProperty;
    }
}
