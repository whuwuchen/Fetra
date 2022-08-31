package whu.edu.totemdb.STCSim.Base;

public class SpatialTemporalRange implements BaseRange {
    Range r;
    private long startTime;
    private long endTime;
    public SpatialTemporalRange(Range r,long startTime, long endTime){
        this.r=r;
        this.startTime=startTime;
        this.endTime=endTime;
    }

    @Override
    public double getlatMin() {
        return r.getlatMin();
    }

    @Override
    public double getlatMax() {
        return r.getlatMax();
    }

    @Override
    public double getlonMin() {
        return r.getlonMin();
    }

    @Override
    public double getlonMax() {
        return r.getlonMax();
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }
}
