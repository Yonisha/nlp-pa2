package grammar;

import java.util.*;

import utils.CountMap;

/**
 * 
 * @author rtsarfat
 *
 * CLASS: Grammar
 * 
 * Definition: formally <N,T,S,R> 
 * Role: holds two collection of grammatical and lexical grammar rules  
 * Responsibility: define a start symbol 
 * 
 */

public class Grammar {

	protected Set<String> m_setStartSymbols = new HashSet<String>();
	protected Set<String> m_setTerminalSymbols = new HashSet<String>();
	protected Set<String> m_setNonTerminalSymbols = new HashSet<String>();

	protected Set<Rule> m_setSyntacticRules = new HashSet<Rule>();
	protected Set<Rule> m_setLexicalRules = new HashSet<Rule>();
	protected CountMap<Rule> m_cmRuleCounts = new CountMap<Rule>();
	protected Map<String, Set<Rule>> m_lexLexicalEntries = new HashMap<String, Set<Rule>>();

	private boolean m_useSmoothing;
		
	public Grammar(boolean useSmoothing) {
		super();
		m_useSmoothing = useSmoothing;
	}
	
	public Map<String, Set<Rule>> getLexicalEntries() {
		return m_lexLexicalEntries;
	}

	public void setLexicalEntries(Map<String, Set<Rule>> m_lexLexicalEntries) {
		this.m_lexLexicalEntries = m_lexLexicalEntries;
	}

	public CountMap<Rule> getRuleCounts() {
		return m_cmRuleCounts;
	}

	private void addSmoothing() {

		if (!m_useSmoothing){
			HashSet<Rule> rulesForUnknowns = new HashSet<>();
			Rule rule = new Rule("NN", "UNK", true);
			rule.setMinusLogProb(0);
			rulesForUnknowns.add(rule);
			m_lexLexicalEntries.put("UNK", rulesForUnknowns);
			return;
		}

		Set<Rule> allUnknownRules = new HashSet<>();
		Set<String> allSeenWords = m_lexLexicalEntries.keySet();
		List<String> allSeenWordsThatAppearOnce = new ArrayList<>();
		for (String seenWord : allSeenWords){
			Set<Rule> rulesForCurrentKey = m_lexLexicalEntries.get(seenWord);
			if (rulesForCurrentKey.size() == 1){
				Rule newRuleForUnk = new Rule(rulesForCurrentKey.iterator().next().getLHS().getSymbols().get(0), "UNK", true);
				allUnknownRules.add(newRuleForUnk);
				allSeenWordsThatAppearOnce.add(seenWord);
				getRuleCounts().increment(newRuleForUnk);
			}
		}
//		allSeenWordsThatAppearOnce.stream().forEach(w -> m_lexLexicalEntries.remove(w));
		m_lexLexicalEntries.put("UNK", allUnknownRules);
	}

	private void calcRuleProbs() {
		// Grouping
		m_cmRuleCounts.forEach((rule, ruleCount) -> {
			Integer count = counts.get(rule.getLHS());
			if (count == null) {
				counts.put(rule.getLHS(), ruleCount);
			} else {
				counts.put(rule.getLHS(), count + ruleCount);
			}
		});

		m_cmRuleCounts.forEach((rule, ruleCount) -> {
			Integer totalLHSCount = counts.get(rule.getLHS());

			double minusLogProb = -Math.log(ruleCount / (double)totalLHSCount);
			rule.setMinusLogProb(minusLogProb);
		});
	}

	Dictionary<Event, Integer> counts = new Hashtable<Event, Integer>();

	public void addRule(Rule r)
	{	
		Event eLhs = r.getLHS();
		Event eRhs = r.getRHS();
				
		if (r.isLexical())
		{
			// update the sets T, N, R
			getLexicalRules().add(r);
			getNonTerminalSymbols().addAll(eLhs.getSymbols());
			getTerminalSymbols().addAll(eRhs.getSymbols());
			
			// update the dictionary
			if (!getLexicalEntries().containsKey(eRhs.toString()) )
				getLexicalEntries().put(eRhs.toString(), new HashSet<Rule>());
			getLexicalEntries().get(eRhs.toString()).add(r);
		}
		else 
		{
			// update the sets T, N, R
			getSyntacticRules().add(r);
			getNonTerminalSymbols().addAll(eLhs.getSymbols());
			getNonTerminalSymbols().addAll(eRhs.getSymbols());
		}
		
		// update the start symbol(s)
		if (r.isTop())
			getStartSymbols().add(eLhs.toString());
		
		// update the rule counts 
		getRuleCounts().increment(r);
	}
	

	public Set<String> getNonTerminalSymbols() {
		return m_setNonTerminalSymbols;
	}

	public Set<Rule> getSyntacticRules() {
		return m_setSyntacticRules;
	}

	public void setSyntacticRules(Set<Rule> syntacticRules) {
		m_setSyntacticRules = syntacticRules;
	}

	public Set<Rule> getLexicalRules() {
		return m_setLexicalRules;
	}

	public void setLexicalRules(Set<Rule> lexicalRules) {
		m_setLexicalRules = lexicalRules;
	}

	public Set<String> getStartSymbols() {
		return m_setStartSymbols;
	}

	public void setStartSymbols(Set<String> startSymbols) {
		m_setStartSymbols = startSymbols;
	}

	public Set<String> getTerminalSymbols() {
		return m_setTerminalSymbols;
	}

	public void setTerminalSymbols(Set<String> terminalSymbols) {
		m_setTerminalSymbols = terminalSymbols;
	}

	public int getNumberOfLexicalRuleTypes()
	{
		return getLexicalRules().size();
	}
	
	public int getNumberOfSyntacticRuleTypes()
	{
		return getSyntacticRules().size();
	}
	
	public int getNumberOfStartSymbols()
	{
		return getStartSymbols().size();
	}
	
	public int getNumberOfTerminalSymbols()
	{
		return getTerminalSymbols().size();
	}
	
	public void addStartSymbol(String string) {
		getStartSymbols().add(string);
	}

	public void removeStartSymbol(String string) {
		getStartSymbols().remove(string);
	}

	public void addAll(List<Rule> theRules) {
		for (int i = 0; i < theRules.size(); i++) {
			addRule(theRules.get(i));
		}

		addSmoothing();
		calcRuleProbs();
	}
}
