package whu.edu.totemdb.STCSim.Device;

import me.lemire.integercompression.differential.IntegratedIntCompressor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import whu.edu.totemdb.STCSim.Base.*;
import whu.edu.totemdb.STCSim.Index.GridIndex;
import whu.edu.totemdb.STCSim.Index.HopInvertedIndex;
import whu.edu.totemdb.STCSim.Index.LocalRTreeIndex;
import whu.edu.totemdb.STCSim.Settings;
import whu.edu.totemdb.STCSim.StayPointDetection.SPExtrator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class MobileDevice {

    private static final Log mobileLogger = LogFactory.getLog(MobileDevice.class);

    private boolean isRiskyUser = false;

    // local activity range
    private Range localRange;

    private String deviceId = "";

    // private List<POI> localPois;

    private List<RawTrajectory> rawTrajectories;

    private List<MappedTrajectory> mappedTrajectoryList;

    private List<StayPoint> stayPoints;

    private SPExtrator sp;

    private LocalRTreeIndex localRTreeIndex;

    private Map<Integer,List<TimeInterval>> gridAbbr;

    // local index of stay points
    GridIndex localGridIndex;

    HopInvertedIndex gridInvertedIndex;

    private double deviceRisk;

    private boolean partialMapping = false ;

    private long baseTimeStamp;

    private boolean hasLocalBoost = false;

    private boolean isLog = false;

    private static IntegratedIntCompressor iic = new IntegratedIntCompressor();

    private boolean indexCompressed = false;

    private Map<String, List<Hop>> hash = new HashMap<>();

    /*public MobileDevice(String deviceId,Range range, List<POI> pois,TrajectoryGenerator tg){
        this.localRange = range;
        this.deviceId = deviceId;

        localPois = pois;
        localGridIndex = new GridIndex(localRange.getlonMax(),localRange.getlonMin()
                ,localRange.getlatMax(),localRange.getlatMin()
                ,localRange.getLatGridWidth(),localRange.getLonGridWidth());
        localGridIndex.init(new ArrayList<>(localPois));
        sp = new SPExtrator(localGridIndex);

        stayPoints = new ArrayList<>();
        gridAbbr = new HashMap<>();
        deviceRisk = 0;
    }*/

    public MobileDevice(String deviceId,GridIndex globalGridIndex,List<RawTrajectory> rawTrajectoryList,SPExtrator sp){
        this.deviceId = deviceId;
        this.localGridIndex = globalGridIndex;
        this.rawTrajectories = rawTrajectoryList;
        this.sp=sp;
        gridAbbr = new HashMap<>();
        baseTimeStamp = Long.MAX_VALUE;
    }

    public MobileDevice(String deviceId,GridIndex globalGridIndex,List<RawTrajectory> rawTrajectoryList,SPExtrator sp,boolean hasLocalBoost,boolean indexCompressed){
        this.deviceId = deviceId;
        this.localGridIndex = globalGridIndex;
        this.rawTrajectories = rawTrajectoryList;
        this.sp=sp;
        stayPoints = new ArrayList<>();
        gridAbbr = new HashMap<>();
        baseTimeStamp = Long.MAX_VALUE;
        this.hasLocalBoost = hasLocalBoost;
        this.indexCompressed = indexCompressed;

    }

    public MobileDevice(String deviceId,GridIndex globalGridIndex,List<RawTrajectory> rawTrajectoryList,SPExtrator sp,boolean hasLocalBoost,boolean indexCompressed,boolean partialMapping){
        this.deviceId = deviceId;
        this.localGridIndex = globalGridIndex;
        this.rawTrajectories = rawTrajectoryList;
        this.sp=sp;
        stayPoints = new ArrayList<>();
        gridAbbr = new HashMap<>();
        baseTimeStamp = Long.MAX_VALUE;
        this.hasLocalBoost = hasLocalBoost;
        this.indexCompressed = indexCompressed;
        this.partialMapping = partialMapping;
    }

    public MobileDevice(String deviceId,GridIndex globalGridIndex, List<MappedTrajectory> mappedTrajectories,boolean hasLocalBoost,boolean indexCompressed){
        this.deviceId = deviceId;
        this.localGridIndex = globalGridIndex;
        this.mappedTrajectoryList = mappedTrajectories;
        this.hasLocalBoost = hasLocalBoost;
        this.indexCompressed = indexCompressed;
        gridAbbr = new HashMap<>();
        baseTimeStamp = Long.MAX_VALUE;
    }

    public void logInfo(String info){
        if(isLog){
            mobileLogger.info(info);
        }

    }

    public void matchRawTrajectories(double radius){

        double beforeMatch = (double)System.currentTimeMillis()/1000;
        mappedTrajectoryList=sp.extractBatch(rawTrajectories,
                Settings.trajectoryClusterDistanceThreshold,
                Settings.trajectoryClusterTimeThreshold,radius);
        double endMatch = (double) System.currentTimeMillis()/1000;
        // mobileLogger.info(String.format("Device %s map %d trajectories in %2f seconds",deviceId,rawTrajectories.size(),(endMatch-beforeMatch)));
        logInfo(String.format("Device %s map %d trajectories in %2f seconds",deviceId,rawTrajectories.size(),(endMatch-beforeMatch)));
    }


    public void partialLocalIndex(){
        stayPoints = sp.extractStayPointsBatch(rawTrajectories,
                Settings.trajectoryClusterDistanceThreshold,Settings.trajectoryClusterTimeThreshold);
        for(StayPoint sp:stayPoints){
            int id = localGridIndex.calculateGridId(sp.getLat(),sp.getLon());
            List<TimeInterval> til = gridAbbr.getOrDefault(id, new ArrayList<>());
            baseTimeStamp = Math.min(sp.getStartTime(),baseTimeStamp);
            til.add(new TimeInterval(sp.getStartTime(),sp.getEndTime()));
            gridAbbr.put(id,til);
        }
        localRTreeIndex = new LocalRTreeIndex(stayPoints);

    }

    // 通过病例轨迹(映射后的轨迹)来匹配
    public double matchRawTrajectories(List<StayPoint> stayPoints, double radius){
        /*List<StayPoint> stayPoints = sp.extractStayPointsBatch(rawTrajectories,
                Settings.trajectoryClusterDistanceThreshold,Settings.trajectoryClusterTimeThreshold);
        localRTreeIndex = new LocalRTreeIndex(stayPoints);*/
        //double risk = 0;

        return stayPoints.stream().mapToDouble(sp -> {
            List<StayPoint> sps = localRTreeIndex.queryStayPoints(sp.getLat(),sp.getLon(),radius);
            return sps.stream().mapToDouble(sp1->Math.max(0,Math.min(sp.getEndTime(),sp1.getEndTime())
                    -Math.max(sp.getStartTime(),sp1.getStartTime()))).sum();
        }).sum();

        /*for(Hop hop:hops){
            List<StayPoint> sps = localRTreeIndex.queryStayPoints(hop.getPoi().getLat(),hop.getPoi().getLon(),radius);
            risk+=sps.stream().mapToDouble(sp->Math.max(0,Math.min(sp.getEndTime(),hop.getEndtime())
                    -Math.max(sp.getStartTime(),hop.getStarttime()))).sum();
        }*/
        // return risk;

    }



/*    public void extractStayPoints(){
        stayPoints = rawTrajectories.parallelStream()
                .map(s->sp.extractStayPoints(s,Settings.trajectoryClusterDistanceThreshold,Settings.trajectoryClusterTimeThreshold))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        for(int i=0;i<stayPoints.size();i++){
            //stayPoints.get(i).setId(String.format("%s_%d",deviceId,i));
            stayPoints.get(i).setId(String.format("%d",i));
        }

    }*/

    /*public void generateStayPoint(int num){
        // Settings.windowSize/Settings.maxStayTime , the maximum number of staypoint ;
        int maxStayTime = Settings.windowSize/num;
        int curTimeStamp = 0;
        int poisize = localPois.size();
        if(poisize==0)
            return;
        for(int i=0;i<num;i++){
            int poiId = (int) (Math.random()*poisize-1);
            POI p = localPois.get(poiId);
            int stayTime = (int) (Math.random()*maxStayTime+Settings.trajectoryClusterTimeThreshold);
            StayPoint sp = new StayPoint(p.getLat(),p.getLon(),curTimeStamp,curTimeStamp+stayTime);
            curTimeStamp = curTimeStamp+stayTime;
            sp.setId(String.valueOf(p.getId()));
            stayPoints.add(sp);

        }
        indexStayPoint();
    }*/

/*    public void indexStayPoint(){
        double beforeIndex = (double) System.currentTimeMillis()/1000;
        ArrayList<BasePoint> s = new ArrayList<>(stayPoints);
        localGridIndex.init(s);
        gridInvertedIndex = new HopInvertedIndex();
        List<Integer> grids=s.parallelStream()
                .map(p->localGridIndex.calculateGridId(p.getLat(),p.getLon()))
                .collect(Collectors.toList());
        gridInvertedIndex.initFromStayPoints(stayPoints,grids);
        for(StayPoint sp : stayPoints){
            int id = localGridIndex.calculateGridId(sp.getLat(),sp.getLon());
            List<TimeInterval> til = gridAbbr.getOrDefault(id, new ArrayList<>());
            til.add(new TimeInterval(sp.getStartTime(),sp.getEndTime()));
            gridAbbr.put(id,til);
        }
        double endIndex = (double) System.currentTimeMillis()/1000;
        mobileLogger.info(String.format("Device %s build index for %d trajs : %2f seconds",
                deviceId,mappedTrajectoryList.size(),(endIndex-beforeIndex)));

    }*/

    public void indexCheckinData(){
        for(MappedTrajectory mt:mappedTrajectoryList){
            for(Hop h:mt.getHops()){
                POI p = h.getPoi();
                int id = localGridIndex.calculateGridId(p.getLat(),p.getLon());
                List<TimeInterval> til = gridAbbr.getOrDefault(id, new ArrayList<>());
                baseTimeStamp = Math.min(h.getStarttime(),baseTimeStamp);
                til.add(new TimeInterval(h.getStarttime(),h.getEndtime()));
                gridAbbr.put(id,til);
            }
        }

        for(MappedTrajectory mt:mappedTrajectoryList){
            for(Hop h:mt.getHops()){
                String poiid = String.valueOf(h.getPoi().getId());
                List<Hop> localhops = hash.getOrDefault(poiid,new ArrayList<>());
                localhops.add(h);
                hash.put(poiid,localhops);
            }
        }


    }

    public void indexMappedTraj(){
        double beforeIndex = (double) System.currentTimeMillis()/1000;
        gridInvertedIndex = new HopInvertedIndex();
        gridInvertedIndex.initFromMappedTrajectories(mappedTrajectoryList);
        double endIndex = (double) System.currentTimeMillis()/1000;

        logInfo(String.format("Device %s build index for %d trajs : %2f seconds",
                deviceId,mappedTrajectoryList.size(),(endIndex-beforeIndex)));
        for(MappedTrajectory mt:mappedTrajectoryList){
            for(Hop h:mt.getHops()){
                POI p = h.getPoi();
                int id = localGridIndex.calculateGridId(p.getLat(),p.getLon());
                List<TimeInterval> til = gridAbbr.getOrDefault(id, new ArrayList<>());
                baseTimeStamp = Math.min(h.getStarttime(),baseTimeStamp);
                til.add(new TimeInterval(h.getStarttime(),h.getEndtime()));
                gridAbbr.put(id,til);
            }
        }

        for(MappedTrajectory mt:mappedTrajectoryList){
            for(Hop h:mt.getHops()){
                String poiid = String.valueOf(h.getPoi().getId());
                List<Hop> localhops = hash.getOrDefault(poiid,new ArrayList<>());
                localhops.add(h);
                hash.put(poiid,localhops);
            }
        }


    }

    public double calculateRiskWithHashJoin(List<Hop> hops){
        double risk = 0;
        /* Map<String, List<Hop>> hash = new HashMap<>();
        for(MappedTrajectory mt:mappedTrajectoryList){
            for(Hop h:mt.getHops()){
                String poiid = String.valueOf(h.getPoi().getId());
                List<Hop> localhops = hash.getOrDefault(poiid,new ArrayList<>());
                localhops.add(h);
                hash.put(poiid,localhops);
            }
        }*/
        /*for(Hop s:hops){
            String id = s.getId();
            // int id = s.getPoi().getId();
            if(hash.containsKey(id)){
                List<Hop> hops1 = hash.get(id);
                for(Hop p:hops1){
                    risk+= Math.max(0,Math.min(p.getEndtime(),s.getEndtime())-Math.max(p.getStarttime(),s.getStarttime()));
                }
            }
        }*/
        return hops.stream().mapToDouble(hop->{
            if(hash.containsKey(hop.getId())){
                return hash.get(hop.getId()).stream().mapToDouble(p-> Math.max(0,Math.min(p.getEndtime(),hop.getEndtime())-Math.max(p.getStarttime(),hop.getStarttime()))).sum();
            }
            else{
                return 0;
            }
        }).sum();
        // return risk;
    }

    public double calculateRiskDirectly(List<Hop> hops){
        double risk = 0;
        //List<Hop> localHops = mappedTrajectoryList.parallelStream().map(t->t.getHops()).flatMap(Collection::stream).collect(Collectors.toList());
        List<Hop> localHops = mappedTrajectoryList.stream().map(t->t.getHops()).flatMap(Collection::stream).collect(Collectors.toList());

        //risk = hops.stream().mapToDouble(h->calculateRiskWithHopList(h,localHops)).sum();

        for(Hop hop:hops){
            for(MappedTrajectory mappedTrajectory:mappedTrajectoryList){
                List<Hop> hops1 = mappedTrajectory.getHops();
                // risk+=calculateRiskWithHopList(hop,hops1);
                for(Hop hop1:hops1){
                    if(hop1.getPoi().getId()==Integer.parseInt(hop.getId())){
                        risk+=Math.max(0,Math.min(hop.getEndtime(),hop1.getEndtime())-Math.max(hop.getStarttime(),hop1.getStarttime()));
                    }
                    risk+= hop1.getPoi().getId()!=Integer.parseInt(hop.getId()) ? 0: Math.max(0,Math.min(hop.getEndtime(),hop1.getEndtime())-Math.max(hop.getStarttime(),hop1.getStarttime()));
                }
            }
        }

        return risk;
    }

    /*public double calculateRisk(List<StayPoint> riskyTraj){
        double risk = 0;
        // use hash join to calculate the similarity

        Map<String,List<StayPoint>> hash = new HashMap<>();
        for(StayPoint sp:stayPoints){
            List<StayPoint> sps = hash.getOrDefault(sp.getId(),new ArrayList<>());
            sps.add(sp);
            hash.put(sp.getId(),sps);
        }

        for(StayPoint sp:riskyTraj){
            String id = sp.getId();
            if(hash.containsKey(id)){
                List<StayPoint> sps = hash.get(id);
                for(StayPoint p: sps){
                    risk+=Math.max(0,Math.min(sp.getEndTime(),p.getEndTime())-Math.max(sp.getStartTime(),p.getStartTime()));
                }

            }
        }

        return risk;
    }*/

    // send grid index info to server
    /*public void sendGridIndexToServer(String url, TimeInterval temporalRange){
        //
        List<String> rangeList = new ArrayList<>();
        for(Map.Entry<Integer, TimeIntervalTree> e : gridInvertedIndex.invertedIndexs.entrySet()){
            int gridId = e.getKey();
            TimeIntervalTree timeIntervalTree = e.getValue();
            List<BaseTimeInterval> res = timeIntervalTree.overlap(new LongInterval(temporalRange.getStartTime(),temporalRange.getEndTime()));
            if(res.size()==0){
                continue;
            }
            Range range = localGridIndex.calculatedGridRange(gridId);
            String s = String.format("%5f,%5f,%5f,%5f",range.getlatMin(),range.getlatMax(),range.getlonMin(),range.getlatMax());
            rangeList.add(s);
        }
        String r = String.join(";", rangeList);
        String temporal = String.format("%d,%d",temporalRange.getStartTime(),temporalRange.getEndTime());
        String jsonstr = new MobileRequestJson(deviceId,r,temporal).toString();
        postRequest(url,jsonstr);

    }*/

    public double calculateRiskWithHopList(Hop h, List<Hop> hops){
        // double risk = 0;
        return hops.stream().mapToDouble(hop->calculateRiskWithHop(h,hop)).sum();
    }

    public double calculateRiskWithHop(Hop h1,Hop h2){
        if(Integer.parseInt(h1.getId())==h2.getPoi().getId()){
            return Math.max(0,Math.min(h1.getEndtime(),h2.getEndtime())-Math.max(h1.getStarttime(),h2.getStarttime()));
        }
        else{
            return 0;
        }
    }

    public void sendLocalIndexToServer(String url){
        StringBuilder indexInfo = new StringBuilder();
        // 采用delta编码的方式压缩索引信息，同时设置基址时间，将其他时间转换为基址时间上的偏移量
        // 以进一步压缩
        indexInfo.append(deviceId).append(";");
        indexInfo.append(baseTimeStamp).append(";");
        StringBuilder indexInfoWithoutCompressed = new StringBuilder();
        indexInfoWithoutCompressed.append(deviceId).append(";");
        for(Integer i:gridAbbr.keySet()){
            indexInfo.append(i).append(",");
            indexInfoWithoutCompressed.append(i).append(",");
            List<TimeInterval> timeIntervals = gridAbbr.get(i);
            timeIntervals.sort((o1, o2) -> (int) (o1.getStartTime()-o2.getStartTime()));
            if(indexCompressed){
                List<String> tiStrings = timeIntervals.stream().map(s->s.toStringWithOffset(baseTimeStamp)).collect(Collectors.toList());
                String tiString = String.join(",",tiStrings);
                indexInfo.append(tiString).append(";");
            }
            else{
                List<String> tiStringsWithoutCompressed = timeIntervals.stream().map(s->s.toString()).collect(Collectors.toList());
                String tiStringWithoutCompressed = String.join(",",tiStringsWithoutCompressed);
                indexInfoWithoutCompressed.append(tiStringWithoutCompressed).append(";");
            }

            //List<String> tiStrings = timeIntervals.stream().map(s->s.toStringWithOffset(baseTimeStamp)).collect(Collectors.toList());
            //List<String> tiStringsWithoutCompressed = timeIntervals.stream().map(s->s.toString()).collect(Collectors.toList());
            //String tiString = String.join(",",tiStrings);
            //String tiStringWithoutCompressed = String.join(",",tiStringsWithoutCompressed);
            //indexInfo.append(tiString).append(";");
            //indexInfoWithoutCompressed.append(tiStringWithoutCompressed).append(";");

        }

        logInfo(String.format("Uncompressed index size %d bytes, Compressed index size %d bytes",
                indexInfoWithoutCompressed.length(),indexInfo.length()));
        if(indexCompressed){
            postRequest(url,indexInfo.toString());
        }
        else{
            postRequest(url,indexInfoWithoutCompressed.toString());
        }
        //postRequest(url,indexInfo.toString());

    }

    // 压缩本地索引
    public List<Long> compressLocalIndex(){
        StringBuilder compressedLocalIndex = new StringBuilder();
        StringBuilder uncompressedLocalIndex = new StringBuilder();
        for(String s : hash.keySet()){
            compressedLocalIndex.append(s).append(",");
            uncompressedLocalIndex.append(s).append(",");
            List<Hop> hops = hash.get(s);
            for(Hop h:hops){
                long ts = h.getStarttime();
                long te = h.getEndtime();
                compressedLocalIndex.append(ts-baseTimeStamp).append(te-baseTimeStamp).append(",");
                uncompressedLocalIndex.append(ts).append(te).append(",");
            }
        }
        String compressedIndexFileName = "compressedIndex_"+deviceId;
        String uncompressedIndexFileName = "uncompressedIndex_"+deviceId;
        File compressedIndexFile = new File(compressedIndexFileName);
        File uncompressedIndexFile = new File(uncompressedIndexFileName);
        List<Long> fileSize = new ArrayList<>(2);

        if(compressedIndexFile.exists()){
            compressedIndexFile.delete();
        }
        if(uncompressedIndexFile.exists()){
            uncompressedIndexFile.delete();
        }
        try{
            BufferedWriter compressedIndexWriter = new BufferedWriter(new FileWriter(compressedIndexFile));
            BufferedWriter uncompressedIndexWriter = new BufferedWriter(new FileWriter(uncompressedIndexFile));
            compressedIndexWriter.write(compressedLocalIndex.toString());
            uncompressedIndexWriter.write(uncompressedLocalIndex.toString());
            compressedIndexWriter.close();
            uncompressedIndexWriter.close();

            long compressedIndexSize = compressedIndexFile.length();
            long uncompressedIndexSize = uncompressedIndexFile.length();
            fileSize.add(compressedIndexSize);
            fileSize.add(uncompressedIndexSize);
            if(compressedIndexFile.exists()){
                compressedIndexFile.delete();
            }
            if(uncompressedIndexFile.exists()){
                uncompressedIndexFile.delete();
            }

        }
        catch (Exception e){
            e.printStackTrace();
        }
        finally {

        }
        return fileSize;

    }

    // 压缩上传至中央端的索引信息
    public List<Integer> compressIndexInfo(){
        StringBuilder compressedIndexInfo = new StringBuilder();
        StringBuilder uncompressedIndexInfo = new StringBuilder();
        List<Integer> indexSize = new ArrayList<>(2);
        compressedIndexInfo.append(baseTimeStamp).append(deviceId);
        for(Integer i:gridAbbr.keySet()){
            List<TimeInterval> tis = gridAbbr.get(i);
            compressedIndexInfo.append(i).append(",");
            for(TimeInterval ti:tis){
                compressedIndexInfo.append(ti.toStringWithOffset(baseTimeStamp)).append(",");
                uncompressedIndexInfo.append(ti.toString()).append(",");

            }
        }
        indexSize.add(compressedIndexInfo.length());
        indexSize.add(uncompressedIndexInfo.length());
        return indexSize;
    }

    public void postRequest(String url,String jsonStr){
        /*
        * url: server url
        * jsonStr: json string for response in json format
        *
        */
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost httpPost = new HttpPost(url);
        StringEntity se = new StringEntity(jsonStr,"UTF-8");
        httpPost.setEntity(se);
        httpPost.setHeader("Content-Type", "application/json;charset=utf8");
        CloseableHttpResponse response = null;
        try {
            // Send Post request from client
            response = httpClient.execute(httpPost);
            // Get entity from response
            HttpEntity responseEntity = response.getEntity();

            //mobileLogger.info(String.format("Device %s Response status : %s", deviceId, response.getStatusLine()));

            if (responseEntity != null) {
                String responseStr = EntityUtils.toString(responseEntity);
                //mobileLogger.info(String.format("Device %s Response length : %d", deviceId, responseEntity.getContentLength()));
                //mobileLogger.info(String.format("Device %s Response content : %s", deviceId ,responseStr));

                // begin top-k query
                if(jsonStr.equals("begin:"+deviceId)){
                    if(responseStr.equals("end")){
                        //mobileLogger.info(String.format("Device %s is no risk", deviceId));
                    }
                    else {
                        // List<StayPoint> sps = extractStayPointsFromResponse(responseStr);

                        //List<Hop> hops = extractHopsFromResponse(responseStr);
                        double risk = 0;
                        if(partialMapping){
                            List<StayPoint> stayPoints = extractStayPointsFromResponse(responseStr);
                            risk = matchRawTrajectories(stayPoints,Settings.radius/1000);
                        }
                        else if(hasLocalBoost){
                            List<Hop> hops = extractHopsFromResponse(responseStr);
                            risk = calculateRiskWithHashJoin(hops);
                        }
                        else{
                            List<Hop> hops = extractHopsFromResponse(responseStr);
                            risk = calculateRiskDirectly(hops);
                        }
                        deviceRisk = risk;
                        //mobileLogger.info(String.format("Device %s risk is %2f", deviceId, risk));
                    }
                }
            }
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        } finally {
            try {
                // Release resource
                if (httpClient != null) {
                    httpClient.close();
                }
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public List<MappedTrajectory> getMappedTrajectoryList() {
        return mappedTrajectoryList;
    }

    public List<Hop> extractHopsFromResponse(String response){
        // <hopid,t_s,t_e>
        List<Hop> hops = new ArrayList<>();
        String[] vals = response.split(";");
        for(String val : vals){
            if(val.length()==0){
                continue;
            }
            String[] vs = val.split(",");
            String id = vs[0];
            long startTime = Long.parseLong(vs[1]);
            long endtime = Long.parseLong(vs[2]);
            Hop hop = new Hop(id,startTime,endtime);
            hops.add(hop);
        }

        return hops;
    }


    // "lat,lon,(ts,te)+;"
    public List<StayPoint> extractStayPointsFromResponse(String response){
        List<StayPoint> stayPoints = new ArrayList<>();
        String[] vals = response.split(";");
        for(String s:vals){
            if(s.length()==0) {
                continue;
            }
            String[] tmps = s.split(",");
            double lat = Double.parseDouble(tmps[0]);
            double lon = Double.parseDouble(tmps[1]);
            for(int i=1;i<tmps.length/2;i++){
                long ts = Long.parseLong(tmps[2*i]);
                long te = Long.parseLong(tmps[2*i+1]);
                stayPoints.add(new StayPoint(lat,lon,ts,te));
            }
        }
        return stayPoints;
    }

    /*public List<StayPoint> extractStayPointsFromResponse(String response){
        List<StayPoint> sps = new ArrayList<>();
        String[] vals = response.split(";");
        for(String val : vals){
            String[] vs = val.split(",");
            String id = vs[0];
            long startTime = Long.valueOf(vs[1]);
            long endTime = Long.valueOf(vs[2]);
            double lat = Double.valueOf(vs[3]);
            double lon = Double.valueOf(vs[4]);
            StayPoint sp = new StayPoint(lat,lon,startTime,endTime);
            sp.setId(id);
            sps.add(sp);
        }
        return sps;
    }*/

    public List<StayPoint> getStayPoints() {
        return stayPoints;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public double getRisk() {
        return deviceRisk;
    }
}
