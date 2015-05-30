package decode;

import grammar.Grammar;
import grammar.Rule;

import java.util.*;
import java.util.stream.Collectors;

import tree.Node;
import tree.Tree;

public class Decode {

	public static Set<Rule> m_setGrammarRules = null;
	public static Map<String, Set<Rule>> m_syntacticUnaryRulesByRhsAsKey = null;
	public static Map<String, Set<Rule>> m_mapLexicalRules = null;
	private static Map<String, Set<Rule>> m_syntacticBinaryRulesByRhsAsKey = null;

	/**
	 * Implementation of a singleton pattern
	 * Avoids redundant instances in memory
	 */
	public static Decode m_singDecoder = null;

	public static Decode getInstance(Grammar g)
	{
		if (m_singDecoder == null)
		{
			System.out.println("Started creating Decoder");
			m_singDecoder = new Decode();
			m_setGrammarRules = g.getSyntacticRules();
			m_mapLexicalRules = g.getLexicalEntries();
			m_syntacticUnaryRulesByRhsAsKey = createMapForSyntacticRulesWithGivenLengthByRhs(1);
			m_syntacticBinaryRulesByRhsAsKey = createMapForSyntacticRulesWithGivenLengthByRhs(2);
			System.out.println("Finished creating Decoder");

		}
		return m_singDecoder;
	}

	private static Map<String, Set<Rule>> createMapForSyntacticRulesWithGivenLengthByRhs(int ruleRhsLength) {
		Map<String, Set<Rule>> map = new HashMap<>();
		for(Rule rule : m_setGrammarRules){
			if (rule.getRHS().getSymbols().size() != ruleRhsLength)
				continue;

			String rhs = rule.getRHS().toString();
			if (map.containsKey(rhs))
				map.get(rhs).add(rule);
			else{
				Set<Rule> setToAdd = new HashSet<>();
				setToAdd.add(rule);
				map.put(rhs, setToAdd);
			}
		}

		return map;
	}

	public Tree decode(List<String> input){
		for (int i = 0; i < input.size(); i++) {
			System.out.print(input.get(i) + " ");
		}

		System.out.println();

		// CYK decoder
		Tree parsedTree = cykDecode(input);
		return parsedTree;
	}

	private Tree cykDecode(List<String> input) {

		int numOfWords = input.size();
		ParseChartCell[][] parseChart = new ParseChartCell[numOfWords][numOfWords];

		for (int i = 0; i < parseChart.length; i++) {
			for (int j = 0; j < parseChart.length; j++) {
				parseChart[i][j] = new ParseChartCell();
			}
		}

		populateFromLexical(parseChart, input);
		populateFromGrams(parseChart);
		Optional<SubTree> bestTreeAccordingToObjectiveFunc = parseChart[0][0].getSubTreeOptions().stream().filter(t -> !t.getTag().contains("@")).collect(Collectors.minBy(Comparator.comparingDouble(c -> c.getAccumulatedMinusLogProb())));

		Tree tree;
		if (bestTreeAccordingToObjectiveFunc.isPresent())
		{
			Node rootNode = buildNodeFromBestSubTreeOption(bestTreeAccordingToObjectiveFunc.get());
			Node top = new Node("TOP");
			top.addDaughter(rootNode);
			tree = new Tree(top);
		}else{
			tree = DummyParser.Decode(input);
		}
		return tree;
	}

	private Node buildNodeFromBestSubTreeOption(SubTree bestSubTree){

		Node node = new Node(bestSubTree.getTag());

		if (bestSubTree.getDaughters().isEmpty())
			return node;

		Node firstDaughter = buildNodeFromBestSubTreeOption(bestSubTree.getDaughters().get(0));
		node.addDaughter(firstDaughter);

		if (bestSubTree.getDaughters().size() == 2){
			Node secondDaughter = buildNodeFromBestSubTreeOption(bestSubTree.getDaughters().get(1));
			node.addDaughter(secondDaughter);
		}

		return node;
	}

	private void populateFromGrams(ParseChartCell[][] parseChart) {
		int startLevel = parseChart.length - 2;

		for (int i = startLevel; i >= 0; i--) { // the level we currently work on
			for (int k = 0; k < i + 1; k++) { // the current tree in the level
				int secondTreeHeadHeight = i;
				int secondTreeHeadDepth = k;

				for (int j = parseChart.length - 1; j > i; j--) { // sub-trees
					int firstTreeHeadHeight = j;
					int firstTreeHeadDepth = k;

					secondTreeHeadHeight++;
					secondTreeHeadDepth++;

					List<SubTree> subTreeOptionsForFirst = parseChart[firstTreeHeadHeight][firstTreeHeadDepth].getSubTreeOptions();
					List<SubTree> subTreeOptionsForSecond = parseChart[secondTreeHeadHeight][secondTreeHeadDepth].getSubTreeOptions();

					if (subTreeOptionsForFirst.isEmpty() || subTreeOptionsForSecond.isEmpty())
						continue;

					for (int l = 0; l < subTreeOptionsForFirst.size(); l++) {
						for (int m = 0; m < subTreeOptionsForSecond.size(); m++) {
							SubTree subTreeOptionForFirst = subTreeOptionsForFirst.get(l);
							SubTree subTreeOptionForSecond = subTreeOptionsForSecond.get(m);

							Set<Rule> rulesForTwoSubTreeOptions = findRulesForTwoSubTrees(subTreeOptionForFirst, subTreeOptionForSecond);

							List<SubTree> daughters = new ArrayList<>();
							daughters.add(subTreeOptionForFirst);
							daughters.add(subTreeOptionForSecond);

							for (Rule rule : rulesForTwoSubTreeOptions) {
								SubTree subTree = new SubTree(rule.getLHS().getSymbols().get(0), rule.getMinusLogProb(), daughters);
								parseChart[i][k].addSubTreeOption(subTree);
							}
						}
					}
				}

				addAllUnarySubTrees(parseChart, i, k);
			}
		}
	}

	private Set<Rule> findRulesForTwoSubTrees(SubTree first, SubTree second) {
		Set<Rule> matchingRules;
		matchingRules = m_syntacticBinaryRulesByRhsAsKey.get(first.getTag() + " " + second.getTag());
		if (matchingRules != null)
			return matchingRules;

		return new HashSet<>();
	}

	private void addAllUnarySubTrees(ParseChartCell[][] parseChart, int i, int k) {
		List<SubTree> unarySubTreesStack = new ArrayList<>();
		List<SubTree> allSubTreesInCell = parseChart[i][k].getSubTreeOptions();

		// Pushing all new unaries to a stack
		for (SubTree subTree : allSubTreesInCell){
			Set<SubTree> newUnarySubTrees = getUnaryRulesForSingleSubTreeOption(subTree);
			for (SubTree newUnarySubTree : newUnarySubTrees) {
				boolean isUnaryRuleRecursive = isUnaryRuleRecursive(newUnarySubTree.getTag(), newUnarySubTree);
				if (!isUnaryRuleRecursive)
					addOrReplace(unarySubTreesStack, newUnarySubTree);
			}
		}

		// Iterating the stack to find all unaries
		while (!unarySubTreesStack.isEmpty()) {
			SubTree unary = unarySubTreesStack.get(0);
			unarySubTreesStack.remove(0);
			parseChart[i][k].addSubTreeOption(unary);

			Set<SubTree> unaryRulesForSingleSubTree = getUnaryRulesForSingleSubTreeOption(unary);
			for (SubTree subTree : unaryRulesForSingleSubTree){
				parseChart[i][k].addSubTreeOption(subTree);
				boolean isUnaryRuleRecursive = isUnaryRuleRecursive(subTree.getTag(), subTree);
				if (!isUnaryRuleRecursive)
					addOrReplace(unarySubTreesStack, subTree);
			}
		}
	}

	// if a sub tree with same tag exists - take the one with the better accumulated prob
	private void addOrReplace(List<SubTree> all, SubTree newSubTree){
		Optional<SubTree> existingOne = all.stream().filter(t -> t.getTag().equalsIgnoreCase(newSubTree.getTag())).findFirst();
		if (!existingOne.isPresent()){
			all.add(newSubTree);
			return;
		}

		double accumulatedProbOfExistingOne = existingOne.get().getAccumulatedMinusLogProb();
		double accumulatedProbOfNewOne = newSubTree.getAccumulatedMinusLogProb();

		if (accumulatedProbOfNewOne < accumulatedProbOfExistingOne){
			for (int i = 0; i < all.size(); i++) {
				if (all.get(i).getTag().equalsIgnoreCase(newSubTree.getTag())){
					all.remove(i);
					break;
				}
			}

			all.add(newSubTree);
		}

		return;
	}

	private boolean isUnaryRuleRecursive(String lhs, SubTree unary){
		if (unary.getDaughters().size() != 1)
			return false;

		if (lhs.equalsIgnoreCase(unary.getDaughters().get(0).getTag()))
			return true;

		return isUnaryRuleRecursive(lhs, unary.getDaughters().get(0));
	}

	private void populateFromLexical(ParseChartCell[][] parseChart, List<String> input) {
		int numOfWords = input.size();
		int startLevel = numOfWords - 1;
		for (int i = 0; i < numOfWords; i++) {
			String word = input.get(i);
			Set<Rule> lexicalRules = m_mapLexicalRules.get(word);

			if (lexicalRules == null) {
				lexicalRules = m_mapLexicalRules.get("UNK");
			}

			List<SubTree> daughters = new ArrayList<>();
			daughters.add(new SubTree(word, 0, new ArrayList<>()));
			final int terminalIndex = i;
			lexicalRules.forEach(r -> {
				String tag = r.getLHS().getSymbols().get(0);
				SubTree subTreeOption = new SubTree(tag, r.getMinusLogProb(), daughters);
				parseChart[startLevel][terminalIndex].addSubTreeOption(subTreeOption);
			});

			addAllUnarySubTrees(parseChart, startLevel, terminalIndex);
		}
	}

	private Set<SubTree> getUnaryRulesForSingleSubTreeOption(SubTree subTree){
		Set<Rule> matchingUnaryGrammarRules =  m_syntacticUnaryRulesByRhsAsKey.get(subTree.getTag());
		if (matchingUnaryGrammarRules == null)
			return new HashSet<>();

		Set<SubTree> newSubTrees = new HashSet<>();

		List<SubTree> daughters = new ArrayList<>();
		daughters.add(subTree);

		matchingUnaryGrammarRules.forEach(r -> {
			String tag = r.getLHS().getSymbols().get(0);
			SubTree newSubTreeOption = new SubTree(tag, r.getMinusLogProb(), daughters);
			newSubTrees.add(newSubTreeOption);
		});

		return newSubTrees;
	}
}