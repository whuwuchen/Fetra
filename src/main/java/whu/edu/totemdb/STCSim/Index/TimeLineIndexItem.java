package whu.edu.totemdb.STCSim.Index;
public class TimeLineIndexItem {
    private int deviceId;
    private int type; // 0 for start time, 1 for end time
    public long timestamp; // 时间点

    public TimeLineIndexItem(int deviceId, int type, long timestamp){
        this.deviceId = deviceId;
        this.type = type;
        this.timestamp = timestamp;

    }

}
