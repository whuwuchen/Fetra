package whu.edu.totemdb.STCSim.Index;

import whu.edu.totemdb.STCSim.Base.BaseTimeInterval;
import whu.edu.totemdb.STCSim.Base.Hop;
import whu.edu.totemdb.STCSim.Base.MappedTrajectory;
import whu.edu.totemdb.STCSim.Base.StayPoint;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HopInvertedIndex {
    //Pair of <POIId,IntervalTree>
    public Map<Integer, TimeIntervalTree> invertedIndexs;
    public HopInvertedIndex(){
        invertedIndexs = new HashMap<>();
    }

    public void initFromMappedTrajectories(List<MappedTrajectory> mappedTrajectoryList){
        for(MappedTrajectory mappedTrajectory:mappedTrajectoryList){
            for(Hop hop : mappedTrajectory.getHops()){
                if(null==hop||null==hop.getPoi())
                    continue;
                int POIId = hop.getPoi().getId();
                TimeIntervalTree tree = invertedIndexs.getOrDefault(POIId,new TimeIntervalTree());
                tree.addInterval(new BaseTimeInterval(String.valueOf(mappedTrajectory.getMappedTrajId()),hop.getStarttime(),hop.getEndtime()));
                invertedIndexs.put(POIId,tree);
            }
        }
    }

    public void initFromStayPoints(List<StayPoint> stayPoints,List<Integer> gridIds){
        assert stayPoints.size()== gridIds.size();
        for(int i=0;i<stayPoints.size();i++){
            int id = Integer.parseInt(stayPoints.get(i).getId());
            TimeIntervalTree tree = invertedIndexs.getOrDefault(id, new TimeIntervalTree());
            StayPoint s = stayPoints.get(i);
            tree.addInterval(new BaseTimeInterval(s.getId(),s.getStartTime(),s.getEndTime()));
            invertedIndexs.put(id,tree);
        }
    }

    public void initFromMappedTrajectory(List<MappedTrajectory> mappedTrajectories){
        for(MappedTrajectory mt : mappedTrajectories){
            for(Hop h:mt.getHops()){
                int id = h.getPoi().getId();
                TimeIntervalTree tree = invertedIndexs.getOrDefault(id, new TimeIntervalTree());
                tree.addInterval(new BaseTimeInterval(String.valueOf(mt.getRawTraj().getId()),h.getStarttime(),h.getEndtime()));
                invertedIndexs.put(id,tree);
            }
        }

    }




}
