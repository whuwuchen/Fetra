package whu.edu.totemdb.STCSim.TestOfModule;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import whu.edu.totemdb.STCSim.Device.GridIndexJson;

public class JsonStringTest {
    private static Log logger = LogFactory.getLog(JsonStringTest.class);
    public static void main(String[] args){
        GridIndexJson gridIndexJson = new GridIndexJson("0",0,0,0,0,0,0,"null");
        logger.info(String.format("Device %d grid json: %s",0,gridIndexJson.toJson()));
    }
}
