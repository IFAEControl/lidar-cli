package cat.ifae.cta.lidar.control.cli.commands;

import cat.ifae.cta.lidar.ArmGrpc;
import cat.ifae.cta.lidar.Node;
import cat.ifae.cta.lidar.Null;
import cat.ifae.cta.lidar.Point2D;
import cat.ifae.cta.lidar.Helpers;
import cat.ifae.cta.lidar.control.cli.Licli;
import picocli.CommandLine;

@CommandLine.Command(name = "arms", mixinStandardHelpOptions = true)
public class Arm implements Runnable {
    private ArmGrpc.ArmBlockingStub stub;

    @CommandLine.ParentCommand
    private Licli parent;

    @CommandLine.Option(names = "-init", description = "Initalize arms")
    private boolean is_init = false;

    @CommandLine.Option(names = "-get-pos", description = "Get arms position")
    private boolean is_get_position = false;

    @CommandLine.Option(names = "-check-node", description = "Check communications with node N")
    private int node = -1;

    @CommandLine.Option(names = "-set-speed", description = "Set speed of node. format=Axis:Speed")
    private String node_speed;

    @CommandLine.Option(names = "-go", description = "Go to position. format=X:Y")
    private String position;

    @CommandLine.Option(names = "-emergency-stop", description = "Stop arms")
    private boolean is_stop = false;

    @Override
    public void run() {
        stub = ArmGrpc.newBlockingStub(parent.sm.getCh());
        stub = parent.sm.addMetadata(stub);

        CommandLine.populateCommand(this);

        try {
            if (is_init) init();
            else if (is_get_position) getPosition();
            else if (!position.isEmpty()) goTo(position);
            else if (is_stop) stop();
            else if (node != -1) checkCommunicationsNode(node);
            else if (!node_speed.isEmpty()) setNodeSpeed(node_speed);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    private void init() {
        Null req = Null.newBuilder().build();
        stub.init(req);
    }

    private void getPosition() {
        Null req = Null.newBuilder().build();
        Point2D resp = stub.getPosition(req);
        System.out.println("Position: " + resp);
    }

    private void goTo(String p) {
        String[] components = Helpers.split(p, 2);

        double x = Double.parseDouble(components[0]);
        double y = Double.parseDouble(components[1]);

        Point2D req = Point2D.newBuilder().setX(x).setY(y).build();
        stub.gotoPosition(req);
    }

    private void stop() {
        Null req = Null.newBuilder().build();
        stub.emergencyStop(req);
    }

    private Null checkCommunicationsNode(int n) {
        Null req = Null.newBuilder().build();
        switch (n) {
            case 1:
                return stub.checkCommunicationsNode1(req);
            case 2:
                return stub.checkCommunicationsNode2(req);
            default:
                throw new RuntimeException("Invalid node number");
        }
    }

    private void setNodeSpeed(String node) {
        String[] components = Helpers.split(node, 2);

        int axis = Integer.parseInt(components[0]);
        int speed = Integer.parseInt(components[1]);

        Node n = Node.newBuilder().setAxis(axis).setSpeed(speed).build();
        stub.setNodeSpeed(n);
    }
}