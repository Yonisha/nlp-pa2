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

    public boolean isPreTerminalExists(PreTerminalWithProb newPreTerminalWithProb) {
        for (PreTerminalWithProb preTerminalWithProb : this.preTerminalsWithProbs) {
            if (isPreTerminalExistsInternal(preTerminalWithProb, preTerminalWithProb, newPreTerminalWithProb))
                return true;

            if (preTerminalWithProb.getPreTerminal().equalsIgnoreCase(newPreTerminalWithProb.getPreTerminal())
                    && preTerminalWithProb.getDaughters().size() == 1
                    && preTerminalWithProb.getDaughters().get(0).getPreTerminal().equalsIgnoreCase(newPreTerminalWithProb.getDaughters().get(0).getPreTerminal())){
                return true;
            }
        }

        return false;
    }

    private boolean isPreTerminalExistsInternal(PreTerminalWithProb existing, PreTerminalWithProb existingDaughter, PreTerminalWithProb newP) {
        if (existingDaughter.getDaughters().size() != 1)
            return false;

        if (existingDaughter.getDaughters().get(0).getPreTerminal().equalsIgnoreCase(newP.getPreTerminal())
                && existing.getPreTerminal().equalsIgnoreCase(newP.getDaughters().get(0).getPreTerminal())) {
            return true;
        }

        return isPreTerminalExistsInternal(existing, existingDaughter.getDaughters().get(0), newP);
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