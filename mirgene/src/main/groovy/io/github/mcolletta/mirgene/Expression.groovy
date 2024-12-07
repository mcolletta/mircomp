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

import java.math.*;
import java.util.List;

import com.xenoage.utils.math.Fraction

import com.googlecode.lingwah.*;
import com.googlecode.lingwah.Grammar as LingwahGrammar;
import com.googlecode.lingwah.parser.ParserReference;


public class ExpressionGrammar extends LingwahGrammar {
		public final Parser inline_comment = seq(str("/*"), zeroOrMore(anyChar()), str("*/"));
		public final Parser ws  = oneOrMore(cho(oneOrMore(regex("[ \t\n\f\r]")), inline_comment));
		public final Parser character = regex("[a-zA-Z]");
		public final Parser identifier = seq(character, zeroOrMore(cho(character,digit)));
		public final Parser digit = regex("[0-9]");
		public final Parser number = oneOrMore(digit);
		public final Parser fraction = seq(number, opt(seq(str('/'), number)));
		public final ParserReference expr = ref();
		public final Parser addition = seq(expr, str('+'), expr).separatedBy(opt(ws));
		public final Parser subtraction = seq(expr, str('-'), expr).separatedBy(opt(ws));
		public final Parser multiplication = seq(expr, str('*'), expr).separatedBy(opt(ws));
		public final Parser group = seq(str('('), expr, str(')')).separatedBy(opt(ws));
		{
				expr.define(cho(character, fraction, addition, subtraction, multiplication, group));
		}
		
		private ExpressionGrammar() {
				init();
		}
		public static final ExpressionGrammar INSTANCE = new ExpressionGrammar();
}

//@Processes(ExpressionGrammar.class)
public class ExpressionProcessor extends AbstractProcessor {

		Map binding
		
		ExpressionProcessor(Map binding) {
			super()
			this.binding = binding
		}
		
		static final ExpressionGrammar grammar= ExpressionGrammar.INSTANCE;
		
		public void completeAddition(Match addition) {
				List<Match> children= addition.getChildrenByType(grammar.expr);
				Fraction left = getResult(children.get(0));
				println left
				Fraction right= getResult(children.get(1));
				println right
				putResult(left.add(right));
		}
		public void completeSubtraction(Match subtraction) {
				List<Match> children= subtraction.getChildrenByType(grammar.expr);
				Fraction left= getResult(children.get(0));
				Fraction right= getResult(children.get(1));
				putResult(left.sub(right));
		}
		public void completeMultiplication(Match multiplication) {
				List<Match> children= multiplication.getChildrenByType(grammar.expr);
				Fraction left= getResult(children.get(0));
				Fraction right= getResult(children.get(1));
				putResult(left.mult(right));
		}
		public void completeGroup(Match group) {
				putResult(getResult(group.getChildByType(grammar.expr)));
		}
		public void completeCharacter(Match character) {
				if (binding.containsKey(character.getText()))
					putResult(binding[character.getText()]);
		}
		public void completeFraction(Match fraction) {
				putResult(Fraction.parse(fraction.getText()));
		}
		public void completeExpr(Match expr) {
				putResult(getResult(expr.getFirstChild()));
		}

		public static Fraction process(ParseResults results, Map binding) {
				ExpressionProcessor processor= new ExpressionProcessor(binding);
				results.getLongestMatch().accept(processor);
				return processor.getResult(results.getLongestMatch());
		}

}


public class Expression {
		static final Parser PARSER= ExpressionGrammar.INSTANCE.expr;
		public static Fraction parse(String expression, Map binding)
		{
				ParseResults parseResults= ParseContext.parse(PARSER, expression);
				if (!parseResults.success())
						throw parseResults.getError();
				return ExpressionProcessor.process(parseResults, binding);
		}
}

