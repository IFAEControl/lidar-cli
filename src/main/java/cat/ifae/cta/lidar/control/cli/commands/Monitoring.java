package cat.ifae.cta.lidar.control.cli.commands;

import cat.ifae.cta.lidar.*;
import cat.ifae.cta.lidar.control.cli.Licli;
import io.grpc.stub.StreamObserver;
import picocli.CommandLine;

import java.util.concurrent.CountDownLatch;

@CommandLine.Command(name = "monitoring", description = "Monitoring commands", mixinStandardHelpOptions = true)
public
class Monitoring implements Runnable {
    private MonitoringGrpc.MonitoringStub stub;
    private MonitoringGrpc.MonitoringBlockingStub blockingStub;

    @CommandLine.Option(names = "--humidity")
    private boolean humidity = false;

    @CommandLine.Option(names = "--env-temperature")
    private boolean env_temperature = false;

    @CommandLine.Option(names = "--last-value", description = "Get last value")
    private boolean last_value = false;

    @Override
    public final void run() {
        stub = MonitoringGrpc.newStub(Licli.sm.getCh());
        stub = Licli.sm.addMetadata(stub);

        blockingStub = MonitoringGrpc.newBlockingStub(Licli.sm.getCh());
        blockingStub = Licli.sm.addMetadata(blockingStub);

        CommandLine.populateCommand(this);

        try {
            String name;
            if(humidity) name = "humidity";
            else if(env_temperature) name = "temperature";
            else
                throw new RuntimeException("Select one variable (and only one)");


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
        stub.watchVariable(req, new StreamObserver<>() {
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
