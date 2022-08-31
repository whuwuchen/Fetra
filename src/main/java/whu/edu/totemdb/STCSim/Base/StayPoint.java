package whu.edu.totemdb.STCSim.Base;

import java.util.Arrays;

public class StayPoint implements BasePoint {
    private long[] timeInterval;
    private double[] latlngs;
    private String id;
    public StayPoint(double lat, double lon, long startTime,long endTime) {
        latlngs = new double[2];
        timeInterval = new long[2];
        latlngs[0] = lat;
        latlngs[1] = lon;
        timeInterval[0] = startTime;
        timeInterval[1] = endTime;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public double getLat(){
        return latlngs[0];
    }

    public double getLon(){
        return latlngs[1];
    }

    public long getStartTime(){
        return timeInterval[0];
    }

    public long getEndTime(){
        return timeInterval[1];
    }

    @Override
    public String toString() {
        String r = id+","+Arrays.toString(timeInterval) +
                "," + Arrays.toString(latlngs);

        return r.replace("[","").replace("]","").replace(" ","");
    }
}
