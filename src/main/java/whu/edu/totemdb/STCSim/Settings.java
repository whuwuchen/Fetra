package whu.edu.totemdb.STCSim;

public class Settings {
    private Settings(){}
    public static long trajectorySplitTimeThreshold = 5*60;
    public static double trajectoryClusterDistanceThreshold = 50;
    public static long trajectoryClusterTimeThreshold = 3*60;
    public static double maxLonBeiJing = 117;
    public static double minLonBeiJing = 116;
    public static double maxLatBeiJing = 41;
    public static double minLatBeiJing = 39;
    public static double latGridWidth = 0.005;
    public static double lonGridWidth = 0.005;
    public static double maxLatNewYork = 41;
    public static double minLatNewYork = 40.5;
    public static double maxLonNewYork = -73.6;
    public static double minLonNewYork = -74.3;

    public static double maxLatTokyo = 35.9;
    public static double minLatTokyo = 35.5;
    public static double maxLonTokyo = 140;
    public static double minLonTokyo = 139.4;

    public static double poiExtractDistanceThreshold = 50;
    public static int poiExtractTimeThreshold = 5*60;
    // public static double gridIndexWidth = ;
    // window size of pandemic
    public static int windowSize =14*24*3600;


    public static double BeiJingOSMRange[] = {116.0800004246831,116.76997952394142,39.680010222196806,40.17999355152235};

    public static int BeiJingSplitNum = 50; // 50*50

    public static double deltaDegree = 9.09 * Math.pow(10,-6);

    public static double speedOfFoot = 2; // m/s

    public static int samplingRate = 5;

    public static int maxStayTime = 30*60;

    public static double radius = 50;
}
