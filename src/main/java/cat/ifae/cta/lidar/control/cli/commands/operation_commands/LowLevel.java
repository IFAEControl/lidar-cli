package cat.ifae.cta.lidar.control.cli.commands.operation_commands;

import cat.ifae.cta.lidar.*;
import cat.ifae.cta.lidar.control.cli.Configuration;
import cat.ifae.cta.lidar.control.cli.Licli;
import cat.ifae.cta.lidar.logging.Logging;
import io.grpc.StatusRuntimeException;
import picocli.CommandLine;

import java.text.MessageFormat;

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
        } catch (StatusRuntimeException e) {
            log.error(e.getStatus().getCause().getLocalizedMessage());
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

    private void initSequence() {
        var p = getPosition();
        var point_req = Point2D.newBuilder().setX(p.getX()).setY(p.getY()).build();
        var req = InitSequenceOptions.newBuilder().setHotwindTmep(getTemperature()).setPosition(point_req).setPmtDacVoltage(getDacVoltage()).build();
        blocking_stub.executeMicroInitSequence(req);
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
