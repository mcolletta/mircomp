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

class GrammarTests extends GroovyTestCase {

	void testChordProgression() {
		def grammarText = getClass().getResourceAsStream("resources/grammars/chordprogression.mirgram").getText()
		def gr = new MirGram(grammarText, "progression", 3, true)
		def cods = []
		def rand = new Random(123456789L)
		(1..1000).each {
			cods << rand.nextInt(256)
		}
		def result = gr.translate(cods)
		assert result.trim() == "C Amin F G C Amin G C Amin Dmin G C"
	}
}