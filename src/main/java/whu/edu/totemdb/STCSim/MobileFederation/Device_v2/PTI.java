package whu.edu.totemdb.STCSim.MobileFederation.Device_v2;


import javafx.util.Pair;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PTI implements TemporalIndex{
    // period temporal index

    // initial timestamp of all intervals
    private static int startTimeOfDay;
    private static int dayLength;
    private static int maxLevel;
    // store key-value pair in terms of (day_id, pbti)
    private Map<Integer,PBTI> pbtiMap;

    // invoke only once
    public static void setDayLengthAndLevel(int dayLength, int maxLevel){
        PTI.dayLength = dayLength;
        PTI.maxLevel = maxLevel;
        PBTI.setDayLength(dayLength);
        PBTI.setLevel(maxLevel);
    }

    public Set<Integer> rangeQuery(int start, int end){
        Set<Integer> result = new HashSet<>();
        int dayOffset = (int) Math.floor((double)start/dayLength);
        if(pbtiMap.containsKey(dayOffset)){
            return pbtiMap.get(dayOffset).rangeQuery(start%dayLength,end%dayLength);
        }
        else{
            return result;
        }

    }

    public void addRange(int deviceid, int start, int end){
        int offset = (int) Math.floor((double)start/dayLength);
        if(pbtiMap.containsKey(offset)){
            pbtiMap.get(offset).addRange(deviceid,start%dayLength,end%dayLength);
        }
        else{
            PBTI pbti = new PBTI();
            pbti.addRange(deviceid,start%dayLength,end%dayLength);
            pbtiMap.put(offset,pbti);
        }
    }


    public void addBatch(List<Pair<Integer, Pair<Integer,Integer>>> ranges){
        ranges.parallelStream().forEach(r->addRange(r.getKey(),r.getValue().getKey(), r.getValue().getValue()));
    }

    public PTI(){
        pbtiMap = new ConcurrentHashMap<>();
    }

}
