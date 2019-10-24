package cat.ifae.cta.lidar.control.cli.commands.operation_commands;

import cat.ifae.cta.lidar.AcqConfig;
import cat.ifae.cta.lidar.FileID;
import cat.ifae.cta.lidar.LicelData;
import cat.ifae.cta.lidar.OperationGrpc;
import cat.ifae.cta.lidar.control.cli.Licli;
import cat.ifae.cta.lidar.logging.Logging;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.MessageFormat;

class DataSelection {
    private int d;

    void enablePHO() {
        d |= 1 << 0;
    }

    void enableLSW() {
        d |= 1 << 1;
    }

    void enableMSW() {
        d |= 1 << 2;
    }

    void enablePHM() {
        d |= 1 << 3;
    }

    void enableAnalogCombined() {
        d |= 1<<4;
    }

    void enableAnalogData() {
        d |= 1<<5;
    }

    void enableConvertedPHO() {
        d |= 1 << 6;
    }

    void enableFileID() {
        d |= 1 << 7;
    }

    int getBitmap() {
        return d;
    }
}

@CommandLine.Command(name = "acq", mixinStandardHelpOptions = true)
public class Acquisition implements Runnable {
    private final static Logging log = new Logging(Acquisition.class);

    @CommandLine.Option(names = "--download", description = "Download file with given ID")
    private int download_file_id = -1;

    @CommandLine.Option(names = "--start", description = "Start acquisition manually")
    private boolean acquisition_start = false;

    @CommandLine.Option(names = "--stop", description = "Stop Acquisition manually")
    private boolean acquisition_stop = false;

    @CommandLine.Option(names = "--shots", description = "Acquire a given number of shots")
    private int acquire_shots = 0;

    @CommandLine.Option(names = "--disc", required = true, description = "Discriminator level")
    private int disc = 0;

    private OperationGrpc.OperationBlockingStub blocking_stub;

    @Override
    public void run() {
        var ch = Licli.sm.getCh();

        blocking_stub = OperationGrpc.newBlockingStub(ch);
        blocking_stub = Licli.sm.addMetadata(blocking_stub);

        try {
            if(disc == 0) {
                System.out.println("Discriminator level must be set");
                return;
            }

            if(acquisition_start) acquisitionStart();
            else if(acquisition_stop) acquisitionStop();
            else if(download_file_id != -1) downloadFile();
            else if(acquire_shots != 0) {
                if(acquire_shots >= 2)
                    acquireShots();
                else
                    System.out.println("Minimum shot number is 2");
            }
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

    private void acquireShots() throws IOException {
        var t = new DataSelection();
        t.enableAnalogData();
        t.enableFileID();
        var b = t.getBitmap();
        var req = AcqConfig.newBuilder().setMaxBins(16381).setDiscriminator(disc).setShots(acquire_shots).setDataToRetrieve(b).build();
        var resp = blocking_stub.acquireShots(req);
        writeDataToFiles(resp);
    }

    private void acquisitionStart() {
        var t = new DataSelection();
        t.enableAnalogData();
        t.enableFileID();
        var b = t.getBitmap();
        var req = AcqConfig.newBuilder().setMaxBins(16381).setDiscriminator(3).setDataToRetrieve(b).build();
        blocking_stub.acquisitionStart(req);
    }

    private void acquisitionStop() throws IOException {
        var t = new DataSelection();
        t.enableAnalogData();
        t.enableFileID();
        var b = t.getBitmap();
        var req = AcqConfig.newBuilder().setMaxBins(16381).setDataToRetrieve(b).build();
        var resp = blocking_stub.acquisitionStop(req);
        System.out.println("File ID: " + resp.getAnalogLicelFileId());
        writeDataToFiles(resp);
    }

    private void writeDataToFiles(LicelData resp) throws IOException {
        {
            var writer = new BufferedWriter(new FileWriter("data/analog_combined_converted_data_0.out"));
            for (var v : resp.getData(0).getAnalogConvertedList()) {
                writer.write(MessageFormat.format("{0} ", String.valueOf(v)));
            }

            writer.close();
        }

        {
            var writer = new BufferedWriter(new FileWriter("data/analog_combined_converted_data_1.out"));
            for (var v : resp.getData(1).getAnalogConvertedList()) {
                writer.write(MessageFormat.format("{0} ", String.valueOf(v)));
            }

            writer.close();
        }
    }
}