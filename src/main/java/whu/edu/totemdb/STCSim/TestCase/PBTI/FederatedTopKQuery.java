package whu.edu.totemdb.STCSim.TestCase.PBTI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import whu.edu.totemdb.STCSim.Base.Hop;
import whu.edu.totemdb.STCSim.Base.MappedTrajectory;
import whu.edu.totemdb.STCSim.Base.POI;
import whu.edu.totemdb.STCSim.Base.Range;
import whu.edu.totemdb.STCSim.MobileFederation.Device_v2.Footprint;
import whu.edu.totemdb.STCSim.MobileFederation.Device_v2.GTI;
import whu.edu.totemdb.STCSim.MobileFederation.Device_v2.PTI;
import whu.edu.totemdb.STCSim.Settings;
import whu.edu.totemdb.STCSim.TestCase.FederatedTopKQueryTest;
import whu.edu.totemdb.STCSim.Utils.DataLoader;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FederatedTopKQuery {
    public static Log logger = LogFactory.getLog(FederatedTopKQuery.class);
    public static void main(String[] args) throws InterruptedException {
        testNewYork();
    }

    public static void testNewYork() throws InterruptedException {
        int deviceNumber = 100000;
        int rawtrajsPerDevice = 100;

        int defaultDeviceNumber = 600;
        int defaultTrajPerDevice = 100;

        int[] deviceNumbers =  {100,1000,10000,100000,100000};
        int[] TrajPerDevices = {100,200,300,400,500};
        double[] gridWidths = {0.001,0.003,0.005,0.007,0.009};
        double[] ratioOfQ = {0.001, 0.002, 0.003, 0.004, 0.005};
        int[] parameterK = {10, 20, 30, 40, 50};

        List<POI> pois = new ArrayList<>();

        List<MappedTrajectory> mappedTrajectories = new ArrayList<>();
        DataLoader.readTrajFileNewYork(mappedTrajectories,pois);

        Range range = new Range(Settings.minLatNewYork,Settings.maxLatNewYork,
                Settings.minLonNewYork, Settings.maxLonNewYork,Settings.latGridWidth, Settings.lonGridWidth);

        Map<Integer, List<MappedTrajectory>> trajs = initTrajs(mappedTrajectories,deviceNumber,defaultTrajPerDevice);
        // System.out.println(trajs.size());

        //GTI gti = buildPTI(trajs,range);
        GTI gti = buildIntervalTree(trajs,range);
        logger.info("Build federated index");
        for(int i=0;i<ratioOfQ.length;i++){
            int Qsize = (int) (ratioOfQ[i]*mappedTrajectories.size());
            List<MappedTrajectory> queryDataset = generateQueryData(mappedTrajectories,Qsize);
            long before = System.currentTimeMillis();
            Map<Integer, List<Hop>> queryData = filter(gti,queryDataset,range);
            long end = System.currentTimeMillis();
            logger.info(String.format("Filter time : %2f when |Q| = %f",((double)end-before)/1000,ratioOfQ[i]));

        }



    }

    public static Map<Integer, List<Hop>> filter(GTI gti, List<MappedTrajectory> queryDataset, Range range){

        Set<Integer> candidate = new HashSet<>();
        Map<Integer, List<Hop>> queryData = new ConcurrentHashMap<>();
        queryDataset.parallelStream().forEach(t->{
            List<Hop> hops = t.getHops();
            hops.parallelStream().forEach(hop->{
                int id = range.calculateGridId(hop.getPoi().getLat(),hop.getPoi().getLon());
                Set<Integer> res = gti.gridTemporalQuery(id, (int) hop.getStarttime(), (int) hop.getEndtime());
                synchronized (candidate){
                    candidate.addAll(res);
                }
                for(Integer i : res){
                    if(queryData.containsKey(i)){
                        queryData.get(i).add(hop);
                    }
                    else{
                        List<Hop> tmp = new ArrayList<>();
                        tmp.add(hop);
                        queryData.put(i,tmp);
                    }
                }
            });

        });
        return queryData;
    }

    public static List<MappedTrajectory> generateQueryData(List<MappedTrajectory> mappedTrajectoryList, int Qsize){
        List<MappedTrajectory> mappedTrajectories = new ArrayList<>();
        int size = mappedTrajectoryList.size();
        for(int i=0;i<Qsize;i++){
            int id = ((i+1)*(Qsize))%size;
            mappedTrajectories.add(mappedTrajectoryList.get(id));
        }


        return mappedTrajectories;
    }

    public static Map<Integer, List<MappedTrajectory>> initTrajs(List<MappedTrajectory> mappedTrajectoryList, int deviceNumber, int trajPerDevice) throws InterruptedException {
        Map<Integer, List<MappedTrajectory>> trajs = new ConcurrentHashMap<>();
        ExecutorService pool = Executors.newFixedThreadPool(20);
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

    public static void computationWithoutFiltering(Map<Integer, List<MappedTrajectory>> allTrajs, List<MappedTrajectory> queryDataset){


    }

    public static void computationWithFiltering(Map<Integer, List<MappedTrajectory>> allTrajs, Map<Integer, List<Hop>> queryData){

    }

    public static void communicationTotalCost(Map<Integer, List<MappedTrajectory>> allTrajs){

    }

    public static void communicationSingleCost(List<MappedTrajectory> trajs){


    }


}
