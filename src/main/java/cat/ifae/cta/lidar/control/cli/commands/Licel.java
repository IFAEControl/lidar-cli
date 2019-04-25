package cat.ifae.cta.lidar.control.cli.commands;

import cat.ifae.cta.lidar.*;
import cat.ifae.cta.lidar.Helpers;
import cat.ifae.cta.lidar.control.cli.Licli;
import picocli.CommandLine;

@CommandLine.Command(name = "licel", mixinStandardHelpOptions = true)
public
class Licel implements Runnable {
    private LicelGrpc.LicelBlockingStub stub;

    @CommandLine.ParentCommand
    private Licli parent;

    // XXX: Insecure
    @CommandLine.Option(names = "-set-net", description = "Set network configuration. format=host:mask:port:gateway:passwd")
    private String _net_config;

    @CommandLine.Option(names = "-activate-dhcp", description = "Activate dhcp. format=port:passwd")
    private String _dhcp_settings;

    @CommandLine.Option(names = "-get-id", description = "Get id")
    private boolean _is_get_id;

    @CommandLine.Option(names = "-get-caps", description = "Is get capabilities")
    private boolean _is_get_caps;

    @CommandLine.Option(names = "-wait-ready", description = "Wait for ready")
    private int _wait_delay = -1;

    @CommandLine.Option(names = "-clear-memory", description = "clear memory")
    private boolean _is_clear;

    @CommandLine.Option(names = "-get-data-set", description = "Get data set. format=device:data_set:number_to_read:memory")
    private String _data_set;

    @CommandLine.Option(names = "-get-status", description = "Get status. format=number:memory:acq_state:recording")
    private boolean _is_get_status;

    @CommandLine.Option(names = "-multiple-clear-memory", description = "Multiple clear memory")
    private boolean _is_multiple_clean;

    @CommandLine.Option(names = "-multiple-wait-ready", description = "Multiple wait for ready")
    private int _multiple_wait_delay = -1;

    @CommandLine.Option(names = "-select-tr", description = "Select TR")
    private int _tr_number = -1;

    // TODO: @Option(names = "-x", split = ",") Picocli supports automating split
    @CommandLine.Option(names = "-select-tr-list", description = "Select a list of TR. format=list_length:TR-1:TR-2:TR-N")
    private String _tr_list;

    @CommandLine.Option(names = "-set-discrimination-level", description = "Set discrimination level")
    private int _discrimination_level = -1;

    @CommandLine.Option(names = "-input-range", description = "Set input ranges")
    private int _input_range = -1;

    @CommandLine.Option(names = "-slave-mode", description = "Set slave mode")
    private boolean _is_slave_mode;

    @CommandLine.Option(names = "-push-mode", description = "Set push mode. format=shots:dataset:number:memory")
    private String _push_mode_info;

    @CommandLine.Option(names = "-threshold-mode", description = "Set threshold mode")
    private int _threshold = -1;

    @CommandLine.Option(names = "-single-shot", description = "Single shot")
    private boolean _single_shot;

    @CommandLine.Option(names = "-start-acq", description = "Start acquisition")
    private boolean _acq_start;

    @CommandLine.Option(names = "-stop-acq", description = "Stop acquisition")
    private boolean _acq_stop;

    @CommandLine.Option(names = "-continue-acq", description = "Continue acquisition")
    private boolean _acq_continue;

    @CommandLine.Option(names = "-multi-continue-acq", description = "Multiple continue acquisition")
    private boolean _acq_continue_mul;

    @CommandLine.Option(names = "-multi-start-acq", description = "Multiple start acquisition")
    private boolean _acq_start_mul;

    @CommandLine.Option(names = "-multi-stop-acq", description = "Multiple stop acquisition")
    private boolean _acq_stop_mul;

    @CommandLine.Option(names = "-inc-shots", description = "Increase shots")
    private int _inc_shots = -1;

    @CommandLine.Option(names = "-shot-limit", description = "Set shots limit")
    private int _shot_limit = -1;

    @CommandLine.Option(names = "-pmt-get-status", description = "Get pmt status")
    private int _pmt_get_status = -1;

    @CommandLine.Option(names = "-pmt-set-gain", description = "Set pmt gain. format=pmt:gain")
    private String _pmt_info;

    @CommandLine.Option(names = "-set-trigger-mode",
            description = "Set trigger mode. format=id:laser_on:pretrigger_on:q_switch_on:gating_on:is_master_trigger")
    private String _trigger_mode;

    @CommandLine.Option(names = "-set-trigger-timing",
            description = "Set trigger timing. format=id:rate:pretrigger:pretr_length:q_switch:length")
    private String _trigger_timing;

    @Override
    public void run() {
        stub = LicelGrpc.newBlockingStub(parent.sm.getCh());
        stub = parent.sm.addMetadata(stub);

        CommandLine.populateCommand(this);

        try {
            if(_net_config != null) setNetConfig(_net_config);
            else if(_dhcp_settings != null) setDHCPConfig(_dhcp_settings);
            else if(_is_get_id) getID();
            else if(_is_get_caps) getCaps();
            else if(_wait_delay != -1) waitForReady(_wait_delay);
            else if(_is_clear) clearMemory();
            else if(_data_set != null) getDataSet(_data_set);
            else if(_is_get_status) getStatus();
            else if(_is_multiple_clean) multipleClearMemory();
            else if(_multiple_wait_delay != -1) multipleWaitForReady(_multiple_wait_delay);
            else if(_tr_number != -1) selectTR(_tr_number);
            else if(_tr_list != null) selectMultipleTR(_tr_list);
            else if(_discrimination_level != -1) setDiscriminationLevel(_discrimination_level);
            else if(_input_range != -1) setInputRange(_input_range);
            else if(_is_slave_mode) setSlaveMode();
            else if(_push_mode_info != null) setPushMode(_push_mode_info);
            else if(_threshold != -1) setThresholdMode(_threshold);
            else if(_single_shot) singleShot();
            else if(_acq_start) startAcquisition();
            else if(_acq_stop) stopAcquisition();
            else if(_acq_continue) continueAcquisition();
            else if(_acq_continue_mul) multipleContinueAcquisition();
            else if(_acq_start_mul) multipleStartAcquisition();
            else if(_acq_stop_mul) multipleStopAcquisition();
            else if(_inc_shots != -1) increaseShots(_inc_shots);
            else if(_shot_limit != -1) setShotLimit(_shot_limit);
            else if(_pmt_get_status != -1) pmtGetStatus(_pmt_get_status);
            else if(_pmt_info != null) pmtSetGain(_pmt_info);
            else if(_trigger_mode != null) setTriggerMode(_trigger_mode);
            else if(_trigger_timing != null) setTriggerTiming(_trigger_timing);
        } catch (Exception e) {
            //System.out.println(e.toString());
            e.printStackTrace(System.out);
        }
    }

    private void setNetConfig(String c) {
        String[] components = Helpers.split(c, 5);

        String host = components[0];
        String mask = components[1];
        int port = Integer.parseInt(components[2]);
        String gateway = components[3];
        String passwd = components[4];

        NetConfig config = NetConfig.newBuilder().setHost(host).setMask(mask).setPort(port).setGateway(gateway)
                .setPasswd(passwd).build();

        LicelAnswer resp = stub.setIPParameter(config);
        System.out.println(resp);
    }

    private void setDHCPConfig(String c) {
        String[] components = Helpers.split(c, 2);

        int port = Integer.parseInt(components[0]);
        String passwd = components[1];

        DHCPConfig req = DHCPConfig.newBuilder().setPort(port).setPasswd(passwd).build();
        LicelAnswer resp = stub.activateDHCP(req);
        System.out.println(resp);
    }

    private void getID() {
        Null req = Null.newBuilder().build();
        LicelAnswer resp = stub.getID(req);
        System.out.println(resp);
    }

    private void getCaps() {
        Null req = Null.newBuilder().build();
        LicelAnswer resp = stub.getCapabilities(req);
        System.out.println(resp);
    }

    private void waitForReady(int delay) {
        if(delay < 0)
            throw new RuntimeException("Invalid value");

        Delay d = Delay.newBuilder().setDelay(delay).build();
        LicelAnswer resp = stub.waitForReady(d);
        System.out.println(resp);
    }

    private void clearMemory() {
        Null req = Null.newBuilder().build();
        stub.clearMemory(req);
    }

    private void getDataSet(String d) {
        String[] components = Helpers.split(d, 4);

        int device = Integer.parseInt(components[0]);
        int data_set = Integer.parseInt(components[1]);
        int number = Integer.parseInt(components[2]);
        int memory = Integer.parseInt(components[3]);

        DataSet req = DataSet.newBuilder().setDevice(device).setDataSet(data_set).setNumberToRead(number)
                .setMemory(memory).build();
        LicelAnswer resp = stub.getDatasets(req);
        System.out.println(resp);

    }

    private void getStatus() {
        Null req = Null.newBuilder().build();
        LicelAnswer resp = stub.getStatus(req);
        System.out.println(resp);
    }

    private void multipleClearMemory() {
        Null req = Null.newBuilder().build();
        LicelAnswer resp = stub.multipleClearMemory(req);
        System.out.println(resp);
    }

    private void multipleWaitForReady(int delay) {
        if(delay < 0)
            throw new RuntimeException("Invalid value");

        Delay req = Delay.newBuilder().setDelay(delay).build();
        LicelAnswer resp = stub.multipleWaitForReady(req);
        System.out.println(resp);
    }

    private void selectTR(int n) {
        if(n < 0)
            throw new RuntimeException("Invalid value");

        TRNumber req = TRNumber.newBuilder().setTr(n).build();
        LicelAnswer resp = stub.selectTR(req);
        System.out.println(resp);
    }

    private void selectMultipleTR(String s) {
        String[] c = s.split(":");

        int number = Integer.parseInt(c[0]);
        if(number < 0)
            throw new RuntimeException("Invalid value");

        TRList.Builder tmp = TRList.newBuilder();
        for(int i = 0; i < number; i++)
            tmp.setList(i+1, Integer.parseInt(c[i+1]));

        TRList req = tmp.build();
        LicelAnswer resp = stub.selectMultipleTR(req);
        System.out.println(resp);
    }

    private void setDiscriminationLevel(int discrimination_level) {
        if(discrimination_level < 0)
            throw new RuntimeException("Invalid value");

        Level req = Level.newBuilder().setDiscriminationLevel(discrimination_level).build();
        LicelAnswer resp = stub.setDiscriminatorLevel(req);
        System.out.println(resp);
    }

    private void setInputRange(int input_range) {
        if(input_range < 0)
            throw new RuntimeException("Invalid value");

        Range req = Range.newBuilder().setInputRange(input_range).build();
        LicelAnswer resp = stub.setInputRange(req);
        System.out.println(resp);
    }

    private void setSlaveMode() {
        Null req = Null.newBuilder().build();
        LicelAnswer resp = stub.setSlaveMode(req);
        System.out.println(resp);
    }

    private void setPushMode(String s) {
        String[] c = Helpers.split(s, 4);

        int shots = Integer.parseInt(c[0]);
        int dataset = Integer.parseInt(c[1]);
        int number = Integer.parseInt(c[2]);
        int memory = Integer.parseInt(c[3]);

        PushInfo req = PushInfo.newBuilder().setShots(shots).setDataset(dataset).setNumberToRead(number)
                .setMemory(memory).build();

        LicelAnswer resp = stub.setPushMode(req);
        System.out.println(resp);
    }

    private void setThresholdMode(int threshold) {
        if(threshold < 0)
            throw new RuntimeException("Invalid vlaue");

        ThresholdMode req = ThresholdMode.newBuilder().setMode(threshold).build();
        LicelAnswer resp = stub.setThresholdMode(req);
        System.out.println(resp);
    }

    private void singleShot() {
        Null req = Null.newBuilder().build();
        LicelAnswer resp = stub.singleShot(req);
        System.out.println(resp);
    }

    private void startAcquisition() {
        Null req = Null.newBuilder().build();
        LicelAnswer resp = stub.startAcquisition(req);
        System.out.println(resp);
    }

    private void stopAcquisition() {
        Null req = Null.newBuilder().build();
        LicelAnswer resp = stub.stopAcquisition(req);
        System.out.println(resp);
    }

    private void continueAcquisition() {
        Null req = Null.newBuilder().build();
        LicelAnswer resp = stub.continueAcquisition(req);
        System.out.println(resp);
    }

    private void multipleContinueAcquisition() {
        Null req = Null.newBuilder().build();
        LicelAnswer resp = stub.multipleContinueAcquisition(req);
        System.out.println(resp);
    }

    private void multipleStartAcquisition() {
        Null req = Null.newBuilder().build();
        LicelAnswer resp = stub.multipleStartAcquisition(req);
        System.out.println(resp);
    }

    private void multipleStopAcquisition() {
        Null req = Null.newBuilder().build();
        LicelAnswer resp = stub.multipleStopAcquisition(req);
        System.out.println(resp);
    }

    private void increaseShots(int shots) {
        if(shots < 0)
            throw new RuntimeException("Invalid value");

        Shots n = Shots.newBuilder().setShots(shots).build();
        LicelAnswer resp = stub.increaseShots(n);
        System.out.println(resp);
    }

    private void setShotLimit(int new_limit) {
        Mode m = Mode.newBuilder().setMode(new_limit).build();
        LicelAnswer resp = stub.setShotLimit(m);
        System.out.println(resp);

    }

    private void pmtGetStatus(int pmt) {
        if(pmt < 0)
            throw new RuntimeException("Invalid val");

        PMTInfo i = PMTInfo.newBuilder().setPmt(pmt).build();
        LicelAnswer resp = stub.pmtGetStatus(i);
        System.out.println(resp);
    }

    private void pmtSetGain(String info) {
        String[] components = Helpers.split(info, 2);

        int pmt = Integer.parseInt(components[0]);
        int hv = Integer.parseInt(components[1]);

        PMTInfo i = PMTInfo.newBuilder().setPmt(pmt).setHv(hv).build();
        LicelAnswer resp = stub.pmtSetGain(i);
        System.out.println(resp);
    }

    private void setTriggerMode(String info) {
        String[] comp = Helpers.split(info, 6);

        int id = Integer.parseInt(comp[0]);
        boolean laser = Boolean.parseBoolean(comp[1]);
        boolean pretrigger = Boolean.parseBoolean(comp[2]);
        boolean q_switch = Boolean.parseBoolean(comp[3]);
        boolean gating = Boolean.parseBoolean(comp[4]);
        boolean master_trigger = Boolean.parseBoolean(comp[5]);

        TriggerMode m = TriggerMode.newBuilder().setBoardId(id).setLaserActive(laser).setPretriggerActive(pretrigger)
                .setQSwitchActive(q_switch).setGatingActive(gating).setMasterTrigger(master_trigger)
                .build();
        LicelAnswer resp = stub.setTriggerMode(m);
        System.out.println(resp);
    }

    private void setTriggerTiming(String info) {
        String[] comp = Helpers.split(info, 6);

        int id = Integer.parseInt(comp[0]);
        long rate = Long.parseLong(comp[1]);
        long pretr = Long.parseLong(comp[2]);
        long pretr_length = Long.parseLong(comp[3]);
        long qs = Long.parseLong(comp[4]);
        long qs_length = Long.parseLong(comp[5]);

        TriggerTiming t = TriggerTiming.newBuilder().setBoardId(id).setRepetitionRate(rate).setPretrigger(pretr)
                .setPretriggerLength(pretr_length).setQSwitch(qs).setQSwitchLength(qs_length)
                .build();
        LicelAnswer resp = stub.setTriggerTiming(t);
        System.out.println(resp);
    }
}