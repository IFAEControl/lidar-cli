package cat.ifae.cta.lidar.control.cli;

import cat.ifae.cta.lidar.config.Config;

public class Configuration {
   public static final String VERSION = "1.0.2";

   public static class Acquisition {
      private static final Config cfg = new Config("client", "acquisition");

      public static final Integer wl_ch_1 = cfg.getInteger("wavelength_ch_1");
      public static final Integer wl_ch_2 = cfg.getInteger("wavelength_ch_2");
      public static final Integer wl_ch_3 = cfg.getInteger("wavelength_ch_3");
      public static final Integer wl_ch_4 = cfg.getInteger("wavelength_ch_4");
   }

   private static final Config cfg = new Config("client", "micro_init_sequence");

   public static final Float arm_alignment_x = cfg.getFloat("allignment_arm_X");
   public static final Float arm_alignment_y = cfg.getFloat("allignment_arm_Y");
   public static final Integer pmt_dac_voltage = cfg.getInteger("pmt_dac_voltage");

   void checkConfiguration() {
      if(Acquisition.wl_ch_1 > 5 || Acquisition.wl_ch_2 > 5 || Acquisition.wl_ch_3 > 5 ||
         Acquisition.wl_ch_4 > 5)
         throw new RuntimeException("Wavelength can not have more than 5 characters");
   }
}
