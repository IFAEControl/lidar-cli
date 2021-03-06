package cat.ifae.cta.lidar.control.cli.session;

import cat.ifae.cta.lidar.Helpers;
import cat.ifae.cta.lidar.control.cli.Configuration;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.grpc.stub.AbstractStub;
import io.grpc.stub.MetadataUtils;

import javax.net.ssl.SSLException;
import java.util.concurrent.TimeUnit;

interface AbstractGrpc {
    <T> T newBlockingStub(io.grpc.ManagedChannel ch);
}

class BaseGrpcManager {
    final ManagedChannel channel;
    private String token = "";

    <T> BaseGrpcManager(String host, int port, ManagedChannel grpc) throws SSLException {
        this(grpc);
    }

    /*public  <T extends AbstractGrpc> T newStub(Object grpc) {
        return ((T) grpc).newBlockingStub(channel);
    }*/

    private BaseGrpcManager(ManagedChannel channel) {
        this.channel = channel;
    }

    void setToken(String t) {
        if(!token.equals(""))
            throw new RuntimeException("Internal error: token was already set");

        token = t;
    }

    <T extends AbstractStub<T>> T addMetadata(T stub) {
        Metadata header = new Metadata();
        var key = Metadata.Key.of("token", Metadata.ASCII_STRING_MARSHALLER);
        header.put(key, token);

        var type = Metadata.Key.of("type", Metadata.ASCII_STRING_MARSHALLER);
        header.put(type, "cli");

        var version = Metadata.Key.of("version", Metadata.ASCII_STRING_MARSHALLER);
        header.put(version, Configuration.VERSION);

        return MetadataUtils.attachHeaders(stub, header);
    }

    void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
}

class GrpcManager extends BaseGrpcManager {
    GrpcManager(String host, int port) throws SSLException {
        super(host, port, NettyChannelBuilder.forAddress(host, port).sslContext(
                GrpcSslContexts.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build()).build());
    }
}

class InsecureGrpcManager extends BaseGrpcManager {
    InsecureGrpcManager(String host, int port) throws SSLException {
        super(host, port, ManagedChannelBuilder.forAddress(host, port).usePlaintext().build());
    }
}
