package whu.edu.totemdb.STCSim.Device.Handlers;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import whu.edu.totemdb.STCSim.Base.BaseTimeInterval;
import whu.edu.totemdb.STCSim.Device.Server;
import whu.edu.totemdb.STCSim.Index.TimeIntervalTree;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Map;

// Accept Grid index from mobile and merge them
public class GridIndexHandler implements HttpHandler {
    private Server s;
    private Double buildTime = Double.valueOf(0);
    private boolean indexCompressed;
    public GridIndexHandler(Server s,boolean indexCompressed){
        this.s = s;
        this.indexCompressed = indexCompressed;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        try {

                String paramStr = getRequestParam(httpExchange);
                double beforeTime = (double) System.currentTimeMillis()/1000;
                if(indexCompressed){
                    gridIndexBuildWithCompression(paramStr);
                }
                else{
                    gridIndexBuildWithoutCompression(paramStr);
                }
                // gridIndexBuildWithCompression(paramStr);
                double endTime = (double) System.currentTimeMillis()/1000;
                buildTime += endTime - beforeTime;

            // Server.serverLogger.info(String.format("Accept from device %s, grid index info %s",gij.getDeviceId(),gij.getIndexInfo()));
            Headers responseHeader = httpExchange.getResponseHeaders();
            responseHeader.set("Content-Type", "application/json;charset=utf8");
            httpExchange.sendResponseHeaders(200, 0);
            OutputStream os = httpExchange.getResponseBody();
            os.write(1);
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getRequestParam(HttpExchange httpExchange) throws Exception {
        String paramStr = "";

        if (httpExchange.getRequestMethod().equals("GET")) {
            //GET请求读queryString
            paramStr = httpExchange.getRequestURI().getQuery();
        } else {
            //非GET请求读请求体
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



    public void gridIndexBuildWithCompression(String paramStr){

        String[] vals = paramStr.split(";");
        String deviceID = vals[0];
        long basetime = Long.parseLong(vals[1]);
        Map<String,TimeIntervalTree> gridft = s.gridFootprint;
        for(int i=2;i<vals.length;i++){
            String val = vals[i];
            String[] tmps = val.split(",");
            String gridId = tmps[0];
            TimeIntervalTree tit = gridft.getOrDefault(gridId,new TimeIntervalTree());
            for(int j=1;j<= tmps.length/2;j++){
                long ts = Long.parseLong(tmps[2*j-1])+basetime;
                long te= Long.parseLong(tmps[2*j])+basetime;
                tit.addInterval(new BaseTimeInterval(deviceID,ts,te));
            }
            gridft.put(gridId,tit);
        }

    }

    public void gridIndexBuildWithoutCompression(String paramStr){

        String[] vals = paramStr.split(";");
        String deviceID = vals[0];
        // long basetime = Long.parseLong(vals[1]);
        Map<String,TimeIntervalTree> gridft = s.gridFootprint;
        for(int i=1;i<vals.length;i++){
            String val = vals[i];
            String[] tmps = val.split(",");
            String gridId = tmps[0];
            TimeIntervalTree tit = gridft.getOrDefault(gridId,new TimeIntervalTree());
            for(int j=1;j<= tmps.length/2;j++){
                long ts = Long.parseLong(tmps[2*j-1]);
                long te= Long.parseLong(tmps[2*j]);
                tit.addInterval(new BaseTimeInterval(deviceID,ts,te));
            }
            gridft.put(gridId,tit);
        }

    }

    public double getBuildTime(){
        return buildTime;
    }

    public void reset(boolean indexCompressed){
        s.gridFootprint.clear();
        buildTime = Double.valueOf(0);
    }


}
