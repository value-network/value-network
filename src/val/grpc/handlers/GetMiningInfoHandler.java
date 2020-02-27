package val.grpc.handlers;

import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import val.Block;
import val.BlockchainProcessor;
import val.crypto.hash.Shabal256;
import val.grpc.StreamResponseGrpcApiHandler;
import val.grpc.proto.VlmApi;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class GetMiningInfoHandler implements StreamResponseGrpcApiHandler<Empty, VlmApi.MiningInfo> {

    /**
     * Listener should close connection if it receives null.
     */
    private final Set<Consumer<VlmApi.MiningInfo>> listeners = new HashSet<>();

    private final AtomicReference<VlmApi.MiningInfo> currentMiningInfo = new AtomicReference<>();

    public GetMiningInfoHandler(BlockchainProcessor blockchainProcessor) {
        blockchainProcessor.addListener(this::onBlock, BlockchainProcessor.Event.BLOCK_PUSHED);
    }

    private void onBlock(Block block) {
        synchronized (currentMiningInfo) {
            byte[] nextGenSig = calculateGenerationSignature(block);
            VlmApi.MiningInfo miningInfo = currentMiningInfo.get();
            if (miningInfo == null || !Arrays.equals(miningInfo.getGenerationSignature().toByteArray(), nextGenSig) || miningInfo.getHeight() - 1 != block.getHeight() || miningInfo.getBaseTarget() != block.getBaseTarget()) {
                currentMiningInfo.set(VlmApi.MiningInfo.newBuilder()
                        .setGenerationSignature(ByteString.copyFrom(nextGenSig))
                        .setHeight(block.getHeight() + 1)
                        .setBaseTarget(block.getBaseTarget())
                        .build());
                notifyListeners(currentMiningInfo.get());
            }
        }
    }

    private void notifyListeners(VlmApi.MiningInfo miningInfo) {
        synchronized (listeners) {
            listeners.removeIf(listener -> {
                try {
                    listener.accept(miningInfo);
                    return false;
                } catch (Exception e) {
                    try {
                        listener.accept(null);
                    } catch (Exception ignored) {
                        // Ignore any errors attempting to disconnect as we may already be disconnected
                    }
                    return true;
                }
            });
        }
    }

    private void addListener(Consumer<VlmApi.MiningInfo> listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    @Override
    public void handleStreamRequest(Empty input, StreamObserver<VlmApi.MiningInfo> responseObserver) {
        responseObserver.onNext(currentMiningInfo.get());
        addListener(miningInfo -> {
            if (miningInfo == null) {
                responseObserver.onCompleted();
            } else {
                responseObserver.onNext(miningInfo);
            }
        });
    }

    private byte[] calculateGenerationSignature(Block previousBlock) {
        byte[] lastGenSig = previousBlock.getGenerationSignature();
        long lastGenerator = previousBlock.getGeneratorId();

        ByteBuffer buf = ByteBuffer.allocate(32 + 8);
        buf.put(lastGenSig);
        buf.putLong(lastGenerator);

        Shabal256 md = new Shabal256();
        md.update(buf.array());
        return md.digest();
    }
}
