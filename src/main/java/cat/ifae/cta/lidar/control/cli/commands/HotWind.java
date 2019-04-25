package cat.ifae.cta.lidar.control.cli.commands;

import cat.ifae.cta.lidar.HotwindGrpc;
import cat.ifae.cta.lidar.Null;
import cat.ifae.cta.lidar.control.cli.Licli;
import picocli.CommandLine;

@CommandLine.Command(name = "hotwind", mixinStandardHelpOptions = true)
public
class HotWind implements Runnable {
    private HotwindGrpc.HotwindBlockingStub stub;

    @CommandLine.Option(names = "-lock", description = "Lock")
    private boolean is_lock = false;

    @CommandLine.Option(names = "-unlock", description = "Unlock")
    private boolean is_unlock = false;

    // XXX: WTF
    @CommandLine.Option(names = "-error", description = "Error")
    private boolean error = false;

    @CommandLine.ParentCommand
    private Licli parent;

    @Override
    public void run() {
        stub = HotwindGrpc.newBlockingStub(parent.sm.getCh());
        stub = parent.sm.addMetadata(stub);

        CommandLine.populateCommand(this);

        try {
            if(is_lock) lock();
            else if(is_unlock) unlock();
        } catch(Exception e) {
            System.out.println(e.toString());
        }
    }


    private void lock() {
        Null req = Null.newBuilder().build();
        stub.lock(req);
    }

    private void unlock() {
        Null req = Null.newBuilder().build();
        stub.unlock(req);
    }

}