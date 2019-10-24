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
        } catch (StatusRuntimeException e) {
            log.error(e.getStatus().getCause().getLocalizedMessage());
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
