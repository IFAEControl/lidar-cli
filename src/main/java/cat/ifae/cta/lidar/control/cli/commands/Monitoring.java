package cat.ifae.cta.lidar.control.cli.commands;

import cat.ifae.cta.lidar.MotorsMonitoringGrpc;
import cat.ifae.cta.lidar.Null;
import cat.ifae.cta.lidar.SensorsMonitoringGrpc;
import cat.ifae.cta.lidar.Str;
import cat.ifae.cta.lidar.control.cli.Licli;
import cat.ifae.cta.lidar.logging.Logging;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import picocli.CommandLine;

import java.util.concurrent.CountDownLatch;

@CommandLine.Command(name = "sensors", description = "Sensors monitoring commands",
        mixinStandardHelpOptions = true)
class SensorsMonitoring implements Runnable {
   private final static Logging _log = new Logging(SensorsMonitoring.class);

   private SensorsMonitoringGrpc.SensorsMonitoringStub stub;
   private SensorsMonitoringGrpc.SensorsMonitoringBlockingStub blockingStub;

   @CommandLine.Option(names = "--humidity")
   private boolean humidity = false;

   @CommandLine.Option(names = "--env-temperature")
   private boolean env_temperature = false;

   @CommandLine.Option(names = "--last-value", description = "Get last value")
   private boolean last_value = false;

   @Override
   public final void run() {
      stub = SensorsMonitoringGrpc.newStub(Licli.sm.getCh());
      stub = Licli.sm.addMetadata(stub);

      blockingStub = SensorsMonitoringGrpc.newBlockingStub(Licli.sm.getCh());
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
      } catch (StatusRuntimeException e) {
         _log.error(e.getStatus().getCause().getLocalizedMessage());
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

@CommandLine.Command(name = "motors", description = "Motors monitoring commands",
        mixinStandardHelpOptions = true)
class MotorsMonitoring implements Runnable {
   private final static Logging _log = new Logging(MotorsMonitoring.class);

   private MotorsMonitoringGrpc.MotorsMonitoringBlockingStub blockingStub;

   @Override
   public final void run() {
      blockingStub = MotorsMonitoringGrpc.newBlockingStub(Licli.sm.getCh());
      blockingStub = Licli.sm.addMetadata(blockingStub);

      CommandLine.populateCommand(this);

      try {
         var req = Null.newBuilder().build();
         System.out.println(blockingStub.readEncoders(req));
      } catch (StatusRuntimeException e) {
         _log.error(e.getStatus().getCause().getLocalizedMessage());
      } catch(RuntimeException e) {
         System.out.println(e.toString());
      }
   }
}

@CommandLine.Command(name = "monitoring", description = "Monitoring commands",
        mixinStandardHelpOptions = true,
        subcommands = {MotorsMonitoring.class, SensorsMonitoring.class})
public class Monitoring implements Runnable {
   @Override
   public void run() {
      CommandLine.populateCommand(this);
   }
}
