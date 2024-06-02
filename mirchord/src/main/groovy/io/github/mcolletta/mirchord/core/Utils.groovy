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

package io.github.mcolletta.mirchord.core

import static java.lang.Math.*
import javax.sound.midi.*

import com.xenoage.utils.math.Fraction
import static com.xenoage.utils.math.Fraction.fr
import static com.xenoage.utils.math.Fraction._0

import static ChordKind.*

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper


class Utils {

	// it works only for object that implements Serializable
	static def deepcopy(orig) {
	     def bos = new ByteArrayOutputStream()
	     def oos = new ObjectOutputStream(bos)
	     oos.writeObject(orig); oos.flush()
	     def bin = new ByteArrayInputStream(bos.toByteArray())
	     def ois = new ObjectInputStream(bin)
	     return ois.readObject()
	}

	// it works only for object that implements Serializable
	static void saveObjectToBinaryFile(Object obj, String path) {
		try(
		    FileOutputStream fos = new FileOutputStream(new File(path), true)
		    ObjectOutputStream oos = new ObjectOutputStream(fos)
		) {
		    oos.writeObject(obj)
		} catch (Exception ex) {
		    ex.printStackTrace()
		}
	}

	// it works only for object that implements Serializable
	static <T> loadObjectFromBinaryFile(String path) {
		ObjectInputStream  ois = null
		try {
		    def fis = new FileInputStream(path)
		    ois = new ObjectInputStream(fis)
		    return (T) ois.readObject()
		} catch (Exception e) {
		    e.printStackTrace()
		} finally {
		    if(ois != null){
		        ois.close()
		    } 
		}
	}

	static void saveObjectToJsonFile(Object obj, String path) {
		def json = new JsonBuilder(obj).toPrettyString()
		def f = new File(path)
		f.createNewFile()
		f.text = json
	}

	static <T> loadObjectFromJsonFile(String path) {
		return new JsonSlurper().parseText(new File(path).text)
	}

	static float Dot(float[] a, float[] b) {
        float sum = 0
        int n = a.length
        for (int i = 0; i < n; i++) {
            sum += (float)(a[i] * b[i])    
        }
        return sum
	}

	static List<List<Pitch>> GetTimeGrid(Score score, Fraction step) {
		List<List<Pitch>> grid = [].withDefault{[]}
		// fr(1,2).divideBy(fr(1,8)) = 4/1
		for (Part part: score.parts) {
			for (Voice voice: part.voices) {
				ScoreTime time = ScoreTime._t0
				List<Chord> chords = voice.getChords()
				for (Chord chord: chords) {
					def pitches = chord.pitches
					def duration = chord.duration
					int start = 0
					if (time.measure > 0) {
					    start = (int)Math.ceil(fr(time.measure,1).divideBy(step).toFloat())
					}
					start += (int)Math.ceil(time.beat.divideBy(step).toFloat())
					int span = (int)Math.ceil(duration.divideBy(step).toFloat())
					//println "start: $start - span $span"
					for (int i=start; i < start + span; i++) {
						grid[i].addAll(pitches)
					}
					time += duration
				}
			}
		}
		return grid
	}

	static Map<Integer, String> pitch_names = [0: 'C', 1: 'C#/Db', 2: 'D', 3: 'D#/Eb', 4: 'E', 5: 'F',
		                                       6: 'F#/Gb', 7: 'G', 8: 'G#/Ab', 9: 'A', 10: 'A#/Bb', 11: 'B']

	static Map<String, Map<String, Float>> GetLeadSheetChordStatistics(Voice melody, Voice harmony, boolean transpose=true) {

		Map<String, Map<String, Float>> chordStats
		chordStats = [:].withDefault{['C': 0.0f, 'C#/Db': 0.0f, 'D': 0.0f, 'D#/Eb': 0.0f, 'E': 0.0f, 'F': 0.0f,
		                              'F#/Gb': 0.0f, 'G': 0.0f, 'G#/Ab': 0.0f, 'A': 0.0f, 'A#/Bb': 0.0f, 'B': 0.0f]}

		List<MusicElement> mel = []
		int transposition = 0
		def elements = Score.getElementsByTypes(melody.elements, ["KeySignature", "ChordSymbol", "Chord", "Rest"])
		for (MusicElement el: elements) {
			switch (el) {
				case { it.getMusicElementType() == "KeySignature" }:
					if (transpose)
						transposition = Utils.getTonicTransposition((KeySignature)el)
					break
				case { it.getMusicElementType() == "Chord" }:
					Chord note = (Chord)el.copy()
					if (transposition > 0)
						note.pitch.midiValue -= transposition
					mel.add(note)
					break
				case { it.getMusicElementType() == "Rest" }:
					mel.add((Rest)el.copy())
					break
				case { it.getMusicElementType() == "ChordSymbol" }:
					throw new Exception("Not a Lead Sheet")
					break
				default:
					break
			}
		}

		List<MusicElement> har = []
		transposition = 0
		elements = Score.getElementsByTypes(harmony.elements, ["KeySignature", "ChordSymbol", "Chord", "Rest"])
		for (MusicElement el: elements) {
			switch (el) {
				case { it.getMusicElementType() == "KeySignature" }:
					if (transpose)
						transposition = Utils.getTonicTransposition((KeySignature)el)
					break
				case { it.getMusicElementType() == "ChordSymbol" }:
					ChordSymbol chordSymbol = (ChordSymbol)el.copy()
					if (transposition > 0) {
						chordSymbol.root.midiValue -= transposition
						if (chordSymbol.bass != null)
							chordSymbol.bass.midiValue -= transposition
					}
					har.add(chordSymbol)
					break
				case { it.getMusicElementType() == "Chord" }:
					throw new Exception("Not a Lead Sheet")
					break
				case { it.getMusicElementType() == "Rest" }:
					har.add((Rest)el.copy())
					break
				default:
					break
			}
		}

		def htime = [ScoreTime._t0, ScoreTime._t0] // chord sym window
		ScoreTime mtime = ScoreTime._t0
		int j = 0
		for (int i = 0; i < har.size(); i++) {
			MusicElement el = har[i]
			if (el.getMusicElementType() == "Rest") {
				htime[0] = htime[1]
				htime[1] = htime[1] + ((Rest)el).duration
				continue
			} else if (el.getMusicElementType() == "ChordSymbol") {
				ChordSymbol curChordSym = (ChordSymbol)el
				def root_name = pitch_names[curChordSym.root.midiValue % 12]
				def chordsym_name = root_name + "_" + curChordSym.getKindText().toLowerCase()
				if (mtime > htime[1] && mel[j-1].getMusicElementType() == "Chord") {
					def note = ((Chord)mel[j-1])
					def note_name = pitch_names[note.pitch.midiValue % 12]
					chordStats[chordsym_name][note_name] += 1.0f
				}
				htime[0] = htime[1]
				htime[1] = htime[1] + curChordSym.duration
				while(mtime <= htime[1] && j < mel.size()) {
					if (mel[j].getMusicElementType() == "Rest") {
						mtime += ((Rest)mel[j]).duration
					} else if (mel[j].getMusicElementType() == "Chord") {
						def note = ((Chord)mel[j])
						def onset = mtime
						mtime += note.duration
						if (onset >= htime[0] || mtime > htime[0]) {
							def note_name = pitch_names[note.pitch.midiValue % 12]
							chordStats[chordsym_name][note_name] += 1.0f
						}
					} 
					j += 1
				}
			}
		}

		// normalize
		chordStats.each { String chord, Map<String, Float> pitch_histogram ->
			float sum = (float) pitch_histogram.values().sum()
			if (sum > 0) {
				pitch_histogram.each { String k, double v ->
					pitch_histogram[k] = (float) (v/sum)
				}
			}
		}

		return chordStats
	}

	static Map<Class<?>, Class<?>> PRIMITIVE_WRAPPERS = [
											(Void.class): void.class,
											(Boolean.class): boolean.class,
											(Short.class): short.class,
											(Integer.class): int.class,
											(Long.class): long.class,
											(Float.class): float.class,
											(Double.class): double.class,
											(Character.class): char.class,
											(Byte.class): byte.class
										]

	static unboxingWrapper(Class<?> klass) {
		if (PRIMITIVE_WRAPPERS.containsKey(klass)) {
			return PRIMITIVE_WRAPPERS[klass]
		}
		return klass
	}
	
	static void play(Sequence sequence, float bpm=120.0f) {
		Sequencer sequencer
		sequencer = MidiSystem.getSequencer()
		sequencer.open()
		sequencer.setSequence(sequence)
		sequencer.setTempoInBPM(bpm)
		sequencer.start()
		int ticksPerSecond = (int)( sequence.resolution * (sequencer.tempoInBPM / 60.0) )
		double tickSize = 1.0 / ticksPerSecond
		long secsLength = (long)(tickSize * sequence.tickLength)
		long millisToSleep = secsLength * 1000
		Thread.sleep(millisToSleep + 1000)
		sequencer.close()
	}

	static Fraction parseFraction(String ratio) {
		String[] parts =  ratio.split('/')
		if (parts.size() > 1) {
			int num = Integer.parseInt(parts[0])
			int den = Integer.parseInt(parts[1])
			return fr(num, den)
		} else {
			return fr(Integer.parseInt(parts[0]), 1)
		}
		return _0
	}


	static List<String> NOTES = ["C", "C#", "D", "E&", "E", "F", "F#", "G", "G#", "A", "B&", "B"]

	static Map<String, Integer> PITCH_MAP = [C:0, D:2, E:4, F:5, G:7, A:9, B:11]
	static Map<String, Integer> NOTE_NAMES = [C: 1, D: 2, E: 3, F: 4, G: 5, A: 6, B: 7]

	static Map<KeyMode, List<Integer>> MODE_INTERVALS = [
											(KeyMode.MAJOR): [2, 2, 1, 2, 2, 2, 1],
											(KeyMode.MINOR): [2, 1, 2, 2, 1, 2, 2]
										]

	static int getHalfStepsFromDiatonic(Pitch pitch, int diatonicSteps, KeyMode mode) {
		String symbol = pitch.getSymbol()
		int interval = abs(diatonicSteps)
		int start = NOTE_NAMES[symbol] - 1
		List<Integer> scale = MODE_INTERVALS[mode]
		int halfSteps = 0
		for(int i = 0; i < interval; i++) {
			halfSteps += scale[start % scale.size()]
			start++
		}
		if (diatonicSteps < 0)
			halfSteps = -halfSteps
		return halfSteps
	}

	static int getDiatonicPitchesInterval(Pitch from, Pitch to) {
		return NOTE_NAMES[from.getSymbol()] - NOTE_NAMES[to.getSymbol()]
	}
	
//	static def DynamicsToMidi =
//		['fff' : 120, // forte-fortissimo
//		'ff' : 104, // fortissimo
//		'f' : 88, // forte
//		'mf' : 72, // mezzo-forte
//		'mp' : 56, // mezzo-piano
//		'p' : 40, // piano
//		'pp' : 24, // pianissimo
//		'ppp' : 8 // piano-pianissimo
//		]
		
	static Map<String,Float> DynamicsLevels = 
		["sf": 1.00f,
		"fffff": 0.95f,
		"ffff": 0.92f,
		"fff": 0.85f,
		"ff": 0.80f,
		"f": 0.75f,
		"mf": 0.68f,
		"mp": 0.61f,
		"p": 0.55f,
		"pp": 0.49f,
		"ppp": 0.42f,
		"pppp": 0.34f,
		"ppppp": 0.25f]

	static KeyMode ModeFromName(String name) {
		String modeName = name.toLowerCase()
		KeyMode mode //fifths
		switch(modeName) {
			case 'maj':
			case 'major':
				mode = KeyMode.MAJOR
				break
			case 'min':
			case 'minor':
				mode = KeyMode.MINOR
				break
			default:
				throw new Exception("Scale $name is not valid")
				break
		}
		return mode
	}
	
	static int FifthsFromName(String key, String mode) {
		if (mode.size() < 3)
			throw new Exception("Mode $mode is not valid: prove with major or minor")
		String name = key + mode[0..2]
		int keySig // fifths
		switch(name.toLowerCase( ).trim()) {
			case ['c&maj', 'a&min']:
				keySig = -7
				break
			case ['g&maj', 'e&min']:
				keySig = -6
				break
			case ['d&maj', 'b&min']:
				keySig = -5
				break
			case ['a&maj', 'fmin']:
				keySig = -4
				break
			case ['e&maj', 'cmin']:
				keySig = -3
				break
			case ['b&maj', 'gmin']:
				keySig = -2
				break
			case ['fmaj', 'dmin']:
				keySig = -1
				break
			case ['cmaj', 'amin']:
				keySig = 0
				break
			case ['gmaj', 'emin']:
				keySig = 1
				break
			case ['dmaj', 'bmin']:
				keySig = 2
				break
			case ['amaj', 'f#min']:
				keySig = 3
				break
			case ['emaj', 'c#min']:
				keySig = 4
				break
			case ['bmaj', 'g#min']:
				keySig = 5
				break
			case ['f#maj', 'd#min']:
				keySig = 6
				break
			case ['c#maj', 'a#min']:
				keySig = 7
				break
			default:
				throw new Exception("Key not found")
				break
		}
		return keySig
	}
	
	static List<Integer> AlterForKeySignature(int keySig, int pitch, int octave) {
		
		int alteredPitch = pitch
		int alteredOctave = octave
		
		switch(keySig) {
			case { pitch == 11 && (keySig <= -1) }:
				alteredPitch = 10
				break
			case { pitch == 4 && (keySig <= -2) }:
				alteredPitch = 3
				break
			case { pitch == 9 && (keySig <= -3) }:
				alteredPitch = 8
				break
			case { pitch == 2 && (keySig <= -4) }:
				alteredPitch = 1
				break
			case { pitch == 7 && (keySig <= -5) }:
				alteredPitch = 6
				break
			case { pitch == 0 && (keySig <= -6) }:
				alteredPitch = 11
				alteredOctave -= 1
				break
			case { pitch == 5 && (keySig <= -7) }:
				alteredPitch = 4
				break
			case { pitch == 5 && (keySig >= 1) }:
				alteredPitch = 6
				break
			case { pitch == 0 && (keySig >= 2) }:
				alteredPitch = 1
				break
			case { pitch == 7 && (keySig >= 3) }:
				alteredPitch = 8
				break
			case { pitch == 2 && (keySig >= 4) }:
				alteredPitch = 3
				break
			case { pitch == 9 && (keySig >= 5) }:
				alteredPitch = 10
				break
			case { pitch == 4 && (keySig >= 6) }:
				alteredPitch = 5
				break
			case { pitch == 11 && (keySig >= 7) }:
				alteredPitch = 0
				alteredOctave += 1
				break
			default:
				break
		}
		
		return [alteredPitch, alteredOctave]
	}
	
	static Map<List<Integer>, Integer> TonicFromKeySignature = [
				[0, -7]: 11, //scale 0 sig -7 -> Cb maj
				[0, -6]: 6, //scale 0 sig -6 -> Gb maj
				[0, -5]: 1, //scale 0 sig -5 -> Db maj
				[0, -4]: 8, //scale 0 sig -4 -> Ab maj
				[0, -3]: 3, //scale 0 sig -3 -> Eb maj
				[0, -2]: 10, //scale 0 sig -2 -> Bb maj
				[0, -1]: 5, //scale 0 sig -1 -> F maj
				[0, 0]: 0, //scale 0 sig 0 -> C maj
				[0, 1]: 7, //scale 0 sig 1 -> G maj
				[0, 2]: 2, //scale 0 sig 2 -> D maj
				[0, 3]: 9, //scale 0 sig 3 -> A maj
				[0, 4]: 4, //scale 0 sig 4 -> E maj
				[0, 5]: 11, //scale 0 sig 5 -> B maj
				[0, 6]: 6, //scale 0 sig 6 -> F# maj
				[0, 7]: 1, //scale 0 sig 7 -> C# maj
				
				
				[1, -7]: 8, //scale 1 sig -7 -> Ab min
				[1, -6]: 3, //scale 1 sig -6 -> Eb min
				[1, -5]: 10, //scale 1 sig -5 -> Bb min
				[1, -4]: 5, //scale 1 sig -4 -> F min
				[1, -3]: 0, //scale 1 sig -3 -> C min
				[1, -2]: 7, //scale 1 sig -2 -> G min
				[1, -1]: 2, //scale 1 sig -1 -> D min
				[1, 0]: 9, //scale 1 sig 0 -> A min
				[1, 1]: 4, //scale 1 sig 1 -> E min
				[1, 2]: 11, //scale 1 sig 2 -> B min
				[1, 3]: 6, //scale 1 sig 3 -> F# min
				[1, 4]: 1, //scale 1 sig 4 -> C# min
				[1, 5]: 8, //scale 1 sig 5 -> G# min
				[1, 6]: 3, //scale 1 sig 6 -> D# min
				[1, 7]: 10, //scale 1 sig 7 -> A# min
		]

	static int getTonicTransposition(KeySignature keysig) {
		// normalize to Cmaj or Amin
		// moving on circle of fifths
		// a fifth = 7 semitones
		// minor scales 3 semitones below major scales
		int tr = 0
		if (keysig.fifths >= 0)
			tr = (keysig.fifths * 7) % 12
		else
			tr = ((12 + keysig.fifths) * 7) % 12
		int adjust_minor = (keysig.mode == KeyMode.MINOR) ? -3 : 0
		tr += adjust_minor
		tr = tr >= 0 ? tr : 12 + tr
		return tr
	}

	static String getPitchLetterFromSymbol(String symbol, KeySignature keysig) {
		int scaleDegree = getScaleDegreeFromSymbol(symbol)
		int tonic = getTonicOrdinalFromKeySignature(keysig)
		int mapped = (tonic + (scaleDegree - 1))
		if (mapped > 7)
			mapped %= 7
		String letter = CmajorDegreeToLetter[mapped]
		return letter
	}

	static Map<Integer,String> CmajorDegreeToLetter = [
		1: 'C', 2: 'D', 3: 'E', 4: 'F', 5: 'G', 6: 'A', 7: 'B'
	]

	static int getScaleDegreeFromSymbol(String symbol) {
		int result = 1;
		switch(symbol) {
			case "I":
			case "do":
				result = 1
				break
			case "II":
			case "re":
				result = 2
				break
			case "III":
			case "mi":
				result = 3
				break
			case "IV":
			case "fa":
				result = 4
				break
			case "V":
			case "so":
			case "sol":
				result = 5
				break
			case "VI":
			case "la":
				result = 6
				break
			case "VII":
			case "si":
			case "ti":
				result = 7
				break
			default:
				break
		}
		return result
	}

	static int getTonicOrdinalFromKeySignature(KeySignature keysig) {
		int result = 1;
		if (keysig.mode == KeyMode.MAJOR) {
			switch(keysig.fifths) {
				case -7:
				case 0:
				case 7:
					result = 1 // C
					break
				case -6:
				case 1:
					result = 5 // G
					break
				case -5:
				case 2:
					result = 2 // D
					break
				case -4:
				case 3:
					result = 6 // A	
					break
				case -3:
				case 4:
					result = 3 // E	
					break
				case -2:
				case 5:
					result = 7 // B	
					break
				case -1:
				case 6:
					result = 4 // F	
					break
				default:
					break
			}
		}
		else if (keysig.mode == KeyMode.MINOR) {
			switch(keysig.fifths) {
				case -7:
				case 0:
				case 7:
					result = 6 // A	
					break
				case -6:
				case 1:
					result = 3 // E	
					break
				case -5:
				case 2:
					result = 7 // B	
					break
				case -4:
				case 3:
					result = 4 // F	
					break
				case -3:
				case 4:
					result = 1 // C	
					break
				case -2:
				case 5:
					result = 5 // G	
					break
				case -1:
				case 6:
					result = 2 // D	
					break
				default:
					break
			}
		}
		return result
	}
	
	/*
	static int P1 = 0
	static int m2 = 1
	static int M2 = 2
	static int m3 = 3
	static int M3 = 4
	static int P4 = 5
	static int P5 = 7
	static int m6 = 8
	static int M6 = 9
	static int m7 = 10
	static int M7 = 11
	static int P8 = 12
	
				
	}
	
	*/
	
	static List<Integer> getIntervalsFromChordKind(ChordKind kind) {
		List<Integer> result
		switch(kind) {
			// TRIADS
			case MAJOR: 
				result = [4, 7]				// major (major third, perfect fifth)							1 - 3 - 5
				break
			case MINOR: 
				result = [3, 7]				// minor (minor third, perfect fifth)							1 - &3 - 5
				break
			case AUGMENTED: 
				result = [4, 8]				// augmented (major third, augmented fifth)						1 - 3 - #5
				break
			case DIMINISHED: 
				result = [3, 6]				// diminished (minor third, diminished fifth)					1 - &3 - &5	
				break
			// SEVENTHS
			case DOMINANT: 
				result = [4, 7, 10]			// dominant (major triad, minor seventh)						1 - 3 - 5 - &7
				break
			case MAJOR_SEVENTH: 
				result = [4, 7, 11]			// major-seventh (major triad, major seventh)					1 - 3 - 5 - 7
				break
			case MINOR_SEVENTH: 
				result = [3, 7, 10]			// minor-seventh (minor triad, minor seventh)					1 - &3 - 5 - &7
				break
			case DIMINISHED_SEVENTH: 
				result = [3, 6, 9]			// diminished-seventh (diminished triad, diminished seventh)	1 - &3 - &5 - &&7
				break
			case AUGMENTED_SEVENTH: 
				result = [4, 8, 10]			// augmented-seventh (augmented triad, minor seventh)			1 - 3 - #5 - &7
				break
			case HALF_DIMINISHED: 
				result = [3, 6, 10]			// half-diminished (diminished triad, minor seventh)			1 - &3 - &5 - &7
				break
			case MAJOR_MINOR: 
				result = [3, 7, 11]			// major-minor (minor triad, major seventh)						1 - &3 - 5 - 7
				break
			// SIXTHS
			case MAJOR_SIXTH: 
				result = [4, 7, 9]			// major-sixth (major triad, added sixth)						1 - 3 - 5 - 6
				break
			case MINOR_SIXTH: 
				result = [3, 7, 9]			// minor-sixth (minor triad, added sixth)						1 - &3 - 5 - 6
				break
			// NINTHS
			case DOMINANT_NINTH: 
				result = [4, 7, 10, 14]		// dominant-ninth (dominant-seventh, major ninth)				1 - 3 - 5 - &7 - 9
				break
			case MAJOR_NINTH: 
				result = [4, 7, 11, 14]		// major-ninth (major-seventh, major ninth)						1 - 3 - 5 - 7 - 9
				break
			case MINOR_NINTH: 
				result = [3, 7, 10, 14]		// minor-ninth (minor-seventh, major ninth)						1 - &3 - 5 - &7 - 9
				break
			// 11ths
			case DOMINANT_11TH: 
				result = [4, 7, 10, 14, 17]		// dominant-11th (dominant-ninth, perfect 11th)				1 - 3 - 5 - &7 - 9 - 11
				break
			case MAJOR_11TH: 
				result = [4, 7, 11, 14, 17]		// major-11th (major-ninth, perfect 11th)					1 - 3 - 5 - 7 - 9 - 11
				break
			case MINOR_11TH: 
				result = [3, 7, 10, 14, 17]		// minor-11th (minor-ninth, perfect 11th)					1 - &3 - 5 - &7 - 9 - 11
				break
			// 13ths
			case DOMINANT_13TH: 
				result = [4, 7, 10, 14, 17, 21]	// dominant-13th (dominant-11th, major 13th)			1 - 3 - 5 - &7 - 9 - (11) - 13
				break
			case MAJOR_13TH: 
				result = [4, 7, 11, 14, 17, 21]	// major-13th (major-11th, major 13th) 					1 - 3 - 5 - 7 - 9 - 11 - 13
				break
			case MINOR_13TH: 
				result = [3, 7, 10, 14, 17, 21]	// minor-13th (minor-11th, major 13th)					1 - &3 - 5 - &7 - 9 - 11 - 13
				break
			// SUSPENDED
			case SUSPENDED_SECOND: 
				result = [2, 7]			// suspended-second (major second, perfect fifth)			1 - 2 - 5
				break
			case SUSPENDED_FOURTH: 
				result = [5, 7]			// suspended-fourth (perfect fourth, perfect fifth)			1 - 4 - 5
				break
			case NEAPOLITAN:
			case ITALIAN:
			case FRENCH:
			case GERMAN:
			case PEDAL:
				break
			case POWER: 
				result = [7]			// (perfect fifth)												1 - 5
				break
			case TRISTAN:
			case OTHER:
				break
			case NONE: 
				result = []
				break
			default:
				break
		}
		return result
	}
	
	static ChordKind getChordSymboKind(String desc) {
		ChordKind result
		switch(desc) {
			// TRIADS
			case ['', 'maj', 'major']: 
				result = MAJOR
				break
			case ['m', 'min', 'minor']:
				result = MINOR
				break
			case ['+', 'aug', 'augmented']:
				result = AUGMENTED
				break
			case ['°', 'dim', 'diminished']:
				result = DIMINISHED
				break
			// SEVENTHS
			case ['7', 'dom7', 'dominant', 'dominant-seventh']:
				result = DOMINANT
				break
			case ['M7', 'maj7', 'major-seventh']:
				result = MAJOR_SEVENTH
				break
			case ['m7', 'min7', 'minor-seventh']:
				result = MINOR_SEVENTH
				break
			case ['°7', 'dim7', 'diminished-seventh']:
				result = DIMINISHED_SEVENTH
				break
			case ['+7', 'aug7', 'augmented-seventh']:
				result = AUGMENTED_SEVENTH
				break
			case ['07', 'half-diminished']:
				result = HALF_DIMINISHED
				break
			case ['mM7', 'minMaj7', 'major-minor', 'minor-major']:
				result = MAJOR_MINOR
				break
			// SIXTHS
			case ['6', 'major-sixth']:
				result = MAJOR_SIXTH
				break
			case ['m6', 'minor-sixth']:
				result = MINOR_SIXTH
				break
			// NINTHS
			case ['9', 'dominant-ninth']:
				result = DOMINANT_NINTH
				break
			case ['M9', 'maj9', 'major-ninth']:
				result = MAJOR_NINTH
				break
			case ['m9', 'minor-ninth']:
				result = MINOR_NINTH
				break
			// 11ths
			case ['11', 'dominant-11th']:
				result = DOMINANT_11TH
				break
			case ['M11', 'maj11', 'major-11th']:
				result = MAJOR_11TH
				break
			case ['m11', 'min11', 'minor-11th']:
				result = MINOR_11TH
				break
			// 13ths:
			case ['13', 'dominant-13th']:
				result = DOMINANT_13TH
				break
			case ['M13', 'maj13', 'major-13th']:
				result = MAJOR_13TH
				break
			case ['m13', 'min13', 'minor-13th']:
				result = MINOR_13TH
				break
			// SUSPENDED
			case ['sus2', 'suspended-second']:
				result = SUSPENDED_SECOND
				break
			case ['sus', 'sus4', 'suspended-fourth']:
				result = SUSPENDED_FOURTH
				break
			case ['power']:
				result = POWER
				break
			default:
				// TODO: UnSupportedChordException
				break
		}
		return result
	}
	
	static List<Float> quantizedDurations = (List<Float>)[1.0f,
														 0.75f,
														 0.5f,
														 0.375f,
														 0.25f,
														 0.1875f,
														 0.125f,
														 0.09375f,
														 0.0625f,
														 0.046875f,
														 0.03125f,
														 0.0234375f,
														 0.015625,
														 0.01171875f,
														 0.0078125f]
	
	static float quantizeDuration(float duration) {
		return nearest(quantizedDurations, duration)
	}
	
	static float nearest(List<Float> list, float n) {
		return list.min{(it - n).abs()}
	}
		
	static Map<String, Boolean> getDurationType(float duration) {
		boolean dotted = false
		String type = ''
		switch(duration) {
			case 1.0f:
				type = 'whole'
			break
			case 0.75f:
				type = 'half'
				dotted = true
			break
			case 0.5f:
				type = 'half'
			break
			case 0.375f:
				type = 'quarter'
				dotted = true
			break
			case 0.25f:
				type = 'quarter'
			break
			case 0.1875f:
				type = 'eighth'
				dotted = true
			break
			case 0.125f:
				type = 'eighth'
			break
			case 0.09375f:
				type = '16th'
				dotted = true
			break
			case 0.0625f:
				type = '16th'
			break
			case 0.046875f:
				type = '32th'
				dotted = true
			break
			case 0.03125f:
				type = '32th'
			break
			case 0.0234375f:
				type = '64th'
				dotted = true
			break
			case 0.015625:
				type = '64th'
			break
			case 0.01171875f:
				type = '128th'
				dotted = true
			case 0.0078125f:
				type = '128th'
			default:
			break
		}
		return [type: type, dotted: dotted] as Map<String, Boolean>
	}
	
	static Map<String, String> chordkind_lily = [
		'MAJOR': '5',
		'MINOR': 'm5',
		'AUGMENTED': 'aug5',
		'DIMINISHED': 'dim5',
		'DOMINANT': '7',
		'DOMINANT_SEVENTH': '7',
		'MAJOR_SEVENTH': 'maj7',
		'MINOR_SEVENTH': 'm7',
		'DIMINISHED_SEVENTH': 'dim7',
		'AUGMENTED_SEVENTH': 'aug7',
		'HALF_DIMINISHED': 'dim5m7',
		'MAJOR_MINOR': 'maj7m5',
		'MAJOR_SIXTH': '6',
		'MINOR_SIXTH': 'm6',
		'DOMINANT_NINTH': '9',
		'MAJOR_NINTH': 'maj9',
		'MINOR_NINTH': 'm9',
		'DOMINANT_11TH': '11',
		'MAJOR_11TH': 'maj11',
		'MINOR_11TH': 'm11',
		'DOMINANT_13TH': '13.11',
		'MAJOR_13TH': 'maj13.11',
		'MINOR_13TH': 'm13',
		'SUSPENDED_SECOND': 'sus2',
		'SUSPENDED_FOURTH': 'sus4',
		'POWER': '5^3',
		'OTHER': '1',
		'NONE': ''
	]


	static Fraction f1 = fr(4, 4)
	static Fraction f3 = fr(3, 4)
	static Fraction f2 = fr(2, 4)
	static Fraction f4 = fr(1, 4)
	static Fraction f8 = fr(1, 8)
	static Fraction f16 = fr(1, 16)
	static Fraction f32 = fr(1, 32)
	static Fraction f64 = fr(1, 64)
	static Fraction f128 = fr(1, 128)
	static Fraction f256 = fr(1, 256)

	static Fraction f8dot = f8.add(f16)
	static Fraction f8dotdot = f8dot.add(f32)
	static Fraction f4dot = f4.add(f8)
	static Fraction f4dotdot = f4dot.add(f16)
	static Fraction f2dot = f2.add(f4)
	static Fraction f2dotdot = f2dot.add(f8)

	static Fraction f6 = fr(1, 6)
	static Fraction f12 = fr(1, 12)

	static Map<Float,Fraction> allowedDurations = [
		0.00390625f: f256,
		0.0078125f: f128,
		0.015625f: f64,
		0.0625f: f16,
		0.083f: f12, // triplet?
		0.125f: f8,
		0.17f: f6, // triplet?
		0.1875f: f8dot,
		0.25f: f4,
		0.375f: f4dot,
		0.5f: f2,
		0.75f: f3,
		1.0f: f1
	]

	static Fraction getDurationFromDecimal(float num) {
		float key = nearest((List<Float>)allowedDurations.keySet(), num)
		return allowedDurations[key]
	}
	
}

