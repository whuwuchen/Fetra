package whu.edu.totemdb.STCSim.Device;

import com.google.gson.Gson;

public class GridIndexJson {

    private String deviceId;

    private double latMin;

    private double latMax;

    private double lonMin;

    private double lonMax;

    double lonGridWidth;
    double latGridWidth;

    // index info represented by str
    private String indexInfo;

    public double getLatMax() {
        return latMax;
    }

    public double getLatMin() {
        return latMin;
    }

    public double getLonMax() {
        return lonMax;
    }

    public double getLonMin() {
        return lonMin;
    }

    public double getLatGridWidth() {
        return latGridWidth;
    }

    public double getLonGridWidth() {
        return lonGridWidth;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getIndexInfo() {
        return indexInfo;
    }

    public GridIndexJson(String deviceId, double latMin, double latMax, double lonMin, double lonMax, double latGridWidth,double lonGridWidth,String indexInfo){
        this.deviceId=deviceId;
        this.latMin = latMin;
        this.latMax = latMax;
        this.lonMin = lonMin;
        this.lonMax = lonMax;
        this.latGridWidth=latGridWidth;
        this.lonGridWidth=lonGridWidth;
        this.indexInfo = indexInfo;
    }

    public String toJson(){
        Gson gson = new Gson();
        return gson.toJson(this);
    }

}
