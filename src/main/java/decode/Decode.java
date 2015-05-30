package decode;

import grammar.Event;
import grammar.Grammar;
import grammar.Rule;

import java.sql.Time;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import tree.Node;
import tree.Terminal;
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

		// TODO: CYK decoder
		Tree parsedTree = buildParseChartMatrix(input);
		return parsedTree;
	}

	private Node buildNodeFromPreTerminal(PreTerminalWithProb preTerminal){

		Node node = new Node(preTerminal.getPreTerminal());

		if (preTerminal.getDaughters().isEmpty())
			return node;

		Node firstDaughter = buildNodeFromPreTerminal(preTerminal.getDaughters().get(0));
		node.addDaughter(firstDaughter);

		if (preTerminal.getDaughters().size() == 2){
			Node secondDaughter = buildNodeFromPreTerminal(preTerminal.getDaughters().get(1));
			node.addDaughter(secondDaughter);
		}

		return node;
	}

	private Tree buildParseChartMatrix(List<String> input) {

		int numOfWords = input.size();
		Cell[][] preTerminalsWithProbs = new Cell[numOfWords][numOfWords];

		for (int i = 0; i < preTerminalsWithProbs.length; i++) {
			for (int j = 0; j < preTerminalsWithProbs.length; j++) {
				preTerminalsWithProbs[i][j] = new Cell();
			}
		}

		long startLex = System.currentTimeMillis();
		populateFromLexical(preTerminalsWithProbs, input);
		long endLex = System.currentTimeMillis();
		System.out.println("Time for lexical: " +  (endLex-startLex) );

		long start = System.currentTimeMillis();
		populateFromGrams(preTerminalsWithProbs);
		long end = System.currentTimeMillis();
		System.out.println("Time for grams: " + (end-start)  );

		long startBest = System.currentTimeMillis();
		Optional<PreTerminalWithProb> bestPreTerminal = preTerminalsWithProbs[0][0].getAllPreTerminals().stream().filter(t -> !t.getPreTerminal().contains("@")).collect(Collectors.minBy(Comparator.comparingDouble(c -> c.getAccumulatedProb())));
		long endBest = System.currentTimeMillis();
		System.out.println("Time for finding best: " + (endBest-startBest ));
		Tree tree;
		if (bestPreTerminal.isPresent())
		{
			Node rootNode = buildNodeFromPreTerminal(bestPreTerminal.get());
			Node top = new Node("TOP");
			top.addDaughter(rootNode);
			tree = new Tree(top);
		}else{
			tree = DummyParser.Decode(input);
		}

		return tree;
	}

	private void populateFromGrams(Cell[][] preTerminalsWithProbs) {
		int startLevel = preTerminalsWithProbs.length - 2;

		long unaryTime = 0;
		long firstSecondLoopTime = 0;

		for (int i = startLevel; i >= 0; i--) { // the level we currently work on
			for (int k = 0; k < i + 1; k++) { // the current tree in the level
				int secondTreeHeadHeight = i;
				int secondTreeHeadDepth = k;

				for (int j = preTerminalsWithProbs.length - 1; j > i; j--) { // sub-trees
					int firstTreeHeadHeight = j;
					int firstTreeHeadDepth = k;

					secondTreeHeadHeight++;
					secondTreeHeadDepth++;

					List<PreTerminalWithProb> allPreTerminalsForFirstHead = preTerminalsWithProbs[firstTreeHeadHeight][firstTreeHeadDepth].getAllPreTerminals();
					List<PreTerminalWithProb> allPreTerminalsForSecondHead = preTerminalsWithProbs[secondTreeHeadHeight][secondTreeHeadDepth].getAllPreTerminals();

					if (allPreTerminalsForFirstHead.isEmpty() || allPreTerminalsForSecondHead.isEmpty())
						continue;

					long start = System.currentTimeMillis();
					for (int l = 0; l < allPreTerminalsForFirstHead.size(); l++) {
						for (int m = 0; m < allPreTerminalsForSecondHead.size(); m++) {
							PreTerminalWithProb firstHeadPreTerminalWithProb = allPreTerminalsForFirstHead.get(l);
							PreTerminalWithProb secondHeadPreTerminalWithProb = allPreTerminalsForSecondHead.get(m);

							Set<Rule> rulesForPreTerminals = findRulesForPreTerminals(firstHeadPreTerminalWithProb, secondHeadPreTerminalWithProb);

							List<PreTerminalWithProb> daughters = new ArrayList<>();
							daughters.add(firstHeadPreTerminalWithProb);
							daughters.add(secondHeadPreTerminalWithProb);

							for (Rule rule : rulesForPreTerminals) {
								PreTerminalWithProb preTerminalWithProb = new PreTerminalWithProb(rule.getLHS().getSymbols().get(0), rule.getMinusLogProb(), daughters);
								preTerminalsWithProbs[i][k].addPreTerminal(preTerminalWithProb);
							}
						}
					}
					long end = System.currentTimeMillis();
					firstSecondLoopTime += end-start;
				}

				long start = System.currentTimeMillis();
				addAllUnaryPreTerminals(preTerminalsWithProbs, i, k);
				long end = System.currentTimeMillis();
				unaryTime += end-start;

			}
		}

		System.out.println("Time for add unaries per sentence: " + unaryTime );
		System.out.println("Time for first/second loop: " +  firstSecondLoopTime );
	}

	private Set<Rule> findRulesForPreTerminals(PreTerminalWithProb first, PreTerminalWithProb second) {
		Set<Rule> matchingRules;
		matchingRules = m_syntacticBinaryRulesByRhsAsKey.get(first.getPreTerminal() + " " + second.getPreTerminal());
		if (matchingRules != null)
			return matchingRules;

		return new HashSet<>();
	}

	private void addAllUnaryPreTerminals(Cell[][] preTerminalsWithProbs, int i, int k) {
		List<PreTerminalWithProb> unaryPreTerminalsStack = new ArrayList<>();
		List<PreTerminalWithProb> allPreTerminalsInCell = preTerminalsWithProbs[i][k].getAllPreTerminals();

		// Pushing all new unaries to a stack
		for (PreTerminalWithProb preTerminal : allPreTerminalsInCell){
			Set<PreTerminalWithProb> unaryRulesForSinglePreTerminal = getUnaryRulesForSinglePreTerminal(preTerminal);
			for (PreTerminalWithProb preTerminalWithProb : unaryRulesForSinglePreTerminal) {
				boolean isUnaryRuleRecursive = isUnaryRuleRecursive(preTerminalWithProb.getPreTerminal(), preTerminalWithProb);
				if (!isUnaryRuleRecursive)
					addOrReplace(unaryPreTerminalsStack, preTerminalWithProb);
			}
		}

		// Iterating the stack to find all unaries
		while (!unaryPreTerminalsStack.isEmpty()) {
			PreTerminalWithProb unary = unaryPreTerminalsStack.get(0);
			unaryPreTerminalsStack.remove(0);
			preTerminalsWithProbs[i][k].addPreTerminal(unary);

			Set<PreTerminalWithProb> unaryRulesForSinglePreTerminal = getUnaryRulesForSinglePreTerminal(unary);
			for (PreTerminalWithProb newPreTerminal : unaryRulesForSinglePreTerminal){
				preTerminalsWithProbs[i][k].addPreTerminal(newPreTerminal);
				boolean isUnaryRuleRecursive = isUnaryRuleRecursive(newPreTerminal.getPreTerminal(), newPreTerminal);
				if (!isUnaryRuleRecursive)
					addOrReplace(unaryPreTerminalsStack, newPreTerminal);
			}
		}
	}

	private void addOrReplace(List<PreTerminalWithProb> all, PreTerminalWithProb preTerminalWithProb){
		Optional<PreTerminalWithProb> existingOne = all.stream().filter(t -> t.getPreTerminal().equalsIgnoreCase(preTerminalWithProb.getPreTerminal())).findFirst();
		if (!existingOne.isPresent()){
			all.add(preTerminalWithProb);
			return;
		}

		double accumulatedProbOfExistingOne = existingOne.get().getAccumulatedProb();
		double accumulatedProbOfNewOne = preTerminalWithProb.getAccumulatedProb();

		if (accumulatedProbOfNewOne < accumulatedProbOfExistingOne){
			for (int i = 0; i < all.size(); i++) {
				if (all.get(i).getPreTerminal().equalsIgnoreCase(preTerminalWithProb.getPreTerminal())){
					all.remove(i);
					break;
				}
			}

			all.add(preTerminalWithProb);
		}

		return;
	}

	private boolean isUnaryRuleRecursive(String lhs, PreTerminalWithProb unary){
		if (unary.getDaughters().size() != 1)
			return false;

		if (lhs.equalsIgnoreCase(unary.getDaughters().get(0).getPreTerminal()))
			return true;

		return isUnaryRuleRecursive(lhs, unary.getDaughters().get(0));
	}

	private void populateFromLexical(Cell[][] preTerminalsWithProbs, List<String> input) {
		int numOfWords = input.size();
		int startLevel = numOfWords - 1;
		for (int i = 0; i < numOfWords; i++) {
			String terminal = input.get(i);
			Set<Rule> lexicalRules = m_mapLexicalRules.get(terminal);

			if (lexicalRules == null) {
//				lexicalRules = new HashSet<>();
//				lexicalRules.add(new Rule("NN", terminal, true));
				lexicalRules = m_mapLexicalRules.get("UNK");
			}

			List<PreTerminalWithProb> daughters = new ArrayList<>();
			daughters.add(new PreTerminalWithProb(terminal, 0, new ArrayList<>()));
			final int terminalIndex = i;
			lexicalRules.forEach(r -> {
				String preTerminal = r.getLHS().getSymbols().get(0);
				PreTerminalWithProb preTerminalWithProb = new PreTerminalWithProb(preTerminal, r.getMinusLogProb(), daughters);
				preTerminalsWithProbs[startLevel][terminalIndex].addPreTerminal(preTerminalWithProb);
			});

			addAllUnaryPreTerminals(preTerminalsWithProbs, startLevel, terminalIndex);
		}
	}

	private Set<PreTerminalWithProb> getUnaryRulesForSinglePreTerminal(PreTerminalWithProb terminalWithProb){
		Set<Rule> matchingUnaryGrammarRules =  m_syntacticUnaryRulesByRhsAsKey.get(terminalWithProb.getPreTerminal());
		if (matchingUnaryGrammarRules == null)
			return new HashSet<>();

		Set<PreTerminalWithProb> newPreTerminals = new HashSet<>();

		List<PreTerminalWithProb> daughters = new ArrayList<>();
		daughters.add(terminalWithProb);

		matchingUnaryGrammarRules.forEach(r -> {
			String preTerminal = r.getLHS().getSymbols().get(0);
			PreTerminalWithProb preTerminalWithProb = new PreTerminalWithProb(preTerminal, r.getMinusLogProb(), daughters);
			newPreTerminals.add(preTerminalWithProb);
		});

		return newPreTerminals;
	}
}