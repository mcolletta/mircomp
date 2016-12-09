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

import java.math.RoundingMode
import java.util.List

import groovy.transform.CompileStatic

import com.googlecode.lingwah.Grammar
import com.googlecode.lingwah.Parser
import com.googlecode.lingwah.parser.ParserReference
import com.googlecode.lingwah.Document
import com.googlecode.lingwah.StringDocument
import com.googlecode.lingwah.ParseResults
import com.googlecode.lingwah.ParseContext


@CompileStatic
public class MirChordGrammar extends Grammar {
		public final Parser inline_comment = seq(str("/*"), zeroOrMore(anyChar()), str("*/"))
		//public final Parser inline_comment = seq(str(";"), zeroOrMore(regex("[^;]+")), str(";"))
		//public final Parser inline_comment = seq(str(";"), zeroOrMore(regex("[^\n]+")))
		public final Parser measure = str("|")
		public final Parser slash = str("/")
		public final Parser ws  = oneOrMore(cho(oneOrMore(regex("[ \\\t\n\f\r]")), inline_comment))
		public final Parser digit = regex("[0-9]")
		public final Parser number = oneOrMore(digit)
		public final Parser integerNumber = seq(opt(str('-')), number)
		public final Parser decimal = seq(opt(str('-')), seq(number, opt(seq(str('.'), number))))
		public final Parser symbol = regex("^[_A-Za-z](?:_?[A-Za-z0-9])*")
		public final Parser stringa = regex("\"[^\"]*\"")
		public final Parser identifier = seq(str(":"), symbol)
		public final Parser command = cho(symbol, str("+"), str("-"), str("*"))
		
		public final Parser sharp = str("#")
		public final Parser flat = str("&")
		public final Parser natural = str("§")
		public final Parser accidental = cho(sharp, flat, natural)
		public final Parser accidentals = oneOrMore(accidental)
		public final Parser unpitched = regex("[xo]")
		public final Parser pitchName = regex("[a-g]")
		public final Parser chordPitchName = regex("[A-G]")
		public final Parser octaveUp = str("'")
		public final Parser octaveDown = str(",")
		public final Parser octave = cho(octaveUp, octaveDown)
		public final Parser octaves = zeroOrMore(octave)
		public final Parser pitch = seq(pitchName, opt(accidentals), opt(octaves))		
		public final Parser velocity = seq(str("`"), cho(str("fffff"), str("ffff"), str("fff"), str("ff"), str("f"), str("mf"), str("mp"), str("p"), str("pp"), str("ppp"), str("pppp"), str("ppppp")))
		
		public final Parser stemUp = str("stemUp")
		public final Parser stemDown = str("stemDown")
		public final Parser stemAuto = str("stemAuto")
		public final Parser stem = cho(stemUp, stemDown, stemAuto)

		public final Parser tieStart = str("_")
		public final Parser tieEnd = str("_")
		public final Parser dot = str(".")
		public final Parser duration =  seq(number, zeroOrMore(dot))
		public final Parser rest = seq(str("r"), opt(duration))
		public final ParserReference pitchList = ref()
		public final Parser pitches = oneOrMore(pitch).separatedBy(ws)
		public final Parser chord = seq(cho(pitchList, pitch, unpitched), opt(duration))

		public final Parser part = seq(str("="), number)  // staff
		public final Parser voice = seq(str("~"), number)
		public final Parser anchor = seq(str("@"), symbol)
		public final Parser repeatStart = str("|:")
		public final Parser repeatEnd = seq(number, str(":|"))

		public final Parser relativeOctave = seq(str("^"), digit)
		public final Parser stickyDuration = seq(str("%"), duration)

		// chord symbols	
		public final Parser chordRoot = seq(chordPitchName, opt(accidentals))
		public final Parser chordAltOp = cho(str("add"), str("sub"))
		
		public final Parser chordModifierMin = cho(str("m"), str("min"))
		public final Parser chordModifierMaj = cho(str("M"), str("maj"))
		public final Parser chordModifierMinMaj = cho(str("mM"), str("minMaj"))
		public final Parser chordModifierAug = cho(str("+"), str("aug"))
		public final Parser chordModifierDim = cho(str("°"), str("dim"))
		public final Parser chordModifierHDim = str("0")
		public final Parser chordModifierSus = str("sus")
		
		public final Parser chordModifier = cho(chordModifierMin, chordModifierMaj, chordModifierMinMaj, chordModifierAug, chordModifierDim, chordModifierHDim, chordModifierSus)
		public final Parser chordExtension = cho(str("2"), str("4"), str("5"), str("7"), str("9"), str("11"), str("13"))
		public final Parser chordKind = seq(opt(chordModifier), opt(chordExtension))
		
		public final Parser chordAltDegree = cho(str("5"),str("7"), str("9"), str("11"), str("13"))
		public final Parser chordAlteration = seq(str("("), opt(chordAltOp), opt(accidentals), chordAltDegree, str(")"))
		
		public final Parser chordBassSeparator = cho(str("/"),str("\\"))
		public final Parser chordBass = seq(chordPitchName, opt(accidentals))
		
		public final Parser sameChordSymbol = str("//")
		
		public final Parser chordSymbol = seq(chordRoot, chordKind, opt(chordAlteration), opt(seq(chordBassSeparator, chordBass)))

		public final Parser atom = seq(opt(tieEnd), cho(rest, chord, chordSymbol, sameChordSymbol), opt(velocity), opt(tieStart))
		public final ParserReference phrase = ref() 
		public final ParserReference sexpr = ref()
		public final Parser contextElement = cho(relativeOctave, stickyDuration, stem, measure)
		public final Parser musicElement = cho(contextElement, anchor, repeatStart, repeatEnd, sexpr, atom, phrase, identifier)
		public final Parser elements = oneOrMore(musicElement).separatedBy(opt(ws))

		public final Parser parm = cho(stringa, identifier, number, integerNumber, decimal, musicElement, sexpr)
		public final Parser parms = oneOrMore(parm).separatedBy(ws)

		public final Parser scorePosition = cho(part, voice)
		public final Parser scoreElement = cho(scorePosition, musicElement)
		public final Parser score = oneOrMore(scoreElement).separatedBy(opt(ws))
		
		private MirChordGrammar() {
				init()
				sexpr.define(seq(str("("), command, parms, str(")")).separatedBy(opt(ws)))
				phrase.define(seq(str("{"), elements, str("}")).separatedBy(opt(ws)))
				pitchList.define(seq(str("["), pitches, str("]")).separatedBy(opt(ws)))
		}
		
		public static final MirChordGrammar INSTANCE = new MirChordGrammar()		
}

