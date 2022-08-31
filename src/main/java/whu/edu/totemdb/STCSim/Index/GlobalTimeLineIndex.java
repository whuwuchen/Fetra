package whu.edu.totemdb.STCSim.Index;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GlobalTimeLineIndex {
    // 记录全局网格的TimeLineIndex, 先对TimeLine上的时间线进行排序

    // 按天对时间进行划分
    private long baseTime;

    //<Grid_id,<Day_id,TimeLine>>
    private Map<Integer, Map<Integer,List<TimeLineIndexItem>>> startTimeLine;
    private Map<Integer, Map<Integer,List<TimeLineIndexItem>>> endTimeLine;


    // 查找给定区间内的候选设备
    // 两次二分查找求交集
    public Set<Integer> getCandidate(long startTimestamp, long endTimestamp){
        int dayNumber = (int) ((startTimestamp-baseTime)/86400);
        Set<Integer> res = new HashSet<>();


        return res;
    }

    public Set<Integer> lessthanEndTimestamp(long targetTimestamp){
        Set<Integer> res = new HashSet<>();


        return res;
    }


    public Set<Integer> greaterthanStartTimestamp(long targetTimestamp){
        Set<Integer> res = new HashSet<>();


        return res;
    }


}
