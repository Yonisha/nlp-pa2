package decode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ParseChartCell {
    private List<SubTree> subTreeOptions = new ArrayList<>();

    public void addSubTreeOption(SubTree subTreeOption) {
        addOrReplace(subTreeOption);
    }

    public List<SubTree> getSubTreeOptions() {
        return this.subTreeOptions;
    }

    // if a sub tree with same tag exists - take the one with the better accumulated prob
    private void addOrReplace(SubTree subTreeOption){
        Optional<SubTree> existingOne = this.getSubTreeOptions().stream().filter(t -> t.getTag().equalsIgnoreCase(subTreeOption.getTag())).findFirst();
        if (!existingOne.isPresent()){
            this.subTreeOptions.add(subTreeOption);
            return;
        }

        double accumulatedProbOfExistingOne = existingOne.get().getAccumulatedMinusLogProb();
        double accumulatedProbOfNewOne = subTreeOption.getAccumulatedMinusLogProb();

        if (accumulatedProbOfNewOne < accumulatedProbOfExistingOne){
            for (int i = 0; i < this.getSubTreeOptions().size(); i++) {
                if (this.getSubTreeOptions().get(i).getTag().equalsIgnoreCase(subTreeOption.getTag())){
                    this.getSubTreeOptions().remove(i);
                    break;
                }
            }

            this.subTreeOptions.add(subTreeOption);
        }
    }
}