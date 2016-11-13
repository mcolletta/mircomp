/*
 * Copyright (C) 2016 Mirco Colletta
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

package io.github.mcolletta.mirchord.interpreter

import groovy.transform.CompileStatic

import io.github.mcolletta.mirchord.core.*
import static io.github.mcolletta.mirchord.core.Utils.*

import com.googlecode.lingwah.Document
import com.googlecode.lingwah.ParseContext
import com.googlecode.lingwah.ParseResults
import com.googlecode.lingwah.StringDocument

import groovy.text.GStringTemplateEngine

@CompileStatic
class MirChordInterpreter {
	
	MirChordGrammar PARSER
	
	MirChordInterpreter() {
		PARSER = MirChordGrammar.INSTANCE
	}
	
	Score evaluate(String source) {
		Document doc= new StringDocument(source.trim())
		ParseContext ctx= new ParseContext(doc)
		ParseResults parseResults= ctx.getParseResults(PARSER.score, 0)
		if (!parseResults.success())
				throw parseResults.getError()
		MirChordProcessor processor = new MirChordProcessor([new Utils()])
		Score score = processor.process(parseResults)
		
		return score
	}
	
	public static void main(String[] args) {
		String source = new File("/home/mircoc/mirchords/test1.mirchord").text
		MirChordInterpreter interpreter = new MirChordInterpreter()
		Score score = interpreter.evaluate(source)
		println "RESULT: " + score
	}

}

@CompileStatic
class Utils {

	@MirChord 
	public Phrase transpose(Phrase phrase) {
		// TODO maybe Phrase could become List<MusicElement> ???
		println "transposing phrase " + phrase
		Phrase newPhrase = new Phrase()
		for(Chord el : phrase.elements) {
			newPhrase.elements << el
		}
		return newPhrase
	}

}