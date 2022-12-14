package whu.edu.totemdb.STCSim.MobileFederation.Device_v2.Handlers;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import javafx.util.Pair;
import whu.edu.totemdb.STCSim.Base.BaseTimeInterval;
import whu.edu.totemdb.STCSim.Base.Hop;
import whu.edu.totemdb.STCSim.Base.POI;
import whu.edu.totemdb.STCSim.Base.StayPoint;
import whu.edu.totemdb.STCSim.Device.Server;
import whu.edu.totemdb.STCSim.Index.GridIndex;
import whu.edu.totemdb.STCSim.Index.TimeIntervalTree;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.*;
import java.util.stream.Collectors;

public class TopKQueryHandler implements HttpHandler {
    private Server s;

    // attention to multi-thread server context, need a synchronized lock
    private Map<String,Double> allResults;

    private Map<String,Double> candidates;

    private TreeSet<Pair<String,Double>> upperBounds;

    private PriorityQueue<Double> result;

    private String queryTraj;

    private int K;

    public boolean partialMappingPruning = false;

    public Double totalQueryTime = Double.valueOf(0);

    public boolean isEnd = false;

    public boolean partialMapping = false;

    public Map<String,StringBuilder> patientTrajForEachDevices;

    public Map<String,StringBuilder> pruningpatientTraj;

    public boolean isPruning = false;

    public Double totalQueryTimeWithoutPruning = Double.valueOf(0);

    public TopKQueryHandler(Server s, boolean isPruning){
        this.s = s;
        this.candidates = Collections.synchronizedMap( new HashMap<>());

        this.allResults = Collections.synchronizedMap(new HashMap<>());

        this.patientTrajForEachDevices = new HashMap<>();

        this.pruningpatientTraj = Collections.synchronizedMap(new HashMap<>());

        this.isPruning = isPruning;
        /*this.result = new PriorityQueue<>(new Comparator<Double>() {
            @Override
            public int compare(Double o1, Double o2) {
                if(o1>=o2){
                    return 1;
                }
                else{
                    return -1;
                }
            }
        });*/
        // this.isPruning = isPruning;

        /*this.upperBounds = new TreeSet<>(new Comparator<Pair<String, Double>>() {
            @Override
            public int compare(Pair<String, Double> o1, Pair<String, Double> o2) {
                if(o1.getValue()<o2.getValue()){
                    return 1;
                }
                else if(o1.getKey().equals(o2.getKey())&& o1.getValue().equals(o2.getValue())){
                    return 0;
                }
                else{
                    return -1;
                }
            }
        });*/

    }

    /*@Override
    public void handle(HttpExchange httpExchange) throws IOException {

        try {
            double before = (double) System.currentTimeMillis()/1000;
            String paramStr = getRequestParam(httpExchange);
            Headers responseHeader = httpExchange.getResponseHeaders();
            responseHeader.set("Content-Type", "application/json;charset=utf8");
            httpExchange.sendResponseHeaders(200, 0);
            OutputStreamWriter os = new OutputStreamWriter(httpExchange.getResponseBody(),"UTF-8");

            if(paramStr.startsWith("begin")){

                String[] s = paramStr.split(":");
                String deviceId = s[1];
                if(isPruning){
                    if(candidates.containsKey(deviceId)&&candidates.get(deviceId).getValue()>0){
                        os.write(queryTraj);
                        os.close();
                    }
                    else{

                        os.write("end");
                        os.close();
                    }
                    double end = (double) System.currentTimeMillis()/1000;
                    if(!isEnd) {
                        totalQueryTime += end - before;
                    }
                }

                else{
                    os.write(queryTraj);
                    os.close();
                    double end = (double) System.currentTimeMillis()/1000;
                    if(!isEnd) {
                        totalQueryTime += end - before;
                    }
                }

            }
            // a double value for similarity
            else{
                // double end = (double) System.currentTimeMillis()/1000;
                String[] v = paramStr.split(":");
                String deviceId = v[0];
                double risk = Double.valueOf(v[1]);
                allResults.put(deviceId,risk);
                double endWithoutPruning = (double) System.currentTimeMillis()/1000;
                totalQueryTimeWithoutPruning += endWithoutPruning - before;
                if(isPruning){
                    // resultBound.add(risk);
                    if(!isEnd) {
                        if (updateUpperBound(deviceId, risk)) {
                            isEnd = true;
                            // Server.serverLogger.info(String.format("Top k query end in %2f second"));
                        }
                    }
                }
                os.write(1);
                os.close();
                double end =(double) System.currentTimeMillis()/1000;
                if(!isEnd){
                    totalQueryTime += end-before;
                }


            }

        } catch (Exception e) {
            e.printStackTrace();
        }


    }*/

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {

        try {
            // double before = (double) System.currentTimeMillis()/1000;
            String paramStr = getRequestParam(httpExchange);
            Headers responseHeader = httpExchange.getResponseHeaders();
            responseHeader.set("Content-Type", "application/json;charset=utf8");
            httpExchange.sendResponseHeaders(200, 0);
            OutputStreamWriter os = new OutputStreamWriter(httpExchange.getResponseBody(),"UTF-8");

            if(paramStr.startsWith("begin")){

                String[] s = paramStr.split(":");
                String deviceId = s[1];
                if(partialMapping){
                    if(partialMappingPruning){
                        os.write(patientTrajForEachDevices.get(deviceId).toString());
                    }
                    else{
                        os.write(queryTraj);
                    }

                }
                /*else{
                    os.write(queryTraj);
                }*/
                else if(isPruning) {
                    //os.write(queryTraj);
                    os.write(pruningpatientTraj.get(deviceId).toString());
                }
                else{
                    os.write(queryTraj);
                }

                os.close();
            }
            // a double value for similarity
            else{
                // double end = (double) System.currentTimeMillis()/1000;
                String[] v = paramStr.split(":");
                String deviceId = v[0];
                double risk = Double.valueOf(v[1]);
                allResults.put(deviceId,risk);
                os.write(1);
                os.close();

            }

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private String getRequestParam(HttpExchange httpExchange) throws Exception {
        String paramStr = "";

        if (httpExchange.getRequestMethod().equals("GET")) {
            //GET?????????queryString
            paramStr = httpExchange.getRequestURI().getQuery();
        } else {
            //???GET??????????????????
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpExchange.getRequestBody(), "utf-8"));
            StringBuilder requestBodyContent = new StringBuilder();
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                requestBodyContent.append(line);
            }
            paramStr = requestBodyContent.toString();

        }

        return paramStr;
    }


    public void topKQuery(List<Hop> Q,int k){
        GridIndex gi = s.getGlobalGridIndex();
        this.K = k;
        Map<String, TimeIntervalTree> titMap = s.gridFootprint;
        Q.parallelStream().forEach(hop->{
            POI p = hop.getPoi();
            String gridId = String.valueOf(gi.calculateGridId(hop.getPoi().getLat(),hop.getPoi().getLon()));
            BaseTimeInterval li = new BaseTimeInterval("",hop.getStarttime(),hop.getEndtime());
            if(titMap.containsKey(gridId)){
                TimeIntervalTree tit = titMap.get(gridId);
                List<BaseTimeInterval> btis = tit.overlap(li);
                btis.parallelStream().forEach(bti->
                {
                    String deviceId = bti.getTrajId();
                    double risk = Math.max(0,Math.min(bti.getEnd(),li.getEnd())-Math.max(bti.getStart(),li.getStart()));
                    candidates.put(deviceId, risk+candidates.getOrDefault(deviceId,0.0));
                    if(partialMappingPruning) {
                    StringBuilder sb = patientTrajForEachDevices.getOrDefault(deviceId, new StringBuilder());
                    sb.append(p.getLat()).append(",").append(p.getLon()).append(",")
                            .append(hop.getStarttime()).append(",").append(hop.getEndtime()).append(";");
                    patientTrajForEachDevices.put(deviceId, sb);
                    }
                    synchronized (this){
                        StringBuilder sb = pruningpatientTraj.getOrDefault(deviceId,new StringBuilder());
                        sb.append(p.getId()).append(",").append(hop.getStarttime()).append(",").append(hop.getEndtime()).append(";");
                        pruningpatientTraj.put(deviceId,sb);
                    }
                });

            }
        });

        queryTraj = q2String(Q);
    }

    public double getTotalQueryTime() {
        return totalQueryTime;
    }

    public Double getTotalQueryTimeWithoutPruning() {
        return totalQueryTimeWithoutPruning;
    }

    public String traj2String(List<StayPoint> riskyTraj){
        List<String> str = riskyTraj.stream().map(s->s.toString()).collect(Collectors.toList());
        String r = String.join(";",str);
        return r;
    }

    public String q2String(List<Hop> Q){
        List<String> str = Q.stream()
                        .map(s->s.getPoi().getId()+","+s.getStarttime()+","+s.getEndtime())
                        .collect(Collectors.toList());
        String r = String.join(";",str);
        return r;
    }

    public String q2StayPointsString(List<Hop> Q){
        List<String> str = Q.stream()
                .map(s->s.getPoi().getLat()+","+s.getPoi().getLon()+","+s.getStarttime()+","+s.getEndtime())
                .collect(Collectors.toList());
        String r = String.join(";",str);
        return r;
    }

    public void outputResult(){
        List<Pair<String,Double>> res = new ArrayList<>();
        for(Map.Entry<String,Double> e : allResults.entrySet()){
            res.add(new Pair<>(e.getKey(),e.getValue()));
        }
        res.sort((a,b)-> (int) (b.getValue()-a.getValue()));
        res.stream().forEach((p)->Server.serverLogger.info(String.format("Device %s risk is %2f",p.getKey(),p.getValue())));
    }

    public void reset(){
        allResults.clear();
        candidates.clear();
        patientTrajForEachDevices.clear();
        pruningpatientTraj.clear();
        // result.clear();

    }

    public void reset(boolean isPruning){
        this.isPruning = isPruning;
        allResults.clear();
        candidates.clear();
        patientTrajForEachDevices.clear();
        pruningpatientTraj.clear();
    }

    public Map<String, Double> getCandidates() {
        return candidates;
    }

    public Map<String, Double> getAllResults() {
        return allResults;
    }
}
