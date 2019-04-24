package cat.ifae.cta.lidar.control.cli;

import cat.ifae.cta.lidar.Null;
import cat.ifae.cta.lidar.TelescopTestsGrpc;
import picocli.CommandLine;

@CommandLine.Command(name = "telescope", mixinStandardHelpOptions = true)
class Telescope implements Runnable {
    private TelescopTestsGrpc.TelescopTestsBlockingStub stub;

    @CommandLine.Option(names = "-test", description = "Test telescopeJ")
    private boolean test = false;

    @CommandLine.ParentCommand
    private Control parent;

    @Override
    public void run() {
        stub = TelescopTestsGrpc.newBlockingStub(parent.grpc.channel);
        stub = parent.grpc.addMetadata(stub);

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