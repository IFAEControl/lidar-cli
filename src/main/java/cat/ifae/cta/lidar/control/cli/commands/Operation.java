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
    @CommandLine.ParentCommand
    private Licli parent;

    private OperationGrpc.OperationStub stub;

    @Override
    public void run() {
        stub = OperationGrpc.newStub(parent.sm.getCh());
        stub = parent.sm.addMetadata(stub);

        CommandLine.populateCommand(this);

        try {
            var finishedLatch = new CountDownLatch(1);

            Null req = Null.newBuilder().build();
            stub.executeTelescopTests(req, new StreamObserver<>() {
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
        } catch(Exception e) {
            System.out.println(e.toString());
        }
    }
}
