package cat.ifae.cta.lidar.control.cli.commands;

import cat.ifae.cta.lidar.*;
import cat.ifae.cta.lidar.control.cli.Configuration;
import cat.ifae.cta.lidar.control.cli.Licli;
import cat.ifae.cta.lidar.logging.Logging;
import io.grpc.stub.StreamObserver;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.concurrent.CountDownLatch;


class DataSelection {
    private int d;

    void enablePHO() {
        d |= 1 << 0;
    }

    void enableLSW() {
        d |= 1 << 1;
    }

    void enableMSW() {
        d |= 1 << 2;
    }

    void enablePHM() {
        d |= 1 << 3;
    }

    void enableAnalogCombined() {
        d |= 1<<4;
    }

    void enableAnalogData() {
        d |= 1<<5;
    }

    void enableConvertedPHO() {
        d |= 1 << 6;
    }

    void enableFileID() {
        d |= 1 << 7;
    }

    int getBitmap() {
        return d;
    }
}

@CommandLine.Command(name = "acq", mixinStandardHelpOptions = true)
class Acquisition implements Runnable {
    private final static Logging log = new Logging(Acquisition.class);

    @CommandLine.Option(names = "--download", description = "Download file with given ID")
    private int download_file_id = -1;

    @CommandLine.Option(names = "--start", description = "Start acquisition manually")
    private boolean acquisition_start = false;

    @CommandLine.Option(names = "--stop", description = "Stop Acquisition manually")
    private boolean acquisition_stop = false;

    @CommandLine.Option(names = "--shots", description = "Acquire a given number of shots")
    private int acquire_shots = 0;
    
    @CommandLine.Option(names = "--disc", description = "Discriminator level")
    private int disc = 0;

    private OperationGrpc.OperationBlockingStub blocking_stub;

    @Override
    public void run() {
        var ch = Licli.sm.getCh();

        blocking_stub = OperationGrpc.newBlockingStub(ch);
        blocking_stub = Licli.sm.addMetadata(blocking_stub);

        try {
            if(disc == 0) {
                System.out.println("Discriminator level must be set");
                return;
            }

            if(acquisition_start) acquisitionStart();
            else if(acquisition_stop) acquisitionStop();
            else if(download_file_id != -1) downloadFile();
            else if(acquire_shots != 0) {
                if(acquire_shots >= 2)
                    acquireShots();
                else
                    System.out.println("Minimum shot number is 2");
            }
        } catch(Exception e) {
            e.printStackTrace();
            log.error(e.toString());
        }
    }

    private void downloadFile() {
        var req = FileID.newBuilder().setId(download_file_id).build();
        var resp = blocking_stub.downloadFile(req);
        System.out.println(new String(resp.getData().toByteArray()));
    }

    private void acquireShots() throws IOException {
        var t = new DataSelection();
        t.enableAnalogData();
        t.enableFileID();
        var b = t.getBitmap();
        var req = AcqConfig.newBuilder().setMaxBins(16381).setDiscriminator(disc).setShots(acquire_shots).setDataToRetrieve(b).build();
        var resp = blocking_stub.acquireShots(req);
        writeDataToFiles(resp);
    }

    private void acquisitionStart() {
        var t = new DataSelection();
        t.enableAnalogData();
        t.enableFileID();
        var b = t.getBitmap();
        var req = AcqConfig.newBuilder().setMaxBins(16381).setDiscriminator(3).setDataToRetrieve(b).build();
        blocking_stub.acquisitionStart(req);
    }

    private void acquisitionStop() throws IOException {
        var t = new DataSelection();
        t.enableAnalogData();
        t.enableFileID();
        var b = t.getBitmap();
        var req = AcqConfig.newBuilder().setMaxBins(16381).setDataToRetrieve(b).build();
        var resp = blocking_stub.acquisitionStop(req);
        System.out.println("File ID: " + resp.getAnalogLicelFileId());
        writeDataToFiles(resp);
    }

    private void writeDataToFiles(LicelData resp) throws IOException {
        {
            var writer = new BufferedWriter(new FileWriter("data/analog_combined_converted_data_0.out"));
            for (var v : resp.getData(0).getAnalogConvertedList()) {
                writer.write(MessageFormat.format("{0} ", String.valueOf(v)));
            }

            writer.close();
        }

        {
            var writer = new BufferedWriter(new FileWriter("data/analog_combined_converted_data_1.out"));
            for (var v : resp.getData(1).getAnalogConvertedList()) {
                writer.write(MessageFormat.format("{0} ", String.valueOf(v)));
            }

            writer.close();
        }
    }
}

@CommandLine.Command(name = "telescope", mixinStandardHelpOptions = true)
class Telescope implements Runnable {
    private final static Logging log = new Logging(Telescope.class);

    @CommandLine.Option(names = "--start-test", description = "Start telescope test")
    private boolean telescope_test_start = false;

    @CommandLine.Option(names = "--stop", description = "Stop telescope")
    private boolean teelescope_test_stop = false;

    @CommandLine.Option(names = "--to-max-zenith", description = "Move telescope zenith axis to maximum position")
    private boolean to_max_zenith = false;

    @CommandLine.Option(names = "--to-max-azimuth", description = "Move telescope azimuth axis to maximum position")
    private boolean to_max_azimuth = false;

    @CommandLine.Option(names = "--to-min-azimuth", description = "Move telescope azimuth axis to minimum position")
    private boolean to_min_azimuth = false;

    @CommandLine.Option(names = "--go-parking", description = "Go to parking position")
    private boolean parking_position = false;

    @CommandLine.Option(names = "--get-parking", description =  "Get parking position")
    private boolean get_parking_position = false;

    @CommandLine.Option(names = "--go-zenith-parking", description = "Go to zenith parking position")
    private boolean zenith_parking_position = false;

    @CommandLine.Option(names = "--go-azimuth-parking", description = "Go to azimuth parking position")
    private boolean azimuth_parking_postiion = false;

    private TelescopeGrpc.TelescopeStub stub;
    private TelescopeGrpc.TelescopeBlockingStub blocking_stub;

    @Override
    public void run() {
        var ch = Licli.sm.getCh();

        blocking_stub = TelescopeGrpc.newBlockingStub(ch);
        blocking_stub = Licli.sm.addMetadata(blocking_stub);

        stub = TelescopeGrpc.newStub(ch);
        stub = Licli.sm.addMetadata(stub);

        try {
            if(telescope_test_start) executeTelescopeTests();
            else if(teelescope_test_stop) stopTelescopeTests();
            else if(to_max_zenith) goToMaximumZenith();
            else if(to_max_azimuth) goToMaximumAzimuth();
            else if(to_min_azimuth) goToMinimumAzimuth();
            else if(parking_position) goToParkingPosition();
            else if(get_parking_position) getParkingPosition();
            else if(zenith_parking_position) goToZenithParkingPosition();
            else if(azimuth_parking_postiion) goToAzimuthParkingPosition();
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
        blocking_stub.stopTelescope(req);
    }

    private void goToMaximumZenith() throws InterruptedException {
        var finishedLatch = new CountDownLatch(1);

        Null req = Null.newBuilder().build();
        stub.goToMaximumZenithPosition(req, new StreamObserver<>() {
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

    private void goToMaximumAzimuth() throws InterruptedException {
        var finishedLatch = new CountDownLatch(1);

        Null req = Null.newBuilder().build();
        stub.goToMaximumAzimuthPosition(req, new StreamObserver<>() {
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

    private void goToMinimumAzimuth() {
        Null req = Null.newBuilder().build();
        blocking_stub.goToMinimumAzimuthPosition(req);
    }

    private void goToParkingPosition() {
        Null req = Null.newBuilder().build();
        blocking_stub.goToParkingPosition(req);
    }

    private void getParkingPosition() {
        var req = Null.newBuilder().build();
        var resp = blocking_stub.getParkingPosition(req);
        System.out.println("Azimuth=" + resp.getAzimuth());
        System.out.println("Zenith=" + resp.getZenith());
    }

    private void goToZenithParkingPosition() {
        Null req = Null.newBuilder().build();
        blocking_stub.goToZenithParkingPosition(req);
    }

    private void goToAzimuthParkingPosition() {
        Null req = Null.newBuilder().build();
        blocking_stub.goToAzimuthParkingPosition(req);
    }

}

@CommandLine.Command(name = "ll", mixinStandardHelpOptions = true)
class LowLevel implements Runnable {
    private final static Logging log = new Logging(LowLevel.class);

    @CommandLine.Option(names = "--micro-init", description = "Micro initialization sequence")
    private boolean micro_init = false;

    @CommandLine.Option(names = "--micro-shutdown", description = "Micro shutdown sequence")
    private boolean micro_shutdown = false;

    @CommandLine.Option(names = "--arm-init", description = "Initialize arm")
    private boolean arm_init = false;

    @CommandLine.Option(names = "--arm-align", description = "Move arm to alignment position")
    private boolean arm_align = false;

    @CommandLine.Option(names = "--laser-init", description = "Initialize laser")
    private  boolean laser_init = false;

    @CommandLine.Option(names = "--laser-power-on", description = "Power on laser")
    private  boolean laser_power_on = false;

    @CommandLine.Option(names = "--laser-power-off", description = "Power off laser")
    private  boolean laser_power_off = false;

    @CommandLine.Option(names = "--ramp-up", description = "Execute ramp up DACs")
    private  boolean ramp_up = false;

    @CommandLine.Option(names = "--ramp-down", description = "Execute ramp down DACs")
    private  boolean ramp_down = false;

    @CommandLine.Option(names = "--ramp-single", description = "Modify voltage of a single DAC")
    private String ramp_single = "";

    private OperationGrpc.OperationBlockingStub blocking_stub;
    private LLCDriversGrpc.LLCDriversBlockingStub drivers_stub;
    private LLCSensorsGrpc.LLCSensorsBlockingStub sensors_stub;

    @Override
    public void run() {
        var ch = Licli.sm.getCh();

        blocking_stub = OperationGrpc.newBlockingStub(ch);
        blocking_stub = Licli.sm.addMetadata(blocking_stub);

        drivers_stub = LLCDriversGrpc.newBlockingStub(ch);
        drivers_stub = Licli.sm.addMetadata(drivers_stub);

        sensors_stub = LLCSensorsGrpc.newBlockingStub(ch);
        sensors_stub = Licli.sm.addMetadata(sensors_stub);

        try {
            if(micro_init) initSequence();
            else if(micro_shutdown) microShutdownSequence();
            else if(arm_init) initializeArm();
            else if(arm_align) moveArmToAlignmentPos();
            else if(laser_init) initalizeLaser();
            else if(laser_power_on) laserPowerOn();
            else if(laser_power_off) laserPowerOff();
            else if(ramp_up) rampUp();
            else if(ramp_down) rampDown();
            else if(!ramp_single.isEmpty()) rampSingle(ramp_single);
            else printHelp();
        } catch(Exception e) {
            e.printStackTrace();
            log.error(e.toString());
        }
    }

    // Private methods

    private static float getTemperature() {
        return Configuration.temperature_threshold;
    }

    private static int getDacVoltage() {
        return Configuration.pmt_dac_voltage;
    }

    private static java.awt.geom.Point2D getPosition() {
        var p_x = Configuration.arm_alignment_x;
        var p_y = Configuration.arm_alignment_y;

        return new java.awt.geom.Point2D.Float(p_x, p_y);
    }

    private java.util.List getConvertedData() {
        Null req = Null.newBuilder().build();
        Data resp = sensors_stub.getConvertedData(req);
        return resp.getDataList();
    }

    private String getDriverName(int index) {
        Index req = Index.newBuilder().setIndex(index).build();
        var resp = drivers_stub.getName(req);
        return resp.getStr();
    }

    private void printDacsVoltage(){
        var d = getConvertedData();
        var fmt_str = MessageFormat.format("DAC#1: {}V\tDAC#2: {}V\tDAC#3: {}V\tDAC#4: {}V",
                d.get(17), d.get(18), d.get(19), d.get(20));

        System.out.println("Current DACs Voltage:");
        System.out.println(fmt_str);
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

    private void initSequence() {
        var p = getPosition();
        var point_req = Point2D.newBuilder().setX(p.getX()).setY(p.getY()).build();
        var req = InitSequenceOptions.newBuilder().setHotwindTmep(getTemperature()).setPosition(point_req).setPmtDacVoltage(getDacVoltage()).build();
        blocking_stub.executeMicroInitSequence(req);

        printDacsVoltage();
        printDriversStatus();
    }

    private void microShutdownSequence() {
        var req = Null.newBuilder().build();
        blocking_stub.executeMicroShutdownSequence(req);
    }

    private void initializeArm() {
        var req = Null.newBuilder().build();
        blocking_stub.initializeArm(req);
    }

    private void moveArmToAlignmentPos() {
        var p_x = Configuration.arm_alignment_x;
        var p_y = Configuration.arm_alignment_y;

        var req = Point2D.newBuilder().setX(p_x).setY(p_y).build();
        blocking_stub.moveArmToAlignmentPos(req);
    }

    private void rampSingle(String s) {
        String[] components = Helpers.split(s, 2);

        int dac = Integer.parseInt(components[0]);
        int voltage = Integer.parseInt(components[1]);

        var c = DAC.newBuilder().setIdx(dac).setVoltage(voltage).build();
        blocking_stub.rampUpSingleDAC(c);
    }

    private void rampUp() {
        var req = InitSequenceOptions.newBuilder().setPmtDacVoltage(getDacVoltage()).build();
        blocking_stub.rampUpDACs(req);
    }

    private void rampDown() {
        var req = Null.newBuilder().build();
        blocking_stub.rampDownDACs(req);
    }

    private void initalizeLaser() {
        var req = Null.newBuilder().build();
        blocking_stub.initializeLaser(req);
    }

    private void laserPowerOn() {
        var req = Null.newBuilder().build();
        blocking_stub.powerOnLaser(req);
    }

    private void laserPowerOff() {
        var req = Null.newBuilder().build();
        blocking_stub.powerOffLaser(req);
    }

    private static void printHelp() {
        System.out.println("Properties");
        System.out.println("Temperature: " + getTemperature());
        System.out.println("DAC Voltage: " + getDacVoltage());
        System.out.println("Position: " + getPosition());
    }
}


@CommandLine.Command(name = "operation", description = "Operation commands",
        mixinStandardHelpOptions = true, subcommands = {Acquisition.class, Telescope.class, LowLevel.class})
public class Operation implements Runnable {
    private final static Logging log = new Logging(Operation.class);

    @CommandLine.Option(names = "--startup", description = "Start up normal mode")
    private boolean startup_normal_mode = false;

    @CommandLine.Option(names = "--shutdown", description = "Shutdown")
    private boolean shutdown = false;

    private OperationGrpc.OperationStub stub;
    private OperationGrpc.OperationBlockingStub blocking_stub;

    @Override
    public void run() {
        var ch = Licli.sm.getCh();

        stub = OperationGrpc.newStub(ch);
        stub = Licli.sm.addMetadata(stub);

        blocking_stub = OperationGrpc.newBlockingStub(ch);
        blocking_stub = Licli.sm.addMetadata(blocking_stub);

        CommandLine.populateCommand(this);

        try {
            if(startup_normal_mode) startUpNormalMode();
            else if(shutdown) shutdownSequence();
            else printHelp();
        } catch(Exception e) {
            e.printStackTrace();
            log.error(e.toString());
        }
    }

    private void startUpNormalMode() throws InterruptedException {
        var finishedLatch = new CountDownLatch(1);

        var p = getPosition();
        var point_req = Point2D.newBuilder().setX(p.getX()).setY(p.getY()).build();
        var req = InitSequenceOptions.newBuilder().setHotwindTmep(getTemperature()).setPosition(point_req).setPmtDacVoltage(getDacVoltage()).build();
        stub.startUpNormalMode(req, new StreamObserver<>() {
            public void onNext(TraceOperation response) {
                System.out.println(response.getLine());
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

    private void shutdownSequence() throws InterruptedException{
        var finishedLatch = new CountDownLatch(1);

        var req = Null.newBuilder().build();
        stub.shutdown(req, new StreamObserver<>() {
            public void onNext(TraceOperation response) {
                System.out.println(response.getLine());
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

    private static void printHelp() {
        System.out.println("Properties");
        System.out.println("Temperature: " + getTemperature());
        System.out.println("DAC Voltage: " + getDacVoltage());
        System.out.println("Position: " + getPosition());
    }

    private static java.awt.geom.Point2D getPosition() {
        var p_x = Configuration.arm_alignment_x;
        var p_y = Configuration.arm_alignment_y;

        return new java.awt.geom.Point2D.Float(p_x, p_y);
    }

    private static float getTemperature() {
        return Configuration.temperature_threshold;
    }

    private static int getDacVoltage() {
        return Configuration.pmt_dac_voltage;
    }
}
