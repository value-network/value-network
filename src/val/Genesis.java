package val;

import val.util.Convert;

public final class Genesis {

    public static final long GENESIS_BLOCK_ID = 191068885632154880L;
    public static final long CREATOR_ID = 0L;

//    public static final long TNX_AMOUNT_NQT = 300000000L * Constants.ONE_COIN;
    public static final long TNX_AMOUNT_NQT = 10000000L * Constants.ONE_COIN;

    public static final String GENESIS_ACCOUNT = "VAL-RFWD-7TXD-EDUW-64JAW";

    private static final byte[] CREATOR_PUBLIC_KEY = {
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    };

    private static final byte[] GENESIS_BLOCK_SIGNATURE = new byte[]{
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    };

    private Genesis() {
    } // never

    public static byte[] getCreatorPublicKey() {
        return CREATOR_PUBLIC_KEY.clone();
    }

    public static byte[] getGenesisBlockSignature() {
        return GENESIS_BLOCK_SIGNATURE.clone();
    }

    public static long getTnxReceientID() {
        return Convert.parseAccountId(GENESIS_ACCOUNT);
    }

}
