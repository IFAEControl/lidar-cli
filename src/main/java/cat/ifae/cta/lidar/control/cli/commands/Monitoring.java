package cat.ifae.cta.lidar.control.cli.commands;

import cat.ifae.cta.lidar.*;
import cat.ifae.cta.lidar.control.cli.Licli;
import io.grpc.stub.StreamObserver;
import picocli.CommandLine;

import java.util.concurrent.CountDownLatch;

@CommandLine.Command(name = "monitoring", mixinStandardHelpOptions = true)
public
class Monitoring implements Runnable {
    private MonitoringGrpc.MonitoringStub stub;
    private MonitoringGrpc.MonitoringBlockingStub blockingStub;

    @CommandLine.Option(names = "-last-value", description = "Get last value")
    private boolean last_value = false;

    @CommandLine.ParentCommand
    private Licli parent;

    @Override
    public final void run() {
        stub = MonitoringGrpc.newStub(parent.sm.getCh());
        stub = parent.sm.addMetadata(stub);

        blockingStub = MonitoringGrpc.newBlockingStub(parent.sm.getCh());
        blockingStub = parent.sm.addMetadata(blockingStub);

        CommandLine.populateCommand(this);

        try {
            //var requestObserverRef = new AtomicReference<>();
            if(last_value) getLastValue();
            else monitoring();
        } catch(InterruptedException | RuntimeException e) {
            System.out.println(e.toString());
        }

    }

    private void getLastValue() {
        var request = Str.newBuilder().setText("temperature").build();
        var resp = blockingStub.getLastValue(request);
        System.out.println(resp.getText());
    }

    private void monitoring() throws InterruptedException {
        var finishedLatch = new CountDownLatch(1);
        Str req = Str.newBuilder().setText("temperature").build();
        stub.watchVariable(req, new StreamObserver<Str>() {
            public void onNext(Str response) {
                System.out.print(response);
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
}
