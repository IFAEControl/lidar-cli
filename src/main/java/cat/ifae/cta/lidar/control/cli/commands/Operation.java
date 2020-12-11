package cat.ifae.cta.lidar.control.cli.commands;

import cat.ifae.cta.lidar.*;
import cat.ifae.cta.lidar.control.cli.Configuration;
import cat.ifae.cta.lidar.control.cli.Licli;
import cat.ifae.cta.lidar.control.cli.commands.operation_commands.Acquisition;
import cat.ifae.cta.lidar.control.cli.commands.operation_commands.LowLevel;
import cat.ifae.cta.lidar.control.cli.commands.operation_commands.Telescope;
import cat.ifae.cta.lidar.logging.Logging;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import picocli.CommandLine;

import java.util.concurrent.CountDownLatch;

@CommandLine.Command(name = "operation", description = "Operation commands",
        mixinStandardHelpOptions = true, subcommands = {Acquisition.class, Telescope.class, LowLevel.class})
public class Operation implements Runnable {
    private final static Logging log = new Logging(Operation.class);

    @CommandLine.Option(names = "--warmup", description = "Heat the laser")
    private boolean warmup = false;

    @CommandLine.Option(names = "--startup", description = "Start up normal mode")
    private boolean startup_normal_mode = false;

    @CommandLine.Option(names = "--shutdown", description = "Shutdown")
    private boolean shutdown = false;

    @CommandLine.Option(names = "--emergency-stop", description = "Execute emergency stop")
    private boolean _emergency_stop = false;

    @CommandLine.Option(names = "--ignore-humidity", description = "Ignore humidity check")
    private boolean _ignore_humidity = false;

    @CommandLine.Option(names = "--without-ramp", description = "Startup without ramp up")
    private boolean _disable_ramp = false;

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
            if(warmup) warmUp();
            else if(startup_normal_mode) startUpNormalMode();
            else if(shutdown) shutdownSequence();
            else if(_emergency_stop) emergencyStop();
            else printHelp();
        } catch (StatusRuntimeException e) {
            log.error(e.getLocalizedMessage());
        } catch(Exception e) {
            e.printStackTrace();
            log.error(e.toString());
        }
    }

    private void warmUp() throws InterruptedException {
        var finishedLatch = new CountDownLatch(1);

        var req = Flags.newBuilder().setDisableHumidity(_ignore_humidity).build();
        stub.warmUp(req, new StreamObserver<>() {
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

    private void startUpNormalMode() throws InterruptedException {
        var finishedLatch = new CountDownLatch(1);

        var p = getPosition();
        var point_req = Point2D.newBuilder().setX(p.getX()).setY(p.getY()).build();

        var vlts_0 = Configuration.pmt_dac_vlts_0;
        var vlts_1 = Configuration.pmt_dac_vlts_1;
        var vlts_2 = Configuration.pmt_dac_vlts_2;
        var vlts_3 = Configuration.pmt_dac_vlts_3;
        var vlts_4 = Configuration.pmt_dac_vlts_4;
        var vlts_5 = Configuration.pmt_dac_vlts_5;

        var req =
                InitSequenceOptions.newBuilder().setPosition(point_req)
                        .putPmtDacVoltages(0, vlts_0).putPmtDacVoltages(1, vlts_1)
                        .putPmtDacVoltages(2, vlts_2).putPmtDacVoltages(3, vlts_3)
                        .putPmtDacVoltages(4, vlts_4).putPmtDacVoltages(5, vlts_5)
                        .setDisableHumidity(_ignore_humidity).setDisableRamp(_disable_ramp).build();
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

    private void emergencyStop() throws InterruptedException {
        var finishedLatch = new CountDownLatch(1);

        var req = Null.newBuilder().build();
        stub.emergencyStop(req, new StreamObserver<>() {
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
        System.out.println("DAC Voltage#0: " + Configuration.pmt_dac_vlts_0);
        System.out.println("DAC Voltage#1: " + Configuration.pmt_dac_vlts_1);
        System.out.println("DAC Voltage#2: " + Configuration.pmt_dac_vlts_2);
        System.out.println("DAC Voltage#3 " + Configuration.pmt_dac_vlts_3);
        System.out.println("DAC Voltage#4: " + Configuration.pmt_dac_vlts_4);
        System.out.println("DAC Voltage#5: " + Configuration.pmt_dac_vlts_5);
        System.out.println("Position: " + getPosition());
    }

    private static java.awt.geom.Point2D getPosition() {
        var p_x = Configuration.arm_alignment_x;
        var p_y = Configuration.arm_alignment_y;

        return new java.awt.geom.Point2D.Float(p_x, p_y);
    }
}
