package whu.edu.totemdb.STCSim.Utils;

import whu.edu.totemdb.STCSim.Base.Hop;
import whu.edu.totemdb.STCSim.Base.MappedTrajectory;
import whu.edu.totemdb.STCSim.Base.Point;
import whu.edu.totemdb.STCSim.Base.RawTrajectory;
import whu.edu.totemdb.STCSim.Settings;

import java.util.ArrayList;
import java.util.List;

public class TrajUtil {

    public static List<RawTrajectory> splitRawTrajectory(RawTrajectory rawTrajectory){
        List<RawTrajectory> res = new ArrayList<>();
        List<Point> source = rawTrajectory.getGpslog();
        List<Point> curList = new ArrayList<>();
        if(source.size()==0){
            return res;
        }
        Point pre=source.get(0);
        curList.add(pre);
        for(int i=1;i<source.size();i++){
            Point cur = source.get(i);
            if (cur.getTimestamp()-pre.getTimestamp()>=Settings.trajectorySplitTimeThreshold){
                res.add(new RawTrajectory(curList));
                curList = new ArrayList<>();
                curList.add(cur);
            }
            else {
                curList.add(cur);
            }
            pre=cur;
        }
        res.add(new RawTrajectory(curList));
        return res;
    }

    public static void statisticOfRawTrajectories(List<RawTrajectory> rawTrajectories){
        double totalLength = rawTrajectories.parallelStream()
                .map(t->lengthOfTraj(t))
                .reduce((a,b)->a+b).get();
        double totalPointNum = rawTrajectories.parallelStream()
                .map(t->t.getGpslog().size())
                .reduce((a,b)->a+b).get();

        DataLoader.logger.info(String.format("Average travel length: %f(m)",totalLength/rawTrajectories.size()));
        DataLoader.logger.info(String.format("Points per trajectory: %f",totalPointNum/rawTrajectories.size()));


    }

    public static void statisticOfMappedTrajectories(List<MappedTrajectory> mappedTrajectories){
        int totalPOIs = mappedTrajectories.parallelStream()
                .map(t->t.getHops().size())
                .reduce((a,b)->a+b).get();
        long totalStayTime = mappedTrajectories.parallelStream()
                .map(t->t.getHops())
                .flatMap(List::stream)
                .map(s->s.getEndtime()-s.getStarttime())
                .reduce((a,b)->a+b).get();
        DataLoader.logger.info(String.format("Average stay point number: %f",(double)totalPOIs/mappedTrajectories.size()));
        DataLoader.logger.info(String.format("Average stay time: %f(s)",(double)totalStayTime/mappedTrajectories.size()));
    }

    public static double lengthOfTraj(RawTrajectory rawTrajectory){
        if (rawTrajectory.getGpslog().size()<2){
            return 0;
        }
        List<Point> pts = rawTrajectory.getGpslog();
        double res = 0;
        Point pre = pts.get(0);
        for(int i=1;i<pts.size();i++){
            Point cur = pts.get(i);
            res += distanceOfPoints(pre,cur);
            pre = cur;
        }
        return res;

    }

    public static double STCSim(MappedTrajectory t1,MappedTrajectory t2){
        List<Hop> hops1 = t1.getHops();
        List<Hop> hops2 = t2.getHops();
        int size1 = hops1.size();
        int size2 = hops2.size();
        if(0== hops1.size()||0== hops2.size()){
            return 0;
        }
        double[][] res = new double[size1][size2];
        for(int i=0;i<size1;i++){
            for(int j=0;j<size2;j++){
                Hop hop1 = hops1.get(i);
                Hop hop2 = hops2.get(j);
                if(hops1.get(i).overlap(hops2.get(j))){
                    if(i==0||j==0){
                        res[i][j] = Math.max(0,Math.min(hop1.getEndtime()
                                ,hop2.getEndtime())-Math.max(hop1.getStarttime(),hop1.getStarttime()));
                    }
                    else{
                        res[i][j] = res[i-1][j-1]+Math.max(0,Math.min(hop1.getEndtime()
                                ,hop2.getEndtime())-Math.max(hop1.getStarttime(),hop1.getStarttime()));
                    }
                }
                else{
                    if(i==0||j==0){
                        res[i][j]=0;
                    }
                    else{
                        res[i][j]=Math.max(res[i-1][j],res[i][j-1]);
                    }
                }
            }
        }

        return res[size1-1][size2-1];
    }

    public static double overlapInterval(long s1,long e1,long s2,long e2){

        return Math.max(0,Math.min(e1,e2)-Math.max(s1,s2));

    }

    public static double distanceOfPoints(Point p1,Point p2){

        return distance(p1.getLat(),p2.getLat(),p1.getLon(),p2.getLon(),0,0);
    }

    public static double distance(double lat1, double lat2, double lon1, double lon2, double el1, double el2) {

        final int R = 6371; // Radius of the earth

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2)) * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        double height = el1 - el2;

        distance = Math.pow(distance, 2) + Math.pow(height, 2);

        return Math.sqrt(distance);
    }

}
