package cat.ifae.cta.lidar.control.cli.commands.operation_commands;

import cat.ifae.cta.lidar.*;
import cat.ifae.cta.lidar.control.cli.Configuration;
import cat.ifae.cta.lidar.control.cli.Licli;
import cat.ifae.cta.lidar.logging.Logging;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import picocli.CommandLine;

import java.text.MessageFormat;
import java.util.concurrent.CountDownLatch;

@CommandLine.Command(name = "ll", mixinStandardHelpOptions = true)
public class LowLevel implements Runnable {
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

    @CommandLine.Option(names = "--laser-fire", description = "Initialize laser")
    private  boolean laser_fire = false;

    @CommandLine.Option(names = "--ramp-up", description = "Execute ramp up DACs")
    private  boolean ramp_up = false;

    @CommandLine.Option(names = "--ramp-down", description = "Execute ramp down DACs")
    private  boolean ramp_down = false;

    @CommandLine.Option(names = "--ramp-single", description = "Modify voltage of a single DAC")
    private String ramp_single = "";

    private OperationGrpc.OperationStub stub;
    private OperationGrpc.OperationBlockingStub blocking_stub;
    private LLCDriversGrpc.LLCDriversBlockingStub drivers_stub;
    private LLCSensorsGrpc.LLCSensorsBlockingStub sensors_stub;

    @Override
    public void run() {
        var ch = Licli.sm.getCh();

        stub = OperationGrpc.newStub(ch);
        stub = Licli.sm.addMetadata(stub);

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
            else if(laser_fire) laserFire();
            else if(ramp_up) rampUp();
            else if(ramp_down) rampDown();
            else if(!ramp_single.isEmpty()) rampSingle(ramp_single);
            else printHelp();
        } catch (StatusRuntimeException e) {
            log.error(e.getLocalizedMessage());
        } catch(Exception e) {
            e.printStackTrace();
            log.error(e.toString());
        }
    }

    // Private methods

    private static java.awt.geom.Point2D getPosition() {
        var p_x = Configuration.arm_alignment_x;
        var p_y = Configuration.arm_alignment_y;

        return new java.awt.geom.Point2D.Float(p_x, p_y);
    }

    private void initSequence() throws InterruptedException {
        var p = getPosition();
        var point_req = Point2D.newBuilder().setX(p.getX()).setY(p.getY()).build();

        var vlts_0 = Configuration.pmt_dac_vlts_0;
        var vlts_1 = Configuration.pmt_dac_vlts_1;
        var vlts_2 = Configuration.pmt_dac_vlts_2;
        var vlts_3 = Configuration.pmt_dac_vlts_3;
        var vlts_4 = Configuration.pmt_dac_vlts_4;
        var vlts_5 = Configuration.pmt_dac_vlts_5;
        
        var req = InitSequenceOptions.newBuilder().setPosition(point_req).putPmtDacVoltages(0, vlts_0).putPmtDacVoltages(1, vlts_1)
                .putPmtDacVoltages(2, vlts_2).putPmtDacVoltages(3, vlts_3)
                .putPmtDacVoltages(4, vlts_4).putPmtDacVoltages(5, vlts_5).build();

        var finishedLatch = new CountDownLatch(1);

        stub.executeMicroInitSequence(req, new StreamObserver<>() {
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

    private void rampSingle(String s) throws InterruptedException {
        String[] components = Helpers.split(s, 2);

        int dac = Integer.parseInt(components[0]);
        int voltage = Integer.parseInt(components[1]);

        var c = DAC.newBuilder().setIdx(dac).setVoltage(voltage).build();

        var finishedLatch = new CountDownLatch(1);

        stub.rampUpSingleDAC(c, new StreamObserver<>() {
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

    private void rampUp() throws InterruptedException {
        var vlts_0 = Configuration.pmt_dac_vlts_0;
        var vlts_1 = Configuration.pmt_dac_vlts_1;
        var vlts_2 = Configuration.pmt_dac_vlts_2;
        var vlts_3 = Configuration.pmt_dac_vlts_3;
        var vlts_4 = Configuration.pmt_dac_vlts_4;
        var vlts_5 = Configuration.pmt_dac_vlts_5;

        var req = InitSequenceOptions.newBuilder().putPmtDacVoltages(0, vlts_0)
                .putPmtDacVoltages(1, vlts_1).putPmtDacVoltages(2, vlts_2)
                .putPmtDacVoltages(3, vlts_3).putPmtDacVoltages(4, vlts_4)
                .putPmtDacVoltages(5, vlts_5).build();

        var finishedLatch = new CountDownLatch(1);

        stub.rampUpDACs(req, new StreamObserver<>() {
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

    private void rampDown() throws InterruptedException {
        var finishedLatch = new CountDownLatch(1);

        var req = Null.newBuilder().build();
        stub.rampDownDACs(req, new StreamObserver<>() {
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

    private void laserFire() {
        var req = Null.newBuilder().build();
        blocking_stub.fireLaser(req);
    }

    private static void printHelp() {
        System.out.println("Properties");
        //System.out.println("DAC Voltage: " + getDacVoltage());
        System.out.println("Position: " + getPosition());
    }
}
