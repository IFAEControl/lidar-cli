package cat.ifae.cta.lidar.control.cli;

import cat.ifae.cta.lidar.LaserGrpc;
import cat.ifae.cta.lidar.Null;
import cat.ifae.cta.lidar.Power;
import picocli.CommandLine;

@CommandLine.Command(name = "laser", mixinStandardHelpOptions = true)
class Laser implements Runnable {
    private LaserGrpc.LaserBlockingStub stub;

    @CommandLine.ParentCommand
    private Control parent;

    @CommandLine.Option(names = "-init", description = "Initalize laser")
    private boolean is_init = false;

    @CommandLine.Option(names = "-fire", description = "Fire laser")
    private boolean is_fire = false;

    @CommandLine.Option(names = "-stop", description = "Stop laser")
    private boolean is_stop = false;

    @CommandLine.Option(names = "-pause", description = "Pause laser")
    private boolean is_pause = false;

    @CommandLine.Option(names = "-check", description = "Check laser communications")
    private boolean check = false;

    @CommandLine.Option(names = "-power", description = "Set laser power")
    private int power = -1;

    @Override
    public final void run() {
        stub = LaserGrpc.newBlockingStub(parent.grpc.channel);
        stub = parent.grpc.addMetadata(stub);

        CommandLine.populateCommand(this);

        try {
            if(is_init) init();
            else if(is_fire) fire();
            else if(is_stop) stop();
            else if(is_pause) pause();
            else if(check) checkCommunications();
            else if(power > -1) setPower(power);
        } catch(RuntimeException e) {
            System.out.println(e.toString());
        }
    }

    private void init() {
        Null req = Null.newBuilder().build();
        stub.init(req);
    }

    private void fire() {
        Null req = Null.newBuilder().build();
        stub.fire(req);
    }

    private void stop() {
        Null req = Null.newBuilder().build();
        stub.stop(req);
    }

    private void pause() {
        Null req = Null.newBuilder().build();
        stub.pause(req);
    }

    private void checkCommunications() {
        Null req = Null.newBuilder().build();
        stub.checkCommunications(req);
    }

    private void setPower(int power) {
        Power req = Power.newBuilder().setPower(power).build();
        stub.setPower(req);
    }
}
