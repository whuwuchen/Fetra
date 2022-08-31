package whu.edu.totemdb.STCSim.Base;

import java.util.List;

public class Hop {
    private POI poi;
    private long starttime;
    private long endtime;
    private String id;
    private MappedTrajectory mappedTrajectory;
    public Hop(POI poi){
        this.poi=poi;
    }

    public Hop(String id, long starttime, long endtime){
        this.id = id;
        this.starttime=starttime;
        this.endtime=endtime;
    }


    public Hop(POI poi, long starttime, long endtime){
        this.poi=poi;
        this.starttime=starttime;
        this.endtime=endtime;
    }

    public String getId() {
        return id;
    }

    public POI getPoi() {
        return poi;
    }

    public long getStarttime() {
        return starttime;
    }

    public long getEndtime() {
        return endtime;
    }

    public void setEndtime(long endtime) {
        this.endtime = endtime;
    }

    public void setStarttime(long starttime) {
        this.starttime = starttime;
    }

    public boolean overlap(Hop o){
        if(this == o){
            return true;
        }

        if(null==o)
            return false;

        return this.poi.getId()==o.getPoi().getId();

        // return this.poi.equals(o);

    }

}
