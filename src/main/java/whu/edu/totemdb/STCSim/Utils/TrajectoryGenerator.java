package whu.edu.totemdb.STCSim.Utils;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.PathWrapper;
import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.FootFlagEncoder;
import com.graphhopper.util.Parameters;
import com.graphhopper.util.PointList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import whu.edu.totemdb.STCSim.Base.POI;
import whu.edu.totemdb.STCSim.Base.Point;
import whu.edu.totemdb.STCSim.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TrajectoryGenerator {
    private List<POI> pois;
    private GraphHopperOSM hopper;
    private static Log logger = LogFactory.getLog(TrajectoryGenerator.class);

    public TrajectoryGenerator(String osmFile,List<POI> pois){
        this.pois=pois;
        if(null==hopper){
            hopper = new GraphHopperOSM();
            hopper.setDataReaderFile(osmFile);
            hopper.setGraphHopperLocation("Resources/BeiJing/routing-graph-cache");
            hopper.setEncodingManager(new EncodingManager(new FootFlagEncoder()));
            hopper.getCHFactoryDecorator().setEnabled(false);
            hopper.importOrLoad();
            logger.info("The range bound in "+osmFile+" : "+hopper.getGraphHopperStorage().getBounds());

        }

    }


    public List<Point> routing(Point p1, Point p2,long startTimestamp,long samplingRate,long stayTime){
        GHRequest req = new GHRequest(p1.getLat(),p1.getLon(),p2.getLat(),p2.getLon())
                .setVehicle("foot")
                .setAlgorithm(Parameters.Algorithms.ASTAR_BI)
                .setLocale(Locale.CHINA);
        GHResponse rsp = hopper.route(req);

        // handle errors
        if (rsp.hasErrors())
            throw new RuntimeException(rsp.getErrors().toString());
        PathWrapper p = rsp.getBest();
        PointList pl= p.getPoints();
        if(pl.size()==0){
            logger.error(String.format("Routing result between of %s and %s is null", p1, p2));
            return null;
        }

        List<Point> res = new ArrayList<>();
        long curTime = startTimestamp;
        int num = Math.max((int)((1+stayTime/samplingRate)*1.5),1);
        for(int j=0;j<num;j++){
            res.add(new Point(p1.getLat(),p1.getLon(),curTime));
            curTime += samplingRate;
        }
        res.addAll(extendPathBetweenPoints(p1,new Point(pl.getLat(0),pl.getLon(0),0),curTime,samplingRate));

        for(int i=0;i<pl.size()-1;i++){
            curTime += samplingRate;
            Point arg1 = new Point(pl.getLat(i),pl.getLon(i),0);
            Point arg2 = new Point(pl.getLat(i+1),pl.getLon(i+1),0);
            res.addAll(extendPathBetweenPoints(arg1,arg2,curTime,samplingRate));
        }

        res.addAll(extendPathBetweenPoints(new Point(pl.getLat(pl.size()-1),pl.getLon(pl.size()-1),0),p2,curTime,samplingRate));

        for(int j=0;j<num;j++){
            res.add(new Point(p2.getLat(),p2.getLon(),curTime));
            curTime += samplingRate;
        }

        return res;

    }

    public List<Point> extendPathBetweenPoints(Point p1, Point p2, long timestamp,long samplingRate){
        List<Point> res = new ArrayList<>();

        double delta = Settings.deltaDegree*Settings.speedOfFoot;
        double deltaLat = p2.getLat() - p1.getLat();
        double deltaLon = p2.getLon() - p1.getLon();
        double latFrac = deltaLat/Math.sqrt(Math.pow(deltaLat,2)+Math.pow(deltaLon,2));
        double lonFrac = deltaLon/Math.sqrt(Math.pow(deltaLat,2)+Math.pow(deltaLon,2));
        timestamp += samplingRate;
        res.add(new Point(p1.getLat(),p1.getLon(),timestamp));
        Point cur = new Point(p1.getLat(),p1.getLon(),timestamp);
        int num = (int) (TrajUtil.distanceOfPoints(cur,p2)/(Settings.speedOfFoot*samplingRate))+1;
        double lat = cur.getLat();
        double lon = cur.getLon();
        for(int i=0;i<num;i++){
            timestamp += samplingRate;
            lat += delta*latFrac;
            lon += delta*lonFrac;
            res.add(new Point(lat,lon,timestamp));
        }


        res.add(new Point(p2.getLat(),p2.getLon(),timestamp+samplingRate));
        return res;
    }


}
