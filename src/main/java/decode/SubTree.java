package decode;

import java.util.List;

public class SubTree {
    private final String tag;
    private List<SubTree> daughters;
    private double prob;
    private double accumulatedMinusLogProb;

    public SubTree(String tag, double minusLogProb, List<SubTree> daughters) {
        this.tag = tag;
        this.prob = minusLogProb;
        this.daughters = daughters;
        this.accumulatedMinusLogProb = calcAccumulatedProb();
    }

    private double calcAccumulatedProb() {
        double accumulatedProb = this.prob;
        for (SubTree daughter : daughters) {
            accumulatedProb += daughter.accumulatedMinusLogProb;
        }

        return accumulatedProb;
    }

    public String getTag() {
        return this.tag;
    }

    public double getAccumulatedMinusLogProb() {
        return this.accumulatedMinusLogProb;
    }

    public List<SubTree> getDaughters() {
        return this.daughters;
    }

    public String toString() {
        return this.tag;
    }
}