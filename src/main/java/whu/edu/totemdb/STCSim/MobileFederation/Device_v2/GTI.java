package whu.edu.totemdb.STCSim.MobileFederation.Device_v2;

import whu.edu.totemdb.STCSim.Base.Range;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GTI {
    // Grid temporal index
    private Map<Integer, TemporalIndex> gridTIMap;

    // 0 for temporal index; 1 for PTI
    private int temporalIndexType;
    // private Range r;

    public void addFootprint(Footprint ft){
        int gridId = ft.gridId;
        if(gridTIMap.containsKey(gridId)){
            gridTIMap.get(gridId).addRange(ft.deviceid,ft.startTimestamp,ft.endTimestamp);
        }
        else {
            TemporalIndex ti;
            if(temporalIndexType==0){
                ti = new IntervalTree();
            }
            else {
                ti = new PTI();
            }
            ti.addRange(ft.deviceid,ft.startTimestamp,ft.endTimestamp);
            gridTIMap.put(gridId,ti);

        }
    }

    public void addFootprintBatch(List<Footprint> ftlist){
        ftlist.parallelStream().forEach(ft->addFootprint(ft));
    }

    public Set<Integer> gridTemporalQuery(int gridId, int start, int end){
        Set<Integer> result = new HashSet<>();
        if(gridTIMap.containsKey(gridId)){
            return gridTIMap.get(gridId).rangeQuery(start, end);
        }
        else {
            return result;
        }

    }

    public GTI(int temporalIndexType){
        gridTIMap = new ConcurrentHashMap<>();
        this.temporalIndexType = temporalIndexType;
    }

    public Map<Integer, TemporalIndex> getGridTIMap() {
        return gridTIMap;
    }
}
