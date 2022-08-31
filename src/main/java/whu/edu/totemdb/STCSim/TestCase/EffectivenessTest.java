package whu.edu.totemdb.STCSim.TestCase;

import javafx.util.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import whu.edu.totemdb.STCSim.Base.*;
import whu.edu.totemdb.STCSim.Index.GridIndex;
import whu.edu.totemdb.STCSim.Settings;
import whu.edu.totemdb.STCSim.StayPointDetection.SPExtrator;
import whu.edu.totemdb.STCSim.Utils.DataLoader;
import whu.edu.totemdb.STCSim.Utils.TrajUtil;

import java.util.*;

public class EffectivenessTest {
    // 对比LCTS, distance LCTS以及带有时间扩展的LCSS相似度的有效性，GPS 采样率，误差以及位置偏移对相似度结果的比较

    public static List<RawTrajectory> rawTrajectories = DataLoader.loadRawTrajDataBeiJing();

    public static double distanceThreshold = 50;
    public static long temporalThreshold = 20;

    public static List<POI> pois = DataLoader.loadPOIDataBeiJing();

    public static Log logger = LogFactory.getLog(EffectivenessTest.class);

    public static SPExtrator sp = new SPExtrator(null,pois);

    public static void main(String[] args){
        TestSTLCSS();
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

    public static double LCTS(MappedTrajectory t1, MappedTrajectory t2){
        double sim = TrajUtil.STCSim(t1,t2);
        return sim ;
    }

    public static double d_LCTS(StayPointTrajectory t1, StayPointTrajectory t2,double distanceThreshold){
        double sim = 0;
        for(StayPoint p1:t1.getStayPointList()){
            Point point1 = new Point(p1.getLat(),p1.getLon(),0);
            for(StayPoint p2:t2.getStayPointList()){
                Point point2 = new Point(p2.getLat(),p2.getLon(),0);
                if(TrajUtil.distanceOfPoints(point1,point2)<=distanceThreshold){
                    sim += Math.max(0,
                            Math.min(p1.getEndTime(),p2.getEndTime())-Math.max(p1.getStartTime(),p2.getStartTime()));
                }
            }
        }
        return sim;
    }




    public static List<RawTrajectory> generateGroundTruthSet(List<StayPoint> stayPoints, int samplingRate, int k, double gps_error, long timeshifting){
        // beijing数据集的sampling rate 为 5s, sampling rate也设为 5s
        // delta = 0.00002
        List<RawTrajectory> rawTrajectoryList = new ArrayList<>();
        double delta = 0.00002*samplingRate;
        assert stayPoints.size() >= 2;
        int segments = stayPoints.size()-1;

        for(int i=0;i<k;i++){
            RawTrajectory rawTrajectory = new RawTrajectory();
            StayPoint s0= stayPoints.get(0);
            int stay_number = (int) ((s0.getEndTime()-s0.getStartTime())/samplingRate)+1;
            long starttime0 = s0.getStartTime();
            for(int count = 0;count<stay_number;count++){
                double lat = Math.random()*delta + s0.getLat();
                double lon = Math.random()*delta + s0.getLon();
                rawTrajectory.addPoint(new Point(lat,lon, starttime0+count*samplingRate));
            }

            for(int j = 0; j < segments;j++){
                StayPoint s1 = stayPoints.get(j);

                long starttime2 = s1.getEndTime();
                StayPoint s2 = stayPoints.get(j+1);
                double delta_lat_direction = s2.getLat()-s1.getLat();
                double delta_lon_direction = s2.getLon()-s1.getLon();
                int segmentlength = (int) ((s2.getStartTime()-s1.getEndTime())/samplingRate);
                double delta_lat_ = delta * (delta_lat_direction/Math.sqrt(Math.pow(delta_lat_direction,2) + Math.pow(delta_lon_direction,2)));
                double delta_lon_ = delta * (delta_lon_direction/Math.sqrt(Math.pow(delta_lat_direction,2) + Math.pow(delta_lon_direction,2)));
                for(int count = 0; count<segmentlength;count++){
                    double lat = s1.getLat() + delta_lat_;
                    double lon = s1.getLon() + delta_lon_;
                    rawTrajectory.addPoint(new Point(lat,lon, starttime2 + count * samplingRate));
                }

                int stay_numberat_s2 = (int) ((s1.getEndTime()-s1.getStartTime())/samplingRate)+1;
                long starttime3 = s2.getStartTime();
                for(int count=0;count<stay_numberat_s2;count++){
                    double lat = Math.random()*delta + s2.getLat();
                    double lon = Math.random()*delta + s2.getLon();
                    rawTrajectory.addPoint(new Point(lat,lon, starttime3+count*samplingRate));
                }

            }
            for(Point p:rawTrajectory.getGpslog()){
                p.setLat(p.getLat()+gps_error);
                p.setLon(p.getLon()+gps_error);
                p.setTimestamp(p.getTimestamp()+timeshifting);
            }
            rawTrajectoryList.add(rawTrajectory);
        }

        return rawTrajectoryList;
    }

    public static void TestSTLCSS(){
        // 考察STLCSS的 topk precision以及NDCG

        // 随机选取1000条轨迹，每条轨迹生成k条相关轨迹作为ground truth set


        // 变换sampling rate, 观察准确率的变化
        int[] k_variables = {5,10,15};
        for(int a=0;a<k_variables.length;a++)
        {
            int totalLength = rawTrajectories.size();
            // int random = (int) (Math.random()*totalLength*1/2);
            int basesize = 1000;

            int k = k_variables[a];

            // 记录轨迹的grade值
            HashMap<Integer, Integer> trajGrade = new HashMap<>();
            // 记录query trajectory的ground truth set
            HashMap<Integer, HashSet<Integer>> groundtruthset = new HashMap<>();

            // 选取stay point数量大于5的轨迹作为待查询轨迹并进行ground truth set的生成
            List<RawTrajectory> queryTrajs = new ArrayList<>();
            HashMap<Integer, List<StayPoint>> staypoinTrajs = new HashMap<>();

            // 将生成的轨迹加入到数据集中作为最终的查询轨迹集
            List<RawTrajectory> baseTrajs = new ArrayList<>();
            //int samplingRate = 5 + k/5;
            int samplingRate = 30 / k;


            baseTrajs.addAll(rawTrajectories);

            for (RawTrajectory rt : rawTrajectories) {

                List<StayPoint> sps = sp.extractStayPoints(rt, Settings.trajectoryClusterDistanceThreshold
                        , Settings.trajectoryClusterTimeThreshold);

                // 大约1600条轨迹

                if (sps.size() >= 5) {
                    queryTrajs.add(rt);
                    staypoinTrajs.put(rt.getId(), sps);
                }
            }

            for (int i = 0; i < basesize; i++) {
                RawTrajectory t = queryTrajs.get(i);
                int queryId = t.getId();
                groundtruthset.put(queryId, new HashSet<>());
                List<StayPoint> sps = staypoinTrajs.get(queryId);
                List<RawTrajectory> generatedTrajs = generateGroundTruthSet(sps, samplingRate, k,0,0);
                int tmpbasesize = baseTrajs.size();
                for (int j = 0; j < generatedTrajs.size(); j++) {
                    RawTrajectory rawTrajectory = generatedTrajs.get(j);
                    rawTrajectory.setId(tmpbasesize + j);
                    baseTrajs.add(rawTrajectory);
                    groundtruthset.get(queryId).add(rawTrajectory.getId());
                }
            }

            int queryTrajNumber = 40;
            // 观察stlcss的变化
            for(int j=0;j<1;j++){
                // 利用相似度查询给定轨迹的top k条轨迹， 并计算相应的精确度。
                double topk_precision_stlcss = 0;
                double topk_ndcg_stlcss = 0;
                List<Integer> querylist = new ArrayList<>(groundtruthset.keySet());

                for (int i = 0; i < queryTrajNumber; i++) {

                    // 获取待查询的轨迹数据, 每条轨迹记录其id以及相似度值 <T_id, sim>
                    RawTrajectory queryTraj = baseTrajs.get(querylist.get(i));

                    List<Pair<Integer, Double>> result = Collections.synchronizedList(new ArrayList<>());
                    int querylength = queryTraj.getGpslog().size();
                    long starttime = queryTraj.getGpslog().get(0).getTimestamp();
                    long endtime = queryTraj.getGpslog().get(querylength - 1).getTimestamp();

                    /*for (RawTrajectory t : baseTrajs) {
                        if (t.getId() == queryTraj.getId()) {
                            continue;
                        }
                        int tlength = t.getGpslog().size();
                        long ts = t.getGpslog().get(0).getTimestamp();
                        long te = t.getGpslog().get(tlength - 1).getTimestamp();
                        if (ts > endtime + temporalThreshold || te < starttime - temporalThreshold) {
                            continue;
                        }

                        double sim = STLCSS(t, queryTraj, distanceThreshold, temporalThreshold);
                        result.add(new Pair<>(t.getId(), sim));

                    }*/


                    baseTrajs.parallelStream().forEach(t-> {
                        if (t.getId() == queryTraj.getId()) {
                            return;
                        }
                        int tlength = t.getGpslog().size();
                        long ts = t.getGpslog().get(0).getTimestamp();
                        long te = t.getGpslog().get(tlength - 1).getTimestamp();
                        if (ts > endtime + temporalThreshold || te < starttime - temporalThreshold) {
                            return;
                        }

                        // double sim = STLCSS(t, queryTraj, distanceThreshold, temporalThreshold);
                        double sim = STLCSS(t, queryTraj, distanceThreshold, temporalThreshold);
                        result.add(new Pair<>(t.getId(), sim));
                    });

                    result.sort(new Comparator<Pair<Integer, Double>>() {
                        @Override
                        public int compare(Pair<Integer, Double> o1, Pair<Integer, Double> o2) {
                            if (Objects.equals(o1.getValue(), o2.getValue())) {
                                return 0;
                            } else if (o1.getValue() < o2.getValue()) {
                                return 1;
                            }
                            return -1;
                        }
                    });

                    double topk_gain = 0;
                    double idcg = 0;
                    double dcg = 0;

                    for (int m = 0; m < k && m < result.size(); m++) {
                        Pair<Integer, Double> p = result.get(m);
                        idcg = idcg + 2 / (Math.log(m + 2) / Math.log(2));
                        if (groundtruthset.get(querylist.get(i)).contains(p.getKey())) {
                            topk_gain += 1;
                            dcg = dcg + 2 / (Math.log(m + 2) / Math.log(2));
                        } else {
                            dcg = dcg + 1 / (Math.log(m + 2) / Math.log(2));
                        }
                    }
                    topk_precision_stlcss += topk_gain / k;
                    topk_ndcg_stlcss += dcg / idcg;
                }
                topk_precision_stlcss /= queryTrajNumber;
                topk_ndcg_stlcss /= queryTrajNumber;
                logger.info(String.format("Varying sampling rate, top %d precision of STLCSS : %f, Top %d NDCG of STLCSS : %f", k, topk_precision_stlcss, k, topk_ndcg_stlcss));
            }

            // 观察stlc的变化
            for(int j=0;j<1;j++){
                // 利用相似度查询给定轨迹的top k条轨迹， 并计算相应的精确度。
                double topk_precision_stlc = 0;
                double topk_ndcg_stlc = 0;
                List<Integer> querylist = new ArrayList<>(groundtruthset.keySet());
                //int queryTrajNumber = 50;

                for (int i = 0; i < queryTrajNumber; i++) {

                    // 获取待查询的轨迹数据, 每条轨迹记录其id以及相似度值 <T_id, sim>
                    RawTrajectory queryTraj = baseTrajs.get(querylist.get(i));

                    List<Pair<Integer, Double>> result = Collections.synchronizedList(new ArrayList<>());
                    int querylength = queryTraj.getGpslog().size();
                    long starttime = queryTraj.getGpslog().get(0).getTimestamp();
                    long endtime = queryTraj.getGpslog().get(querylength - 1).getTimestamp();

                    /*for (RawTrajectory t : baseTrajs) {
                        if (t.getId() == queryTraj.getId()) {
                            continue;
                        }
                        int tlength = t.getGpslog().size();
                        long ts = t.getGpslog().get(0).getTimestamp();
                        long te = t.getGpslog().get(tlength - 1).getTimestamp();
                        if (ts > endtime + temporalThreshold || te < starttime - temporalThreshold) {
                            continue;
                        }

                        // double sim = STLCSS(t, queryTraj, distanceThreshold, temporalThreshold);
                        double sim = STLC(t,queryTraj,0.5);
                        result.add(new Pair<>(t.getId(), sim));
                    }*/


                    baseTrajs.parallelStream().forEach(t-> {
                        if (t.getId() == queryTraj.getId()) {
                            return;
                        }
                        int tlength = t.getGpslog().size();
                        long ts = t.getGpslog().get(0).getTimestamp();
                        long te = t.getGpslog().get(tlength - 1).getTimestamp();
                        if (ts > endtime + temporalThreshold || te < starttime - temporalThreshold) {
                            return;
                        }

                        // double sim = STLCSS(t, queryTraj, distanceThreshold, temporalThreshold);
                        double sim = STLC(t,queryTraj,0.5);
                        result.add(new Pair<>(t.getId(), sim));
                    });

                    result.sort(new Comparator<Pair<Integer, Double>>() {
                        @Override
                        public int compare(Pair<Integer, Double> o1, Pair<Integer, Double> o2) {
                            if (Objects.equals(o1.getValue(), o2.getValue())) {
                                return 0;
                            } else if (o1.getValue() < o2.getValue()) {
                                return 1;
                            }
                            return -1;
                        }
                    });

                    double topk_gain = 0;
                    double idcg = 0;
                    double dcg = 0;

                    for (int m = 0; m < k && m < result.size(); m++) {
                        Pair<Integer, Double> p = result.get(m);
                        idcg = idcg + 2 / (Math.log(m + 2) / Math.log(2));
                        if (groundtruthset.get(querylist.get(i)).contains(p.getKey())) {
                            topk_gain += 1;
                            dcg = dcg + 2 / (Math.log(m + 2) / Math.log(2));
                        } else {
                            dcg = dcg + 1 / (Math.log(m + 2) / Math.log(2));
                        }
                    }
                    topk_precision_stlc += topk_gain / k;
                    topk_ndcg_stlc += dcg / idcg;
                }
                topk_precision_stlc /= queryTrajNumber;
                topk_ndcg_stlc /= queryTrajNumber;
                logger.info(String.format("Varying sampling rate, top %d precision of STLC : %f, top %d NDCG of STLC : %f", k, topk_precision_stlc, k, topk_ndcg_stlc));
            }

            // 观察d-lcts的变化
            for(int j=0;j<1;j++){
                // 利用相似度查询给定轨迹的top k条轨迹， 并计算相应的精确度。
                double topk_precision_d_lcts = 0;
                double topk_ndcg_d_lcts = 0;
                List<Integer> querylist = new ArrayList<>(groundtruthset.keySet());
                //int queryTrajNumber = 50;

                for (int i = 0; i < queryTrajNumber; i++) {

                    // 获取待查询的轨迹数据, 每条轨迹记录其id以及相似度值 <T_id, sim>
                    RawTrajectory queryTraj = baseTrajs.get(querylist.get(i));

                    List<StayPoint> stayPointList = sp.extractStayPoints(queryTraj, Settings.trajectoryClusterDistanceThreshold
                            , Settings.trajectoryClusterTimeThreshold);
                    StayPointTrajectory stayPointTrajectory = new StayPointTrajectory(queryTraj.getId(),stayPointList);

                    List<Pair<Integer, Double>> result = Collections.synchronizedList(new ArrayList<>());
                    int querylength = queryTraj.getGpslog().size();
                    long starttime = queryTraj.getGpslog().get(0).getTimestamp();
                    long endtime = queryTraj.getGpslog().get(querylength - 1).getTimestamp();

                    /*for (RawTrajectory t : baseTrajs) {
                        if (t.getId() == queryTraj.getId()) {
                            continue;
                        }
                        int tlength = t.getGpslog().size();
                        long ts = t.getGpslog().get(0).getTimestamp();
                        long te = t.getGpslog().get(tlength - 1).getTimestamp();
                        if (ts > endtime + temporalThreshold || te < starttime - temporalThreshold) {
                            continue;
                        }

                        // double sim = STLCSS(t, queryTraj, distanceThreshold, temporalThreshold);
                        double sim = STLC(t,queryTraj,0.5);
                        result.add(new Pair<>(t.getId(), sim));
                    }*/


                    baseTrajs.parallelStream().forEach(t-> {
                        if (t.getId() == queryTraj.getId()) {
                            return;
                        }
                        int tlength = t.getGpslog().size();
                        long ts = t.getGpslog().get(0).getTimestamp();
                        long te = t.getGpslog().get(tlength - 1).getTimestamp();
                        if (ts > endtime + temporalThreshold || te < starttime - temporalThreshold) {
                            return;
                        }

                        List<StayPoint> tstaypoints = sp.extractStayPoints(t, Settings.trajectoryClusterDistanceThreshold
                                , Settings.trajectoryClusterTimeThreshold);

                        StayPointTrajectory tstraj = new StayPointTrajectory(t.getId(), tstaypoints);

                        double sim = d_LCTS(tstraj,stayPointTrajectory,distanceThreshold);
                        result.add(new Pair<>(t.getId(), sim));
                    });

                    result.sort(new Comparator<Pair<Integer, Double>>() {
                        @Override
                        public int compare(Pair<Integer, Double> o1, Pair<Integer, Double> o2) {
                            if (Objects.equals(o1.getValue(), o2.getValue())) {
                                return 0;
                            } else if (o1.getValue() < o2.getValue()) {
                                return 1;
                            }
                            return -1;
                        }
                    });

                    double topk_gain = 0;
                    double idcg = 0;
                    double dcg = 0;

                    for (int m = 0; m < k && m < result.size(); m++) {
                        Pair<Integer, Double> p = result.get(m);
                        idcg = idcg + 2 / (Math.log(m + 2) / Math.log(2));
                        if (groundtruthset.get(querylist.get(i)).contains(p.getKey())) {
                            topk_gain += 1;
                            dcg = dcg + 2 / (Math.log(m + 2) / Math.log(2));
                        } else {
                            dcg = dcg + 1 / (Math.log(m + 2) / Math.log(2));
                        }
                    }
                    topk_precision_d_lcts += topk_gain / k;
                    topk_ndcg_d_lcts += dcg / idcg;
                }
                topk_precision_d_lcts /= queryTrajNumber;
                topk_ndcg_d_lcts /= queryTrajNumber;
                logger.info(String.format("Varying sampling rate, top %d precision of d-LCTS : %f, top %d NDCG of d_LCTS : %f", k, topk_precision_d_lcts, k, topk_ndcg_d_lcts));
            }

            // 观察lcts的变化
            for(int j=0;j<1;j++){
                // 利用相似度查询给定轨迹的top k条轨迹， 并计算相应的精确度。
                double topk_precision_lcts = 0;
                double topk_ndcg_lcts = 0;
                List<Integer> querylist = new ArrayList<>(groundtruthset.keySet());
                //int queryTrajNumber = 50;

                for (int i = 0; i < queryTrajNumber; i++) {

                    // 获取待查询的轨迹数据, 每条轨迹记录其id以及相似度值 <T_id, sim>
                    RawTrajectory queryTraj = baseTrajs.get(querylist.get(i));

                    /*List<StayPoint> stayPointList = sp.extractStayPoints(queryTraj, Settings.trajectoryClusterDistanceThreshold
                            , Settings.trajectoryClusterTimeThreshold);
                    StayPointTrajectory stayPointTrajectory = new StayPointTrajectory(queryTraj.getId(),stayPointList);
*/

                    MappedTrajectory queryMappedTraj = sp.extract(queryTraj,Settings.trajectoryClusterDistanceThreshold
                            , Settings.trajectoryClusterTimeThreshold,Settings.radius / 1000);


                    List<Pair<Integer, Double>> result = Collections.synchronizedList(new ArrayList<>());
                    int querylength = queryTraj.getGpslog().size();
                    long starttime = queryTraj.getGpslog().get(0).getTimestamp();
                    long endtime = queryTraj.getGpslog().get(querylength - 1).getTimestamp();

                    /*for (RawTrajectory t : baseTrajs) {
                        if (t.getId() == queryTraj.getId()) {
                            continue;
                        }
                        int tlength = t.getGpslog().size();
                        long ts = t.getGpslog().get(0).getTimestamp();
                        long te = t.getGpslog().get(tlength - 1).getTimestamp();
                        if (ts > endtime + temporalThreshold || te < starttime - temporalThreshold) {
                            continue;
                        }

                        // double sim = STLCSS(t, queryTraj, distanceThreshold, temporalThreshold);
                        double sim = STLC(t,queryTraj,0.5);
                        result.add(new Pair<>(t.getId(), sim));
                    }*/

                    baseTrajs.parallelStream().forEach(t-> {
                        if (t.getId() == queryTraj.getId()) {
                            return;
                        }
                        int tlength = t.getGpslog().size();
                        long ts = t.getGpslog().get(0).getTimestamp();
                        long te = t.getGpslog().get(tlength - 1).getTimestamp();
                        if (ts > endtime + temporalThreshold || te < starttime - temporalThreshold) {
                            return;
                        }

                        MappedTrajectory tMappedTraj = sp.extract(t, Settings.trajectoryClusterDistanceThreshold
                                , Settings.trajectoryClusterTimeThreshold,Settings.radius / 1000);

                        double sim = LCTS(queryMappedTraj,tMappedTraj);
                        result.add(new Pair<>(t.getId(), sim));
                    });

                    result.sort(new Comparator<Pair<Integer, Double>>() {
                        @Override
                        public int compare(Pair<Integer, Double> o1, Pair<Integer, Double> o2) {
                            if (Objects.equals(o1.getValue(), o2.getValue())) {
                                return 0;
                            } else if (o1.getValue() < o2.getValue()) {
                                return 1;
                            }
                            return -1;
                        }
                    });

                    double topk_gain = 0;
                    double idcg = 0;
                    double dcg = 0;

                    for (int m = 0; m < k && m < result.size(); m++) {
                        Pair<Integer, Double> p = result.get(m);
                        idcg = idcg + 2 / (Math.log(m + 2) / Math.log(2));
                        if (groundtruthset.get(querylist.get(i)).contains(p.getKey())) {
                            topk_gain += 1;
                            dcg = dcg + 2 / (Math.log(m + 2) / Math.log(2));
                        } else {
                            dcg = dcg + 1 / (Math.log(m + 2) / Math.log(2));
                        }
                    }
                    topk_precision_lcts += topk_gain / k;
                    topk_ndcg_lcts += dcg / idcg;
                }
                topk_precision_lcts /= queryTrajNumber;
                topk_ndcg_lcts /= queryTrajNumber;
                logger.info(String.format("Varying sampling rate, top %d precision of LCTS : %f, top %d NDCG of d_LCTS : %f", k, topk_precision_lcts, k, topk_ndcg_lcts));
            }


        }

        // 变换gps error, 观察准确率的变化
        for(int a=0;a<k_variables.length;a++)
        {
            int totalLength = rawTrajectories.size();
            // int random = (int) (Math.random()*totalLength*1/2);
            int basesize = 1000;

            int k = k_variables[a];

            // 记录轨迹的grade值
            HashMap<Integer, Integer> trajGrade = new HashMap<>();
            // 记录query trajectory的ground truth set
            HashMap<Integer, HashSet<Integer>> groundtruthset = new HashMap<>();

            // 选取stay point数量大于5的轨迹作为待查询轨迹并进行ground truth set的生成
            List<RawTrajectory> queryTrajs = new ArrayList<>();
            HashMap<Integer, List<StayPoint>> staypoinTrajs = new HashMap<>();

            // 将生成的轨迹加入到数据集中作为最终的查询轨迹集
            List<RawTrajectory> baseTrajs = new ArrayList<>();
            //int samplingRate = 5 + k/5;
            int samplingRate = 5;

            double gps_error = 0.000078;

            baseTrajs.addAll(rawTrajectories);

            for (RawTrajectory rt : rawTrajectories) {

                List<StayPoint> sps = sp.extractStayPoints(rt, Settings.trajectoryClusterDistanceThreshold
                        , Settings.trajectoryClusterTimeThreshold);

                // 大约1600条轨迹

                if (sps.size() >= 5) {
                    queryTrajs.add(rt);
                    staypoinTrajs.put(rt.getId(), sps);
                }
            }

            for (int i = 0; i < basesize; i++) {
                RawTrajectory t = queryTrajs.get(i);
                int queryId = t.getId();
                groundtruthset.put(queryId, new HashSet<>());
                List<StayPoint> sps = staypoinTrajs.get(queryId);
                List<RawTrajectory> generatedTrajs = generateGroundTruthSet(sps, samplingRate, k,gps_error,0);
                int tmpbasesize = baseTrajs.size();
                for (int j = 0; j < generatedTrajs.size(); j++) {
                    RawTrajectory rawTrajectory = generatedTrajs.get(j);
                    rawTrajectory.setId(tmpbasesize + j);
                    baseTrajs.add(rawTrajectory);
                    groundtruthset.get(queryId).add(rawTrajectory.getId());
                }
            }

            int queryTrajNumber = 40;
            // 观察stlcss的变化
            for(int j=0;j<1;j++){
                // 利用相似度查询给定轨迹的top k条轨迹， 并计算相应的精确度。
                double topk_precision_stlcss = 0;
                double topk_ndcg_stlcss = 0;
                List<Integer> querylist = new ArrayList<>(groundtruthset.keySet());

                for (int i = 0; i < queryTrajNumber; i++) {

                    // 获取待查询的轨迹数据, 每条轨迹记录其id以及相似度值 <T_id, sim>
                    RawTrajectory queryTraj = baseTrajs.get(querylist.get(i));

                    List<Pair<Integer, Double>> result = Collections.synchronizedList(new ArrayList<>());
                    int querylength = queryTraj.getGpslog().size();
                    long starttime = queryTraj.getGpslog().get(0).getTimestamp();
                    long endtime = queryTraj.getGpslog().get(querylength - 1).getTimestamp();

                    /*for (RawTrajectory t : baseTrajs) {
                        if (t.getId() == queryTraj.getId()) {
                            continue;
                        }
                        int tlength = t.getGpslog().size();
                        long ts = t.getGpslog().get(0).getTimestamp();
                        long te = t.getGpslog().get(tlength - 1).getTimestamp();
                        if (ts > endtime + temporalThreshold || te < starttime - temporalThreshold) {
                            continue;
                        }

                        double sim = STLCSS(t, queryTraj, distanceThreshold, temporalThreshold);
                        result.add(new Pair<>(t.getId(), sim));

                    }*/


                    baseTrajs.parallelStream().forEach(t-> {
                        if (t.getId() == queryTraj.getId()) {
                            return;
                        }
                        int tlength = t.getGpslog().size();
                        long ts = t.getGpslog().get(0).getTimestamp();
                        long te = t.getGpslog().get(tlength - 1).getTimestamp();
                        if (ts > endtime + temporalThreshold || te < starttime - temporalThreshold) {
                            return;
                        }

                        // double sim = STLCSS(t, queryTraj, distanceThreshold, temporalThreshold);
                        double sim = STLCSS(t, queryTraj, distanceThreshold, temporalThreshold);
                        result.add(new Pair<>(t.getId(), sim));
                    });

                    result.sort(new Comparator<Pair<Integer, Double>>() {
                        @Override
                        public int compare(Pair<Integer, Double> o1, Pair<Integer, Double> o2) {
                            if (Objects.equals(o1.getValue(), o2.getValue())) {
                                return 0;
                            } else if (o1.getValue() < o2.getValue()) {
                                return 1;
                            }
                            return -1;
                        }
                    });

                    double topk_gain = 0;
                    double idcg = 0;
                    double dcg = 0;

                    for (int m = 0; m < k && m < result.size(); m++) {
                        Pair<Integer, Double> p = result.get(m);
                        idcg = idcg + 2 / (Math.log(m + 2) / Math.log(2));
                        if (groundtruthset.get(querylist.get(i)).contains(p.getKey())) {
                            topk_gain += 1;
                            dcg = dcg + 2 / (Math.log(m + 2) / Math.log(2));
                        } else {
                            dcg = dcg + 1 / (Math.log(m + 2) / Math.log(2));
                        }
                    }
                    topk_precision_stlcss += topk_gain / k;
                    topk_ndcg_stlcss += dcg / idcg;
                }
                topk_precision_stlcss /= queryTrajNumber;
                topk_ndcg_stlcss /= queryTrajNumber;
                logger.info(String.format("Varying gps error, top %d precision of STLCSS : %f, Top %d NDCG of STLCSS : %f", k, topk_precision_stlcss, k, topk_ndcg_stlcss));
            }

            // 观察stlc的变化
            for(int j=0;j<1;j++){
                // 利用相似度查询给定轨迹的top k条轨迹， 并计算相应的精确度。
                double topk_precision_stlc = 0;
                double topk_ndcg_stlc = 0;
                List<Integer> querylist = new ArrayList<>(groundtruthset.keySet());
                //int queryTrajNumber = 50;

                for (int i = 0; i < queryTrajNumber; i++) {

                    // 获取待查询的轨迹数据, 每条轨迹记录其id以及相似度值 <T_id, sim>
                    RawTrajectory queryTraj = baseTrajs.get(querylist.get(i));

                    List<Pair<Integer, Double>> result = Collections.synchronizedList(new ArrayList<>());
                    int querylength = queryTraj.getGpslog().size();
                    long starttime = queryTraj.getGpslog().get(0).getTimestamp();
                    long endtime = queryTraj.getGpslog().get(querylength - 1).getTimestamp();

                    /*for (RawTrajectory t : baseTrajs) {
                        if (t.getId() == queryTraj.getId()) {
                            continue;
                        }
                        int tlength = t.getGpslog().size();
                        long ts = t.getGpslog().get(0).getTimestamp();
                        long te = t.getGpslog().get(tlength - 1).getTimestamp();
                        if (ts > endtime + temporalThreshold || te < starttime - temporalThreshold) {
                            continue;
                        }

                        // double sim = STLCSS(t, queryTraj, distanceThreshold, temporalThreshold);
                        double sim = STLC(t,queryTraj,0.5);
                        result.add(new Pair<>(t.getId(), sim));
                    }*/


                    baseTrajs.parallelStream().forEach(t-> {
                        if (t.getId() == queryTraj.getId()) {
                            return;
                        }
                        int tlength = t.getGpslog().size();
                        long ts = t.getGpslog().get(0).getTimestamp();
                        long te = t.getGpslog().get(tlength - 1).getTimestamp();
                        if (ts > endtime + temporalThreshold || te < starttime - temporalThreshold) {
                            return;
                        }

                        // double sim = STLCSS(t, queryTraj, distanceThreshold, temporalThreshold);
                        double sim = STLC(t,queryTraj,0.5);
                        result.add(new Pair<>(t.getId(), sim));
                    });

                    result.sort(new Comparator<Pair<Integer, Double>>() {
                        @Override
                        public int compare(Pair<Integer, Double> o1, Pair<Integer, Double> o2) {
                            if (Objects.equals(o1.getValue(), o2.getValue())) {
                                return 0;
                            } else if (o1.getValue() < o2.getValue()) {
                                return 1;
                            }
                            return -1;
                        }
                    });

                    double topk_gain = 0;
                    double idcg = 0;
                    double dcg = 0;

                    for (int m = 0; m < k && m < result.size(); m++) {
                        Pair<Integer, Double> p = result.get(m);
                        idcg = idcg + 2 / (Math.log(m + 2) / Math.log(2));
                        if (groundtruthset.get(querylist.get(i)).contains(p.getKey())) {
                            topk_gain += 1;
                            dcg = dcg + 2 / (Math.log(m + 2) / Math.log(2));
                        } else {
                            dcg = dcg + 1 / (Math.log(m + 2) / Math.log(2));
                        }
                    }
                    topk_precision_stlc += topk_gain / k;
                    topk_ndcg_stlc += dcg / idcg;
                }
                topk_precision_stlc /= queryTrajNumber;
                topk_ndcg_stlc /= queryTrajNumber;
                logger.info(String.format("Varying gps error, top %d precision of STLC : %f, top %d NDCG of STLC : %f", k, topk_precision_stlc, k, topk_ndcg_stlc));
            }

            // 观察d-lcts的变化
            for(int j=0;j<1;j++){
                // 利用相似度查询给定轨迹的top k条轨迹， 并计算相应的精确度。
                double topk_precision_d_lcts = 0;
                double topk_ndcg_d_lcts = 0;
                List<Integer> querylist = new ArrayList<>(groundtruthset.keySet());
                //int queryTrajNumber = 50;

                for (int i = 0; i < queryTrajNumber; i++) {

                    // 获取待查询的轨迹数据, 每条轨迹记录其id以及相似度值 <T_id, sim>
                    RawTrajectory queryTraj = baseTrajs.get(querylist.get(i));

                    List<StayPoint> stayPointList = sp.extractStayPoints(queryTraj, Settings.trajectoryClusterDistanceThreshold
                            , Settings.trajectoryClusterTimeThreshold);
                    StayPointTrajectory stayPointTrajectory = new StayPointTrajectory(queryTraj.getId(),stayPointList);

                    List<Pair<Integer, Double>> result = Collections.synchronizedList(new ArrayList<>());
                    int querylength = queryTraj.getGpslog().size();
                    long starttime = queryTraj.getGpslog().get(0).getTimestamp();
                    long endtime = queryTraj.getGpslog().get(querylength - 1).getTimestamp();

                    /*for (RawTrajectory t : baseTrajs) {
                        if (t.getId() == queryTraj.getId()) {
                            continue;
                        }
                        int tlength = t.getGpslog().size();
                        long ts = t.getGpslog().get(0).getTimestamp();
                        long te = t.getGpslog().get(tlength - 1).getTimestamp();
                        if (ts > endtime + temporalThreshold || te < starttime - temporalThreshold) {
                            continue;
                        }

                        // double sim = STLCSS(t, queryTraj, distanceThreshold, temporalThreshold);
                        double sim = STLC(t,queryTraj,0.5);
                        result.add(new Pair<>(t.getId(), sim));
                    }*/


                    baseTrajs.parallelStream().forEach(t-> {
                        if (t.getId() == queryTraj.getId()) {
                            return;
                        }
                        int tlength = t.getGpslog().size();
                        long ts = t.getGpslog().get(0).getTimestamp();
                        long te = t.getGpslog().get(tlength - 1).getTimestamp();
                        if (ts > endtime + temporalThreshold || te < starttime - temporalThreshold) {
                            return;
                        }

                        List<StayPoint> tstaypoints = sp.extractStayPoints(t, Settings.trajectoryClusterDistanceThreshold
                                , Settings.trajectoryClusterTimeThreshold);

                        StayPointTrajectory tstraj = new StayPointTrajectory(t.getId(), tstaypoints);

                        double sim = d_LCTS(tstraj,stayPointTrajectory,distanceThreshold);
                        result.add(new Pair<>(t.getId(), sim));
                    });

                    result.sort(new Comparator<Pair<Integer, Double>>() {
                        @Override
                        public int compare(Pair<Integer, Double> o1, Pair<Integer, Double> o2) {
                            if (Objects.equals(o1.getValue(), o2.getValue())) {
                                return 0;
                            } else if (o1.getValue() < o2.getValue()) {
                                return 1;
                            }
                            return -1;
                        }
                    });

                    double topk_gain = 0;
                    double idcg = 0;
                    double dcg = 0;

                    for (int m = 0; m < k && m < result.size(); m++) {
                        Pair<Integer, Double> p = result.get(m);
                        idcg = idcg + 2 / (Math.log(m + 2) / Math.log(2));
                        if (groundtruthset.get(querylist.get(i)).contains(p.getKey())) {
                            topk_gain += 1;
                            dcg = dcg + 2 / (Math.log(m + 2) / Math.log(2));
                        } else {
                            dcg = dcg + 1 / (Math.log(m + 2) / Math.log(2));
                        }
                    }
                    topk_precision_d_lcts += topk_gain / k;
                    topk_ndcg_d_lcts += dcg / idcg;
                }
                topk_precision_d_lcts /= queryTrajNumber;
                topk_ndcg_d_lcts /= queryTrajNumber;
                logger.info(String.format("Varying gps error, top %d precision of d-LCTS : %f, top %d NDCG of d_LCTS : %f", k, topk_precision_d_lcts, k, topk_ndcg_d_lcts));
            }

            // 观察lcts的变化
            for(int j=0;j<1;j++){
                // 利用相似度查询给定轨迹的top k条轨迹， 并计算相应的精确度。
                double topk_precision_lcts = 0;
                double topk_ndcg_lcts = 0;
                List<Integer> querylist = new ArrayList<>(groundtruthset.keySet());
                //int queryTrajNumber = 50;

                for (int i = 0; i < queryTrajNumber; i++) {

                    // 获取待查询的轨迹数据, 每条轨迹记录其id以及相似度值 <T_id, sim>
                    RawTrajectory queryTraj = baseTrajs.get(querylist.get(i));

                    /*List<StayPoint> stayPointList = sp.extractStayPoints(queryTraj, Settings.trajectoryClusterDistanceThreshold
                            , Settings.trajectoryClusterTimeThreshold);
                    StayPointTrajectory stayPointTrajectory = new StayPointTrajectory(queryTraj.getId(),stayPointList);
*/

                    MappedTrajectory queryMappedTraj = sp.extract(queryTraj,Settings.trajectoryClusterDistanceThreshold
                            , Settings.trajectoryClusterTimeThreshold,Settings.radius / 1000);


                    List<Pair<Integer, Double>> result = Collections.synchronizedList(new ArrayList<>());
                    int querylength = queryTraj.getGpslog().size();
                    long starttime = queryTraj.getGpslog().get(0).getTimestamp();
                    long endtime = queryTraj.getGpslog().get(querylength - 1).getTimestamp();

                    /*for (RawTrajectory t : baseTrajs) {
                        if (t.getId() == queryTraj.getId()) {
                            continue;
                        }
                        int tlength = t.getGpslog().size();
                        long ts = t.getGpslog().get(0).getTimestamp();
                        long te = t.getGpslog().get(tlength - 1).getTimestamp();
                        if (ts > endtime + temporalThreshold || te < starttime - temporalThreshold) {
                            continue;
                        }

                        // double sim = STLCSS(t, queryTraj, distanceThreshold, temporalThreshold);
                        double sim = STLC(t,queryTraj,0.5);
                        result.add(new Pair<>(t.getId(), sim));
                    }*/

                    baseTrajs.parallelStream().forEach(t-> {
                        if (t.getId() == queryTraj.getId()) {
                            return;
                        }
                        int tlength = t.getGpslog().size();
                        long ts = t.getGpslog().get(0).getTimestamp();
                        long te = t.getGpslog().get(tlength - 1).getTimestamp();
                        if (ts > endtime + temporalThreshold || te < starttime - temporalThreshold) {
                            return;
                        }

                        MappedTrajectory tMappedTraj = sp.extract(t, Settings.trajectoryClusterDistanceThreshold
                                , Settings.trajectoryClusterTimeThreshold,Settings.radius / 1000);

                        double sim = LCTS(queryMappedTraj,tMappedTraj);
                        result.add(new Pair<>(t.getId(), sim));
                    });

                    result.sort(new Comparator<Pair<Integer, Double>>() {
                        @Override
                        public int compare(Pair<Integer, Double> o1, Pair<Integer, Double> o2) {
                            if (Objects.equals(o1.getValue(), o2.getValue())) {
                                return 0;
                            } else if (o1.getValue() < o2.getValue()) {
                                return 1;
                            }
                            return -1;
                        }
                    });

                    double topk_gain = 0;
                    double idcg = 0;
                    double dcg = 0;

                    for (int m = 0; m < k && m < result.size(); m++) {
                        Pair<Integer, Double> p = result.get(m);
                        idcg = idcg + 2 / (Math.log(m + 2) / Math.log(2));
                        if (groundtruthset.get(querylist.get(i)).contains(p.getKey())) {
                            topk_gain += 1;
                            dcg = dcg + 2 / (Math.log(m + 2) / Math.log(2));
                        } else {
                            dcg = dcg + 1 / (Math.log(m + 2) / Math.log(2));
                        }
                    }
                    topk_precision_lcts += topk_gain / k;
                    topk_ndcg_lcts += dcg / idcg;
                }
                topk_precision_lcts /= queryTrajNumber;
                topk_ndcg_lcts /= queryTrajNumber;
                logger.info(String.format("Varying gps error, top %d precision of LCTS : %f, top %d NDCG of d_LCTS : %f", k, topk_precision_lcts, k, topk_ndcg_lcts));
            }


        }


        // 变换time shifting, 观察准确率的变化
        for(int a=0;a<k_variables.length;a++)
        {
            int totalLength = rawTrajectories.size();
            // int random = (int) (Math.random()*totalLength*1/2);
            int basesize = 1000;

            int k = k_variables[a];

            // 记录轨迹的grade值
            HashMap<Integer, Integer> trajGrade = new HashMap<>();
            // 记录query trajectory的ground truth set
            HashMap<Integer, HashSet<Integer>> groundtruthset = new HashMap<>();

            // 选取stay point数量大于5的轨迹作为待查询轨迹并进行ground truth set的生成
            List<RawTrajectory> queryTrajs = new ArrayList<>();
            HashMap<Integer, List<StayPoint>> staypoinTrajs = new HashMap<>();

            // 将生成的轨迹加入到数据集中作为最终的查询轨迹集
            List<RawTrajectory> baseTrajs = new ArrayList<>();
            //int samplingRate = 5 + k/5;
            int samplingRate = 5;

            double gps_error = 0;

            long timeshifting = k * 10;

            baseTrajs.addAll(rawTrajectories);

            for (RawTrajectory rt : rawTrajectories) {

                List<StayPoint> sps = sp.extractStayPoints(rt, Settings.trajectoryClusterDistanceThreshold
                        , Settings.trajectoryClusterTimeThreshold);

                // 大约1600条轨迹

                if (sps.size() >= 5) {
                    queryTrajs.add(rt);
                    staypoinTrajs.put(rt.getId(), sps);
                }
            }

            for (int i = 0; i < basesize; i++) {
                RawTrajectory t = queryTrajs.get(i);
                int queryId = t.getId();
                groundtruthset.put(queryId, new HashSet<>());
                List<StayPoint> sps = staypoinTrajs.get(queryId);
                List<RawTrajectory> generatedTrajs = generateGroundTruthSet(sps, samplingRate, k,gps_error,timeshifting);
                int tmpbasesize = baseTrajs.size();
                for (int j = 0; j < generatedTrajs.size(); j++) {
                    RawTrajectory rawTrajectory = generatedTrajs.get(j);
                    rawTrajectory.setId(tmpbasesize + j);
                    baseTrajs.add(rawTrajectory);
                    groundtruthset.get(queryId).add(rawTrajectory.getId());
                }
            }

            int queryTrajNumber = 40;
            // 观察stlcss的变化
            for(int j=0;j<1;j++){
                // 利用相似度查询给定轨迹的top k条轨迹， 并计算相应的精确度。
                double topk_precision_stlcss = 0;
                double topk_ndcg_stlcss = 0;
                List<Integer> querylist = new ArrayList<>(groundtruthset.keySet());

                for (int i = 0; i < queryTrajNumber; i++) {

                    // 获取待查询的轨迹数据, 每条轨迹记录其id以及相似度值 <T_id, sim>
                    RawTrajectory queryTraj = baseTrajs.get(querylist.get(i));

                    List<Pair<Integer, Double>> result = Collections.synchronizedList(new ArrayList<>());
                    int querylength = queryTraj.getGpslog().size();
                    long starttime = queryTraj.getGpslog().get(0).getTimestamp();
                    long endtime = queryTraj.getGpslog().get(querylength - 1).getTimestamp();

                    /*for (RawTrajectory t : baseTrajs) {
                        if (t.getId() == queryTraj.getId()) {
                            continue;
                        }
                        int tlength = t.getGpslog().size();
                        long ts = t.getGpslog().get(0).getTimestamp();
                        long te = t.getGpslog().get(tlength - 1).getTimestamp();
                        if (ts > endtime + temporalThreshold || te < starttime - temporalThreshold) {
                            continue;
                        }

                        double sim = STLCSS(t, queryTraj, distanceThreshold, temporalThreshold);
                        result.add(new Pair<>(t.getId(), sim));

                    }*/


                    baseTrajs.parallelStream().forEach(t-> {
                        if (t.getId() == queryTraj.getId()) {
                            return;
                        }
                        int tlength = t.getGpslog().size();
                        long ts = t.getGpslog().get(0).getTimestamp();
                        long te = t.getGpslog().get(tlength - 1).getTimestamp();
                        if (ts > endtime + temporalThreshold || te < starttime - temporalThreshold) {
                            return;
                        }

                        // double sim = STLCSS(t, queryTraj, distanceThreshold, temporalThreshold);
                        double sim = STLCSS(t, queryTraj, distanceThreshold, temporalThreshold);
                        result.add(new Pair<>(t.getId(), sim));
                    });

                    result.sort(new Comparator<Pair<Integer, Double>>() {
                        @Override
                        public int compare(Pair<Integer, Double> o1, Pair<Integer, Double> o2) {
                            if (Objects.equals(o1.getValue(), o2.getValue())) {
                                return 0;
                            } else if (o1.getValue() < o2.getValue()) {
                                return 1;
                            }
                            return -1;
                        }
                    });

                    double topk_gain = 0;
                    double idcg = 0;
                    double dcg = 0;

                    for (int m = 0; m < k && m < result.size(); m++) {
                        Pair<Integer, Double> p = result.get(m);
                        idcg = idcg + 2 / (Math.log(m + 2) / Math.log(2));
                        if (groundtruthset.get(querylist.get(i)).contains(p.getKey())) {
                            topk_gain += 1;
                            dcg = dcg + 2 / (Math.log(m + 2) / Math.log(2));
                        } else {
                            dcg = dcg + 1 / (Math.log(m + 2) / Math.log(2));
                        }
                    }
                    topk_precision_stlcss += topk_gain / k;
                    topk_ndcg_stlcss += dcg / idcg;
                }
                topk_precision_stlcss /= queryTrajNumber;
                topk_ndcg_stlcss /= queryTrajNumber;
                logger.info(String.format("Varying time shifting, top %d precision of STLCSS : %f, Top %d NDCG of STLCSS : %f", k, topk_precision_stlcss, k, topk_ndcg_stlcss));
            }

            // 观察stlc的变化
            for(int j=0;j<1;j++){
                // 利用相似度查询给定轨迹的top k条轨迹， 并计算相应的精确度。
                double topk_precision_stlc = 0;
                double topk_ndcg_stlc = 0;
                List<Integer> querylist = new ArrayList<>(groundtruthset.keySet());
                //int queryTrajNumber = 50;

                for (int i = 0; i < queryTrajNumber; i++) {

                    // 获取待查询的轨迹数据, 每条轨迹记录其id以及相似度值 <T_id, sim>
                    RawTrajectory queryTraj = baseTrajs.get(querylist.get(i));

                    List<Pair<Integer, Double>> result = Collections.synchronizedList(new ArrayList<>());
                    int querylength = queryTraj.getGpslog().size();
                    long starttime = queryTraj.getGpslog().get(0).getTimestamp();
                    long endtime = queryTraj.getGpslog().get(querylength - 1).getTimestamp();

                    /*for (RawTrajectory t : baseTrajs) {
                        if (t.getId() == queryTraj.getId()) {
                            continue;
                        }
                        int tlength = t.getGpslog().size();
                        long ts = t.getGpslog().get(0).getTimestamp();
                        long te = t.getGpslog().get(tlength - 1).getTimestamp();
                        if (ts > endtime + temporalThreshold || te < starttime - temporalThreshold) {
                            continue;
                        }

                        // double sim = STLCSS(t, queryTraj, distanceThreshold, temporalThreshold);
                        double sim = STLC(t,queryTraj,0.5);
                        result.add(new Pair<>(t.getId(), sim));
                    }*/


                    baseTrajs.parallelStream().forEach(t-> {
                        if (t.getId() == queryTraj.getId()) {
                            return;
                        }
                        int tlength = t.getGpslog().size();
                        long ts = t.getGpslog().get(0).getTimestamp();
                        long te = t.getGpslog().get(tlength - 1).getTimestamp();
                        if (ts > endtime + temporalThreshold || te < starttime - temporalThreshold) {
                            return;
                        }

                        // double sim = STLCSS(t, queryTraj, distanceThreshold, temporalThreshold);
                        double sim = STLC(t,queryTraj,0.5);
                        result.add(new Pair<>(t.getId(), sim));
                    });

                    result.sort(new Comparator<Pair<Integer, Double>>() {
                        @Override
                        public int compare(Pair<Integer, Double> o1, Pair<Integer, Double> o2) {
                            if (Objects.equals(o1.getValue(), o2.getValue())) {
                                return 0;
                            } else if (o1.getValue() < o2.getValue()) {
                                return 1;
                            }
                            return -1;
                        }
                    });

                    double topk_gain = 0;
                    double idcg = 0;
                    double dcg = 0;

                    for (int m = 0; m < k && m < result.size(); m++) {
                        Pair<Integer, Double> p = result.get(m);
                        idcg = idcg + 2 / (Math.log(m + 2) / Math.log(2));
                        if (groundtruthset.get(querylist.get(i)).contains(p.getKey())) {
                            topk_gain += 1;
                            dcg = dcg + 2 / (Math.log(m + 2) / Math.log(2));
                        } else {
                            dcg = dcg + 1 / (Math.log(m + 2) / Math.log(2));
                        }
                    }
                    topk_precision_stlc += topk_gain / k;
                    topk_ndcg_stlc += dcg / idcg;
                }
                topk_precision_stlc /= queryTrajNumber;
                topk_ndcg_stlc /= queryTrajNumber;
                logger.info(String.format("Varying time shifting, top %d precision of STLC : %f, top %d NDCG of STLC : %f", k, topk_precision_stlc, k, topk_ndcg_stlc));
            }

            // 观察d-lcts的变化
            for(int j=0;j<1;j++){
                // 利用相似度查询给定轨迹的top k条轨迹， 并计算相应的精确度。
                double topk_precision_d_lcts = 0;
                double topk_ndcg_d_lcts = 0;
                List<Integer> querylist = new ArrayList<>(groundtruthset.keySet());
                //int queryTrajNumber = 50;

                for (int i = 0; i < queryTrajNumber; i++) {

                    // 获取待查询的轨迹数据, 每条轨迹记录其id以及相似度值 <T_id, sim>
                    RawTrajectory queryTraj = baseTrajs.get(querylist.get(i));

                    List<StayPoint> stayPointList = sp.extractStayPoints(queryTraj, Settings.trajectoryClusterDistanceThreshold
                            , Settings.trajectoryClusterTimeThreshold);
                    StayPointTrajectory stayPointTrajectory = new StayPointTrajectory(queryTraj.getId(),stayPointList);

                    List<Pair<Integer, Double>> result = Collections.synchronizedList(new ArrayList<>());
                    int querylength = queryTraj.getGpslog().size();
                    long starttime = queryTraj.getGpslog().get(0).getTimestamp();
                    long endtime = queryTraj.getGpslog().get(querylength - 1).getTimestamp();

                    /*for (RawTrajectory t : baseTrajs) {
                        if (t.getId() == queryTraj.getId()) {
                            continue;
                        }
                        int tlength = t.getGpslog().size();
                        long ts = t.getGpslog().get(0).getTimestamp();
                        long te = t.getGpslog().get(tlength - 1).getTimestamp();
                        if (ts > endtime + temporalThreshold || te < starttime - temporalThreshold) {
                            continue;
                        }

                        // double sim = STLCSS(t, queryTraj, distanceThreshold, temporalThreshold);
                        double sim = STLC(t,queryTraj,0.5);
                        result.add(new Pair<>(t.getId(), sim));
                    }*/


                    baseTrajs.parallelStream().forEach(t-> {
                        if (t.getId() == queryTraj.getId()) {
                            return;
                        }
                        int tlength = t.getGpslog().size();
                        long ts = t.getGpslog().get(0).getTimestamp();
                        long te = t.getGpslog().get(tlength - 1).getTimestamp();
                        if (ts > endtime + temporalThreshold || te < starttime - temporalThreshold) {
                            return;
                        }

                        List<StayPoint> tstaypoints = sp.extractStayPoints(t, Settings.trajectoryClusterDistanceThreshold
                                , Settings.trajectoryClusterTimeThreshold);

                        StayPointTrajectory tstraj = new StayPointTrajectory(t.getId(), tstaypoints);

                        double sim = d_LCTS(tstraj,stayPointTrajectory,distanceThreshold);
                        result.add(new Pair<>(t.getId(), sim));
                    });

                    result.sort(new Comparator<Pair<Integer, Double>>() {
                        @Override
                        public int compare(Pair<Integer, Double> o1, Pair<Integer, Double> o2) {
                            if (Objects.equals(o1.getValue(), o2.getValue())) {
                                return 0;
                            } else if (o1.getValue() < o2.getValue()) {
                                return 1;
                            }
                            return -1;
                        }
                    });

                    double topk_gain = 0;
                    double idcg = 0;
                    double dcg = 0;

                    for (int m = 0; m < k && m < result.size(); m++) {
                        Pair<Integer, Double> p = result.get(m);
                        idcg = idcg + 2 / (Math.log(m + 2) / Math.log(2));
                        if (groundtruthset.get(querylist.get(i)).contains(p.getKey())) {
                            topk_gain += 1;
                            dcg = dcg + 2 / (Math.log(m + 2) / Math.log(2));
                        } else {
                            dcg = dcg + 1 / (Math.log(m + 2) / Math.log(2));
                        }
                    }
                    topk_precision_d_lcts += topk_gain / k;
                    topk_ndcg_d_lcts += dcg / idcg;
                }
                topk_precision_d_lcts /= queryTrajNumber;
                topk_ndcg_d_lcts /= queryTrajNumber;
                logger.info(String.format("Varying time shifting, top %d precision of d-LCTS : %f, top %d NDCG of d_LCTS : %f", k, topk_precision_d_lcts, k, topk_ndcg_d_lcts));
            }

            // 观察lcts的变化
            for(int j=0;j<1;j++){
                // 利用相似度查询给定轨迹的top k条轨迹， 并计算相应的精确度。
                double topk_precision_lcts = 0;
                double topk_ndcg_lcts = 0;
                List<Integer> querylist = new ArrayList<>(groundtruthset.keySet());
                //int queryTrajNumber = 50;

                for (int i = 0; i < queryTrajNumber; i++) {

                    // 获取待查询的轨迹数据, 每条轨迹记录其id以及相似度值 <T_id, sim>
                    RawTrajectory queryTraj = baseTrajs.get(querylist.get(i));

                    /*List<StayPoint> stayPointList = sp.extractStayPoints(queryTraj, Settings.trajectoryClusterDistanceThreshold
                            , Settings.trajectoryClusterTimeThreshold);
                    StayPointTrajectory stayPointTrajectory = new StayPointTrajectory(queryTraj.getId(),stayPointList);
*/

                    MappedTrajectory queryMappedTraj = sp.extract(queryTraj,Settings.trajectoryClusterDistanceThreshold
                            , Settings.trajectoryClusterTimeThreshold,Settings.radius / 1000);


                    List<Pair<Integer, Double>> result = Collections.synchronizedList(new ArrayList<>());
                    int querylength = queryTraj.getGpslog().size();
                    long starttime = queryTraj.getGpslog().get(0).getTimestamp();
                    long endtime = queryTraj.getGpslog().get(querylength - 1).getTimestamp();

                    /*for (RawTrajectory t : baseTrajs) {
                        if (t.getId() == queryTraj.getId()) {
                            continue;
                        }
                        int tlength = t.getGpslog().size();
                        long ts = t.getGpslog().get(0).getTimestamp();
                        long te = t.getGpslog().get(tlength - 1).getTimestamp();
                        if (ts > endtime + temporalThreshold || te < starttime - temporalThreshold) {
                            continue;
                        }

                        // double sim = STLCSS(t, queryTraj, distanceThreshold, temporalThreshold);
                        double sim = STLC(t,queryTraj,0.5);
                        result.add(new Pair<>(t.getId(), sim));
                    }*/

                    baseTrajs.parallelStream().forEach(t-> {
                        if (t.getId() == queryTraj.getId()) {
                            return;
                        }
                        int tlength = t.getGpslog().size();
                        long ts = t.getGpslog().get(0).getTimestamp();
                        long te = t.getGpslog().get(tlength - 1).getTimestamp();
                        if (ts > endtime + temporalThreshold || te < starttime - temporalThreshold) {
                            return;
                        }

                        MappedTrajectory tMappedTraj = sp.extract(t, Settings.trajectoryClusterDistanceThreshold
                                , Settings.trajectoryClusterTimeThreshold,Settings.radius / 1000);

                        double sim = LCTS(queryMappedTraj,tMappedTraj);
                        result.add(new Pair<>(t.getId(), sim));
                    });

                    result.sort(new Comparator<Pair<Integer, Double>>() {
                        @Override
                        public int compare(Pair<Integer, Double> o1, Pair<Integer, Double> o2) {
                            if (Objects.equals(o1.getValue(), o2.getValue())) {
                                return 0;
                            } else if (o1.getValue() < o2.getValue()) {
                                return 1;
                            }
                            return -1;
                        }
                    });

                    double topk_gain = 0;
                    double idcg = 0;
                    double dcg = 0;

                    for (int m = 0; m < k && m < result.size(); m++) {
                        Pair<Integer, Double> p = result.get(m);
                        idcg = idcg + 2 / (Math.log(m + 2) / Math.log(2));
                        if (groundtruthset.get(querylist.get(i)).contains(p.getKey())) {
                            topk_gain += 1;
                            dcg = dcg + 2 / (Math.log(m + 2) / Math.log(2));
                        } else {
                            dcg = dcg + 1 / (Math.log(m + 2) / Math.log(2));
                        }
                    }
                    topk_precision_lcts += topk_gain / k;
                    topk_ndcg_lcts += dcg / idcg;
                }
                topk_precision_lcts /= queryTrajNumber;
                topk_ndcg_lcts /= queryTrajNumber;
                logger.info(String.format("Varying time shifting, top %d precision of LCTS : %f, top %d NDCG of d_LCTS : %f", k, topk_precision_lcts, k, topk_ndcg_lcts));
            }


        }




    }

    public static boolean stOverlap(RawTrajectory t1, RawTrajectory query, double distanceThreshold, long temporalThreshold){
        for(Point p1:t1.getGpslog()){
            for(Point p2:query.getGpslog()){
                if(TrajUtil.distanceOfPoints(p1,p2)<=distanceThreshold
                &&Math.abs(p1.getTimestamp()-p2.getTimestamp())<=temporalThreshold){
                    return true;
                }
            }
        }
        return false;
    }

}
