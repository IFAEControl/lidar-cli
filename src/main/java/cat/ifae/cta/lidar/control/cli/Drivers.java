package cat.ifae.cta.lidar.control.cli;

import cat.ifae.cta.lidar.DriversGrpc;
import cat.ifae.cta.lidar.Index;
import cat.ifae.cta.lidar.Null;
import cat.ifae.cta.lidar.StatusArray;
import picocli.CommandLine;

@CommandLine.Command(name = "drivers", mixinStandardHelpOptions = true)
class  Drivers implements Runnable {
    private DriversGrpc.DriversBlockingStub stub;

    @CommandLine.ParentCommand
    private Control parent;

    @CommandLine.Option(names = "-gs", description = "Get status")
    private boolean get_status;

    @Override
    public void run() {
        stub = DriversGrpc.newBlockingStub(parent.grpc.channel);
        stub = parent.grpc.addMetadata(stub);

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