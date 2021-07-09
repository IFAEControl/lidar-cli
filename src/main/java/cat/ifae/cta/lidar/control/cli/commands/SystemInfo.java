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
            System.out.println("access: " + resp.getAccess());
            System.out.println("btn door: " + resp.getBtnDoor());
            System.out.println("btn outside: " + resp.getBtnOutside());
            System.out.println("btn rack: " + resp.getBtnRack());
            System.out.println("key switch: " + resp.getKeySwitch());
            System.out.println("laser interlock: " + resp.getLaserInterlock());
            System.out.println("fence: " + resp.getFence());
            System.out.println("pk azimuth: " + resp.getPkAzimuth());
            System.out.println("pk zenith: " + resp.getPkZenith());
            System.out.println("tilt: " + resp.getTilt());
         }
      } catch (StatusRuntimeException e) {
         _log.error(e.getLocalizedMessage());
      } catch (Exception e) {
         System.out.println(e.toString());
      }
   }
}