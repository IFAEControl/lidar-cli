package cat.ifae.cta.lidar.control.cli.commands.operation_commands;

import cat.ifae.cta.lidar.AcqConfig;
import cat.ifae.cta.lidar.FileID;
import cat.ifae.cta.lidar.LicelData;
import cat.ifae.cta.lidar.OperationGrpc;
import cat.ifae.cta.lidar.control.cli.Licli;
import cat.ifae.cta.lidar.logging.Logging;
import io.grpc.StatusRuntimeException;
import net.harawata.appdirs.AppDirs;
import net.harawata.appdirs.AppDirsFactory;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

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
    private final static AppDirs appDirs = AppDirsFactory.getInstance();

    private final DataSelection __desired_data;
    private final String _data_dir;

    LicelRespWriter(DataSelection d) {
        __desired_data = d;
        _data_dir = get_dir();
    }

    void write(LicelData resp) throws IOException {
        if(__desired_data.isAnalogCombined()) {
            writeAnalogCombined(resp);
        }
    }

    private void writeAnalogCombined(LicelData resp) throws IOException {
        {
            var writer = get_writer("analog_combined_converted_0.out");
            for (var v : resp.getData(0).getAnalogConvertedList()) {
                writer.write(MessageFormat.format("{0} ", String.valueOf(v)));
            }

            writer.close();
        }

        {
            var writer = get_writer("analog_combined_converted_1.out");
            for (var v : resp.getData(1).getAnalogConvertedList()) {
                writer.write(MessageFormat.format("{0} ", String.valueOf(v)));
            }

            writer.close();
        }
    }

    private String get_dir() {
        var data_dir = appDirs.getUserDataDir("lidar", "0.1", "ifae");
        data_dir += "/" + new SimpleDateFormat("yyyyMMdd.HHmmss").format(new Date());

        var dir = new File(data_dir);
        if(! dir.exists())
            dir.mkdirs();

        return data_dir;
    }

    private BufferedWriter get_writer(String f_name) throws IOException {
        return new BufferedWriter(new FileWriter(_data_dir + "/" + f_name));
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

    private OperationGrpc.OperationBlockingStub blocking_stub;

    @Override
    public void run() {
        var ch = Licli.sm.getCh();

        blocking_stub = OperationGrpc.newBlockingStub(ch);
        blocking_stub = Licli.sm.addMetadata(blocking_stub);

        try {
            var ds = new DataSelection();
            ds.enableFileID();
            if(_analog_data) ds.enableAnalogData();

            if (disc == 0) {
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
            log.error(e.getStatus().getCause().getLocalizedMessage());
        } catch(Exception e) {
            e.printStackTrace();
            log.error(e.toString());
        }
    }

    private void downloadFile() {
        var req = FileID.newBuilder().setId(download_file_id).build();
        var resp = blocking_stub.downloadFile(req);
        System.out.println(new String(resp.getData().toByteArray()));
    }

    private void acquireShots(DataSelection ds) throws IOException {
        var b = ds.getBitmap();
        var req = AcqConfig.newBuilder().setMaxBins(16381).setDiscriminator(disc).setShots(acquire_shots).setDataToRetrieve(b).build();
        var resp = blocking_stub.acquireShots(req);
        System.out.println("File ID: " + resp.getAnalogLicelFileId());
        new LicelRespWriter(ds).write(resp);
    }

    private void acquisitionStart(DataSelection ds) {
        var b = ds.getBitmap();
        var req = AcqConfig.newBuilder().setMaxBins(16381).setDiscriminator(3).setDataToRetrieve(b).build();
        blocking_stub.acquisitionStart(req);
    }

    private void acquisitionStop(DataSelection ds) throws IOException {
        var b = ds.getBitmap();
        var req = AcqConfig.newBuilder().setMaxBins(16381).setDataToRetrieve(b).build();
        var resp = blocking_stub.acquisitionStop(req);
        System.out.println("File ID: " + resp.getAnalogLicelFileId());
        new LicelRespWriter(ds).write(resp);
    }
}