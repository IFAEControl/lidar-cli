package cat.ifae.cta.lidar.control.cli;

import cat.ifae.cta.lidar.config.Config;

public class Configuration {
   private static final Config cfg = new Config("client", "micro_init_sequence");;

   public static final String VERSION = "1.0.0";

   public static final Float arm_alignment_x = cfg.getFloat("allignment_arm_X");
   public static final Float arm_alignment_y = cfg.getFloat("allignment_arm_Y");
   public static final Integer pmt_dac_voltage = cfg.getInteger("pmt_dac_voltage");

   void checkConfiguration() {
   }
}
