package whu.edu.totemdb.STCSim.TestCase;

import com.sun.net.httpserver.HttpServer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import whu.edu.totemdb.STCSim.Base.*;
import whu.edu.totemdb.STCSim.Device.Handlers.GridIndexHandler;
import whu.edu.totemdb.STCSim.Device.Handlers.TopKQueryHandler;
import whu.edu.totemdb.STCSim.Device.MobileDevice;
import whu.edu.totemdb.STCSim.Device.Server;
import whu.edu.totemdb.STCSim.Index.GridIndex;
import whu.edu.totemdb.STCSim.Settings;
import whu.edu.totemdb.STCSim.StayPointDetection.SPExtrator;
import whu.edu.totemdb.STCSim.Utils.DataLoader;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

// consistent grid index query
// In this query, server and mobiles have same grid index structure
public class FederatedTopKQueryTest {

    public static Log logger = LogFactory.getLog(FederatedTopKQueryTest.class);
    public static void main(String[] args) throws IOException {

        //topKQueryTestOfBeijing();
        topKQueryTestOfNewYork();
    }

    /*public static void initServer() throws IOException {
        pois = DataLoader.loadPOIDataBeiJing();

        globalGridIndex = new GridIndex(Settings.maxLonBeiJing,Settings.minLonBeiJing,
                Settings.maxLatBeiJing, Settings.minLatBeiJing,Settings.latGridWidth, Settings.lonGridWidth);
        globalGridIndex.init(new ArrayList<>(pois));
        s = new Server(pois);
        GridIndexHandler gridIndexHandler = new GridIndexHandler(s);
        TopKQueryHandler topKQueryHandler = new TopKQueryHandler(s,false);
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(9090), 0);
        httpServer.createContext("/doGridIndexBuilding", gridIndexHandler);
        httpServer.createContext("/doTopKQuery",topKQueryHandler);
        httpServer.setExecutor(Executors.newFixedThreadPool(1));
        httpServer.start();

        for(MobileDevice d:devices){
            d.matchRawTrajectory(Settings.radius/1000);
            d.indexMappedTraj();
            d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
        }
        double indexBuildTime = gridIndexHandler.getBuildTime();
        logger.info(String.format("Global index build time is %2f",indexBuildTime));



    }*/

    /*public static void initMobileDevices(int deviceAmount,GridIndex globalGridIndex,List<POI> globalPOIs){

        int hsize = globalGridIndex.getIndexHorizontalSize();
        int vsize = globalGridIndex.getIndexVerticalSize();
        int maxHLength = (int) (0.4*hsize);
        int maxVLength = (int) (0.4*vsize);
        for(int i=0;i<deviceAmount;i++){
            // generate the active range
            int hStartIndex = (int) (Math.random()*(hsize-maxHLength));
            int hLength = (int) (Math.random()*(maxHLength/2)+maxHLength/2);
            int vStartIndex = (int) (Math.random()*(vsize-maxVLength));
            int vLength = (int) (Math.random()*(maxVLength/2)+maxVLength/2);
            List<POI> localPOIs = new ArrayList<>();
            int leftDownGridId = vStartIndex*hsize+hStartIndex;
            int rightUpGridId = (vStartIndex+vLength-1)*hsize+hStartIndex+hLength-1;
            Range lDR = globalGridIndex.calculatedGridRange(leftDownGridId);
            Range rUR = globalGridIndex.calculatedGridRange(rightUpGridId);
            // Range activeRange = new Range(lDR.getlatMin(), rUR.getlatMax(), lDR.getlonMin(), rUR.getlonMax(), globalGridIndex.getLatGridWidth(), globalGridIndex.getLonGridWidth());
            Range activeRange = new Range( Settings.minLatBeiJing,Settings.maxLatBeiJing,Settings.minLonBeiJing,Settings.maxLonBeiJing,
                    Settings.latGridWidth, Settings.lonGridWidth);
            for(int j=hStartIndex;j<hStartIndex+hLength;j++){
                for(int k=vStartIndex;k<vStartIndex+vLength;k++){
                    int gridId = k*hsize+j;
                    List<BasePoint> tmp = globalGridIndex.getPoiMap().get(String.valueOf(gridId));
                    if(null==tmp){
                        continue;
                    }
                    for(BasePoint bp:tmp){
                        localPOIs.add((POI) bp);
                    }

                }
            }
            MobileDevice device = new MobileDevice(String.valueOf(i),activeRange,localPOIs,globalTg);
            //MobileDevice device = new MobileDevice(String.valueOf(i),activeRange,localPOIs,null);
            int spnum = Settings.windowSize/Settings.maxStayTime;
            device.generateStayPoint(spnum);
            devices.add(device);

        }


    }*/


   /* public static void topKQueryTestOfBeijing() throws IOException {

        int deviceNumber = 1000;
        List<POI> pois = DataLoader.loadPOIDataBeiJing();
        List<RawTrajectory> rawTrajectories = DataLoader.loadRawTrajDataBeiJing();

        int rawtrajsPerDevice = 100;
        Server s = new Server(pois);
        SPExtrator sp = new SPExtrator(s.getGlobalGridIndex(),pois);

        List<MobileDevice> devices = initMobileDevices(deviceNumber,rawtrajsPerDevice,s.getGlobalGridIndex()
                                        ,sp,rawTrajectories);
        GridIndexHandler gridIndexHandler = new GridIndexHandler(s);
        TopKQueryHandler topKQueryHandler = new TopKQueryHandler(s,true);
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(9090), 0);
        httpServer.createContext("/doGridIndexBuilding", gridIndexHandler);
        httpServer.createContext("/doTopKQuery",topKQueryHandler);

        httpServer.setExecutor(Executors.newFixedThreadPool(1));
        httpServer.start();

        for(MobileDevice d:devices){
            d.matchRawTrajectory(Settings.radius/1000);
            d.indexMappedTraj();
        }
        double beforeIndexBuild = (double)System.currentTimeMillis()/1000;
        for(MobileDevice d:devices){
            d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
        }
        double endIndexBuild = (double)System.currentTimeMillis()/1000;
        logger.info(String.format("Global index build time is %2f",(endIndexBuild-beforeIndexBuild)));


        // double indexBuildTime = gridIndexHandler.getBuildTime();
        // logger.info(String.format("Global index build time is %2f",indexBuildTime));
        List<Hop> Q = devices.get(2).getMappedTrajectoryList()
                .stream().map(t->t.getHops()).flatMap(List::stream).collect(Collectors.toList());
        int k = 10;


        topKQueryHandler.topKQuery(Q,k);


        for(MobileDevice d:devices){
            d.postRequest("http://localhost:9090/doTopKQuery","begin:"+d.getDeviceId());
            d.postRequest("http://localhost:9090/doTopKQuery",d.getDeviceId()+":"+ d.getRisk());
        }

        double totalQueryTime = topKQueryHandler.getTotalQueryTime();
        double totalQueryTimeWithoutPruning = topKQueryHandler.getTotalQueryTimeWithoutPruning();
        logger.info(String.format("Total query time with pruning %2f seconds ", totalQueryTime));
        logger.info(String.format("Total query time without pruning %2f seconds ", totalQueryTimeWithoutPruning));
        httpServer.stop(0);


    }*/

    public static void topKQueryTestOfBeijing() throws IOException {

        int deviceNumber = 1000;

        List<POI> pois = DataLoader.loadPOIDataBeiJing();
        List<RawTrajectory> rawTrajectories = DataLoader.loadRawTrajDataBeiJing();

        int rawtrajsPerDevice = 100;

        int defaultDeviceNumber = 600;
        int defaultTrajPerDevice = 200;
        double defaultRatio = 0.003;
        int defaultK = 30;

        int[] deviceNumbers = {200, 400, 600, 800, 1000};
        int[] TrajPerDevices = {100, 200, 300, 400, 500};
        double[] gridWidths = {0.001, 0.003, 0.005, 0.007, 0.009};
        double[] ratioOfQ = {0.001, 0.002, 0.003, 0.004, 0.005};
        int[] parameterK = {10, 20, 30, 40, 50};

        List<Double> filterTimeWithPruningWithLocalBoostVaryingDeviceNumber = new ArrayList<>();
        List<Double> refineTimeWithPruningWithLocalBoostVaryingDeviceNumber = new ArrayList<>();
        List<Double> filterTimeWithPruningWithLocalBoostVaryingTrajNumber = new ArrayList<>();
        List<Double> refineTimeWithPruningWithLocalBoostVaryingTrajNumber = new ArrayList<>();
        List<Double> filterTimeWithPruningWithLocalBoostVaryingGridWidth = new ArrayList<>();
        List<Double> refineTimeWithPruningWithLocalBoostVaryingGridWidth = new ArrayList<>();
        List<Double> filterTimeWithPruningWithLocalBoostVaryingQ = new ArrayList<>();
        List<Double> refineTimeWithPruningWithLocalBoostVaryingQ = new ArrayList<>();
        List<Double> filterTimeWithPruningWithLocalBoostVaryingK = new ArrayList<>();
        List<Double> refineTimeWithPruningWithLocalBoostVaryingK = new ArrayList<>();


        List<Double> filterTimeWithPruningWithoutLocalBoostVaryingDeviceNumber = new ArrayList<>();
        List<Double> refineTimeWithPruningWithoutLocalBoostVaryingDeviceNumber = new ArrayList<>();
        List<Double> filterTimeWithPruningWithoutLocalBoostVaryingTrajNumber = new ArrayList<>();
        List<Double> refineTimeWithPruningWithoutLocalBoostVaryingTrajNumber = new ArrayList<>();
        List<Double> filterTimeWithPruningWithoutLocalBoostVaryingGridWidth = new ArrayList<>();
        List<Double> refineTimeWithPruningWithoutLocalBoostVaryingGridWidth = new ArrayList<>();
        List<Double> filterTimeWithPruningWithoutLocalBoostVaryingQ = new ArrayList<>();
        List<Double> refineTimeWithPruningWithoutLocalBoostVaryingQ = new ArrayList<>();
        List<Double> filterTimeWithPruningWithoutLocalBoostVaryingK = new ArrayList<>();
        List<Double> refineTimeWithPruningWithoutLocalBoostVaryingK = new ArrayList<>();


        List<Double> queryTimeWithoutPruningWithoutLocalBoostVaryingDeviceNumber = new ArrayList<>();
        List<Double> queryTimeWithoutPruningWithoutLocalBoostVaryingTrajNumber = new ArrayList<>();
        List<Double> queryTimeWithoutPruningWithoutLocalBoostVaryingGridWidth = new ArrayList<>();
        List<Double> queryTimeWithoutPruningWithoutLocalBoostVaryingQ = new ArrayList<>();
        List<Double> queryTimeWithoutPruningWithoutLocalBoostVaryingK = new ArrayList<>();


        List<Double> queryTimeWithoutPruningWithLocalBoostVaryingDeviceNumber = new ArrayList<>();
        List<Double> queryTimeWithoutPruningWithLocalBoostVaryingTrajNumber = new ArrayList<>();
        List<Double> queryTimeWithoutPruningWithLocalBoostVaryingGridWidth = new ArrayList<>();
        List<Double> queryTimeWithoutPruningWithLocalBoostVaryingQ = new ArrayList<>();
        List<Double> queryTimeWithoutPruningWithLocalBoostVaryingK = new ArrayList<>();
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(9090), 0);
        Server s = new Server(pois);
        GridIndexHandler gridIndexHandler = new GridIndexHandler(s, false);
        TopKQueryHandler topKQueryHandler = new TopKQueryHandler(s, true);

        httpServer.createContext("/doGridIndexBuilding", gridIndexHandler);
        httpServer.createContext("/doTopKQuery", topKQueryHandler);
        httpServer.setExecutor(Executors.newFixedThreadPool(1));
        httpServer.start();

        // 每种参数组合跑10轮，取均值

        // 不剪枝，移动端不加速
        // 1. 设备数量变化
        for(int i=0;i<deviceNumbers.length;i++){
            int device_number = deviceNumbers[i];

            SPExtrator sp = new SPExtrator(s.getGlobalGridIndex(), pois);
            double totalQueryTime = 0;
            for(int j=0;j<10;j++) {
                List<MobileDevice> devices = initMobileDevices(device_number, defaultTrajPerDevice, s.getGlobalGridIndex()
                        , sp, rawTrajectories, false, false);

                for (MobileDevice d : devices) {
                    d.matchRawTrajectories(Settings.radius / 1000);
                    d.indexMappedTraj();
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }

                List<Hop> Q = extractQuery(rawTrajectories, sp, defaultRatio);
                topKQueryHandler.topKQuery(Q,defaultK);
                double beforeQuery = (double) System.currentTimeMillis() / 1000;
                for (MobileDevice d : devices) {
                    d.postRequest("http://localhost:9090/doTopKQuery", "begin:" + d.getDeviceId());
                    d.postRequest("http://localhost:9090/doTopKQuery", d.getDeviceId() + ":" + d.getRisk());
                }
                List<Map.Entry<String,Double>> result = topKQueryHandler.getAllResults()
                        .entrySet().stream().sorted(Comparator.comparing(Map.Entry::getValue)).collect(Collectors.toList());
                double endQuery = (double) System.currentTimeMillis() / 1000;
                topKQueryHandler.reset();
                gridIndexHandler.reset(false);
                totalQueryTime += endQuery - beforeQuery;
            }

            queryTimeWithoutPruningWithoutLocalBoostVaryingDeviceNumber.add(totalQueryTime/10);
            logger.info(String.format("Total query time %2f seconds ", totalQueryTime/10));


        }

        // 2. 移动端轨迹数量变化
        for(int i=0;i<TrajPerDevices.length;i++){
            int traj_number = TrajPerDevices[i];

            SPExtrator sp = new SPExtrator(s.getGlobalGridIndex(), pois);
            double totalQueryTime = 0;
            for(int j=0;j<10;j++) {
                List<MobileDevice> devices = initMobileDevices(defaultDeviceNumber, traj_number, s.getGlobalGridIndex()
                        , sp, rawTrajectories, false, false);

                for (MobileDevice d : devices) {
                    d.matchRawTrajectories(Settings.radius / 1000);
                    d.indexMappedTraj();
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }

                List<Hop> Q = extractQuery(rawTrajectories, sp, defaultRatio);
                topKQueryHandler.topKQuery(Q,defaultK);
                double beforeQuery = (double) System.currentTimeMillis() / 1000;
                for (MobileDevice d : devices) {
                    d.postRequest("http://localhost:9090/doTopKQuery", "begin:" + d.getDeviceId());
                    d.postRequest("http://localhost:9090/doTopKQuery", d.getDeviceId() + ":" + d.getRisk());
                }
                List<Map.Entry<String,Double>> result = topKQueryHandler.getAllResults()
                        .entrySet().stream().sorted(Comparator.comparing(Map.Entry::getValue)).collect(Collectors.toList());
                double endQuery = (double) System.currentTimeMillis() / 1000;
                topKQueryHandler.reset();
                gridIndexHandler.reset(false);
                totalQueryTime += endQuery - beforeQuery;
            }

            queryTimeWithoutPruningWithoutLocalBoostVaryingTrajNumber.add(totalQueryTime/10);
            logger.info(String.format("Total query time %2f seconds ", totalQueryTime/10));
        }

        // 3. 网格宽度变化
        for(int i=0;i<gridWidths.length;i++){
            double grid_width = gridWidths[i];
            s.reset(grid_width);
            SPExtrator sp = new SPExtrator(s.getGlobalGridIndex(), pois);
            double totalQueryTime = 0;
            for(int j=0;j<10;j++) {
                List<MobileDevice> devices = initMobileDevices(defaultDeviceNumber, defaultTrajPerDevice, s.getGlobalGridIndex()
                        , sp, rawTrajectories, false, false);
                for (MobileDevice d : devices) {
                    d.matchRawTrajectories(Settings.radius / 1000);
                    d.indexMappedTraj();
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }

                List<Hop> Q = extractQuery(rawTrajectories, sp, defaultRatio);
                topKQueryHandler.topKQuery(Q,defaultK);
                double beforeQuery = (double) System.currentTimeMillis() / 1000;
                for (MobileDevice d : devices) {
                    d.postRequest("http://localhost:9090/doTopKQuery", "begin:" + d.getDeviceId());
                    d.postRequest("http://localhost:9090/doTopKQuery", d.getDeviceId() + ":" + d.getRisk());
                }
                List<Map.Entry<String,Double>> result = topKQueryHandler.getAllResults()
                        .entrySet().stream().sorted(Comparator.comparing(Map.Entry::getValue)).collect(Collectors.toList());
                double endQuery = (double) System.currentTimeMillis() / 1000;
                topKQueryHandler.reset();
                gridIndexHandler.reset(false);
                totalQueryTime += endQuery - beforeQuery;
            }

            queryTimeWithoutPruningWithoutLocalBoostVaryingGridWidth.add(totalQueryTime/10);
            logger.info(String.format("Total query time %2f seconds ", totalQueryTime/10));
        }

        s.reset(Settings.latGridWidth);

        // 4. Q变化
        for(int i=0;i<ratioOfQ.length;i++){
            double ratio_q = ratioOfQ[i];

            SPExtrator sp = new SPExtrator(s.getGlobalGridIndex(), pois);
            double totalQueryTime = 0;
            for(int j=0;j<10;j++) {
                List<MobileDevice> devices = initMobileDevices(defaultDeviceNumber, defaultTrajPerDevice, s.getGlobalGridIndex()
                        , sp, rawTrajectories, false, false);

                for (MobileDevice d : devices) {
                    d.matchRawTrajectories(Settings.radius / 1000);
                    d.indexMappedTraj();
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }

                List<Hop> Q = extractQuery(rawTrajectories, sp, ratio_q);
                topKQueryHandler.topKQuery(Q,defaultK);
                double beforeQuery = (double) System.currentTimeMillis() / 1000;
                for (MobileDevice d : devices) {
                    d.postRequest("http://localhost:9090/doTopKQuery", "begin:" + d.getDeviceId());
                    d.postRequest("http://localhost:9090/doTopKQuery", d.getDeviceId() + ":" + d.getRisk());
                }
                List<Map.Entry<String,Double>> result = topKQueryHandler.getAllResults()
                        .entrySet().stream().sorted(Comparator.comparing(Map.Entry::getValue)).collect(Collectors.toList());
                double endQuery = (double) System.currentTimeMillis() / 1000;
                topKQueryHandler.reset();
                gridIndexHandler.reset(false);
                totalQueryTime += endQuery - beforeQuery;
            }

            queryTimeWithoutPruningWithoutLocalBoostVaryingQ.add(totalQueryTime/10);
            logger.info(String.format("Total query time %2f seconds ", totalQueryTime/10));
        }

        // 5. K变化
        for(int i=0;i<parameterK.length;i++){
            int k = parameterK[i];

            SPExtrator sp = new SPExtrator(s.getGlobalGridIndex(), pois);
            double totalQueryTime = 0;
            for(int j=0;j<10;j++) {
                List<MobileDevice> devices = initMobileDevices(defaultDeviceNumber, defaultTrajPerDevice, s.getGlobalGridIndex()
                        , sp, rawTrajectories, false, false);

                for (MobileDevice d : devices) {
                    d.matchRawTrajectories(Settings.radius / 1000);
                    d.indexMappedTraj();
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }

                List<Hop> Q = extractQuery(rawTrajectories, sp, defaultRatio);
                topKQueryHandler.topKQuery(Q,k);
                double beforeQuery = (double) System.currentTimeMillis() / 1000;
                for (MobileDevice d : devices) {
                    d.postRequest("http://localhost:9090/doTopKQuery", "begin:" + d.getDeviceId());
                    d.postRequest("http://localhost:9090/doTopKQuery", d.getDeviceId() + ":" + d.getRisk());
                }
                List<Map.Entry<String,Double>> result = topKQueryHandler.getAllResults()
                        .entrySet().stream().sorted(Comparator.comparing(Map.Entry::getValue)).collect(Collectors.toList());
                double endQuery = (double) System.currentTimeMillis() / 1000;
                topKQueryHandler.reset();
                gridIndexHandler.reset(false);
                totalQueryTime += endQuery - beforeQuery;
            }

            queryTimeWithoutPruningWithoutLocalBoostVaryingK.add(totalQueryTime/10);
            logger.info(String.format("Total query time %2f seconds ", totalQueryTime/10));
        }


        //不剪枝，移动端加速
        // 1. 设备数量变化
        for(int i=0;i<deviceNumbers.length;i++){
            int device_number = deviceNumbers[i];

            SPExtrator sp = new SPExtrator(s.getGlobalGridIndex(), pois);
            double totalQueryTime = 0;
            for(int j=0;j<10;j++) {
                List<MobileDevice> devices = initMobileDevices(device_number, defaultTrajPerDevice, s.getGlobalGridIndex()
                        , sp, rawTrajectories, true, false);
                for (MobileDevice d : devices) {
                    d.matchRawTrajectories(Settings.radius / 1000);
                    d.indexMappedTraj();
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }

                List<Hop> Q = extractQuery(rawTrajectories, sp, defaultRatio);
                topKQueryHandler.topKQuery(Q,defaultK);
                double beforeQuery = (double) System.currentTimeMillis() / 1000;
                for (MobileDevice d : devices) {
                    d.postRequest("http://localhost:9090/doTopKQuery", "begin:" + d.getDeviceId());
                    d.postRequest("http://localhost:9090/doTopKQuery", d.getDeviceId() + ":" + d.getRisk());
                }
                List<Map.Entry<String,Double>> result = topKQueryHandler.getAllResults()
                        .entrySet().stream().sorted(Comparator.comparing(Map.Entry::getValue)).collect(Collectors.toList());
                double endQuery = (double) System.currentTimeMillis() / 1000;
                topKQueryHandler.reset();
                gridIndexHandler.reset(false);
                totalQueryTime += endQuery - beforeQuery;
            }

            queryTimeWithoutPruningWithLocalBoostVaryingDeviceNumber.add(totalQueryTime/10);
            logger.info(String.format("Total query time %2f seconds ", totalQueryTime/10));


        }

        // 2. 移动端轨迹数量变化
        for(int i=0;i<TrajPerDevices.length;i++){
            int traj_number = TrajPerDevices[i];

            SPExtrator sp = new SPExtrator(s.getGlobalGridIndex(), pois);
            double totalQueryTime = 0;
            for(int j=0;j<10;j++) {
                List<MobileDevice> devices = initMobileDevices(defaultDeviceNumber, traj_number, s.getGlobalGridIndex()
                        , sp, rawTrajectories, true, false);
                for (MobileDevice d : devices) {
                    d.matchRawTrajectories(Settings.radius / 1000);
                    d.indexMappedTraj();
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }

                List<Hop> Q = extractQuery(rawTrajectories, sp, defaultRatio);
                topKQueryHandler.topKQuery(Q,defaultK);
                double beforeQuery = (double) System.currentTimeMillis() / 1000;
                for (MobileDevice d : devices) {
                    d.postRequest("http://localhost:9090/doTopKQuery", "begin:" + d.getDeviceId());
                    d.postRequest("http://localhost:9090/doTopKQuery", d.getDeviceId() + ":" + d.getRisk());
                }
                List<Map.Entry<String,Double>> result = topKQueryHandler.getAllResults()
                        .entrySet().stream().sorted(Comparator.comparing(Map.Entry::getValue)).collect(Collectors.toList());
                double endQuery = (double) System.currentTimeMillis() / 1000;
                topKQueryHandler.reset();
                gridIndexHandler.reset(false);
                totalQueryTime += endQuery - beforeQuery;
            }

            queryTimeWithoutPruningWithLocalBoostVaryingTrajNumber.add(totalQueryTime/10);
            logger.info(String.format("Total query time %2f seconds ", totalQueryTime/10));
        }

        // 3. 网格宽度变化
        for(int i=0;i<gridWidths.length;i++){
            double grid_width = gridWidths[i];

            s.reset(grid_width);
            SPExtrator sp = new SPExtrator(s.getGlobalGridIndex(), pois);
            double totalQueryTime = 0;
            for(int j=0;j<10;j++) {
                List<MobileDevice> devices = initMobileDevices(defaultDeviceNumber, defaultTrajPerDevice, s.getGlobalGridIndex()
                        , sp, rawTrajectories, true, false);

                for (MobileDevice d : devices) {
                    d.matchRawTrajectories(Settings.radius / 1000);
                    d.indexMappedTraj();
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }

                List<Hop> Q = extractQuery(rawTrajectories, sp, defaultRatio);
                topKQueryHandler.topKQuery(Q,defaultK);
                double beforeQuery = (double) System.currentTimeMillis() / 1000;
                for (MobileDevice d : devices) {
                    d.postRequest("http://localhost:9090/doTopKQuery", "begin:" + d.getDeviceId());
                    d.postRequest("http://localhost:9090/doTopKQuery", d.getDeviceId() + ":" + d.getRisk());
                }
                List<Map.Entry<String,Double>> result = topKQueryHandler.getAllResults()
                        .entrySet().stream().sorted(Comparator.comparing(Map.Entry::getValue)).collect(Collectors.toList());
                double endQuery = (double) System.currentTimeMillis() / 1000;
                topKQueryHandler.reset();
                gridIndexHandler.reset(false);
                totalQueryTime += endQuery - beforeQuery;
            }

            queryTimeWithoutPruningWithLocalBoostVaryingGridWidth.add(totalQueryTime/10);
            logger.info(String.format("Total query time %2f seconds ", totalQueryTime/10));
        }

        s.reset(Settings.latGridWidth);

        // 4. Q变化
        for(int i=0;i<ratioOfQ.length;i++){
            double ratio_q = ratioOfQ[i];

            SPExtrator sp = new SPExtrator(s.getGlobalGridIndex(), pois);
            double totalQueryTime = 0;
            for(int j=0;j<10;j++) {
                List<MobileDevice> devices = initMobileDevices(defaultDeviceNumber, defaultTrajPerDevice, s.getGlobalGridIndex()
                        , sp, rawTrajectories, true, false);

                for (MobileDevice d : devices) {
                    d.matchRawTrajectories(Settings.radius / 1000);
                    d.indexMappedTraj();
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }

                List<Hop> Q = extractQuery(rawTrajectories, sp, ratio_q);
                topKQueryHandler.topKQuery(Q,defaultK);
                double beforeQuery = (double) System.currentTimeMillis() / 1000;
                for (MobileDevice d : devices) {
                    d.postRequest("http://localhost:9090/doTopKQuery", "begin:" + d.getDeviceId());
                    d.postRequest("http://localhost:9090/doTopKQuery", d.getDeviceId() + ":" + d.getRisk());
                }
                List<Map.Entry<String,Double>> result = topKQueryHandler.getAllResults()
                        .entrySet().stream().sorted(Comparator.comparing(Map.Entry::getValue)).collect(Collectors.toList());
                double endQuery = (double) System.currentTimeMillis() / 1000;
                topKQueryHandler.reset();
                gridIndexHandler.reset(false);
                totalQueryTime += endQuery - beforeQuery;
            }

            queryTimeWithoutPruningWithLocalBoostVaryingQ.add(totalQueryTime/10);
            logger.info(String.format("Total query time %2f seconds ", totalQueryTime/10));
        }

        // 5. K变化
        for(int i=0;i<parameterK.length;i++){
            int k = parameterK[i];

            SPExtrator sp = new SPExtrator(s.getGlobalGridIndex(), pois);
            double totalQueryTime = 0;
            for(int j=0;j<10;j++) {
                List<MobileDevice> devices = initMobileDevices(defaultDeviceNumber, defaultTrajPerDevice, s.getGlobalGridIndex()
                        , sp, rawTrajectories, true, false);

                for (MobileDevice d : devices) {
                    d.matchRawTrajectories(Settings.radius / 1000);
                    d.indexMappedTraj();
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }

                List<Hop> Q = extractQuery(rawTrajectories, sp, defaultRatio);
                topKQueryHandler.topKQuery(Q,k);
                double beforeQuery = (double) System.currentTimeMillis() / 1000;
                for (MobileDevice d : devices) {
                    d.postRequest("http://localhost:9090/doTopKQuery", "begin:" + d.getDeviceId());
                    d.postRequest("http://localhost:9090/doTopKQuery", d.getDeviceId() + ":" + d.getRisk());
                }
                List<Map.Entry<String,Double>> result = topKQueryHandler.getAllResults()
                        .entrySet().stream().sorted(Comparator.comparing(Map.Entry::getValue)).collect(Collectors.toList());
                double endQuery = (double) System.currentTimeMillis() / 1000;
                topKQueryHandler.reset();
                gridIndexHandler.reset(false);
                totalQueryTime += endQuery - beforeQuery;
            }

            queryTimeWithoutPruningWithLocalBoostVaryingK.add(totalQueryTime/10);
            logger.info(String.format("Total query time %2f seconds ", totalQueryTime/10));
        }

        // 剪枝，移动端不加速
        // 1. 设备数量变化
        for(int i=0;i<deviceNumbers.length;i++){
            int device_number = deviceNumbers[i];
            double totalFilterTime = 0;
            double totalRefineTime = 0;

            for(int j=0;j<10;j++) {

                SPExtrator sp = new SPExtrator(s.getGlobalGridIndex(), pois);
                List<MobileDevice> devices = initMobileDevices(device_number, defaultTrajPerDevice, s.getGlobalGridIndex()
                        , sp, rawTrajectories, false, false);

                for (MobileDevice d : devices) {
                    d.matchRawTrajectories(Settings.radius / 1000);
                    d.indexMappedTraj();
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }

                List<Hop> Q = extractQuery(rawTrajectories, sp, defaultRatio);

                double beforeFilter = (double) System.currentTimeMillis() / 1000;
                topKQueryHandler.topKQuery(Q, defaultK);
                double afterFilter = (double) System.currentTimeMillis() / 1000;
                totalFilterTime += afterFilter - beforeFilter;

                double refineTime = 0;
                double beforeRefine = (double) System.currentTimeMillis() / 1000;
                Map<String, Double> candidates = topKQueryHandler.getCandidates();
                PriorityQueue<Double> result = new PriorityQueue<>(new Comparator<Double>() {
                    @Override
                    public int compare(Double o1, Double o2) {
                        if (o1 > o2) {
                            return 1;
                        } else if(o1.equals(o2)){
                            return 0;
                        }
                        else{
                            return -1;
                        }

                    }
                });
                List<Map.Entry<String, Double>> cans = candidates.entrySet().stream().sorted(
                        new Comparator<Map.Entry<String, Double>>() {
                            @Override
                            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                                if (o1.getValue() < o2.getValue()) {
                                    return 1;
                                } else if(o1.getValue().equals(o2.getValue())){
                                    return 0;
                                }
                                else{
                                    return -1;
                                }
                            }
                        }
                ).collect(Collectors.toList());

                List<MobileDevice> candidateDevices = cans.stream().map(d -> devices.get(Integer.parseInt(d.getKey()))).collect(Collectors.toList());

                if (candidates.size() < defaultK) {
                    double endRefine = (double) System.currentTimeMillis() / 1000;
                    refineTime += endRefine - beforeRefine;
                } else {
                    String mobileId;
                    for (MobileDevice d : candidateDevices) {
                        d.postRequest("http://localhost:9090/doTopKQuery", "begin:" + d.getDeviceId());
                        d.postRequest("http://localhost:9090/doTopKQuery", d.getDeviceId() + ":" + d.getRisk());
                        mobileId = d.getDeviceId();
                        result.add(topKQueryHandler.getAllResults().get(mobileId));
                        if (result.peek() > candidates.get(mobileId)) {
                            break;
                        }
                        while(result.size()>defaultK){
                            result.poll();
                        }
                    }

                    double endRefine = (double) System.currentTimeMillis() / 1000;
                    refineTime += endRefine - beforeRefine;

                }
                totalRefineTime += refineTime;

                gridIndexHandler.reset(false);
                topKQueryHandler.reset();
            }

            filterTimeWithPruningWithoutLocalBoostVaryingDeviceNumber.add(totalFilterTime/10);
            refineTimeWithPruningWithoutLocalBoostVaryingDeviceNumber.add(totalRefineTime/10);
            logger.info(String.format("Total filter time %2f seconds ", totalFilterTime/10));
            logger.info(String.format("Total refine time %2f seconds ", totalRefineTime/10));


        }

        // 2. 移动端轨迹数量变化
        for(int i=0;i<TrajPerDevices.length;i++){
            int traj_number = TrajPerDevices[i];
            double totalFilterTime = 0;
            double totalRefineTime = 0;

            for(int j=0;j<10;j++) {

                SPExtrator sp = new SPExtrator(s.getGlobalGridIndex(), pois);
                List<MobileDevice> devices = initMobileDevices(defaultDeviceNumber, traj_number, s.getGlobalGridIndex()
                        , sp, rawTrajectories, false, false);

                for (MobileDevice d : devices) {
                    d.matchRawTrajectories(Settings.radius / 1000);
                    d.indexMappedTraj();
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }

                List<Hop> Q = extractQuery(rawTrajectories, sp, defaultRatio);

                double beforeFilter = (double) System.currentTimeMillis() / 1000;
                topKQueryHandler.topKQuery(Q, defaultK);
                double afterFilter = (double) System.currentTimeMillis() / 1000;
                totalFilterTime += afterFilter - beforeFilter;

                double refineTime = 0;
                double beforeRefine = (double) System.currentTimeMillis() / 1000;
                Map<String, Double> candidates = topKQueryHandler.getCandidates();
                PriorityQueue<Double> result = new PriorityQueue<>(new Comparator<Double>() {
                    @Override
                    public int compare(Double o1, Double o2) {
                        if (o1 > o2) {
                            return 1;
                        }
                        else if(o1.equals(o2)){
                            return 0;
                        }
                        else {
                            return -1;
                        }

                    }
                });
                List<Map.Entry<String, Double>> cans = candidates.entrySet().stream().sorted(
                        new Comparator<Map.Entry<String, Double>>() {
                            @Override
                            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                                if (o1.getValue() < o2.getValue()) {
                                    return 1;
                                }
                                else if(o1.getValue().equals(o2.getValue())){
                                    return 0;
                                }
                                else {
                                    return -1;
                                }
                            }
                        }
                ).collect(Collectors.toList());

                List<MobileDevice> candidateDevices = cans.stream().map(d -> devices.get(Integer.parseInt(d.getKey()))).collect(Collectors.toList());

                if (candidates.size() < defaultK) {
                    double endRefine = (double) System.currentTimeMillis() / 1000;
                    refineTime += endRefine - beforeRefine;
                } else {
                    String mobileId;
                    for (MobileDevice d : candidateDevices) {
                        d.postRequest("http://localhost:9090/doTopKQuery", "begin:" + d.getDeviceId());
                        d.postRequest("http://localhost:9090/doTopKQuery", d.getDeviceId() + ":" + d.getRisk());
                        mobileId = d.getDeviceId();
                        result.add(topKQueryHandler.getAllResults().get(mobileId));
                        if (result.peek() > candidates.get(mobileId)) {
                            break;
                        }
                        while(result.size()>defaultK){
                            result.poll();
                        }

                    }

                    double endRefine = (double) System.currentTimeMillis() / 1000;
                    refineTime += endRefine - beforeRefine;

                }
                totalRefineTime += refineTime;

                gridIndexHandler.reset(false);
                topKQueryHandler.reset();
            }

            filterTimeWithPruningWithoutLocalBoostVaryingTrajNumber.add(totalFilterTime/10);
            refineTimeWithPruningWithoutLocalBoostVaryingTrajNumber.add(totalRefineTime/10);
            logger.info(String.format("Total filter time %2f seconds ", totalFilterTime/10));
            logger.info(String.format("Total refine time %2f seconds ", totalRefineTime/10));


        }

        // 3. 网格宽度变化
        for(int i=0;i<gridWidths.length;i++){
            double gridWidth = gridWidths[i];
            double totalFilterTime = 0;
            double totalRefineTime = 0;

            for(int j=0;j<10;j++) {

                s.reset(gridWidth);
                SPExtrator sp = new SPExtrator(s.getGlobalGridIndex(), pois);
                List<MobileDevice> devices = initMobileDevices(defaultDeviceNumber, defaultTrajPerDevice, s.getGlobalGridIndex()
                        , sp, rawTrajectories, false, false);
                for (MobileDevice d : devices) {
                    d.matchRawTrajectories(Settings.radius / 1000);
                    d.indexMappedTraj();
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }

                List<Hop> Q = extractQuery(rawTrajectories, sp, defaultRatio);

                double beforeFilter = (double) System.currentTimeMillis() / 1000;
                topKQueryHandler.topKQuery(Q, defaultK);
                double afterFilter = (double) System.currentTimeMillis() / 1000;
                totalFilterTime += afterFilter - beforeFilter;

                double refineTime = 0;
                double beforeRefine = (double) System.currentTimeMillis() / 1000;
                Map<String, Double> candidates = topKQueryHandler.getCandidates();
                PriorityQueue<Double> result = new PriorityQueue<>(new Comparator<Double>() {
                    @Override
                    public int compare(Double o1, Double o2) {
                        if (o1 > o2) {
                            return 1;
                        }
                        else if(o1.equals(o2)){
                            return 0;
                        }
                        else {
                            return -1;
                        }

                    }
                });
                List<Map.Entry<String, Double>> cans = candidates.entrySet().stream().sorted(
                        new Comparator<Map.Entry<String, Double>>() {
                            @Override
                            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                                if (o1.getValue() < o2.getValue()) {
                                    return 1;
                                }
                                else if(o1.getValue().equals(o2.getValue())){
                                    return 0;
                                }
                                else {
                                    return -1;
                                }
                            }
                        }
                ).collect(Collectors.toList());

                List<MobileDevice> candidateDevices = cans.stream().map(d -> devices.get(Integer.parseInt(d.getKey()))).collect(Collectors.toList());

                if (candidates.size() < defaultK) {
                    double endRefine = (double) System.currentTimeMillis() / 1000;
                    refineTime += endRefine - beforeRefine;
                } else {
                    String mobileId;
                    for (MobileDevice d : candidateDevices) {
                        d.postRequest("http://localhost:9090/doTopKQuery", "begin:" + d.getDeviceId());
                        d.postRequest("http://localhost:9090/doTopKQuery", d.getDeviceId() + ":" + d.getRisk());
                        mobileId = d.getDeviceId();
                        result.add(topKQueryHandler.getAllResults().get(mobileId));
                        if (result.peek() > candidates.get(mobileId)) {
                            break;
                        }
                        while(result.size()>defaultK){
                            result.poll();
                        }
                    }

                    double endRefine = (double) System.currentTimeMillis() / 1000;
                    refineTime += endRefine - beforeRefine;

                }
                totalRefineTime += refineTime;

                gridIndexHandler.reset(false);
                topKQueryHandler.reset();
            }

            filterTimeWithPruningWithoutLocalBoostVaryingGridWidth.add(totalFilterTime/10);
            refineTimeWithPruningWithoutLocalBoostVaryingGridWidth.add(totalRefineTime/10);
            logger.info(String.format("Total filter time %2f seconds ", totalFilterTime/10));
            logger.info(String.format("Total refine time %2f seconds ", totalRefineTime/10));


        }

        s.reset(Settings.latGridWidth);

        // 4. Q变化
        for(int i=0;i<ratioOfQ.length;i++){
            double ratio_q = ratioOfQ[i];
            double totalFilterTime = 0;
            double totalRefineTime = 0;

            for(int j=0;j<10;j++) {

                SPExtrator sp = new SPExtrator(s.getGlobalGridIndex(), pois);
                List<MobileDevice> devices = initMobileDevices(defaultDeviceNumber, defaultTrajPerDevice, s.getGlobalGridIndex()
                        , sp, rawTrajectories, false, false);

                for (MobileDevice d : devices) {
                    d.matchRawTrajectories(Settings.radius / 1000);
                    d.indexMappedTraj();
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }

                List<Hop> Q = extractQuery(rawTrajectories, sp, ratio_q);

                double beforeFilter = (double) System.currentTimeMillis() / 1000;
                topKQueryHandler.topKQuery(Q, defaultK);
                double afterFilter = (double) System.currentTimeMillis() / 1000;
                totalFilterTime += afterFilter - beforeFilter;

                double refineTime = 0;
                double beforeRefine = (double) System.currentTimeMillis() / 1000;
                Map<String, Double> candidates = topKQueryHandler.getCandidates();
                PriorityQueue<Double> result = new PriorityQueue<>(new Comparator<Double>() {
                    @Override
                    public int compare(Double o1, Double o2) {
                        if (o1 > o2) {
                            return 1;
                        }
                        else if (o1.equals(o2)){
                            return 0;
                        }
                        else {
                            return -1;
                        }

                    }
                });
                List<Map.Entry<String, Double>> cans = candidates.entrySet().stream().sorted(
                        new Comparator<Map.Entry<String, Double>>() {
                            @Override
                            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                                if (o1.getValue() < o2.getValue()) {
                                    return 1;
                                }
                                else if (o1.getValue().equals(o2.getValue())){
                                    return 0;
                                }
                                else {
                                    return -1;
                                }
                            }
                        }
                ).collect(Collectors.toList());

                List<MobileDevice> candidateDevices = cans.stream().map(d -> devices.get(Integer.parseInt(d.getKey()))).collect(Collectors.toList());

                if (candidates.size() < defaultK) {
                    double endRefine = (double) System.currentTimeMillis() / 1000;
                    refineTime += endRefine - beforeRefine;
                } else {
                    String mobileId;
                    for (MobileDevice d : candidateDevices) {
                        d.postRequest("http://localhost:9090/doTopKQuery", "begin:" + d.getDeviceId());
                        d.postRequest("http://localhost:9090/doTopKQuery", d.getDeviceId() + ":" + d.getRisk());
                        mobileId = d.getDeviceId();
                        result.add(topKQueryHandler.getAllResults().get(mobileId));
                        if (result.peek() > candidates.get(mobileId)) {
                            break;
                        }
                        while(result.size()>defaultK){
                            result.poll();
                        }
                    }

                    double endRefine = (double) System.currentTimeMillis() / 1000;
                    refineTime += endRefine - beforeRefine;

                }
                totalRefineTime += refineTime;

                gridIndexHandler.reset(false);
                topKQueryHandler.reset();
            }

            filterTimeWithPruningWithoutLocalBoostVaryingQ.add(totalFilterTime/10);
            refineTimeWithPruningWithoutLocalBoostVaryingQ.add(totalRefineTime/10);
            logger.info(String.format("Total filter time %2f seconds ", totalFilterTime/10));
            logger.info(String.format("Total refine time %2f seconds ", totalRefineTime/10));


        }


        // 5. K变化
        for(int i=0;i<parameterK.length;i++){
            int k = parameterK[i];
            double totalFilterTime = 0;
            double totalRefineTime = 0;

            for(int j=0;j<10;j++) {

                SPExtrator sp = new SPExtrator(s.getGlobalGridIndex(), pois);
                List<MobileDevice> devices = initMobileDevices(defaultDeviceNumber, defaultTrajPerDevice, s.getGlobalGridIndex()
                        , sp, rawTrajectories, false, false);

                for (MobileDevice d : devices) {
                    d.matchRawTrajectories(Settings.radius / 1000);
                    d.indexMappedTraj();
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }

                List<Hop> Q = extractQuery(rawTrajectories, sp, defaultRatio);

                double beforeFilter = (double) System.currentTimeMillis() / 1000;
                topKQueryHandler.topKQuery(Q, k);
                double afterFilter = (double) System.currentTimeMillis() / 1000;
                totalFilterTime += afterFilter - beforeFilter;

                double refineTime = 0;
                double beforeRefine = (double) System.currentTimeMillis() / 1000;
                Map<String, Double> candidates = topKQueryHandler.getCandidates();
                PriorityQueue<Double> result = new PriorityQueue<>(new Comparator<Double>() {
                    @Override
                    public int compare(Double o1, Double o2) {
                        if (o1 > o2) {
                            return 1;
                        }
                        else if(o1.equals(o2)){
                            return 0;
                        }
                        else {
                            return -1;
                        }

                    }
                });
                List<Map.Entry<String, Double>> cans = candidates.entrySet().stream().sorted(
                        new Comparator<Map.Entry<String, Double>>() {
                            @Override
                            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                                if (o1.getValue() < o2.getValue()) {
                                    return 1;
                                }
                                else if(o1.getValue().equals(o2.getValue())){
                                    return 0;
                                }
                                else {
                                    return -1;
                                }
                            }
                        }
                ).collect(Collectors.toList());

                List<MobileDevice> candidateDevices = cans.stream().map(d -> devices.get(Integer.parseInt(d.getKey()))).collect(Collectors.toList());

                if (candidates.size() < k) {
                    double endRefine = (double) System.currentTimeMillis() / 1000;
                    refineTime += endRefine - beforeRefine;
                } else {
                    String mobileId;
                    for (MobileDevice d : candidateDevices) {
                        d.postRequest("http://localhost:9090/doTopKQuery", "begin:" + d.getDeviceId());
                        d.postRequest("http://localhost:9090/doTopKQuery", d.getDeviceId() + ":" + d.getRisk());
                        mobileId = d.getDeviceId();
                        result.add(topKQueryHandler.getAllResults().get(mobileId));
                        if (result.peek() > candidates.get(mobileId)) {
                            break;
                        }
                        while(result.size()>k){
                            result.poll();
                        }
                    }

                    double endRefine = (double) System.currentTimeMillis() / 1000;
                    refineTime += endRefine - beforeRefine;

                }
                totalRefineTime += refineTime;

                gridIndexHandler.reset(false);
                topKQueryHandler.reset();
            }

            filterTimeWithPruningWithoutLocalBoostVaryingK.add(totalFilterTime/10);
            refineTimeWithPruningWithoutLocalBoostVaryingK.add(totalRefineTime/10);
            logger.info(String.format("Total filter time %2f seconds ", totalFilterTime/10));
            logger.info(String.format("Total refine time %2f seconds ", totalRefineTime/10));


        }


        //剪枝，移动端加速
        // 1. 设备数量变化
        for(int i=0;i<deviceNumbers.length;i++){
            int device_number = deviceNumbers[i];
            double totalFilterTime = 0;
            double totalRefineTime = 0;

            for(int j=0;j<10;j++) {

                SPExtrator sp = new SPExtrator(s.getGlobalGridIndex(), pois);
                List<MobileDevice> devices = initMobileDevices(device_number, defaultTrajPerDevice, s.getGlobalGridIndex()
                        , sp, rawTrajectories, true, false);

                for (MobileDevice d : devices) {
                    d.matchRawTrajectories(Settings.radius / 1000);
                    d.indexMappedTraj();
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }

                List<Hop> Q = extractQuery(rawTrajectories, sp, defaultRatio);

                double beforeFilter = (double) System.currentTimeMillis() / 1000;
                topKQueryHandler.topKQuery(Q, defaultK);
                double afterFilter = (double) System.currentTimeMillis() / 1000;
                totalFilterTime += afterFilter - beforeFilter;

                double refineTime = 0;
                double beforeRefine = (double) System.currentTimeMillis() / 1000;
                Map<String, Double> candidates = topKQueryHandler.getCandidates();
                PriorityQueue<Double> result = new PriorityQueue<>(new Comparator<Double>() {
                    @Override
                    public int compare(Double o1, Double o2) {
                        if (o1 > o2) {
                            return 1;
                        }
                        else if(o1.equals(o2)){
                            return 0;
                        }
                        else {
                            return -1;
                        }

                    }
                });
                List<Map.Entry<String, Double>> cans = candidates.entrySet().stream().sorted(
                        new Comparator<Map.Entry<String, Double>>() {
                            @Override
                            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                                if (o1.getValue() < o2.getValue()) {
                                    return 1;
                                }
                                else if(o1.getValue().equals(o2.getValue())){
                                    return 0;
                                }
                                else {
                                    return -1;
                                }
                            }
                        }
                ).collect(Collectors.toList());

                List<MobileDevice> candidateDevices = cans.stream().map(d -> devices.get(Integer.parseInt(d.getKey()))).collect(Collectors.toList());

                if (candidates.size() < defaultK) {
                    double endRefine = (double) System.currentTimeMillis() / 1000;
                    refineTime += endRefine - beforeRefine;
                } else {
                    String mobileId;
                    for (MobileDevice d : candidateDevices) {
                        d.postRequest("http://localhost:9090/doTopKQuery", "begin:" + d.getDeviceId());
                        d.postRequest("http://localhost:9090/doTopKQuery", d.getDeviceId() + ":" + d.getRisk());
                        mobileId = d.getDeviceId();
                        result.add(topKQueryHandler.getAllResults().get(mobileId));
                        if (result.peek() > candidates.get(mobileId)) {
                            break;
                        }
                        while(result.size()>defaultK){
                            result.poll();
                        }
                    }

                    double endRefine = (double) System.currentTimeMillis() / 1000;
                    refineTime += endRefine - beforeRefine;

                }
                totalRefineTime += refineTime;

                gridIndexHandler.reset(false);
                topKQueryHandler.reset();
            }

            filterTimeWithPruningWithLocalBoostVaryingDeviceNumber.add(totalFilterTime/10);
            refineTimeWithPruningWithLocalBoostVaryingDeviceNumber.add(totalRefineTime/10);
            logger.info(String.format("Total filter time %2f seconds ", totalFilterTime/10));
            logger.info(String.format("Total refine time %2f seconds ", totalRefineTime/10));


        }
        // 2. 移动端轨迹数量变化
        for(int i=0;i<TrajPerDevices.length;i++){
            int traj_number = TrajPerDevices[i];
            double totalFilterTime = 0;
            double totalRefineTime = 0;

            for(int j=0;j<10;j++) {

                SPExtrator sp = new SPExtrator(s.getGlobalGridIndex(), pois);
                List<MobileDevice> devices = initMobileDevices(defaultDeviceNumber, traj_number, s.getGlobalGridIndex()
                        , sp, rawTrajectories, true, false);

                for (MobileDevice d : devices) {
                    d.matchRawTrajectories(Settings.radius / 1000);
                    d.indexMappedTraj();
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }

                List<Hop> Q = extractQuery(rawTrajectories, sp, defaultRatio);

                double beforeFilter = (double) System.currentTimeMillis() / 1000;
                topKQueryHandler.topKQuery(Q, defaultK);
                double afterFilter = (double) System.currentTimeMillis() / 1000;
                totalFilterTime += afterFilter - beforeFilter;

                double refineTime = 0;
                double beforeRefine = (double) System.currentTimeMillis() / 1000;
                Map<String, Double> candidates = topKQueryHandler.getCandidates();
                PriorityQueue<Double> result = new PriorityQueue<>(new Comparator<Double>() {
                    @Override
                    public int compare(Double o1, Double o2) {
                        if (o1 > o2) {
                            return 1;
                        }
                        else if (o1.equals(o2)){
                            return 0;
                        }
                        else {
                            return -1;
                        }

                    }
                });
                List<Map.Entry<String, Double>> cans = candidates.entrySet().stream().sorted(
                        new Comparator<Map.Entry<String, Double>>() {
                            @Override
                            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                                if (o1.getValue() < o2.getValue()) {
                                    return 1;
                                }
                                else if(o1.getValue().equals(o2.getValue())){
                                    return 0;
                                }
                                else {
                                    return -1;
                                }
                            }
                        }
                ).collect(Collectors.toList());

                List<MobileDevice> candidateDevices = cans.stream().map(d -> devices.get(Integer.parseInt(d.getKey()))).collect(Collectors.toList());

                if (candidates.size() < defaultK) {
                    double endRefine = (double) System.currentTimeMillis() / 1000;
                    refineTime += endRefine - beforeRefine;
                } else {
                    String mobileId;
                    for (MobileDevice d : candidateDevices) {
                        d.postRequest("http://localhost:9090/doTopKQuery", "begin:" + d.getDeviceId());
                        d.postRequest("http://localhost:9090/doTopKQuery", d.getDeviceId() + ":" + d.getRisk());
                        mobileId = d.getDeviceId();
                        result.add(topKQueryHandler.getAllResults().get(mobileId));
                        if (result.peek() > candidates.get(mobileId)) {
                            break;
                        }
                        while(result.size()>defaultK){
                            result.poll();
                        }
                    }

                    double endRefine = (double) System.currentTimeMillis() / 1000;
                    refineTime += endRefine - beforeRefine;

                }
                totalRefineTime += refineTime;

                gridIndexHandler.reset(false);
                topKQueryHandler.reset();
            }

            filterTimeWithPruningWithLocalBoostVaryingTrajNumber.add(totalFilterTime/10);
            refineTimeWithPruningWithLocalBoostVaryingTrajNumber.add(totalRefineTime/10);
            logger.info(String.format("Total filter time %2f seconds ", totalFilterTime/10));
            logger.info(String.format("Total refine time %2f seconds ", totalRefineTime/10));


        }

        // 3. 网格宽度变化
        for(int i=0;i<gridWidths.length;i++){
            double gridWidth = gridWidths[i];
            double totalFilterTime = 0;
            double totalRefineTime = 0;

            for(int j=0;j<10;j++) {

                s.reset(gridWidth);
                SPExtrator sp = new SPExtrator(s.getGlobalGridIndex(), pois);
                List<MobileDevice> devices = initMobileDevices(defaultDeviceNumber, defaultTrajPerDevice, s.getGlobalGridIndex()
                        , sp, rawTrajectories, true, false);

                for (MobileDevice d : devices) {
                    d.matchRawTrajectories(Settings.radius / 1000);
                    d.indexMappedTraj();
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }

                List<Hop> Q = extractQuery(rawTrajectories, sp, defaultRatio);

                double beforeFilter = (double) System.currentTimeMillis() / 1000;
                topKQueryHandler.topKQuery(Q, defaultK);
                double afterFilter = (double) System.currentTimeMillis() / 1000;
                totalFilterTime += afterFilter - beforeFilter;

                double refineTime = 0;
                double beforeRefine = (double) System.currentTimeMillis() / 1000;
                Map<String, Double> candidates = topKQueryHandler.getCandidates();
                PriorityQueue<Double> result = new PriorityQueue<>(new Comparator<Double>() {
                    @Override
                    public int compare(Double o1, Double o2) {
                        if (o1 > o2) {
                            return 1;
                        }
                        else if (o1.equals(o2)){
                            return 0;
                        }
                        else {
                            return -1;
                        }

                    }
                });
                List<Map.Entry<String, Double>> cans = candidates.entrySet().stream().sorted(
                        new Comparator<Map.Entry<String, Double>>() {
                            @Override
                            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                                if (o1.getValue() < o2.getValue()) {
                                    return 1;
                                }
                                else if(o1.getValue().equals(o2.getValue())){
                                    return 0;
                                }
                                else {
                                    return -1;
                                }
                            }
                        }
                ).collect(Collectors.toList());

                List<MobileDevice> candidateDevices = cans.stream().map(d -> devices.get(Integer.parseInt(d.getKey()))).collect(Collectors.toList());

                if (candidates.size() < defaultK) {
                    double endRefine = (double) System.currentTimeMillis() / 1000;
                    refineTime += endRefine - beforeRefine;
                } else {
                    String mobileId;
                    for (MobileDevice d : candidateDevices) {
                        d.postRequest("http://localhost:9090/doTopKQuery", "begin:" + d.getDeviceId());
                        d.postRequest("http://localhost:9090/doTopKQuery", d.getDeviceId() + ":" + d.getRisk());
                        mobileId = d.getDeviceId();
                        result.add(topKQueryHandler.getAllResults().get(mobileId));
                        if (result.peek() > candidates.get(mobileId)) {
                            break;
                        }
                        while(result.size()>defaultK){
                            result.poll();
                        }
                    }

                    double endRefine = (double) System.currentTimeMillis() / 1000;
                    refineTime += endRefine - beforeRefine;

                }
                totalRefineTime += refineTime;

                gridIndexHandler.reset(false);
                topKQueryHandler.reset();
            }

            filterTimeWithPruningWithLocalBoostVaryingGridWidth.add(totalFilterTime/10);
            refineTimeWithPruningWithLocalBoostVaryingGridWidth.add(totalRefineTime/10);
            logger.info(String.format("Total filter time %2f seconds ", totalFilterTime/10));
            logger.info(String.format("Total refine time %2f seconds ", totalRefineTime/10));


        }

        s.reset(Settings.latGridWidth);

        // 4. Q变化
        for(int i=0;i<ratioOfQ.length;i++){
            double ratio_q = ratioOfQ[i];
            double totalFilterTime = 0;
            double totalRefineTime = 0;

            for(int j=0;j<10;j++) {

                SPExtrator sp = new SPExtrator(s.getGlobalGridIndex(), pois);
                List<MobileDevice> devices = initMobileDevices(defaultDeviceNumber, defaultTrajPerDevice, s.getGlobalGridIndex()
                        , sp, rawTrajectories, true, false);

                for (MobileDevice d : devices) {
                    d.matchRawTrajectories(Settings.radius / 1000);
                    d.indexMappedTraj();
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }

                List<Hop> Q = extractQuery(rawTrajectories, sp, ratio_q);

                double beforeFilter = (double) System.currentTimeMillis() / 1000;
                topKQueryHandler.topKQuery(Q, defaultK);
                double afterFilter = (double) System.currentTimeMillis() / 1000;
                totalFilterTime += afterFilter - beforeFilter;

                double refineTime = 0;
                double beforeRefine = (double) System.currentTimeMillis() / 1000;
                Map<String, Double> candidates = topKQueryHandler.getCandidates();
                PriorityQueue<Double> result = new PriorityQueue<>(new Comparator<Double>() {
                    @Override
                    public int compare(Double o1, Double o2) {
                        if (o1 > o2) {
                            return 1;
                        }
                        else if(o1.equals(o2)){
                            return 0;
                        }
                        else {
                            return -1;
                        }

                    }
                });
                List<Map.Entry<String, Double>> cans = candidates.entrySet().stream().sorted(
                        new Comparator<Map.Entry<String, Double>>() {
                            @Override
                            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                                if (o1.getValue() < o2.getValue()) {
                                    return 1;
                                }
                                else if(o1.getValue().equals(o2.getValue())){
                                    return 0;
                                }
                                else {
                                    return -1;
                                }
                            }
                        }
                ).collect(Collectors.toList());

                List<MobileDevice> candidateDevices = cans.stream().map(d -> devices.get(Integer.parseInt(d.getKey()))).collect(Collectors.toList());

                if (candidates.size() < defaultK) {
                    double endRefine = (double) System.currentTimeMillis() / 1000;
                    refineTime += endRefine - beforeRefine;
                } else {
                    String mobileId;
                    for (MobileDevice d : candidateDevices) {
                        d.postRequest("http://localhost:9090/doTopKQuery", "begin:" + d.getDeviceId());
                        d.postRequest("http://localhost:9090/doTopKQuery", d.getDeviceId() + ":" + d.getRisk());
                        mobileId = d.getDeviceId();
                        result.add(topKQueryHandler.getAllResults().get(mobileId));
                        if (result.peek() > candidates.get(mobileId)) {
                            break;
                        }
                        while(result.size()>defaultK){
                            result.poll();
                        }
                    }

                    double endRefine = (double) System.currentTimeMillis() / 1000;
                    refineTime += endRefine - beforeRefine;

                }
                totalRefineTime += refineTime;

                gridIndexHandler.reset(false);
                topKQueryHandler.reset();
            }

            filterTimeWithPruningWithLocalBoostVaryingQ.add(totalFilterTime/10);
            refineTimeWithPruningWithLocalBoostVaryingQ.add(totalRefineTime/10);
            logger.info(String.format("Total filter time %2f seconds ", totalFilterTime/10));
            logger.info(String.format("Total refine time %2f seconds ", totalRefineTime/10));


        }


        // 5. K变化
        for(int i=0;i<parameterK.length;i++){
            int k = parameterK[i];
            double totalFilterTime = 0;
            double totalRefineTime = 0;

            for(int j=0;j<10;j++) {

                SPExtrator sp = new SPExtrator(s.getGlobalGridIndex(), pois);
                List<MobileDevice> devices = initMobileDevices(defaultDeviceNumber, defaultTrajPerDevice, s.getGlobalGridIndex()
                        , sp, rawTrajectories, true, false);

                for (MobileDevice d : devices) {
                    d.matchRawTrajectories(Settings.radius / 1000);
                    d.indexMappedTraj();
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }

                List<Hop> Q = extractQuery(rawTrajectories, sp, defaultRatio);

                double beforeFilter = (double) System.currentTimeMillis() / 1000;
                topKQueryHandler.topKQuery(Q, k);
                double afterFilter = (double) System.currentTimeMillis() / 1000;
                totalFilterTime += afterFilter - beforeFilter;

                double refineTime = 0;
                double beforeRefine = (double) System.currentTimeMillis() / 1000;
                Map<String, Double> candidates = topKQueryHandler.getCandidates();
                PriorityQueue<Double> result = new PriorityQueue<>(new Comparator<Double>() {
                    @Override
                    public int compare(Double o1, Double o2) {
                        if (o1 > o2) {
                            return 1;
                        }
                        else if(o1.equals(o2)){
                            return 0;
                        }
                        else {
                            return -1;
                        }

                    }
                });
                List<Map.Entry<String, Double>> cans = candidates.entrySet().stream().sorted(
                        new Comparator<Map.Entry<String, Double>>() {
                            @Override
                            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                                if (o1.getValue() < o2.getValue()) {
                                    return 1;
                                }
                                else if(o1.getValue().equals(o2.getValue())){
                                    return 0;
                                }
                                else {
                                    return -1;
                                }
                            }
                        }
                ).collect(Collectors.toList());

                List<MobileDevice> candidateDevices = cans.stream().map(d -> devices.get(Integer.parseInt(d.getKey()))).collect(Collectors.toList());

                if (candidates.size() < k) {
                    double endRefine = (double) System.currentTimeMillis() / 1000;
                    refineTime += endRefine - beforeRefine;
                } else {
                    String mobileId;
                    for (MobileDevice d : candidateDevices) {
                        d.postRequest("http://localhost:9090/doTopKQuery", "begin:" + d.getDeviceId());
                        d.postRequest("http://localhost:9090/doTopKQuery", d.getDeviceId() + ":" + d.getRisk());
                        mobileId = d.getDeviceId();
                        result.add(topKQueryHandler.getAllResults().get(mobileId));
                        if (result.peek() > candidates.get(mobileId)) {
                            break;
                        }
                        while(result.size()>k){
                            result.poll();
                        }
                    }

                    double endRefine = (double) System.currentTimeMillis() / 1000;
                    refineTime += endRefine - beforeRefine;

                }
                totalRefineTime += refineTime;

                gridIndexHandler.reset(false);
                topKQueryHandler.reset();
            }

            filterTimeWithPruningWithLocalBoostVaryingK.add(totalFilterTime/10);
            refineTimeWithPruningWithLocalBoostVaryingK.add(totalRefineTime/10);
            logger.info(String.format("Total filter time %2f seconds ", totalFilterTime/10));
            logger.info(String.format("Total refine time %2f seconds ", totalRefineTime/10));


        }


        System.out.println(queryTimeWithoutPruningWithoutLocalBoostVaryingDeviceNumber);
        System.out.println(queryTimeWithoutPruningWithoutLocalBoostVaryingTrajNumber);
        System.out.println(queryTimeWithoutPruningWithoutLocalBoostVaryingGridWidth);
        System.out.println(queryTimeWithoutPruningWithoutLocalBoostVaryingQ);
        System.out.println(queryTimeWithoutPruningWithoutLocalBoostVaryingK);

        System.out.println(queryTimeWithoutPruningWithLocalBoostVaryingDeviceNumber);
        System.out.println(queryTimeWithoutPruningWithLocalBoostVaryingTrajNumber);
        System.out.println(queryTimeWithoutPruningWithLocalBoostVaryingGridWidth);
        System.out.println(queryTimeWithoutPruningWithLocalBoostVaryingQ);
        System.out.println(queryTimeWithoutPruningWithLocalBoostVaryingK);

        System.out.println(filterTimeWithPruningWithoutLocalBoostVaryingDeviceNumber);
        System.out.println(refineTimeWithPruningWithoutLocalBoostVaryingDeviceNumber);
        System.out.println(filterTimeWithPruningWithoutLocalBoostVaryingTrajNumber);
        System.out.println(refineTimeWithPruningWithoutLocalBoostVaryingTrajNumber);
        System.out.println(filterTimeWithPruningWithoutLocalBoostVaryingGridWidth);
        System.out.println(refineTimeWithPruningWithoutLocalBoostVaryingGridWidth);
        System.out.println(filterTimeWithPruningWithoutLocalBoostVaryingQ);
        System.out.println(refineTimeWithPruningWithoutLocalBoostVaryingQ);
        System.out.println(filterTimeWithPruningWithoutLocalBoostVaryingK);
        System.out.println(refineTimeWithPruningWithoutLocalBoostVaryingK);


        System.out.println(filterTimeWithPruningWithLocalBoostVaryingDeviceNumber);
        System.out.println(refineTimeWithPruningWithLocalBoostVaryingDeviceNumber);
        System.out.println(filterTimeWithPruningWithLocalBoostVaryingTrajNumber);
        System.out.println(refineTimeWithPruningWithLocalBoostVaryingTrajNumber);
        System.out.println(filterTimeWithPruningWithLocalBoostVaryingGridWidth);
        System.out.println(refineTimeWithPruningWithLocalBoostVaryingGridWidth);
        System.out.println(filterTimeWithPruningWithLocalBoostVaryingQ);
        System.out.println(refineTimeWithPruningWithLocalBoostVaryingQ);
        System.out.println(filterTimeWithPruningWithLocalBoostVaryingK);
        System.out.println(refineTimeWithPruningWithLocalBoostVaryingK);


        httpServer.stop(0);


    }

    public static List<MobileDevice> initMobileDevices(int deviceAmount,int rawTrajsPerDevice,GridIndex globalGridIndex,
                                         SPExtrator sp, List<RawTrajectory> rawTrajectoryList,boolean hasLocalBoost,boolean indexCompressed){
        List<MobileDevice> res = new ArrayList<>();
        for(int i=0;i<deviceAmount;i++){
            res.add(initMobile(i,deviceAmount,rawTrajsPerDevice,globalGridIndex,sp,rawTrajectoryList,hasLocalBoost,indexCompressed));
        }
        return res;
    }

    public static MobileDevice initMobile(int deviceNo,int deviceAmount,int rawTrajsPerDevice,GridIndex globalGridIndex,
                                          SPExtrator sp,List<RawTrajectory> rawTrajectoryList,boolean hasLocalBoost,boolean indexCompressed){
        int count = deviceNo;
        int size = rawTrajectoryList.size();
        List<RawTrajectory> res = new ArrayList<>();
        for(int i=0;i<rawTrajsPerDevice;i++){
            count += deviceAmount;
            res.add(rawTrajectoryList.get(count%size));
        }

        return new MobileDevice(String.valueOf(deviceNo),globalGridIndex,res,sp,hasLocalBoost,indexCompressed);
    }

    public static List<MobileDevice> initMobileDevices(int deviceAmount,int trajPerDevice,GridIndex globalGridIndex,
                                List<MappedTrajectory> mappedTrajectoryList,boolean hasLocalBoost,boolean indexCompression){
        List<MobileDevice> res = new ArrayList<>();
        for(int i=0;i<deviceAmount;i++){
            res.add(initMobile(i,deviceAmount,trajPerDevice,globalGridIndex,mappedTrajectoryList,hasLocalBoost,indexCompression));
        }
        return res;
    }

    public static MobileDevice initMobile(int deviceNo,int deviceAmount,int trajPerDevice, GridIndex globalGridIndex,
                              List<MappedTrajectory> mappedTrajectoryList,boolean hasLocalBoost,boolean indexCompression){
        int count =deviceNo;
        int size = mappedTrajectoryList.size();
        List<MappedTrajectory> res = new ArrayList<>();
        for(int i=0;i<trajPerDevice;i++){
            count+=deviceAmount;
            res.add(mappedTrajectoryList.get(count%size));
        }
        return new MobileDevice(String.valueOf(deviceNo),globalGridIndex,res,hasLocalBoost,indexCompression);
    }

    public static List<Hop> extractQuery(List<RawTrajectory> trajs,SPExtrator sp, double ratio){
        List<MappedTrajectory> mappedTrajectoryList ;
        List<RawTrajectory> tmp = new ArrayList<>();
        int number = (int) (trajs.size()*ratio);
        int totalsize = trajs.size();
        for(int i=0;i<number;i++){
            int tid = (int) (Math.random()*totalsize);
            tmp.add(trajs.get(tid));
        }

        mappedTrajectoryList = sp.extractBatch(tmp,Settings.trajectoryClusterDistanceThreshold
                ,Settings.trajectoryClusterTimeThreshold,Settings.radius/1000);

        return mappedTrajectoryList.stream().map(MappedTrajectory::getHops).flatMap(Collection::stream).collect(Collectors.toList());

    }

    public static List<Hop> extractQueryFromCheckinData(List<MappedTrajectory> mappedTrajectoryList,
                                                        double ratio){
        List<MappedTrajectory> mappedTrajectories = new ArrayList<>();
        int totalsize = mappedTrajectoryList.size();
        int number = (int) (totalsize*ratio);
        for(int i=0;i<number;i++){
            int tid = (int) (Math.random()*totalsize);
            mappedTrajectories.add(mappedTrajectoryList.get(tid));
        }
        return mappedTrajectories.stream().map(MappedTrajectory::getHops).flatMap(Collection::stream).collect(Collectors.toList());
    }



    public static void topKQueryTestOfNewYork() throws IOException {

        List<POI> pois = new ArrayList<>();
        // List<RawTrajectory> rawTrajectories = DataLoader.loadRawTrajDataBeiJing();
        List<MappedTrajectory> mappedTrajectoryList = new ArrayList<>();
        DataLoader.readTrajFileTokyo(mappedTrajectoryList,pois);

        int rawtrajsPerDevice = 100;

        int defaultDeviceNumber = 600;
        int defaultTrajPerDevice = 200;
        double defaultRatio = 0.003;
        int defaultK = 30;

        int[] deviceNumbers = {200, 400, 600, 800, 1000};
        int[] TrajPerDevices = {100, 200, 300, 400, 500};
        double[] gridWidths = {0.001, 0.003, 0.005, 0.007, 0.009};
        double[] ratioOfQ = {0.001, 0.002, 0.003, 0.004, 0.005};
        int[] parameterK = {10, 20, 30, 40, 50};

        List<Double> filterTimeWithPruningWithLocalBoostVaryingDeviceNumber = new ArrayList<>();
        List<Double> refineTimeWithPruningWithLocalBoostVaryingDeviceNumber = new ArrayList<>();
        List<Double> filterTimeWithPruningWithLocalBoostVaryingTrajNumber = new ArrayList<>();
        List<Double> refineTimeWithPruningWithLocalBoostVaryingTrajNumber = new ArrayList<>();
        List<Double> filterTimeWithPruningWithLocalBoostVaryingGridWidth = new ArrayList<>();
        List<Double> refineTimeWithPruningWithLocalBoostVaryingGridWidth = new ArrayList<>();
        List<Double> filterTimeWithPruningWithLocalBoostVaryingQ = new ArrayList<>();
        List<Double> refineTimeWithPruningWithLocalBoostVaryingQ = new ArrayList<>();
        List<Double> filterTimeWithPruningWithLocalBoostVaryingK = new ArrayList<>();
        List<Double> refineTimeWithPruningWithLocalBoostVaryingK = new ArrayList<>();


        List<Double> filterTimeWithPruningWithoutLocalBoostVaryingDeviceNumber = new ArrayList<>();
        List<Double> refineTimeWithPruningWithoutLocalBoostVaryingDeviceNumber = new ArrayList<>();
        List<Double> filterTimeWithPruningWithoutLocalBoostVaryingTrajNumber = new ArrayList<>();
        List<Double> refineTimeWithPruningWithoutLocalBoostVaryingTrajNumber = new ArrayList<>();
        List<Double> filterTimeWithPruningWithoutLocalBoostVaryingGridWidth = new ArrayList<>();
        List<Double> refineTimeWithPruningWithoutLocalBoostVaryingGridWidth = new ArrayList<>();
        List<Double> filterTimeWithPruningWithoutLocalBoostVaryingQ = new ArrayList<>();
        List<Double> refineTimeWithPruningWithoutLocalBoostVaryingQ = new ArrayList<>();
        List<Double> filterTimeWithPruningWithoutLocalBoostVaryingK = new ArrayList<>();
        List<Double> refineTimeWithPruningWithoutLocalBoostVaryingK = new ArrayList<>();


        List<Double> queryTimeWithoutPruningWithoutLocalBoostVaryingDeviceNumber = new ArrayList<>();
        List<Double> queryTimeWithoutPruningWithoutLocalBoostVaryingTrajNumber = new ArrayList<>();
        List<Double> queryTimeWithoutPruningWithoutLocalBoostVaryingGridWidth = new ArrayList<>();
        List<Double> queryTimeWithoutPruningWithoutLocalBoostVaryingQ = new ArrayList<>();
        List<Double> queryTimeWithoutPruningWithoutLocalBoostVaryingK = new ArrayList<>();


        List<Double> queryTimeWithoutPruningWithLocalBoostVaryingDeviceNumber = new ArrayList<>();
        List<Double> queryTimeWithoutPruningWithLocalBoostVaryingTrajNumber = new ArrayList<>();
        List<Double> queryTimeWithoutPruningWithLocalBoostVaryingGridWidth = new ArrayList<>();
        List<Double> queryTimeWithoutPruningWithLocalBoostVaryingQ = new ArrayList<>();
        List<Double> queryTimeWithoutPruningWithLocalBoostVaryingK = new ArrayList<>();
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(9090), 0);
        Server s = new Server(pois,1);
        GridIndexHandler gridIndexHandler = new GridIndexHandler(s, false);
        TopKQueryHandler topKQueryHandler = new TopKQueryHandler(s, true);

        httpServer.createContext("/doGridIndexBuilding", gridIndexHandler);
        httpServer.createContext("/doTopKQuery", topKQueryHandler);
        httpServer.setExecutor(Executors.newFixedThreadPool(1));
        httpServer.start();

        // 每种参数组合跑10轮，取均值

/*
        // 不剪枝，移动端不加速
        // 1. 设备数量变化
        for(int i=0;i<deviceNumbers.length;i++){
            int device_number = deviceNumbers[i];
            topKQueryHandler.reset(false);
            double totalQueryTime = 0;
            for(int j=0;j<10;j++) {
                List<MobileDevice> devices = initMobileDevices(device_number, defaultTrajPerDevice, s.getGlobalGridIndex()
                        ,mappedTrajectoryList, false, false);

                for (MobileDevice d : devices) {
                    d.indexCheckinData();
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }

                List<Hop> Q = extractQueryFromCheckinData(mappedTrajectoryList,  defaultRatio);
                topKQueryHandler.topKQuery(Q,defaultK);
                double beforeQuery = (double) System.currentTimeMillis() / 1000;
                for (MobileDevice d : devices) {
                    d.postRequest("http://localhost:9090/doTopKQuery", "begin:" + d.getDeviceId());
                    d.postRequest("http://localhost:9090/doTopKQuery", d.getDeviceId() + ":" + d.getRisk());
                }
                List<Map.Entry<String,Double>> result = topKQueryHandler.getAllResults()
                        .entrySet().stream().sorted(Comparator.comparing(Map.Entry::getValue)).collect(Collectors.toList());
                double endQuery = (double) System.currentTimeMillis() / 1000;
                topKQueryHandler.reset(false);
                gridIndexHandler.reset(false);
                totalQueryTime += endQuery - beforeQuery;
            }

            queryTimeWithoutPruningWithoutLocalBoostVaryingDeviceNumber.add(totalQueryTime/10);
            logger.info(String.format("Total query time %2f seconds ", totalQueryTime/10));
        }

        // 2. 移动端轨迹数量变化
        for(int i=0;i<TrajPerDevices.length;i++){
            int traj_number = TrajPerDevices[i];
            topKQueryHandler.reset(false);
            double totalQueryTime = 0;
            for(int j=0;j<10;j++) {
                List<MobileDevice> devices = initMobileDevices(defaultDeviceNumber, traj_number, s.getGlobalGridIndex()
                        ,mappedTrajectoryList, false, false);

                for (MobileDevice d : devices) {
                    d.indexCheckinData();
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }

                List<Hop> Q = extractQueryFromCheckinData(mappedTrajectoryList,defaultRatio);
                topKQueryHandler.topKQuery(Q,defaultK);
                double beforeQuery = (double) System.currentTimeMillis() / 1000;
                for (MobileDevice d : devices) {
                    d.postRequest("http://localhost:9090/doTopKQuery", "begin:" + d.getDeviceId());
                    d.postRequest("http://localhost:9090/doTopKQuery", d.getDeviceId() + ":" + d.getRisk());
                }
                List<Map.Entry<String,Double>> result = topKQueryHandler.getAllResults()
                        .entrySet().stream().sorted(Comparator.comparing(Map.Entry::getValue)).collect(Collectors.toList());
                double endQuery = (double) System.currentTimeMillis() / 1000;
                topKQueryHandler.reset(false);
                gridIndexHandler.reset(false);
                totalQueryTime += endQuery - beforeQuery;
            }

            queryTimeWithoutPruningWithoutLocalBoostVaryingTrajNumber.add(totalQueryTime/10);
            logger.info(String.format("Total query time %2f seconds ", totalQueryTime/10));
        }

        // 3. 网格宽度变化
        for(int i=0;i<gridWidths.length;i++){
            double grid_width = gridWidths[i];
            s.reset(grid_width);
            topKQueryHandler.reset(false);
            double totalQueryTime = 0;
            for(int j=0;j<10;j++) {
                List<MobileDevice> devices = initMobileDevices(defaultDeviceNumber, defaultTrajPerDevice, s.getGlobalGridIndex()
                        , mappedTrajectoryList, false, false);
                for (MobileDevice d : devices) {
                    d.indexCheckinData();
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }

                List<Hop> Q = extractQueryFromCheckinData(mappedTrajectoryList,defaultRatio);
                topKQueryHandler.topKQuery(Q,defaultK);
                double beforeQuery = (double) System.currentTimeMillis() / 1000;
                for (MobileDevice d : devices) {
                    d.postRequest("http://localhost:9090/doTopKQuery", "begin:" + d.getDeviceId());
                    d.postRequest("http://localhost:9090/doTopKQuery", d.getDeviceId() + ":" + d.getRisk());
                }
                List<Map.Entry<String,Double>> result = topKQueryHandler.getAllResults()
                        .entrySet().stream().sorted(Comparator.comparing(Map.Entry::getValue)).collect(Collectors.toList());
                double endQuery = (double) System.currentTimeMillis() / 1000;
                topKQueryHandler.reset(false);
                gridIndexHandler.reset(false);
                totalQueryTime += endQuery - beforeQuery;
            }

            queryTimeWithoutPruningWithoutLocalBoostVaryingGridWidth.add(totalQueryTime/10);
            logger.info(String.format("Total query time %2f seconds ", totalQueryTime/10));
        }

        s.reset(Settings.latGridWidth);

        // 4. Q变化
        for(int i=0;i<ratioOfQ.length;i++){
            double ratio_q = ratioOfQ[i];
            topKQueryHandler.reset(false);
            double totalQueryTime = 0;
            for(int j=0;j<10;j++) {
                List<MobileDevice> devices = initMobileDevices(defaultDeviceNumber, defaultTrajPerDevice, s.getGlobalGridIndex()
                        ,mappedTrajectoryList, false, false);

                for (MobileDevice d : devices) {
                    d.indexCheckinData();
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }

                List<Hop> Q = extractQueryFromCheckinData(mappedTrajectoryList,ratio_q);
                topKQueryHandler.topKQuery(Q,defaultK);
                double beforeQuery = (double) System.currentTimeMillis() / 1000;
                for (MobileDevice d : devices) {
                    d.postRequest("http://localhost:9090/doTopKQuery", "begin:" + d.getDeviceId());
                    d.postRequest("http://localhost:9090/doTopKQuery", d.getDeviceId() + ":" + d.getRisk());
                }
                List<Map.Entry<String,Double>> result = topKQueryHandler.getAllResults()
                        .entrySet().stream().sorted(Comparator.comparing(Map.Entry::getValue)).collect(Collectors.toList());
                double endQuery = (double) System.currentTimeMillis() / 1000;
                topKQueryHandler.reset(false);
                gridIndexHandler.reset(false);
                totalQueryTime += endQuery - beforeQuery;
            }

            queryTimeWithoutPruningWithoutLocalBoostVaryingQ.add(totalQueryTime/10);
            logger.info(String.format("Total query time %2f seconds ", totalQueryTime/10));
        }

        // 5. K变化
        for(int i=0;i<parameterK.length;i++){
            int k = parameterK[i];
            topKQueryHandler.reset(false);
            double totalQueryTime = 0;
            for(int j=0;j<10;j++) {
                List<MobileDevice> devices = initMobileDevices(defaultDeviceNumber, defaultTrajPerDevice, s.getGlobalGridIndex()
                        ,mappedTrajectoryList, false, false);

                for (MobileDevice d : devices) {
                    d.indexCheckinData();
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }

                List<Hop> Q = extractQueryFromCheckinData(mappedTrajectoryList,defaultRatio);
                topKQueryHandler.topKQuery(Q,k);
                double beforeQuery = (double) System.currentTimeMillis() / 1000;
                for (MobileDevice d : devices) {
                    d.postRequest("http://localhost:9090/doTopKQuery", "begin:" + d.getDeviceId());
                    d.postRequest("http://localhost:9090/doTopKQuery", d.getDeviceId() + ":" + d.getRisk());
                }
                List<Map.Entry<String,Double>> result = topKQueryHandler.getAllResults()
                        .entrySet().stream().sorted(Comparator.comparing(Map.Entry::getValue)).collect(Collectors.toList());
                double endQuery = (double) System.currentTimeMillis() / 1000;
                topKQueryHandler.reset(false);
                gridIndexHandler.reset(false);
                totalQueryTime += endQuery - beforeQuery;
            }

            queryTimeWithoutPruningWithoutLocalBoostVaryingK.add(totalQueryTime/10);
            logger.info(String.format("Total query time %2f seconds ", totalQueryTime/10));
        }


        //不剪枝，移动端加速
        // 1. 设备数量变化
        for(int i=0;i<deviceNumbers.length;i++){
            int device_number = deviceNumbers[i];
            topKQueryHandler.reset(false);
            double totalQueryTime = 0;
            for(int j=0;j<10;j++) {
                List<MobileDevice> devices = initMobileDevices(device_number, defaultTrajPerDevice, s.getGlobalGridIndex()
                        ,mappedTrajectoryList, true, false);
                for (MobileDevice d : devices) {
                    d.indexCheckinData();
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }

                List<Hop> Q = extractQueryFromCheckinData(mappedTrajectoryList,defaultRatio);
                topKQueryHandler.topKQuery(Q,defaultK);
                double beforeQuery = (double) System.currentTimeMillis() / 1000;
                for (MobileDevice d : devices) {
                    d.postRequest("http://localhost:9090/doTopKQuery", "begin:" + d.getDeviceId());
                    d.postRequest("http://localhost:9090/doTopKQuery", d.getDeviceId() + ":" + d.getRisk());
                }
                List<Map.Entry<String,Double>> result = topKQueryHandler.getAllResults()
                        .entrySet().stream().sorted(Comparator.comparing(Map.Entry::getValue)).collect(Collectors.toList());
                double endQuery = (double) System.currentTimeMillis() / 1000;
                topKQueryHandler.reset(false);
                gridIndexHandler.reset(false);
                totalQueryTime += endQuery - beforeQuery;
            }

            queryTimeWithoutPruningWithLocalBoostVaryingDeviceNumber.add(totalQueryTime/10);
            logger.info(String.format("Total query time %2f seconds ", totalQueryTime/10));

        }

        // 2. 移动端轨迹数量变化
        for(int i=0;i<TrajPerDevices.length;i++){
            int traj_number = TrajPerDevices[i];
            topKQueryHandler.reset(false);
            double totalQueryTime = 0;
            for(int j=0;j<10;j++) {
                List<MobileDevice> devices = initMobileDevices(defaultDeviceNumber, traj_number, s.getGlobalGridIndex()
                        ,mappedTrajectoryList, true, false);
                for (MobileDevice d : devices) {
                    d.indexCheckinData();
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }

                List<Hop> Q = extractQueryFromCheckinData(mappedTrajectoryList,defaultRatio);
                topKQueryHandler.topKQuery(Q,defaultK);
                double beforeQuery = (double) System.currentTimeMillis() / 1000;
                for (MobileDevice d : devices) {
                    d.postRequest("http://localhost:9090/doTopKQuery", "begin:" + d.getDeviceId());
                    d.postRequest("http://localhost:9090/doTopKQuery", d.getDeviceId() + ":" + d.getRisk());
                }
                List<Map.Entry<String,Double>> result = topKQueryHandler.getAllResults()
                        .entrySet().stream().sorted(Comparator.comparing(Map.Entry::getValue)).collect(Collectors.toList());
                double endQuery = (double) System.currentTimeMillis() / 1000;
                topKQueryHandler.reset(false);
                gridIndexHandler.reset(false);
                totalQueryTime += endQuery - beforeQuery;
            }

            queryTimeWithoutPruningWithLocalBoostVaryingTrajNumber.add(totalQueryTime/10);
            logger.info(String.format("Total query time %2f seconds ", totalQueryTime/10));
        }

        // 3. 网格宽度变化
        for(int i=0;i<gridWidths.length;i++){
            double grid_width = gridWidths[i];
            topKQueryHandler.reset(false);
            s.reset(grid_width);
            double totalQueryTime = 0;
            for(int j=0;j<10;j++) {
                List<MobileDevice> devices = initMobileDevices(defaultDeviceNumber, defaultTrajPerDevice, s.getGlobalGridIndex()
                        ,mappedTrajectoryList, true, false);

                for (MobileDevice d : devices) {
                    d.indexCheckinData();
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }

                List<Hop> Q = extractQueryFromCheckinData(mappedTrajectoryList,defaultRatio);
                topKQueryHandler.topKQuery(Q,defaultK);
                double beforeQuery = (double) System.currentTimeMillis() / 1000;
                for (MobileDevice d : devices) {
                    d.postRequest("http://localhost:9090/doTopKQuery", "begin:" + d.getDeviceId());
                    d.postRequest("http://localhost:9090/doTopKQuery", d.getDeviceId() + ":" + d.getRisk());
                }
                List<Map.Entry<String,Double>> result = topKQueryHandler.getAllResults()
                        .entrySet().stream().sorted(Comparator.comparing(Map.Entry::getValue)).collect(Collectors.toList());
                double endQuery = (double) System.currentTimeMillis() / 1000;
                topKQueryHandler.reset(false);
                gridIndexHandler.reset(false);
                totalQueryTime += endQuery - beforeQuery;
            }

            queryTimeWithoutPruningWithLocalBoostVaryingGridWidth.add(totalQueryTime/10);
            logger.info(String.format("Total query time %2f seconds ", totalQueryTime/10));
        }

        s.reset(Settings.latGridWidth);

        // 4. Q变化
        for(int i=0;i<ratioOfQ.length;i++){
            double ratio_q = ratioOfQ[i];
            topKQueryHandler.reset(false);
            double totalQueryTime = 0;
            for(int j=0;j<10;j++) {
                List<MobileDevice> devices = initMobileDevices(defaultDeviceNumber, defaultTrajPerDevice, s.getGlobalGridIndex()
                        ,mappedTrajectoryList, true, false);

                for (MobileDevice d : devices) {
                    d.indexCheckinData();
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }

                List<Hop> Q = extractQueryFromCheckinData(mappedTrajectoryList,ratio_q);
                topKQueryHandler.topKQuery(Q,defaultK);
                double beforeQuery = (double) System.currentTimeMillis() / 1000;
                for (MobileDevice d : devices) {
                    d.postRequest("http://localhost:9090/doTopKQuery", "begin:" + d.getDeviceId());
                    d.postRequest("http://localhost:9090/doTopKQuery", d.getDeviceId() + ":" + d.getRisk());
                }
                List<Map.Entry<String,Double>> result = topKQueryHandler.getAllResults()
                        .entrySet().stream().sorted(Comparator.comparing(Map.Entry::getValue)).collect(Collectors.toList());
                double endQuery = (double) System.currentTimeMillis() / 1000;
                topKQueryHandler.reset(false);
                gridIndexHandler.reset(false);
                totalQueryTime += endQuery - beforeQuery;
            }

            queryTimeWithoutPruningWithLocalBoostVaryingQ.add(totalQueryTime/10);
            logger.info(String.format("Total query time %2f seconds ", totalQueryTime/10));
        }

        // 5. K变化
        for(int i=0;i<parameterK.length;i++){
            int k = parameterK[i];
            topKQueryHandler.reset(false);
            double totalQueryTime = 0;
            for(int j=0;j<10;j++) {
                List<MobileDevice> devices = initMobileDevices(defaultDeviceNumber, defaultTrajPerDevice, s.getGlobalGridIndex()
                        ,mappedTrajectoryList, true, false);

                for (MobileDevice d : devices) {
                    d.indexCheckinData();
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }

                List<Hop> Q = extractQueryFromCheckinData(mappedTrajectoryList,defaultRatio);
                topKQueryHandler.topKQuery(Q,k);
                double beforeQuery = (double) System.currentTimeMillis() / 1000;
                for (MobileDevice d : devices) {
                    d.postRequest("http://localhost:9090/doTopKQuery", "begin:" + d.getDeviceId());
                    d.postRequest("http://localhost:9090/doTopKQuery", d.getDeviceId() + ":" + d.getRisk());
                }
                List<Map.Entry<String,Double>> result = topKQueryHandler.getAllResults()
                        .entrySet().stream().sorted(Comparator.comparing(Map.Entry::getValue)).collect(Collectors.toList());
                double endQuery = (double) System.currentTimeMillis() / 1000;
                topKQueryHandler.reset(false);
                gridIndexHandler.reset(false);
                totalQueryTime += endQuery - beforeQuery;
            }

            queryTimeWithoutPruningWithLocalBoostVaryingK.add(totalQueryTime/10);
            logger.info(String.format("Total query time %2f seconds ", totalQueryTime/10));
        }
*/

        // 剪枝，移动端不加速
        // 1. 设备数量变化
        for(int i=0;i<deviceNumbers.length;i++){
            int device_number = deviceNumbers[i];
            double totalFilterTime = 0;
            double totalRefineTime = 0;
            topKQueryHandler.reset(true);
            for(int j=0;j<10;j++) {


                List<MobileDevice> devices = initMobileDevices(device_number, defaultTrajPerDevice, s.getGlobalGridIndex()
                        ,mappedTrajectoryList, false, false);

                for (MobileDevice d : devices) {
                    d.indexCheckinData();
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }

                List<Hop> Q = extractQueryFromCheckinData(mappedTrajectoryList,defaultRatio);

                double beforeFilter = (double) System.currentTimeMillis() / 1000;
                topKQueryHandler.topKQuery(Q, defaultK);
                double afterFilter = (double) System.currentTimeMillis() / 1000;
                totalFilterTime += afterFilter - beforeFilter;

                double refineTime = 0;
                double beforeRefine = (double) System.currentTimeMillis() / 1000;
                Map<String, Double> candidates = topKQueryHandler.getCandidates();
                PriorityQueue<Double> result = new PriorityQueue<>(new Comparator<Double>() {
                    @Override
                    public int compare(Double o1, Double o2) {
                        if (o1 > o2) {
                            return 1;
                        } else if(o1.equals(o2)){
                            return 0;
                        }
                        else{
                            return -1;
                        }

                    }
                });
                List<Map.Entry<String, Double>> cans = candidates.entrySet().stream().sorted(
                        new Comparator<Map.Entry<String, Double>>() {
                            @Override
                            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                                if (o1.getValue() < o2.getValue()) {
                                    return 1;
                                } else if(o1.getValue().equals(o2.getValue())){
                                    return 0;
                                }
                                else{
                                    return -1;
                                }
                            }
                        }
                ).collect(Collectors.toList());

                List<MobileDevice> candidateDevices = cans.stream().map(d -> devices.get(Integer.parseInt(d.getKey()))).collect(Collectors.toList());

                if (candidates.size() < defaultK) {
                    double endRefine = (double) System.currentTimeMillis() / 1000;
                    refineTime += endRefine - beforeRefine;
                } else {
                    String mobileId;
                    for (MobileDevice d : candidateDevices) {
                        d.postRequest("http://localhost:9090/doTopKQuery", "begin:" + d.getDeviceId());
                        d.postRequest("http://localhost:9090/doTopKQuery", d.getDeviceId() + ":" + d.getRisk());
                        mobileId = d.getDeviceId();
                        result.add(topKQueryHandler.getAllResults().get(mobileId));
                        if (result.size()>=defaultK&&result.peek() > candidates.get(mobileId)) {
                            break;
                        }
                        while(result.size()>defaultK){
                            result.poll();
                        }
                    }

                    double endRefine = (double) System.currentTimeMillis() / 1000;
                    refineTime += endRefine - beforeRefine;

                }
                totalRefineTime += refineTime;

                gridIndexHandler.reset(false);
                topKQueryHandler.reset(true);
            }

            filterTimeWithPruningWithoutLocalBoostVaryingDeviceNumber.add(totalFilterTime/10);
            refineTimeWithPruningWithoutLocalBoostVaryingDeviceNumber.add(totalRefineTime/10);
            logger.info(String.format("Total filter time %2f seconds ", totalFilterTime/10));
            logger.info(String.format("Total refine time %2f seconds ", totalRefineTime/10));


        }

        // 2. 移动端轨迹数量变化
        for(int i=0;i<TrajPerDevices.length;i++){
            int traj_number = TrajPerDevices[i];
            double totalFilterTime = 0;
            double totalRefineTime = 0;
            topKQueryHandler.reset(true);
            for(int j=0;j<10;j++) {
                List<MobileDevice> devices = initMobileDevices(defaultDeviceNumber, traj_number, s.getGlobalGridIndex()
                        ,mappedTrajectoryList, false, false);

                for (MobileDevice d : devices) {
                    d.indexCheckinData();
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }

                List<Hop> Q = extractQueryFromCheckinData(mappedTrajectoryList,defaultRatio);

                double beforeFilter = (double) System.currentTimeMillis() / 1000;
                topKQueryHandler.topKQuery(Q, defaultK);
                double afterFilter = (double) System.currentTimeMillis() / 1000;
                totalFilterTime += afterFilter - beforeFilter;

                double refineTime = 0;
                double beforeRefine = (double) System.currentTimeMillis() / 1000;
                Map<String, Double> candidates = topKQueryHandler.getCandidates();
                PriorityQueue<Double> result = new PriorityQueue<>(new Comparator<Double>() {
                    @Override
                    public int compare(Double o1, Double o2) {
                        if (o1 > o2) {
                            return 1;
                        }
                        else if(o1.equals(o2)){
                            return 0;
                        }
                        else {
                            return -1;
                        }

                    }
                });
                List<Map.Entry<String, Double>> cans = candidates.entrySet().stream().sorted(
                        new Comparator<Map.Entry<String, Double>>() {
                            @Override
                            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                                if (o1.getValue() < o2.getValue()) {
                                    return 1;
                                }
                                else if(o1.getValue().equals(o2.getValue())){
                                    return 0;
                                }
                                else {
                                    return -1;
                                }
                            }
                        }
                ).collect(Collectors.toList());

                List<MobileDevice> candidateDevices = cans.stream().map(d -> devices.get(Integer.parseInt(d.getKey()))).collect(Collectors.toList());

                if (candidates.size() < defaultK) {
                    double endRefine = (double) System.currentTimeMillis() / 1000;
                    refineTime += endRefine - beforeRefine;
                } else {
                    String mobileId;
                    for (MobileDevice d : candidateDevices) {
                        d.postRequest("http://localhost:9090/doTopKQuery", "begin:" + d.getDeviceId());
                        d.postRequest("http://localhost:9090/doTopKQuery", d.getDeviceId() + ":" + d.getRisk());
                        mobileId = d.getDeviceId();
                        result.add(topKQueryHandler.getAllResults().get(mobileId));
                        if (result.size()>=defaultK&&result.peek() > candidates.get(mobileId)) {
                            break;
                        }
                        while(result.size()>defaultK){
                            result.poll();
                        }

                    }

                    double endRefine = (double) System.currentTimeMillis() / 1000;
                    refineTime += endRefine - beforeRefine;

                }
                totalRefineTime += refineTime;

                gridIndexHandler.reset(false);
                topKQueryHandler.reset(true);
            }

            filterTimeWithPruningWithoutLocalBoostVaryingTrajNumber.add(totalFilterTime/10);
            refineTimeWithPruningWithoutLocalBoostVaryingTrajNumber.add(totalRefineTime/10);
            logger.info(String.format("Total filter time %2f seconds ", totalFilterTime/10));
            logger.info(String.format("Total refine time %2f seconds ", totalRefineTime/10));


        }

        // 3. 网格宽度变化
        for(int i=0;i<gridWidths.length;i++){
            double gridWidth = gridWidths[i];
            double totalFilterTime = 0;
            double totalRefineTime = 0;

            for(int j=0;j<10;j++) {

                s.reset(gridWidth);

                List<MobileDevice> devices = initMobileDevices(defaultDeviceNumber, defaultTrajPerDevice, s.getGlobalGridIndex()
                        ,mappedTrajectoryList, false, false);
                for (MobileDevice d : devices) {
                    d.indexCheckinData();
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }

                List<Hop> Q = extractQueryFromCheckinData(mappedTrajectoryList,defaultRatio);

                double beforeFilter = (double) System.currentTimeMillis() / 1000;
                topKQueryHandler.topKQuery(Q, defaultK);
                double afterFilter = (double) System.currentTimeMillis() / 1000;
                totalFilterTime += afterFilter - beforeFilter;

                double refineTime = 0;
                double beforeRefine = (double) System.currentTimeMillis() / 1000;
                Map<String, Double> candidates = topKQueryHandler.getCandidates();
                PriorityQueue<Double> result = new PriorityQueue<>(new Comparator<Double>() {
                    @Override
                    public int compare(Double o1, Double o2) {
                        if (o1 > o2) {
                            return 1;
                        }
                        else if(o1.equals(o2)){
                            return 0;
                        }
                        else {
                            return -1;
                        }

                    }
                });
                List<Map.Entry<String, Double>> cans = candidates.entrySet().stream().sorted(
                        new Comparator<Map.Entry<String, Double>>() {
                            @Override
                            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                                if (o1.getValue() < o2.getValue()) {
                                    return 1;
                                }
                                else if(o1.getValue().equals(o2.getValue())){
                                    return 0;
                                }
                                else {
                                    return -1;
                                }
                            }
                        }
                ).collect(Collectors.toList());

                List<MobileDevice> candidateDevices = cans.stream().map(d -> devices.get(Integer.parseInt(d.getKey()))).collect(Collectors.toList());

                if (candidates.size() < defaultK) {
                    double endRefine = (double) System.currentTimeMillis() / 1000;
                    refineTime += endRefine - beforeRefine;
                } else {
                    String mobileId;
                    for (MobileDevice d : candidateDevices) {
                        d.postRequest("http://localhost:9090/doTopKQuery", "begin:" + d.getDeviceId());
                        d.postRequest("http://localhost:9090/doTopKQuery", d.getDeviceId() + ":" + d.getRisk());
                        mobileId = d.getDeviceId();
                        result.add(topKQueryHandler.getAllResults().get(mobileId));
                        if (result.size()>=defaultK&&result.peek() > candidates.get(mobileId)) {
                            break;
                        }
                        while(result.size()>defaultK){
                            result.poll();
                        }
                    }

                    double endRefine = (double) System.currentTimeMillis() / 1000;
                    refineTime += endRefine - beforeRefine;

                }
                totalRefineTime += refineTime;

                gridIndexHandler.reset(false);
                topKQueryHandler.reset(true);
            }

            filterTimeWithPruningWithoutLocalBoostVaryingGridWidth.add(totalFilterTime/10);
            refineTimeWithPruningWithoutLocalBoostVaryingGridWidth.add(totalRefineTime/10);
            logger.info(String.format("Total filter time %2f seconds ", totalFilterTime/10));
            logger.info(String.format("Total refine time %2f seconds ", totalRefineTime/10));


        }

        s.reset(Settings.latGridWidth);

        // 4. Q变化
        for(int i=0;i<ratioOfQ.length;i++){
            double ratio_q = ratioOfQ[i];
            double totalFilterTime = 0;
            double totalRefineTime = 0;

            for(int j=0;j<10;j++) {

                List<MobileDevice> devices = initMobileDevices(defaultDeviceNumber, defaultTrajPerDevice, s.getGlobalGridIndex()
                        ,mappedTrajectoryList, false, false);

                for (MobileDevice d : devices) {
                    d.indexCheckinData();
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }

                List<Hop> Q =extractQueryFromCheckinData(mappedTrajectoryList,ratio_q);

                double beforeFilter = (double) System.currentTimeMillis() / 1000;
                topKQueryHandler.topKQuery(Q, defaultK);
                double afterFilter = (double) System.currentTimeMillis() / 1000;
                totalFilterTime += afterFilter - beforeFilter;

                double refineTime = 0;
                double beforeRefine = (double) System.currentTimeMillis() / 1000;
                Map<String, Double> candidates = topKQueryHandler.getCandidates();
                PriorityQueue<Double> result = new PriorityQueue<>(new Comparator<Double>() {
                    @Override
                    public int compare(Double o1, Double o2) {
                        if (o1 > o2) {
                            return 1;
                        }
                        else if (o1.equals(o2)){
                            return 0;
                        }
                        else {
                            return -1;
                        }

                    }
                });
                List<Map.Entry<String, Double>> cans = candidates.entrySet().stream().sorted(
                        new Comparator<Map.Entry<String, Double>>() {
                            @Override
                            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                                if (o1.getValue() < o2.getValue()) {
                                    return 1;
                                }
                                else if (o1.getValue().equals(o2.getValue())){
                                    return 0;
                                }
                                else {
                                    return -1;
                                }
                            }
                        }
                ).collect(Collectors.toList());

                List<MobileDevice> candidateDevices = cans.stream().map(d -> devices.get(Integer.parseInt(d.getKey()))).collect(Collectors.toList());

                if (candidates.size() < defaultK) {
                    double endRefine = (double) System.currentTimeMillis() / 1000;
                    refineTime += endRefine - beforeRefine;
                } else {
                    String mobileId;
                    for (MobileDevice d : candidateDevices) {
                        d.postRequest("http://localhost:9090/doTopKQuery", "begin:" + d.getDeviceId());
                        d.postRequest("http://localhost:9090/doTopKQuery", d.getDeviceId() + ":" + d.getRisk());
                        mobileId = d.getDeviceId();
                        result.add(topKQueryHandler.getAllResults().get(mobileId));
                        if (result.size()>=defaultK&&result.peek() > candidates.get(mobileId)) {
                            break;
                        }
                        while(result.size()>defaultK){
                            result.poll();
                        }
                    }

                    double endRefine = (double) System.currentTimeMillis() / 1000;
                    refineTime += endRefine - beforeRefine;

                }
                totalRefineTime += refineTime;

                gridIndexHandler.reset(false);
                topKQueryHandler.reset(true);
            }

            filterTimeWithPruningWithoutLocalBoostVaryingQ.add(totalFilterTime/10);
            refineTimeWithPruningWithoutLocalBoostVaryingQ.add(totalRefineTime/10);
            logger.info(String.format("Total filter time %2f seconds ", totalFilterTime/10));
            logger.info(String.format("Total refine time %2f seconds ", totalRefineTime/10));


        }


        // 5. K变化
        for(int i=0;i<parameterK.length;i++){
            int k = parameterK[i];
            double totalFilterTime = 0;
            double totalRefineTime = 0;

            for(int j=0;j<10;j++) {

                List<MobileDevice> devices = initMobileDevices(defaultDeviceNumber, defaultTrajPerDevice, s.getGlobalGridIndex()
                        ,mappedTrajectoryList, false, false);

                for (MobileDevice d : devices) {
                    d.indexCheckinData();
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }

                List<Hop> Q = extractQueryFromCheckinData(mappedTrajectoryList,defaultRatio);

                double beforeFilter = (double) System.currentTimeMillis() / 1000;
                topKQueryHandler.topKQuery(Q, k);
                double afterFilter = (double) System.currentTimeMillis() / 1000;
                totalFilterTime += afterFilter - beforeFilter;

                double refineTime = 0;
                double beforeRefine = (double) System.currentTimeMillis() / 1000;
                Map<String, Double> candidates = topKQueryHandler.getCandidates();
                PriorityQueue<Double> result = new PriorityQueue<>(new Comparator<Double>() {
                    @Override
                    public int compare(Double o1, Double o2) {
                        if (o1 > o2) {
                            return 1;
                        }
                        else if(o1.equals(o2)){
                            return 0;
                        }
                        else {
                            return -1;
                        }

                    }
                });
                List<Map.Entry<String, Double>> cans = candidates.entrySet().stream().sorted(
                        new Comparator<Map.Entry<String, Double>>() {
                            @Override
                            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                                if (o1.getValue() < o2.getValue()) {
                                    return 1;
                                }
                                else if(o1.getValue().equals(o2.getValue())){
                                    return 0;
                                }
                                else {
                                    return -1;
                                }
                            }
                        }
                ).collect(Collectors.toList());

                List<MobileDevice> candidateDevices = cans.stream().map(d -> devices.get(Integer.parseInt(d.getKey()))).collect(Collectors.toList());

                if (candidates.size() < k) {
                    double endRefine = (double) System.currentTimeMillis() / 1000;
                    refineTime += endRefine - beforeRefine;
                } else {
                    String mobileId;
                    for (MobileDevice d : candidateDevices) {
                        d.postRequest("http://localhost:9090/doTopKQuery", "begin:" + d.getDeviceId());
                        d.postRequest("http://localhost:9090/doTopKQuery", d.getDeviceId() + ":" + d.getRisk());
                        mobileId = d.getDeviceId();
                        result.add(topKQueryHandler.getAllResults().get(mobileId));
                        if (result.size()>=k&&result.peek() > candidates.get(mobileId)) {
                            break;
                        }
                        while(result.size()>k){
                            result.poll();
                        }
                    }

                    double endRefine = (double) System.currentTimeMillis() / 1000;
                    refineTime += endRefine - beforeRefine;

                }
                totalRefineTime += refineTime;

                gridIndexHandler.reset(false);
                topKQueryHandler.reset(true);
            }

            filterTimeWithPruningWithoutLocalBoostVaryingK.add(totalFilterTime/10);
            refineTimeWithPruningWithoutLocalBoostVaryingK.add(totalRefineTime/10);
            logger.info(String.format("Total filter time %2f seconds ", totalFilterTime/10));
            logger.info(String.format("Total refine time %2f seconds ", totalRefineTime/10));


        }



        //剪枝，移动端加速
        // 1. 设备数量变化
        for(int i=0;i<deviceNumbers.length;i++){
            int device_number = deviceNumbers[i];
            double totalFilterTime = 0;
            double totalRefineTime = 0;

            for(int j=0;j<10;j++) {
                List<MobileDevice> devices = initMobileDevices(device_number, defaultTrajPerDevice, s.getGlobalGridIndex()
                        ,mappedTrajectoryList, true, false);

                for (MobileDevice d : devices) {
                    d.indexCheckinData();
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }

                List<Hop> Q = extractQueryFromCheckinData(mappedTrajectoryList,defaultRatio);

                double beforeFilter = (double) System.currentTimeMillis() / 1000;
                topKQueryHandler.topKQuery(Q, defaultK);
                double afterFilter = (double) System.currentTimeMillis() / 1000;
                totalFilterTime += afterFilter - beforeFilter;

                double refineTime = 0;
                double beforeRefine = (double) System.currentTimeMillis() / 1000;
                Map<String, Double> candidates = topKQueryHandler.getCandidates();
                PriorityQueue<Double> result = new PriorityQueue<>(new Comparator<Double>() {
                    @Override
                    public int compare(Double o1, Double o2) {
                        if (o1 > o2) {
                            return 1;
                        }
                        else if(o1.equals(o2)){
                            return 0;
                        }
                        else {
                            return -1;
                        }

                    }
                });
                List<Map.Entry<String, Double>> cans = candidates.entrySet().stream().sorted(
                        new Comparator<Map.Entry<String, Double>>() {
                            @Override
                            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                                if (o1.getValue() < o2.getValue()) {
                                    return 1;
                                }
                                else if(o1.getValue().equals(o2.getValue())){
                                    return 0;
                                }
                                else {
                                    return -1;
                                }
                            }
                        }
                ).collect(Collectors.toList());

                List<MobileDevice> candidateDevices = cans.stream().map(d -> devices.get(Integer.parseInt(d.getKey()))).collect(Collectors.toList());

                if (candidates.size() < defaultK) {
                    double endRefine = (double) System.currentTimeMillis() / 1000;
                    refineTime += endRefine - beforeRefine;
                } else {
                    String mobileId;
                    for (MobileDevice d : candidateDevices) {
                        d.postRequest("http://localhost:9090/doTopKQuery", "begin:" + d.getDeviceId());
                        d.postRequest("http://localhost:9090/doTopKQuery", d.getDeviceId() + ":" + d.getRisk());
                        mobileId = d.getDeviceId();
                        result.add(topKQueryHandler.getAllResults().get(mobileId));
                        if (result.size()>=defaultK&&result.peek() > candidates.get(mobileId)) {
                            break;
                        }
                        while(result.size()>defaultK){
                            result.poll();
                        }
                    }

                    double endRefine = (double) System.currentTimeMillis() / 1000;
                    refineTime += endRefine - beforeRefine;

                }
                totalRefineTime += refineTime;

                gridIndexHandler.reset(false);
                topKQueryHandler.reset(true);
            }

            filterTimeWithPruningWithLocalBoostVaryingDeviceNumber.add(totalFilterTime/10);
            refineTimeWithPruningWithLocalBoostVaryingDeviceNumber.add(totalRefineTime/10);
            logger.info(String.format("Total filter time %2f seconds ", totalFilterTime/10));
            logger.info(String.format("Total refine time %2f seconds ", totalRefineTime/10));


        }


        // 2. 移动端轨迹数量变化
        for(int i=0;i<TrajPerDevices.length;i++){
            int traj_number = TrajPerDevices[i];
            double totalFilterTime = 0;
            double totalRefineTime = 0;

            for(int j=0;j<10;j++) {

                List<MobileDevice> devices = initMobileDevices(defaultDeviceNumber, traj_number, s.getGlobalGridIndex()
                        ,mappedTrajectoryList, true, false);

                for (MobileDevice d : devices) {
                    d.indexCheckinData();
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }

                List<Hop> Q = extractQueryFromCheckinData(mappedTrajectoryList,defaultRatio);

                double beforeFilter = (double) System.currentTimeMillis() / 1000;
                topKQueryHandler.topKQuery(Q, defaultK);
                double afterFilter = (double) System.currentTimeMillis() / 1000;
                totalFilterTime += afterFilter - beforeFilter;

                double refineTime = 0;
                double beforeRefine = (double) System.currentTimeMillis() / 1000;
                Map<String, Double> candidates = topKQueryHandler.getCandidates();
                PriorityQueue<Double> result = new PriorityQueue<>(new Comparator<Double>() {
                    @Override
                    public int compare(Double o1, Double o2) {
                        if (o1 > o2) {
                            return 1;
                        }
                        else if (o1.equals(o2)){
                            return 0;
                        }
                        else {
                            return -1;
                        }

                    }
                });
                List<Map.Entry<String, Double>> cans = candidates.entrySet().stream().sorted(
                        new Comparator<Map.Entry<String, Double>>() {
                            @Override
                            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                                if (o1.getValue() < o2.getValue()) {
                                    return 1;
                                }
                                else if(o1.getValue().equals(o2.getValue())){
                                    return 0;
                                }
                                else {
                                    return -1;
                                }
                            }
                        }
                ).collect(Collectors.toList());

                List<MobileDevice> candidateDevices = cans.stream().map(d -> devices.get(Integer.parseInt(d.getKey()))).collect(Collectors.toList());

                if (candidates.size() < defaultK) {
                    double endRefine = (double) System.currentTimeMillis() / 1000;
                    refineTime += endRefine - beforeRefine;
                } else {
                    String mobileId;
                    for (MobileDevice d : candidateDevices) {
                        d.postRequest("http://localhost:9090/doTopKQuery", "begin:" + d.getDeviceId());
                        d.postRequest("http://localhost:9090/doTopKQuery", d.getDeviceId() + ":" + d.getRisk());
                        mobileId = d.getDeviceId();
                        result.add(topKQueryHandler.getAllResults().get(mobileId));
                        if (result.size()>=defaultK&&result.peek() > candidates.get(mobileId)) {
                            break;
                        }
                        while(result.size()>defaultK){
                            result.poll();
                        }
                    }

                    double endRefine = (double) System.currentTimeMillis() / 1000;
                    refineTime += endRefine - beforeRefine;

                }
                totalRefineTime += refineTime;

                gridIndexHandler.reset(false);
                topKQueryHandler.reset(true);
            }

            filterTimeWithPruningWithLocalBoostVaryingTrajNumber.add(totalFilterTime/10);
            refineTimeWithPruningWithLocalBoostVaryingTrajNumber.add(totalRefineTime/10);
            logger.info(String.format("Total filter time %2f seconds ", totalFilterTime/10));
            logger.info(String.format("Total refine time %2f seconds ", totalRefineTime/10));


        }


        // 3. 网格宽度变化
        for(int i=0;i<gridWidths.length;i++){
            double gridWidth = gridWidths[i];
            double totalFilterTime = 0;
            double totalRefineTime = 0;

            for(int j=0;j<10;j++) {

                s.reset(gridWidth);
                List<MobileDevice> devices = initMobileDevices(defaultDeviceNumber, defaultTrajPerDevice, s.getGlobalGridIndex()
                        ,mappedTrajectoryList, true, false);

                for (MobileDevice d : devices) {
                    d.indexCheckinData();
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }

                List<Hop> Q = extractQueryFromCheckinData(mappedTrajectoryList,defaultRatio);

                double beforeFilter = (double) System.currentTimeMillis() / 1000;
                topKQueryHandler.topKQuery(Q, defaultK);
                double afterFilter = (double) System.currentTimeMillis() / 1000;
                totalFilterTime += afterFilter - beforeFilter;

                double refineTime = 0;
                double beforeRefine = (double) System.currentTimeMillis() / 1000;
                Map<String, Double> candidates = topKQueryHandler.getCandidates();
                PriorityQueue<Double> result = new PriorityQueue<>(new Comparator<Double>() {
                    @Override
                    public int compare(Double o1, Double o2) {
                        if (o1 > o2) {
                            return 1;
                        }
                        else if (o1.equals(o2)){
                            return 0;
                        }
                        else {
                            return -1;
                        }

                    }
                });
                List<Map.Entry<String, Double>> cans = candidates.entrySet().stream().sorted(
                        new Comparator<Map.Entry<String, Double>>() {
                            @Override
                            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                                if (o1.getValue() < o2.getValue()) {
                                    return 1;
                                }
                                else if(o1.getValue().equals(o2.getValue())){
                                    return 0;
                                }
                                else {
                                    return -1;
                                }
                            }
                        }
                ).collect(Collectors.toList());

                List<MobileDevice> candidateDevices = cans.stream().map(d -> devices.get(Integer.parseInt(d.getKey()))).collect(Collectors.toList());

                if (candidates.size() < defaultK) {
                    double endRefine = (double) System.currentTimeMillis() / 1000;
                    refineTime += endRefine - beforeRefine;
                } else {
                    String mobileId;
                    for (MobileDevice d : candidateDevices) {
                        d.postRequest("http://localhost:9090/doTopKQuery", "begin:" + d.getDeviceId());
                        d.postRequest("http://localhost:9090/doTopKQuery", d.getDeviceId() + ":" + d.getRisk());
                        mobileId = d.getDeviceId();
                        result.add(topKQueryHandler.getAllResults().get(mobileId));
                        if (result.size()>=defaultK&&result.peek() > candidates.get(mobileId)) {
                            break;
                        }
                        while(result.size()>defaultK){
                            result.poll();
                        }
                    }

                    double endRefine = (double) System.currentTimeMillis() / 1000;
                    refineTime += endRefine - beforeRefine;

                }
                totalRefineTime += refineTime;

                gridIndexHandler.reset(false);
                topKQueryHandler.reset(true);
            }

            filterTimeWithPruningWithLocalBoostVaryingGridWidth.add(totalFilterTime/10);
            refineTimeWithPruningWithLocalBoostVaryingGridWidth.add(totalRefineTime/10);
            logger.info(String.format("Total filter time %2f seconds ", totalFilterTime/10));
            logger.info(String.format("Total refine time %2f seconds ", totalRefineTime/10));


        }

        s.reset(Settings.latGridWidth);

        // 4. Q变化
        for(int i=0;i<ratioOfQ.length;i++){
            double ratio_q = ratioOfQ[i];
            double totalFilterTime = 0;
            double totalRefineTime = 0;

            for(int j=0;j<10;j++) {

                List<MobileDevice> devices = initMobileDevices(defaultDeviceNumber, defaultTrajPerDevice, s.getGlobalGridIndex()
                        ,mappedTrajectoryList, true, false);

                for (MobileDevice d : devices) {
                    d.indexCheckinData();
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }

                List<Hop> Q = extractQueryFromCheckinData(mappedTrajectoryList,ratio_q);

                double beforeFilter = (double) System.currentTimeMillis() / 1000;
                topKQueryHandler.topKQuery(Q, defaultK);
                double afterFilter = (double) System.currentTimeMillis() / 1000;
                totalFilterTime += afterFilter - beforeFilter;

                double refineTime = 0;
                double beforeRefine = (double) System.currentTimeMillis() / 1000;
                Map<String, Double> candidates = topKQueryHandler.getCandidates();
                PriorityQueue<Double> result = new PriorityQueue<>(new Comparator<Double>() {
                    @Override
                    public int compare(Double o1, Double o2) {
                        if (o1 > o2) {
                            return 1;
                        }
                        else if(o1.equals(o2)){
                            return 0;
                        }
                        else {
                            return -1;
                        }

                    }
                });
                List<Map.Entry<String, Double>> cans = candidates.entrySet().stream().sorted(
                        new Comparator<Map.Entry<String, Double>>() {
                            @Override
                            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                                if (o1.getValue() < o2.getValue()) {
                                    return 1;
                                }
                                else if(o1.getValue().equals(o2.getValue())){
                                    return 0;
                                }
                                else {
                                    return -1;
                                }
                            }
                        }
                ).collect(Collectors.toList());

                List<MobileDevice> candidateDevices = cans.stream().map(d -> devices.get(Integer.parseInt(d.getKey()))).collect(Collectors.toList());

                if (candidates.size() < defaultK) {
                    double endRefine = (double) System.currentTimeMillis() / 1000;
                    refineTime += endRefine - beforeRefine;
                } else {
                    String mobileId;
                    for (MobileDevice d : candidateDevices) {
                        d.postRequest("http://localhost:9090/doTopKQuery", "begin:" + d.getDeviceId());
                        d.postRequest("http://localhost:9090/doTopKQuery", d.getDeviceId() + ":" + d.getRisk());
                        mobileId = d.getDeviceId();
                        result.add(topKQueryHandler.getAllResults().get(mobileId));
                        if (result.size()>=defaultK&&result.peek() > candidates.get(mobileId)) {
                            break;
                        }
                        while(result.size()>defaultK){
                            result.poll();
                        }
                    }

                    double endRefine = (double) System.currentTimeMillis() / 1000;
                    refineTime += endRefine - beforeRefine;

                }
                totalRefineTime += refineTime;

                gridIndexHandler.reset(false);
                topKQueryHandler.reset(true);
            }

            filterTimeWithPruningWithLocalBoostVaryingQ.add(totalFilterTime/10);
            refineTimeWithPruningWithLocalBoostVaryingQ.add(totalRefineTime/10);
            logger.info(String.format("Total filter time %2f seconds ", totalFilterTime/10));
            logger.info(String.format("Total refine time %2f seconds ", totalRefineTime/10));


        }


        // 5. K变化
        for(int i=0;i<parameterK.length;i++){
            int k = parameterK[i];
            double totalFilterTime = 0;
            double totalRefineTime = 0;

            for(int j=0;j<10;j++) {

                List<MobileDevice> devices = initMobileDevices(defaultDeviceNumber, defaultTrajPerDevice, s.getGlobalGridIndex()
                        ,mappedTrajectoryList, true, false);

                for (MobileDevice d : devices) {
                    d.indexCheckinData();
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }

                List<Hop> Q = extractQueryFromCheckinData(mappedTrajectoryList,defaultRatio);

                double beforeFilter = (double) System.currentTimeMillis() / 1000;
                topKQueryHandler.topKQuery(Q, k);
                double afterFilter = (double) System.currentTimeMillis() / 1000;
                totalFilterTime += afterFilter - beforeFilter;

                double refineTime = 0;
                double beforeRefine = (double) System.currentTimeMillis() / 1000;
                Map<String, Double> candidates = topKQueryHandler.getCandidates();
                PriorityQueue<Double> result = new PriorityQueue<>(new Comparator<Double>() {
                    @Override
                    public int compare(Double o1, Double o2) {
                        if (o1 > o2) {
                            return 1;
                        }
                        else if(o1.equals(o2)){
                            return 0;
                        }
                        else {
                            return -1;
                        }

                    }
                });
                List<Map.Entry<String, Double>> cans = candidates.entrySet().stream().sorted(
                        new Comparator<Map.Entry<String, Double>>() {
                            @Override
                            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                                if (o1.getValue() < o2.getValue()) {
                                    return 1;
                                }
                                else if(o1.getValue().equals(o2.getValue())){
                                    return 0;
                                }
                                else {
                                    return -1;
                                }
                            }
                        }
                ).collect(Collectors.toList());

                List<MobileDevice> candidateDevices = cans.stream().map(d -> devices.get(Integer.parseInt(d.getKey()))).collect(Collectors.toList());

                if (candidates.size() < k) {
                    double endRefine = (double) System.currentTimeMillis() / 1000;
                    refineTime += endRefine - beforeRefine;
                } else {
                    String mobileId;
                    for (MobileDevice d : candidateDevices) {
                        d.postRequest("http://localhost:9090/doTopKQuery", "begin:" + d.getDeviceId());
                        d.postRequest("http://localhost:9090/doTopKQuery", d.getDeviceId() + ":" + d.getRisk());
                        mobileId = d.getDeviceId();
                        result.add(topKQueryHandler.getAllResults().get(mobileId));
                        if (result.size()>=k&&result.peek() > candidates.get(mobileId)) {
                            break;
                        }
                        while(result.size()>k){
                            result.poll();
                        }
                    }

                    double endRefine = (double) System.currentTimeMillis() / 1000;
                    refineTime += endRefine - beforeRefine;

                }
                totalRefineTime += refineTime;

                gridIndexHandler.reset(false);
                topKQueryHandler.reset(true);
            }

            filterTimeWithPruningWithLocalBoostVaryingK.add(totalFilterTime/10);
            refineTimeWithPruningWithLocalBoostVaryingK.add(totalRefineTime/10);
            logger.info(String.format("Total filter time %2f seconds ", totalFilterTime/10));
            logger.info(String.format("Total refine time %2f seconds ", totalRefineTime/10));


        }


        System.out.println(queryTimeWithoutPruningWithoutLocalBoostVaryingDeviceNumber);
        System.out.println(queryTimeWithoutPruningWithoutLocalBoostVaryingTrajNumber);
        System.out.println(queryTimeWithoutPruningWithoutLocalBoostVaryingGridWidth);
        System.out.println(queryTimeWithoutPruningWithoutLocalBoostVaryingQ);
        System.out.println(queryTimeWithoutPruningWithoutLocalBoostVaryingK);

        System.out.println(queryTimeWithoutPruningWithLocalBoostVaryingDeviceNumber);
        System.out.println(queryTimeWithoutPruningWithLocalBoostVaryingTrajNumber);
        System.out.println(queryTimeWithoutPruningWithLocalBoostVaryingGridWidth);
        System.out.println(queryTimeWithoutPruningWithLocalBoostVaryingQ);
        System.out.println(queryTimeWithoutPruningWithLocalBoostVaryingK);

        System.out.println(filterTimeWithPruningWithoutLocalBoostVaryingDeviceNumber);
        System.out.println(refineTimeWithPruningWithoutLocalBoostVaryingDeviceNumber);
        System.out.println(filterTimeWithPruningWithoutLocalBoostVaryingTrajNumber);
        System.out.println(refineTimeWithPruningWithoutLocalBoostVaryingTrajNumber);
        System.out.println(filterTimeWithPruningWithoutLocalBoostVaryingGridWidth);
        System.out.println(refineTimeWithPruningWithoutLocalBoostVaryingGridWidth);
        System.out.println(filterTimeWithPruningWithoutLocalBoostVaryingQ);
        System.out.println(refineTimeWithPruningWithoutLocalBoostVaryingQ);
        System.out.println(filterTimeWithPruningWithoutLocalBoostVaryingK);
        System.out.println(refineTimeWithPruningWithoutLocalBoostVaryingK);


        System.out.println(filterTimeWithPruningWithLocalBoostVaryingDeviceNumber);
        System.out.println(refineTimeWithPruningWithLocalBoostVaryingDeviceNumber);
        System.out.println(filterTimeWithPruningWithLocalBoostVaryingTrajNumber);
        System.out.println(refineTimeWithPruningWithLocalBoostVaryingTrajNumber);
        System.out.println(filterTimeWithPruningWithLocalBoostVaryingGridWidth);
        System.out.println(refineTimeWithPruningWithLocalBoostVaryingGridWidth);
        System.out.println(filterTimeWithPruningWithLocalBoostVaryingQ);
        System.out.println(refineTimeWithPruningWithLocalBoostVaryingQ);
        System.out.println(filterTimeWithPruningWithLocalBoostVaryingK);
        System.out.println(refineTimeWithPruningWithLocalBoostVaryingK);


        httpServer.stop(0);


    }

}
