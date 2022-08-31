package whu.edu.totemdb.STCSim.Base;

import com.brein.time.timeintervals.intervals.LongInterval;

public class BaseTimeInterval extends LongInterval {
    private String trajId;
    public BaseTimeInterval(String trajId,long ts,long te){
        super(ts,te);
        this.trajId=trajId;
    }

    public String getTrajId() {
        return trajId;
    }

}
