package cat.ifae.cta.lidar.control.cli.commands;

import cat.ifae.cta.lidar.*;
import cat.ifae.cta.lidar.control.cli.Licli;
import picocli.CommandLine;

@CommandLine.Command(name = "petals", mixinStandardHelpOptions = true)
class Petals implements Runnable {
    @CommandLine.Option(names = "--status", description = "Get status of petals")
    private boolean is_status = false;

    @CommandLine.Option(names = "--close", description = "Start (1) or stop (0) closing petals")
    private int is_close = -1;

    @CommandLine.Option(names = "--open", description = "Start (1) or stop (0) opening petals")
    private int is_open = -1;

    private MotorsGrpc.MotorsBlockingStub stub = null;

    @Override
    public final void run() {
        stub = MotorsGrpc.newBlockingStub(Licli.sm.getCh());
        stub = Licli.sm.addMetadata(stub);

        try {
            if(is_close == 1) close(true);
            else if(is_close == 0) close(false);
            else if(is_open == 1) open(true);
            else if(is_open == 0) open(false);
            else if(is_status) getStatus();
        } catch(Exception e) {
            System.out.println(e.toString());
        }
    }

    private void getStatus() {
        Null req = Null.newBuilder().build();
        MotorStatus resp = stub.getStatusPetals(req);
        System.out.println(resp);
    }

    private void close(boolean close_petals) {
        Null req = Null.newBuilder().build();
        if(close_petals)
            stub.startClosingPetals(req);
        else
            stub.stopClosingPetals(req);
    }

    private void open(boolean open_petals) {
        Null req = Null.newBuilder().build();
        if(open_petals)
            stub.startOpeningPetals(req);
        else
            stub.stopOpeningPetals(req);
    }

}

@CommandLine.Command(name = "doors", mixinStandardHelpOptions = true)
class Doors implements Runnable {
    @CommandLine.Option(names = "--close", description = "Start (1) or stop (0) closing doors")
    private int is_close = -1;

    @CommandLine.Option(names = "--open", description = "Start (1) or stop (0) opening doors")
    private int is_open = -1;

    @CommandLine.Option(names = "--status", description = "Get status of doors")
    private boolean is_status = false;

    private MotorsGrpc.MotorsBlockingStub stub = null;

    @Override
    public final void run() {
        stub = MotorsGrpc.newBlockingStub(Licli.sm.getCh());
        stub = Licli.sm.addMetadata(stub);

        try {
            if(is_close == 1) close(true);
            else if(is_close == 0) close(false);
            else if(is_open == 1) open(true);
            else if(is_open == 0) open(false);
            else if(is_status) getStatus();
        } catch(Exception e) {
            System.out.println(e.toString());
        }
    }

    private void close(boolean close) {
        Null req = Null.newBuilder().build();
        if(close)
            stub.startCloseDoors(req);
        else
            stub.stopCloseDoors(req);
    }

    private void open(boolean open) {
        Null req = Null.newBuilder().build();
        if(open)
            stub.startOpenDoors(req);
        else
            stub.stopOpenDoors(req);
    }

    private void getStatus() {
        Null req = Null.newBuilder().build();
        MotorStatus resp = stub.getStatusDoors(req);
        System.out.println(resp);
    }
}


@CommandLine.Command(name = "motors", description = "Motors commands",
        mixinStandardHelpOptions = true, subcommands = {Doors.class, Petals.class})
public class Motors implements Runnable {
    private MotorsGrpc.MotorsBlockingStub stub = null;

    @CommandLine.Option(names = "--sz", paramLabel = "Steps", description = "Set zenith")
    private int zenith_steps = -1;

    @CommandLine.Option(names = "--gz", description = "Get zenith")
    private boolean get_zenith = false;

    @CommandLine.Option(names = "--ga", description = "Get azimuth")
    private boolean get_azimuth = false;

    @CommandLine.Option(names = "--sa", paramLabel = "Steps", description = "Set azimuth")
    private int azimuth_steps = -1;

    @CommandLine.Option(names = "--home", description = "Go home")
    private boolean home = false;

    @Override
    public final void run() {
        stub = MotorsGrpc.newBlockingStub(Licli.sm.getCh());
        stub = Licli.sm.addMetadata(stub);

        CommandLine.populateCommand(this);

        try {
            if(get_zenith) getZenith();
            else if(zenith_steps > 0) setZenith(zenith_steps);
            else if(get_azimuth) getAzimuth();
            else if(azimuth_steps > 0) setAzimuth(azimuth_steps);
            else if(home) goHome();
        } catch(Exception e) {
            System.out.println(e.toString());
        }
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

    private void goHome() {
        Null req = Null.newBuilder().build();
        stub.goHome(req);
    }
}