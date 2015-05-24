package decode;

import java.util.Iterator;
import java.util.List;

public class PreTerminalWithProb {
    private final String preTerminal;
    private List<PreTerminalWithProb> daughters;
    private double prob;
    private double accumulatedProb;

    public PreTerminalWithProb(String preTerminal, double prob, List<PreTerminalWithProb> daughters) {
        this.preTerminal = preTerminal;
        this.prob = prob;
        this.daughters = daughters;
        this.accumulatedProb = calcAccumulatedProb();
    }

    private double calcAccumulatedProb() {
        double accumulatedProb = this.prob;
        for (PreTerminalWithProb daughter : daughters) {
            accumulatedProb += daughter.accumulatedProb;
        }

        return accumulatedProb;
    }

    public String getPreTerminal() {
        return this.preTerminal;
    }

    public double getProb() {
        return this.prob;
    }

    public double getAccumulatedProb() {
        return this.accumulatedProb;
    }

    public List<PreTerminalWithProb> getDaughters() {
        return this.daughters;
    }

    public String toString() {
        return this.preTerminal;
    }

    public String toStringSubtree() {
        StringBuffer sb = new StringBuffer();

        if (!daughters.isEmpty())
        {
            sb.append("(");
            sb.append(this.toString());
            sb.append(" ");

            boolean bFirst = true;
            for (Iterator<PreTerminalWithProb> iterator =
                 getDaughters().iterator();
                 iterator.hasNext();)
            {
                PreTerminalWithProb n = (PreTerminalWithProb) iterator.next();
                if (n.daughters.isEmpty())
                {
                    if (!bFirst)
                    {
                        sb.append(" ");
                    }
                    else
                    {
                        bFirst = false;
                    }
                    sb.append(n.toString());
                }
                else
                {
                    if (!bFirst)
                    {
                        sb.append(" ");
                    }
                    else
                    {
                        bFirst = false;
                    }
                    sb.append(n.toStringSubtree());
                }
            }
            sb.append(")");
        }
        else
        {
            sb.append(this.toString());
        }
        return sb.toString();
    }
}