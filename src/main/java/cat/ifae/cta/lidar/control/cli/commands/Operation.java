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

@CommandLine.Command(name = "acq", mixinStandardHelpOptions = true)
class Acquisition implements Runnable {
    private final static Logging log = new Logging(Acquisition.class);

    @CommandLine.Option(names = "-start", description = "Start acquisition manually")
    private boolean acquisition_start = false;

    @CommandLine.Option(names = "-stop", description = "Stop Acquisition manually")
    private boolean acquisition_stop = false;

    @CommandLine.Option(names = "-shots", description = "Acquire a given number of shots")
    private int acquire_shots = 0;

    @CommandLine.ParentCommand
    private Operation parent;

    private OperationGrpc.OperationBlockingStub blocking_stub;

    @Override
    public void run() {
        var ch = parent.parent.sm.getCh();

        blocking_stub = OperationGrpc.newBlockingStub(ch);
        blocking_stub = parent.parent.sm.addMetadata(blocking_stub);

        try {
            if(acquisition_start) acquisitionStart();
            else if(acquisition_stop) acquisitionStop();
            else if(acquire_shots != 0) acquireShots();
        } catch(Exception e) {
            e.printStackTrace();
            log.error(e.toString());
        }

    }

    private void acquireShots() {
        var req = AcqConfig.newBuilder().setMaxBins(16380).setDiscriminator(3).setShots(acquire_shots).build();
        blocking_stub.acquireShots(req);
    }

    private void acquisitionStart() {
        var req = AcqConfig.newBuilder().setMaxBins(16380).setDiscriminator(3).build();
        blocking_stub.acquisitionStart(req);
    }

    private void acquisitionStop() {
        var req = AcqConfig.newBuilder().setMaxBins(16380).build();
        var resp = blocking_stub.acquisitionStop(req);
        System.out.println(resp.getData(0).getLsw().toByteArray().length);
        System.out.println(resp.getData(1).getLsw().toByteArray().length);
    }
}

@CommandLine.Command(name = "operation", mixinStandardHelpOptions = true, subcommands = {Acquisition.class})
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

    @CommandLine.Option(names = "-go-parking", description = "Go to parking position")
    private boolean parking_position = false;

    @CommandLine.ParentCommand
    Licli parent;


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
            else if(stop_telescope_test) stopTelescopeTests();
            else if(micro_init) initSequence();
            else if(micro_shutdown) shutdownSequence();
            else if(parking_position) goToParkingPosition();
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

    private void goToParkingPosition() {
        Null req = Null.newBuilder().build();
        blocking_stub.goToParkingPosition(req);
    }

    // Micro

    private static void printHelp() {
        System.out.println("Properties");
        System.out.println("Temperature: " + getTemperature());
        System.out.println("DAC Voltage: " + getDacVoltage());
        System.out.println("Position: " + getPosition());
    }

    private void initSequence() {
        var p = getPosition();
        var point_req = Point2D.newBuilder().setX(p.getX()).setY(p.getY()).build();
        var req = InitSequenceOptions.newBuilder().setHotwindTmep(getTemperature()).setPosition(point_req).setPmtDacVoltage(getDacVoltage()).build();
        blocking_stub.executeMicroInitSequence(req);

        printDacsVoltage();
        printDriversStatus();
    }

    private void shutdownSequence() {
        var req = Null.newBuilder().build();
        blocking_stub.executeMicroShutdownSequence(req);
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
