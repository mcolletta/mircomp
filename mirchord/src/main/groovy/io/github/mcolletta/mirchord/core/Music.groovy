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
// import java.io.Serializable;

import com.xenoage.utils.math.Fraction
import static com.xenoage.utils.math.Fraction.fr
import static com.xenoage.utils.math.Fraction._0

import groovy.transform.CompileStatic
import groovy.transform.Canonical
import groovy.transform.ToString
import groovy.transform.TupleConstructor


@CompileStatic
enum ClefType { 
		TREBLE, 
		BASS, 
		PERCUSSION 
	}


@CompileStatic
public enum KeyMode {
			MAJOR,
			MINOR,
			IONIAN,
			DORIAN,
			PHRYGIAN,
			LYDIAN,
			MIXOLYDIAN,
			AEOLIAN,
			LOCRIAN
		}


@CompileStatic
public enum StemDirection {
			UP,
			DOWN,
			AUTO
		}


@CompileStatic
public enum ChordsMode { 
		LEADSHEET, 
		STAFF, 
		BOTH 
	}


trait MusicElement {

	String getMusicElementType() {
		return "MusicElement"
	}

	boolean isCopyable() {
		return false
	}

	MusicElement copy() {
		throw new Exception("This element cannot be copied")
	}
}

@Canonical
class Score {
	Map<String, Part> parts = [:]
	CompositionInfo info = null
}

@CompileStatic
@Canonical
class Part {
	String id
	Map<String, Voice> voices = [:]

	public boolean equals(Part part) {
		return (id == part.id)
	}
}

@CompileStatic
@Canonical
class Voice {
	String id
	List<MusicElement> elements
}

@CompileStatic
@Canonical
class Phrase implements MusicElement {
	List<MusicElement> elements = []

	Phrase() {}

	Phrase(Phrase phrase) {
		for(MusicElement element : phrase.elements) {
			if (element.isCopyable())
				this.elements.add(element.copy())
		}
	}

	String getMusicElementType() {
		return "Phrase"
	}

	boolean isCopyable() {
		return true
	}

	Phrase copy() {
		Phrase clone = new Phrase(this)
	}
}

@CompileStatic
@Canonical
class Tuplet implements MusicElement {
	// fr(3, 2) as in lilypond means a ratio of 2/3
	Fraction fraction
	List<Chord> chords = []

	Tuplet(Fraction fraction, List<Chord> chords) {
		this.fraction = fraction
		this.chords = chords
	}

	Tuplet(Tuplet tuplet) {
		this.fraction = fr(tuplet.fraction.numerator, tuplet.fraction.denominator)
		for(Chord chord : tuplet.chords) {
			this.chords.add(chord.copy())
		}
	}

	Fraction getBaseDuration() {
		return chords[0].duration // TODO min duration?
	}

	String getMusicElementType() {
		return "Tuplet"
	}

	boolean isCopyable() {
		return true
	}

	Tuplet copy() {
		Tuplet clone = new Tuplet(this)
	}
}

@CompileStatic
@Canonical
class Anchor implements MusicElement {
	String id
}

@CompileStatic
@Canonical
class Clef implements MusicElement {
	ClefType type
}

@CompileStatic
@Canonical
class KeySignature implements MusicElement {

	int fifths
	KeyMode mode

	KeySignature() {
		fifths = 0
		mode = KeyMode.MAJOR
	}

	KeySignature(int f, String modeText) {
		this.fifths = fifths
		mode = Utils.ModeFromName(modeText)
	}

	KeySignature(String keyText, String modeText) {
		fifths = Utils.FifthsFromName(keyText, modeText)
		mode = Utils.ModeFromName(modeText)
	}
}

@CompileStatic
@Canonical
class TimeSignature implements MusicElement {
	Fraction time
}

@CompileStatic
@Canonical
class Tempo implements MusicElement {
	Fraction baseBeat
	int bpm  // beatsPerMinute
	String text

	Tempo(String tempo = "Allegro", Fraction bbeat = fr(1,4)) {
		text = tempo
		if (TEMPI.containsKey(tempo.toLowerCase()))
			bpm = TEMPI[tempo.toLowerCase()]
		else
			bpm = 120
		baseBeat = bbeat
	}

	static Map<String,Integer> TEMPI = [
		"grave" : 40,
        "largo" : 45,
        "larghetto" : 50,
        "lento" : 55,
        "adagio" : 60,
        "adagietto" : 65,
        "andante" : 70,
        "andantino" : 80,
        "moderato" : 95,
        "allegretto" : 110,
        "allegro" : 120,
        "vivace" : 145,
        "presto" : 180,
        "prestissimo" : 220]
}


@CompileStatic
@Canonical
class CompositionInfo {
	String title
	String composer
	String poet
	ChordsMode chordsMode
}

@CompileStatic
@Canonical
class Lyrics implements MusicElement {
	String text
	
	public Lyrics(String txt) {
		text = txt
	}
}

@CompileStatic
@Canonical
class ControlChange implements MusicElement {
	int index
	int value
}

@CompileStatic
@Canonical
class Repeat implements MusicElement {
	boolean start = true
	int times
	boolean getStop() { !start }
}
