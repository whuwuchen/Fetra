package whu.edu.totemdb.STCSim.TestCase;

import com.sun.net.httpserver.HttpServer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import whu.edu.totemdb.STCSim.Base.Hop;
import whu.edu.totemdb.STCSim.Base.MappedTrajectory;
import whu.edu.totemdb.STCSim.Base.POI;
import whu.edu.totemdb.STCSim.Base.RawTrajectory;
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

public class PartialPOIMatchTest {
    public static Log logger = LogFactory.getLog(PartialPOIMatchTest.class);
    
    public static void main(String[] args) throws IOException {
        partialPOIMatchTestOfBeiJing();
    }

    public static void partialPOIMatchTestOfBeiJing() throws IOException {

        List<POI> pois = DataLoader.loadPOIDataBeiJing();
        List<RawTrajectory> rawTrajectories = DataLoader.loadRawTrajDataBeiJing();

        int defaultDeviceNumber = 600;
        int defaultTrajPerDevice = 200;
        double defaultRatio = 0.003;
        int defaultK = 30;

        int[] deviceNumbers = {200, 400, 600, 800, 1000};
        int[] TrajPerDevices = {100, 200, 300, 400, 500};
        double[] gridWidths = {0.001, 0.003, 0.005, 0.007, 0.009};
        double[] ratioOfQ = {0.001, 0.002, 0.003, 0.004, 0.005};
        int[] parameterK = {10, 20, 30, 40, 50};

        List<Double> topkqueryWithoutPruningVaryingDeviceNumber = new ArrayList<>();
        List<Double> topkqueryWithPruningVaryingDebiceNumber = new ArrayList<>();

        List<Double> topkqueryWithoutPruningVaryingLocalTrajNumber = new ArrayList<>();
        List<Double> topkqueryWithPruningVaryingLocalTrajNumber = new ArrayList<>();

        List<Double> topkqueryWithoutPruningVaryingGridWidth = new ArrayList<>();
        List<Double> topkqueryWithPruningVaryingGridWidth = new ArrayList<>();

        List<Double> topkqueryWithoutPruningVaryingQ = new ArrayList<>();
        List<Double> topkqueryWithPruningVaryingQ = new ArrayList<>();

        List<Double> topkqueryWithoutPruningVaryingk = new ArrayList<>();
        List<Double> topkqueryWithPruningVaryingk = new ArrayList<>();


        HttpServer httpServer = HttpServer.create(new InetSocketAddress(9090), 0);
        Server s = new Server(pois);
        GridIndexHandler gridIndexHandler = new GridIndexHandler(s, false);
        TopKQueryHandler topKQueryHandler = new TopKQueryHandler(s, true);
        topKQueryHandler.partialMapping = true;
        httpServer.createContext("/doGridIndexBuilding", gridIndexHandler);
        httpServer.createContext("/doTopKQuery", topKQueryHandler);
        httpServer.setExecutor(Executors.newFixedThreadPool(1));
        httpServer.start();

        // 不剪枝
        // 1.设备数量变化
        for(int i=0;i<deviceNumbers.length;i++){
            int device_number = deviceNumbers[i];

            SPExtrator sp = new SPExtrator(s.getGlobalGridIndex(), pois);
            double totalQueryTime = 0;
            for(int j=0;j<10;j++) {
                List<MobileDevice> devices = initMobileDevices(device_number, defaultTrajPerDevice, s.getGlobalGridIndex()
                        , sp, rawTrajectories, false, false,true);

                for (MobileDevice d : devices) {
                    d.partialLocalIndex();
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

            topkqueryWithoutPruningVaryingDeviceNumber.add(totalQueryTime/10);
            logger.info(String.format("Total query time %2f seconds ", totalQueryTime/10));

        }

        // 2.移动端轨迹数量变化
        for(int i=0;i<TrajPerDevices.length;i++){
            int traj_number = TrajPerDevices[i];

            SPExtrator sp = new SPExtrator(s.getGlobalGridIndex(), pois);
            double totalQueryTime = 0;
            for(int j=0;j<10;j++) {
                List<MobileDevice> devices = initMobileDevices(defaultDeviceNumber, traj_number, s.getGlobalGridIndex()
                        , sp, rawTrajectories, false, false,true);

                for (MobileDevice d : devices) {
                    d.partialLocalIndex();
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

            topkqueryWithoutPruningVaryingLocalTrajNumber.add(totalQueryTime/10);
            logger.info(String.format("Total query time %2f seconds ", totalQueryTime/10));

        }


        // 3.网格大小变化
        for(int i=0;i<gridWidths.length;i++){
            double gridWidth = gridWidths[i];
            s.reset(gridWidth);
            SPExtrator sp = new SPExtrator(s.getGlobalGridIndex(), pois);
            double totalQueryTime = 0;
            for(int j=0;j<10;j++) {
                List<MobileDevice> devices = initMobileDevices(defaultDeviceNumber, defaultTrajPerDevice, s.getGlobalGridIndex()
                        , sp, rawTrajectories, false, false,true);

                for (MobileDevice d : devices) {
                    d.partialLocalIndex();
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

            topkqueryWithoutPruningVaryingGridWidth.add(totalQueryTime/10);
            logger.info(String.format("Total query time %2f seconds ", totalQueryTime/10));

        }

        s.reset(Settings.latGridWidth);

        // 4.查询轨迹大小变化
        for(int i=0;i<ratioOfQ.length;i++){
            double ratioQ = ratioOfQ[i];
            SPExtrator sp = new SPExtrator(s.getGlobalGridIndex(), pois);
            double totalQueryTime = 0;
            for(int j=0;j<10;j++) {
                List<MobileDevice> devices = initMobileDevices(defaultDeviceNumber, defaultTrajPerDevice, s.getGlobalGridIndex()
                        , sp, rawTrajectories, false, false,true);

                for (MobileDevice d : devices) {
                    d.partialLocalIndex();
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }

                List<Hop> Q = extractQuery(rawTrajectories, sp, ratioQ);


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

            topkqueryWithoutPruningVaryingQ.add(totalQueryTime/10);
            logger.info(String.format("Total query time %2f seconds ", totalQueryTime/10));

        }

        // 5.k变化
        for(int i=0;i<parameterK.length;i++){
            int k = parameterK[i];
            SPExtrator sp = new SPExtrator(s.getGlobalGridIndex(), pois);
            double totalQueryTime = 0;
            for(int j=0;j<10;j++) {
                List<MobileDevice> devices = initMobileDevices(defaultDeviceNumber, defaultTrajPerDevice, s.getGlobalGridIndex()
                        , sp, rawTrajectories, false, false,true);

                for (MobileDevice d : devices) {
                    d.partialLocalIndex();
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

            topkqueryWithoutPruningVaryingk.add(totalQueryTime/10);
            logger.info(String.format("Total query time %2f seconds ", totalQueryTime/10));

        }





        topKQueryHandler.partialMappingPruning=true;

        //剪枝
        // 1. 设备数量变化
        for(int i=0;i<deviceNumbers.length;i++){
            int device_number = deviceNumbers[i];
            double totalFilterTime = 0;
            double totalRefineTime = 0;

            for(int j=0;j<10;j++) {
                SPExtrator sp = new SPExtrator(s.getGlobalGridIndex(), pois);
                List<MobileDevice> devices = initMobileDevices(device_number, defaultTrajPerDevice, s.getGlobalGridIndex()
                        , sp, rawTrajectories, true, false,true);

                for (MobileDevice d : devices) {
                    d.partialLocalIndex();
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
                        if(result.size()>=defaultK){
                            if (result.peek() > candidates.get(mobileId)) {
                                break;
                            }
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

            double totalQueryTime = (totalFilterTime+totalRefineTime)/10;
            topkqueryWithPruningVaryingDebiceNumber.add(totalQueryTime);
            logger.info(String.format("Total filter time %2f seconds ", totalFilterTime/10));
            logger.info(String.format("Total refine time %2f seconds ", totalRefineTime/10));


        }

        // 2. 移动端轨迹数量变化
        for(int i=0;i<TrajPerDevices.length;i++){
            int trajnumber = TrajPerDevices[i];
            double totalFilterTime = 0;
            double totalRefineTime = 0;

            for(int j=0;j<10;j++) {
                SPExtrator sp = new SPExtrator(s.getGlobalGridIndex(), pois);
                List<MobileDevice> devices = initMobileDevices(defaultDeviceNumber, trajnumber, s.getGlobalGridIndex()
                        , sp, rawTrajectories, true, false,true);

                for (MobileDevice d : devices) {
                    d.partialLocalIndex();
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
                        if(result.size()>=defaultK){
                            if (result.peek() > candidates.get(mobileId)) {
                                break;
                            }
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

            double totalQueryTime = (totalFilterTime+totalRefineTime)/10;
            topkqueryWithPruningVaryingLocalTrajNumber.add(totalQueryTime);
            logger.info(String.format("Total filter time %2f seconds ", totalFilterTime/10));
            logger.info(String.format("Total refine time %2f seconds ", totalRefineTime/10));


        }

        // 3. 网格大小的变化
        for(int i=0;i<gridWidths.length;i++){
            double gridWidth = gridWidths[i];
            double totalFilterTime = 0;
            double totalRefineTime = 0;
            s.reset(gridWidth);
            for(int j=0;j<10;j++) {
                SPExtrator sp = new SPExtrator(s.getGlobalGridIndex(), pois);
                List<MobileDevice> devices = initMobileDevices(defaultDeviceNumber, defaultTrajPerDevice, s.getGlobalGridIndex()
                        , sp, rawTrajectories, true, false,true);

                for (MobileDevice d : devices) {
                    d.partialLocalIndex();
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
                        if(result.size()>=defaultK){
                            if (result.peek() > candidates.get(mobileId)) {
                                break;
                            }
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

            double totalQueryTime = (totalFilterTime+totalRefineTime)/10;
            topkqueryWithPruningVaryingGridWidth.add(totalQueryTime);
            logger.info(String.format("Total filter time %2f seconds ", totalFilterTime/10));
            logger.info(String.format("Total refine time %2f seconds ", totalRefineTime/10));


        }

        s.reset(Settings.latGridWidth);


        // 4. 查询轨迹大小的变化
        for(int i=0;i<ratioOfQ.length;i++){
            double ratioQ = ratioOfQ[i];
            double totalFilterTime = 0;
            double totalRefineTime = 0;

            for(int j=0;j<10;j++) {
                SPExtrator sp = new SPExtrator(s.getGlobalGridIndex(), pois);
                List<MobileDevice> devices = initMobileDevices(defaultDeviceNumber, defaultTrajPerDevice, s.getGlobalGridIndex()
                        , sp, rawTrajectories, true, false,true);

                for (MobileDevice d : devices) {
                    d.partialLocalIndex();
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }

                List<Hop> Q = extractQuery(rawTrajectories, sp, ratioQ);

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
                        if(result.size()>=defaultK){
                            if (result.peek() > candidates.get(mobileId)) {
                                break;
                            }
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

            double totalQueryTime = (totalFilterTime+totalRefineTime)/10;
            topkqueryWithPruningVaryingQ.add(totalQueryTime);
            logger.info(String.format("Total filter time %2f seconds ", totalFilterTime/10));
            logger.info(String.format("Total refine time %2f seconds ", totalRefineTime/10));


        }

        // 5. k的变化
        for(int i=0;i<parameterK.length;i++){
            int k = parameterK[i];
            double totalFilterTime = 0;
            double totalRefineTime = 0;

            for(int j=0;j<10;j++) {
                SPExtrator sp = new SPExtrator(s.getGlobalGridIndex(), pois);
                List<MobileDevice> devices = initMobileDevices(defaultDeviceNumber, defaultTrajPerDevice, s.getGlobalGridIndex()
                        , sp, rawTrajectories, true, false,true);

                for (MobileDevice d : devices) {
                    d.partialLocalIndex();
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
                        if(result.size()>=defaultK){
                            if (result.peek() > candidates.get(mobileId)) {
                                break;
                            }
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

            double totalQueryTime = (totalFilterTime+totalRefineTime)/10;
            topkqueryWithPruningVaryingk.add(totalQueryTime);
            logger.info(String.format("Total filter time %2f seconds ", totalFilterTime/10));
            logger.info(String.format("Total refine time %2f seconds ", totalRefineTime/10));


        }

        System.out.println(topkqueryWithoutPruningVaryingDeviceNumber);
        System.out.println(topkqueryWithoutPruningVaryingLocalTrajNumber);
        System.out.println(topkqueryWithoutPruningVaryingGridWidth);
        System.out.println(topkqueryWithoutPruningVaryingQ);
        System.out.println(topkqueryWithoutPruningVaryingk);

        System.out.println(topkqueryWithPruningVaryingDebiceNumber);
        System.out.println(topkqueryWithPruningVaryingLocalTrajNumber);
        System.out.println(topkqueryWithPruningVaryingGridWidth);
        System.out.println(topkqueryWithPruningVaryingQ);
        System.out.println(topkqueryWithPruningVaryingk);


    }

    public static List<MobileDevice> initMobileDevices(int deviceAmount,int rawTrajsPerDevice,GridIndex globalGridIndex,
                                                       SPExtrator sp, List<RawTrajectory> rawTrajectoryList,boolean hasLocalBoost,boolean indexCompressed,boolean partialMapping){
        List<MobileDevice> res = new ArrayList<>();
        for(int i=0;i<deviceAmount;i++){
            res.add(initMobile(i,deviceAmount,rawTrajsPerDevice,globalGridIndex,sp,rawTrajectoryList,hasLocalBoost,indexCompressed,partialMapping));
        }
        return res;
    }

    public static MobileDevice initMobile(int deviceNo, int deviceAmount, int rawTrajsPerDevice, GridIndex globalGridIndex,
                                          SPExtrator sp, List<RawTrajectory> rawTrajectoryList, boolean hasLocalBoost, boolean indexCompressed,boolean partialMapping){
        int count = deviceNo;
        int size = rawTrajectoryList.size();
        List<RawTrajectory> res = new ArrayList<>();
        for(int i=0;i<rawTrajsPerDevice;i++){
            count += deviceAmount;
            res.add(rawTrajectoryList.get(count%size));
        }

        return new MobileDevice(String.valueOf(deviceNo),globalGridIndex,res,sp,hasLocalBoost,indexCompressed,partialMapping);
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

}
