/*
 * Copyright (C) 2016-2023 Mirco Colletta
 *
 * This file is part of MirComp.
 *
 * MirComp is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MirComp is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MirComp.  If not, see <http://www.gnu.org/licenses/>.
*/

/**
 * @author Mirco Colletta
 */

package io.github.mcolletta.mirgene

import java.util.regex.Pattern
import java.util.regex.Matcher

import com.xenoage.utils.pdlib.PMap
import com.xenoage.utils.pdlib.PList


class MirGram  {

	Grammar grammar

	static String EMPTY_PATTERN = /empty/
	static Pattern START_RULE_PATTERN = ~/(<[-_\^,\*,°,.a-zA-Z0-9]+(\([\-\+\*\/\[\] ,.a-zA-Z0-9]+\))*>)[\s\t\r\n]*::=/
	static Pattern NON_TERMINAL_PATTERN_LHS = ~/(<[-_\^,\*,°,.a-zA-Z0-9]+(\([\[\]\. a-zA-Z0-9]+\))*>)/
	static Pattern NON_TERMINAL_PATTERN_RHS = ~/(<[-_\^,\*,°,.a-zA-Z0-9]+(\([\-\+\*\.\/\[\] ,.a-zA-Z0-9]+\))*>)/
	static Pattern FUNC_PATTERN = ~/<([-_\^,\*,°,.a-zA-Z0-9]+)\(([\-\+\*\.\/ ,.a-zA-Z0-9]+)[\s]*\[{0,1}[\s]*(gt|gte|lt|lte){0,1}[\s]*([\.0-9]*)[\s]*\]{0,1}\)>/
	static Pattern WEIGHT_PATTERN = ~/\![0-9]+/
	static String GRAMMAR_SEPARATOR = "ç"
	static String RULE_SEPARATOR = "::="
	static String PRODUCTION_SEPARATOR = "\\|"
	static String CODE_PATTERN = /(<[\%][^%]+?[\%]>)/
	
	String startSymbol
	int maxWraps
	boolean useWeights

	MirGram(String sourceText, String startSymbol="program", int maxWraps=2, boolean useWeights=true) {
		grammar = parse(sourceText)
		this.startSymbol = startSymbol
		this.maxWraps = maxWraps
		this.useWeights = useWeights
	}

	static Grammar parse(String bnf, boolean actions=true) {
		Grammar g = new Grammar()
		
		if (actions)
			bnf = bnf.replaceAll(CODE_PATTERN, { String full, String action -> full.replace('::=', '<--') } )
		bnf = bnf.trim().replaceAll(START_RULE_PATTERN, { List<String> it ->  GRAMMAR_SEPARATOR + it[0] } )[1..-1]
		if (actions)
			bnf = bnf.replaceAll(CODE_PATTERN, {String full, String action -> full.replace('<--', '::=') } )

		for(String rule: bnf.split(GRAMMAR_SEPARATOR)) {
			if (rule.find(RULE_SEPARATOR)) {
				String[] parts = rule.split(RULE_SEPARATOR,2)
				String lhs_str = parts[0].trim()
				String rhs_str = parts[1]
				if (!lhs_str.find(NON_TERMINAL_PATTERN_LHS))
					throw new Exception("$lhs_str is not a Non Terminal")
				
				NonTerminal lhs = createNonTerminal(lhs_str)
				def PATTERN = ~/${NON_TERMINAL_PATTERN_RHS}|${WEIGHT_PATTERN}|${CODE_PATTERN}|${EMPTY_PATTERN}|[^<>\%\!]*/
				
				PList<Production> rhs = new PList()
				for(String prod_str: rhs_str.split(PRODUCTION_SEPARATOR)) {
					if (prod_str.trim() != "") {
						def production = new Production()
						List<String> tokens = prod_str.trim().findAll(PATTERN)
						for(String token: tokens) {
							if (token != null && token.trim() != "") {
								if (token.trim() ==~ EMPTY_PATTERN) {
									production << ""
								}
								else if (token.trim() ==~ WEIGHT_PATTERN) {
									production.weight = Integer.parseInt(token[1..-1].trim())
								}
								else if (token ==~ CODE_PATTERN) {
									def code = token[2..-3]
									if (code.startsWith('+')) {
										def istr = new InterpolatedString(code[1..-1].trim())
										production << new NewRuleAction(istr)
									}
									else if (code.startsWith('=')) {
										production << new NewIdAction(code[1..-1].trim())
									}
									else if (code.startsWith('/')) {
										production << new CutAction()
									}
									else {
										production << new InterpolatedString(code)
									}
								}
								else if (token ==~ NON_TERMINAL_PATTERN_RHS) {
									production << createNonTerminal(token)
								}
								else {
								 	production << token.toString()
								}
							 }
						 }
						 rhs = rhs.plus(production)
					}
				}

				g = g.plus(lhs, rhs)
			}
		}
		return g
	}
 
	String translate(List<Integer> codons) {
		List<String> symbolsUsed = []

		int usedCodons = 0
        int wraps = 0
        def programSource = []
        PList<Production> choices = new PList()
        
        def toVisit = []
        List<ProductionScope> scopes = []
        
		NonTerminal startRule = createNonTerminal("<$startSymbol>")
        toVisit << startRule

        Grammar curGram = grammar
        ProductionScope scope = new ProductionScope("ROOT", curGram)
        scopes << scope

        int maxCycles = 10000
        while ((wraps < maxWraps) && maxCycles > 0) {

            if (((usedCodons % codons.size()) == 0) && (usedCodons > 0) && (choices.size() > 1))
                wraps += 1
            maxCycles -= 1

            while (scope.symToConsume == 0 && scopes.size() > 1) {
            	ProductionScope tmp = scope
            	scopes.remove(0)
        		scope = scopes.head()	
            	scope.value.append(tmp.value)
            	scope.context = scope.context.plus(tmp.name, tmp.value.toString())
            	scope.grammar = curGram
            }

            if (toVisit.size() == 0 && scopes.size() <= 1)
            	break;

            if (toVisit.size() > 0) {
	            def node = toVisit.head()
	            toVisit.remove(0)
	            scope.symToConsume -= 1
	            
	            switch(node) {
	                case {it instanceof NonTerminal}:

	                	NonTerminal nt = (NonTerminal) node

	                	choices = new PList()
						PMap<String,Object> bindings = new PMap<String,Object>()

						// Choice the rules
						for(Map.Entry<NonTerminal, PList<Production>> e : curGram.rules.entrySet()) {
							NonTerminal lhs = e.getKey()
				            PList<Production> rhs = e.getValue()
							if (lhs.name == nt.name) {
								if (lhs.attrValue >= 0 && nt.attrValue >= 0) {
									// ex. <NT(3)> with <NT(3)> or <NT> with <NT>
									if (lhs.attrValue == nt.attrValue) {
										choices = rhs
										break
									}
								}
								else {
									if (lhs.isAllowed(nt.attrValue)) {
										// ex. <NT(n)> with <NT(3)>
										if (lhs.hasSymbolicAttribute() && nt.attrValue >= 0) {
											bindings = bindings.plus(lhs.attr, nt.attrValue)
											choices = choices.plusAll(rhs)
										}
									}
								}
							}
						}

						if (choices == null)
	                        throw new Exception("not found nt name ${nt.name} in grammar $curGram")
	                    if (choices.size() == 0)
	                        throw new Exception("not found non terminal symbol ${nt.name} in grammar ${curGram}")

	                    // Choose production
	                    int chosenProduction = -1
						chosenProduction = selectProduction(choices, codons[usedCodons % codons.size()])
						if (choices.size() > 1)
		                    usedCodons += 1

						def nts = []
						Production prod = choices[chosenProduction]
						prod.elements.each { item ->
							if (item instanceof NonTerminal) {
								NonTerminal element = (NonTerminal) item
								NonTerminal newNT = element.copy()
								if (element.hasAttribute())
									newNT.evaluateAttribute(bindings)
								nts << newNT
							}
							else
								nts << item
						}
						toVisit = nts + toVisit

						scope = new ProductionScope(nt.name, curGram)
	                    scopes = [scope] + scopes
	                    
	                    scope.symToConsume = prod.elements.size()

	                    scope.context = scope.context.plusAll(bindings)
	                break;
					case {it instanceof NewIdAction}:
						String newId = ((NewIdAction)node).getId(symbolsUsed)
						symbolsUsed << newId
						scope.value.append(newId)
	                break;
	                case {it instanceof NewRuleAction}:
						Grammar newGram = ((NewRuleAction)node).getGrammar(curGram, scope.context)
						curGram = newGram
	                break;
	                case {it instanceof CutAction}:
	                	ProductionScope parentScope = scopes.size() > 1 ? scopes[1] : null
	                	if (parentScope != null)
	                		curGram = parentScope.getGrammar()
	                break;
	                case {it instanceof InterpolatedString}:
	                	def istr = (InterpolatedString) node
	                	istr.setBinding(scope.context)
	                	scope.value.append(istr.toString().trim())
	                break;
	                default:
	                	// Terminal: simple string
	                    scope.value.append(node.toString().trim())
	                break;
	            }
	        }            
        }

        if (maxCycles <= 0 || wraps > maxWraps || toVisit.size() > 0)
			return "<ABORTED>" //aborted
            //throw new Exception("max number of iteration reached: check the recursion nodes in your grammar")
        String output = scope.value.toString()
        if (output != null)
			return replaceEntities(output, replacements)
		return "<NULL>"

        //--------------------------------------------------------

	}
	
	Map<String,String> replacements = [ '\\gt': '>', '\\lt': '<', '\\s': ' ', '\\n': '\n\r', '\\p': '|',  '¦': '|' ]
	String replaceEntities(String str, Map<String,String> replacements) {
		String tmp = str
		replacements.each { String key, String value ->
			tmp = tmp.replace(key, value)
		}
		return tmp
	}
	
	static int selectProduction(List<Production> choices, int codon) {
		// roulette wheel
		int max = choices.sum { Production prod -> prod.weight } as int
		int pick = codon % max
		int cursor = 0
		for (int i=0; i < choices.size(); i++) {
			cursor += choices[i].weight
			if (cursor > pick)
				return i
		}
		return -1
	}
	
	static NonTerminal createNonTerminal(String nterm) {
		List<String> nts = extractNonTerminal(nterm)
		def nt = new NonTerminal(nts[0],nts[1])
		if (nts.size() == 4) {
			Constraint c = Constraint.CreateConstraint(nts[2], Float.parseFloat(nts[3]))
			nt.constraint = c
		}
		return nt
	}
	
	static List<String> extractNonTerminal(String nterm) {
		Matcher m = FUNC_PATTERN.matcher(nterm)
		List<String> grs = []
		if (m.matches()) {
		    String g1 = m.group(1)
		    if (g1 != null && g1 != "")
		        grs << g1
		    String g2 = m.group(2)
		    if (g2 != null && g2 != "")
		        grs << g2.trim()
		    String g3 = m.group(3)
		    if (g3 != null && g3 != "")
		        grs << g3.trim()
		    String g4 = m.group(4)
		    if (g4 != null && g4 != "")
		        grs << g4.trim()
		    return grs
		}
		else
			return [nterm[1..-2], '']
	}
	
}