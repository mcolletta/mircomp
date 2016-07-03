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

package io.github.mcolletta.mircomp.mirchord

import groovy.transform.*

import com.xenoage.utils.math.Fraction

@Canonical
class Score {
	Map<String, Part> parts
}

@Canonical
class Rest implements MusicElement  {
	Fraction duration
	boolean hidden
}

public enum StemDirection {
			Up,
			Down,
			Auto
		}

@Canonical
class Chord implements MusicElement  {
	List<Integer> midiPitches
	Fraction duration
	boolean tieStart
	boolean tieEnd

	boolean unpitched = false

	StemDirection stem = StemDirection.Auto

	int getMidiPitch() { // getRoot
		if (midiPitches && midiPitches.size() > 0)
			return midiPitches[0]
		return null
	}

	void setMidiPitch(int midiPitch) {
		midiPitches = [midiPitch]
	}
}

@Canonical
class Phrase implements MusicElement {
	List<Chord> elements = []
}

@Canonical
class Tuplet implements MusicElement {
	// fr(3, 2) as in lilypond means a ratio of 2/3
	Fraction fraction
	//Phrase phrase
	List<Chord> elements = []

	Fraction getBaseDuration() {
		return elements[0].duration // TODO min duration?
	}
}

@Canonical
class Anchor implements MusicElement {
	String id
}


@Canonical
class Part {
	String id
	Map<String, Voice> voices
}

@Canonical
class Voice {
	String id
	List<MusicElement> elements
}

@Canonical
class Repeat implements MusicElement {
	boolean start = true
	int times
	boolean getStop() { !start }
}


interface MusicElement {}

@Canonical
class Instrument implements MusicElement {
	String id
	int midiProgram
	boolean unpitched = false

	void setMidiProgram(int midiProgram) {
		if (midiProgram < 0 || midiProgram > 128)
			throw new IllegalArgumentException("MIDI program must be between 0 and 127");
		this.midiProgram = midiProgram;
	}
}

enum ClefType { Treble, Bass, Percussion }

@Canonical
class Clef implements MusicElement {
	ClefType type
}

public enum KeyMode {
			Major,
			Minor,
			Ionian,
			Dorian,
			Phrygian,
			Lydian,
			Mixolydian,
			Aeolian,
			Locrian
		}

@Canonical
class Key implements MusicElement {
	int fifths
	KeyMode mode
}

@Canonical
class Time implements MusicElement {
	Fraction time
}

@Canonical
class Tempo implements MusicElement {
	Fraction baseBeat
	int beatsPerMinute
	String text
}

/*
static List<Phrase> RepeatCommand(int num, MusicElement... elements) {
	TODO
}

static Phrase TransposeCommand(int num, MusicElement... elements) {
	TODO
	def list = []
	elements.each { chord -> 
		list << chord.copyWith(midiPitches: ... + num...)
	}
	return list
}*/

