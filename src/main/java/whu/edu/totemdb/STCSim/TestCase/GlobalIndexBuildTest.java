package whu.edu.totemdb.STCSim.TestCase;

import com.sun.net.httpserver.HttpServer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import whu.edu.totemdb.STCSim.Base.*;
import whu.edu.totemdb.STCSim.Device.Handlers.GridIndexHandler;
import whu.edu.totemdb.STCSim.Device.MobileDevice;
import whu.edu.totemdb.STCSim.Device.Server;
import whu.edu.totemdb.STCSim.Index.GridIndex;
import whu.edu.totemdb.STCSim.Settings;
import whu.edu.totemdb.STCSim.StayPointDetection.SPExtrator;
import whu.edu.totemdb.STCSim.Utils.DataLoader;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

// consistent grid index query
// In this query, server and mobiles have same grid index structure
public class GlobalIndexBuildTest {
    // The range bound in Resources/BeiJing/Beijing.osm.pbf : 116.0800004246831,116.76997952394142,39.680010222196806,40.17999355152235
    public static List<POI> pois ;

    public static List<MobileDevice> devices;
    public static String osmFile = "Resources/BeiJing/Beijing.osm.pbf";
    public static GridIndex globalGridIndex;

    public static Log logger = LogFactory.getLog(FederatedTopKQueryTest.class);
    public static void main(String[] args) throws IOException {
        // 测试索引构建时间，包括传输时间和服务端构建时间，考虑压缩和不压缩两种情况
        // 测试参数包括设备数量w，每个设备的轨迹数量|s|，网格宽度$delta_d$
        testIndexBuildingOfNewYork();


    }



    public static void testIndexBuildingOfBeijing() throws IOException {

        int deviceNumber = 1000;
        int rawtrajsPerDevice = 100;

        int defaultDeviceNumber = 600;
        int defaultTrajPerDevice = 200;

        int[] deviceNumbers = {200,400,600,800,1000};
        int[] TrajPerDevices = {100,200,300,400,500};
        double[] gridWidths = {0.001,0.003,0.005,0.007,0.009};

        List<Double> buildTimeVaryingDeviceNumberWithCompressedIndex = new ArrayList<>();
        List<Double> buildTimeVaryingDeviceNumberWithoutCompressedIndex = new ArrayList<>();
        List<Double> buildTimeVaryingTrajNumberWithCompressedIndex = new ArrayList<>();
        List<Double> buildTimeVaryingTrajNumberWithoutCompressedIndex = new ArrayList<>();

        List<Double> buildTimeVaryingGridWidthWithCompressedIndex = new ArrayList<>();
        List<Double> buildTimeVaryingGridWidthWithoutCompressedIndex = new ArrayList<>();

        List<POI> pois = DataLoader.loadPOIDataBeiJing();
        List<RawTrajectory> rawTrajectories = DataLoader.loadRawTrajDataBeiJing();


        Server s = new Server(pois);

        SPExtrator sp = new SPExtrator(s.getGlobalGridIndex(),pois);

        List<MobileDevice> devices = initMobileDevices(deviceNumber,rawtrajsPerDevice,s.getGlobalGridIndex()
                ,sp,rawTrajectories,true,false);
        GridIndexHandler gridIndexHandler = new GridIndexHandler(s,false);
        // TopKQueryHandler topKQueryHandler = new TopKQueryHandler(s,true);

        HttpServer httpServer = HttpServer.create(new InetSocketAddress(9090), 0);
        httpServer.createContext("/doGridIndexBuilding", gridIndexHandler);
        // httpServer.createContext("/doTopKQuery",topKQueryHandler);

        httpServer.setExecutor(Executors.newFixedThreadPool(1));
        httpServer.start();





        // 测试设备数量的变化,基于不压缩索引传输
        for(int i=0;i<deviceNumbers.length;i++)
        {

            deviceNumber = deviceNumbers[i];
            devices = initMobileDevices(deviceNumber,defaultTrajPerDevice,s.getGlobalGridIndex(),sp,rawTrajectories,true,false);

            for (MobileDevice d : devices) {
                d.matchRawTrajectories(Settings.radius / 1000);
                d.indexMappedTraj();
            }

            double totalIndexBuildTime = 0;
            double totalIndexTransferTime = 0;

            for(int j=0;j<10;j++) {
                double beforeIndexBuild = (double) System.currentTimeMillis() / 1000;
                gridIndexHandler.reset(false);
                for (MobileDevice d : devices) {
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }
                double endIndexBuild = (double) System.currentTimeMillis() / 1000;
                double buildTimeOnServer = gridIndexHandler.getBuildTime();
                totalIndexBuildTime += buildTimeOnServer;
                totalIndexTransferTime += (endIndexBuild - beforeIndexBuild - buildTimeOnServer);
            }
            logger.info(String.format("Global index build time is %2f", (totalIndexBuildTime/10)));
            logger.info(String.format("Global index transfer time is %2f", (totalIndexTransferTime/10)));

            buildTimeVaryingDeviceNumberWithoutCompressedIndex.add(totalIndexBuildTime/10);
            buildTimeVaryingDeviceNumberWithoutCompressedIndex.add(totalIndexTransferTime/10);
        }

        // 测试设备数量的变化,基于压缩索引传输
        for(int i=0;i<deviceNumbers.length;i++)
        {
            deviceNumber = deviceNumbers[i];
            devices = initMobileDevices(deviceNumber,defaultTrajPerDevice,s.getGlobalGridIndex(),sp,rawTrajectories,true,true);

            for (MobileDevice d : devices) {
                d.matchRawTrajectories(Settings.radius / 1000);
                d.indexMappedTraj();
            }

            double totalIndexBuildTime = 0;
            double totalIndexTransferTime = 0;
            for(int j=0;j<10;j++) {
                double beforeIndexBuild = (double) System.currentTimeMillis() / 1000;
                gridIndexHandler.reset(true);
                for (MobileDevice d : devices) {
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }
                double endIndexBuild = (double) System.currentTimeMillis() / 1000;
                double buildTimeOnServer = gridIndexHandler.getBuildTime();
                totalIndexBuildTime+=buildTimeOnServer;
                totalIndexTransferTime+=(endIndexBuild-beforeIndexBuild-buildTimeOnServer);
            }
            logger.info(String.format("Global index build time is %2f", (totalIndexBuildTime/10)));
            logger.info(String.format("Global index transfer time is %2f", (totalIndexTransferTime/10)));

            buildTimeVaryingDeviceNumberWithCompressedIndex.add(totalIndexBuildTime/10);
            buildTimeVaryingDeviceNumberWithCompressedIndex.add(totalIndexTransferTime/10);

        }


        // 测试轨迹数量的变化，基于不压缩索引传输
        for(int i=0;i<TrajPerDevices.length;i++)
        {
            rawtrajsPerDevice = TrajPerDevices[i];
            devices = initMobileDevices(defaultDeviceNumber,rawtrajsPerDevice,s.getGlobalGridIndex(),sp,rawTrajectories,true,false);

            for (MobileDevice d : devices) {
                d.matchRawTrajectories(Settings.radius / 1000);
                d.indexMappedTraj();
            }

            double totalIndexBuildTime = 0;
            double totalIndexTransferTime = 0;

            for(int j=0;j<10;j++) {
                double beforeIndexBuild = (double) System.currentTimeMillis() / 1000;
                gridIndexHandler.reset(false);
                for (MobileDevice d : devices) {
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }
                double endIndexBuild = (double) System.currentTimeMillis() / 1000;
                double buildTimeOnServer = gridIndexHandler.getBuildTime();

                totalIndexBuildTime += buildTimeOnServer;
                totalIndexTransferTime += endIndexBuild-beforeIndexBuild-buildTimeOnServer;
            }

            logger.info(String.format("Global index build time is %2f", (totalIndexBuildTime/10)));
            logger.info(String.format("Global index transfer time is %2f", (totalIndexTransferTime/10)));

            buildTimeVaryingTrajNumberWithoutCompressedIndex.add(totalIndexBuildTime/10);
            buildTimeVaryingTrajNumberWithoutCompressedIndex.add(totalIndexTransferTime/10);

        }

        // 测试轨迹数量的变化,基于压缩索引传输
        for(int i=0;i<TrajPerDevices.length;i++)
        {
            rawtrajsPerDevice = TrajPerDevices[i];
            devices = initMobileDevices(defaultDeviceNumber,rawtrajsPerDevice,s.getGlobalGridIndex(),sp,rawTrajectories,true,true);

            for (MobileDevice d : devices) {
                d.matchRawTrajectories(Settings.radius / 1000);
                d.indexMappedTraj();
            }

            double totalIndexBuildTime = 0;
            double totalIndexTransferTime = 0;
            for(int j=0;j<10;j++) {
                double beforeIndexBuild = (double) System.currentTimeMillis() / 1000;
                gridIndexHandler.reset(true);
                for (MobileDevice d : devices) {
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }
                double endIndexBuild = (double) System.currentTimeMillis() / 1000;
                double buildTimeOnServer = gridIndexHandler.getBuildTime();
                totalIndexBuildTime+=buildTimeOnServer;
                totalIndexTransferTime+=endIndexBuild-beforeIndexBuild-buildTimeOnServer;
            }


            logger.info(String.format("Global index build time is %2f", (totalIndexBuildTime/10)));
            logger.info(String.format("Global index transfer time is %2f", (totalIndexTransferTime/10)));

            buildTimeVaryingTrajNumberWithCompressedIndex.add(totalIndexBuildTime/10);
            buildTimeVaryingTrajNumberWithCompressedIndex.add(totalIndexTransferTime/10);

        }


        // 测试网格大小的变化，基于不压缩索引传输
        for(int i=0;i<gridWidths.length;i++){
            double gridWidth = gridWidths[i];
            s.reset(gridWidth);
            sp = new SPExtrator(s.getGlobalGridIndex(),pois);
            devices = initMobileDevices(defaultDeviceNumber,defaultTrajPerDevice,s.getGlobalGridIndex(),sp,rawTrajectories,true,false);

            for (MobileDevice d : devices) {
                d.matchRawTrajectories(Settings.radius / 1000);
                d.indexMappedTraj();
            }


            double totalIndexBuildTime = 0;
            double totalIndexTransferTime = 0;
            for(int j=0;j<10;j++) {
                double beforeIndexBuild = (double) System.currentTimeMillis() / 1000;
                gridIndexHandler.reset(false);
                for (MobileDevice d : devices) {
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }
                double endIndexBuild = (double) System.currentTimeMillis() / 1000;
                double buildTimeOnServer = gridIndexHandler.getBuildTime();
                totalIndexBuildTime+=buildTimeOnServer;
                totalIndexTransferTime+=endIndexBuild-beforeIndexBuild-buildTimeOnServer;

            }
            logger.info(String.format("Global index build time is %2f", (totalIndexBuildTime/10)));
            logger.info(String.format("Global index transfer time is %2f", (totalIndexTransferTime/10)));

            buildTimeVaryingGridWidthWithoutCompressedIndex.add(totalIndexBuildTime/10);
            buildTimeVaryingGridWidthWithoutCompressedIndex.add(totalIndexTransferTime/10);

        }




        // 测试网格大小的变化，基于压缩索引传输
        for(int i=0;i<gridWidths.length;i++){
            double gridWidth = gridWidths[i];
            s.reset(gridWidth);
            sp = new SPExtrator(s.getGlobalGridIndex(),pois);
            devices = initMobileDevices(defaultDeviceNumber,defaultTrajPerDevice,s.getGlobalGridIndex(),sp,rawTrajectories,true,true);

            for (MobileDevice d : devices) {
                d.matchRawTrajectories(Settings.radius / 1000);
                d.indexMappedTraj();
            }

            double totalIndexBuildTime = 0;
            double totalIndexTransferTime = 0;

            for(int j=0;j<10;j++) {
                double beforeIndexBuild = (double) System.currentTimeMillis() / 1000;
                gridIndexHandler.reset(true);
                for (MobileDevice d : devices) {
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }
                double endIndexBuild = (double) System.currentTimeMillis() / 1000;
                double buildTimeOnServer = gridIndexHandler.getBuildTime();
                totalIndexBuildTime+=buildTimeOnServer;
                totalIndexTransferTime+=endIndexBuild-beforeIndexBuild-buildTimeOnServer;
            }

            logger.info(String.format("Global index build time is %2f", (totalIndexBuildTime/10)));
            logger.info(String.format("Global index transfer time is %2f", (totalIndexTransferTime/10)));

            buildTimeVaryingGridWidthWithCompressedIndex.add(totalIndexBuildTime/10);
            buildTimeVaryingGridWidthWithCompressedIndex.add(totalIndexTransferTime/10);

        }


        System.out.println(buildTimeVaryingDeviceNumberWithCompressedIndex);
        System.out.println(buildTimeVaryingDeviceNumberWithoutCompressedIndex);
        System.out.println(buildTimeVaryingTrajNumberWithCompressedIndex);
        System.out.println(buildTimeVaryingTrajNumberWithoutCompressedIndex);
        System.out.println(buildTimeVaryingGridWidthWithCompressedIndex);
        System.out.println(buildTimeVaryingGridWidthWithoutCompressedIndex);


        /*File VarDNumFile = new File("./idx_varying_dnum.dat");
        if(!VarDNumFile.exists()){
            VarDNumFile.createNewFile();
        }
        else{
            VarDNumFile.delete();
            VarDNumFile.createNewFile();
        }
        */

        httpServer.stop(0);


    }


    public static void testIndexBuildingOfNewYork() throws IOException {

        int deviceNumber = 1000;
        int rawtrajsPerDevice = 100;

        int defaultDeviceNumber = 600;
        int defaultTrajPerDevice = 200;

        int[] deviceNumbers = {200,400,600,800,1000};
        int[] TrajPerDevices = {100,200,300,400,500};
        double[] gridWidths = {0.001,0.003,0.005,0.007,0.009};

        List<Double> buildTimeVaryingDeviceNumberWithCompressedIndex = new ArrayList<>();
        List<Double> buildTimeVaryingDeviceNumberWithoutCompressedIndex = new ArrayList<>();
        List<Double> buildTimeVaryingTrajNumberWithCompressedIndex = new ArrayList<>();
        List<Double> buildTimeVaryingTrajNumberWithoutCompressedIndex = new ArrayList<>();

        List<Double> buildTimeVaryingGridWidthWithCompressedIndex = new ArrayList<>();
        List<Double> buildTimeVaryingGridWidthWithoutCompressedIndex = new ArrayList<>();

        List<POI> pois = new ArrayList<>();

        List<MappedTrajectory> mappedTrajectories = new ArrayList<>();
        DataLoader.readTrajFileTokyo(mappedTrajectories,pois);

        Server s = new Server(pois,2);

        List<MobileDevice> devices;

        GridIndexHandler gridIndexHandler = new GridIndexHandler(s,false);
        // TopKQueryHandler topKQueryHandler = new TopKQueryHandler(s,true);

        HttpServer httpServer = HttpServer.create(new InetSocketAddress(9090), 0);
        httpServer.createContext("/doGridIndexBuilding", gridIndexHandler);
        // httpServer.createContext("/doTopKQuery",topKQueryHandler);

        httpServer.setExecutor(Executors.newFixedThreadPool(1));
        httpServer.start();





        // 测试设备数量的变化,基于不压缩索引传输
        for(int i=0;i<deviceNumbers.length;i++)
        {

            deviceNumber = deviceNumbers[i];
            devices = initMobileDevices(deviceNumber,defaultTrajPerDevice,s.getGlobalGridIndex(),mappedTrajectories,true,false);

            for (MobileDevice d : devices) {
                /*d.matchRawTrajectories(Settings.radius / 1000);
                d.indexMappedTraj();*/
                d.indexCheckinData();
            }

            double totalIndexBuildTime = 0;
            double totalIndexTransferTime = 0;

            for(int j=0;j<10;j++) {
                double beforeIndexBuild = (double) System.currentTimeMillis() / 1000;
                gridIndexHandler.reset(false);
                for (MobileDevice d : devices) {
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }
                double endIndexBuild = (double) System.currentTimeMillis() / 1000;
                double buildTimeOnServer = gridIndexHandler.getBuildTime();
                totalIndexBuildTime += buildTimeOnServer;
                totalIndexTransferTime += (endIndexBuild - beforeIndexBuild - buildTimeOnServer);
            }
            logger.info(String.format("Global index build time is %2f", (totalIndexBuildTime/10)));
            logger.info(String.format("Global index transfer time is %2f", (totalIndexTransferTime/10)));

            buildTimeVaryingDeviceNumberWithoutCompressedIndex.add(totalIndexBuildTime/10);
            buildTimeVaryingDeviceNumberWithoutCompressedIndex.add(totalIndexTransferTime/10);
        }

        // 测试设备数量的变化,基于压缩索引传输
        for(int i=0;i<deviceNumbers.length;i++)
        {
            deviceNumber = deviceNumbers[i];
            devices = initMobileDevices(deviceNumber,defaultTrajPerDevice,s.getGlobalGridIndex(),mappedTrajectories,true,true);

            for (MobileDevice d : devices) {
                /*d.matchRawTrajectories(Settings.radius / 1000);
                d.indexMappedTraj();*/
                d.indexCheckinData();
            }

            double totalIndexBuildTime = 0;
            double totalIndexTransferTime = 0;
            for(int j=0;j<10;j++) {
                double beforeIndexBuild = (double) System.currentTimeMillis() / 1000;
                gridIndexHandler.reset(true);
                for (MobileDevice d : devices) {
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }
                double endIndexBuild = (double) System.currentTimeMillis() / 1000;
                double buildTimeOnServer = gridIndexHandler.getBuildTime();
                totalIndexBuildTime+=buildTimeOnServer;
                totalIndexTransferTime+=(endIndexBuild-beforeIndexBuild-buildTimeOnServer);
            }
            logger.info(String.format("Global index build time is %2f", (totalIndexBuildTime/10)));
            logger.info(String.format("Global index transfer time is %2f", (totalIndexTransferTime/10)));

            buildTimeVaryingDeviceNumberWithCompressedIndex.add(totalIndexBuildTime/10);
            buildTimeVaryingDeviceNumberWithCompressedIndex.add(totalIndexTransferTime/10);

        }


        // 测试轨迹数量的变化，基于不压缩索引传输
        for(int i=0;i<TrajPerDevices.length;i++)
        {
            rawtrajsPerDevice = TrajPerDevices[i];
            devices = initMobileDevices(defaultDeviceNumber,rawtrajsPerDevice,s.getGlobalGridIndex(),mappedTrajectories,true,false);

            for (MobileDevice d : devices) {
                /*d.matchRawTrajectories(Settings.radius / 1000);
                d.indexMappedTraj();*/
                d.indexCheckinData();
            }

            double totalIndexBuildTime = 0;
            double totalIndexTransferTime = 0;

            for(int j=0;j<10;j++) {
                double beforeIndexBuild = (double) System.currentTimeMillis() / 1000;
                gridIndexHandler.reset(false);
                for (MobileDevice d : devices) {
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }
                double endIndexBuild = (double) System.currentTimeMillis() / 1000;
                double buildTimeOnServer = gridIndexHandler.getBuildTime();

                totalIndexBuildTime += buildTimeOnServer;
                totalIndexTransferTime += endIndexBuild-beforeIndexBuild-buildTimeOnServer;
            }

            logger.info(String.format("Global index build time is %2f", (totalIndexBuildTime/10)));
            logger.info(String.format("Global index transfer time is %2f", (totalIndexTransferTime/10)));

            buildTimeVaryingTrajNumberWithoutCompressedIndex.add(totalIndexBuildTime/10);
            buildTimeVaryingTrajNumberWithoutCompressedIndex.add(totalIndexTransferTime/10);

        }

        // 测试轨迹数量的变化,基于压缩索引传输
        for(int i=0;i<TrajPerDevices.length;i++)
        {
            rawtrajsPerDevice = TrajPerDevices[i];
            devices = initMobileDevices(defaultDeviceNumber,rawtrajsPerDevice,s.getGlobalGridIndex(),mappedTrajectories,true,true);

            for (MobileDevice d : devices) {
                /*d.matchRawTrajectories(Settings.radius / 1000);
                d.indexMappedTraj();*/
                d.indexCheckinData();
            }

            double totalIndexBuildTime = 0;
            double totalIndexTransferTime = 0;
            for(int j=0;j<10;j++) {
                double beforeIndexBuild = (double) System.currentTimeMillis() / 1000;
                gridIndexHandler.reset(true);
                for (MobileDevice d : devices) {
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }
                double endIndexBuild = (double) System.currentTimeMillis() / 1000;
                double buildTimeOnServer = gridIndexHandler.getBuildTime();
                totalIndexBuildTime+=buildTimeOnServer;
                totalIndexTransferTime+=endIndexBuild-beforeIndexBuild-buildTimeOnServer;
            }


            logger.info(String.format("Global index build time is %2f", (totalIndexBuildTime/10)));
            logger.info(String.format("Global index transfer time is %2f", (totalIndexTransferTime/10)));

            buildTimeVaryingTrajNumberWithCompressedIndex.add(totalIndexBuildTime/10);
            buildTimeVaryingTrajNumberWithCompressedIndex.add(totalIndexTransferTime/10);

        }


        // 测试网格大小的变化，基于不压缩索引传输
        for(int i=0;i<gridWidths.length;i++){
            double gridWidth = gridWidths[i];
            s.reset(gridWidth);
            devices = initMobileDevices(defaultDeviceNumber,defaultTrajPerDevice,s.getGlobalGridIndex(),mappedTrajectories,true,false);

            for (MobileDevice d : devices) {
                /*d.matchRawTrajectories(Settings.radius / 1000);
                d.indexMappedTraj();*/
                d.indexCheckinData();
            }


            double totalIndexBuildTime = 0;
            double totalIndexTransferTime = 0;
            for(int j=0;j<10;j++) {
                double beforeIndexBuild = (double) System.currentTimeMillis() / 1000;
                gridIndexHandler.reset(false);
                for (MobileDevice d : devices) {
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }
                double endIndexBuild = (double) System.currentTimeMillis() / 1000;
                double buildTimeOnServer = gridIndexHandler.getBuildTime();
                totalIndexBuildTime+=buildTimeOnServer;
                totalIndexTransferTime+=endIndexBuild-beforeIndexBuild-buildTimeOnServer;

            }
            logger.info(String.format("Global index build time is %2f", (totalIndexBuildTime/10)));
            logger.info(String.format("Global index transfer time is %2f", (totalIndexTransferTime/10)));

            buildTimeVaryingGridWidthWithoutCompressedIndex.add(totalIndexBuildTime/10);
            buildTimeVaryingGridWidthWithoutCompressedIndex.add(totalIndexTransferTime/10);

        }




        // 测试网格大小的变化，基于压缩索引传输
        for(int i=0;i<gridWidths.length;i++){
            double gridWidth = gridWidths[i];
            s.reset(gridWidth);
            devices = initMobileDevices(defaultDeviceNumber,defaultTrajPerDevice,s.getGlobalGridIndex(),mappedTrajectories,true,true);

            for (MobileDevice d : devices) {
                /*d.matchRawTrajectories(Settings.radius / 1000);
                d.indexMappedTraj();*/
                d.indexCheckinData();
            }

            double totalIndexBuildTime = 0;
            double totalIndexTransferTime = 0;

            for(int j=0;j<10;j++) {
                double beforeIndexBuild = (double) System.currentTimeMillis() / 1000;
                gridIndexHandler.reset(true);
                for (MobileDevice d : devices) {
                    d.sendLocalIndexToServer("http://localhost:9090/doGridIndexBuilding");
                }
                double endIndexBuild = (double) System.currentTimeMillis() / 1000;
                double buildTimeOnServer = gridIndexHandler.getBuildTime();
                totalIndexBuildTime+=buildTimeOnServer;
                totalIndexTransferTime+=endIndexBuild-beforeIndexBuild-buildTimeOnServer;
            }

            logger.info(String.format("Global index build time is %2f", (totalIndexBuildTime/10)));
            logger.info(String.format("Global index transfer time is %2f", (totalIndexTransferTime/10)));

            buildTimeVaryingGridWidthWithCompressedIndex.add(totalIndexBuildTime/10);
            buildTimeVaryingGridWidthWithCompressedIndex.add(totalIndexTransferTime/10);

        }


        System.out.println(buildTimeVaryingDeviceNumberWithCompressedIndex);
        System.out.println(buildTimeVaryingDeviceNumberWithoutCompressedIndex);
        System.out.println(buildTimeVaryingTrajNumberWithCompressedIndex);
        System.out.println(buildTimeVaryingTrajNumberWithoutCompressedIndex);
        System.out.println(buildTimeVaryingGridWidthWithCompressedIndex);
        System.out.println(buildTimeVaryingGridWidthWithoutCompressedIndex);


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


}
