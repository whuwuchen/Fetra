package whu.edu.totemdb.STCSim.MobileFederation.Device_v2;

import com.brein.time.timeintervals.collections.ListIntervalCollection;
import com.brein.time.timeintervals.indexes.IntervalTreeBuilder;
import com.brein.time.timeintervals.intervals.LongInterval;
import javafx.util.Pair;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class IntervalTree implements TemporalIndex{

    private com.brein.time.timeintervals.indexes.IntervalTree tree;


    public IntervalTree(){
        tree = IntervalTreeBuilder.newBuilder()
                .collectIntervals(interval -> new ListIntervalCollection())
                .usePredefinedType(IntervalTreeBuilder.IntervalType.LONG).build();
    }

    @Override
    public Set<Integer> rangeQuery(int start, int end) {
        LongInterval li = new LongInterval((long)start,(long)end);
        List<Integer> list = tree.overlap(li)
                .parallelStream()
                .map(s->((BaseTimeInterval)s).getDeviceid())
                .collect(Collectors.toList());
        return new HashSet<>(list);

    }

    @Override
    public void addRange(int deviceid, int start, int end) {
        BaseTimeInterval baseTimeInterval = new BaseTimeInterval(deviceid,start,end);
        synchronized(this) {
            tree.add(baseTimeInterval);
        }
    }

    @Override
    public void addBatch(List<Pair<Integer, Pair<Integer, Integer>>> ranges) {
        //

    }


}

class BaseTimeInterval extends LongInterval{
    private int deviceid;
    public BaseTimeInterval(int deviceid,long ts,long te){
        super(ts,te);
        this.deviceid = deviceid;
    }

    public int getDeviceid() {
        return deviceid;
    }
}
