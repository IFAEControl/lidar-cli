package cat.ifae.cta.lidar.control.cli.commands.operation_commands;

import cat.ifae.cta.lidar.AcqConfig;
import cat.ifae.cta.lidar.FileContent;
import cat.ifae.cta.lidar.FileID;
import cat.ifae.cta.lidar.LicelData;
import cat.ifae.cta.lidar.OperationGrpc;
import cat.ifae.cta.lidar.control.cli.PathUtilsCli;
import cat.ifae.cta.lidar.control.cli.Licli;
import cat.ifae.cta.lidar.logging.Logging;
import io.grpc.StatusRuntimeException;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.text.MessageFormat;

class DataSelection {
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
        d |= 1<<4;
    }

    boolean isAnalogCombined() {
        return ((d >> 4) & 0b1) == 1;
    }

    // Combined and converted
    void enableAnalogData() {
        d |= 1<<5;
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

    int getBitmap() {
        return d;
    }
}

class LicelRespWriter {
    private final static PathUtilsCli _utils = new PathUtilsCli();

    private final DataSelection _desired_data;
    private final OperationGrpc.OperationBlockingStub _blocking_stub;

    LicelRespWriter(DataSelection d, OperationGrpc.OperationBlockingStub b) {
        _desired_data = d;
        _blocking_stub = b;
    }

    void write(LicelData resp) throws IOException {
        if(_desired_data.isLSW() || _desired_data.isMSW())
            writeRawData(resp);
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
        {
            var writer = _utils.getFileWriter("raw_lsw_0");
            for (var v : resp.getData(0).getLsw()) {
                writer.write(MessageFormat.format("{0} ", String.valueOf(v)).getBytes());
            }

            writer.close();
        }

        {
            var writer = _utils.getFileWriter("raw_lsw_1");
            for (var v : resp.getData(1).getLsw()) {
                writer.write(MessageFormat.format("{0} ", String.valueOf(v)).getBytes());
            }

            writer.close();
        }

        {
            var writer = _utils.getFileWriter("raw_msw_0");
            for (var v : resp.getData(0).getMsw()) {
                writer.write(MessageFormat.format("{0} ", String.valueOf(v)).getBytes());
            }

            writer.close();
        }

        {
            var writer = _utils.getFileWriter("raw_msw_1");
            for (var v : resp.getData(1).getMsw()) {
                writer.write(MessageFormat.format("{0} ", String.valueOf(v)).getBytes());
            }

            writer.close();
        }
    }

    private void writeAnalogCombined(LicelData resp) throws IOException {
        {
            var writer = _utils.getFileWriter("analog_combined_0");
            for (var v : resp.getData(0).getAnalogCombinedList()) {
                writer.write(MessageFormat.format("{0} ", String.valueOf(v)).getBytes());
            }

            writer.close();
        }

        {
            var writer = _utils.getFileWriter("analog_combined_1.out");
            for (var v : resp.getData(1).getAnalogCombinedList()) {
                writer.write(MessageFormat.format("{0} ", String.valueOf(v)).getBytes());
            }

            writer.close();
        }
    }

    private void writeAnalogConverted(LicelData resp) throws IOException {
        {
            var writer = _utils.getFileWriter("analog_combined_converted_0.out");
            for (var v : resp.getData(0).getAnalogConvertedList()) {
                writer.write(MessageFormat.format("{0} ", String.valueOf(v)).getBytes());
            }

            writer.close();
        }

        {
            var writer = _utils.getFileWriter("analog_combined_converted_1.out");
            for (var v : resp.getData(1).getAnalogConvertedList()) {
                writer.write(MessageFormat.format("{0} ", String.valueOf(v)).getBytes());
            }

            writer.close();
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
        var writer = _utils.getFileWriter("licel_file.out");
        writer.write(_file_content.getData().toByteArray());
        writer.close();
    }
}

@CommandLine.Command(name = "acq", mixinStandardHelpOptions = true)
public class Acquisition implements Runnable {
    private final static Logging log = new Logging(Acquisition.class);

    @Option(names = "--download", description = "Download file with given ID")
    private int download_file_id = -1;

    @Option(names = "--start", description = "Start acquisition manually")
    private boolean acquisition_start = false;

    @Option(names = "--stop", description = "Stop Acquisition manually")
    private boolean acquisition_stop = false;

    @Option(names = "--shots", description = "Acquire a given number of shots")
    private int acquire_shots = 0;

    @Option(names = "--disc", required = true, description = "Discriminator level")
    private int disc = 0;

    @Option(names = "--analog-data", description = "Get converted and normalized analog data")
    private boolean _analog_data = false;

    @Option(names = "--analog-combined", description = "Get combined data without normalization")
    private boolean _analog_combined_data = false;

    @Option(names = "--enable-lsw-msw", description = "Get raw data parts")
    private boolean _lsw_msw_data = false;

    private OperationGrpc.OperationBlockingStub blocking_stub;

    @Override
    public void run() {
        var ch = Licli.sm.getCh();

        blocking_stub = OperationGrpc.newBlockingStub(ch);
        blocking_stub = Licli.sm.addMetadata(blocking_stub);

        try {
            var ds = new DataSelection();
            ds.enableFileID();
            if(_analog_combined_data) ds.enableAnalogCombined();
            if(_analog_data) ds.enableAnalogData();
            if(_lsw_msw_data) {
                ds.enableLSW();
                ds.enableMSW();
            }

            if (download_file_id == -1 && disc == 0) {
                System.out.println("Discriminator level must be set");
                return;
            }

            if (acquisition_start) acquisitionStart(ds);
            else if (acquisition_stop) acquisitionStop(ds);
            else if (download_file_id != -1) downloadFile();
            else if (acquire_shots != 0) {
                if (acquire_shots >= 2)
                    acquireShots(ds);
                else
                    System.err.println("Minimum shot number is 2");
            }
        } catch (StatusRuntimeException e) {
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

    private void acquireShots(DataSelection ds) throws IOException {
        var b = ds.getBitmap();
        var req = AcqConfig.newBuilder().setMaxBins(16381).setDiscriminator(disc).setShots(acquire_shots).setDataToRetrieve(b).build();
        var resp = blocking_stub.acquireShots(req);
        System.out.println("File ID: " + resp.getAnalogLicelFileId());
        new LicelRespWriter(ds, blocking_stub).write(resp);
    }

    private void acquisitionStart(DataSelection ds) {
        var b = ds.getBitmap();
        var req =
                AcqConfig.newBuilder().setMaxBins(16381).setDiscriminator(disc).setDataToRetrieve(b).build();
        blocking_stub.acquisitionStart(req);
    }

    private void acquisitionStop(DataSelection ds) throws IOException {
        var b = ds.getBitmap();
        var req = AcqConfig.newBuilder().setMaxBins(16381).setDataToRetrieve(b).build();
        var resp = blocking_stub.acquisitionStop(req);
        System.out.println("File ID: " + resp.getAnalogLicelFileId());
        new LicelRespWriter(ds, blocking_stub).write(resp);
    }
}