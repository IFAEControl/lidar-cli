package cat.ifae.cta.lidar.control.cli.commands.llc_commands;

import cat.ifae.cta.lidar.LLCHotwindGrpc;
import cat.ifae.cta.lidar.Null;
import cat.ifae.cta.lidar.control.cli.Licli;
import cat.ifae.cta.lidar.logging.Logging;
import io.grpc.StatusRuntimeException;
import picocli.CommandLine;

@CommandLine.Command(name = "hotwind", mixinStandardHelpOptions = true)
public
class LLCHotWind implements Runnable {
    private final static Logging _log = new Logging(LLCHotWind.class);

    private LLCHotwindGrpc.LLCHotwindBlockingStub stub;

    @CommandLine.Option(names = "--lock", description = "Lock")
    private boolean is_lock = false;

    @CommandLine.Option(names = "--unlock", description = "Unlock")
    private boolean is_unlock = false;

    // XXX: WTF
    @CommandLine.Option(names = "--error", description = "Error")
    private boolean error = false;

    @Override
    public void run() {
        stub = LLCHotwindGrpc.newBlockingStub(Licli.sm.getCh());
        stub = Licli.sm.addMetadata(stub);

        CommandLine.populateCommand(this);

        try {
            if(is_lock) lock();
            else if(is_unlock) unlock();
        } catch (StatusRuntimeException e) {
            _log.error(e.getStatus().getCause().getLocalizedMessage());
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