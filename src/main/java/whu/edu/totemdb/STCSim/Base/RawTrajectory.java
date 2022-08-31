package whu.edu.totemdb.STCSim.Base;

import java.util.ArrayList;
import java.util.List;

public class RawTrajectory {
    private List<Point> gpslog;
    private int id;
    public RawTrajectory(){
        gpslog = new ArrayList<>();
    }

    public RawTrajectory(List<Point> gpslog){
        this.gpslog= gpslog;
    }

    public void addPoint(Point p){
        gpslog.add(p);
    }

    public List<Point> getGpslog() {
        return gpslog;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
