package whu.edu.totemdb.STCSim.Utils;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import whu.edu.totemdb.STCSim.Base.*;
import whu.edu.totemdb.STCSim.Settings;

import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DataLoader {
    public static Log logger = LogFactory.getLog(DataLoader.class);
    //public static Logger logger = Logger.getLogger("DataLoader");
    public static List<POI> loadPOIDataBeiJing(){
        //当前文件路径  E:\paper\research paper\project\.
        List<POI> res = new ArrayList<>();
        File poiDir = new File("/mnt/wuchen/Resources/BeiJing/POI");
        // File poiDir = new File("E:\\fetra\\project\\Resources\\BeiJing\\POI");
        if ((!poiDir.exists())|| (!poiDir.isDirectory())){
            logger.info("POI directory not exists");
        }
        File[] files = poiDir.listFiles();
        int poiCount = 0;
        res = Arrays.stream(files).parallel()
                .map(f->readPOIFileBeiJing(f))
                .flatMap(List::stream)
                .collect(Collectors.toList());
        //Load 625910 POI
        //After filter Load 625910 POI
        logger.info(String.format("Load %d POI", res.size()));
        return res;
    }

    public static List<POI> readPOIFileBeiJing (File poiFile){
        List<POI> pts = new ArrayList<>();
        try(BufferedReader bfr = new BufferedReader(new FileReader(poiFile))){
            String str = null;
            bfr.readLine();
            int i=0;
            while((str = bfr.readLine())!=null){
                try {
                    String[] vals = str.split(",");
                    int len = vals.length;
                    String name = vals[0];
                    double lat = Double.valueOf(vals[len - 1]);
                    double lon = Double.valueOf(vals[len - 2]);
                    //recordCoordinateRange(lon,lat);
                    if (inBeiJing(lon, lat)) {
                        pts.add(new POI(lat, lon, name, i));
                        i++;
                    }
                }catch (Exception e){
                    continue;
                }

            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return pts;
    }

    public static List<RawTrajectory> loadRawTrajDataBeiJing(){
        List<RawTrajectory> res ;
        File trajDir = new File("/mnt/wuchen/Resources/BeiJing/Data");
        // File trajDir = new File("E:\\fetra\\project\\Resources\\BeiJing\\Data");
        if((!trajDir.exists())||(!trajDir.isDirectory())){
            logger.info("Trajectory directory not exists");
        }
        int fileCount=0;
        File[] dirs = trajDir.listFiles();

        res = Arrays.stream(dirs).parallel()
                .map(s-> new File(s.getAbsolutePath()+"/Trajectory").listFiles())
                .flatMap(Arrays::stream)
                .map(f-> TrajUtil.splitRawTrajectory(readTrajFileBeiJing(f)))
                .flatMap(List::stream)
                .collect(Collectors.toList());

        //set it for raw trajectories
        for(int i=0;i<res.size();i++){
            res.get(i).setId(i);
        }

        //Load 58725 raw trajectories from 18670 files
        //After filter Load 51660 raw trajectories from 18670 files
        logger.info(String.format("Load %d raw trajectories from %d files", res.size(),fileCount));
        return res;
    }



    public static List<MappedTrajectory> readTrajFileNewYork(List<MappedTrajectory> mappedTrajectoryList
                                        ,List<POI> pois){
        // read poi and stay point from csv file
        List<MappedTrajectory> result = new ArrayList<>();
        HashMap<Integer,List<Hop>> HopsByUserId = new HashMap<>();

        int poi_count = 0;
        Map<String,POI> poiMap = new HashMap<>();
        double minlat = 90;
        double maxlat = 0;
        double minlon = 180;
        double maxlon = -180;
        //File trajFile = new File("/mnt/wuchen/Resources/NewYork/dataset_TSMC2014_NYC.csv");
        File trajFile = new File("E:/fetra/project/Resources/NewYork/dataset_TSMC2014_NYC.csv");

        try(BufferedReader bfr = new BufferedReader(new FileReader(trajFile))){
            String str = null;
            for(int i=0;i<1;i++){
                bfr.readLine();
            }
            //DateFormat formatter = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy");
            //DateFormat formatter = new SimpleDateFormat("E MMM dd HH:mm:ss Z yyyy",Locale.US);
            DateFormat formatter = new SimpleDateFormat("E MMM dd HH:mm:ss Z yyyy",Locale.US);
            while((str = bfr.readLine())!=null){
                String[] vals = str.split(",");
                int len = vals.length;
                assert len==8;
                int userId = Integer.parseInt(vals[0]);
                String venueId = vals[2];
                double lat = Double.valueOf(vals[4]);
                double lon = Double.valueOf(vals[5]);
                //Date date = (Date)formatter.parse(vals[7].replace("+0000","CST"));

                // vals[7] = vals[7].replace("+0000","UTC");
                //System.out.print(vals[7]);

                minlat = Math.min(lat,minlat);
                maxlat = Math.max(lat,maxlat);
                minlon = Math.min(lon,minlon);
                maxlon = Math.max(lon,maxlon);

                Date date = (Date)formatter.parse(vals[7]);

                long timestamp = date.getTime()/1000;
                // System.out.println(timestamp);
                if(poiMap.containsKey(venueId)){

                }
                else{
                    poiMap.put(venueId,new POI(lat,lon,venueId,poiMap.size()));
                    poi_count++;
                }
                if(HopsByUserId.containsKey(userId)){
                    HopsByUserId.get(userId).add(new Hop(poiMap.get(venueId),timestamp,0));

                }
                else{
                    HopsByUserId.put(userId,new ArrayList<>());
                    HopsByUserId.get(userId).add(new Hop(poiMap.get(venueId),timestamp,0));
                }

            }

            logger.info(String.format("Total poi number of New York is : %d",poiMap.size()));
            logger.info(String.format("Total user number of New York is : %d",HopsByUserId.size()));
            logger.info(String.format("Range of NewYork: %4f,%4f,%4f,%4f",minlat,maxlat,minlon,maxlon));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }


        List<POI> pois1 = poiMap.values().stream().sorted(new Comparator<POI>() {
            @Override
            public int compare(POI o1, POI o2) {
                if(o1.getId()<o2.getId()){
                    return -1;
                }
                else if(o1.getId()>o2.getId()){
                    return 1;
                }
                return 0;
            }
        }).collect(Collectors.toList());

        HopsByUserId.forEach((integer, hops) -> result.addAll(hop2MappedTraj(hops)));

        mappedTrajectoryList.addAll(result);
        pois.addAll(pois1);
        logger.info(String.format("Total trajectory number of New York is : %d",result.size()));
        return result;
    }

    public static List<MappedTrajectory> readTrajFileTokyo(List<MappedTrajectory> mappedTrajectoryList
            ,List<POI> pois){
        // read poi and stay point from csv file
        List<MappedTrajectory> result = new ArrayList<>();
        HashMap<Integer,List<Hop>> HopsByUserId = new HashMap<>();

        int poi_count = 0;
        Map<String,POI> poiMap = new HashMap<>();
        double minlat = 90;
        double maxlat = 0;
        double minlon = 180;
        double maxlon = -180;
        //File trajFile = new File("/mnt/wuchen/Resources/Tokyo/dataset_TSMC2014_TKY.csv");
        File trajFile = new File("F:/project/Resources/Tokyo/dataset_TSMC2014_TKY.csv");

        try(BufferedReader bfr = new BufferedReader(new FileReader(trajFile))){
            String str = null;
            for(int i=0;i<1;i++){
                bfr.readLine();
            }
            //DateFormat formatter = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy");
            //DateFormat formatter = new SimpleDateFormat("E MMM dd HH:mm:ss Z yyyy",Locale.US);
            DateFormat formatter = new SimpleDateFormat("E MMM dd HH:mm:ss Z yyyy",Locale.US);
            while((str = bfr.readLine())!=null){
                String[] vals = str.split(",");
                int len = vals.length;
                assert len==8;
                int userId = Integer.parseInt(vals[0]);
                String venueId = vals[2];
                double lat = Double.valueOf(vals[4]);
                double lon = Double.valueOf(vals[5]);
                //Date date = (Date)formatter.parse(vals[7].replace("+0000","CST"));

                // vals[7] = vals[7].replace("+0000","UTC");
                //System.out.print(vals[7]);

                minlat = Math.min(lat,minlat);
                maxlat = Math.max(lat,maxlat);
                minlon = Math.min(lon,minlon);
                maxlon = Math.max(lon,maxlon);

                Date date = (Date)formatter.parse(vals[7]);

                long timestamp = date.getTime()/1000;
                // System.out.println(timestamp);
                if(poiMap.containsKey(venueId)){

                }
                else{
                    poiMap.put(venueId,new POI(lat,lon,venueId,poiMap.size()));
                    poi_count++;
                }
                if(HopsByUserId.containsKey(userId)){
                    HopsByUserId.get(userId).add(new Hop(poiMap.get(venueId),timestamp,0));

                }
                else{
                    HopsByUserId.put(userId,new ArrayList<>());
                    HopsByUserId.get(userId).add(new Hop(poiMap.get(venueId),timestamp,0));
                }

            }

            logger.info(String.format("Total poi number of New York is : %d",poiMap.size()));
            logger.info(String.format("Total user number of New York is : %d",HopsByUserId.size()));
            logger.info(String.format("Range of NewYork: %4f,%4f,%4f,%4f",minlat,maxlat,minlon,maxlon));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }


        List<POI> pois1 = poiMap.values().stream().sorted(new Comparator<POI>() {
            @Override
            public int compare(POI o1, POI o2) {
                if(o1.getId()<o2.getId()){
                    return -1;
                }
                else if(o1.getId()>o2.getId()){
                    return 1;
                }
                return 0;
            }
        }).collect(Collectors.toList());

        HopsByUserId.forEach((integer, hops) -> result.addAll(hop2MappedTraj(hops)));

        mappedTrajectoryList.addAll(result);
        pois.addAll(pois1);
        logger.info(String.format("Total trajectory number of New York is : %d",result.size()));
        return result;
    }

    public static List<MappedTrajectory> hop2MappedTraj(List<Hop> hops){
        List<MappedTrajectory> mappedTrajectoryList = new ArrayList<>();
        assert hops.size()>=1;
        long curDay = hops.get(0).getStarttime()/86400;
        List<Hop> curHops= new ArrayList<>();
        curHops.add(hops.get(0));
        int minStayTime = 300;
        int maxStayTime = 1800;

        for(int i=0;i<hops.size();i++){
            long tmp = hops.get(i).getStarttime()/86400;
            //hops.get(i-1).setEndtime((long) Math.min(hops.get(i-1).getStarttime()+minStayTime+Math.random()*(maxStayTime-minStayTime),hops.get(i).getStarttime()));
            hops.get(i).setEndtime((long) (hops.get(i).getStarttime()+minStayTime+Math.random()*(maxStayTime-minStayTime)));
            if(tmp-curDay>=1){
                MappedTrajectory traj = new MappedTrajectory(null,curHops);
                mappedTrajectoryList.add(traj);
                curHops = new ArrayList<>();
                curHops.add(hops.get(i));
                curDay = tmp;
            }else{
                curHops.add(hops.get(i));
            }

        }
        /*int i = hops.size()-1;
        hops.get(i).setEndtime((long) (hops.get(i).getStarttime()+minStayTime+Math.random()*(maxStayTime-minStayTime)));*/
        if(curHops.size()>0){
            MappedTrajectory traj = new MappedTrajectory(null,curHops);
            mappedTrajectoryList.add(traj);
        }

        return mappedTrajectoryList;

    }

    public static RawTrajectory readTrajFileBeiJing(File trajFile){
        RawTrajectory traj = new RawTrajectory();
        List<POI> pts = new ArrayList<>();
        try(BufferedReader bfr = new BufferedReader(new FileReader(trajFile))){
            String str = null;
            for(int j=6;j>0;j--) {
                bfr.readLine();
            }
            int i=0;
            while((str = bfr.readLine())!=null){
                String[] vals = str.split(",");
                int len = vals.length;
                String dateStr = vals[len-2]+" "+vals[len-1];
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date date = simpleDateFormat.parse(dateStr);
                long ts = date.getTime()/1000;
                double lat = Double.valueOf(vals[0]);
                double lon = Double.valueOf(vals[1]);
                if(inBeiJing(lon,lat)){
                    // traj.addPoint(new Point(lat,lon,ts%Settings.windowSize));
                    traj.addPoint(new Point(lat,lon,ts));
                    i++;
                }
                //recordCoordinateRange(lon,lat);

            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return traj;
    }

    public static void recordCoordinateRange(double lon,double lat){
        Settings.maxLonBeiJing = Math.max(Settings.maxLonBeiJing,lon);
        Settings.minLonBeiJing = Math.min(Settings.minLonBeiJing,lon);
        Settings.maxLatBeiJing = Math.max(Settings.maxLatBeiJing,lat);
        Settings.minLatBeiJing = Math.min(Settings.minLatBeiJing,lat);

    }

    public static boolean inBeiJing(double lon, double lat){
        if (lon>Settings.minLonBeiJing&&lon<Settings.maxLonBeiJing
            &&lat>Settings.minLatBeiJing&&lat<Settings.maxLatBeiJing){
            return true;
        }
        return false;
    }

}
