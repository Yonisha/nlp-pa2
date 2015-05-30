package decode;

import tree.Node;
import tree.Terminal;
import tree.Tree;

import java.util.Iterator;
import java.util.List;

public class DummyParser {

    public static Tree Decode(List<String> input){
        // Done: Baseline Decoder
        //       Returns a flat tree with NN labels on all leaves
        Tree parsedTree = new Tree(new Node("TOP"));
        Iterator<String> theInput = input.iterator();
        while (theInput.hasNext()) {
            String theWord = (String) theInput.next();
            Node preTerminal = new Node("NN");
            Terminal terminal = new Terminal(theWord);
            preTerminal.addDaughter(terminal);
            parsedTree.getRoot().addDaughter(preTerminal);
        }

        return parsedTree;
    }
}
