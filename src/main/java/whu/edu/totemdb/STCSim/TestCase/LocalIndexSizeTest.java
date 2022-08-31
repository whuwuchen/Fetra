package whu.edu.totemdb.STCSim.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import whu.edu.totemdb.STCSim.Base.MappedTrajectory;
import whu.edu.totemdb.STCSim.Base.POI;
import whu.edu.totemdb.STCSim.Base.RawTrajectory;
import whu.edu.totemdb.STCSim.Device.MobileDevice;
import whu.edu.totemdb.STCSim.Device.Server;
import whu.edu.totemdb.STCSim.Index.GridIndex;
import whu.edu.totemdb.STCSim.Settings;
import whu.edu.totemdb.STCSim.StayPointDetection.SPExtrator;
import whu.edu.totemdb.STCSim.Utils.DataLoader;

import java.util.ArrayList;
import java.util.List;

public class LocalIndexSizeTest {

    public static Log logger = LogFactory.getLog(LocalIndexSizeTest.class);

    public static void main(String[] args){

        //localIndexSizeTestOfBeiJing();
        localIndexSizeTestOfNewYork();

    }
    public static void localIndexSizeTestOfBeiJing(){
        int[] trajPerDevice = {100,200,300,400,500};
        List<Double> compressedindexSizeVaryingTrajNumber = new ArrayList<>();
        List<Double> uncompressedindexSizeVaryingTrajNumber = new ArrayList<>();
        List<Double> compressedindexInfoVaryingTrajNumber = new ArrayList<>();
        List<Double> uncompressedindexInfoVaryingTrajNumber = new ArrayList<>();
        double default_grid_width = 0.005;
        int default_devices = 100;


        List<POI> pois = DataLoader.loadPOIDataBeiJing();
        List<RawTrajectory> rawTrajectories = DataLoader.loadRawTrajDataBeiJing();
        Server s = new Server(pois);
        SPExtrator sp = new SPExtrator(s.getGlobalGridIndex(), pois);
        for(int i=0;i<trajPerDevice.length;i++){
            int traj_number = trajPerDevice[i];
            double compressedIndexSize = 0;
            double uncompressedIndexSize = 0;
            double compressedIndexInfo = 0;
            double uncompressedIndexInfo = 0;
            List<MobileDevice> devices = initMobileDevices(default_devices,traj_number,s.getGlobalGridIndex()
                    ,sp,rawTrajectories,false,false);
            for(MobileDevice device : devices){
                device.matchRawTrajectories(Settings.radius/1000);
                device.indexMappedTraj();
                List<Long> filesizes = device.compressLocalIndex();
                compressedIndexSize+=filesizes.get(0);
                uncompressedIndexSize+=filesizes.get(1);
                List<Integer> indexinfoSize = device.compressIndexInfo();
                compressedIndexInfo+=indexinfoSize.get(0);
                uncompressedIndexInfo+=indexinfoSize.get(1);
            }
            compressedindexSizeVaryingTrajNumber.add(compressedIndexSize/default_devices);
            uncompressedindexSizeVaryingTrajNumber.add(uncompressedIndexSize/default_devices);
            compressedindexInfoVaryingTrajNumber.add(compressedIndexInfo/default_devices);
            uncompressedindexInfoVaryingTrajNumber.add(uncompressedIndexInfo/default_devices);
            logger.info(String.format("uncompressed index size : %2f bytes",uncompressedIndexSize/default_devices));
            logger.info(String.format("compressed index size : %2f bytes",compressedIndexSize/default_devices));
            logger.info(String.format("uncompressed index info size : %2f bytes",uncompressedIndexInfo/default_devices));
            logger.info(String.format("compressed index info size : %2f bytes",compressedIndexInfo/default_devices));


        }
        System.out.println(uncompressedindexSizeVaryingTrajNumber);
        System.out.println(compressedindexSizeVaryingTrajNumber);
        System.out.println(uncompressedindexInfoVaryingTrajNumber);
        System.out.println(compressedindexInfoVaryingTrajNumber);



        
    }


    public static void localIndexSizeTestOfNewYork(){
        int[] trajPerDevice = {100,200,300,400,500};
        List<Double> compressedindexSizeVaryingTrajNumber = new ArrayList<>();
        List<Double> uncompressedindexSizeVaryingTrajNumber = new ArrayList<>();
        List<Double> compressedindexInfoVaryingTrajNumber = new ArrayList<>();
        List<Double> uncompressedindexInfoVaryingTrajNumber = new ArrayList<>();
        double default_grid_width = 0.005;
        int default_devices = 100;


        List<POI> pois = new ArrayList<>();

        List<MappedTrajectory> mappedTrajectories = new ArrayList<>();
        DataLoader.readTrajFileTokyo(mappedTrajectories,pois);
        Server s = new Server(pois,2);
        for(int i=0;i<trajPerDevice.length;i++){
            int traj_number = trajPerDevice[i];
            double compressedIndexSize = 0;
            double uncompressedIndexSize = 0;
            double compressedIndexInfo = 0;
            double uncompressedIndexInfo = 0;
            List<MobileDevice> devices = initMobileDevices(default_devices,traj_number,s.getGlobalGridIndex()
                    ,mappedTrajectories,false,false);
            for(MobileDevice device : devices){
                /*device.matchRawTrajectories(Settings.radius/1000);
                device.indexMappedTraj();*/
                device.indexCheckinData();
                List<Long> filesizes = device.compressLocalIndex();
                compressedIndexSize+=filesizes.get(0);
                uncompressedIndexSize+=filesizes.get(1);
                List<Integer> indexinfoSize = device.compressIndexInfo();
                compressedIndexInfo+=indexinfoSize.get(0);
                uncompressedIndexInfo+=indexinfoSize.get(1);
            }
            compressedindexSizeVaryingTrajNumber.add(compressedIndexSize/default_devices);
            uncompressedindexSizeVaryingTrajNumber.add(uncompressedIndexSize/default_devices);
            compressedindexInfoVaryingTrajNumber.add(compressedIndexInfo/default_devices);
            uncompressedindexInfoVaryingTrajNumber.add(uncompressedIndexInfo/default_devices);
            logger.info(String.format("uncompressed index size : %2f bytes",uncompressedIndexSize/default_devices));
            logger.info(String.format("compressed index size : %2f bytes",compressedIndexSize/default_devices));
            logger.info(String.format("uncompressed index info size : %2f bytes",uncompressedIndexInfo/default_devices));
            logger.info(String.format("compressed index info size : %2f bytes",compressedIndexInfo/default_devices));

        }
        System.out.println(uncompressedindexSizeVaryingTrajNumber);
        System.out.println(compressedindexSizeVaryingTrajNumber);
        System.out.println(uncompressedindexInfoVaryingTrajNumber);
        System.out.println(compressedindexInfoVaryingTrajNumber);




    }

    public static List<MobileDevice> initMobileDevices(int deviceAmount, int trajPerDevice, GridIndex globalGridIndex,
                                                       List<MappedTrajectory> mappedTrajectoryList, boolean hasLocalBoost, boolean indexCompression){
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


    public static List<MobileDevice> initMobileDevices(int deviceAmount, int rawTrajsPerDevice, GridIndex globalGridIndex,
                                                       SPExtrator sp, List<RawTrajectory> rawTrajectoryList, boolean hasLocalBoost, boolean indexCompressed){
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

}
