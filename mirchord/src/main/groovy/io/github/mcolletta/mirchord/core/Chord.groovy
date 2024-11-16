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

package io.github.mcolletta.mirchord.core

import com.xenoage.utils.math.Fraction
import static com.xenoage.utils.math.Fraction.fr
import static com.xenoage.utils.math.Fraction._0

import groovy.transform.Canonical
import groovy.transform.ToString
import groovy.transform.TupleConstructor


class Pitch implements MusicElement, Comparable<Pitch> {
	
	private String symbol
	int alteration
	int octave
	int symbolSemitones
	
	static Map<String,Integer> mapping = ['C': 0, 'D': 2, 'E': 4, 'F': 5, 'G': 7, 'A': 9, 'B': 11]
    
	Pitch(String symbol='C', int octave=4, int alteration=0) {
		this.setSymbol(symbol)
		this.octave = octave
		this.alteration = alteration		
		this.symbolSemitones = mapping[symbol]
	}

	Pitch(Pitch pitch) {
		this.setSymbol(pitch.symbol)
		this.octave = pitch.octave
		this.alteration = pitch.alteration
	}
	
	void setSymbol(String symbol) {
		this.symbol = symbol
		this.symbolSemitones = mapping[symbol]
	}

	String getSymbol() {
		return symbol
	}
	
	@Override public int compareTo(Pitch pitch) {
		if (getMidiValue() > pitch.getMidiValue()) 
			return 1
		else {
			if (getMidiValue() < pitch.getMidiValue()) 
				return -1
			else {
				if (getMidiValue() == pitch.getMidiValue()) return 0
					return 0
			}
		}
	}
	
	void  alterForKeySignature(int keySig) {
		switch(keySig) {
			case { symbolSemitones == 11 && (keySig <= -1) }:
				alteration -= 1
				break
			case { symbolSemitones == 4 && (keySig <= -2) }:
				alteration -= 1
				break
			case {symbolSemitones == 9 && (keySig <= -3) }:
				alteration -= 1
				break
			case { symbolSemitones == 2 && (keySig <= -4) }:
				alteration -= 1
				break
			case { symbolSemitones == 7 && (keySig <= -5) }:
				alteration -= 1
				break
			case { symbolSemitones == 0 && (keySig <= -6) }:
				alteration -= 1
				break
			case { symbolSemitones == 5 && (keySig <= -7) }:
				alteration -= 1
				break
			case { symbolSemitones == 5 && (keySig >= 1) }:
				alteration += 1
				break
			case { symbolSemitones == 0 && (keySig >= 2) }:
				alteration += 1
				break
			case { symbolSemitones == 7 && (keySig >= 3) }:
				alteration += 1
				break
			case { symbolSemitones == 2 && (keySig >= 4) }:
				alteration += 1
				break
			case { symbolSemitones == 9 && (keySig >= 5) }:
				alteration += 1
				break
			case { symbolSemitones == 4 && (keySig >= 6) }:
				alteration += 1
				break
			case { symbolSemitones == 11 && (keySig >= 7) }:
				alteration += 1
				break
			default:
				break
		}
	}
	
	void setMidiValue(int value) {
		octave = (int)(value / 12) - 1
		int chromatic = value % 12
		// ["C", "C#", "D", "Eb", "E", "F", "F#", "G", "G#", "A", "Bb", "B"]
		switch(chromatic) {
			case 0:
				symbol =  'C'
				alteration = 0
			break
			case 1:
				symbol =  'C'
				alteration = 1
			break
			case 2:
				symbol =  'D'
				alteration = 0
			break
			case 3:
				symbol =  'E'
				alteration = -1
			break
			case 4:
				symbol =  'E'
				alteration = 0
			break
			case 5:
				symbol =  'F'
				alteration = 0
			break
			case 6:
				symbol =  'F'
				alteration = 1
			break
			case 7:
				symbol =  'G'
				alteration = 0
			break
			case 8:
				symbol =  'G'
				alteration = 1
			break
			case 9:
				symbol =  'A'
				alteration = 0
			break
			case 10:
				symbol =  'B'
				alteration = -1
			break
			case 11:
				symbol =  'B'
				alteration = 0
			break
			default:
			break
		}
		this.symbolSemitones = mapping[symbol]
	}
	
	int getMidiValue() {
		return (symbolSemitones + ((octave+1) * 12)) + alteration
	}

	public String toSymbolString() {
		String strAlter = ""
		String alterSym = ""
		if (alteration != 0) {
			alterSym = (alteration > 0) ? "#" : "&"
			(1..Math.abs(alteration)).each {
				strAlter += alterSym
			}
		}
		return "$symbol$strAlter"
	}
	
	public String toString() {
		return  "${toSymbolString()}$octave[${getMidiValue()}]"
	}

	boolean isCopyable() {
		return true
	}

	Pitch copy() {
		Pitch clone = new Pitch(this)
	}
}

enum ACCIDENTALS {
	SHARP(1), FLAT(-1), NATURAL(0)
	ACCIDENTALS(int value) { this.value = value }
	private final int value
	public int value() { return value }
}

@Canonical
class Rest implements MusicElement  {
	Fraction duration
	boolean hidden

	Rest() {
		duration = fr(1,4)
		hidden = false
	}

	Rest(Fraction duration) {
		this.duration = duration
	}

	Rest(Rest rest) {
		this.duration = rest.duration
		this.hidden = rest.hidden
	}

	String getMusicElementType() {
		return "Rest"
	}

	boolean isCopyable() {
		return true
	}

	Rest copy() {
		Rest clone = new Rest(this)
	}
}

class Chord implements MusicElement {
	
	List<Pitch> pitches = []
	Fraction duration = fr(1,4)
	float velocity = 90
	String dynamicMark = ""
	
	boolean tieStart = false
	boolean tieEnd = false

	StemDirection stem = StemDirection.AUTO

	boolean unpitched;
	
	// Use a Map constructor
	Chord() {
		this.pitches = []
	}

	Chord(Pitch pitch) {
		this.pitches = [pitch]
	}

	Chord(Pitch pitch, Fraction duration) {
		this.pitches = [pitch]
		this.duration = duration
	}

	Chord(List<Pitch> pitches, Fraction duration) {
		this.pitches = pitches
		this.duration = duration
	}
	
	Chord(Chord chord) {
		List<Pitch> chord_pitches = []
		for (Pitch p : chord.getPitches()) {
			Pitch newPitch = p.copy()  // new Pitch(p.symbol, p.octave, p.alteration)
			chord_pitches.add(newPitch)
		}
		this.pitches = chord_pitches
		this.duration = chord.duration
		this.dynamicMark = dynamicMark
		this.velocity = chord.velocity
		this.tieStart = chord.tieStart
		this.tieEnd = chord.tieEnd
		this.unpitched = chord.unpitched
	}

	Pitch getPitch() { // getRoot
		if (pitches && pitches.size() > 0)
			return pitches[0]
		return null
	}

	void setPitch(Pitch pitch) {
		pitches = [pitch]
	}
	
	void setDynamicMark(String dynamic) {
		this.dynamicMark = dynamic
		this.velocity = Utils.DynamicsLevels[dynamic]
	}
	
	void setValue(Chord chord, boolean bDuration=true) {
		pitch.octave = chord.pitch.octave
		pitch.symbol = chord.pitch.symbol
		pitch.alteration = chord.pitch.alteration
		if (bDuration)
			duration = chord.duration
	}
	
	public String toString() {
		String str = ""
		if (tieEnd)
			str += "-"
		for(Pitch p : pitches) {
			str += p.toString()
		}
		if (tieStart)
			str += "-"
		return "<" + str + ", " + duration + ">"
	}

	String getMusicElementType() {
		return "Chord"
	}

	boolean isCopyable() {
		return true
	}

	Chord copy() {
		Chord clone = new Chord(this)
	}
}


