package whu.edu.totemdb.STCSim.Base;

import com.github.davidmoten.rtree.geometry.Geometry;
import com.github.davidmoten.rtree.geometry.Rectangle;

public class Point /*implements Geometry, BasePoint*/ {
    private double lat;
    private double lon;
    private long timestamp;

    public Point(double lat, double lon, long timestamp){
        this.lat=lat;
        this.lon=lon;
        this.timestamp=timestamp;
    }

    public double getLat(){
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

   /*    @Override
    public double distance(Rectangle rectangle) {
        return 0;
    }

    @Override
    public Rectangle mbr() {
        return null;
    }

    @Override
    public boolean intersects(Rectangle rectangle) {
        return false;
    }*/


    public String toString(){
        return String.format("(%f,%f)",lat,lon);
    }

}
