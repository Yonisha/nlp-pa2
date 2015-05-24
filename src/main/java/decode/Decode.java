package decode;

import grammar.Event;
import grammar.Grammar;
import grammar.Rule;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import tree.Node;
import tree.Terminal;
import tree.Tree;

public class Decode {

	public static Set<Rule> m_setGrammarRules = null;
	public static Set<Rule> m_setUnaryGrammarRules = null;
	public static Map<String, Set<Rule>> m_mapLexicalRules = null;

	/**
	 * Implementation of a singleton pattern
	 * Avoids redundant instances in memory
	 */
	public static Decode m_singDecoder = null;

	public static Decode getInstance(Grammar g)
	{
		if (m_singDecoder == null)
		{
			m_singDecoder = new Decode();
			m_setGrammarRules = g.getSyntacticRules();
			m_mapLexicalRules = g.getLexicalEntries();
			m_setUnaryGrammarRules = m_setGrammarRules.stream().filter(r -> r.getRHS().getSymbols().size() == 1).collect(Collectors.toSet());
		}
		return m_singDecoder;
	}

	public Tree decode(List<String> input){

		for (int i = 0; i < input.size(); i++) {
			System.out.print(input.get(i) + " ");
		}

		System.out.println();
		// Done: Baseline Decoder
		//       Returns a flat tree with NN labels on all leaves

		Tree t = new Tree(new Node("TOP"));
//    Iterator<String> theInput = input.iterator();
//    while (theInput.hasNext()) {
//       String theWord = (String) theInput.next();
//       Node preTerminal = new Node("NN");
//       Terminal terminal = new Terminal(theWord);
//       preTerminal.addDaughter(terminal);
//       t.getRoot().addDaughter(preTerminal);
//    }

		// TODO: CYK decoder
		Cell[][] cells = buildParseChartMatrix(input);

//    for (int i = 0; i < cells.length; i++) {
//       for (int j = 0; j < cells.length; j++) {
//          System.out.print("--");
//          Cell cell = cells[i][j];
//
//          if (cell.getAllPreTerminals().isEmpty()) {
//             System.out.print("X");
//          }
//
//          for (PreTerminalWithProb preTerminalWithProb : cell.getAllPreTerminals()) {
//             System.out.print(preTerminalWithProb.getPreTerminal() + "|");
//          }
//
//          System.out.print("\t");
//       }
//
//       System.out.println();
//    }

//    System.out.println("-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
		return t;

	}

	private Cell[][] buildParseChartMatrix(List<String> input) {
		int numOfWords = input.size();
		Cell[][] preTerminalsWithProbs = new Cell[numOfWords][numOfWords];

		for (int i = 0; i < preTerminalsWithProbs.length; i++) {
			for (int j = 0; j < preTerminalsWithProbs.length; j++) {
				preTerminalsWithProbs[i][j] = new Cell();
			}
		}

		populateFromLexical(preTerminalsWithProbs, input);
		populateFromGrams(preTerminalsWithProbs);

		Optional<PreTerminalWithProb> bestPreTerminal = preTerminalsWithProbs[0][0].getAllPreTerminals().stream().collect(Collectors.minBy(Comparator.comparingDouble(c -> c.getAccumulatedProb())));
		if (bestPreTerminal.isPresent())
		{
			String s = bestPreTerminal.get().toStringSubtree();

		}else{
			System.out.println("Problem with sentence ");
			input.stream().forEach(w -> System.out.print(w + " "));
		}

		return preTerminalsWithProbs;
	}

	private void populateFromGrams(Cell[][] preTerminalsWithProbs) {
		int startLevel = preTerminalsWithProbs.length - 2;

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

					for (int l = 0; l < allPreTerminalsForFirstHead.size(); l++) {
						for (int m = 0; m < allPreTerminalsForSecondHead.size(); m++) {
							PreTerminalWithProb firstHeadPreTerminalWithProb = allPreTerminalsForFirstHead.get(l);
							PreTerminalWithProb secondHeadPreTerminalWithProb = allPreTerminalsForSecondHead.get(m);

							List<Rule> rulesForPreTerminals = findRulesForPreTerminals(firstHeadPreTerminalWithProb, secondHeadPreTerminalWithProb);

							List<PreTerminalWithProb> daughters = new ArrayList<>();
							daughters.add(firstHeadPreTerminalWithProb);
							daughters.add(secondHeadPreTerminalWithProb);

							for (Rule rule : rulesForPreTerminals) {
								PreTerminalWithProb preTerminalWithProb = new PreTerminalWithProb(rule.getLHS().getSymbols().get(0), rule.getMinusLogProb(), daughters);
								preTerminalsWithProbs[i][k].addPreTerminal(preTerminalWithProb);
							}
						}
					}
				}

				addAllUnaryPreTerminals(preTerminalsWithProbs, i, k);
			}


			// Print row
//		  for (int j = 0; j < preTerminalsWithProbs.length; j++) {
//			 List<PreTerminalWithProb> allPreTerminals = preTerminalsWithProbs[i][j].getAllPreTerminals();
//			 allPreTerminals.forEach(p -> System.out.print(p.getPreTerminal() + "\t"));
//		  }
		}

		System.out.print("");
	}

	private List<Rule> findRulesForPreTerminals(PreTerminalWithProb first, PreTerminalWithProb second) {
		List<Rule> matchingRules = new ArrayList<>();

		for (Rule rule : m_setGrammarRules) {
			List<String> symbols = rule.getRHS().getSymbols();

			// TODO: how to handle unary rules? Mor to fix.
			if (symbols.size() != 2) {
				continue;
			}

			if (symbols.get(0).equalsIgnoreCase(first.getPreTerminal()) && symbols.get(1).equalsIgnoreCase(second.getPreTerminal())) {
				matchingRules.add(rule);
			}
		}

		return matchingRules;
	}

	private void addAllUnaryPreTerminals(Cell[][] preTerminalsWithProbs, int i, int k) {
		Stack<PreTerminalWithProb> unaryPreTerminalsStack = new Stack<>();
		List<PreTerminalWithProb> allPreTerminalsInCell = preTerminalsWithProbs[i][k].getAllPreTerminals();

		// Pushing all new unaries to a stack
		for (PreTerminalWithProb preTerminal : allPreTerminalsInCell){
			List<PreTerminalWithProb> unaryRulesForSinglePreTerminal = getUnaryRulesForSinglePreTerminal(preTerminal);
			for (PreTerminalWithProb preTerminalWithProb : unaryRulesForSinglePreTerminal) {
				unaryPreTerminalsStack.push(preTerminalWithProb);
			}
		}

		// Iterating the stack to find all unaries
		while (!unaryPreTerminalsStack.isEmpty()) {
			PreTerminalWithProb unary = unaryPreTerminalsStack.pop();

			// check if exists in the current cell
//			boolean preTerminalWithSameRhsExists = preTerminalsWithProbs[i][k].isPreTerminalExists(unary);
//			if (preTerminalWithSameRhsExists) {
//				continue;
//			}

			// check if rhs and lhs are same
			boolean isUnaryRuleRecursive = isUnaryRuleRecursive(unary.getPreTerminal(), unary);
			if (isUnaryRuleRecursive) {
				continue;
			}

			preTerminalsWithProbs[i][k].addPreTerminal(unary);

			List<PreTerminalWithProb> unaryRulesForSinglePreTerminal = getUnaryRulesForSinglePreTerminal(unary);
			for (PreTerminalWithProb newPreTerminal : unaryRulesForSinglePreTerminal){
				preTerminalsWithProbs[i][k].addPreTerminal(newPreTerminal);
				unaryPreTerminalsStack.push(newPreTerminal);
			}
		}
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
				lexicalRules = new HashSet<>();
				lexicalRules.add(new Rule("NN", terminal, true));
			}

			final int terminalIndex = i;
			lexicalRules.forEach(r -> {
				String preTerminal = r.getLHS().getSymbols().get(0);
				double minusLogProb = r.getMinusLogProb();

				List<PreTerminalWithProb> daughters = new ArrayList<>();
				daughters.add(new PreTerminalWithProb(terminal, 0, new ArrayList<>()));
				PreTerminalWithProb preTerminalWithProb = new PreTerminalWithProb(preTerminal, minusLogProb, daughters);
				preTerminalsWithProbs[startLevel][terminalIndex].addPreTerminal(preTerminalWithProb);
			});

			addAllUnaryPreTerminals(preTerminalsWithProbs, startLevel, terminalIndex);
		}
	}

	private List<PreTerminalWithProb> getUnaryRulesForSinglePreTerminal(PreTerminalWithProb terminalWithProb){
		List<Rule> matchingUnaryGrammarRules =  m_setUnaryGrammarRules.stream().filter(r -> r.getRHS().getSymbols().get(0).equals(terminalWithProb.getPreTerminal())).collect(Collectors.toList());
		List<PreTerminalWithProb> newPreTerminals = new ArrayList<>();

		matchingUnaryGrammarRules.forEach(r -> {
			String preTerminal = r.getLHS().getSymbols().get(0);
			double minusLogProb = r.getMinusLogProb() + terminalWithProb.getProb();

			List<PreTerminalWithProb> daughters = new ArrayList<PreTerminalWithProb>();
			daughters.add(terminalWithProb);
			PreTerminalWithProb preTerminalWithProb = new PreTerminalWithProb(preTerminal, minusLogProb, daughters);
			newPreTerminals.add(preTerminalWithProb);
		});

		return newPreTerminals;
	}
}