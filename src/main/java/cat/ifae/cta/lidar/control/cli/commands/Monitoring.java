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

    @CommandLine.Parameters(paramLabel = "NAME", description = "Name")
    private String name;


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
            if(last_value) getLastValue(name);
            else monitoring(name);
        } catch(InterruptedException | RuntimeException e) {
            System.out.println(e.toString());
        }

    }

    private void getLastValue(String m) {
        var request = Str.newBuilder().setText(m).build();
        var resp = blockingStub.getLastValue(request);
        System.out.println(resp.getText());
    }

    private void monitoring(String m) throws InterruptedException {
        var finishedLatch = new CountDownLatch(1);
        Str req = Str.newBuilder().setText(m).build();
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
