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

import groovy.test.GroovyTestCase

class GrammaticalEvolutionTests extends GroovyTestCase {

	void testKeplerGE() {
		def grammarText = getClass().getResourceAsStream("resources/grammars/symreg.mirgram").getText()
		def gr = new MirGram(grammarText, "closure", 3)
		Map<Float,Float> data = [0.72f: 0.61f, 1.00f: 1.00f, 1.52f: 1.84f, 5.20f: 11.90f, 9.53f: 29.40f, 19.10f: 83.50f]
		def fit = new SymbolicRegression(data)
		def ge = new MirGene(gr,fit,100L).with {
			populationSize = 25
			genomeSize = 100
			maxGenerations = 5
			it
		}
		def best = ge.runGE()
		assert best.individual.blueprint.trim() == "{ float distance ->  sqrt(pow(distance,3))}"
		assert best.fitness == 0.02018882f
	}
}