package cat.ifae.cta.lidar.control.cli.commands;

import cat.ifae.cta.lidar.Null;
import cat.ifae.cta.lidar.SystemGrpc;
import cat.ifae.cta.lidar.control.cli.Licli;
import cat.ifae.cta.lidar.logging.Logging;
import io.grpc.StatusRuntimeException;
import picocli.CommandLine;

@CommandLine.Command(name = "system", description = "System", mixinStandardHelpOptions = true)
public class SystemInfo implements Runnable {
   private final static Logging _log = new Logging(Trace.class);

   private SystemGrpc.SystemBlockingStub stub;

   @CommandLine.Option(names = "--get-moxa-pins")
   boolean _get_moxa = false;

   @Override
   public void run(){
      stub = SystemGrpc.newBlockingStub(Licli.sm.getCh());
      stub = Licli.sm.addMetadata(stub);

      CommandLine.populateCommand(this);

      try {
         if(_get_moxa) {
            var req = Null.newBuilder().build();
            var resp = stub.getMoxaPins(req);
            System.out.println(resp);
         }
      } catch (StatusRuntimeException e) {
         _log.error(e.getLocalizedMessage());
      } catch (Exception e) {
         System.out.println(e.toString());
      }
   }
}