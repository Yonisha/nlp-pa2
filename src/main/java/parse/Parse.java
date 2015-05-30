package parse;

import decode.DummyParser;
import grammar.Grammar;
import grammar.Rule;

import java.util.*;
import java.util.stream.Collectors;

import bracketimport.TreebankReader;

import decode.Decode;
import train.Train;

import tree.Node;
import tree.Tree;
import treebank.Treebank;

import utils.LineWriter;
import utils.ListExtensions;

public class Parse {
	
	public static void main(String[] args) {
		
		//**************************//
		//*      NLP@IDC PA2       *//
		//*   Statistical Parsing  *//
		//*     Point-of-Entry     *//
		//**************************//
		
		if (args.length < 3)
		{
			System.out.println("Usage: Parse <goldset> <trainset> <experiment-identifier-string>");
			return;
		}

		// TODO move
		int m_h = 0;
		boolean m_useSmoothing = false;
		System.out.println("Using horizontal markovization in level: " + m_h);
		System.out.println("Using smoothing: " + m_useSmoothing);

		// 1. read input
		Treebank myGoldTreebank = TreebankReader.getInstance().read(true, args[0]);
		Treebank myTrainTreebank = TreebankReader.getInstance().read(true, args[1]);
		
		// 2. transform trees
		Treebank binarizedTrainTreebank = new Treebank();
		myTrainTreebank.getAnalyses().stream().forEach(t -> binarizedTrainTreebank.add(binarizeTree(t, m_h)));
		
		// 3. train
		Grammar myGrammar = Train.getInstance(m_useSmoothing).train(myTrainTreebank);
		
		// 4. decode
		List<Tree> myParseTrees = new ArrayList<Tree>();
		Decode cykDecoder = Decode.getInstance(myGrammar);
		for (int i = 0; i < myGoldTreebank.size(); i++) {
			List<String> mySentence = myGoldTreebank.getAnalyses().get(i).getYield();
			if (mySentence.size() > 40)
				myParseTrees.add(DummyParser.Decode(mySentence));
			else
				myParseTrees.add(cykDecoder.decode(mySentence));
		}
		
		// 5. de-transform trees
		List<Tree> myParseTreesUnBinarized = myParseTrees.stream().map(pt -> deBinarizeTree(pt)).collect(Collectors.toList());

		// 6. write output
		writeOutput(args[2], myGrammar, myParseTreesUnBinarized);
	}

	private static Tree deBinarizeTree(Tree tree){
		List<Node> nodes = tree.getNodes();

		for (int i = 1; i < nodes.size(); i++) {
			Node currentNode = nodes.get(i);
			if (!currentNode.getIdentifier().contains("@"))
				continue;

			if (i == 1){
				currentNode.setIdentifier(currentNode.getIdentifier().split("@")[0]);
				continue;
			}

			// remove current node from its parent
			currentNode.getParent().getDaughters().remove(currentNode.getParent().getDaughters().size()-1);
			// add daughters of this node to the parent
			currentNode.getDaughters().forEach(d -> currentNode.getParent().addDaughter(d));
		}

		return tree;
	}

	private static Tree binarizeTree(Tree tree, int m_h) {
		List<Node> originalNodes = tree.getNodes();

		for (int i = 0; i < originalNodes.size(); i++) {
			Node current = originalNodes.get(i);

			int numberOfDaughtersInCurrentNode = current.getDaughters().size();
			// no need to binarize this node
			if (numberOfDaughtersInCurrentNode < 3)
				continue;

			String newInternalNodeId = current.getIdentifier() + "@/";
			binarizeNodeRecursive(current, numberOfDaughtersInCurrentNode, newInternalNodeId, m_h);

		}

		return tree;
	}

	private static void binarizeNodeRecursive(Node current, int numberOfDaughtersInCurrentNode, String newInternalNodeIdWithFullHistory, int m_h){

		List<Node> daughterOfNewInternalNode = new ArrayList<>(current.getDaughters().subList(1, numberOfDaughtersInCurrentNode));

		newInternalNodeIdWithFullHistory += current.getDaughters().get(0).getIdentifier() + "/";

		Node newInternalNode = new Node(getNewInternalNodeIdByGivenH(newInternalNodeIdWithFullHistory, m_h));
		for (int j = 0; j < daughterOfNewInternalNode.size(); j++) {
			newInternalNode.addDaughter(daughterOfNewInternalNode.get(j));
			current.removeDaughter(daughterOfNewInternalNode.get(j));
		}

		current.addDaughter(newInternalNode);

		int numberOfDaughtersInNewInternalNode = newInternalNode.getDaughters().size();
		if (numberOfDaughtersInNewInternalNode > 2)
			binarizeNodeRecursive(newInternalNode, numberOfDaughtersInNewInternalNode, newInternalNodeIdWithFullHistory, m_h);
	}

	private static String getNewInternalNodeIdByGivenH(String newInternalNodeIdWithFullHistory, int m_h){
		StringBuilder stringBuilder = new StringBuilder();
		List<String> histories = Arrays.asList(newInternalNodeIdWithFullHistory.split("/"));
		List<String> relevantHistories = new ArrayList<>();
		relevantHistories.add(histories.get(0));
		if (m_h == -1)
			relevantHistories.addAll(histories.subList(1, histories.size()));
		else
			relevantHistories.addAll(ListExtensions.takeRight(histories.subList(1, histories.size()), m_h));

		for (int i = 0; i < relevantHistories.size(); i++) {
			stringBuilder.append(relevantHistories.get(i));
			stringBuilder.append('/');
		}

		return stringBuilder.toString();
	}


	/**
	 * Writes output to files:
	 * = the trees are written into a .parsed file
	 * = the grammar rules are written into a .gram file
	 * = the lexicon entries are written into a .lex file
	 */
	private static void writeOutput(
			String sExperimentName, 
			Grammar myGrammar,
			List<Tree> myTrees) {
		
		writeParseTrees(sExperimentName, myTrees);
		writeGrammarRules(sExperimentName, myGrammar);
		writeLexicalEntries(sExperimentName, myGrammar);
	}

	/**
	 * Writes the parsed trees into a file.
	 */
	private static void writeParseTrees(String sExperimentName,
			List<Tree> myTrees) {
		LineWriter writer = new LineWriter(sExperimentName+".parsed");
		for (int i = 0; i < myTrees.size(); i++) {
			writer.writeLine(myTrees.get(i).toString());
		}
		writer.close();
	}
	
	/**
	 * Writes the grammar rules into a file.
	 */
	private static void writeGrammarRules(String sExperimentName,
			Grammar myGrammar) {
		LineWriter writer;
		writer = new LineWriter(sExperimentName+".gram");
		Set<Rule> myRules = myGrammar.getSyntacticRules();
		Iterator<Rule> myItrRules = myRules.iterator();
		while (myItrRules.hasNext()) {
			Rule r = (Rule) myItrRules.next();
			writer.writeLine(r.getMinusLogProb()+"\t"+r.getLHS()+"\t"+r.getRHS()); 
		}
		writer.close();
	}
	
	/**
	 * Writes the lexical entries into a file.
	 */
	private static void writeLexicalEntries(String sExperimentName, Grammar myGrammar) {
		LineWriter writer;
		Iterator<Rule> myItrRules;
		writer = new LineWriter(sExperimentName+".lex");
		Set<String> myEntries = myGrammar.getLexicalEntries().keySet();
		Iterator<String> myItrEntries = myEntries.iterator();
		while (myItrEntries.hasNext()) {
			String myLexEntry = myItrEntries.next();
			StringBuffer sb = new StringBuffer();
			sb.append(myLexEntry);
			sb.append("\t");
			Set<Rule> myLexRules =   myGrammar.getLexicalEntries().get(myLexEntry);
			myItrRules = myLexRules.iterator();
			while (myItrRules.hasNext()) {
				Rule r = (Rule) myItrRules.next();
				sb.append(r.getLHS().toString());
				sb.append(" ");
				sb.append(r.getMinusLogProb());
				sb.append(" ");
			}
			writer.writeLine(sb.toString());
		}

		writer.close();
	}
}
