package whu.edu.totemdb.STCSim.MobileFederation.Device_v2;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import whu.edu.totemdb.STCSim.Base.Hop;
import whu.edu.totemdb.STCSim.Base.MappedTrajectory;
import whu.edu.totemdb.STCSim.Base.Range;
import whu.edu.totemdb.STCSim.Base.StayPoint;
import whu.edu.totemdb.STCSim.Index.LocalTimeLineIndex;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MobileDevice_v2 {
    private static final Log mobileLogger = LogFactory.getLog(MobileDevice_v2.class);
    private int deviceId;

    private Range range;
    private List<MappedTrajectory> mappedTrajectories;

    private List<StayPoint> sps;

    private Map<Integer, IntervalTree> localIndex;

    private boolean hasLocalBoost;

    private boolean indexCompressed;

    public MobileDevice_v2(int deviceId, List<MappedTrajectory> mappedTrajectories, Range range, boolean hasLocalBoost, boolean indexCompressed){
        this.deviceId = deviceId;
        this.hasLocalBoost = hasLocalBoost;
        this.indexCompressed = indexCompressed;
        this.sps = new ArrayList<>();
        this.mappedTrajectories = mappedTrajectories;
        this.range = range;
    }

 /*   public void buildTimeLineIndexFromMappedTraj(){
        HashMap<Integer, List<Long>> poi2StartTimeList = new HashMap<>();
        HashMap<Integer, List<Long>> poi2EndTimeList = new HashMap<>();
        for(MappedTrajectory mappedTrajectory : mappedTrajectories){
            List<Hop> hops = mappedTrajectory.getHops();
            for(Hop hop:hops){
                int poi_id = hop.getPoi().getId();
                if(poi2StartTimeList.containsKey(poi_id)){
                    poi2StartTimeList.get(poi_id).add(hop.getStarttime());
                }
                else{
                    List<Long> startTimeList = new ArrayList<>();
                    startTimeList.add(hop.getStarttime());
                    poi2StartTimeList.put(poi_id,startTimeList);
                }
                if(poi2EndTimeList.containsKey(poi_id)){
                    poi2EndTimeList.get(poi_id).add(hop.getEndtime());
                }
                else{
                    List<Long> endTimeList = new ArrayList<>();
                    endTimeList.add(hop.getEndtime());
                    poi2EndTimeList.put(poi_id,endTimeList);
                }
            }

        }

        Set<Integer> poiSet = poi2StartTimeList.keySet();

        for(Integer i : poiSet){

        }



    }*/

    public void buildIndex(){
        localIndex = new ConcurrentHashMap<>();
        mappedTrajectories.parallelStream().forEach(t->buildIndex(t));
    }

    public void buildIndex(MappedTrajectory t){
        t.getHops().parallelStream().forEach(h->{
            int id = h.getPoi().getId();
            int st = (int) h.getStarttime();
            int end = (int) h.getEndtime();
            if(localIndex.containsKey(id)){
                localIndex.get(id).addRange(id,st, end);
            }
            else{
                IntervalTree intervalTree = new IntervalTree();
                intervalTree.addRange(id,st, end);
                localIndex.put(id,intervalTree);
            }

        });
    }
    public void sendLocalData(){

    }

}
