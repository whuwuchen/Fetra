package whu.edu.totemdb.STCSim.TestCase;

import com.brein.time.timeintervals.indexes.IntervalTree;
import com.brein.time.timeintervals.intervals.LongInterval;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import whu.edu.totemdb.STCSim.Base.BaseTimeInterval;
import whu.edu.totemdb.STCSim.Base.Point;
import whu.edu.totemdb.STCSim.Base.RawTrajectory;
import whu.edu.totemdb.STCSim.Index.TimeIntervalTree;
import whu.edu.totemdb.STCSim.Utils.DataLoader;
import whu.edu.totemdb.STCSim.Utils.TrajUtil;

import java.util.*;

public class STLCSSandSTLCTest {

    public static List<RawTrajectory> rawTrajectories = DataLoader.loadRawTrajDataBeiJing();

    public static double distanceThreshold = 50;
    public static long temporalThreshold = 20;

    public static Log logger = LogFactory.getLog(STLCSSandSTLCTest.class);


    // 利用网格索引对STLCSS及STLC进行剪枝

    //针对STLCSS以及STLC进行实验

    // 观察|S|的变化

    // 观察Q的变化

    // 观察D_i的变化

    // 观察k的变化

    // 观察网格大小的变化

    public static void main(String[] args){
        testSTLCSSAndSTLC();
        // System.out.println("test");
    }

    public static void testSTLCSSAndSTLC(){
        int[] deviceNumbers = {200, 400, 600, 800, 1000};
        int[] TrajPerDevices = {100, 200, 300, 400, 500};
        double[] gridWidths = {0.001, 0.003, 0.005, 0.007, 0.009};
        double[] ratioOfQ = {0.001, 0.002, 0.003, 0.004, 0.005};
        int[] parameterK = {10, 20, 30, 40, 50};

        // 设备数量的变化 |S|
        for(int i=0;i< deviceNumbers.length;i++){
            //List<Double> STLCSS_times = new ArrayList<>();
            int deviceNum = deviceNumbers[i];
            int trajNum = 300;
            int querySize = (int) (0.001*rawTrajectories.size());
            HashMap<Integer,List<RawTrajectory>>  devices = sampleTrajs(rawTrajectories,trajNum, deviceNum);
            List<RawTrajectory> queryTrajs = generateQuery(rawTrajectories, querySize);
            Map<Integer, TimeIntervalTree> indexes = buildIndex(devices);
            // filter用时
            long beforeFilter = System.currentTimeMillis();
            List<Integer> candidates = filter(queryTrajs, indexes);
            long afterFilter = System.currentTimeMillis();

            // 本地计算用时, 0 for stlcss, 1 for stlc
            long beforeSTLCSSRefinement = System.currentTimeMillis();
            candidates.parallelStream().forEach(d->{
                List<RawTrajectory> localRes = devices.get(d);
                calculate(queryTrajs,localRes,0);
            });
            /*for(Integer d : candidates){
                List<RawTrajectory> localRes = devices.get(i);
                calculate(queryTrajs,localRes,0);
            }*/
            long endSTLCSSRefinement = System.currentTimeMillis();

            long beforeSTLCRefinement = System.currentTimeMillis();
            candidates.parallelStream().forEach(d->{
                List<RawTrajectory> localRes = devices.get(d);
                calculate(queryTrajs,localRes,1);
            });
            /*for(Integer d : candidates){
                List<RawTrajectory> localRes = devices.get(i);
                calculate(queryTrajs,localRes,0);
            }*/

            long endSTLCRefinement = System.currentTimeMillis();

            double stlcssTime = (double) (afterFilter - beforeFilter + endSTLCSSRefinement - beforeSTLCSSRefinement)/1000;
            double stlcTime = (double)(afterFilter - beforeFilter + endSTLCRefinement - beforeSTLCRefinement)/1000;

            logger.info(String.format("Federation size %d, total time for STLCSS : %3f , total time for STLC : %3f, ",deviceNum,stlcssTime,stlcTime));

        }

        // 设备内轨迹数量的变化  |D|
        for(int i=0;i< TrajPerDevices.length;i++){
            //List<Double> STLCSS_times = new ArrayList<>();
            int deviceNum = 600;
            int trajNum = TrajPerDevices[i];
            int querySize = (int) (0.001*rawTrajectories.size());
            HashMap<Integer,List<RawTrajectory>>  devices = sampleTrajs(rawTrajectories,trajNum, deviceNum);
            List<RawTrajectory> queryTrajs = generateQuery(rawTrajectories, querySize);
            Map<Integer, TimeIntervalTree> indexes = buildIndex(devices);
            // filter用时
            long beforeFilter = System.currentTimeMillis();
            List<Integer> candidates = filter(queryTrajs, indexes);
            long afterFilter = System.currentTimeMillis();

            // 本地计算用时, 0 for stlcss, 1 for stlc
            long beforeSTLCSSRefinement = System.currentTimeMillis();
            candidates.parallelStream().forEach(d->{
                List<RawTrajectory> localRes = devices.get(d);
                calculate(queryTrajs,localRes,0);
            });
            /*for(Integer d : candidates){
                List<RawTrajectory> localRes = devices.get(i);
                calculate(queryTrajs,localRes,0);
            }*/
            long endSTLCSSRefinement = System.currentTimeMillis();

            long beforeSTLCRefinement = System.currentTimeMillis();
            candidates.parallelStream().forEach(d->{
                List<RawTrajectory> localRes = devices.get(d);
                calculate(queryTrajs,localRes,1);
            });
            /*for(Integer d : candidates){
                List<RawTrajectory> localRes = devices.get(i);
                calculate(queryTrajs,localRes,0);
            }*/

            long endSTLCRefinement = System.currentTimeMillis();

            double stlcssTime = (double) (afterFilter - beforeFilter + endSTLCSSRefinement - beforeSTLCSSRefinement)/1000;
            double stlcTime = (double)(afterFilter - beforeFilter + endSTLCRefinement - beforeSTLCRefinement)/1000;

            logger.info(String.format("Trajectory in device %d, total time for STLCSS : %3f , total time for STLC : %3f, ",trajNum,stlcssTime,stlcTime));

        }

        // 查询轨迹数量的变化  |Q|
        for(int i=0;i< ratioOfQ.length;i++){
            //List<Double> STLCSS_times = new ArrayList<>();
            int deviceNum = 600;
            int trajNum = 300;
            double q_ratio = ratioOfQ[i];
            int querySize = (int) (q_ratio*rawTrajectories.size());
            HashMap<Integer,List<RawTrajectory>>  devices = sampleTrajs(rawTrajectories,trajNum, deviceNum);
            List<RawTrajectory> queryTrajs = generateQuery(rawTrajectories, querySize);
            Map<Integer, TimeIntervalTree> indexes = buildIndex(devices);
            // filter用时
            long beforeFilter = System.currentTimeMillis();
            List<Integer> candidates = filter(queryTrajs, indexes);
            long afterFilter = System.currentTimeMillis();

            // 本地计算用时, 0 for stlcss, 1 for stlc
            long beforeSTLCSSRefinement = System.currentTimeMillis();
            candidates.parallelStream().forEach(d->{
                List<RawTrajectory> localRes = devices.get(d);
                calculate(queryTrajs,localRes,0);
            });
            /*for(Integer d : candidates){
                List<RawTrajectory> localRes = devices.get(i);
                calculate(queryTrajs,localRes,0);
            }*/
            long endSTLCSSRefinement = System.currentTimeMillis();

            long beforeSTLCRefinement = System.currentTimeMillis();
            candidates.parallelStream().forEach(d->{
                List<RawTrajectory> localRes = devices.get(d);
                calculate(queryTrajs,localRes,1);
            });
            /*for(Integer d : candidates){
                List<RawTrajectory> localRes = devices.get(i);
                calculate(queryTrajs,localRes,0);
            }*/

            long endSTLCRefinement = System.currentTimeMillis();

            double stlcssTime = (double) (afterFilter - beforeFilter + endSTLCSSRefinement - beforeSTLCSSRefinement)/1000;
            double stlcTime = (double)(afterFilter - beforeFilter + endSTLCRefinement - beforeSTLCRefinement)/1000;

            logger.info(String.format("Query trajectory size %f, total time for STLCSS : %3f , total time for STLC : %3f, ",q_ratio,stlcssTime,stlcTime));

        }



    }

    public static void calculate(List<RawTrajectory> queryTrajs, List<RawTrajectory> localTrajs, int style){
        queryTrajs.parallelStream().forEach(qt-> {
            int q_length = qt.getGpslog().size();
            long starttime = qt.getGpslog().get(0).getTimestamp();
            long endtime = qt.getGpslog().get(q_length-1).getTimestamp();

            for(RawTrajectory rt:localTrajs){
                int rt_length = rt.getGpslog().size();
                long rt_start = rt.getGpslog().get(0).getTimestamp();
                long rt_end = rt.getGpslog().get(rt_length-1).getTimestamp();
                if(rt_end+2*temporalThreshold<starttime||rt_start>endtime+2*temporalThreshold){
                    continue;
                }
                // 0  for stlcss
                if(style==0){
                    STLCSS(qt,rt,distanceThreshold,temporalThreshold);
                }
                // 1 for stlc
                else{
                    STLC(qt,rt,0.5);
                }


            }
        });
        /*for(RawTrajectory qt:queryTrajs){
            int q_length = qt.getGpslog().size();
            long starttime = qt.getGpslog().get(0).getTimestamp();
            long endtime = qt.getGpslog().get(q_length-1).getTimestamp();

            for(RawTrajectory rt:localTrajs){
                int rt_length = rt.getGpslog().size();
                long rt_start = rt.getGpslog().get(0).getTimestamp();
                long rt_end = rt.getGpslog().get(rt_length-1).getTimestamp();
                if(rt_end+2*temporalThreshold<starttime||rt_start>endtime+2*temporalThreshold){
                    continue;
                }
                // 0  for stlcss
                if(style==0){
                    STLCSS(qt,rt,distanceThreshold,temporalThreshold);
                }
                // 1 for stlc
                else{
                    STLC(qt,rt,0.5);
                }


            }
        }*/
    }

    public static double STLCSS(RawTrajectory t1,RawTrajectory t2 , double spatialthreshold, long temporalthreshold){

        int sim = 0;

        for(Point p1:t1.getGpslog()){
            for(Point p2:t2.getGpslog()){
                if(TrajUtil.distanceOfPoints(p1,p2)<=spatialthreshold
                        && Math.abs(p1.getTimestamp()-p2.getTimestamp())<=temporalthreshold){
                    sim+=1;
                }
            }
        }
        return sim;
    }

    public static double STLC(RawTrajectory t1, RawTrajectory t2, double lambda){
        int length1 = t1.getGpslog().size();
        int length2 = t2.getGpslog().size();
        double sim_t1_t2=0;
        double sim_t2_t1=0;
        for(Point p1:t1.getGpslog()){
            double d_sp = Double.MAX_VALUE;
            double d_tem = Double.MAX_VALUE;
            for(Point p2:t2.getGpslog()){
                double d = TrajUtil.distanceOfPoints(p1,p2);
                double d_t = Math.abs(p1.getTimestamp()-p2.getTimestamp());
                d_sp = Math.min(d,d_sp);
                d_tem = Math.min(d_t,d_tem);
            }
            sim_t1_t2 += Math.exp(-d_sp)/length1;
            sim_t1_t2 += Math.exp(-d_tem)/length1;
        }
        for(Point p2:t2.getGpslog()){
            double d_sp = Double.MAX_VALUE;
            double d_tem = Double.MAX_VALUE;
            for(Point p1:t1.getGpslog()){
                double d = TrajUtil.distanceOfPoints(p1,p2);
                double d_t = Math.abs(p1.getTimestamp()-p2.getTimestamp());
                d_sp = Math.min(d,d_sp);
                d_tem = Math.min(d_t,d_tem);
            }
            sim_t2_t1 += Math.exp(-d_sp)/length2;
            sim_t2_t1 += Math.exp(-d_tem)/length2;
        }

        return lambda*sim_t1_t2+(1-lambda)*sim_t2_t1;
    }

    public static List<Integer> filter(List<RawTrajectory> queryTrajs, Map<Integer, TimeIntervalTree> allIndexes){
        Set<Integer> res = Collections.synchronizedSet(new HashSet<>());

        queryTrajs.parallelStream().forEach(t->{
            int length = t.getGpslog().size();
            long starttime = t.getGpslog().get(0).getTimestamp();
            long endtime = t.getGpslog().get(length-1).getTimestamp();
            for(Map.Entry<Integer,TimeIntervalTree> e:allIndexes.entrySet()){
                int deviceno = e.getKey();
                if(res.contains(deviceno)){
                    continue;
                }
                TimeIntervalTree tree = e.getValue();
                List<BaseTimeInterval> baseTimeIntervals = tree.overlap(new LongInterval(starttime,endtime));
                if(baseTimeIntervals.size()!=0){
                    res.add(deviceno);
                }
            }

        });

        /*for(RawTrajectory t:queryTrajs){
            int length = t.getGpslog().size();
            long starttime = t.getGpslog().get(0).getTimestamp();
            long endtime = t.getGpslog().get(length-1).getTimestamp();
            for(Map.Entry<Integer,TimeIntervalTree> e:allIndexes.entrySet()){
                int deviceno = e.getKey();
                if(res.contains(deviceno)){
                    continue;
                }
                TimeIntervalTree tree = e.getValue();
                List<BaseTimeInterval> baseTimeIntervals = tree.overlap(new LongInterval(starttime,endtime));
                if(baseTimeIntervals.size()!=0){
                    res.add(deviceno);
                }
            }
        }*/

        return new ArrayList<>(res);
    }

    // 利用时间进行剪枝
    public static Map<Integer, TimeIntervalTree> buildIndex(HashMap<Integer,List<RawTrajectory>> deviceTrajs){
        Map<Integer, TimeIntervalTree> integerIntervalTreeHashMap = Collections.synchronizedMap( new HashMap<>());

        deviceTrajs.entrySet().parallelStream().forEach(e->{
            Integer i = e.getKey();
            List<RawTrajectory> rawTrajectoryList = e.getValue();
            TimeIntervalTree intervals = new TimeIntervalTree();
            for(RawTrajectory t:rawTrajectoryList){
                int length = t.getGpslog().size();
                long starttime = t.getGpslog().get(0).getTimestamp();
                long endtime = t.getGpslog().get(length-1).getTimestamp();
                intervals.addInterval(new BaseTimeInterval(String.valueOf(i),starttime,endtime));
            }
            integerIntervalTreeHashMap.put(i,intervals);

        });

        /*for(Map.Entry<Integer,List<RawTrajectory>> e : deviceTrajs.entrySet()){
            // deviceno及其轨迹数据集
            Integer i = e.getKey();
            List<RawTrajectory> rawTrajectoryList = e.getValue();
            TimeIntervalTree intervals = new TimeIntervalTree();
            for(RawTrajectory t:rawTrajectoryList){
                int length = t.getGpslog().size();
                long starttime = t.getGpslog().get(0).getTimestamp();
                long endtime = t.getGpslog().get(length-1).getTimestamp();
                intervals.addInterval(new BaseTimeInterval(String.valueOf(i),starttime,endtime));
            }
            integerIntervalTreeHashMap.put(i,intervals);
        }*/
        return integerIntervalTreeHashMap;
    }


    // 生成deviceNum个轨迹集,每个轨迹集数量为trajNum
    public static HashMap<Integer,List<RawTrajectory>> sampleTrajs(List<RawTrajectory> allTrajs, int trajNum, int deviceNum){
        int size = allTrajs.size();
        HashMap<Integer, List<RawTrajectory>> result = new HashMap<>();

        for(int j=0;j<deviceNum;j++){
            int count = j;
            List<RawTrajectory> rawTrajectoryList = new ArrayList<>();

            for(int i=0;i<trajNum;i++){
                count = (count+deviceNum)%size;
                rawTrajectoryList.add(allTrajs.get(count));
            }
            result.put(j,rawTrajectoryList);
        }
        return result;
    }

    public static List<RawTrajectory> generateQuery(List<RawTrajectory> rawTrajectories, int querysize){

        List<RawTrajectory> result = new ArrayList<>();
        int size = rawTrajectories.size();
        int random = (int) (Math.random()*size*1/2);
        for(int i=0;i<querysize;i++){
            result.add(rawTrajectories.get((i+random)%size));
        }
        return result;
    }



}
