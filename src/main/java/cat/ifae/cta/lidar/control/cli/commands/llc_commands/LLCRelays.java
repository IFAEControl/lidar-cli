package cat.ifae.cta.lidar.control.cli.commands.llc_commands;

import cat.ifae.cta.lidar.*;
import cat.ifae.cta.lidar.control.cli.Licli;
import cat.ifae.cta.lidar.logging.Logging;
import io.grpc.StatusRuntimeException;
import picocli.CommandLine;

@CommandLine.Command(name = "relays", mixinStandardHelpOptions = true)
public
class LLCRelays implements Runnable {
    private final static Logging _log = new Logging(LLCRelays.class);

    private LLCRelayGrpc.LLCRelayBlockingStub stub;

    @CommandLine.Option(names = "--status", description = "Get status")
    private boolean get_status = false;

    @CommandLine.Option(names = "--device", description = "Device to set")
    private int device_number = -1;

    @CommandLine.Option(names = "--laser-on", description = "Power On LLCLaser")
    private boolean laser_on = false;

    @CommandLine.Option(names = "--laser-off", description = "Power Off LLCLaser")
    private boolean laser_off = false;

    @CommandLine.Option(names = "--hotwind-on", description = "Power On Hotwind")
    private boolean hotwind_on = false;

    @CommandLine.Option(names = "--hotwind-off", description = "Power Off Hotwind")
    private boolean hotwind_off = false;

    @CommandLine.Option(names = "--licel-on", description = "Power on Licel")
    private boolean licel_on = false;

    @CommandLine.Option(names = "--licel-off", description = "Power off licel")
    private boolean licel_off = false;

    @Override
    public final void run() {
        stub = LLCRelayGrpc.newBlockingStub(Licli.sm.getCh());
        stub = Licli.sm.addMetadata(stub);

        CommandLine.populateCommand(this);

        try {
            if(get_status) getStatus();
            else if(laser_on) powerOnLaser();
            else if(laser_off) powerOffLaser();
            else if(hotwind_on) powerOnHotwind();
            else if(hotwind_off) powerOffHotwind();
            else if(licel_on) powerOnLicel();
            else if(licel_off) powerOffLicel();
        } catch (StatusRuntimeException e) {
            _log.error(e.getLocalizedMessage());
        } catch (RuntimeException e) {
            System.out.println(e.toString());
        }
    }

    private void getStatus() {
        Null req = Null.newBuilder().build();
        StatusArray resp = stub.getStatus(req);
        System.out.println("LLCRelays's Status: ");
        for (int i=0; i < resp.getStatusCount(); i++){
            if(resp.getStatus(i).getStatus()) System.out.print("ON");
            else System.out.print("OFF");
            var req2 = Index.newBuilder().setIndex(i).build();
            var resp2 = stub.getName(req2);
            System.out.println("\t"+resp2.getStr());
        }
    }

    private void powerOnLaser() {
        var req = Relay.newBuilder().setRealy(Relay.RelayEnum.LASER).build();
        stub.setRelayOn(req);
    }

    private void powerOffLaser() {
        var req = Relay.newBuilder().setRealy(Relay.RelayEnum.LASER).build();
        stub.setRelayOff(req);
    }

    private void powerOnHotwind() {
        var req = Relay.newBuilder().setRealy(Relay.RelayEnum.HOTWIND).build();
        stub.setRelayOn(req);
    }

    private void powerOffHotwind() {
        var req = Relay.newBuilder().setRealy(Relay.RelayEnum.HOTWIND).build();
        stub.setRelayOff(req);
    }

    private void powerOnLicel() {
        var req = Relay.newBuilder().setRealy(Relay.RelayEnum.LICEL).build();
        stub.setRelayOn(req);
    }

    private void powerOffLicel() {
        var req = Relay.newBuilder().setRealy(Relay.RelayEnum.LICEL).build();
        stub.setRelayOff(req);
    }
}