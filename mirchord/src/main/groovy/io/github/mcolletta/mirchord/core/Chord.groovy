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

package io.github.mcolletta.mirchord.core

import com.xenoage.utils.math.Fraction
import static com.xenoage.utils.math.Fraction.fr
import static com.xenoage.utils.math.Fraction._0

import groovy.transform.CompileStatic
import groovy.transform.Canonical
import groovy.transform.ToString
import groovy.transform.TupleConstructor

@CompileStatic
class Pitch implements MusicElement, Comparable<Pitch> {
	
	String symbol
	int alteration
	int octave
	int symbolSemitones
	
	static Map<String,Integer> mapping = ['C': 0, 'D': 2, 'E': 4, 'F': 5, 'G': 7, 'A': 9, 'B': 11]
    
	Pitch(String symbol='C', int alteration=0, int octave=5) {
		this.symbol = symbol
		this.alteration = alteration
		this.octave = octave
		this.symbolSemitones = mapping[symbol]
	}
	
	void setSymbol(String symbol) {
		this.symbol = symbol
		this.symbolSemitones = mapping[symbol]
	}
	
	@Override public int compareTo(Pitch pitch) {
		if (midiValue > pitch.midiValue) 
			return 1
		else {
			if (midiValue < pitch.midiValue) 
				return -1
			else {
				if (midiValue == pitch.midiValue) return 0
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
		octave = (int)(value / 12)
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
		return (symbolSemitones + (octave * 12)) + alteration
	}
	
	public String toString() {
		String strAlter = ""
		String alterSym = ""
		if (alteration != 0) {
			alterSym = (alteration > 0) ? "#" : "&"
			(1..Math.abs(alteration)).each {
				strAlter += alterSym
			}
		}
		return  "$symbol$strAlter$octave[$midiValue]"
	}
}

enum ACCIDENTALS {
	SHARP(1), FLAT(-1), NATURAL(0)
	ACCIDENTALS(int value) { this.value = value }
	private final int value
	public int value() { return value }
}

@CompileStatic
@Canonical
class Rest implements MusicElement  {
	Fraction duration
	boolean hidden
}

@CompileStatic
class Chord implements MusicElement {
	
	List<Pitch> pitches
	Fraction duration = fr(1,4)
	float velocity = 90
	String dynamicMark = ""
	
	boolean tieStart = false
	boolean tieEnd = false

	StemDirection stem = StemDirection.AUTO

	boolean unpitched
	
	// Use a Map constructor
	Chord() {
		this.pitches = [new Pitch()]
	}
	
	Chord(Chord chord) {
		List<Pitch> chord_pitches = []
		for (Pitch p : chord.getPitches()) {
			def newPitch = new Pitch(p.symbol, p.alteration, p.octave)
			chord_pitches.add(newPitch)
		}
		this.pitches = chord_pitches
		this.duration = chord.duration
		this.dynamicMark = dynamicMark
		this.velocity = chord.velocity
		this.tieStart = chord.tieStart
		this.tieEnd = chord.tieEnd
	}

	Pitch getPitch() { // getRoot
		if (pitches && pitches.size() > 0)
			return pitches[0]
		return null
	}

	void setPitch(Pitch pitch) {
		pitches = [pitch]
	}
	
	boolean isChordSymbol() {
		return false
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
		def str = ""
		if (tieEnd)
			str += "-"
		pitches.each { Pitch p -> str += p.toString()}
		if (tieStart)
			str += "-"
		return "<" + str + ", " + duration + ">"
	}
}


