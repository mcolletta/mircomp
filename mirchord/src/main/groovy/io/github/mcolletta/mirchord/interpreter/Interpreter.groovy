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

package io.github.mcolletta.mirchord.interpreter


import io.github.mcolletta.mirchord.core.*
import static io.github.mcolletta.mirchord.core.Utils.*

import java.util.stream.Collectors

import com.xenoage.utils.math.Fraction
import static com.xenoage.utils.math.Fraction.fr
import static com.xenoage.utils.math.Fraction._0

import com.googlecode.lingwah.Document
import com.googlecode.lingwah.ParseContext
import com.googlecode.lingwah.ParseResults
import com.googlecode.lingwah.StringDocument


class MirChordInterpreter {
	
	MirChordGrammar PARSER
	MirChordProcessor processor
	
	MirChordInterpreter(List ext=[]) {
		PARSER = MirChordGrammar.INSTANCE
		List extensions = []
		extensions.add(new MirchordAddon())
		if (ext != null && ext.size() > 0)
			extensions.addAll(ext)
		processor = new MirChordProcessor(extensions)
	}
	
	Score evaluate(String source) {
		String cleanSource = source.trim().replaceAll(/;[^\n]*/, "")   // .replaceAll("(?s)/\\*.*?\\*/", "")
		Document doc= new StringDocument(cleanSource)
		ParseContext ctx= new ParseContext(doc)
		ParseResults parseResults= ctx.getParseResults(PARSER.score, 0)
		if (!parseResults.success())
				throw parseResults.getError()
		Score score = processor.process(parseResults)
		return score
	}
	
	public static void main(String[] args) {
		String source = new File("compositions/mirchords/test1.mirchord").text
		MirChordInterpreter interpreter = new MirChordInterpreter()
		Score score = interpreter.evaluate(source)
		println "SCORE PARSED: " + score
	}

}

class MirchordAddon {

	@MirChord 
	public List<MusicElement> transpose(int halfSteps, List<MusicElement> phrase) {
		List<MusicElement> newPhrase = phrase.stream()
                .filter({ el -> el.isCopyable() })
                .map({ el -> el.copy() })
                .collect(Collectors.toList()) as List<MusicElement>
		for(MusicElement el : newPhrase) {
			if (el.getMusicElementType() == "Chord") {
				Chord chord = (Chord)el
				for(Pitch pitch : chord.getPitches()) {
					pitch.setMidiValue(pitch.getMidiValue() + halfSteps)
				}
			}
		}
		return newPhrase
	}

	@MirChord
	public List<MusicElement> transposeDiatonic(int diatonicSteps, String modeText, List<MusicElement> phrase) {
		KeyMode mode = ModeFromName(modeText)
		List<MusicElement> newPhrase = phrase.stream()
                .filter({ el -> el.isCopyable() })
                .map({ el -> el.copy() })
                .collect(Collectors.toList()) as List<MusicElement>
		for(MusicElement el : newPhrase) {
			if (el.getMusicElementType() == "Chord") {
				Chord chord = (Chord)el
				int halfSteps = getHalfStepsFromDiatonic(chord.getPitch(), diatonicSteps, mode)
				for(Pitch pitch : chord.getPitches()) {
					pitch.setMidiValue(pitch.getMidiValue() + halfSteps)
				}
			}
		}
		return newPhrase
	}

	@MirChord
	public List<MusicElement> invert(List<MusicElement> phrase) {
		List<MusicElement> newPhrase = phrase.stream()
                .filter({ el -> el.isCopyable() })
                .map({ el -> el.copy() })
                .collect(Collectors.toList()) as List<MusicElement>
		List<MusicElement> chords = newPhrase.findAll { it.getMusicElementType() == "Chord" }
		Chord mirror = (Chord)chords[0]
		for(MusicElement el : newPhrase) {
			if (el.getMusicElementType() == "Chord") {
				Chord chord = (Chord)el
				int interval = mirror.getPitch().getMidiValue() - chord.getPitch().getMidiValue()
				chord.getPitch().setMidiValue(mirror.getPitch().getMidiValue() + interval)
			}
		}
		return newPhrase
	}

	@MirChord
	public List<MusicElement> invertDiatonic(String modeText, List<MusicElement> phrase) {
		KeyMode mode = ModeFromName(modeText)
		List<MusicElement> newPhrase = phrase.stream()
                .filter({ el -> el.isCopyable() })
                .map({ el -> el.copy() })
                .collect(Collectors.toList()) as List<MusicElement>
		List<MusicElement> chords = newPhrase.findAll { it.getMusicElementType() == "Chord" }
		Chord mirror = (Chord)chords[0]
		for(MusicElement el : newPhrase) {
			if (el.getMusicElementType() == "Chord") {
				Chord chord = (Chord)el
				int octaves = (int)((mirror.getPitch().getMidiValue() - chord.getPitch().getMidiValue()) / 12) * 12
				int interval = getDiatonicPitchesInterval(mirror.getPitch(), chord.getPitch())
				int halfSteps = getHalfStepsFromDiatonic(chord.getPitch(), interval, mode)
				chord.getPitch().midiValue = mirror.getPitch().midiValue + halfSteps + octaves
			}
		}
		return newPhrase
	}

	@MirChord 
	public List<MusicElement> retrograde(List<MusicElement> phrase) {
		List<MusicElement> newPhrase = phrase.stream()
                .filter({ el -> el.isCopyable() })
                .map({ el -> el.copy() })
                .collect(Collectors.toList()) as List<MusicElement>
		return newPhrase.reverse()
	}

	@MirChord 
	public List<MusicElement> augment(Fraction ratio, List<MusicElement> phrase) {
		List<MusicElement> newPhrase = phrase.stream()
                .filter({ el -> el.isCopyable() })
                .map({ el -> el.copy() })
                .collect(Collectors.toList()) as List<MusicElement>
		for(MusicElement el : newPhrase) {
			if (el.getMusicElementType() == "Chord") {
				Chord chord = (Chord)el
				chord.duration = chord.duration.mult(ratio)
			}
		}
		return newPhrase
	}

	@MirChord 
	public List<MusicElement> diminuition(Fraction ratio, List<MusicElement> phrase) {
		List<MusicElement> newPhrase = phrase.stream()
                .filter({ el -> el.isCopyable() })
                .map({ el -> el.copy() })
                .collect(Collectors.toList()) as List<MusicElement>
		for(MusicElement el : newPhrase) {
			if (el.getMusicElementType() == "Chord") {
				Chord chord = (Chord)el
				chord.duration = chord.duration.divideBy(ratio)
			}
		}
		return newPhrase
	}

	@MirChord 
	public List<MusicElement> zip(List<MusicElement> phrase, List<MusicElement> pattern) {
		List<MusicElement> newPhrase = phrase.stream()
                .filter({ el -> el.isCopyable() })
                .map({ el -> el.copy() })
                .collect(Collectors.toList()) as List<MusicElement>
		List<MusicElement> rhythm = pattern.findAll { it.getMusicElementType() == "Chord" }
		int i = 0;
		for(MusicElement el : newPhrase) {
			if (el.getMusicElementType() == "Chord") {
				Chord chord = (Chord)el
				chord.duration = (rhythm.size() < i) ? ((Chord)rhythm[i]).duration : ((Chord)rhythm[i % rhythm.size()]).duration
				i++
			}
		}
		return newPhrase
	}

}