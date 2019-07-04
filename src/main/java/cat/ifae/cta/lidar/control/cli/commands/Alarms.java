package cat.ifae.cta.lidar.control.cli.commands;

import cat.ifae.cta.lidar.AlarmMessage;
import cat.ifae.cta.lidar.AlarmsGrpc;
import cat.ifae.cta.lidar.Null;
import cat.ifae.cta.lidar.control.cli.Licli;
import io.grpc.stub.StreamObserver;
import picocli.CommandLine;

import java.util.concurrent.CountDownLatch;

@CommandLine.Command(name = "alarms", description = "Alarms commands", mixinStandardHelpOptions = true)
public class Alarms implements Runnable {
    private AlarmsGrpc.AlarmsStub stub;

    @Override
    public void run() {
        stub = AlarmsGrpc.newStub(Licli.sm.getCh());
        stub = Licli.sm.addMetadata(stub);

        CommandLine.populateCommand(this);

        try {
            //var requestObserverRef = new AtomicReference<>();
            var finishedLatch = new CountDownLatch(1);
            Null req = Null.newBuilder().build();
            stub.listenAlarms(req, new StreamObserver<>() {
                public void onNext(AlarmMessage response) {
                    System.out.println(response.getMsg());
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
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }
}
