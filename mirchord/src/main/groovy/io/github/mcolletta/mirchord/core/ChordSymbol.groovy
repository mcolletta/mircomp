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

import groovy.transform.ToString
import groovy.transform.TupleConstructor



enum ChordAltType { ADD, SUB, ALT }


class ChordAlteration {
	int degree
	int accidental
	ChordAltType type
	int interval

	ChordAlteration(String degree) {
		this(Integer.parseInt(degree))
	}
	
	ChordAlteration(int degree) {
		this.degree =degree
		switch(degree) {
			case '5':
				interval = 7
				break
			case '7':
				interval = 11
				break
			case '9':
				interval = 14
				break
			case '11':
				interval = 17
				break
			case '13':
				interval = 21
				break
			default:
				break
		}
	}
	
	String toString() {
		return "($degree, $accidental, $type)"
	}
}

/* 
 
Chord Leading Reference
These Chords ... Lead to These Chords ...
I 		->		Any chord
ii		->		IV, V, vii°
iii		-> 		ii, IV, vi
IV		-> 		I, iii, V, vii°
V		-> 		I
vi		-> 		ii, IV, V, I
vii°	-> 		I, iii

 */

@ToString
@TupleConstructor()
class ChordSymbol implements MusicElement {
	ChordKind kind
	Pitch root
	Pitch bass
	Fraction duration=fr(1,4)
	boolean upInversion = true
	ChordAlteration chordAlteration
	String text = ""
	
	ChordSymbol(ChordSymbol chordsym) {
		this.root = chordsym.root
		this.kind = chordsym.kind
		this.chordAlteration = chordsym.chordAlteration
		this.bass = chordsym.bass
		this.text = chordsym.text
		this.duration = chordsym.duration
	}
	
	ChordSymbol(Pitch root, ChordKind kind) {
		this.root = root
		this.kind = kind != null ? kind : ChordKind.MAJOR
	}
	
	boolean isChordSymbol() {
		return true
	}
	
	Chord getChord() {
		List<Pitch> pitches = []
		Pitch _root = root.copy()
		pitches << _root

		List<Integer> intervals = Utils.getIntervalsFromChordKind(kind)
		if (chordAlteration != null) {
			switch(chordAlteration.type) {
				case ChordAltType.ALT:
					for(int i=0; i<intervals.size(); i++) {
						int interval = intervals[i]
						if (interval == chordAlteration.interval)
							intervals[i] += chordAlteration.accidental
					}
					break
				case ChordAltType.ADD:
					intervals << (chordAlteration.interval + chordAlteration.accidental) 
					break
				case ChordAltType.SUB:
					intervals.remove(chordAlteration.interval + chordAlteration.accidental)
					break
			}
		}
		
		for(int interval : intervals) {
			Pitch pitch = new Pitch()
			pitch.setMidiValue(root.midiValue + interval)
			pitches << pitch
		}
		if (bass != null) {
			boolean foundBassPitch = false
			// pitches.reverseEach { Pitch pitch ->
			for(int i=pitches.size()-1; i>=0; i--) {
				Pitch pitch = pitches[i]
				if (upInversion) {
					if (foundBassPitch) 
						pitch.octave += 1
				} else {
					if (!foundBassPitch) 
						pitch.octave -= 1
				}
				if (pitch.symbol == bass.symbol)
					foundBassPitch = true
			}
			if (!foundBassPitch) {
				Pitch _bass = bass.copy()
				_bass.octave = _root.octave
				if (_bass >= _root)
					_bass.octave -= 1
				pitches << _bass
			}
		}
		return new Chord([pitches:pitches, duration:duration])   // or [:] as Chord
	}
	
	String getKindText() {
		return kind.toString().replace('_', '-')
	}

	String toSymbolString() {
		String str = root.toSymbolString()
		str += getKindText().toLowerCase()
		return str
	}
	
	String toString() {
		String str = root.toString()
		if (text != "")
			str += text
		else
			str += kind.toString()
		return str
	}

	String getMusicElementType() {
		return "ChordSymbol"
	}

	boolean isCopyable() {
		return true
	}

	ChordSymbol copy() {
		ChordSymbol clone = new ChordSymbol(this)
	}
}

enum ChordKind {
	MAJOR,
	MINOR,
	AUGMENTED,
	DIMINISHED,
	DOMINANT,
	MAJOR_SEVENTH,
	MINOR_SEVENTH,
	DIMINISHED_SEVENTH,
	AUGMENTED_SEVENTH,
	HALF_DIMINISHED,
	MAJOR_MINOR,
	MAJOR_SIXTH,
	MINOR_SIXTH,
	DOMINANT_NINTH,
	MAJOR_NINTH,
	MINOR_NINTH,
	DOMINANT_11TH,
	MAJOR_11TH,
	MINOR_11TH,
	DOMINANT_13TH,
	MAJOR_13TH,
	MINOR_13TH,
	SUSPENDED_SECOND,
	SUSPENDED_FOURTH,
	NEAPOLITAN,
	ITALIAN,
	FRENCH,
	GERMAN,
	PEDAL,
	POWER,
	TRISTAN,
	OTHER,
	NONE
}

