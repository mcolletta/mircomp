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

import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer

import com.xenoage.utils.math.Fraction
import static com.xenoage.utils.math.Fraction.fr
import static com.xenoage.utils.math.Fraction._0

import com.googlecode.lingwah.*
import com.googlecode.lingwah.util.*
import com.googlecode.lingwah.annotations.Processes

import java.util.stream.Collectors

import java.lang.reflect.Method
import java.lang.reflect.Parameter
import java.lang.reflect.Type
import java.lang.reflect.Modifier

import java.lang.annotation.Target
import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy


@Target([ElementType.METHOD])
@Retention(RetentionPolicy.RUNTIME)
@interface MirChord {
    String info() default "MirChord method used by the interpreter"
}

enum OctaveMode {
		ABSOLUTE,
		RELATIVE
	}

enum DurationMode {
		SCOPED,
		STICKY
	}


@Processes(MirChordGrammar.class)
class MirChordProcessor extends AbstractProcessor {

	boolean DEBUG = false

	int DEFAULT_OCTAVE = 4  // C4 is middle C in scientific pitch notation
	Fraction DEFAULT_DURATION = fr(1,4)
	KeySignature DEFAULT_KEYSIGNATURE = new KeySignature()

	Score score
	int currentPart
	Map<Integer, Integer> currentVoice = [:]
    Map<Integer, Map<Integer, Stack>> environments = [:]
    Map<String, Object> symbolsTable = [:]

	Map<String, String> commands_abbr = [
		"info": "scoreInfo",
		"name": "partName",
		"rel": "relative", 
		"def": "define",
		"p": "part",
		"v": "voice",
		"time": "timeSignature",
		"key": "keySignature",
		"i": "instrument",
		"instr": "instrument",
		"tp": "tuplet",
		"cc": "controlChange",
		"call": "callSymbol", 
		"expand": "callSymbol",
		"*": "copyTimes",
		"ch": "chordSymElement"
		]
	
	Map<String, Map<String, Object>> extMethods = [:]
    
    static final MirChordGrammar grammar= MirChordGrammar.INSTANCE
    
    MirChordProcessor(List extensions=[]) {
    	super()
    	processExtMethods(this)
		extensions.each { ext ->
    		processExtMethods(ext)
    	}
    }
	
	void printChildren(name, children) {
		println "--------------------"
		println "MATCH: $name"
		children.each { Match m ->
			println "____________________"
			println "child: ${m.getText()}"
			println "parser: ${m.parser}"
			println getResult(m)
			println "____________________"
		}
		println "--------------------"
	}
	

	private void processExtMethods(Object xobj) {
		Method[] methods = xobj.getClass().getDeclaredMethods()
		for(Method method : methods) {
			// println method.getName() + " " + method.isSynthetic()+ " " + method.getModifiers() + " " + method.getAnnotation(MirChord)
			if (!method.isSynthetic() && (method.getModifiers() == Modifier.PUBLIC) 
            		&& (method.getAnnotation(MirChord) != null)) {
				Map<String, Object> map = [:]
				map.put("method", method)
				map.put("object", xobj)
				if (!extMethods.containsKey(method.getName())) {
					extMethods.put(method.getName(), map)
				} else {
					if (xobj != extMethods[method.getName()]["object"])
						throw new Exception("The Mirchord method " + method.getName() + " is already in use")
				}

				if (DEBUG) {
					// standard case
					// es. keySignature(class java.lang.String,class java.lang.String)
					// keySignature(int,class java.lang.String)
					List<String> paramsType = []
					for(Parameter param : method.getParameters()) {
		                paramsType.add("" + unboxingWrapper(param.getType()))
		            }
					String methodSignature = "" + method.getReturnType() + " " + method.getName() + "(" + String.join(",", paramsType) + ")"
					println "processExtMethods: methodSignature= " + methodSignature
					// special case of method with generic List as param
					// ex: tuplet(interface java.util.List)
					if (method.getParameters().size() == 1) {
						Type t = method.getParameters()[0].getType()
						if (Collection.class.isAssignableFrom(t))
							println "processExtMethods: Collection as parameter for " + method.getName()
					}
				}
			}
		}
	}

	private int getVoice() {
		return currentVoice[currentPart]
	}

	// Scopes

	private Stack getEnvironment() {
		if (!environments.containsKey(currentPart))
			environments.put(currentPart,[:])
		if (!environments[currentPart].containsKey(getVoice()))
			environments[currentPart].put(getVoice(), new Stack())
		return environments[currentPart][getVoice()]
	}
	
	private Map getScope() {
		ensureScope()
		return (Map)getEnvironment().peek()
	}

	private void ensureScope() {
		Stack env = getEnvironment()
		if (env.size() < 1)
			env.add(['octave': DEFAULT_OCTAVE, 'symbol': null,
				     'keySignature': DEFAULT_KEYSIGNATURE,
				     'duration': DEFAULT_DURATION])
	}
	
	private Object getVarFromScopes(name) {
		Stack<Map> env = getEnvironment()
		Object obj = null
		if (env != null && env.size() > 0) {
			for(Map sc : env[-1..0]) {
				if (sc.containsKey(name)) {
					obj = sc[name]
					break
				}
			}
		}
		return obj
	}
	
	private void setVarFromScopes(String name, Object value) {
		Stack<Map> env = getEnvironment()
		for(Map sc : env[-1..0]) {
			if (sc.containsKey(name)) {
				sc[name] = value
				break
			}
		}
	}

	// end Scopes

	// private Map deepCopyElementsMap(Map scope) {
	// 	Map newScope = [:]
	// 	for (Map.Entry<K, V> entry: scope) {
	// 		newScope[entry.key] = entry.value.copy()
	// 	}
	// }

	private void addToScore(MusicElement element) {
		score.parts[currentPart].voices[getVoice()].elements << element
		if (DEBUG)
			println "addToScore $element  in  $currentPart  ${getVoice()}"
	}

	private void updateCurrentInstrument(Instrument instrument) {
		Map scope = getScope()
		scope["instrument"] = instrument
	}

	private Instrument getPercussionInstrument() {
		return (Instrument)getScope()["instrument"]
	}

	private void stem(String val) {
		Map scope = getScope()
		StemDirection stemDir = StemDirection.valueOf(val.toUpperCase())
		scope['stem'] = stemDir
	}

	// COMMANDS

	@MirChord
	void partName(String name) {
		score.parts[currentPart].setName(name)
	}

	@MirChord 
	void setCurrentVoice(int i) {
		if (DEBUG)
			println "SETTING VOICE " + i
		Part curPart = score.parts[currentPart]
		if (curPart.voices.size() <= i) {
			if (i - curPart.voices.size() > 1)
				throw new Exception("Voice expected <= ${curPart.voices.size()+1}, found ${i+1}")
			curPart.voices.add(new Voice())
			curPart.voices[i].elements = []
			currentVoice.put(currentPart, i)
		}
		currentVoice[currentPart] = i
	}
	
	@MirChord
	void setCurrentPart(int i) {
		if (DEBUG)
			println "SETTING PART " + i
		if (score.parts.size() <= i) {
			if (i - score.parts.size() > 1)
				throw new Exception("Part expected <= ${score.parts.size()+1}, found ${i+1}")
			score.parts.add(new Part("Part ${i+1}"))
		}
		currentPart = i
	}
	
	@MirChord
	Instrument unpitched(String name, String displayPitch, int octave) {
		Instrument instr = new Instrument(name, true)
		String letter = displayPitch[0]
		int alteration = 0
		if (displayPitch.length() > 1) {
			for(String alt : displayPitch[1..-1]) {
				if (alt == "#")
					alteration += 1
				else if (alt == "&")
					alteration -= 1

			}
		}
		// update scope
		Map scope = getScope()
		Pitch pitch = new Pitch(letter, octave, alteration)
		instr.setDisplayPitch(pitch)
		updateCurrentInstrument(instr)
		return instr
	}
	
	@MirChord
	Instrument instrument(Integer program) {
		new Instrument(program)
	}
	
	@MirChord
	Instrument instrument(String name) {
		new Instrument(name)			
	}
	
	@MirChord
	Clef clef(String name) {
		ClefType clefType = ClefType.valueOf(name.toUpperCase())
		new Clef(clefType)
	}
	
	@MirChord
	void label(name) {
		Map scope = getScope()
		scope['label'] = name
	}
	
	@MirChord
	Lyrics lyrics(String text) {
		return new Lyrics(text)
	}
	
	@MirChord
	CompositionInfo scoreInfo(List<Map> args) {
		CompositionInfo info =  new CompositionInfo()
		for(Map map : args) {
			if (map.containsKey('title'))
				info.title = (String)map['title']
			if (map.containsKey('composer'))
				info.composer = (String)map['composer']
			if (map.containsKey('poet'))
				info.poet = (String)map['poet']
			if (map.containsKey('chordsMode'))
				info.chordsMode = (ChordsMode)map['chordsMode']
		}
		return info
	}
	
	@MirChord
	Map<String, String> title(String val) {
		return ['title':val]
	}
	
	@MirChord
	Map<String, String> composer(String val) {
		return ['composer':val]
	}
	
	@MirChord
	Map<String, String> poet(String val) {
		return ['poet':val]
	}
	
	@MirChord
	Map<String, ChordsMode> chordsMode(String val) {
		ChordsMode mode = ChordsMode.valueOf(val.toUpperCase())
		return ['chordsMode': mode]
	}
	
	@MirChord
	void define(String id, List<MusicElement> elements) {
		symbolsTable.put(id, elements)
	}
	
	@MirChord
	Tempo tempo(String tmp) {
		return new Tempo(tmp)
	}
	
	@MirChord
	TimeSignature timeSignature(int numerator, int denominator) {
		Fraction timeSig = fr(numerator, denominator)
		new TimeSignature(timeSig)
	}
	
	@MirChord	
	KeySignature keySignature(String key, String mode) {
		KeySignature keySig = new KeySignature(key, mode)
		Map scope = getScope()
		scope.put('keySignature', keySig)
		return keySig
	}

	@MirChord	
	KeySignature keySignature(int fifths, String mode) {
		KeySignature keySig = new KeySignature(fifths, mode)
		Map scope = getScope()
		scope.put('keySignature', keySig)
		return keySig
	}
	
	@MirChord
	ControlChange controlChange(int index, int value) {
		new ControlChange(index, value)
	}
	
	@MirChord
	List<MusicElement> copyTimes(int n, List<MusicElement> elements) {
		List<MusicElement> rep = []
		for(int i=0; i<n; i++) {
			for(MusicElement element : elements) {
				if (element.isCopyable())
					rep.add(element.copy())
			}
		}
		return rep
	}
	
	@MirChord
	List<MusicElement> callSymbol(List<MusicElement> elements) {
		return elements
	}
	
	@MirChord
	List<Chord> tuplet(List args) {
		Fraction r = (Fraction)args[0]
		def tp = new Tuplet(r)
		List<Chord> chords = (List<Chord>)args[1..-1]
		Fraction ratio = fr(r.denominator, r.numerator)
		Fraction dur = chords[0].duration
		for(Chord c : chords) {
			if (c.duration != dur)
				throw new Exception("The chords of a tuplet must have equal duration")
			Fraction actualDuration = c.duration.mult(ratio)
			c.duration = actualDuration
			c.tuplet = tp
			tp.chords << c
		}
		return chords
	}

	@MirChord
	void octaveMode(String val) {
		OctaveMode mode = OctaveMode.valueOf(val.toUpperCase())
		Map scope = getScope()
		scope['octaveMode'] = mode
	}

	@MirChord
	void durationMode(String val) {
		DurationMode mode = DurationMode.valueOf(val.toUpperCase())
		Map scope = getScope()
		scope['durationMode'] = mode
	}

	// Former Addon

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

	@MirChord
	public List<MusicElement> chordSymElement(int pos, ChordSymbol sym, Chord note) {
		assert pos >= 1
		assert note.unpitched
		var chord = sym.getChord()
		var pitches = chord.pitches;
		int len = pitches.size()
		int idx = Math.min(pos-1, len-1)
		note.unpitched = false
		note.setPitch(pitches[idx])
		return [note]
	}

	// END COMMANDS

	// VISITORS METHODS
	      
    void completeSexpr(Match match) {
		List<Match> children = match.children
		//printChildren(match.getText(), children)
		String cmd = ""
		List params = []

		cmd = match.findMatchByType(grammar.command).getText()
		List<Match> params_matches = match.findAllMatchByType(grammar.param)
		for(Match pm : params_matches) {
			def res = getResult(pm)
			if (res != null)
				params << res
		}

		if (commands_abbr.containsKey(cmd))
			cmd = commands_abbr[cmd]
		
		if (extMethods.containsKey(cmd)) {
			if (DEBUG)
				println "calling $cmd with $params"
			Method meth = (Method)extMethods[cmd]["method"]
			def obj = extMethods[cmd]["object"]
			if (params.size() == 1 && params[0] instanceof Collection<?>)
				params = params[0] as List // invert, retrograde
			def res = obj.invokeMethod(cmd, params)
			if (res != null)
				putResult(res)
		} else
			throw new Exception("not found command named $cmd")
    }

    void completeStem(Match match) {
		Match child = match.getFirstChild()
		String dir = "AUTO"
		if (child.parser == grammar.stemUp)
			dir = "UP"
		if (child.parser == grammar.stemDown)
			dir = "DOWN"
		stem(dir)
	}
	
	static int getAlterationFromAccidentals(List<ACCIDENTALS> accidentals) {
		int alteration = 0
		if (accidentals != null) {
			for(ACCIDENTALS alter : accidentals) {
				switch(alter) {
					case ACCIDENTALS.FLAT:
						alteration -= 1
					break
					case ACCIDENTALS.SHARP:
						alteration += 1
					break
					case ACCIDENTALS.NATURAL:
						alteration = 0
					break
					default:
					break
				}
			}
		}
		return alteration
	}
	
    void completeSharp(Match match) {
        putResult(ACCIDENTALS.SHARP)
    }
    
    void completeFlat(Match match) {
        putResult(ACCIDENTALS.FLAT)
    }
    
    void completeNatural(Match match) {
        putResult(ACCIDENTALS.NATURAL)
    }
    
    void completeAccidental(Match match) {
        putResult(getResult(match.children[0]))
    }
    
    void completeAccidentals(Match match) {
        List<Match> children = match.children
        List<ACCIDENTALS> list = []
        for(Match child : children) {
			list << (ACCIDENTALS)getResult(child)
        }
        putResult(list)
    }
    
    void completeOctaveUp(Match match) {
        putResult(1)
    }
    
    void completeOctaveDown(Match match) {
        putResult(-1)
    }
    
    void completeOctaveModifier(Match match) {
        putResult(getResult(match.children[0]))
    }
    
    void completeOctavesModifier(Match match) {
        List<Match> children = match.children
        List<Integer> list = []
        for(Match child : children) {
			list << (int)getResult(child)
        }
        putResult(list)
    }

    void completePitchName(Match match) {
        putResult(match.getText())
    }

    void completeSolfeggioName(Match match) {
        putResult(match.getText())
    }
	
	void processPitch(Pitch pitch, int octaveSteps, int alterations, boolean natural) {
		/*
		 first check octave in scope
		 if present check symbol (C, A, ecc..) in scope
		 if both present then as usual (lilypond - fifth distance)
		 otherwise set the octave as the one from scope 

		 if the octave is not present find in parent scope 
		 if simultan then create octave in local scope

		 (octaveMode "relative")
         c e [g b c] c
         c e {g b c} c

		 for duration similar thing with stickyDuration in scope
		*/
		Map scope = getScope()
		def scopeOctave = scope['octave']
		if (scopeOctave) {
			int octave = (int)scopeOctave
			String symbol = scope['symbol']
			if (symbol) {
				int numNote = NOTE_NAMES[pitch.symbol]
				int relNumNote = NOTE_NAMES[symbol]
				//[C: 1, D: 2, E: 3, F: 4, G: 5, A: 6, B: 7]
				if (Math.abs(numNote - relNumNote) > 3) {
					if ((numNote - relNumNote) < 0)
						octaveSteps += 1
					else
						octaveSteps -= 1
				}
			}
			pitch.octave = octave + octaveSteps
		}
		else {
			pitch.octave += octaveSteps
		}

		def octaveMode = scope['octaveMode']
		if (octaveMode && octaveMode == OctaveMode.RELATIVE) {
			scope['octave'] = pitch.octave
			scope['symbol'] = pitch.symbol
		}

		if (!natural) {
			KeySignature currentKey = (KeySignature)scope['keySignature']
			if (currentKey) {
				int keysig = currentKey.getFifths()
				pitch.alterForKeySignature(keysig)
			}

			if (alterations != 0)
				pitch.alteration = alterations
				/* with:
				   pitch.alteration += alterations   
				   in this case an explicit alteration is added
				   to the one added by the key
				*/
		} else {
			// println "natural pitch " + pitch
			pitch.alteration = 0
		}

	}
    
    void completePitch(Match match) {
        List<Match> children = match.children
		
		List<ACCIDENTALS> accidentals = []
		List<Match> accidentals_match = match.findAllMatchByType(grammar.accidental)
		for(Match acc : accidentals_match) {
			accidentals << (ACCIDENTALS)getResult(acc)
		}
			
		String pitchLetter = ((String)getResult(match.findMatchByType(grammar.pitchName)))
		if (pitchLetter == null) {
			String syllable = ((String)getResult(match.findMatchByType(grammar.solfeggioName)))
			KeySignature currentKey = (KeySignature)scope['keySignature']
			if (currentKey == null) {
				currentKey = new KeySignature()
			}
			pitchLetter = getPitchLetterFromSymbol(syllable, currentKey)
		}
		Pitch pitch = new Pitch(pitchLetter.toUpperCase())
		Match octaves_match = match.findMatchByType(grammar.octavesModifier)
		List<Integer> octaveSteps = (List<Integer>)getResult(octaves_match)
		if (octaveSteps == null || octaveSteps.size() == 0)
			octaveSteps = [0]
			
		int alterations = (accidentals != null) ? getAlterationFromAccidentals(accidentals) : 0
		boolean natural = false
		if (accidentals.size() > 0 && accidentals[-1] == ACCIDENTALS.NATURAL)
			natural = true
		processPitch(pitch, (int)octaveSteps.sum(), alterations, natural)
		
		putResult(pitch)
    }

	void completeReciprocalDuration(Match match) {
		int base_duration = (int)getResult(match.findMatchByType(grammar.number))
		List<Match> dots = match.findAllMatchByType(grammar.dot)
		Fraction duration = fr(1,base_duration)
		for(Match dot : dots) {
			Fraction more_duration = duration.mult(fr(1,2))
			duration = duration.add(more_duration)
		}
		putResult(duration)
	}
	
	void completeVelocity(Match match) {
		putResult(match.getText()[1..-1])
	}
	
	void completeTieStart(Match match) {
		putResult(true)
	}
	
	void completeTieEnd(Match match) {
		putResult(true)
	}

    boolean processPitchList(Match match) {
        // getEnvironment().add([:])
        return true
    }

	void completePitchList(Match match) {
		List<Match> pitches_match = match.findAllMatchByType(grammar.pitch)
		List<Pitch> pitches = []
		for(Match m : pitches_match) {
			Pitch pitch = (Pitch)getResult(m)
			pitches << pitch
		}
        putResult(pitches)
        // getEnvironment().pop()
    }

    void completeUnpitched(Match match) {
    	// later set by completeChord to DisplayPitch
    	putResult(match.getText())
    }
	
	void completeChord(Match match) {
		Chord chord = new Chord()
		String unpitchedSymbol = getResult(match.findMatchByType(grammar.unpitched))
		if (!(unpitchedSymbol == null)) { // unpitched chord
			chord.unpitched = true
			CheckUnpitchedChord(chord)
		} else { // actual chord or note
			List<Pitch> pitchList = (List<Pitch>)getResult(match.findMatchByType(grammar.pitchList))
			if (pitchList == null || pitchList.size() == 0) {
				Pitch pitch = (Pitch)getResult(match.findMatchByType(grammar.pitch))
				pitchList = [pitch]
			}				
			chord.pitches = pitchList
		}
		// duration
		Map scope = getScope()
		Fraction scopeDuration = (Fraction)scope['duration']
		if (scopeDuration)
			chord.duration = scopeDuration			
		Match duration_match = match.findMatchByType(grammar.reciprocalDuration)
		Fraction duration = (Fraction)getResult(duration_match)
		if (duration) {
			chord.duration = duration
			// update sticky duration
			def durationMode = scope['durationMode']
			if (durationMode && durationMode == DurationMode.STICKY)
				scope['duration'] = duration
		}
		// stem
		StemDirection stemDir = (StemDirection)scope['stem']
		if (stemDir)
			chord.setStem(stemDir)
		
		putResult(chord)
	}

	private void CheckUnpitchedChord(Chord chord) {
		Instrument instr = getPercussionInstrument()
		if (instr != null) {
			chord.setUnpitched(true)
			chord.setPitch(instr.getDisplayPitch())
		}
	}

	void completeRest(Match match) {
		Rest rest = new Rest()
		Map scope = getScope()
		Fraction scopeDuration = (Fraction)scope['duration']
		if (scopeDuration)
			rest.duration = scopeDuration			
		Match duration_match = match.findMatchByType(grammar.reciprocalDuration)
		Fraction duration = (Fraction)getResult(duration_match)
		if (duration != null) {
			rest.duration = duration
			// update sticky duration
			def durationMode = scope['durationMode']
			if (durationMode && durationMode == DurationMode.STICKY)
				scope['duration'] = duration
		}
		putResult(rest)
	}
    
	void completeOctave(Match match) {
		Map scope = getScope()
		scope['octave'] = Integer.parseInt(match.getText()[1..-1])
		scope['symbol'] = null
	}
	
	void completeDuration(Match match) {
		Map scope = getScope() // could be omitted since using 'scope' groovy invoke the getter method getScope()
		Fraction duration = (Fraction)getResult(match.findMatchByType(grammar.fraction))
		scope['duration'] = duration
	}
	
	void completeDot(Match match) {
		putResult(match.getText())
	}
	
	void completeAtom(Match match) {
		Chord chord = (Chord)getResult(match.findMatchByType(grammar.chord))
		if (chord != null) {
			boolean tieStart = (boolean)getResult(match.findMatchByType(grammar.tieStart))
			boolean tieEnd = (boolean)getResult(match.findMatchByType(grammar.tieEnd))
			def dynamicMark = getResult(match.findMatchByType(grammar.velocity))			
			if (dynamicMark != null)
				chord.dynamicMark = dynamicMark			
			if (tieStart)
				chord.tieStart = true
			if (tieEnd)
				chord.tieEnd = true
			putResult(chord)
		} 
		ChordSymbol chordSym = (ChordSymbol)getResult(match.findMatchByType(grammar.chordSymbol))
		if (chordSym != null) {
			putResult(chordSym)
		}
		chordSym = (ChordSymbol)getResult(match.findMatchByType(grammar.sameChordSymbol))
		if (chordSym != null) {
			putResult(chordSym)
		}
		Rest rest = (Rest)getResult(match.findMatchByType(grammar.rest))
		if (rest != null) {
			putResult(rest)
		}
	}

	boolean processPhrase(Match match) {
		Map scope = getScope()
		// new scope as copy of parent scope
		Map clone = scope + [:] // [*:scope]
		getEnvironment().add(clone)
        return true
    }

	void completePhrase(Match match) {
		List<Match> children = match.findAllMatchByType(grammar.musicElement)
		List<MusicElement> elements = []
		for(Match m : children) {
			def obj = getResult(m)
			if (obj instanceof MusicElement) {
				elements << (MusicElement)obj
			}
			else if (obj instanceof List<?>) {
				List<MusicElement> phrase = (List<MusicElement>)getResult(m)
				for(MusicElement element : phrase) {
					elements << element
				}
			}
		}
		putResult(elements)
		getEnvironment().pop()
	}

	void completeMusicElement(Match match) {
		Match child = match.getFirstChild()
		putResult(getResult(child))
	}

	void completeScorePosition(Match match) {
		Match m = match.getFirstChild()
		if (m.parser == grammar.part) {
			int cp = Integer.parseInt(m.getText()[1..-1]) - 1
			// for current scope update
			setCurrentPart(cp)
			putResult(['part': cp])
		}
		else if (m.parser == grammar.voice) {
			int cv = Integer.parseInt(m.getText()[1..-1]) - 1
			// for current scope update
			setCurrentVoice(cv)
			putResult(['voice': cv])
		}
	}

	void completeAnchor(Match match) {
		putResult(new Anchor(match.getText()[1..-1]))
	}

	void completeRepeatStart(Match match) {		
		putResult(new Repeat(true))
	}

	void completeRepeatEnd(Match match) {
		int times = 1
		Match m = match.findMatchByType(grammar.number)
		if (m != null)
			times = (int)getResult(m)
		putResult(new Repeat(false, times))
	}

	void completeScoreElement(Match match) {
		Match child = match.getFirstChild()
		putResult(getResult(child))
	}

	void completeScore(Match match) {
		List<Match> children = match.findAllMatchByType(grammar.scoreElement)
		for(Match m : children) {
			// c { e g} { ^5 b {^3 f b}}
			// (tp 3/2 c e g)
			// ^4 `1/8 c d e2 f { (durationMode "sticky")  g4 a} { (octaveMode "relative") b c } c _
			// ~2 `1/2 B
			// ~1 C
			// =2 ~1
			// `=2 ~1
			// `5/7 C
			// ~2
			// _1 `5/8 G `1/4 E
			def obj = getResult(m)
			if (m.getFirstChild().parser == grammar.scorePosition) {
				var pos = (Map<String, Integer>)obj
				// set correct position on score object
				if (pos.containsKey('part'))
					setCurrentPart(pos['part'])
				else if (pos.containsKey('voice'))
					setCurrentVoice(pos['voice'])
			}
			else if (m.getFirstChild().parser == grammar.musicElement) {
				if (obj instanceof CompositionInfo) {
					//println "info= " + obj
					score.setInfo((CompositionInfo)obj)
				}
				else if (obj instanceof MusicElement) {
					addToScore( (MusicElement)obj )
				}
				else if (obj instanceof List<?>) {
					List<MusicElement> phrase = (List<MusicElement>)getResult(m)
					for(MusicElement element : phrase) {
						addToScore( element )
					}
				}
			}
		}
	}

	// CHORD SYMBOLS------------------

	void completeRomanNumeral(Match match) {
		putResult(match.getText())
	}
	
	void completeChordPitchName(Match match) {
		putResult(match.getText())
	}
	
	void completeChordRoot(Match match) {			
		String pitchLetter = getResult(match.findMatchByType(grammar.chordPitchName))
		Map scope = getScope()
		if (pitchLetter == null) {
			String romanNumeral = ((String)getResult(match.findMatchByType(grammar.romanNumeral)))
			KeySignature currentKey = (KeySignature)scope['keySignature']
			if (currentKey == null) {
				currentKey = new KeySignature()
			}
			pitchLetter = getPitchLetterFromSymbol(romanNumeral, currentKey)
		}
		Pitch pitch = new Pitch(pitchLetter.toUpperCase())			
		List<ACCIDENTALS> accidentals = []
		List<Match> accidentals_match = match.findAllMatchByType(grammar.accidental)
		for(Match acc : accidentals_match) {
			accidentals << (ACCIDENTALS)getResult(acc)
		}			
		int octaveSteps = 0			
		int alterations = (accidentals != null) ? getAlterationFromAccidentals(accidentals) : 0
		boolean natural = false
		if (accidentals.size() > 0 && accidentals[-1] == ACCIDENTALS.NATURAL)
			natural = true
		processPitch(pitch, octaveSteps, alterations, natural)			
		putResult(pitch)
	}		
	
	void completeChordModifier(Match match) {
		putResult(match.getText())
	}
	
	void completeChordExtension(Match match) {
		putResult(match.getText())
	}
	
	void completeChordKind(Match match) {
		ChordKind kind = getChordSymboKind(match.getText())
		putResult(kind)
	}
	
	void completeChordAltOp(Match match) {
		putResult(match.getText())
	}
	
	void completeChordAltDegree(Match match) {
		putResult(match.getText())
	}
	
	void completeChordAlteration(Match match) {
		String oper = getResult(match.findMatchByType(grammar.chordAltOp))
		if (oper == null || oper == "")
			oper = 'alt'
		String alterDg = getResult(match.findMatchByType(grammar.chordAltDegree))
		
		List<ACCIDENTALS> accidentals = []
		List<Match> accidentals_match = match.findAllMatchByType(grammar.accidental)
		for(Match acc : accidentals_match) {
			accidentals << (ACCIDENTALS)getResult(acc)
		}
		
		ChordAlteration chordAlteration = new ChordAlteration(alterDg)
		if (accidentals != null) {
			chordAlteration.accidental = getAlterationFromAccidentals(accidentals)
		}
		chordAlteration.type = ChordAltType.valueOf(oper.toUpperCase())
		putResult(chordAlteration)
	}
	
	void completeChordBassSeparator(Match match) {
		putResult(match.getText())
	}
	
	void completeChordBass(Match match) {
		String pitchLetter = getResult(match.findMatchByType(grammar.chordPitchName))
		Map scope = getScope()
		if (pitchLetter == null) {
			String romanNumeral = ((String)getResult(match.findMatchByType(grammar.romanNumeral)))
			KeySignature currentKey = (KeySignature)scope['keySignature']
			if (currentKey == null) {
				currentKey = new KeySignature()
			}
			pitchLetter = getPitchLetterFromSymbol(romanNumeral, currentKey)
		}
		putResult(new Pitch(pitchLetter))
	}
	
	void completeChordSymbol(Match match) {
		String chordName = match.getText()
		
		Pitch root = (Pitch)getResult(match.findMatchByType(grammar.chordRoot))
		ChordKind kind = (ChordKind)getResult(match.findMatchByType(grammar.chordKind))
		ChordAlteration alteration = (ChordAlteration)getResult(match.findMatchByType(grammar.chordAlteration))
		String bassSeparator = getResult(match.findMatchByType(grammar.chordBassSeparator))
		Pitch bass = (Pitch)getResult(match.findMatchByType(grammar.chordBass))
		
		ChordSymbol chordSym = new ChordSymbol(root, kind)
		chordSym.setText(chordName)
		if (bass != null) {
			chordSym.bass = bass
			if (bassSeparator != null && bassSeparator == "\\")
				chordSym.upInversion = false
		}
		if (alteration != null)
			chordSym.chordAlteration = alteration

		Map scope = getScope()
		Fraction duration = (Fraction)scope['duration']
		if (duration)
			chordSym.duration = duration
		else
			chordSym.duration = fr(1,1)

		scope['currentChordSymbol'] = chordSym
		putResult(chordSym)
	}
	
	void completeSameChordSymbol(Match match) {
		ChordSymbol curChordSymbol = (ChordSymbol)scope['currentChordSymbol']
		if (curChordSymbol) 
			putResult(curChordSymbol)
	}
	
	//END CHORD SYMBOLS -----------------------
	
    void completeParam(Match match) {
		putResult(getResult(match.children[0]))
    }
    
	void completeDecimal(Match match) {
		putResult(new BigDecimal(match.getText()))
	}
	
	void completeNumber(Match match) {
		putResult(Integer.parseInt(match.getText()))
	}

	void completeFraction(Match match) {
		putResult(parseFraction(match.getText()))
	}

	void completeIntegerNumber(Match match) {
		putResult(Integer.parseInt(match.getText()))
	}
	
	void completeSymbol(Match match) {
		putResult(match.getText())
	}
	
	void completeStringa(Match match) {
		putResult(match.getText()[1..-2])
	}
	
	void completeIdentifier(Match match) {
		String sym = match.getText()[1..-1]
		if (symbolsTable.containsKey(sym)) {
			List<MusicElement> clone = []
			for(MusicElement element : ((List<MusicElement>)symbolsTable[sym])) {
				if (element.isCopyable())
					clone.add(element.copy())
			}
			putResult(clone)
		}
		else
			putResult(sym)
	}

	// END VISITORS METHODS

    public Score process(ParseResults results) {
		if (score != null || environments.size() > 0 || symbolsTable.size() > 0)
			throw new Exception("Processor already used")
    	score = new Score()
        getResult(results.getLongestMatch())
        return score
    }
}

