package cat.ifae.cta.lidar.control.cli;

import cat.ifae.cta.lidar.*;
import cat.ifae.cta.lidar.Helpers;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.stub.AbstractStub;
import io.grpc.stub.MetadataUtils;
import picocli.CommandLine;


import java.util.concurrent.TimeUnit;

interface AbstractGrpc {
    <T> T newBlockingStub(io.grpc.ManagedChannel ch);
}

class gRPCControl {
    final ManagedChannel channel;
    private String token;

    gRPCControl(String host, int port, String password) {
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext().build());

        // Get Token
        var stub = AuthGrpc.newBlockingStub(channel);

        Password req = Password.newBuilder().setStr(password).build();
        var resp = stub.getToken(req);
        token = resp.getStr();
    }

    /*public  <T extends AbstractGrpc> T newStub(Object grpc) {
        return ((T) grpc).newBlockingStub(channel);
    }*/

    public <T extends AbstractStub<T>> T addMetadata(T stub) {
        Metadata header = new Metadata();
        var key = Metadata.Key.of("token", Metadata.ASCII_STRING_MARSHALLER);
        header.put(key, token);

        var type = Metadata.Key.of("type", Metadata.ASCII_STRING_MARSHALLER);
        header.put(type, "CLI");

        return MetadataUtils.attachHeaders(stub, header);
    }

    private gRPCControl(ManagedChannel channel) {
        this.channel = channel;
    }

    void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
}

@CommandLine.Command(mixinStandardHelpOptions = true, subcommands = {Motors.class, Relay.class, Sensors.class, Laser.class, HotWind.class,
        Arm.class, Dac.class, Drivers.class, Licel.class, Micro.class})
public class Control implements Runnable {
    static gRPCControl grpc;

    Control() {
        Helpers.configureGRPCLog();

        // XXX
        var password = System.getenv("LIDAR_PASSWORD");
        if(password == null || password.isBlank())
            throw new RuntimeException("LIDAR_PASSWORD env variable is not set");

        var ip = System.getenv("LIDAR_ADDR");
        if(ip == null || ip.isBlank())
            throw new RuntimeException("LIDAR_ADDR env variable is not set");

        grpc = new gRPCControl(ip, 50051, password);
    }

    public static void main(String[] args) throws InterruptedException {
        Control client = CommandLine.populateCommand(new Control(), args);

        CommandLine.run(client, System.err, args);
        grpc.shutdown();
    }

    @Override
    public void run() {
        CommandLine.usage(this, System.err);
    }
}