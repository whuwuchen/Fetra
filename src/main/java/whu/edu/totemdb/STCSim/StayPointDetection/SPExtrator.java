package whu.edu.totemdb.STCSim.StayPointDetection;

import whu.edu.totemdb.STCSim.Base.*;
import whu.edu.totemdb.STCSim.Index.GridIndex;
import whu.edu.totemdb.STCSim.Index.LocalRTreeIndex;
import whu.edu.totemdb.STCSim.Index.RTreeIndex;
import whu.edu.totemdb.STCSim.Utils.TrajUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class SPExtrator {
    private GridIndex gridIndex;
    private RTreeIndex rTreeIndex;
    public SPExtrator(GridIndex gridIndex){
        this.gridIndex=gridIndex;

    }

    public SPExtrator(GridIndex gridIndex,RTreeIndex rTreeIndex){
        this.gridIndex = gridIndex;
        this.rTreeIndex = rTreeIndex;
    }

    public SPExtrator(GridIndex gridIndex,List<POI> pois){
        this.gridIndex = gridIndex;
        this.rTreeIndex = new RTreeIndex(pois);
    }

    /*//用于与病例轨迹进行映射
    public SPExtrator(LocalRTreeIndex localRTreeIndex,){

    }*/

    public MappedTrajectory extract(RawTrajectory rawTrajectory,
                                           double distanceThreshold,long timeThreshold){
        List<Point> pts = rawTrajectory.getGpslog();
        MappedTrajectory res = new MappedTrajectory(rawTrajectory);
        List<Hop> hops = new ArrayList<>();
        int length = pts.size();
        int i=0;
        while(i<length){
            int j=i+1;
            while(j<length){
                double distance = TrajUtil.distanceOfPoints(pts.get(i),pts.get(j));
                if(distance>distanceThreshold||j==length-1){
                    long timeInterval = pts.get(j).getTimestamp()-pts.get(i).getTimestamp();
                    if(timeInterval>timeThreshold){
                        Point p = meanPoint(pts.subList(i,j+1));
                        hops.add(new Hop((POI) gridIndex.queryNNPOI(p.getLat(),p.getLon(),distanceThreshold),
                                pts.get(i).getTimestamp(),pts.get(j).getTimestamp()));
                    }
                    i=j;
                    break;
                }
                j++;
            }
            if(i==length-1){
                break;
            }

        }
        res.setMappedTrajId(rawTrajectory.getId());
        res.setHops(hops);
        return res;
    }


    public List<MappedTrajectory> extractBatch(List<RawTrajectory> rawTrajectories,
                                                      double distanceThreshold,long timeThreshold){

        List<MappedTrajectory> trajectories = rawTrajectories.parallelStream()
                        .map(s-> extract(s,distanceThreshold,timeThreshold))
                        .collect(Collectors.toList());
        return trajectories;
    }



    public List<MappedTrajectory> extractBatch(List<RawTrajectory> rawTrajectoryList,
                                               double distanceThreshold,long timeThreshold,double radius){
        List<MappedTrajectory> trajectories = rawTrajectoryList.parallelStream()
                .map(s->extract(s,distanceThreshold,timeThreshold,radius))
                .collect(Collectors.toList());
        return trajectories;
    }

    public MappedTrajectory extract(RawTrajectory rawTrajectory,
                                    double distanceThreshold,long timeThreshold,double radius){
        List<Point> pts = rawTrajectory.getGpslog();
        MappedTrajectory res = new MappedTrajectory(rawTrajectory);
        List<Hop> hops = new ArrayList<>();
        int length = pts.size();
        int i=0;
        while(i<length){
            int j=i+1;
            while(j<length){
                double distance = TrajUtil.distanceOfPoints(pts.get(i),pts.get(j));
                if(distance>distanceThreshold||j==length-1){
                    long timeInterval = pts.get(j).getTimestamp()-pts.get(i).getTimestamp();
                    if(timeInterval>timeThreshold){
                        Point p = meanPoint(pts.subList(i,j+1));
                        List<POI> pois = rTreeIndex.queryPois(p.getLat(),p.getLon(),radius);
                        for(POI poi:pois){
                            hops.add(new Hop(poi,pts.get(i).getTimestamp(),pts.get(j).getTimestamp()));
                        }

                    }
                    i=j;
                    break;
                }
                j++;
            }
            if(i==length-1){
                break;
            }

        }
        res.setMappedTrajId(rawTrajectory.getId());
        res.setHops(hops);
        return res;
    }




    public Point meanPoint(List<Point> subPoints){
        double lat=0;
        double lon=0;
        for(Point p:subPoints){
            lat+=p.getLat();
            lon+=p.getLon();
        }
        Point p = new Point(lat/subPoints.size(),lon/subPoints.size(),0);
        return p;
    }


    public List<StayPoint> extractStayPoints(RawTrajectory rawTrajectory,
                                             double distanceThreshold,long timeThreshold){
        List<Point> pts = rawTrajectory.getGpslog();
        List<StayPoint> stayPoints = new ArrayList<>();
        int length = pts.size();
        int i=0;
        while(i<length){
            int j=i+1;
            while(j<length){
                double distance = TrajUtil.distanceOfPoints(pts.get(i),pts.get(j));
                if(distance>distanceThreshold||j==length-1){
                    long timeInterval = pts.get(j).getTimestamp()-pts.get(i).getTimestamp();
                    if(timeInterval>timeThreshold){
                        Point p = meanPoint(pts.subList(i,j+1));
                        stayPoints.add(new StayPoint(p.getLat(),p.getLon(),pts.get(i).getTimestamp(),pts.get(j).getTimestamp()));
                    }
                    i=j;
                    break;
                }
                j++;
            }
            if(i==length-1){
                break;
            }

        }
        return stayPoints;
    }

    public List<StayPoint> extractStayPointsBatch(List<RawTrajectory> rawTrajectoryList,
                                                  double distanceThreshold,long timeThreshold){
        return rawTrajectoryList.stream()
                .map(rawTrajectory -> extractStayPoints(rawTrajectory,distanceThreshold,timeThreshold))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }


}
