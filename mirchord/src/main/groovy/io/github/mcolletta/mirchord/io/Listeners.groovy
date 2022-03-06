/*
 * Copyright (C) 2016-2022 Mirco Colletta
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

import static com.xenoage.utils.math.Fraction.fr
//import javax.sound.midi.*


interface LeadSheetListener {

	void newSong()
	
	void chordSymbol(ChordSymbol event)
	
	void chord(Chord event)

	void rest(Rest event)
}

class LeadSheetSubscriber implements LeadSheetListener {
	
	List<MusicElement> melody
	List<MusicElement> harmony
	
	LeadSheetSubscriber() {
		melody = []
		harmony = []
	}

	@Override
	public void newSong() {
		melody << new Rest(fr(4,4))
		harmony << new Rest(fr(4,4))
	}

	@Override
	public void chordSymbol(ChordSymbol event) {
		harmony << event
	}

	@Override
	public void chord(Chord event) {
		melody << event
	}

	@Override
	public void rest(Rest event) {
		melody << event
	}
	
}


class MelodicIntervalsSubscriber implements LeadSheetListener {
	
	int lastValue = -1
	List<Integer> intervals
	
	MelodicIntervalsSubscriber() {
		intervals = []
	}

	@Override
	public void newSong() {
		lastValue = -1
	}

	@Override
	public void chordSymbol(ChordSymbol event) {
		//println "Event ChordSymbol " + event
	}

	@Override
	public void chord(Chord event) {
		int midiValue = event.pitch.getMidiValue()
		if (lastValue > 0) {
			intervals << (midiValue - lastValue)
		}
		lastValue = midiValue
	}

	@Override
	public void rest(Rest event) {
		lastValue = -1
	}
}