package whu.edu.totemdb.STCSim.Index;

import whu.edu.totemdb.STCSim.Base.BasePoint;
import whu.edu.totemdb.STCSim.Base.POI;
import whu.edu.totemdb.STCSim.Base.Range;
import whu.edu.totemdb.STCSim.Utils.TrajUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GridIndex {
    private double lonmax;
    private double lonmin;
    private double latmax;
    private double latmin;
    private double latGridWidth = 0.005;
    private double lonGridWidth = 0.005;
    private int indexHorizontalSize = 0;
    private int indexVerticalSize = 0;

    private Map<String, List<BasePoint>> poiMap;
    public GridIndex(double lonmax,double lonmin,double latmax,double latmin,double latGridWidth,double lonGridWidth){
        this.latGridWidth=latGridWidth;
        this.lonGridWidth=lonGridWidth;
        this.latmax=latmax;
        this.latmin=latmin;
        this.lonmax=lonmax;
        this.lonmin=lonmin;
        indexHorizontalSize = (int) ((lonmax-lonmin)/lonGridWidth) + 1;
        indexVerticalSize = (int) ((latmax-latmin)/latGridWidth) + 1;
    }

    public int getIndexHorizontalSize() {
        return indexHorizontalSize;
    }

    public int getIndexVerticalSize() {
        return indexVerticalSize;
    }

    public double getLonGridWidth() {
        return lonGridWidth;
    }

    public double getLatGridWidth() {
        return latGridWidth;
    }

    public double getLatmax() {
        return latmax;
    }

    public double getLatmin() {return latmin; }

    public double getLonmax() {
        return lonmax;
    }

    public double getLonmin() {
        return lonmin;
    }

    public Map<String, List<BasePoint>> getPoiMap() {
        return poiMap;
    }

    public void init(List<BasePoint> poiData){
        poiMap = new HashMap<>();
        for(BasePoint poi:poiData){
            int gridId = calculateGridId(poi.getLat(),poi.getLon());
            if(poiMap.containsKey(String.valueOf(gridId))){
                poiMap.get(String.valueOf(gridId)).add(poi);
            }
            else{
                poiMap.put(String.valueOf(gridId),new ArrayList<>());
                poiMap.get(String.valueOf(gridId)).add(poi);
            }
        }
    }

    public int calculateGridId(double lat,double lon){
        return ((int)((lat-latmin)/latGridWidth))*indexHorizontalSize+(int)((lon-lonmin)/lonGridWidth) +1;

    }

    public Range calculatedGridRange(int i){
        // Range r = new Range();
        int vsize = i/indexHorizontalSize;
        int hsize = i%indexHorizontalSize;
        double latdown = latmin + vsize*latGridWidth;
        double latup = latdown + latGridWidth;
        double lonleft = lonmin + (hsize-1)*lonGridWidth;
        double lonright = lonleft + lonGridWidth;
        return new Range(latdown,latup,lonleft,lonright,0,0);

    }

   /* public Range getKAnonymityRange(int k, BasePoint bp){
        int gridId = calculateGridId(bp.getLat(),bp.getLon());
        int poinum = poiMap.get(gridId).size();
        double ratio = (double) k/poinum;
        double lat = latGridWidth * Math.sqrt(ratio);
        double lon = lonGridWidth * Math.sqrt(ratio);
        double curLat = bp.getLat();
        double curLon = bp.getLon();
        Range res = new Range(curLat-lat/2,curLat+lat/2
                ,curLon - lon/2,curLon + lon/2,0,0 );
        return res;
    }*/

    public BasePoint queryNNPOI(double lat,double lon,double distanceThreshold){

        int gridId = calculateGridId(lat,lon);
        List<BasePoint> candidate = new ArrayList<>();

        int leftUpGridId = gridId + indexHorizontalSize - 1;
        int upGridId = gridId + indexHorizontalSize;
        int rightUpGridId = gridId + indexHorizontalSize + 1;
        int leftGridId = gridId - 1;
        int rightGridId = gridId + 1;
        int leftDownGridId = gridId - indexHorizontalSize - 1;
        int downGridId = gridId - indexHorizontalSize;
        int rightDownGridId = gridId -indexHorizontalSize + 1;
        if(null!=poiMap.get(leftUpGridId)){
            candidate.addAll(poiMap.get(leftUpGridId));
        }
        if(null!=poiMap.get(upGridId)){
            candidate.addAll(poiMap.get(upGridId));
        }
        if(null!=poiMap.get(rightUpGridId)){
            candidate.addAll(poiMap.get(rightUpGridId));
        }
        if(null!=poiMap.get(leftGridId)){
            candidate.addAll(poiMap.get(leftGridId));
        }
        if(null!=poiMap.get(rightGridId)){
            candidate.addAll(poiMap.get(rightGridId));
        }
        if(null!=poiMap.get(leftDownGridId)){
            candidate.addAll(poiMap.get(leftDownGridId));
        }
        if(null!=poiMap.get(downGridId)){
            candidate.addAll(poiMap.get(downGridId));
        }
        if(null!=poiMap.get(rightDownGridId)){
            candidate.addAll(poiMap.get(rightDownGridId));
        }

        if(null!=poiMap.get(gridId)){
            candidate.addAll(poiMap.get(gridId));
        }

        if(candidate.size()==0){
            return null;
        }

        double minDistance = Double.MAX_VALUE;
        BasePoint target = null;
        for(BasePoint poi:candidate){
            double distance = TrajUtil.distance(poi.getLat(),lat,poi.getLon(),lon,0,0);
            if(distance < minDistance){
                minDistance = distance;
                target = poi;
            }
        }

        if(minDistance>distanceThreshold){
            return null;
        }

        return target;

    }

    public List<BasePoint> queryNearPOI(double lat,double lon,double distanceThreshold){
        List<BasePoint> res = new ArrayList<>();
        List<BasePoint> candidateSet = new ArrayList<>();
        int gridId = calculateGridId(lat,lon);
        int leftUpGridId = gridId + indexHorizontalSize - 1;
        int upGridId = gridId + indexHorizontalSize;
        int rightUpGridId = gridId + indexHorizontalSize + 1;
        int leftGridId = gridId - 1;
        int rightGridId = gridId + 1;
        int leftDownGridId = gridId - indexHorizontalSize - 1;
        int downGridId = gridId - indexHorizontalSize;
        int rightDownGridId = gridId -indexHorizontalSize + 1;
        double gridLeftLon = (int)((lon-lonmin)/lonGridWidth)*lonGridWidth+lonmin;
        double gridRightLon = gridLeftLon + lonGridWidth;
        double gridDownLat = (int)((lat-latmin)/latGridWidth)*latGridWidth+latmin;
        double gridUpLat = gridDownLat + latGridWidth;
        if(TrajUtil.distance(gridUpLat,lat,gridLeftLon,lon,0,0) < distanceThreshold){
            if(poiMap.containsKey(leftUpGridId)){
                candidateSet.addAll(poiMap.get(leftUpGridId));
            }
        }
        if(TrajUtil.distance(gridUpLat,lat,lon,lon,0,0) < distanceThreshold){
            if(poiMap.containsKey(upGridId)){
                candidateSet.addAll(poiMap.get(upGridId));
            }
        }
        if(TrajUtil.distance(gridUpLat,lat,gridRightLon,lon,0,0) < distanceThreshold){
            if(poiMap.containsKey(rightUpGridId)){
                candidateSet.addAll(poiMap.get(rightUpGridId));
            }
        }

        if(TrajUtil.distance(lat,lat,gridLeftLon,lon,0,0) < distanceThreshold){
            if(poiMap.containsKey(leftGridId)){
                candidateSet.addAll(poiMap.get(leftGridId));
            }
        }
        if(TrajUtil.distance(lat,lat,gridRightLon,lon,0,0) < distanceThreshold){
            if(poiMap.containsKey(rightGridId)){
                candidateSet.addAll(poiMap.get(rightGridId));
            }
        }
        if(TrajUtil.distance(gridDownLat,lat,gridLeftLon,lon,0,0) < distanceThreshold){
            if(poiMap.containsKey(leftDownGridId)){
                candidateSet.addAll(poiMap.get(leftDownGridId));
            }
        }
        if(TrajUtil.distance(gridDownLat,lat,lon,lon,0,0) < distanceThreshold){
            if(poiMap.containsKey(downGridId)){
                candidateSet.addAll(poiMap.get(downGridId));
            }
        }
        if(TrajUtil.distance(gridDownLat,lat,gridRightLon,lon,0,0) < distanceThreshold){
            if(poiMap.containsKey(rightDownGridId)){
                candidateSet.addAll(poiMap.get(rightDownGridId));
            }
        }
        for(BasePoint p : candidateSet){
            if(TrajUtil.distance(p.getLat(),lat,p.getLon(),lon,0,0)<distanceThreshold){
                res.add(p);
            }
        }

        return res;
    }



}
