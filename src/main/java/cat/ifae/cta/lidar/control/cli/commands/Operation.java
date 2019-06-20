package cat.ifae.cta.lidar.control.cli.commands;

import cat.ifae.cta.lidar.*;
import cat.ifae.cta.lidar.config.Config;
import cat.ifae.cta.lidar.control.cli.Licli;
import cat.ifae.cta.lidar.logging.Logging;
import io.grpc.stub.StreamObserver;
import picocli.CommandLine;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.concurrent.CountDownLatch;

@CommandLine.Command(name = "operation", mixinStandardHelpOptions = true)
public class Operation implements Runnable {
    private final static Logging log = new Logging(Operation.class);

    @CommandLine.Option(names = "-micro-init", description = "Micro initialization sequence")
    private boolean micro_init = false;

    @CommandLine.Option(names = "-micro-shutdown", description = "Micro shutdown sequence")
    private boolean micro_shutdown = false;

    @CommandLine.Option(names = "-telescope-test", description = "Execute telescope test")
    private boolean telescope_test = false;

    @CommandLine.Option(names = "-stop-telescope-test", description = "Stop telescope test")
    private boolean stop_telescope_test = false;

    @CommandLine.ParentCommand
    private Licli parent;

    private static final int all_dacs = 4;

    private static Config cfg;

    private OperationGrpc.OperationStub stub;
    private OperationGrpc.OperationBlockingStub blocking_stub;
    private LLCDriversGrpc.LLCDriversBlockingStub drivers_stub;
    private LLCSensorsGrpc.LLCSensorsBlockingStub sensors_stub;

    public Operation() throws IOException {
         cfg = new Config("client", "micro_init_sequence");
    }

    @Override
    public void run() {
        var ch = parent.sm.getCh();

        stub = OperationGrpc.newStub(ch);
        stub = parent.sm.addMetadata(stub);

        blocking_stub = OperationGrpc.newBlockingStub(ch);
        blocking_stub = parent.sm.addMetadata(blocking_stub);

        drivers_stub = LLCDriversGrpc.newBlockingStub(ch);
        drivers_stub = parent.sm.addMetadata(drivers_stub);

        sensors_stub = LLCSensorsGrpc.newBlockingStub(ch);
        sensors_stub = parent.sm.addMetadata(sensors_stub);

        CommandLine.populateCommand(this);

        try {
            cfg.load();

            if(telescope_test) executeTelescopeTests();
            if(stop_telescope_test) stopTelescopeTests();
            if(micro_init) initSequence();
            if(micro_shutdown) shutdownSequence();
            else printHelp();
        } catch(Exception e) {
            e.printStackTrace();
            log.error(e.toString());
        }
    }

    private void executeTelescopeTests() throws InterruptedException {
        var finishedLatch = new CountDownLatch(1);

        Null req = Null.newBuilder().build();
        stub.executeTelescopeTests(req, new StreamObserver<>() {
            public void onNext(EncoderPosition response) {
                System.out.println(response.getName() + ": " + response.getPosition());
            }

            public void onError(Throwable t) {
                System.out.println("Error: " + t.getMessage());
                finishedLatch.countDown();
            }

            public void onCompleted() {
                finishedLatch.countDown();
            }
        });

        finishedLatch.await();
    }

    private void stopTelescopeTests() {
        Null req = Null.newBuilder().build();
        blocking_stub.stopTelescopeTests(req);
    }

    // Micro

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
        blocking_stub.powerOnLaser(req);
    }

    private void powerOffLaser() {
        Null req = Null.newBuilder().build();
        blocking_stub.powerOffLaser(req);
    }

    private void initializeArm() {
        Null req = Null.newBuilder().build();
        blocking_stub.initializeArm(req);
    }

    private void initializeLaser() {
        Null req = Null.newBuilder().build();
        blocking_stub.initializeLaser(req);
    }

    private void powerHotwindConditionally(float threshold) {
        Temperature req = Temperature.newBuilder().setTemperature(threshold).build();
        blocking_stub.powerHotwindConditionally(req);
    }

    private void powerOffHotwind() {
        Null req = Null.newBuilder().build();
        blocking_stub.powerOffHotwind(req);
    }

    private void moveArmToAlignmentPos(java.awt.geom.Point2D s) {
        var x = s.getX();
        var y = s.getY();

        Point2D req = Point2D.newBuilder().setX(x).setY(y).build();
        blocking_stub.moveArmToAlignmentPos(req);
    }

    private void rampDACs(int dacvoltage) {
        DacConfig req = DacConfig.newBuilder().setNumber(all_dacs).setVoltage(dacvoltage).build();
        blocking_stub.rampDACs(req);
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
