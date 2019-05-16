package cat.ifae.cta.lidar.control.cli.commands;

import cat.ifae.cta.lidar.*;
import cat.ifae.cta.lidar.config.Config;
import cat.ifae.cta.lidar.control.cli.Licli;
import cat.ifae.cta.lidar.logging.Logging;
import picocli.CommandLine;

import java.io.IOException;
import java.text.MessageFormat;

@CommandLine.Command(name = "micro", mixinStandardHelpOptions = true)
public
class Micro implements Runnable {
    private MicroGrpc.MicroBlockingStub stub;
    private SensorsGrpc.SensorsBlockingStub sensors_stub;
    private DriversGrpc.DriversBlockingStub drivers_stub;

    @CommandLine.Option(names = "-init", description = "Initialization sequence")
    private boolean initialize = false;

    @CommandLine.Option(names = "-shutdown", description = "Shutdown sequence")
    private boolean shutdown = false;

    @CommandLine.ParentCommand
    private Licli parent;

    private final static Logging log = new Logging(Micro.class);

    private static Config cfg;

    private static final int all_dacs = 4;

    Micro() throws IOException {
        cfg = new Config("client", "micro_init_sequence");
    }

    @Override
    public final void run() {
        var ch = parent.sm.getCh();

        stub = MicroGrpc.newBlockingStub(ch);
        stub = parent.sm.addMetadata(stub);

        sensors_stub = SensorsGrpc.newBlockingStub(ch);
        sensors_stub = parent.sm.addMetadata(sensors_stub);

        drivers_stub = DriversGrpc.newBlockingStub(ch);
        drivers_stub = parent.sm.addMetadata(drivers_stub);

        CommandLine.populateCommand(this);

        // var dir = System.getProperty("user.dir");

        try {
            cfg.load();

            if(initialize) initSequence();
            else if(shutdown) shutdownSequence();
            else printHelp();
        } catch(Exception e) {
            e.printStackTrace();
            log.error(e.toString());
        }
    }

    private static void printHelp() {
        System.out.println("Properties");
        System.out.println("Temperature: " + getTemperature());
        System.out.println("DAC Voltage: " + getDacVoltage());
        System.out.println("Position: " + getPosition());
    }

    private void initSequence() {
        System.out.println("Starting Barcelona Raman LIDAR MicroController Systems Initialize Sequence!");

        System.out.println("Executing powerOnLaser");
        powerOnLaser();
        System.out.println("Executing powerHotwindConditionally");
        powerHotwindConditionally(getTemperature());
        System.out.println("Executing initializeArm");
        initializeArm();
        System.out.println("Executing initializeLaser");
        initializeLaser();
        System.out.println("Executing moveArmToAlignmentPos");
        moveArmToAlignmentPos(getPosition());
        System.out.println("Executing rampDACs");
        rampDACs(getDacVoltage());
        System.out.println("Executing printDacsVoltage");
        printDacsVoltage();
        System.out.println("Executing printDriversStatus");
        printDriversStatus();

        System.out.println("Ending Barcelona Raman LIDAR MicroController Systems Initialize Sequence!");
    }

    private void shutdownSequence() {
        System.out.println("Starting Barcelona Raman LIDAR MicroController Systems Shutdown Sequence!");

        powerOffLaser();
        powerOffHotwind();
        rampDACs(0);

        System.out.println("Ending Barcelona Raman LIDAR MicroController Systems Shutdown Sequence!");
    }

    private static java.awt.geom.Point2D getPosition() {
        var p_x = cfg.getFloat("allignment_arm_X");
        var p_y = cfg.getFloat("allignment_arm_Y");

        return new java.awt.geom.Point2D.Float(p_x, p_y);
    }

    private static float getTemperature() {
        return cfg.getFloat("temperature_threshold");
    }

    private static int getDacVoltage() {
        return cfg.getInteger("pmt_dac_voltage");
    }

    private void powerOnLaser() {
        Null req = Null.newBuilder().build();
        stub.powerOnLaser(req);
    }

    private void powerOffLaser() {
        Null req = Null.newBuilder().build();
        stub.powerOffLaser(req);
    }

    private void initializeArm() {
        Null req = Null.newBuilder().build();
        stub.initializeArm(req);
    }

    private void initializeLaser() {
        Null req = Null.newBuilder().build();
        stub.initializeLaser(req);
    }

    private void powerHotwindConditionally(float threshold) {
        Temperature req = Temperature.newBuilder().setTemperature(threshold).build();
        stub.powerHotwindConditionally(req);
    }

    private void powerOffHotwind() {
        Null req = Null.newBuilder().build();
        stub.powerOffHotwind(req);
    }

    private void moveArmToAlignmentPos(java.awt.geom.Point2D s) {
        var x = s.getX();
        var y = s.getY();

        Point2D req = Point2D.newBuilder().setX(x).setY(y).build();
        stub.moveArmToAlignmentPos(req);
    }

    private void rampDACs(int dacvoltage) {
        DacConfig req = DacConfig.newBuilder().setNumber(all_dacs).setVoltage(dacvoltage).build();
        stub.rampDACs(req);
    }

    private java.util.List getConvertedData() {
        Null req = Null.newBuilder().build();
        Data resp = sensors_stub.getConvertedData(req);
        return resp.getDataList();
    }

    private void printDacsVoltage(){
        var d = getConvertedData();
        var fmt_str = MessageFormat.format("DAC#1: {}V\tDAC#2: {}V\tDAC#3: {}V\tDAC#4: {}V",
                d.get(17), d.get(18), d.get(19), d.get(20));

        System.out.println("Current DACs Voltage:");
        System.out.println(fmt_str);
    }

    private String getDriverName(int index) {
        Index req = Index.newBuilder().setIndex(index).build();
        var resp = drivers_stub.getName(req);
        return resp.getStr();
    }

    private void printDriversStatus(){
        Null req = Null.newBuilder().build();
        StatusArray resp = drivers_stub.getStatus(req);

        System.out.println("Driver's Status: ");


        for (int i = 0; i < resp.getStatusCount(); i++){
            if(resp.getStatus(i).getStatus()) System.out.print("ON");
            else System.out.print("OFF");
            System.out.println("\t" + getDriverName(i));
        }
    }
}