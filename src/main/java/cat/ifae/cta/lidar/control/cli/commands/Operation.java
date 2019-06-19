package cat.ifae.cta.lidar.control.cli.commands;

import cat.ifae.cta.lidar.EncoderPosition;
import cat.ifae.cta.lidar.Null;
import cat.ifae.cta.lidar.OperationGrpc;
import cat.ifae.cta.lidar.control.cli.Licli;
import io.grpc.stub.StreamObserver;
import picocli.CommandLine;

import java.util.concurrent.CountDownLatch;

@CommandLine.Command(name = "operation", mixinStandardHelpOptions = true)
public class Operation implements Runnable {
    @CommandLine.Option(names = "-telescope-test", description = "Execute telescope test")
    private boolean telescope_test = false;

    @CommandLine.Option(names = "-stop-telescope-test", description = "Stop telescope test")
    private boolean stop_telescope_test = false;

    @CommandLine.ParentCommand
    private Licli parent;

    private OperationGrpc.OperationStub stub;
    private OperationGrpc.OperationBlockingStub blocking_stub;

    @Override
    public void run() {
        stub = OperationGrpc.newStub(parent.sm.getCh());
        stub = parent.sm.addMetadata(stub);

        blocking_stub = OperationGrpc.newBlockingStub(parent.sm.getCh());
        blocking_stub = parent.sm.addMetadata(blocking_stub);

        CommandLine.populateCommand(this);

        try {
            if(telescope_test) executeTelescopeTests();
            if(stop_telescope_test) stopTelescopeTests();
        } catch(Exception e) {
            System.out.println(e.toString());
        }
    }

    private void executeTelescopeTests() throws InterruptedException {
        var finishedLatch = new CountDownLatch(1);

        Null req = Null.newBuilder().build();
        stub.executeTelescopeTests(req, new StreamObserver<>() {
            public void onNext(EncoderPosition response) {
                System.out.println(response.getName() + ": " + response.getPosition());
            }

            public void onError(Throwable t) {
                System.out.println("Error: " + t.getMessage());
                finishedLatch.countDown();
            }

            public void onCompleted() {
                finishedLatch.countDown();
            }
        });

        finishedLatch.await();
    }

    private void stopTelescopeTests() {
        Null req = Null.newBuilder().build();
        blocking_stub.stopTelescopeTests(req);
    }
}
