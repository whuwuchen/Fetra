package whu.edu.totemdb.STCSim.MobileFederation.Device_v2;

import javafx.util.Pair;

import java.util.List;
import java.util.Set;

public interface TemporalIndex {
    public Set<Integer> rangeQuery(int start, int end);
    public void addRange(int deviceid, int start, int end);
    public void addBatch(List<Pair<Integer, Pair<Integer,Integer>>> ranges);
}
