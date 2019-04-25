package cat.ifae.cta.lidar.control.cli.commands;

import cat.ifae.cta.lidar.*;
import cat.ifae.cta.lidar.control.cli.Licli;
import picocli.CommandLine;

@CommandLine.Command(name = "motors", mixinStandardHelpOptions = true)
public
class Motors implements Runnable {
    private MotorsGrpc.MotorsBlockingStub stub = null;

    @CommandLine.Option(names = "-m", paramLabel = "Message", description = "Send a message")
    private String message = "";

    @CommandLine.Option(names = "-sz", paramLabel = "Steps", description = "Set zenith")
    private int zenith_steps = -1;

    @CommandLine.Option(names = "-gz", description = "Get zenith")
    private boolean get_zenith = false;

    @CommandLine.Option(names = "-ga", description = "Get azimuth")
    private boolean get_azimuth = false;

    @CommandLine.Option(names = "-sa", paramLabel = "Steps", description = "Set azimuth")
    private int azimuth_steps = -1;

    @CommandLine.Option(names = "-home", description = "Go home")
    private boolean home = false;

    @CommandLine.Option(names = "-status-petals", description = "Get status of petals")
    private boolean status_petals = false;

    @CommandLine.Option(names = "-close-petals", description = "Start (1) or stop (0) closing petals")
    private int close_petals = -1;

    @CommandLine.Option(names = "-open-petals", description = "Start (1) or stop (0) opening petals")
    private int open_petals = -1;

    @CommandLine.Option(names = "-close-doors", description = "Start (1) or stop (0) closing doors")
    private int close_doors = -1;

    @CommandLine.Option(names = "-open-doors", description = "Start (1) or stop (0) opening doors")
    private int open_doors = -1;

    @CommandLine.Option(names = "-status-doors", description = "Get status of doors")
    private boolean status_doors = false;

    @CommandLine.ParentCommand
    private Licli parent;

    @Override
    public final void run() {
        stub = MotorsGrpc.newBlockingStub(parent.sm.getCh());
        stub = parent.sm.addMetadata(stub);

        CommandLine.populateCommand(this);

        try {
            if (message != "") echo(message);
            else if(get_zenith) getZenith();
            else if(zenith_steps > 0) setZenith(zenith_steps);
            else if(get_azimuth) getAzimuth();
            else if(azimuth_steps > 0) setAzimuth(azimuth_steps);
            else if(status_petals) getStatusPetals();
            else if(close_petals == 1) closePetals(true);
            else if(close_petals == 0) closePetals(false);
            else if(open_petals == 1) openPetals(true);
            else if(open_petals == 0) openPetals(false);
            else if(close_doors == 1) closeDoors(true);
            else if(close_doors == 0) closeDoors(false);
            else if(open_doors == 1) openDoors(true);
            else if(open_doors == 0) openDoors(false);
            else if(status_doors) getStatusDoors();
            else if(home) goHome();
        } catch(Exception e) {
            System.out.println(e.toString());
        }
    }

    public final void echo(String m) {
        Message request = Message.newBuilder().setText(m).build();
        Echo resp = stub.testEcho(request);
        System.out.println(resp.getText());
    }

    private void getZenith() {
        Null req = Null.newBuilder().build();
        Position resp = stub.getZenith(req);
        System.out.println("Zenith position: " + resp.getSteps());
    }

    private void setZenith(int steps) {
        Position req = Position.newBuilder().setSteps(steps).build();
        Null resp = stub.setZenith(req);
        System.out.println(resp);
    }

    private void setAzimuth(int steps) {
        Position req = Position.newBuilder().setSteps(steps).build();
        Null resp = stub.setAzimuth(req);
        System.out.println(resp);
    }

    private void getAzimuth() {
        Null req = Null.newBuilder().build();
        Position resp = stub.getAzimuth(req);
        System.out.println(resp);
    }

    private void getStatusPetals() {
        Null req = Null.newBuilder().build();
        MotorStatus resp = stub.getStatusPetals(req);
        System.out.println(resp);
    }

    private void closePetals(boolean close_petals) {
        Null req = Null.newBuilder().build();
        if(close_petals)
            stub.startClosingPetals(req);
        else
            stub.stopClosingPetals(req);
    }

    private void openPetals(boolean open_petals) {
        Null req = Null.newBuilder().build();
        if(open_petals)
            stub.startOpeningPetals(req);
        else
            stub.stopOpeningPetals(req);
    }

    private void closeDoors(boolean close) {
        Null req = Null.newBuilder().build();
        if(close)
            stub.startCloseDoors(req);
        else
            stub.stopCloseDoors(req);
    }

    private void openDoors(boolean open) {
        Null req = Null.newBuilder().build();
        if(open)
            stub.startOpenDoors(req);
        else
            stub.stopOpenDoors(req);
    }

    private void getStatusDoors() {
        Null req = Null.newBuilder().build();
        MotorStatus resp = stub.getStatusDoors(req);
        System.out.println(resp);

    }

    private void goHome() {
        Null req = Null.newBuilder().build();
        stub.goHome(req);
    }
}