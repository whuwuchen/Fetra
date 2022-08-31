package whu.edu.totemdb.STCSim.MobileFederation.Device_v2;
/*
    by wuchen, 2022.7.23
 */

import javafx.util.Pair;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PBTI {
    // Period Binary Temporal Index

    // maximum level for daily temporal partition
    private static int maxLevel;

    // time length of a day in seconds
    private static int dayLength;

    // store device ids in each temporal cells, key is the cell code
    private Map<String, Set<Integer>> deviceInTemporalCell;

    // find device ids in the target range, start and end are the numbers of cell in the lowest level
    public Set<Integer> normalizedRangeQuery(int start, int end){
        Set<Integer> result = new HashSet<>();
        int l = maxLevel;

        for(int j=maxLevel;j>=0;j--){
            int startPrefix = (start)>>(maxLevel-j);
            int endPrefix = (end)>>(maxLevel-j);
            for(int k=startPrefix;k<=endPrefix;k++){
                String cid = ""+j+","+k;
                if(deviceInTemporalCell.containsKey(cid)){
                    result.addAll(deviceInTemporalCell.get(cid));
                }
            }
        }

        return result;
    }

    public Set<Integer> rangeQuery(int start, int end){
        double temp1 = Math.min((double)start/dayLength, 1.0);
        temp1 = temp1*((1<<maxLevel)-1);
        int a = (int) Math.floor(temp1);
        double temp2 = Math.min((double)end/dayLength, 1.0);
        temp2 = temp2*((1<<maxLevel)-1);
        int b = (int) Math.ceil(temp2);
        return normalizedRangeQuery(a,b);

    }


    public void addRange(int deviceid, int start, int end){
        double temp1 = Math.min((double)start/dayLength, 1.0);
        temp1 = temp1*((1<<maxLevel)-1);
        int a = (int) Math.floor(temp1);
        double temp2 = Math.min((double)end/dayLength, 1.0);
        temp2 = temp2*((1<<maxLevel)-1);
        int b = (int) Math.ceil(temp2);
        //int a = (int) Math.floor(((double)start/dayLength)*(1<<maxLevel-1));
        //int b = (int) Math.ceil(((double)end/dayLength)*(1<<maxLevel-1));
        //addTemporalRange(new Pair<>(deviceid, new Pair<>(a,b)));
        int l = maxLevel;

        while(l>0&&a<=b){
            if(a%2==1){
                String cid = ""+l+","+a;
                if(deviceInTemporalCell.containsKey(cid)){
                    deviceInTemporalCell.get(cid).add(deviceid);
                }
                else{
                    Set<Integer> s = new HashSet<>();
                    s.add(deviceid);
                    deviceInTemporalCell.put(cid, s);
                }
                a = a+1;
            }
            if(b%2==0){
                String cid = ""+l+","+b;
                if(deviceInTemporalCell.containsKey(cid)){
                    deviceInTemporalCell.get(cid).add(deviceid);
                }
                else{
                    Set<Integer> s = new HashSet<>();
                    s.add(deviceid);
                    deviceInTemporalCell.put(cid, s);
                }
                b = b-1;
            }
            a = a/2;
            b = b/2;
            l = l-1;
        }



    }

    // add temporal interval (deviceid,(start, end)) into PBTI, update related cells in deviceInTemporalCell
    public void addTemporalRange(Pair<Integer, Pair<Integer,Integer>> interval){
        int deviceid = interval.getKey();
        int a = interval.getValue().getKey();
        int b = interval.getValue().getValue();
        int l = maxLevel;

        while(l>0&&a<=b){
            if(a%2==1){
                String cid = ""+l+","+a;
                if(deviceInTemporalCell.containsKey(cid)){
                    deviceInTemporalCell.get(cid).add(deviceid);
                }
                else{
                    Set<Integer> s = new HashSet<>();
                    s.add(deviceid);
                    deviceInTemporalCell.put(cid, s);
                }
                a = a+1;
            }
            if(b%2==0){
                String cid = ""+l+","+b;
                if(deviceInTemporalCell.containsKey(cid)){
                    deviceInTemporalCell.get(cid).add(deviceid);
                }
                else{
                    Set<Integer> s = new HashSet<>();
                    s.add(deviceid);
                    deviceInTemporalCell.put(cid, s);
                }
                b = b-1;
            }
            a = a/2;
            b = b/2;
            l = l-1;
        }

    }

    // add temporal range in batch
    public void addTemporalRangeBatch(List<Pair<Integer, Pair<Integer, Integer>>> ranges){

        ranges.parallelStream().forEach(p->addRange(p.getKey(),p.getValue().getKey(),p.getValue().getValue()));

    }


    public static void setLevel(int level){
        maxLevel = level;
    }

    public static void setDayLength(int length){
        dayLength = length;
    }

    public PBTI(){
        // ensure thread safety
        deviceInTemporalCell = new ConcurrentHashMap<>();

    }

}
