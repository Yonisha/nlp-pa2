package train;

import grammar.Event;
import grammar.Grammar;
import grammar.Rule;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import tree.Node;
import tree.Tree;
import treebank.Treebank;



/**
 * 
 * @author Reut Tsarfaty
 * 
 * CLASS: Train
 * 
 * Definition: a learning component
 * Role: reads off a grammar from a treebank
 * Responsibility: keeps track of rule counts
 * 
 */

public class Train {


    /**
     * Implementation of a singleton pattern
     * Avoids redundant instances in memory 
     */
	public static Train m_singTrainer = null;
	    
	public static Train getInstance()
	{
		if (m_singTrainer == null)
		{
			m_singTrainer = new Train();
		}
		return m_singTrainer;
	}
	
	public static void main(String[] args) {

	}
	
	public Grammar train(Treebank myTreebank)
	{
		Grammar myGrammar = new Grammar();
		for (int i = 0; i < myTreebank.size(); i++) {
			Tree myTree = myTreebank.getAnalyses().get(i);
			List<Rule> theRules = getRules(myTree);
			myGrammar.addAll(theRules);
		}
		return myGrammar;
	}

	public List<Rule> getRules(Tree myTree)
	{
		myTree = binarizeTree(myTree);
		List<Rule> theRules = new ArrayList<Rule>();
		
		List<Node> myNodes = myTree.getNodes();
		for (int j = 0; j < myNodes.size(); j++) {
			Node myNode = myNodes.get(j);
			if (myNode.isInternal())
			{
				Event eLHS = new Event(myNode.getIdentifier());
				Iterator<Node> theDaughters = myNode.getDaughters().iterator();
				StringBuffer sb = new StringBuffer();
				while (theDaughters.hasNext()) {
					Node n = (Node) theDaughters.next();
					sb.append(n.getIdentifier());
					if (theDaughters.hasNext())
						sb.append(" ");
				}
				Event eRHS = new Event (sb.toString());
				Rule theRule = new Rule(eLHS, eRHS);
				if (myNode.isPreTerminal())
					theRule.setLexical(true);
				if (myNode.isRoot())
					theRule.setTop(true);
				theRules.add(theRule);
			}	
		}
		return theRules;
	}

	private Tree binarizeTree(Tree originalTree) {
		List<Node> newNodes = new ArrayList<>();
		List<Node> originalNodes = originalTree.getNodes();

		while (!originalNodes.isEmpty()) {
			int lastNode = originalNodes.size() - 1;

			Node current = originalNodes.get(lastNode);

			// TODO: what if only 1 child??
			if (current.getDaughters().size() <= 2) {
				newNodes.add(current);
				originalNodes.remove(lastNode);
			} else {
				List<Node> daughters = current.getDaughters();
				Node beforeLast = daughters.get(daughters.size() - 2);
				Node last = daughters.get(daughters.size() - 1);

				Node node = new Node(beforeLast.getIdentifier() + "-" + last.getIdentifier());
				beforeLast.setParent(node);
				last.setParent(node);
				node.setParent(current);

				daughters.remove(daughters.size() - 2);
				daughters.remove(daughters.size() - 1);
			}
		}

		Node rootNode = originalTree.getRoot();

		return new Tree(rootNode);
	}

}
