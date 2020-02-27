package val.services;

import val.Block;
import val.BlockchainProcessor;
import val.ValException;

public interface BlockService {

    void preVerify(Block block) throws BlockchainProcessor.BlockNotAcceptedException, InterruptedException;

    void preVerify(Block block, byte[] scoopData) throws BlockchainProcessor.BlockNotAcceptedException, InterruptedException;

    long getBlockReward(Block block);

    void calculateBaseTarget(Block block, Block lastBlock) throws BlockchainProcessor.BlockOutOfOrderException;

    void setPrevious(Block block, Block previousBlock);

    boolean verifyGenerationSignature(Block block) throws BlockchainProcessor.BlockNotAcceptedException;

    boolean verifyBlockSignature(Block block) throws BlockchainProcessor.BlockOutOfOrderException;

    void apply(Block block);

    int getScoopNum(Block block);

    String getNextCumulativeDifficulty(Block block) throws ValException.ValidationException, BlockchainProcessor.BlockOutOfOrderException;
}
