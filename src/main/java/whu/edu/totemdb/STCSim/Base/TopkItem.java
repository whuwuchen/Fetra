package whu.edu.totemdb.STCSim.Base;

public class TopkItem {
    // 利用相似度得到的排序位置
    private int rank;
    // 在ground truth set中对应的相似性值
    private int grade;
    // 相似度值
    private double similarity;
    public TopkItem(int rank ,int grade, double similarity){
        this.rank = rank;
        this.grade = grade;
        this.similarity = similarity;
    }


}
