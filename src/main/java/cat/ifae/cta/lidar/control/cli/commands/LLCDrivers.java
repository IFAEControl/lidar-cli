package cat.ifae.cta.lidar.control.cli.commands;

import cat.ifae.cta.lidar.LLCDriversGrpc;
import cat.ifae.cta.lidar.Index;
import cat.ifae.cta.lidar.Null;
import cat.ifae.cta.lidar.StatusArray;
import cat.ifae.cta.lidar.control.cli.Licli;
import picocli.CommandLine;

@CommandLine.Command(name = "drivers", mixinStandardHelpOptions = true)
public
class LLCDrivers implements Runnable {
    private LLCDriversGrpc.LLCDriversBlockingStub stub;

    @CommandLine.ParentCommand
    private Licli parent;

    @CommandLine.Option(names = "-gs", description = "Get status")
    private boolean get_status;

    @Override
    public void run() {
        stub = LLCDriversGrpc.newBlockingStub(parent.sm.getCh());
        stub = parent.sm.addMetadata(stub);

        CommandLine.populateCommand(this);

        try {
            if (get_status) getStatus();
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    private void getStatus() {
        Null req = Null.newBuilder().build();
        StatusArray resp = stub.getStatus(req);
        System.out.println("Driver's Status: ");
        for (int i=0; i< resp.getStatusCount(); i++){
            if(resp.getStatus(i).getStatus()) System.out.print("ON");
            else System.out.print("OFF");
            var req2 = Index.newBuilder().setIndex(i).build();
            var resp2 = stub.getName(req2);
            System.out.println("\t"+resp2.getStr());
        }
    }

}