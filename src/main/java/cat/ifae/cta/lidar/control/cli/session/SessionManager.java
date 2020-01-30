package cat.ifae.cta.lidar.control.cli.session;

import cat.ifae.cta.lidar.AppDirs;
import cat.ifae.cta.lidar.AuthGrpc;
import cat.ifae.cta.lidar.Helpers;
import cat.ifae.cta.lidar.Password;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.AbstractStub;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

// TODO: Handle error if token has not valid characters

public class SessionManager {
    private static gRPCManager grpc;

    private final static AppDirs appDirs = new AppDirs();
    private Scanner scanner = new Scanner(System. in);

    public SessionManager() {
        var ip = Helpers.getEnv("LIDAR_ADDR");
        grpc = new gRPCManager(ip, 50051);

        initializeToken();
        scanner.close();
    }

    private void initializeToken() {
        try {
            // After initializing gRPC get the auth token
            var token = retrieveToken();
            grpc.setToken(token);
        } catch(StatusRuntimeException e) {
            System.err.println("Error: " + e.getMessage());
            System.err.println("Try again");
            System.err.flush();
            initializeToken();
        }
    }

    private String retrieveToken() {
        var cache_dir = appDirs.getUserCacheDir();
        var token_cache = cache_dir + "/token";

        // First try to read the token from the cache file
        try {
            return Files.readString(Paths.get(token_cache));
        } catch (IOException e) {
            //System.err.println("Token not cached, requesting a new one");
            //System.err.flush();
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

        var password = scanner.nextLine();

        Password req = Password.newBuilder().setStr(password).build();
        var stub = AuthGrpc.newBlockingStub(grpc.channel);
        stub = addMetadata(stub);
        var resp = stub.getToken(req);
        return resp.getStr();
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
