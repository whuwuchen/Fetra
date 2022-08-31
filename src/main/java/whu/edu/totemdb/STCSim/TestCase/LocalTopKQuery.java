package whu.edu.totemdb.STCSim.TestCase;

import whu.edu.totemdb.STCSim.Base.MappedTrajectory;
import whu.edu.totemdb.STCSim.Base.POI;
import whu.edu.totemdb.STCSim.Base.RawTrajectory;
import whu.edu.totemdb.STCSim.Index.GridIndex;
import whu.edu.totemdb.STCSim.Query.BaseEngine;
import whu.edu.totemdb.STCSim.Settings;
import whu.edu.totemdb.STCSim.StayPointDetection.SPExtrator;
import whu.edu.totemdb.STCSim.Utils.DataLoader;
import whu.edu.totemdb.STCSim.Utils.TrajUtil;

import java.util.ArrayList;
import java.util.List;

public class LocalTopKQuery {
    public static void main(String[] args){
        //File file  = new File(this.getClass().getResource());
        //DataLoader.loadTrajRawDataBeiJing();
        List<RawTrajectory> rawTrajectories = DataLoader.loadRawTrajDataBeiJing();
        TrajUtil.statisticOfRawTrajectories(rawTrajectories);
        List<POI> pois = DataLoader.loadPOIDataBeiJing();
        GridIndex g = new GridIndex(Settings.maxLonBeiJing,Settings.minLonBeiJing,
                Settings.maxLatBeiJing, Settings.minLatBeiJing,Settings.latGridWidth, Settings.lonGridWidth);
        g.init(new ArrayList<>(pois));
        SPExtrator extrator = new SPExtrator(g);
        Long beforeMatching = System.currentTimeMillis();
        List<MappedTrajectory> mappedTrajectoryList = extrator.extractBatch(rawTrajectories,
                Settings.trajectoryClusterDistanceThreshold,Settings.trajectoryClusterTimeThreshold);
        Long afterMatching = System.currentTimeMillis();
        DataLoader.logger.info(String.format("Total Matching time: %d",(afterMatching-beforeMatching)/1000));
        TrajUtil.statisticOfMappedTrajectories(mappedTrajectoryList);
        BaseEngine baseEngine = new BaseEngine();
        baseEngine.init(mappedTrajectoryList,g);
        int k=10;
        int Qsize=100;

        List<RawTrajectory> result= baseEngine.topKQuery(rawTrajectories,mappedTrajectoryList.subList(0,Qsize),k);
        //MappedTrajectory m= extrator.extract(rawTrajectories.get(0),Settings.trajectoryClusterDistanceThreshold,Settings.trajectoryClusterTimeThreshold);

        System.out.println("end");

    }
}
