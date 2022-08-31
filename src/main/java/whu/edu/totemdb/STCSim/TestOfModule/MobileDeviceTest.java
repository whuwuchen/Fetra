package whu.edu.totemdb.STCSim.TestOfModule;

import whu.edu.totemdb.STCSim.Base.POI;
import whu.edu.totemdb.STCSim.Base.RawTrajectory;
import whu.edu.totemdb.STCSim.Device.MobileDevice;
import whu.edu.totemdb.STCSim.Index.GridIndex;
import whu.edu.totemdb.STCSim.Index.RTreeIndex;
import whu.edu.totemdb.STCSim.Settings;
import whu.edu.totemdb.STCSim.StayPointDetection.SPExtrator;
import whu.edu.totemdb.STCSim.Utils.DataLoader;

import java.util.List;

public class MobileDeviceTest {
    public static void main(String[] args){
        List<RawTrajectory> rtajs = DataLoader.loadRawTrajDataBeiJing();
        List<POI> pois = DataLoader.loadPOIDataBeiJing();
        GridIndex globalgridIndex = new GridIndex(Settings.maxLonBeiJing,Settings.minLonBeiJing,
                Settings.maxLatBeiJing, Settings.minLatBeiJing,Settings.latGridWidth, Settings.lonGridWidth);
        RTreeIndex rtree = new RTreeIndex(pois);
        SPExtrator sp = new SPExtrator(globalgridIndex,rtree);
        MobileDevice m = new MobileDevice("0",globalgridIndex,rtajs.subList(0,100),sp);
        m.matchRawTrajectories(Settings.radius/1000);

    }



}
