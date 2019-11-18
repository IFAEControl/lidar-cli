package cat.ifae.cta.lidar.control.cli.commands.llc_commands;

import cat.ifae.cta.lidar.DacConfig;
import cat.ifae.cta.lidar.LLCDacGrpc;
import cat.ifae.cta.lidar.Helpers;
import cat.ifae.cta.lidar.control.cli.Licli;
import cat.ifae.cta.lidar.logging.Logging;
import io.grpc.StatusRuntimeException;
import picocli.CommandLine;

@CommandLine.Command(name = "dac", mixinStandardHelpOptions = true)
public
class LLCDac implements Runnable {
    private final static Logging _log = new Logging(LLCDac.class);

    private LLCDacGrpc.LLCDacBlockingStub stub;

    @CommandLine.Option(names = "--set-voltage", description = "Set voltage. format=dac:voltage")
    private String dac_setting;

    @Override
    public void run() {
        stub = LLCDacGrpc.newBlockingStub(Licli.sm.getCh());
        stub = Licli.sm.addMetadata(stub);

        CommandLine.populateCommand(this);

        try {
            if (!dac_setting.isEmpty()) setVoltage(dac_setting);
        } catch (StatusRuntimeException e) {
            _log.error(e.getLocalizedMessage());
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    private void setVoltage(String s) {
        String[] components = Helpers.split(s, 2);

        int dac = Integer.parseInt(components[0]);
        int voltage = Integer.parseInt(components[1]);

        DacConfig c = DacConfig.newBuilder().setNumber(dac).setVoltage(voltage).build();
        stub.setVoltage(c);
    }

}