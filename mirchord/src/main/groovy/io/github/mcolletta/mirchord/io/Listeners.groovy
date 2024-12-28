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

package io.github.mcolletta.mirchord.io

import io.github.mcolletta.mirchord.core.*

import java.util.Queue
import java.util.concurrent.ArrayBlockingQueue

import com.xenoage.utils.math.Fraction
import static com.xenoage.utils.math.Fraction.fr
import static com.xenoage.utils.math.Fraction._0


interface LeadSheetListener {

	void newSong(String filename)

	void endSong(String filename)

	void keySignature(KeySignature event)
	
	void chordSymbol(ChordSymbol event)
	
	void chord(Chord event)

	void rest(Rest event)

	void endReading()
}


class ChordStatistics implements LeadSheetListener {
	
	Map<String, Map<String, Float>> chordStats
	ChordSymbol currentChord
	Fraction currentChordDuration

	boolean transpose
	int transposition = 0

	Map<Integer, String> pitch_names = [0: 'C', 1: 'C#/Db', 2: 'D', 3: 'D#/Eb', 4: 'E', 5: 'F',
		                                6: 'F#/Gb', 7: 'G', 8: 'G#/Ab', 9: 'A', 10: 'A#/Bb', 11: 'B']
	
	ChordStatistics(transpose=false) {
		this.transpose = transpose
		chordStats = [:].withDefault{['C': 0.0f, 'C#/Db': 0.0f, 'D': 0.0f, 'D#/Eb': 0.0f, 'E': 0.0f, 'F': 0.0f,
		                              'F#/Gb': 0.0f, 'G': 0.0f, 'G#/Ab': 0.0f, 'A': 0.0f, 'A#/Bb': 0.0f, 'B': 0.0f]}
	}

	void normalizeChordStats() {
		chordStats.each { String chord, Map<String, Float> pitch_histogram ->
			float sum = (float) pitch_histogram.values().sum()
			if (sum > 0) {
				pitch_histogram.each { String k, double v ->
					pitch_histogram[k] = (float) (v/sum)
				}
			}
		}
	}

	@Override
	public void newSong(String filename) {
		currentChord = null
		currentChordDuration = _0
	}

	@Override
	public void endSong(String filename) { }

	@Override
	public void keySignature(KeySignature keysig) {
		if (transpose) {
			transposition = Utils.getTonicTransposition(keysig)
		}
	}

	@Override
	public void chordSymbol(ChordSymbol chordSymbol) {
		currentChord = chordSymbol
		if (transposition > 0) {
			currentChord.root.midiValue -= transposition
			if (currentChord.bass != null)
				currentChord.bass.midiValue -= transposition
		}
	}

	@Override
	public void chord(Chord note) {
		if (currentChord != null) {
			if (transposition > 0)
				note.pitch.midiValue -= transposition
			def note_name = pitch_names[note.pitch.midiValue % 12]
			def root_name = pitch_names[currentChord.root.midiValue % 12]
			def chordsym_name = root_name + "_" + currentChord.getKindText().toLowerCase()
			chordStats[chordsym_name][note_name] += 1.0f
		}
	}

	@Override
	public void rest(Rest rest) {
		currentChordDuration = currentChordDuration.add(rest.duration)
	}

	@Override
	public void endReading() {
		normalizeChordStats()
	}
}


class MetricsMelodicIntervals implements LeadSheetListener {

	int lastValue = -1
	Queue<Integer> fifo2
	Queue<Integer> fifo3

	Map<String, Float> unigrams
	Map<String, Float> bigrams
	Map<String, Float> trigrams

	MetricsMelodicIntervals() {
		unigrams = [:].withDefault{0.0f}
		bigrams = [:].withDefault{0.0f}
		trigrams = [:].withDefault{0.0f}
	}

	void normalizeStats(Map<String, Float> histogram) {
		float sum = (float) histogram.values().sum()
		if (sum > 0) {
			histogram.each { String k, double v ->
				histogram[k] = (float) (v/sum)
			}
		}
	}

	static void append(Queue<Integer> fifo, Integer i) {
	  if (!fifo.offer(i)) {
	      fifo.poll();
	      fifo.offer(i);
	  }
	}

	@Override
	public void newSong(String filename) {
		lastValue = -1
		fifo2 = new ArrayBlockingQueue<>(2)
		fifo3 = new ArrayBlockingQueue<>(3)
	}

	@Override
	public void endSong(String filename) { }

	@Override
	public void chord(Chord event) {
		int midiValue = event.pitch.getMidiValue()
		if (lastValue > 0) {
			int interval = midiValue - lastValue
			append(fifo2, interval)
			append(fifo3, interval)
			unigrams[interval.toString()] += 1.0f
			if (fifo2.size() == 2)
				bigrams[fifo2.toString()] += 1.0f
			if (fifo3.size() == 3)
				trigrams[fifo3.toString()] += 1.0f
		}
		lastValue = midiValue
	}

	@Override
	public void keySignature(KeySignature keysig) { }

	@Override
	public void chordSymbol(ChordSymbol chordSymbol) { }

	@Override
	public void rest(Rest rest) { }

	@Override
	public void endReading() {
		normalizeStats(unigrams)
		normalizeStats(bigrams)
		normalizeStats(trigrams)
	}
}


class MetricsDuration implements LeadSheetListener {

	Fraction currDuration = _0
	Queue<Fraction> fifo2
	Queue<Fraction> fifo3

	Map<String, Float> unigrams
	Map<String, Float> bigrams
	Map<String, Float> trigrams

	MetricsDuration() {
		unigrams = [:].withDefault{0.0f}
		bigrams = [:].withDefault{0.0f}
		trigrams = [:].withDefault{0.0f}		
	}

	void normalizeStats(Map<String, Float> histogram) {
		float sum = (float) histogram.values().sum()
		if (sum > 0) {
			histogram.each { String k, double v ->
				histogram[k] = (float) (v/sum)
			}
		}
	}

	static void append(Queue<Fraction> fifo, Fraction i) {
	  if (!fifo.offer(i)) {
	      fifo.poll();
	      fifo.offer(i);
	  }
	}

	@Override
	public void newSong(String filename) {
		currDuration = _0
		fifo2 = new ArrayBlockingQueue<>(2)
		fifo3 = new ArrayBlockingQueue<>(3)
	}

	@Override
	public void endSong(String filename) { }

	@Override
	public void chord(Chord note) {
		Fraction duration = note.duration
		if (currDuration > _0) {
			Fraction ratio = duration.divideBy(currDuration)
			append(fifo2, ratio)
			append(fifo3, ratio)
			unigrams[ratio.toString()] += 1.0f
			if (fifo2.size() == 2)
				bigrams[fifo2.toString()] += 1.0f
			if (fifo3.size() == 3)
				trigrams[fifo3.toString()] += 1.0f
		}
		currDuration = duration
	}

	@Override
	public void keySignature(KeySignature keysig) { }

	@Override
	public void chordSymbol(ChordSymbol chordSymbol) { }

	@Override
	public void rest(Rest rest) { }

	@Override
	public void endReading() {
		normalizeStats(unigrams)
		normalizeStats(bigrams)
		normalizeStats(trigrams)
	}
}

class MelodicIntervals implements LeadSheetListener {

	int lastValue = -1
	List<Integer> intervals

	MelodicIntervals() {
		intervals = []
	}

	@Override
	public void newSong(String filename) {
		lastValue = -1
	}

	@Override
	public void endSong(String filename) { }

	@Override
	public void chord(Chord event) {
		int midiValue = event.pitch.getMidiValue()
		if (lastValue > 0) {
			int interval = midiValue - lastValue
			intervals << interval
		}
		else
		    intervals << Integer.MIN_VALUE
		lastValue = midiValue
	}

	@Override
	public void keySignature(KeySignature keysig) { }

	@Override
	public void chordSymbol(ChordSymbol chordSymbol) { }

	@Override
	public void rest(Rest rest) { }

	@Override
	public void endReading() { }
}

class DurationRatios implements LeadSheetListener {

	Fraction currDuration = _0
	List<Fraction> ratios

	DurationRatios() {
		ratios = []
	}

	@Override
	public void newSong(String filename) {
		currDuration = _0
	}

	@Override
	public void endSong(String filename) { }

	@Override
	public void chord(Chord note) {
		Fraction duration = note.duration
		if (currDuration > _0) {
			Fraction ratio = duration.divideBy(currDuration)
			ratios << ratio
		}
		else
		    ratios << _0
		currDuration = duration
	}

	@Override
	public void keySignature(KeySignature keysig) { }

	@Override
	public void chordSymbol(ChordSymbol chordSymbol) { }

	@Override
	public void rest(Rest rest) { }

	@Override
	public void endReading() { }
}

class ChromaFeatures implements LeadSheetListener {

	int transposition = 0
	List<Integer> features

	ChromaFeatures() {
		features = []
	}

	@Override
	public void newSong(String filename) {
		features << Integer.MIN_VALUE
	}

	@Override
	public void endSong(String filename) { }

	@Override
	public void keySignature(KeySignature keysig) {
		transposition = Utils.getTonicTransposition(keysig)
	}

	@Override
	public void chord(Chord event) {
		int midiValue = event.pitch.getMidiValue()
		if (transposition > 0)
			midiValue -= transposition
		def chroma = midiValue % 12
		features << chroma
	}

	@Override
	public void chordSymbol(ChordSymbol chordSymbol) { }

	@Override
	public void rest(Rest rest) {
		features << 0
	}

	@Override
	public void endReading() { }
}

class Durations implements LeadSheetListener {

	Fraction currDuration = _0
	List<Fraction> durations

	Durations() {
		durations = []
	}

	@Override
	public void newSong(String filename) {
		durations << fr(-1)
	}

	@Override
	public void endSong(String filename) { }

	@Override
	public void chord(Chord note) {
		durations << note.duration
	}

	@Override
	public void keySignature(KeySignature keysig) { }

	@Override
	public void chordSymbol(ChordSymbol chordSymbol) { }

	@Override
	public void rest(Rest rest) {
		durations << _0
	}

	@Override
	public void endReading() { }
}

class MirchordScoreBuilder implements LeadSheetListener {

	List<Score> scores
	Score score
	boolean useChordSymbols

	MirchordScoreBuilder(boolean useChordSymbols=false) {
		scores = []
		this.useChordSymbols = useChordSymbols
	}

	@Override
	public void newSong(String filename) {
		score = new Score()
		Part part = new Part("Melody")
		score.parts.add(part)
		part.voices.add(new Voice())
		part.voices[0].elements = []
		// if (useChordSymbols) {
		// 	Part hpart = new Part("Harmony")
		// 	score.parts.add(hpart)
		// 	hpart.voices.add(new Voice())
		// 	hpart.voices[0].elements = []
		// }
	}

	@Override
	public void endSong(String filename) {
		scores << score
	}

	@Override
	public void chord(Chord note) {
		score.parts[0].voices[0].elements << note
	}

	@Override
	public void keySignature(KeySignature keysig) {
		score.parts[0].voices[0].elements << keysig
	}

	@Override
	public void chordSymbol(ChordSymbol chordSymbol) {
		// if (useChordSymbols) {
		// 	score.parts[1].voices[0].elements << chordSymbol
		// }
	}

	@Override
	public void rest(Rest rest) {
		score.parts[0].voices[0].elements << rest
	}

	@Override
	public void endReading() { }
}
