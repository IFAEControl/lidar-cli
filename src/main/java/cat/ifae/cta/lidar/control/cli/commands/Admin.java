package cat.ifae.cta.lidar.control.cli.commands;

import cat.ifae.cta.lidar.AdminGrpc;
import cat.ifae.cta.lidar.Null;
import cat.ifae.cta.lidar.control.cli.Licli;
import cat.ifae.cta.lidar.logging.Logging;
import io.grpc.StatusRuntimeException;
import picocli.CommandLine;

@CommandLine.Command(name = "admin", description = "Admin command", mixinStandardHelpOptions = true)
public class Admin implements Runnable {
   private final static Logging _log = new Logging(Alarms.class);

   private AdminGrpc.AdminBlockingStub stub;

   @CommandLine.Option(names = "--restart")
   boolean is_restart = false;

   @CommandLine.Option(names = "--stop")
   boolean is_stop = false;

   @Override
   public void run() {
      stub = AdminGrpc.newBlockingStub(Licli.sm.getCh());
      stub = Licli.sm.addMetadata(stub);

      CommandLine.populateCommand(this);

      CommandLine.populateCommand(this);

      try {
         if(is_restart) restart();
         else if(is_stop) stop();
      } catch (StatusRuntimeException e) {
         _log.error(e.getLocalizedMessage());
      } catch (Exception e) {
         System.out.println(e.toString());
      }
   }

   private void restart() {
      var req = Null.newBuilder().build();
      stub.restartServer(req);
   }

   private void stop() {
      var req = Null.newBuilder().build();
      stub.stop(req);
   }
}
