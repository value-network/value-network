package val;

import val.util.Listener;
import val.util.ThreadPool;

import java.math.BigInteger;
import java.util.Collection;

public interface Generator {

    void generateForBlockchainProcessor(ThreadPool threadPool, BlockchainProcessor blockchainProcessor);

    boolean addListener(Listener<GeneratorState> listener, Event eventType);

    boolean removeListener(Listener<GeneratorState> listener, Event eventType);

    GeneratorState addNonce(String secretPhrase, Long nonce);

    GeneratorState addNonce(String secretPhrase, Long nonce, byte[] publicKey);

    Collection<? extends GeneratorState> getAllGenerators();

    byte[] calculateGenerationSignature(byte[] lastGenSig, long lastGenId);

    int calculateScoop(byte[] genSig, long height);

    BigInteger calculateHit(long accountId, long nonce, byte[] genSig, int scoop, int blockHeight);

    BigInteger calculateHit(long accountId, long nonce, byte[] genSig, byte[] scoopData);

    BigInteger calculateDeadline(long accountId, long nonce, byte[] genSig, int scoop, long baseTarget, int blockHeight);

    enum Event {
        GENERATION_DEADLINE, NONCE_SUBMITTED
    }

    interface GeneratorState {
        byte[] getPublicKey();

        Long getAccountId();

        BigInteger getDeadline();

        long getBlock();
    }
}
