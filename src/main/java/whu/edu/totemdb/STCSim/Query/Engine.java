package whu.edu.totemdb.STCSim.Query;

import whu.edu.totemdb.STCSim.Base.MappedTrajectory;
import whu.edu.totemdb.STCSim.Base.RawTrajectory;

import java.util.List;

public interface Engine {
    public List<RawTrajectory> rangeQuery();
    public List<RawTrajectory> topKQuery(List<RawTrajectory> rawTrajectories,List<MappedTrajectory> queryTrajectories,int k);



}
