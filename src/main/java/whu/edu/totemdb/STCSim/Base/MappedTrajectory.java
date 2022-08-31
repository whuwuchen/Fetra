package whu.edu.totemdb.STCSim.Base;

import java.util.List;

public class MappedTrajectory {
    private List<Hop> hops;
    private RawTrajectory rawTraj;
    private int mappedTrajId;
    public MappedTrajectory(RawTrajectory rt){
        this.rawTraj = rt;
    }

    public MappedTrajectory(RawTrajectory rawTraj,List<Hop> hops){
        this.rawTraj=rawTraj;
        this.hops=hops;
    }

    public void setHops(List<Hop> hops) {
        this.hops = hops;
    }

    public List<Hop> getHops() {
        return hops;
    }

    public void setRawTraj(RawTrajectory rawTraj) {
        this.rawTraj = rawTraj;
    }

    public RawTrajectory getRawTraj() {
        return rawTraj;
    }

    public void setMappedTrajId(int mappedTrajId) {
        this.mappedTrajId = mappedTrajId;
    }

    public int getMappedTrajId() {
        return mappedTrajId;
    }

}
