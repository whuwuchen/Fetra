package whu.edu.totemdb.STCSim.Index;

import whu.edu.totemdb.STCSim.Base.TimeInterval;

public class LocalTimeLineIndex {

    // 本地索引
    private int deviceId;  // 所属的设备

    private int poi_id;    // POI id
    // private TimeLineIndexItem[] items;

    private long[] startTimestamps;  // 起止时间数组

    private long[] endTimestamps;   //  结束时间数组


    // 利用给定POI的时间区间进行初始化
    public LocalTimeLineIndex(int deviceId, int poi_id, TimeInterval[] items){
        // 初始化
        this.deviceId = deviceId;
        this.poi_id = poi_id;

        int itemSize = items.length;
        startTimestamps = new long[itemSize];
        endTimestamps = new long[itemSize];
        for(int i=0; i<itemSize; i++){
            startTimestamps[i] = items[i].getStartTime();
            endTimestamps[i] = items[i].getEndTime();
        }

    }
    public double overlappedIntervalsLengthSum(long startTime, long endTime){
        double res = 0;
        int startIndex = binaryGESearch(startTime);
        int endIndex = binaryLESearch(endTime);
        for(int i=startIndex; i< endIndex;i++){
            // 累加时间区间
            res += endTimestamps[i] - startTimestamps[i];
        }
        return res;
    }



    //查找第一个大于等于给定值的元素
    public int binaryGESearch(long targettimestamp){
        int low = 0;
        int high = startTimestamps.length-1;
        int mid;
        int find = -1;
        while(low<=high){
            mid = low + ((high - low) >>> 1);
            if (startTimestamps[mid] >= targettimestamp) {
                if (mid == 0 || !(startTimestamps[mid - 1] >= targettimestamp)) {
                    find = mid;
                    break;
                } else {
                    high = mid - 1;
                }
            } else {
                low = mid + 1;
            }
        }
        return find;
    }

    // 查找最后一个小于给定值的元素
    public int binaryLESearch(long targettimestamp){
        int low = 0;
        int high = endTimestamps.length - 1;
        int mid;
        int find = -1;
        while (low <= high) {
            mid = low + ((high - low) >>> 1);
            if (endTimestamps[mid] <= targettimestamp) {
                if (mid == high || !(endTimestamps[mid + 1] <= targettimestamp)) {
                    find = mid;
                    break;
                } else {
                    low = mid + 1;
                }
            } else {
                high = mid - 1;
            }
        }
        return find;
    }

}
