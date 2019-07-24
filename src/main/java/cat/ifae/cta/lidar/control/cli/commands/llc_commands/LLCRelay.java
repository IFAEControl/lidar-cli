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
    private static final int LICEL_RELAY = 2;

    @CommandLine.Option(names = "--get-status", description = "Get status")
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

    @CommandLine.Option(names = "--set-status", description = "Set status to relay, status must be either 'true' or 'false'. format=relay_idx:status")
    private String status;

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
            else if(!status.isEmpty()) setStatus(status);
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

    private void setStatus(String s) {
        String[] components = Helpers.split(s, 2);

        try {
            Integer.parseInt(components[1]);
            System.err.println("Status must be either 'true' or 'false'");
            return;
        } catch(NumberFormatException e) {
            // ignore
        }

        int idx = Integer.parseInt(components[0]);
        boolean  status = Boolean.parseBoolean(components[1]);

        System.out.println(components[1]);

        var req = Status.newBuilder().setIdx(idx).setStatus(status).build();
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

    private void powerOnLicel() {
        Status req = Status.newBuilder().setIdx(LICEL_RELAY).setStatus(true).build();
        stub.setStatus(req);
    }

    private void powerOffLicel() {
        Status req = Status.newBuilder().setIdx(LICEL_RELAY).setStatus(false).build();
        stub.setStatus(req);
    }
}