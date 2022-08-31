package whu.edu.totemdb.STCSim.Index;

import com.brein.time.timeintervals.collections.ListIntervalCollection;
import com.brein.time.timeintervals.indexes.IntervalTree;
import com.brein.time.timeintervals.indexes.IntervalTreeBuilder;
import com.brein.time.timeintervals.intervals.IInterval;
import com.brein.time.timeintervals.intervals.LongInterval;
import whu.edu.totemdb.STCSim.Base.BaseTimeInterval;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class TimeIntervalTree {
    private IntervalTree tree;
    private String deviceId;
    public TimeIntervalTree(){
        tree = IntervalTreeBuilder.newBuilder()
                .collectIntervals(interval -> new ListIntervalCollection())
                .usePredefinedType(IntervalTreeBuilder.IntervalType.LONG).build();

    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public List<BaseTimeInterval> overlap(LongInterval i){
        return tree.overlap(i)
                .parallelStream()
                .map(s->(BaseTimeInterval)s)
                .collect(Collectors.toList());
    }

    public IntervalTree getTree() {
        return tree;
    }

    public List<BaseTimeInterval> getAllIntervals(){
        List<BaseTimeInterval> result = new ArrayList<>();
        Iterator<IInterval> it = tree.iterator();
        while(it.hasNext()){
            result.add((BaseTimeInterval) it.next());
        }
        return result;
    }

    public void addInterval(LongInterval l){
        tree.add(l);
    }


}
