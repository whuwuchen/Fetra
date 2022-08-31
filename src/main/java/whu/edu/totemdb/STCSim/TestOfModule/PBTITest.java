package whu.edu.totemdb.STCSim.TestOfModule;

import javafx.util.Pair;
import whu.edu.totemdb.STCSim.MobileFederation.Device_v2.PBTI;

import java.util.Set;

public class PBTITest {
    public static void main(String[] args){
        PBTI.setDayLength(16);
        PBTI.setLevel(4);
        PBTI pbti = new PBTI();
        //pbti.addTemporalRange(new Pair<>(1,new Pair<>(5,9)));
        //pbti.addTemporalRange(new Pair<>(2,new Pair<>(1,8)));
        //pbti.addTemporalRange(new Pair<>(3,new Pair<>(2,15)));
        pbti.addRange(1,5,9);
        pbti.addRange(2,1,8);
        pbti.addRange(3,2,15);
        pbti.addRange(1,7,8);
        Set<Integer> res =pbti.normalizedRangeQuery(1,6);
        System.out.println(res);
    }

    public static void testIntegerRange(){
        PBTI.setLevel(4);
        PBTI pbti = new PBTI();
        pbti.addTemporalRange(new Pair<>(1,new Pair<>(5,9)));
        pbti.addTemporalRange(new Pair<>(2,new Pair<>(1,8)));
        pbti.addTemporalRange(new Pair<>(3,new Pair<>(2,15)));
        Set<Integer> res =pbti.rangeQuery(1,15);
        System.out.println(res);
    }
}
