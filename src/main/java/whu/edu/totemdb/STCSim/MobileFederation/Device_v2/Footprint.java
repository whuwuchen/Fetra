package whu.edu.totemdb.STCSim.MobileFederation.Device_v2;

public class Footprint {
    public int deviceid;
    public int gridId;
    public int startTimestamp;
    public int endTimestamp;
    public Footprint(int deviceid, int gridId, int startTimestamp, int endTimestamp){
        this.deviceid = deviceid;
        this.gridId = gridId;
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
    }
}
