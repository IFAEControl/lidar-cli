package cat.ifae.cta.lidar.control.cli.session;

import cat.ifae.cta.lidar.AuthGrpc;
import cat.ifae.cta.lidar.Password;
import io.grpc.ManagedChannel;
import io.grpc.stub.AbstractStub;
import net.harawata.appdirs.AppDirs;
import net.harawata.appdirs.AppDirsFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

// TODO: Handle error if token has not valid characters
// TODO: Handle received error if the token is invalid

public class SessionManager {
    private static gRPCManager grpc;

    private final static AppDirs appDirs = AppDirsFactory.getInstance();

    public SessionManager() {
        var ip = getEnv("LIDAR_ADDR");
        grpc = new gRPCManager(ip, 50051);

        // After initializing gRPC get the auth token
        var token = retrieveToken();
        grpc.setToken(token);
    }

    private String retrieveToken() {
        var cache_dir = appDirs.getUserCacheDir("lidar", "0.1", "ifae");
        var token_cache = cache_dir + "/token";

        System.out.println(token_cache);

        // First try to read the token from the cache file
        try {
            return Files.readString(Paths.get(token_cache));
        } catch (IOException e) {
            System.err.println("Requesting a new token bc cant read it from cache: " + e.toString());
            System.err.flush();
        }

        // If it fails request a new token and cache it
        var t = requestNewToken();
        try {
            var dir = new File(cache_dir);
            if(! dir.exists())
                dir.mkdirs();

            Files.write(Paths.get(token_cache), t.getBytes());
        } catch(IOException e) {
            System.err.println("Could not cache token");
            e.printStackTrace();
        }
        return t;
    }

    private String requestNewToken() {
        System.out.print("\nEnter password: ");
        System.out.flush();

        var scanner = new Scanner(System. in);
        var password = scanner.nextLine();
        scanner.close();

        Password req = Password.newBuilder().setStr(password).build();
        var stub = AuthGrpc.newBlockingStub(grpc.channel);
        var resp = stub.getToken(req);
        return resp.getStr();
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
