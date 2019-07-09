package cat.ifae.cta.lidar.control.cli.commands;

import cat.ifae.cta.lidar.ConfigGrpc;
import cat.ifae.cta.lidar.Null;
import cat.ifae.cta.lidar.control.cli.Licli;
import picocli.CommandLine;

@CommandLine.Command(name = "config", mixinStandardHelpOptions = true, description = "See or change server configuration")
public class Config implements Runnable {
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

        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    private void printConfig() {
        var req = Null.newBuilder().build();
        var resp = stub.getConfig(req);
        System.out.println(resp);
    }
}
