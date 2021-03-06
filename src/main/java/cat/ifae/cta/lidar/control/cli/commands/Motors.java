package cat.ifae.cta.lidar.control.cli.commands;

import cat.ifae.cta.lidar.*;
import cat.ifae.cta.lidar.control.cli.Licli;
import cat.ifae.cta.lidar.logging.Logging;
import io.grpc.StatusRuntimeException;
import picocli.CommandLine;
import picocli.CommandLine.Option;

@CommandLine.Command(name = "petals", mixinStandardHelpOptions = true)
class Petals implements Runnable {
    private final static Logging _log = new Logging(Petals.class);

    @Option(names = "--status", description = "Get status of petals")
    private boolean is_status = false;

    @Option(names = "--close", description = "Close petals")
    private boolean is_close = false;

    @Option(names = "--open", description = "Open petals")
    private boolean is_open = false;

    @Option(names = "--stop", description = "Stop petals")
    private boolean is_stop = false;

    private MotorsGrpc.MotorsBlockingStub stub = null;

    @Override
    public final void run() {
        stub = MotorsGrpc.newBlockingStub(Licli.sm.getCh());
        stub = Licli.sm.addMetadata(stub);

        try {
            if(is_close ) close();
            else if(is_open) open();
            else if(is_stop) stop();
            else if(is_status) getStatus();
        } catch (StatusRuntimeException e) {
            _log.error(e.getStatus().getCause().getLocalizedMessage());
        } catch(Exception e) {
            System.out.println(e.toString());
        }
    }

    private void getStatus() {
        Null req = Null.newBuilder().build();
        MotorStatus resp = stub.getStatusPetals(req);
        System.out.println("Status: " + resp.getStatus());
    }

    private void close() {
        Null req = Null.newBuilder().build();
        stub.startClosingPetals(req);
    }

    private void open() {
        Null req = Null.newBuilder().build();
        stub.startOpeningPetals(req);
    }

    private void stop() {
        var req = Null.newBuilder().build();
        stub.stopPetals(req);
    }
}

@CommandLine.Command(name = "doors", mixinStandardHelpOptions = true)
class Doors implements Runnable {
    private final static Logging _log = new Logging(Doors.class);


    @Option(names = "--close", description = "Close doors")
    private boolean is_close = false;

    @Option(names = "--open", description = "Open doors")
    private boolean is_open = false;

    @Option(names = "--stop", description = "Stop doors")
    private boolean is_stop = false;

    @Option(names = "--status", description = "Get status of doors")
    private boolean is_status = false;

    private MotorsGrpc.MotorsBlockingStub stub = null;

    @Override
    public final void run() {
        stub = MotorsGrpc.newBlockingStub(Licli.sm.getCh());
        stub = Licli.sm.addMetadata(stub);

        try {
            if(is_close) close();
            else if(is_open) open();
            else if(is_stop) stop();
            else if(is_status) getStatus();
        } catch (StatusRuntimeException e) {
            _log.error(e.getStatus().getCause().getLocalizedMessage());
        } catch(Exception e) {
            System.out.println(e.toString());
        }
    }

    private void close() {
        Null req = Null.newBuilder().build();
        stub.startCloseDoors(req);
    }

    private void open() {
        Null req = Null.newBuilder().build();
        stub.startOpenDoors(req);
    }

    private void stop() {
        var req = Null.newBuilder().build();
        stub.stopDoors(req);
    }

    private void getStatus() {
        Null req = Null.newBuilder().build();
        MotorStatus resp = stub.getStatusDoors(req);
        System.out.println("Status: " + resp.getStatus());
    }
}

@CommandLine.Command(name = "telescope", mixinStandardHelpOptions = true)
class TelescopeMotors implements Runnable {
    private final static Logging _log = new Logging(TelescopeMotors.class);

    @CommandLine.Option(names = "--gz", description = "Get zenith")
    private boolean get_zenith = false;

    @CommandLine.Option(names = "--sz", paramLabel = "Steps", description = "Move zenith to position")
    private int zenith_steps = -1;

    @CommandLine.Option(names = "--ga", description = "Get azimuth")
    private boolean get_azimuth = false;

    @CommandLine.Option(names = "--sa", paramLabel = "Steps", description = "Move azimuth to position")
    private int azimuth_steps = -1;

    @CommandLine.Option(names = "--get-parking-position", description = "Get firmware parking " +
            "position")
    private boolean get_parking = false;

    @CommandLine.Option(names = "--set-parking-position", description = "Set firmware parking position")
    private String set_parking = "";

    @CommandLine.Option(names = "--get-az-es", description = "Get azimuth end switch value")
    private boolean get_az_es = false;

    private MotorsGrpc.MotorsBlockingStub stub = null;

    @Override
    public final void run() {
        stub = MotorsGrpc.newBlockingStub(Licli.sm.getCh());
        stub = Licli.sm.addMetadata(stub);

        try {
            if(get_zenith) getZenith();
            else if(zenith_steps > 0) setZenith(zenith_steps);
            else if(get_azimuth) getAzimuth();
            else if(azimuth_steps > 0) setAzimuth(azimuth_steps);
            else if(get_parking) getParkingPosition();
            else if(!set_parking.isEmpty()) setParkingPosition(set_parking);
            else if(get_az_es) getAzimuthES();
        } catch (StatusRuntimeException e) {
            _log.error(e.getStatus().getCause().getLocalizedMessage());
        } catch(Exception e) {
            e.printStackTrace();
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

    private void getParkingPosition() {
        Null req = Null.newBuilder().build();
        var resp = stub.getParkingPosition(req);
        System.out.println(resp);
    }

    private void setParkingPosition(String p) {
        String[] components = Helpers.split(p, 2);

        int azimuth = Integer.parseInt(components[0]);
        int zenith = Integer.parseInt(components[1]);

        var req = ParkingPosition.newBuilder().setAzimuth(azimuth).setZenith(zenith).build();
        stub.setParkingPosition(req);
    }

    private void getAzimuthES() {
        Null req = Null.newBuilder().build();
        var resp = stub.getAzimuthES(req);
        System.out.println(resp);
    }
}

@CommandLine.Command(name = "motors", description = "Motors commands",
        mixinStandardHelpOptions = true, subcommands = {Doors.class, Petals.class,
        TelescopeMotors.class})
public class Motors implements Runnable {
    @Override
    public final void run() {
        CommandLine.populateCommand(this);
    }
}
