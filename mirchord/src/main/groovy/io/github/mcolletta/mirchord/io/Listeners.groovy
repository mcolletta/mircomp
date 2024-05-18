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

	void newSong()

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
	
	ChordStatistics(transpose=false) {
		this.transpose = transpose
		chordStats = [:].withDefault{[:].withDefault{0.0f}}
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
	public void newSong() {
		currentChord = null
		currentChordDuration = _0
	}

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
			chordStats[currentChord.toSymbolString()][note.pitch.toSymbolString()] += 1.0f
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
	public void newSong() {
		lastValue = -1
		fifo2 = new ArrayBlockingQueue<>(2)
		fifo3 = new ArrayBlockingQueue<>(3)
	}

	@Override
	public void chord(Chord event) {
		int midiValue = event.pitch.getMidiValue()
		if (lastValue > 0) {
			int interval = midiValue - lastValue
			append(fifo2, interval)
			append(fifo3, interval)
			unigrams[interval.toString()] += 1.0f
			bigrams[interval.toString()] += 1.0f
			trigrams[interval.toString()] += 1.0f
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
	public void newSong() {
		currDuration = _0
		fifo2 = new ArrayBlockingQueue<>(2)
		fifo3 = new ArrayBlockingQueue<>(3)
	}

	@Override
	public void chord(Chord note) {
		Fraction duration = note.duration
		if (currDuration > _0) {
			Fraction ratio = duration.divideBy(currDuration)
			append(fifo2, ratio)
			append(fifo3, ratio)
			unigrams[ratio.toString()] += 1.0f
			bigrams[fifo2.toString()] += 1.0f
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
		normalizeStats(trigrams)
	}
}
