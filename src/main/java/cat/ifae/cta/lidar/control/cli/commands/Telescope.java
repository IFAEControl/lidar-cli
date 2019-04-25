package cat.ifae.cta.lidar.control.cli.commands;

import cat.ifae.cta.lidar.Null;
import cat.ifae.cta.lidar.TelescopTestsGrpc;
import cat.ifae.cta.lidar.control.cli.Licli;
import picocli.CommandLine;

@CommandLine.Command(name = "telescope", mixinStandardHelpOptions = true)
public
class Telescope implements Runnable {
    private TelescopTestsGrpc.TelescopTestsBlockingStub stub;

    @CommandLine.Option(names = "-test", description = "Test telescopeJ")
    private boolean test = false;

    @CommandLine.ParentCommand
    private Licli parent;

    @Override
    public void run() {
        stub = TelescopTestsGrpc.newBlockingStub(parent.sm.getCh());
        stub = parent.sm.addMetadata(stub);

        CommandLine.populateCommand(this);

        try {
            if(test) telescopeTest();
        } catch(Exception e) {
            System.out.println(e.toString());
        }
    }

    private void telescopeTest() {
        Null req = Null.newBuilder().build();
        stub.executeTests(req);
    }

}