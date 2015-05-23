package decode;

import java.util.ArrayList;
import java.util.List;

public class Cell {
    private List<PreTerminalWithProb> preTerminalsWithProbs = new ArrayList<>();

    public void addPreTerminal(PreTerminalWithProb preTerminalWithProb) {
        this.preTerminalsWithProbs.add(preTerminalWithProb);
    }

    public List<PreTerminalWithProb> getAllPreTerminals() {
        return this.preTerminalsWithProbs;
    }
}