package cat.ifae.cta.lidar.control.cli.commands;

import cat.ifae.cta.lidar.*;
import cat.ifae.cta.lidar.control.cli.Control;
import io.grpc.stub.StreamObserver;
import picocli.CommandLine;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

@CommandLine.Command(name = "monitoring", mixinStandardHelpOptions = true)
public
class Monitoring implements Runnable {
    private MonitoringGrpc.MonitoringStub stub;

    @CommandLine.ParentCommand
    private Control parent;

    @CommandLine.Option(names = "-gs", description = "Get status")
    private boolean get_status;

    @Override
    public final void run() {
        stub = MonitoringGrpc.newStub(parent.grpc.channel);
        stub = parent.grpc.addMetadata(stub);

        CommandLine.populateCommand(this);

        try {
            //var requestObserverRef = new AtomicReference<>();
            var finishedLatch = new CountDownLatch(1);
            Str req = Str.newBuilder().setText("a").build();
            stub.variable(req, new StreamObserver<Str>() {
                public void onNext(Str response) {
                    System.out.println(response);
                }
                public void onError(Throwable t) {
                    System.out.println("on error");
                    t.printStackTrace();
                }
                public void onCompleted() {
                    System.out.println("A");
                    finishedLatch.countDown();
                }
            });
            finishedLatch.await();
        } catch(InterruptedException | RuntimeException e) {
            System.out.println(e.toString());
        }

    }
}
