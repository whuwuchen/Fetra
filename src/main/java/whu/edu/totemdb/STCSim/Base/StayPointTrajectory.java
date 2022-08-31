package whu.edu.totemdb.STCSim.Base;

import java.util.List;

public class StayPointTrajectory {
    private int id;
    private List<StayPoint> stayPointList;
    public StayPointTrajectory(int id, List<StayPoint> stayPointList){
        this.id = id;
        this.stayPointList = stayPointList;
    }

    public List<StayPoint> getStayPointList() {
        return stayPointList;
    }

    public int getId() {
        return id;
    }
}
