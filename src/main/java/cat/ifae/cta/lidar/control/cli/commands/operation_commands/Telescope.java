package cat.ifae.cta.lidar.control.cli.commands.operation_commands;

import cat.ifae.cta.lidar.EncoderPosition;
import cat.ifae.cta.lidar.Inclination;
import cat.ifae.cta.lidar.Null;
import cat.ifae.cta.lidar.TelescopeGrpc;
import cat.ifae.cta.lidar.control.cli.Licli;
import cat.ifae.cta.lidar.logging.Logging;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import picocli.CommandLine;

import java.util.concurrent.CountDownLatch;

public @CommandLine.Command(name = "telescope", mixinStandardHelpOptions = true)
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

    @CommandLine.Option(names = "--go-zenith", description = "Go to zenith inclination in degrees")
    private int zenith_inclination = -999;

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
            else if(zenith_inclination != -999) goToZenith();
        } catch (StatusRuntimeException e) {
            log.error(e.getLocalizedMessage());
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

    private void goToZenith() {
        var req = Inclination.newBuilder().setDegrees(zenith_inclination).build();
        blocking_stub.goToZenithInclination(req);
    }

}
