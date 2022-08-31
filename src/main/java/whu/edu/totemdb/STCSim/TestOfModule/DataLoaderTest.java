package whu.edu.totemdb.STCSim.TestOfModule;

import whu.edu.totemdb.STCSim.Base.MappedTrajectory;
import whu.edu.totemdb.STCSim.Base.POI;
import whu.edu.totemdb.STCSim.Base.RawTrajectory;
import whu.edu.totemdb.STCSim.Device.Server;
import whu.edu.totemdb.STCSim.Settings;
import whu.edu.totemdb.STCSim.StayPointDetection.SPExtrator;
import whu.edu.totemdb.STCSim.Utils.DataLoader;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DataLoaderTest {
    public static void main(String[] args) throws ParseException {
        //DataLoader.readTrajFileNewYork();
        //DataLoader.readTrajFileTokyo();
        //String dateStr = "Mon Jun 18 00:00:00 UTC 2012";
       /* DateTimeFormatter formatter = DateTimeFormatter.ofPattern("E MMM dd HH:mm:ss z uuuu").withLocale( Locale.US );
        Date date = (Date)formatter.parse(dateStr);*/
        /*DateFormat formatter = new SimpleDateFormat("E MMM dd HH:mm:ss Z yyyy",Locale.US);
        Date date = (Date)formatter.parse(dateStr);
        System.out.println(date);*/

        /*List<POI> pois = DataLoader.loadPOIDataBeiJing();
        List<RawTrajectory> rawTrajectories = DataLoader.loadRawTrajDataBeiJing();
        Server s = new Server(pois);
        SPExtrator sp = new SPExtrator(s.getGlobalGridIndex(),pois);
        List<MappedTrajectory> mappedTrajectories = sp.extractBatch(rawTrajectories,
                Settings.trajectoryClusterDistanceThreshold,Settings.trajectoryClusterTimeThreshold,Settings.radius/1000);
        long num = mappedTrajectories.stream().mapToLong(t->t.getHops().size()).sum(); 385639
        System.out.println(num);*/

        /*List<POI> pois = new ArrayList<>();
        List<MappedTrajectory> mappedTrajectories = new ArrayList<>();
        DataLoader.readTrajFileNewYork(mappedTrajectories,pois);
        long num = mappedTrajectories.stream().mapToLong(t->t.getHops().size()).sum();//228511
        System.out.println(num);*/

        List<POI> pois = new ArrayList<>();
        List<MappedTrajectory> mappedTrajectories = new ArrayList<>();
        DataLoader.readTrajFileTokyo(mappedTrajectories,pois);
        long num = mappedTrajectories.stream().mapToLong(t->t.getHops().size()).sum();//575996
        System.out.println(num);


    }
}
