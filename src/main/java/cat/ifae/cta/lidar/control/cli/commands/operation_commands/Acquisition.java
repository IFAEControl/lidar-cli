package cat.ifae.cta.lidar.control.cli.commands.operation_commands;

import cat.ifae.cta.lidar.*;
import cat.ifae.cta.lidar.control.cli.Configuration;
import cat.ifae.cta.lidar.control.cli.PathUtilsCli;
import cat.ifae.cta.lidar.control.cli.Licli;
import cat.ifae.cta.lidar.logging.Logging;
import io.grpc.StatusRuntimeException;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.text.MessageFormat;

class DataSelection {
   // bit 0: pho
   // bit 1: lsw
   // bit 2: msw
   // bit 3: phm
   // bit 4: analog combined
   // bit 5: converted analog data
   // bit 6: converted pho data
   // bit 7: returns a file ID
   // bit 8: p2l
   // bit 9: p2m
   // bit 10: squared photon
   // bit 11: a2l
   // bit 12: a2m
   // bit 13: a2h
   private int d;

   void enablePHO() {
      d |= 1 << 0;
   }

   boolean isPHO() {
      return ((d >> 0) & 0b1) == 1;
   }

   void enableLSW() {
      d |= 1 << 1;
   }

   boolean isLSW() {
      return ((d >> 1) & 0b1) == 1;
   }

   void enableMSW() {
      d |= 1 << 2;
   }

   boolean isMSW() {
      return ((d >> 2) & 0b1) == 1;
   }

   void enablePHM() {
      d |= 1 << 3;
   }

   boolean isPHM() {
      return ((d >> 3) & 0b1) == 1;
   }

   void enableAnalogCombined() {
      d |= 1 << 4;
   }

   boolean isAnalogCombined() {
      return ((d >> 4) & 0b1) == 1;
   }

   // Combined and converted
   void enableAnalogData() {
      d |= 1 << 5;
   }

   boolean isAnalogData() {
      return ((d >> 5) & 0b1) == 1;
   }

   void enableConvertedPHO() {
      d |= 1 << 6;
   }

   boolean isConvertedPHO() {
      return ((d >> 6) & 0b1) == 1;
   }

   void enableFileID() {
      d |= 1 << 7;
   }

   boolean isFileID() {
      return ((d >> 7) & 0b1) == 1;
   }

   void enableP2L() {
      d |= 1 << 8;
   }

   boolean isP2L() {
      return ((d >> 9) & 0b1) == 1;
   }

   void enableP2M() {
      d |= 1 << 9;
   }

   boolean isP2M() {
      return ((d >> 9) & 0b1) == 1;
   }

   void enableSquaredPho() {
      d |= 1 << 10;
   }

   boolean isSquaredPho() {
      return ((d >> 10) & 0b1) == 1;
   }

   void enableA2L() {
      d |= 1 << 11;
   }

   boolean isA2L() {
      return ((d >> 11) & 0b1) == 1;
   }

   void enableA2M() {
      d |= 1 << 12;
   }

   boolean isA2M() {
      return ((d >> 12) & 0b1) == 1;
   }

   void enableA2H() {
      d |= 1 << 13;
   }

   boolean isA2H() {
      return ((d >> 13) & 0b1) == 1;
   }

   int getBitmap() {
      return d;
   }
}

class LicelRespWriter {
   private final static PathUtilsCli _utils = new PathUtilsCli();
   private final static int _num_tr = 4;

   private final DataSelection _desired_data;
   private final OperationGrpc.OperationBlockingStub _blocking_stub;

   LicelRespWriter(DataSelection d, OperationGrpc.OperationBlockingStub b) {
      _desired_data = d;
      _blocking_stub = b;
   }

   void write(LicelData resp) throws IOException {
      if(_desired_data.isLSW() || _desired_data.isMSW())
         writeRawData(resp);
      if(_desired_data.isA2L() || _desired_data.isA2M() || _desired_data.isA2H())
         writeRawSquaredData(resp);
      if(_desired_data.isAnalogCombined())
         writeAnalogCombined(resp);
      if(_desired_data.isAnalogData())
         writeAnalogConverted(resp);
      if(_desired_data.isFileID()) {
         var req_file = FileID.newBuilder().setId(resp.getAnalogLicelFileId()).build();
         var resp_file = _blocking_stub.downloadFile(req_file);
         new LicelFormatFileWriter(resp_file).write();
      }
   }

   private void writeRawData(LicelData resp) throws IOException {
      for(int i = 0; i < _num_tr; i++) {
         var writer = _utils.getFileWriter("raw_lsw_" + i);
         for(var v : resp.getData(i).getLsw()) {
            writer.write(v);
         }
         writer.close();
         writer.updateLatest();
      }

      for(int i = 0; i < _num_tr; i++) {
         var writer = _utils.getFileWriter("raw_msw_" + i);
         for(var v : resp.getData(i).getMsw()) {
            writer.write(v);
         }

         writer.close();
         writer.updateLatest();
      }
   }

   private void writeRawSquaredData(LicelData resp) throws IOException {
      for(int i = 0; i < _num_tr; i++) {
         var writer = _utils.getFileWriter("raw_a2l_" + i);
         for(var v : resp.getData(i).getA2L()) {
            writer.write(v);
         }
         writer.close();
         writer.updateLatest();
      }

      for(int i = 0; i < _num_tr; i++) {
         var writer = _utils.getFileWriter("raw_a2m_" + i);
         for(var v : resp.getData(i).getA2M()) {
            writer.write(v);
         }

         writer.close();
         writer.updateLatest();
      }

      for(int i = 0; i < _num_tr; i++) {
         var writer = _utils.getFileWriter("raw_a2h_" + i);
         for(var v : resp.getData(i).getA2H()) {
            writer.write(v);
         }

         writer.close();
         writer.updateLatest();
      }
   }

   private void writeAnalogCombined(LicelData resp) throws IOException {
      for(int i = 0; i < _num_tr; i++) {
         var writer = _utils.getFileWriter("analog_combined_" + i);
         for(var v : resp.getData(i).getAnalogCombinedList()) {
            writer.write(MessageFormat.format("{0} ", String.valueOf(v)).getBytes());
         }

         writer.close();
         writer.updateLatest();
      }
   }

   private void writeAnalogConverted(LicelData resp) throws IOException {
      for(int i = 0; i < _num_tr; i++) {
         var writer = _utils.getFileWriter("analog_combined_converted_" + i);
         for(var v : resp.getData(i).getAnalogConvertedList()) {
            writer.write(MessageFormat.format("{0} ", String.valueOf(v)).getBytes());
         }

         writer.close();
         writer.updateLatest();
      }
   }
}

class LicelFormatFileWriter {
   private final static PathUtilsCli _utils = new PathUtilsCli();

   private final FileContent _file_content;

   LicelFormatFileWriter(FileContent file_content) {
      _file_content = file_content;
   }

   void write() throws IOException {
      var writer = _utils.getFileWriter("licel_file");
      writer.write(_file_content.getData().toByteArray());
      writer.close();
      writer.updateLatest();
   }
}

@CommandLine.Command(name = "acq", mixinStandardHelpOptions = true)
public class Acquisition implements Runnable {
   private final static Logging log = new Logging(Acquisition.class);
   private final int _max_bins = 16380;

   @Option(names = "--download", description = "Download file with given ID")
   private int download_file_id = -1;

   @Option(names = "--start", description = "Start acquisition manually")
   private boolean acquisition_start = false;

   @Option(names = "--stop", description = "Stop Acquisition manually")
   private boolean acquisition_stop = false;

   @Option(names = "--shots", description = "Acquire a given number of shots")
   private int acquire_shots = 0;

   @Option(names = "--disc", description = "Discriminator level for all TR (format=disc1:disc2:...)")
   private String _disc;

   @Option(names = "--analog-data", description = "Get converted and normalized analog data")
   private boolean _analog_data = false;

   @Option(names = "--analog-combined", description = "Get combined data without normalization")
   private boolean _analog_combined_data = false;

   @Option(names = "--raw-analog", description = "Get raw data parts")
   private boolean _raw_analog_data = false;

   @Option(names = "--raw-squared", description = "Get raw data parts")
   private boolean _raw_squared_data = false;

   private OperationGrpc.OperationBlockingStub blocking_stub;

   @Override
   public void run() {
      // Number of TR units installed
      var tr_num = 4;

      var ch = Licli.sm.getCh();

      blocking_stub = OperationGrpc.newBlockingStub(ch);
      blocking_stub = Licli.sm.addMetadata(blocking_stub);

      try {
         var ds = new DataSelection();
         ds.enableFileID();

         if(_analog_combined_data)
            ds.enableAnalogCombined();

         if(_analog_data)
            ds.enableAnalogData();

         if(_raw_analog_data) {
            ds.enableLSW();
            ds.enableMSW();
         }

         if(_raw_squared_data) {
            ds.enableA2L();
            ds.enableA2M();
            ds.enableA2H();
         }

         if(download_file_id == -1) {
            if(_disc.equals("")) {
               System.out.println("Discriminator level must be set");
               return;
            }
         }

         var wl1 = Configuration.Acquisition.wl_ch_1;
         var wl2 = Configuration.Acquisition.wl_ch_2;
         var wl3 = Configuration.Acquisition.wl_ch_3;
         var wl4 = Configuration.Acquisition.wl_ch_4;

         var disc_parts = Helpers.split(_disc, tr_num);

         var builder =
                 AcqConfig.newBuilder().setMaxBins(_max_bins).putWavelengths(1, wl1)
                         .putWavelengths(2, wl2).putWavelengths(3, wl3).putWavelengths(4, wl4);

         for(var d : disc_parts)
            builder.addDiscriminator(Integer.parseInt(d));

         if(acquisition_start)
            acquisitionStart(builder, ds);
         else if(acquisition_stop)
            acquisitionStop(builder, ds);
         else if(acquire_shots != 0) {
            if(acquire_shots >= 2 && acquire_shots <= 4096)
               acquireShots(builder, ds);
            else if(acquire_shots > 4096)
               System.err.println("Maximum shot number is limited 4096 due to hardware limitations of one TR unit");
            else
               System.err.println("Minimum shot number is 2");
         } else if(download_file_id != -1) {
            downloadFile();
         }
      } catch(StatusRuntimeException e) {
         log.error(e.getLocalizedMessage());
      } catch(Exception e) {
         e.printStackTrace();
         log.error(e.toString());
      }
   }

   private void downloadFile() throws IOException {
      var req = FileID.newBuilder().setId(download_file_id).build();
      var resp = blocking_stub.downloadFile(req);
      new LicelFormatFileWriter(resp).write();
   }

   private void acquireShots(AcqConfig.Builder builder, DataSelection ds) throws IOException {
      System.out.println("Remember: laser must be firing in order to take data");
      var b = ds.getBitmap();
      var req = builder.setShots(acquire_shots).setDataToRetrieve(b).build();
      var resp = blocking_stub.acquireShots(req);
      System.out.println("File ID: " + resp.getAnalogLicelFileId());
      new LicelRespWriter(ds, blocking_stub).write(resp);
   }

   private void acquisitionStart(AcqConfig.Builder builder, DataSelection ds) {
      System.out.println("Remember: laser must be firing in order to take data");
      var b = ds.getBitmap();
      var req = builder.setDataToRetrieve(b)
              .build();
      blocking_stub.acquisitionStart(req);
   }

   private void acquisitionStop(AcqConfig.Builder builder, DataSelection ds) throws IOException {
      var b = ds.getBitmap();
      var req = builder.setDataToRetrieve(b).build();
      var resp = blocking_stub.acquisitionStop(req);
      System.out.println("File ID: " + resp.getAnalogLicelFileId());
      new LicelRespWriter(ds, blocking_stub).write(resp);
   }
}
