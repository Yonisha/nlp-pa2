package train;

import grammar.Event;
import grammar.Grammar;
import grammar.Rule;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import tree.Node;
import tree.Tree;
import treebank.Treebank;
import utils.ListExtensions;


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
	public static int m_h = 2;
	    
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

			int numberOfDaughtersInCurrentNode = current.getDaughters().size();
			// no need to binarize this node
			if (numberOfDaughtersInCurrentNode < 3)
				continue;

			String newInternalNodeId = current.getIdentifier() + "@/";
			binarizeNodeRecursive(current, numberOfDaughtersInCurrentNode, newInternalNodeId);

		}

		return treeToBinarize;
	}

	private void binarizeNodeRecursive(Node current, int numberOfDaughtersInCurrentNode, String newInternalNodeIdWithFullHistory){

		List<Node> daughterOfNewInternalNode = new ArrayList<>(current.getDaughters().subList(1, numberOfDaughtersInCurrentNode));

		newInternalNodeIdWithFullHistory += current.getDaughters().get(0).getIdentifier() + "/";

		Node newInternalNode = new Node(getNewInternalNodeIdByGivenH(newInternalNodeIdWithFullHistory));
		for (int j = 0; j < daughterOfNewInternalNode.size(); j++) {
			newInternalNode.addDaughter(daughterOfNewInternalNode.get(j));
			current.removeDaughter(daughterOfNewInternalNode.get(j));
		}

		current.addDaughter(newInternalNode);

		int numberOfDaughtersInNewInternalNode = newInternalNode.getDaughters().size();
		if (numberOfDaughtersInNewInternalNode > 2)
			binarizeNodeRecursive(newInternalNode, numberOfDaughtersInNewInternalNode, newInternalNodeIdWithFullHistory);
	}

	private String getNewInternalNodeIdByGivenH(String newInternalNodeIdWithFullHistory){
		StringBuilder stringBuilder = new StringBuilder();
		List<String> histories = Arrays.asList(newInternalNodeIdWithFullHistory.split("/"));
		List<String> relevantHistories = new ArrayList<>();
		if (m_h > -1)
			relevantHistories.add(histories.get(0));

		relevantHistories.addAll(ListExtensions.takeRight(histories.subList(1, histories.size()), m_h));
		for (int i = 0; i < relevantHistories.size(); i++) {
			stringBuilder.append(relevantHistories.get(i));
			stringBuilder.append('/');
		}

		return stringBuilder.toString();
	}
}
