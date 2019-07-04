package cat.ifae.cta.lidar.control.cli.commands.llc_commands;

import cat.ifae.cta.lidar.*;
import cat.ifae.cta.lidar.control.cli.Licli;
import picocli.CommandLine;

@CommandLine.Command(name = "relay", mixinStandardHelpOptions = true)
public
class LLCRelay implements  Runnable {
    private LLCRelayGrpc.LLCRelayBlockingStub stub;
    private static final int LASER_RELAY = 0;
    private static final int HOTWIND_RELAY = 1;

    @CommandLine.Option(names = "-gs", description = "Get status")
    private boolean get_status = false;

    @CommandLine.Option(names = "-device", description = "Device to set")
    private int device_number = -1;

    @CommandLine.Option(names = "-laser-on", description = "Power On LLCLaser")
    private boolean laser_on = false;

    @CommandLine.Option(names = "-laser-off", description = "Power Off LLCLaser")
    private boolean laser_off = false;

    @CommandLine.Option(names = "-hotwind-on", description = "Power On Hotwind")
    private boolean hotwind_on = false;

    @CommandLine.Option(names = "-hotwind-off", description = "Power Off Hotwind")
    private boolean hotwind_off = false;

    @CommandLine.Option(names = "-status", description = "Status to set to the given device")
    private int status = -1;

    @Override
    public final void run() {
        stub = LLCRelayGrpc.newBlockingStub(Licli.sm.getCh());
        stub = Licli.sm.addMetadata(stub);

        CommandLine.populateCommand(this);

        try {
            if (get_status) getStatus();
            else if (laser_on) powerOnLaser();
            else if (laser_off) powerOffLaser();
            else if (hotwind_on) powerOnHotwind();
            else if (hotwind_off) powerOffHotwind();
            else if (device_number == -1)
                System.out.println("Specify a device number");
            else if (status < 0 || status > 1)
                System.out.println("Status should be either True or False");
            else
                setStatus(device_number, status == 1);
        } catch (RuntimeException e) {
            System.out.println(e.toString());
        }
    }

    private void getStatus() {
        Null req = Null.newBuilder().build();
        StatusArray resp = stub.getStatus(req);
        System.out.println("LLCRelay's Status: ");
        for (int i=0; i < resp.getStatusCount(); i++){
            if(resp.getStatus(i).getStatus()) System.out.print("ON");
            else System.out.print("OFF");
            var req2 = Index.newBuilder().setIndex(i).build();
            var resp2 = stub.getName(req2);
            System.out.println("\t"+resp2.getStr());
        }
    }

    private void setStatus(int device_number, boolean status) {
        Status req = Status.newBuilder().setIdx(device_number).setStatus(status).build();
        stub.setStatus(req);
    }

    private void powerOnLaser() {
        Status req = Status.newBuilder().setIdx(LASER_RELAY).setStatus(true).build();
        stub.setStatus(req);
    }

    private void powerOffLaser() {
        Status req = Status.newBuilder().setIdx(LASER_RELAY).setStatus(false).build();
        stub.setStatus(req);
    }

    private void powerOnHotwind() {
        Status req = Status.newBuilder().setIdx(HOTWIND_RELAY).setStatus(true).build();
        stub.setStatus(req);
    }

    private void powerOffHotwind() {
        Status req = Status.newBuilder().setIdx(HOTWIND_RELAY).setStatus(false).build();
        stub.setStatus(req);
    }
}