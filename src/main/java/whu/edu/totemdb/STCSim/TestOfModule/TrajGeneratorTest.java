package whu.edu.totemdb.STCSim.TestOfModule;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import whu.edu.totemdb.STCSim.Base.POI;
import whu.edu.totemdb.STCSim.Base.Point;
import whu.edu.totemdb.STCSim.Settings;
import whu.edu.totemdb.STCSim.Utils.DataLoader;
import whu.edu.totemdb.STCSim.Utils.TrajectoryGenerator;

import java.util.List;

public class TrajGeneratorTest {
    private static Log logger = LogFactory.getLog(TrajGeneratorTest.class);
    public static void main(String[] args){
        List<POI> pois = DataLoader.loadPOIDataBeiJing();
        String osmFile = "Resources/BeiJing/Beijing.osm.pbf";
        TrajectoryGenerator tg = new TrajectoryGenerator(osmFile,pois);
        Point p1 = new Point(39.84015527,116.5042748,0);
        Point p2 = new Point(39.90012772,116.5153068,0);
        List<Point> path = tg.routing(p1,p2,0,5, Settings.trajectoryClusterTimeThreshold);
        System.out.println("End");

    }
}
