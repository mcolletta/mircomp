/*
 * Copyright (C) 2016-2024 Mirco Colletta
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

import com.xenoage.utils.math.Fraction
import static com.xenoage.utils.math.Fraction.fr
import static com.xenoage.utils.math.Fraction._0

import com.xenoage.utils.pdlib.PMap
import com.xenoage.utils.pdlib.PList

import groovy.transform.ToString
import groovy.transform.EqualsAndHashCode


public enum ConstraintType { LESS_THAN, LESS_THAN_OR_EQUAL, GREATER_THAN, GREATER_THAN_OR_EQUAL }


@EqualsAndHashCode
public class Constraint {
	ConstraintType type
	Fraction value

	Constraint() {}
	
	Constraint(ConstraintType type, Fraction value) {
		this.type=type
		this.value=value
	}

	static Constraint CreateConstraint(String typeText, Fraction value) {
		Constraint c = new Constraint()
		c.value = value
		switch(typeText) {
			case "lt":
				c.type = ConstraintType.LESS_THAN
			break
			case "lte":
				c.type = ConstraintType.LESS_THAN_OR_EQUAL
			break
			case "gt":
				c.type = ConstraintType.GREATER_THAN
			break
			case "gte":
				c.type = ConstraintType.GREATER_THAN_OR_EQUAL
			break
			default:
				throw new Exception("Constraint $typeText not valid.")
		}
		return c
	}

	boolean isAllowed(Fraction n) {
		switch(type) {
			case ConstraintType.LESS_THAN:
				return (n < value) 
			break
			case ConstraintType.LESS_THAN_OR_EQUAL:
				return (n <= value) 
			break
			case ConstraintType.GREATER_THAN:
				return (n > value) 
			break
			case ConstraintType.GREATER_THAN_OR_EQUAL:
				return (n >= value) 
			break
			default:
				return true
		}
	}

	public String toString() {
		return "Constraint(" + type + "," + value + ")";
	}
}

@EqualsAndHashCode
public class NonTerminal {
	String name
	String attr
	Fraction attrValue
	Constraint constraint

	NonTerminal(String name, String attr="") {
		this.name = name
		setAttr(attr)
	}

	/*
		<NT(3)>  attr="3"  attrValue=3
		<NT(n)>  attr="n"  attrValue=-1
		<NT>  attr=""  attrValue=0
	*/
	void setAttr(String sa) {
		attr = sa
		if (attr != null && !attr.isEmpty()) {
			String[] parts =  attr.split('/')
			boolean isFraction = parts[0].isNumber()
			if (parts.size() > 1)
				isFraction = isFraction && parts[1].isNumber()
			if (isFraction)
				attrValue = Fraction.parse(attr)
			else
				attrValue = fr(-1)
		}
		else
			attrValue = _0  // default
	}

	boolean hasAttribute() {
		return attr != null && !attr.isEmpty();
	}

	boolean hasSymbolicAttribute() {
		return attrValue < _0;
	}
	
	void evaluateAttribute(Map binding=[:]) {
		attrValue = (Fraction) Expression.parse(attr, binding)
		if (attrValue < _0)
			throw new Exception("Attribute value can't be negative $attr  $binding.")
	}

	boolean isAllowed(Fraction n) {
		if (constraint != null)
			return constraint.isAllowed(n)
		return true
	}

	NonTerminal copy() {
		return new NonTerminal(name, attr)
	}

	String toString() {
		return "NonTerminal(" + name + ", " + attr + ", " + attrValue + "," + constraint + ")"
	}
}

@EqualsAndHashCode
public class Production {
	
	PList elements
	int weight
	
	Production(PList list=new PList(), int weight=10) {
		elements =  new PList(list)
		this.weight = weight
	}
	
	void leftShift(item) {
		elements = elements.plus(item)
	}

	String toString() {
		return "Production(" + elements + ", weight:" + weight + ")"
	}
}

class InterpolatedString {

	String L_DELIM = "\${"
	String R_DELIM = "}"

	private String term
	private LinkedList<LinkedHashMap<String,Object>> indexes
	Map binding

	InterpolatedString(String term) {
		this.term = term
		indexes = new LinkedList<LinkedHashMap<String,Object>>()
		scan()
	}

	private void scan() {
		int idx = 0
		int li = 0
		int ri = 0
		while (li > -1) {
			li = term.indexOf(L_DELIM, idx)
			if (li >= 0) {
				ri = term.indexOf(R_DELIM, li + L_DELIM.length())
				String key = term.substring(li + L_DELIM.length(), ri)
				idx = ri + R_DELIM.length()
				indexes.add([key:key, left:li, right:idx] as LinkedHashMap<String,Object>)
			}
		}
	}

	public String toString() {
		if (binding != null) {
			//println "indexes = $indexes"
			StringBuilder outBuilder = new StringBuilder()
			int idx = 0
			for (LinkedHashMap dict : indexes) {
				String key = (String)dict["key"];
				int left = (int)dict["left"]
				int right =  (int)dict["right"]
				outBuilder.append(term.substring(idx, left))
				if (binding.containsKey(key)) {
					outBuilder.append(binding[key])
				}
				idx = right
			}
			outBuilder.append(term.substring(idx))
			return outBuilder
		}
		return term
	}
}

// Marker
class SemanticAction {}

@EqualsAndHashCode
class NewRuleAction extends SemanticAction {
	InterpolatedString istr
	
	NewRuleAction(InterpolatedString istr) {
		this.istr = istr
	}
	
	Grammar getGrammar(Grammar g0, Map binding=[:]) {
		istr.setBinding(binding)
		Grammar tmp = MirGram.parse(istr.toString(), false)
		Grammar g = g0
		for(Map.Entry<NonTerminal, PList<Production>> e : tmp.rules.entrySet()) {
			NonTerminal k = e.getKey()
            PList<Production> v = e.getValue()
            PList<Production> rhs = v
			if (g0.rules.containsKey(k))
				rhs = g0.rules[k].plusAll(v)
			g = g.plus(k, rhs)
		}
		return g
	}

	public String toString() {
		return "NewRuleAction(" + istr + ")"
	}
}

@ToString
@EqualsAndHashCode
class NewIdAction extends SemanticAction {
	String prefix

	NewIdAction(String prefix) {
		this.prefix = prefix
	}
	
	String getId(List<String> symbolsUsed=[]) {
		int i = 1;
		while (symbolsUsed.contains(prefix + i))
			i++
		return (prefix + i)
	}
}

@EqualsAndHashCode
class CutAction extends SemanticAction {}


@EqualsAndHashCode
public class Grammar {
	PMap<NonTerminal, PList<Production>> rules

	Grammar() {
		rules = new PMap()
	}

	Grammar(PMap<NonTerminal, PList<Production>> paramRules) {
		rules = paramRules
	}


	Grammar plus(NonTerminal lhs, PList<Production> rhs) {
		PMap<NonTerminal, PList<Production>> newRules = rules.plus(lhs, rhs)
		Grammar newGrammar = new Grammar(newRules)
		return newGrammar
	}

	public String toString() {
		String str = ""
		for(Map.Entry<NonTerminal, PList<Production>> e : rules.entrySet()) {
			NonTerminal k = e.getKey()
            PList<Production> v = e.getValue()
            str += "" + k + " ::= " + v + "\n"
        }
		return str
	}
}

@EqualsAndHashCode
public class ProductionScope {
	Grammar grammar

	String name
	StringBuffer value

	PMap<String, Object> context
	int symToConsume

	ProductionScope(String paramName, Grammar paramGrammar) {
		name = paramName
		value = new StringBuffer()
		grammar = paramGrammar
		symToConsume = 1
		context = new PMap<String, Object>()
	}

	public String toString() {
		return "Scope(" + name + "," + value + "," + symToConsume + "," + context + ")"
	}
}