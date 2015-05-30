package decode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
        Optional<PreTerminalWithProb> existingOne = this.getAllPreTerminals().stream().filter(t -> t.getPreTerminal().equalsIgnoreCase(preTerminalWithProb.getPreTerminal())).findFirst();
        if (!existingOne.isPresent()){
            this.preTerminalsWithProbs.add(preTerminalWithProb);
            return;
        }

        double accumulatedProbOfExistingOne = existingOne.get().getAccumulatedProb();
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