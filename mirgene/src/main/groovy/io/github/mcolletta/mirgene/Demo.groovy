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

import io.github.mcolletta.mirgene.*

import java.util.stream.Collectors

import static java.lang.Math.*

import com.xenoage.utils.pdlib.PList


class Test {

	String getSymRegGrammar() {
		return getClass().getResourceAsStream("resources/grammars/symreg.mirgram").getText()
	}

	String getMelodyGrammar() {
		return getClass().getResourceAsStream("resources/grammars/melody.mirgram").getText()
	}
	
	public static void main(String[] args) {
		Test test = new Test()
		def mel = test.getMelodyGrammar()
		exampleMelodyGrammar(mel)
		def symreg = test.getSymRegGrammar()
		exampleGE(symreg)
	}

	static void exampleMelodyGrammar(String grammarText) {
		def gr = new MirGram(grammarText, "score", 3, true)
		List<Integer> cods = []
		def rand = new Random(123456789L)
		(1..1000).each {
			cods << rand.nextInt(256)
		}
		def result = gr.translate(cods)
		println "result="
		println result
	}

	static void exampleGE(String grammarText) {
		def gr = new MirGram(grammarText, "closure", 3)
		Map<Float,Float> data = [:]
		[0.25f, 0.75f, 0.9f, 1.0f, 1.33f, 2.7f, 3.33f, 5.1f, 7.12f].each {
			data[it] = (float) (pow(it,4) + pow(it,3) + pow(it,2) + pow(it,1) + 1)
		}
		def fit = new SymbolicRegression(data)
		def ge = new MirGene(gr,fit,100L).with {
			populationSize = 100
			genomeSize = 100
			maxGenerations = 50
			it
		}
		def best = ge.runGE()
		println """
			Best Individual:
			${best.individual}
			with fitness: ${best.fitness}
		"""
	}
}

Test.main()