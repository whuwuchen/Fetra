package whu.edu.totemdb.STCSim.Query;

import com.brein.time.timeintervals.intervals.LongInterval;
import whu.edu.totemdb.STCSim.Base.BaseTimeInterval;
import whu.edu.totemdb.STCSim.Base.Hop;
import whu.edu.totemdb.STCSim.Base.MappedTrajectory;
import whu.edu.totemdb.STCSim.Base.RawTrajectory;
import whu.edu.totemdb.STCSim.Index.GridIndex;
import whu.edu.totemdb.STCSim.Index.HopInvertedIndex;
import whu.edu.totemdb.STCSim.Utils.TrajUtil;

import java.util.*;
import java.util.stream.Collectors;

public class BaseEngine implements Engine {

    HopInvertedIndex poiInvertedIndex;
    GridIndex gridIndex;
    List<MappedTrajectory> mappedTrajectoryList;
    public void init(List<MappedTrajectory> mappedTrajectoryList,GridIndex gridIndex){
        if(null==this.gridIndex){
            this.gridIndex=gridIndex;
        }
        if(null==poiInvertedIndex){
            poiInvertedIndex = new HopInvertedIndex();
            poiInvertedIndex.initFromMappedTrajectories(mappedTrajectoryList);
        }
        this.mappedTrajectoryList=mappedTrajectoryList;
    }

/*    public Engine buildEngine(List<MappedTrajectory> mappedTrajectoryList){
        BaseEngine baseEngine = new BaseEngine();


        return baseEngine;
    }*/


    @Override
    public List<RawTrajectory> rangeQuery() {


        return null;
    }

    @Override
    public List<RawTrajectory> topKQuery(List<RawTrajectory> rawTrajectories,List<MappedTrajectory> queryTrajectories, int k) {
        // Candidate set of trajectory id
        Set<Integer> candidateTrajs = new HashSet<>();
        // the STCSim upper threshold of candidate trajectories
        HashMap<Integer,Double> upperBounds = new HashMap<>();

        for(MappedTrajectory mt:queryTrajectories){
            for(Hop hop: mt.getHops()){
                if(null==hop||null==hop.getPoi()){
                    continue;
                }
                List<BaseTimeInterval> res = poiInvertedIndex.invertedIndexs.get(hop.getPoi().getId()).overlap(new LongInterval(hop.getStarttime(),hop.getEndtime()));
                for(BaseTimeInterval bt:res){
                    String id = bt.getTrajId();
                    candidateTrajs.add(Integer.valueOf(id));
                    upperBounds.put(Integer.valueOf(id), TrajUtil.overlapInterval(hop.getStarttime(),hop.getEndtime(),bt.getStart(),bt.getEnd()) +upperBounds.getOrDefault(id,Double.valueOf(0)));
                }

            }
        }



        if(candidateTrajs.size()<=k){
            return candidateTrajs.parallelStream()
                    .map(s->rawTrajectories.get(s))
                    .collect(Collectors.toList());
        }

        List<Integer> res = upperBounds.entrySet().stream()
                .sorted(Comparator.comparingDouble(Map.Entry::getValue))
                .map(s->s.getKey()).collect(Collectors.toList());

        return res.subList(0,k).stream()
                .map(s->rawTrajectories.get(s))
                .collect(Collectors.toList());



    }


}
