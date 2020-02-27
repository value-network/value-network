package val.grpc.proto;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.*;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.*;

/**
 *
 */
@javax.annotation.Generated(
        value = "by gRPC proto compiler (version 1.22.1)",
        comments = "Source: vlmApi.proto")
public final class VlmApiServiceGrpc {

    public static final String SERVICE_NAME = "VlmApiService";
    private static final int METHODID_GET_BLOCK = 0;
    private static final int METHODID_GET_ACCOUNT = 1;
    private static final int METHODID_GET_ACCOUNTS = 2;
    private static final int METHODID_GET_MINING_INFO = 3;
    private static final int METHODID_GET_TRANSACTION = 4;
    private static final int METHODID_GET_TRANSACTION_BYTES = 5;
    private static final int METHODID_SUBMIT_NONCE = 6;
    // Static method descriptors that strictly reflect the proto.
    private static volatile io.grpc.MethodDescriptor<val.grpc.proto.VlmApi.GetBlockRequest,
            val.grpc.proto.VlmApi.Block> getGetBlockMethod;
    private static volatile io.grpc.MethodDescriptor<val.grpc.proto.VlmApi.GetAccountRequest,
            val.grpc.proto.VlmApi.Account> getGetAccountMethod;
    private static volatile io.grpc.MethodDescriptor<val.grpc.proto.VlmApi.GetAccountsRequest,
            val.grpc.proto.VlmApi.Accounts> getGetAccountsMethod;
    private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
            val.grpc.proto.VlmApi.MiningInfo> getGetMiningInfoMethod;
    private static volatile io.grpc.MethodDescriptor<val.grpc.proto.VlmApi.GetTransactionRequest,
            val.grpc.proto.VlmApi.Transaction> getGetTransactionMethod;
    private static volatile io.grpc.MethodDescriptor<val.grpc.proto.VlmApi.GetTransactionRequest,
            val.grpc.proto.VlmApi.TransactionBytes> getGetTransactionBytesMethod;
    private static volatile io.grpc.MethodDescriptor<val.grpc.proto.VlmApi.SubmitNonceRequest,
            val.grpc.proto.VlmApi.SubmitNonceResponse> getSubmitNonceMethod;
    private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

    private VlmApiServiceGrpc() {
    }

    @io.grpc.stub.annotations.RpcMethod(
            fullMethodName = SERVICE_NAME + '/' + "GetBlock",
            requestType = val.grpc.proto.VlmApi.GetBlockRequest.class,
            responseType = val.grpc.proto.VlmApi.Block.class,
            methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<val.grpc.proto.VlmApi.GetBlockRequest,
            val.grpc.proto.VlmApi.Block> getGetBlockMethod() {
        io.grpc.MethodDescriptor<val.grpc.proto.VlmApi.GetBlockRequest, val.grpc.proto.VlmApi.Block> getGetBlockMethod;
        if ((getGetBlockMethod = VlmApiServiceGrpc.getGetBlockMethod) == null) {
            synchronized (VlmApiServiceGrpc.class) {
                if ((getGetBlockMethod = VlmApiServiceGrpc.getGetBlockMethod) == null) {
                    VlmApiServiceGrpc.getGetBlockMethod = getGetBlockMethod =
                            io.grpc.MethodDescriptor.<val.grpc.proto.VlmApi.GetBlockRequest, val.grpc.proto.VlmApi.Block>newBuilder()
                                    .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                                    .setFullMethodName(generateFullMethodName(
                                            "VlmApiService", "GetBlock"))
                                    .setSampledToLocalTracing(true)
                                    .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                                            val.grpc.proto.VlmApi.GetBlockRequest.getDefaultInstance()))
                                    .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                                            val.grpc.proto.VlmApi.Block.getDefaultInstance()))
                                    .setSchemaDescriptor(new VlmApiServiceMethodDescriptorSupplier("GetBlock"))
                                    .build();
                }
            }
        }
        return getGetBlockMethod;
    }

    @io.grpc.stub.annotations.RpcMethod(
            fullMethodName = SERVICE_NAME + '/' + "GetAccount",
            requestType = val.grpc.proto.VlmApi.GetAccountRequest.class,
            responseType = val.grpc.proto.VlmApi.Account.class,
            methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<val.grpc.proto.VlmApi.GetAccountRequest,
            val.grpc.proto.VlmApi.Account> getGetAccountMethod() {
        io.grpc.MethodDescriptor<val.grpc.proto.VlmApi.GetAccountRequest, val.grpc.proto.VlmApi.Account> getGetAccountMethod;
        if ((getGetAccountMethod = VlmApiServiceGrpc.getGetAccountMethod) == null) {
            synchronized (VlmApiServiceGrpc.class) {
                if ((getGetAccountMethod = VlmApiServiceGrpc.getGetAccountMethod) == null) {
                    VlmApiServiceGrpc.getGetAccountMethod = getGetAccountMethod =
                            io.grpc.MethodDescriptor.<val.grpc.proto.VlmApi.GetAccountRequest, val.grpc.proto.VlmApi.Account>newBuilder()
                                    .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                                    .setFullMethodName(generateFullMethodName(
                                            "VlmApiService", "GetAccount"))
                                    .setSampledToLocalTracing(true)
                                    .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                                            val.grpc.proto.VlmApi.GetAccountRequest.getDefaultInstance()))
                                    .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                                            val.grpc.proto.VlmApi.Account.getDefaultInstance()))
                                    .setSchemaDescriptor(new VlmApiServiceMethodDescriptorSupplier("GetAccount"))
                                    .build();
                }
            }
        }
        return getGetAccountMethod;
    }

    @io.grpc.stub.annotations.RpcMethod(
            fullMethodName = SERVICE_NAME + '/' + "GetAccounts",
            requestType = val.grpc.proto.VlmApi.GetAccountsRequest.class,
            responseType = val.grpc.proto.VlmApi.Accounts.class,
            methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<val.grpc.proto.VlmApi.GetAccountsRequest,
            val.grpc.proto.VlmApi.Accounts> getGetAccountsMethod() {
        io.grpc.MethodDescriptor<val.grpc.proto.VlmApi.GetAccountsRequest, val.grpc.proto.VlmApi.Accounts> getGetAccountsMethod;
        if ((getGetAccountsMethod = VlmApiServiceGrpc.getGetAccountsMethod) == null) {
            synchronized (VlmApiServiceGrpc.class) {
                if ((getGetAccountsMethod = VlmApiServiceGrpc.getGetAccountsMethod) == null) {
                    VlmApiServiceGrpc.getGetAccountsMethod = getGetAccountsMethod =
                            io.grpc.MethodDescriptor.<val.grpc.proto.VlmApi.GetAccountsRequest, val.grpc.proto.VlmApi.Accounts>newBuilder()
                                    .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                                    .setFullMethodName(generateFullMethodName(
                                            "VlmApiService", "GetAccounts"))
                                    .setSampledToLocalTracing(true)
                                    .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                                            val.grpc.proto.VlmApi.GetAccountsRequest.getDefaultInstance()))
                                    .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                                            val.grpc.proto.VlmApi.Accounts.getDefaultInstance()))
                                    .setSchemaDescriptor(new VlmApiServiceMethodDescriptorSupplier("GetAccounts"))
                                    .build();
                }
            }
        }
        return getGetAccountsMethod;
    }

    @io.grpc.stub.annotations.RpcMethod(
            fullMethodName = SERVICE_NAME + '/' + "GetMiningInfo",
            requestType = com.google.protobuf.Empty.class,
            responseType = val.grpc.proto.VlmApi.MiningInfo.class,
            methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
    public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
            val.grpc.proto.VlmApi.MiningInfo> getGetMiningInfoMethod() {
        io.grpc.MethodDescriptor<com.google.protobuf.Empty, val.grpc.proto.VlmApi.MiningInfo> getGetMiningInfoMethod;
        if ((getGetMiningInfoMethod = VlmApiServiceGrpc.getGetMiningInfoMethod) == null) {
            synchronized (VlmApiServiceGrpc.class) {
                if ((getGetMiningInfoMethod = VlmApiServiceGrpc.getGetMiningInfoMethod) == null) {
                    VlmApiServiceGrpc.getGetMiningInfoMethod = getGetMiningInfoMethod =
                            io.grpc.MethodDescriptor.<com.google.protobuf.Empty, val.grpc.proto.VlmApi.MiningInfo>newBuilder()
                                    .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
                                    .setFullMethodName(generateFullMethodName(
                                            "VlmApiService", "GetMiningInfo"))
                                    .setSampledToLocalTracing(true)
                                    .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                                            com.google.protobuf.Empty.getDefaultInstance()))
                                    .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                                            val.grpc.proto.VlmApi.MiningInfo.getDefaultInstance()))
                                    .setSchemaDescriptor(new VlmApiServiceMethodDescriptorSupplier("GetMiningInfo"))
                                    .build();
                }
            }
        }
        return getGetMiningInfoMethod;
    }

    @io.grpc.stub.annotations.RpcMethod(
            fullMethodName = SERVICE_NAME + '/' + "GetTransaction",
            requestType = val.grpc.proto.VlmApi.GetTransactionRequest.class,
            responseType = val.grpc.proto.VlmApi.Transaction.class,
            methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<val.grpc.proto.VlmApi.GetTransactionRequest,
            val.grpc.proto.VlmApi.Transaction> getGetTransactionMethod() {
        io.grpc.MethodDescriptor<val.grpc.proto.VlmApi.GetTransactionRequest, val.grpc.proto.VlmApi.Transaction> getGetTransactionMethod;
        if ((getGetTransactionMethod = VlmApiServiceGrpc.getGetTransactionMethod) == null) {
            synchronized (VlmApiServiceGrpc.class) {
                if ((getGetTransactionMethod = VlmApiServiceGrpc.getGetTransactionMethod) == null) {
                    VlmApiServiceGrpc.getGetTransactionMethod = getGetTransactionMethod =
                            io.grpc.MethodDescriptor.<val.grpc.proto.VlmApi.GetTransactionRequest, val.grpc.proto.VlmApi.Transaction>newBuilder()
                                    .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                                    .setFullMethodName(generateFullMethodName(
                                            "VlmApiService", "GetTransaction"))
                                    .setSampledToLocalTracing(true)
                                    .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                                            val.grpc.proto.VlmApi.GetTransactionRequest.getDefaultInstance()))
                                    .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                                            val.grpc.proto.VlmApi.Transaction.getDefaultInstance()))
                                    .setSchemaDescriptor(new VlmApiServiceMethodDescriptorSupplier("GetTransaction"))
                                    .build();
                }
            }
        }
        return getGetTransactionMethod;
    }

    @io.grpc.stub.annotations.RpcMethod(
            fullMethodName = SERVICE_NAME + '/' + "GetTransactionBytes",
            requestType = val.grpc.proto.VlmApi.GetTransactionRequest.class,
            responseType = val.grpc.proto.VlmApi.TransactionBytes.class,
            methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<val.grpc.proto.VlmApi.GetTransactionRequest,
            val.grpc.proto.VlmApi.TransactionBytes> getGetTransactionBytesMethod() {
        io.grpc.MethodDescriptor<val.grpc.proto.VlmApi.GetTransactionRequest, val.grpc.proto.VlmApi.TransactionBytes> getGetTransactionBytesMethod;
        if ((getGetTransactionBytesMethod = VlmApiServiceGrpc.getGetTransactionBytesMethod) == null) {
            synchronized (VlmApiServiceGrpc.class) {
                if ((getGetTransactionBytesMethod = VlmApiServiceGrpc.getGetTransactionBytesMethod) == null) {
                    VlmApiServiceGrpc.getGetTransactionBytesMethod = getGetTransactionBytesMethod =
                            io.grpc.MethodDescriptor.<val.grpc.proto.VlmApi.GetTransactionRequest, val.grpc.proto.VlmApi.TransactionBytes>newBuilder()
                                    .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                                    .setFullMethodName(generateFullMethodName(
                                            "VlmApiService", "GetTransactionBytes"))
                                    .setSampledToLocalTracing(true)
                                    .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                                            val.grpc.proto.VlmApi.GetTransactionRequest.getDefaultInstance()))
                                    .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                                            val.grpc.proto.VlmApi.TransactionBytes.getDefaultInstance()))
                                    .setSchemaDescriptor(new VlmApiServiceMethodDescriptorSupplier("GetTransactionBytes"))
                                    .build();
                }
            }
        }
        return getGetTransactionBytesMethod;
    }

    @io.grpc.stub.annotations.RpcMethod(
            fullMethodName = SERVICE_NAME + '/' + "SubmitNonce",
            requestType = val.grpc.proto.VlmApi.SubmitNonceRequest.class,
            responseType = val.grpc.proto.VlmApi.SubmitNonceResponse.class,
            methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<val.grpc.proto.VlmApi.SubmitNonceRequest,
            val.grpc.proto.VlmApi.SubmitNonceResponse> getSubmitNonceMethod() {
        io.grpc.MethodDescriptor<val.grpc.proto.VlmApi.SubmitNonceRequest, val.grpc.proto.VlmApi.SubmitNonceResponse> getSubmitNonceMethod;
        if ((getSubmitNonceMethod = VlmApiServiceGrpc.getSubmitNonceMethod) == null) {
            synchronized (VlmApiServiceGrpc.class) {
                if ((getSubmitNonceMethod = VlmApiServiceGrpc.getSubmitNonceMethod) == null) {
                    VlmApiServiceGrpc.getSubmitNonceMethod = getSubmitNonceMethod =
                            io.grpc.MethodDescriptor.<val.grpc.proto.VlmApi.SubmitNonceRequest, val.grpc.proto.VlmApi.SubmitNonceResponse>newBuilder()
                                    .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                                    .setFullMethodName(generateFullMethodName(
                                            "VlmApiService", "SubmitNonce"))
                                    .setSampledToLocalTracing(true)
                                    .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                                            val.grpc.proto.VlmApi.SubmitNonceRequest.getDefaultInstance()))
                                    .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                                            val.grpc.proto.VlmApi.SubmitNonceResponse.getDefaultInstance()))
                                    .setSchemaDescriptor(new VlmApiServiceMethodDescriptorSupplier("SubmitNonce"))
                                    .build();
                }
            }
        }
        return getSubmitNonceMethod;
    }

    /**
     * Creates a new async stub that supports all call types for the service
     */
    public static VlmApiServiceStub newStub(io.grpc.Channel channel) {
        return new VlmApiServiceStub(channel);
    }

    /**
     * Creates a new blocking-style stub that supports unary and streaming output calls on the service
     */
    public static VlmApiServiceBlockingStub newBlockingStub(
            io.grpc.Channel channel) {
        return new VlmApiServiceBlockingStub(channel);
    }

    /**
     * Creates a new ListenableFuture-style stub that supports unary calls on the service
     */
    public static VlmApiServiceFutureStub newFutureStub(
            io.grpc.Channel channel) {
        return new VlmApiServiceFutureStub(channel);
    }

    public static io.grpc.ServiceDescriptor getServiceDescriptor() {
        io.grpc.ServiceDescriptor result = serviceDescriptor;
        if (result == null) {
            synchronized (VlmApiServiceGrpc.class) {
                result = serviceDescriptor;
                if (result == null) {
                    serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
                            .setSchemaDescriptor(new VlmApiServiceFileDescriptorSupplier())
                            .addMethod(getGetBlockMethod())
                            .addMethod(getGetAccountMethod())
                            .addMethod(getGetAccountsMethod())
                            .addMethod(getGetMiningInfoMethod())
                            .addMethod(getGetTransactionMethod())
                            .addMethod(getGetTransactionBytesMethod())
                            .addMethod(getSubmitNonceMethod())
                            .build();
                }
            }
        }
        return result;
    }

    /**
     *
     */
    public static abstract class VlmApiServiceImplBase implements io.grpc.BindableService {

        /**
         *
         */
        public void getBlock(val.grpc.proto.VlmApi.GetBlockRequest request,
                             io.grpc.stub.StreamObserver<val.grpc.proto.VlmApi.Block> responseObserver) {
            asyncUnimplementedUnaryCall(getGetBlockMethod(), responseObserver);
        }

        /**
         *
         */
        public void getAccount(val.grpc.proto.VlmApi.GetAccountRequest request,
                               io.grpc.stub.StreamObserver<val.grpc.proto.VlmApi.Account> responseObserver) {
            asyncUnimplementedUnaryCall(getGetAccountMethod(), responseObserver);
        }

        /**
         *
         */
        public void getAccounts(val.grpc.proto.VlmApi.GetAccountsRequest request,
                                io.grpc.stub.StreamObserver<val.grpc.proto.VlmApi.Accounts> responseObserver) {
            asyncUnimplementedUnaryCall(getGetAccountsMethod(), responseObserver);
        }

        /**
         *
         */
        public void getMiningInfo(com.google.protobuf.Empty request,
                                  io.grpc.stub.StreamObserver<val.grpc.proto.VlmApi.MiningInfo> responseObserver) {
            asyncUnimplementedUnaryCall(getGetMiningInfoMethod(), responseObserver);
        }

        /**
         *
         */
        public void getTransaction(val.grpc.proto.VlmApi.GetTransactionRequest request,
                                   io.grpc.stub.StreamObserver<val.grpc.proto.VlmApi.Transaction> responseObserver) {
            asyncUnimplementedUnaryCall(getGetTransactionMethod(), responseObserver);
        }

        /**
         *
         */
        public void getTransactionBytes(val.grpc.proto.VlmApi.GetTransactionRequest request,
                                        io.grpc.stub.StreamObserver<val.grpc.proto.VlmApi.TransactionBytes> responseObserver) {
            asyncUnimplementedUnaryCall(getGetTransactionBytesMethod(), responseObserver);
        }

        /**
         *
         */
        public void submitNonce(val.grpc.proto.VlmApi.SubmitNonceRequest request,
                                io.grpc.stub.StreamObserver<val.grpc.proto.VlmApi.SubmitNonceResponse> responseObserver) {
            asyncUnimplementedUnaryCall(getSubmitNonceMethod(), responseObserver);
        }

        @java.lang.Override
        public final io.grpc.ServerServiceDefinition bindService() {
            return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
                    .addMethod(
                            getGetBlockMethod(),
                            asyncUnaryCall(
                                    new MethodHandlers<
                                            val.grpc.proto.VlmApi.GetBlockRequest,
                                            val.grpc.proto.VlmApi.Block>(
                                            this, METHODID_GET_BLOCK)))
                    .addMethod(
                            getGetAccountMethod(),
                            asyncUnaryCall(
                                    new MethodHandlers<
                                            val.grpc.proto.VlmApi.GetAccountRequest,
                                            val.grpc.proto.VlmApi.Account>(
                                            this, METHODID_GET_ACCOUNT)))
                    .addMethod(
                            getGetAccountsMethod(),
                            asyncUnaryCall(
                                    new MethodHandlers<
                                            val.grpc.proto.VlmApi.GetAccountsRequest,
                                            val.grpc.proto.VlmApi.Accounts>(
                                            this, METHODID_GET_ACCOUNTS)))
                    .addMethod(
                            getGetMiningInfoMethod(),
                            asyncServerStreamingCall(
                                    new MethodHandlers<
                                            com.google.protobuf.Empty,
                                            val.grpc.proto.VlmApi.MiningInfo>(
                                            this, METHODID_GET_MINING_INFO)))
                    .addMethod(
                            getGetTransactionMethod(),
                            asyncUnaryCall(
                                    new MethodHandlers<
                                            val.grpc.proto.VlmApi.GetTransactionRequest,
                                            val.grpc.proto.VlmApi.Transaction>(
                                            this, METHODID_GET_TRANSACTION)))
                    .addMethod(
                            getGetTransactionBytesMethod(),
                            asyncUnaryCall(
                                    new MethodHandlers<
                                            val.grpc.proto.VlmApi.GetTransactionRequest,
                                            val.grpc.proto.VlmApi.TransactionBytes>(
                                            this, METHODID_GET_TRANSACTION_BYTES)))
                    .addMethod(
                            getSubmitNonceMethod(),
                            asyncUnaryCall(
                                    new MethodHandlers<
                                            val.grpc.proto.VlmApi.SubmitNonceRequest,
                                            val.grpc.proto.VlmApi.SubmitNonceResponse>(
                                            this, METHODID_SUBMIT_NONCE)))
                    .build();
        }
    }

    /**
     *
     */
    public static final class VlmApiServiceStub extends io.grpc.stub.AbstractStub<VlmApiServiceStub> {
        private VlmApiServiceStub(io.grpc.Channel channel) {
            super(channel);
        }

        private VlmApiServiceStub(io.grpc.Channel channel,
                                  io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
        }

        @java.lang.Override
        protected VlmApiServiceStub build(io.grpc.Channel channel,
                                          io.grpc.CallOptions callOptions) {
            return new VlmApiServiceStub(channel, callOptions);
        }

        /**
         *
         */
        public void getBlock(val.grpc.proto.VlmApi.GetBlockRequest request,
                             io.grpc.stub.StreamObserver<val.grpc.proto.VlmApi.Block> responseObserver) {
            asyncUnaryCall(
                    getChannel().newCall(getGetBlockMethod(), getCallOptions()), request, responseObserver);
        }

        /**
         *
         */
        public void getAccount(val.grpc.proto.VlmApi.GetAccountRequest request,
                               io.grpc.stub.StreamObserver<val.grpc.proto.VlmApi.Account> responseObserver) {
            asyncUnaryCall(
                    getChannel().newCall(getGetAccountMethod(), getCallOptions()), request, responseObserver);
        }

        /**
         *
         */
        public void getAccounts(val.grpc.proto.VlmApi.GetAccountsRequest request,
                                io.grpc.stub.StreamObserver<val.grpc.proto.VlmApi.Accounts> responseObserver) {
            asyncUnaryCall(
                    getChannel().newCall(getGetAccountsMethod(), getCallOptions()), request, responseObserver);
        }

        /**
         *
         */
        public void getMiningInfo(com.google.protobuf.Empty request,
                                  io.grpc.stub.StreamObserver<val.grpc.proto.VlmApi.MiningInfo> responseObserver) {
            asyncServerStreamingCall(
                    getChannel().newCall(getGetMiningInfoMethod(), getCallOptions()), request, responseObserver);
        }

        /**
         *
         */
        public void getTransaction(val.grpc.proto.VlmApi.GetTransactionRequest request,
                                   io.grpc.stub.StreamObserver<val.grpc.proto.VlmApi.Transaction> responseObserver) {
            asyncUnaryCall(
                    getChannel().newCall(getGetTransactionMethod(), getCallOptions()), request, responseObserver);
        }

        /**
         *
         */
        public void getTransactionBytes(val.grpc.proto.VlmApi.GetTransactionRequest request,
                                        io.grpc.stub.StreamObserver<val.grpc.proto.VlmApi.TransactionBytes> responseObserver) {
            asyncUnaryCall(
                    getChannel().newCall(getGetTransactionBytesMethod(), getCallOptions()), request, responseObserver);
        }

        /**
         *
         */
        public void submitNonce(val.grpc.proto.VlmApi.SubmitNonceRequest request,
                                io.grpc.stub.StreamObserver<val.grpc.proto.VlmApi.SubmitNonceResponse> responseObserver) {
            asyncUnaryCall(
                    getChannel().newCall(getSubmitNonceMethod(), getCallOptions()), request, responseObserver);
        }
    }

    /**
     *
     */
    public static final class VlmApiServiceBlockingStub extends io.grpc.stub.AbstractStub<VlmApiServiceBlockingStub> {
        private VlmApiServiceBlockingStub(io.grpc.Channel channel) {
            super(channel);
        }

        private VlmApiServiceBlockingStub(io.grpc.Channel channel,
                                          io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
        }

        @java.lang.Override
        protected VlmApiServiceBlockingStub build(io.grpc.Channel channel,
                                                  io.grpc.CallOptions callOptions) {
            return new VlmApiServiceBlockingStub(channel, callOptions);
        }

        /**
         *
         */
        public val.grpc.proto.VlmApi.Block getBlock(val.grpc.proto.VlmApi.GetBlockRequest request) {
            return blockingUnaryCall(
                    getChannel(), getGetBlockMethod(), getCallOptions(), request);
        }

        /**
         *
         */
        public val.grpc.proto.VlmApi.Account getAccount(val.grpc.proto.VlmApi.GetAccountRequest request) {
            return blockingUnaryCall(
                    getChannel(), getGetAccountMethod(), getCallOptions(), request);
        }

        /**
         *
         */
        public val.grpc.proto.VlmApi.Accounts getAccounts(val.grpc.proto.VlmApi.GetAccountsRequest request) {
            return blockingUnaryCall(
                    getChannel(), getGetAccountsMethod(), getCallOptions(), request);
        }

        /**
         *
         */
        public java.util.Iterator<val.grpc.proto.VlmApi.MiningInfo> getMiningInfo(
                com.google.protobuf.Empty request) {
            return blockingServerStreamingCall(
                    getChannel(), getGetMiningInfoMethod(), getCallOptions(), request);
        }

        /**
         *
         */
        public val.grpc.proto.VlmApi.Transaction getTransaction(val.grpc.proto.VlmApi.GetTransactionRequest request) {
            return blockingUnaryCall(
                    getChannel(), getGetTransactionMethod(), getCallOptions(), request);
        }

        /**
         *
         */
        public val.grpc.proto.VlmApi.TransactionBytes getTransactionBytes(val.grpc.proto.VlmApi.GetTransactionRequest request) {
            return blockingUnaryCall(
                    getChannel(), getGetTransactionBytesMethod(), getCallOptions(), request);
        }

        /**
         *
         */
        public val.grpc.proto.VlmApi.SubmitNonceResponse submitNonce(val.grpc.proto.VlmApi.SubmitNonceRequest request) {
            return blockingUnaryCall(
                    getChannel(), getSubmitNonceMethod(), getCallOptions(), request);
        }
    }

    /**
     *
     */
    public static final class VlmApiServiceFutureStub extends io.grpc.stub.AbstractStub<VlmApiServiceFutureStub> {
        private VlmApiServiceFutureStub(io.grpc.Channel channel) {
            super(channel);
        }

        private VlmApiServiceFutureStub(io.grpc.Channel channel,
                                        io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
        }

        @java.lang.Override
        protected VlmApiServiceFutureStub build(io.grpc.Channel channel,
                                                io.grpc.CallOptions callOptions) {
            return new VlmApiServiceFutureStub(channel, callOptions);
        }

        /**
         *
         */
        public com.google.common.util.concurrent.ListenableFuture<val.grpc.proto.VlmApi.Block> getBlock(
                val.grpc.proto.VlmApi.GetBlockRequest request) {
            return futureUnaryCall(
                    getChannel().newCall(getGetBlockMethod(), getCallOptions()), request);
        }

        /**
         *
         */
        public com.google.common.util.concurrent.ListenableFuture<val.grpc.proto.VlmApi.Account> getAccount(
                val.grpc.proto.VlmApi.GetAccountRequest request) {
            return futureUnaryCall(
                    getChannel().newCall(getGetAccountMethod(), getCallOptions()), request);
        }

        /**
         *
         */
        public com.google.common.util.concurrent.ListenableFuture<val.grpc.proto.VlmApi.Accounts> getAccounts(
                val.grpc.proto.VlmApi.GetAccountsRequest request) {
            return futureUnaryCall(
                    getChannel().newCall(getGetAccountsMethod(), getCallOptions()), request);
        }

        /**
         *
         */
        public com.google.common.util.concurrent.ListenableFuture<val.grpc.proto.VlmApi.Transaction> getTransaction(
                val.grpc.proto.VlmApi.GetTransactionRequest request) {
            return futureUnaryCall(
                    getChannel().newCall(getGetTransactionMethod(), getCallOptions()), request);
        }

        /**
         *
         */
        public com.google.common.util.concurrent.ListenableFuture<val.grpc.proto.VlmApi.TransactionBytes> getTransactionBytes(
                val.grpc.proto.VlmApi.GetTransactionRequest request) {
            return futureUnaryCall(
                    getChannel().newCall(getGetTransactionBytesMethod(), getCallOptions()), request);
        }

        /**
         *
         */
        public com.google.common.util.concurrent.ListenableFuture<val.grpc.proto.VlmApi.SubmitNonceResponse> submitNonce(
                val.grpc.proto.VlmApi.SubmitNonceRequest request) {
            return futureUnaryCall(
                    getChannel().newCall(getSubmitNonceMethod(), getCallOptions()), request);
        }
    }

    private static final class MethodHandlers<Req, Resp> implements
            io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
            io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
            io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
            io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
        private final VlmApiServiceImplBase serviceImpl;
        private final int methodId;

        MethodHandlers(VlmApiServiceImplBase serviceImpl, int methodId) {
            this.serviceImpl = serviceImpl;
            this.methodId = methodId;
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("unchecked")
        public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
            switch (methodId) {
                case METHODID_GET_BLOCK:
                    serviceImpl.getBlock((val.grpc.proto.VlmApi.GetBlockRequest) request,
                            (io.grpc.stub.StreamObserver<val.grpc.proto.VlmApi.Block>) responseObserver);
                    break;
                case METHODID_GET_ACCOUNT:
                    serviceImpl.getAccount((val.grpc.proto.VlmApi.GetAccountRequest) request,
                            (io.grpc.stub.StreamObserver<val.grpc.proto.VlmApi.Account>) responseObserver);
                    break;
                case METHODID_GET_ACCOUNTS:
                    serviceImpl.getAccounts((val.grpc.proto.VlmApi.GetAccountsRequest) request,
                            (io.grpc.stub.StreamObserver<val.grpc.proto.VlmApi.Accounts>) responseObserver);
                    break;
                case METHODID_GET_MINING_INFO:
                    serviceImpl.getMiningInfo((com.google.protobuf.Empty) request,
                            (io.grpc.stub.StreamObserver<val.grpc.proto.VlmApi.MiningInfo>) responseObserver);
                    break;
                case METHODID_GET_TRANSACTION:
                    serviceImpl.getTransaction((val.grpc.proto.VlmApi.GetTransactionRequest) request,
                            (io.grpc.stub.StreamObserver<val.grpc.proto.VlmApi.Transaction>) responseObserver);
                    break;
                case METHODID_GET_TRANSACTION_BYTES:
                    serviceImpl.getTransactionBytes((val.grpc.proto.VlmApi.GetTransactionRequest) request,
                            (io.grpc.stub.StreamObserver<val.grpc.proto.VlmApi.TransactionBytes>) responseObserver);
                    break;
                case METHODID_SUBMIT_NONCE:
                    serviceImpl.submitNonce((val.grpc.proto.VlmApi.SubmitNonceRequest) request,
                            (io.grpc.stub.StreamObserver<val.grpc.proto.VlmApi.SubmitNonceResponse>) responseObserver);
                    break;
                default:
                    throw new AssertionError();
            }
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("unchecked")
        public io.grpc.stub.StreamObserver<Req> invoke(
                io.grpc.stub.StreamObserver<Resp> responseObserver) {
            switch (methodId) {
                default:
                    throw new AssertionError();
            }
        }
    }

    private static abstract class VlmApiServiceBaseDescriptorSupplier
            implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
        VlmApiServiceBaseDescriptorSupplier() {
        }

        @java.lang.Override
        public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
            return val.grpc.proto.VlmApi.getDescriptor();
        }

        @java.lang.Override
        public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
            return getFileDescriptor().findServiceByName("VlmApiService");
        }
    }

    private static final class VlmApiServiceFileDescriptorSupplier
            extends VlmApiServiceBaseDescriptorSupplier {
        VlmApiServiceFileDescriptorSupplier() {
        }
    }

    private static final class VlmApiServiceMethodDescriptorSupplier
            extends VlmApiServiceBaseDescriptorSupplier
            implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
        private final String methodName;

        VlmApiServiceMethodDescriptorSupplier(String methodName) {
            this.methodName = methodName;
        }

        @java.lang.Override
        public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
            return getServiceDescriptor().findMethodByName(methodName);
        }
    }
}
