package whu.edu.totemdb.STCSim.Base;

public class TimeInterval {
    private long startTime;
    private long endTime;

    public TimeInterval(long startTime,long endTime){
        this.startTime = startTime;
        this.endTime = endTime;
    }



    public long getEndTime() {
        return endTime;
    }

    public long getStartTime() {
        return startTime;
    }

    @Override
    public String toString(){
        return startTime+","+endTime;
    }

    public String toStringWithOffset(long base){
        return (startTime - base) + "," + (endTime - base);
    }

}
