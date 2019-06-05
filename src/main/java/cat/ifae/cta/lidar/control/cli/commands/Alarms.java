package cat.ifae.cta.lidar.control.cli.commands;

import cat.ifae.cta.lidar.AlarmMessage;
import cat.ifae.cta.lidar.AlarmsGrpc;
import cat.ifae.cta.lidar.Null;
import cat.ifae.cta.lidar.control.cli.Licli;
import io.grpc.stub.StreamObserver;
import picocli.CommandLine;

import java.util.concurrent.CountDownLatch;

@CommandLine.Command(name = "alarms", mixinStandardHelpOptions = true)
public class Alarms implements Runnable {
    private AlarmsGrpc.AlarmsStub stub;

    @CommandLine.ParentCommand
    private Licli parent;

    @Override
    public void run() {
        stub = AlarmsGrpc.newStub(parent.sm.getCh());
        stub = parent.sm.addMetadata(stub);

        CommandLine.populateCommand(this);

        try {
            //var requestObserverRef = new AtomicReference<>();
            var finishedLatch = new CountDownLatch(1);
            Null req = Null.newBuilder().build();
            stub.listenAlarms(req, new StreamObserver<AlarmMessage>() {
                public void onNext(AlarmMessage response) {
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
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }
}
