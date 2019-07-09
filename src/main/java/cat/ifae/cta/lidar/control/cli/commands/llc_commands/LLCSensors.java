package cat.ifae.cta.lidar.control.cli.commands.llc_commands;

import cat.ifae.cta.lidar.Data;
import cat.ifae.cta.lidar.Null;
import cat.ifae.cta.lidar.Raw;
import cat.ifae.cta.lidar.LLCSensorsGrpc;
import cat.ifae.cta.lidar.control.cli.Licli;
import picocli.CommandLine;

@CommandLine.Command(name = "sensors", mixinStandardHelpOptions = true)
public
class LLCSensors implements Runnable {
    private LLCSensorsGrpc.LLCSensorsBlockingStub stub;

    @CommandLine.Option(names = "--raw", description = "If true get raw data, otherwise get converted data")
    private boolean get_raw = false;

    @CommandLine.Option(names = "--converted", description = "If true get raw data, otherwise get converted data")
    private boolean get_conv = false;

    @Override
    public void run() {
        stub = LLCSensorsGrpc.newBlockingStub(Licli.sm.getCh());
        stub = Licli.sm.addMetadata(stub);

        CommandLine.populateCommand(this);

        try {
            if(get_raw) getRawData();
            else if(get_conv) getConvertedData();
        } catch(Exception e) {
            e.printStackTrace();
            System.out.println(e.toString());
        }
    }

    private void getRawData() {
        Null req = Null.newBuilder().build();
        Raw resp = stub.getRawData(req);
        System.out.println(resp);

    }

    private void getConvertedData() {
        Null req = Null.newBuilder().build();
        Data resp = stub.getConvertedData(req);
        for(int i = 0; i < resp.getDataCount(); i++) {
            System.out.println(resp.getName(i) + ": " + resp.getData(i) + resp.getUnit(i));
        }

    }
}
