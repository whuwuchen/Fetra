package whu.edu.totemdb.STCSim.TestCase.PBTI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import whu.edu.totemdb.STCSim.Base.*;
import whu.edu.totemdb.STCSim.Device.Server;
import whu.edu.totemdb.STCSim.MobileFederation.Device_v2.Footprint;
import whu.edu.totemdb.STCSim.MobileFederation.Device_v2.GTI;
import whu.edu.totemdb.STCSim.MobileFederation.Device_v2.PTI;
import whu.edu.totemdb.STCSim.Settings;
import whu.edu.totemdb.STCSim.StayPointDetection.SPExtrator;
import whu.edu.totemdb.STCSim.Utils.DataLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class GlobalIndexBuildTest {

    public static Log logger = LogFactory.getLog(GlobalIndexBuildTest.class);

    public static void main(String[] args) throws InterruptedException {
        // testOfBeijing();
        testNewYork();
    }

    public static void testOfBeijing() throws InterruptedException {
        int deviceNumber = 1000;
        int rawtrajsPerDevice = 100;

        int defaultDeviceNumber = 10000;
        int defaultTrajPerDevice = 100;

        int[] deviceNumbers =  {100,1000,10000,100000,200000};
        int[] TrajPerDevices = {100,200,300,400,500};

        double[] gridWidths = {0.001,0.003,0.005,0.007,0.009};


        List<POI> pois = DataLoader.loadPOIDataBeiJing();
        List<RawTrajectory> rawTrajectories = DataLoader.loadRawTrajDataBeiJing();
        Server s = new Server(pois);
        SPExtrator sp = new SPExtrator(s.getGlobalGridIndex(), pois);
        List<MappedTrajectory> mappedTrajectories = sp.extractBatch(rawTrajectories,
                Settings.trajectoryClusterDistanceThreshold,
                Settings.trajectoryClusterTimeThreshold,Settings.radius/1000);
        mappedTrajectories = filterTrajs(mappedTrajectories,1);

        Range range = new Range(Settings.minLatBeiJing,Settings.maxLatBeiJing,
                Settings.minLonBeiJing, Settings.maxLonBeiJing,Settings.latGridWidth, Settings.lonGridWidth);

/*
        deviceNumber = 1000000;
        long before = System.currentTimeMillis();
        Map<Integer, List<MappedTrajectory>> trajs = initTrajs(mappedTrajectories,deviceNumber,defaultTrajPerDevice);
        GTI gti = buildPTI(trajs,range);
        long end = System.currentTimeMillis();
        logger.info(String.format("Build time : %2f when |S| = %d",((double)end-before)/1000,deviceNumber));
*/

        for(int i=0;i<deviceNumbers.length;i++){
            deviceNumber = deviceNumbers[i];
            Map<Integer, List<MappedTrajectory>> trajs = initTrajs(mappedTrajectories,deviceNumber,defaultTrajPerDevice);
            // System.out.println(trajs.size());
            long before = System.currentTimeMillis();
            //GTI gti = buildIntervalTree(trajs,range);
            GTI gti = buildPTI(trajs,range);
            long end = System.currentTimeMillis();
            logger.info(String.format("Build time : %2f when |S| = %d",((double)end-before)/1000,deviceNumber));
        }



    }

    public static void testNewYork() throws InterruptedException {
        int deviceNumber = 1000;
        int rawtrajsPerDevice = 100;

        int defaultDeviceNumber = 10000;
        int defaultTrajPerDevice = 100;

        int[] deviceNumbers =  {100,100,10000,50000,100000};
        int[] TrajPerDevices = {100,200,300,400,500};

        double[] gridWidths = {0.001,0.003,0.005,0.007,0.009};

        List<POI> pois = new ArrayList<>();

        List<MappedTrajectory> mappedTrajectories = new ArrayList<>();
        DataLoader.readTrajFileNewYork(mappedTrajectories,pois);

        Range range = new Range(Settings.minLatNewYork,Settings.maxLatNewYork,
                Settings.minLonNewYork, Settings.maxLonNewYork,Settings.latGridWidth, Settings.lonGridWidth);

        for(int i=0;i<deviceNumbers.length;i++){
            deviceNumber = deviceNumbers[i];
            Map<Integer, List<MappedTrajectory>> trajs = initTrajs(mappedTrajectories,deviceNumber,defaultTrajPerDevice);
            // System.out.println(trajs.size());
            long before = System.currentTimeMillis();
            //GTI gti = buildIntervalTree(trajs,range);
            GTI gti = buildPTI(trajs,range);
            long end = System.currentTimeMillis();
            logger.info(String.format("Build time : %2f when |S| = %d",((double)end-before)/1000,deviceNumber));
        }



    }

    public static void testTokyo(){

    }


    // <deviceid, List<MappedTrajectory>>
    public static Map<Integer, List<MappedTrajectory>> initTrajs(List<MappedTrajectory> mappedTrajectoryList, int deviceNumber,int trajPerDevice) throws InterruptedException {
        Map<Integer, List<MappedTrajectory>> trajs = new ConcurrentHashMap<>();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        for(int i=0;i<deviceNumber;i++){
            int finalI = i;
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    trajs.put(finalI,generateTrajs(mappedTrajectoryList,finalI,trajPerDevice));
                }
            };
            pool.execute(runnable);
        }
        pool.shutdown();
        pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

        return trajs;
    }

    public static List<MappedTrajectory> generateTrajs(List<MappedTrajectory> mappedTrajectoryList, int deviceid,int trajPerDevice){
        List<MappedTrajectory> result = new ArrayList<>();
        int size = mappedTrajectoryList.size();
        for(int j=0;j<trajPerDevice;j++){
            int trajid = ((deviceid+1)*j)%size;
            result.add(mappedTrajectoryList.get(trajid));
        }

        return result;
    }

    public static List<MappedTrajectory> filterTrajs(List<MappedTrajectory> mappedTrajectories, int sizeThreshold){
        return mappedTrajectories.parallelStream().filter(t->t.getHops().size()>=sizeThreshold).collect(Collectors.toList());
    }

    public static void sendIndex(Map<Integer, List<MappedTrajectory>> trajs){

    }

    public static GTI buildPTI(Map<Integer, List<MappedTrajectory>> trajs, Range range){
        GTI gti = new GTI(1);
        PTI.setDayLengthAndLevel(86400, 10);
        trajs.entrySet().parallelStream().forEach(
                entry->{
                    addDeviceFootprints(gti, entry.getKey() ,entry.getValue(),range);
                }
        );
        return gti;
    }

    public static GTI buildIntervalTree(Map<Integer, List<MappedTrajectory>> trajs, Range range){
        GTI gti = new GTI(0);
        // PTI.setDayLengthAndLevel(86400, 10);
        trajs.entrySet().parallelStream().forEach(
                entry->{
                    addDeviceFootprints(gti, entry.getKey() ,entry.getValue(),range);
                }
        );
        return gti;
    }

    public static void addDeviceFootprints(GTI gti, int deviceid, List<MappedTrajectory> trajs, Range range){
        /*trajs.parallelStream().forEach(t->{
            List<Hop> hops = t.getHops();
            for(Hop h:hops){
                int grid = range.calculateGridId(h.getPoi().getLat(),h.getPoi().getLon());
                gti.addFootprint(new Footprint(deviceid,grid, (int) h.getStarttime(), (int) h.getEndtime()));
            }
        });*/

        trajs.parallelStream().forEach(t->{
            List<Hop> hops = t.getHops();
            for(Hop h:hops){
                int grid = range.calculateGridId(h.getPoi().getLat(),h.getPoi().getLon());
                gti.addFootprint(new Footprint(deviceid,grid, (int) h.getStarttime(), (int) h.getEndtime()));
            }
        });

    }


}
