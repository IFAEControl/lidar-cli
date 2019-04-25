package cat.ifae.cta.lidar.control.cli.session;

import io.grpc.ManagedChannel;
import io.grpc.stub.AbstractStub;


public class SessionManager {
    private static gRPCManager grpc;

    public SessionManager() {
        // XXX
        var password = getEnv("LIDAR_PASSWORD");
        var ip = getEnv("LIDAR_ADDR");

        grpc = new gRPCManager(ip, 50051, password);
    }

    private String getEnv(String s) {
        var ip = System.getenv(s);
        if(ip == null || ip.isBlank())
            throw new RuntimeException(s + "env variable is not set");

        return ip;
    }

    public void shutdown() throws InterruptedException {
        grpc.shutdown();
    }

    public <T extends AbstractStub<T>> T addMetadata(T stub) {
        return grpc.addMetadata(stub);
    }

    public ManagedChannel getCh() {
        return grpc.channel;
    }

    public gRPCManager getGrpc() {
        return grpc;
    }
}
