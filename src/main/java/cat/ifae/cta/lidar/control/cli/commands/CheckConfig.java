package cat.ifae.cta.lidar.control.cli.commands;

import picocli.CommandLine;

@CommandLine.Command(name = "check_config", description = "Check configuration")
public class CheckConfig implements Runnable {
   @Override
   public void run(){
      System.out.println("Configuration ok");
   }
}