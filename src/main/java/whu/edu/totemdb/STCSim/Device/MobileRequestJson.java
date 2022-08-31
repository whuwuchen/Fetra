package whu.edu.totemdb.STCSim.Device;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class MobileRequestJson {
    private String deviceId;

    /*
    The spatial range is represented as two bounded points 1 and 2,
    (116.6141113,40.9738354;116.6340993,41.02974394); temporal range is
    represented as timestamp pair (starttime,endtime)
     1#######
     ########
     ########
     #######2
     */

    private String ranges;

    private String temporals;

    public MobileRequestJson(String deviceId, String ranges, String temporals){
        this.deviceId = deviceId;
        this.ranges = ranges;
        this.temporals = temporals;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getRanges() {
        return ranges;
    }

    public String getTemporals() {
        return temporals;
    }

    public String toString(){
        Gson gson = new Gson();
        return gson.toJson(this);
    }

}
