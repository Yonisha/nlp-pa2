package train;

import grammar.Event;
import grammar.Grammar;
import grammar.Rule;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
	public static int m_h = 1;
	    
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
					Node n = theDaughters.next();
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

	private Tree binarizeTree(Tree treeToBinarize) {
		List<Node> originalNodes = treeToBinarize.getNodes();

		for (int i = 0; i < originalNodes.size(); i++) {
			Node current = originalNodes.get(i);

			for (int j = 0; j <= current.getDaughters().size() - 2; j++) {
				List<Node> daughters = current.getDaughters();
				Node beforeLast = daughters.get(daughters.size() - 2);
				Node last = daughters.get(daughters.size() - 1);

				// Building new internal node
				Node newNode = new Node(beforeLast.getIdentifier() + "-" + last.getIdentifier());
				newNode.setParent(current.getParent());
				newNode.addDaughter(beforeLast);
				newNode.addDaughter(last);

				// Adding the new node to current and removing redundant daughters
				current.addDaughter(newNode);
				current.removeDaughter(last);
				current.removeDaughter(beforeLast);

				if (m_h > -1){
					newNode.setRoot(current.getRoot());
				}

				if (m_h > 0){
					keepTrackOfSisters(newNode, current, m_h);
				}
			}
		}

		return treeToBinarize;
	}

	private void keepTrackOfSisters(Node newInternalNode, Node parentNode, int wantedNumberOfSisters) {

		int numberOfDaughtersWithoutLast = parentNode.getDaughters().size() - 1;
		int numberOfSistersToSkip = numberOfDaughtersWithoutLast - wantedNumberOfSisters - 1;

		List<Node> sistersToKeep;
		if (numberOfSistersToSkip >= numberOfDaughtersWithoutLast || numberOfSistersToSkip < 0) {
			sistersToKeep = parentNode.getDaughters().subList(0, numberOfDaughtersWithoutLast);
		} else {
			sistersToKeep = parentNode.getDaughters().subList(numberOfSistersToSkip, numberOfDaughtersWithoutLast);
		}

		newInternalNode.setSisters(sistersToKeep);
	}
}
