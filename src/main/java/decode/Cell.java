package decode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Cell {
    private List<PreTerminalWithProb> preTerminalsWithProbs = new ArrayList<>();

    public void addPreTerminal(PreTerminalWithProb preTerminalWithProb) {
        addOrReplace(preTerminalWithProb);
    }

    public List<PreTerminalWithProb> getAllPreTerminals() {
        return this.preTerminalsWithProbs;
    }

    private void addOrReplace(PreTerminalWithProb preTerminalWithProb){
        List<PreTerminalWithProb> preTerminalsWithSameName = this.getAllPreTerminals().stream().filter(t -> t.getPreTerminal().equalsIgnoreCase(preTerminalWithProb.getPreTerminal())).collect(Collectors.toList());
        if (preTerminalsWithSameName.isEmpty()){
            this.preTerminalsWithProbs.add(preTerminalWithProb);
            return;
        }

        double accumulatedProbOfExistingOne = preTerminalsWithSameName.get(0).getAccumulatedProb();
        double accumulatedProbOfNewOne = preTerminalWithProb.getAccumulatedProb();

        if (accumulatedProbOfNewOne < accumulatedProbOfExistingOne){
            for (int i = 0; i < this.getAllPreTerminals().size(); i++) {
                if (this.getAllPreTerminals().get(i).getPreTerminal().equalsIgnoreCase(preTerminalWithProb.getPreTerminal())){
                    this.getAllPreTerminals().remove(i);
                    break;
                }
            }

            this.preTerminalsWithProbs.add(preTerminalWithProb);
        }

        return;
    }
}