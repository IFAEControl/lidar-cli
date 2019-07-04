package cat.ifae.cta.lidar.control.cli.commands;


import cat.ifae.cta.lidar.control.cli.commands.llc_commands.*;
import picocli.CommandLine;

@CommandLine.Command(name = "llc", description = "Commands of the low level control board", mixinStandardHelpOptions = true,
        subcommands = {LLCArm.class, LLCDac.class, LLCDrivers.class, LLCHotWind.class, LLCLaser.class, LLCRelay.class,
                LLCSensors.class})
public class LLControl implements Runnable {
    @Override
    public void run() {
        CommandLine.populateCommand(this);
    }
}
