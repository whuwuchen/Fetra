package whu.edu.totemdb.STCSim.Device;

import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import whu.edu.totemdb.STCSim.Base.MappedTrajectory;
import whu.edu.totemdb.STCSim.Base.POI;
import whu.edu.totemdb.STCSim.Index.GridIndex;
import whu.edu.totemdb.STCSim.Index.HopInvertedIndex;
import whu.edu.totemdb.STCSim.Index.TimeIntervalTree;
import whu.edu.totemdb.STCSim.Settings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Server {

    public static final Log serverLogger = LogFactory.getLog(Server.class);

    // used for POI matching of patients' trajectories
    GridIndex globalGridIndex;

    // index the spatial and temporal info of stay points, e.g. <GridID,TimeIntervalTree>
    Map<Integer, TimeIntervalTree> map;

    // index the risky POIs
    HopInvertedIndex poiInvertedIndex;

    // risky trajectories
    List<MappedTrajectory> riskyTrajectories;

    public Map<String,TimeIntervalTree> gridFootprint;

    public Server(List<POI> pois){
        //this.riskyTrajectories = riskyTrajectories;
        globalGridIndex = new GridIndex(Settings.maxLonBeiJing,Settings.minLonBeiJing,
                Settings.maxLatBeiJing, Settings.minLatBeiJing,Settings.latGridWidth, Settings.lonGridWidth);
        // globalGridIndex.init(new ArrayList<>(pois));
        poiInvertedIndex = new HopInvertedIndex();
        // poiInvertedIndex.initFromMappedTrajectories(riskyTrajectories);

        gridFootprint = new HashMap<>();

    }

    public Server(List<POI> pois,int city){
        // 1 for newyork, 2 for tokyo

        if(city == 1){
            globalGridIndex = new GridIndex(Settings.maxLonNewYork,Settings.minLonNewYork,
                    Settings.maxLatNewYork,Settings.minLatNewYork,Settings.latGridWidth,Settings.lonGridWidth);
        }else if(city == 2){
            globalGridIndex = new GridIndex(Settings.maxLonTokyo,Settings.minLonTokyo,
                    Settings.maxLatTokyo,Settings.minLatTokyo,Settings.latGridWidth,Settings.lonGridWidth);
        }

        poiInvertedIndex = new HopInvertedIndex();
        gridFootprint = new HashMap<>();

    }

    public GridIndex getGlobalGridIndex() {
        return globalGridIndex;
    }

    public void reset(double gridWidth){
        globalGridIndex = new GridIndex(Settings.maxLonBeiJing,Settings.minLonBeiJing,
                Settings.maxLatBeiJing, Settings.minLatBeiJing,gridWidth,gridWidth);
    }



}

