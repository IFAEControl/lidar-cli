package cat.ifae.cta.lidar.control.cli.commands;

import cat.ifae.cta.lidar.ConfigGrpc;
import cat.ifae.cta.lidar.Null;
import cat.ifae.cta.lidar.Options;
import cat.ifae.cta.lidar.control.cli.Licli;
import cat.ifae.cta.lidar.logging.Logging;
import io.grpc.StatusRuntimeException;
import picocli.CommandLine;

@CommandLine.Command(name = "config", mixinStandardHelpOptions = true, description = "See or change server configuration")
public class Config implements Runnable {
    private final static Logging _log = new Logging(Config.class);

    private ConfigGrpc.ConfigBlockingStub stub;

    @CommandLine.Option(names = "--get")
    boolean is_get = false;

    @Override
    public void run(){
        stub = ConfigGrpc.newBlockingStub(Licli.sm.getCh());
        stub = Licli.sm.addMetadata(stub);

        CommandLine.populateCommand(this);

        try {
            if(is_get) printConfig();
        } catch (StatusRuntimeException e) {
            _log.error(e.getLocalizedMessage());
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    private void printConfig() {
        var req = Null.newBuilder().build();
        var resp = stub.getConfig(req);
        System.out.println(resp);

        // Example of how to change config. All values must be set.
        /*var t = resp.getTargetTemperature()-1;
        var builder = Options.newBuilder(resp);
        builder.setTargetTemperature(t);
        stub.setConfig(builder.build());*/

        //System.out.println(builder.getTargetTemperature());
    }
}
