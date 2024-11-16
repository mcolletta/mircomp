
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

package io.github.mcolletta.mirconverter

import io.github.mcolletta.mirchord.core.*
import io.github.mcolletta.mirchord.io.*
import static io.github.mcolletta.mirconverter.Helper.saveAs



class MidiBuilder implements LeadSheetListener {

	Score score
	String savePath
	boolean useChordSymbols

	MidiBuilder(String savePath, boolean useChordSymbols=false) {
		this.savePath = savePath
		this.useChordSymbols = useChordSymbols
	}

	private saveMidiFile(String filename) {
		String midiname = filename
		int i = filename.toString().lastIndexOf('.')
        if (i > 0) {
            def ext = filename.substring(i+1);
            midiname = filename.replace(".$ext", ".mid")
        }
        else
        	midiname += ".mid"
        
		saveAs(score, new File("$savePath/$midiname"))
	}

	@Override
	public void newSong(String filename) {
		score = new Score()
		Part part = new Part("Melody")
		score.parts.add(part)
		part.voices.add(new Voice())
		part.voices[0].elements = []
		// if (useChordSymbols) {
		// 	Part part = new Part("Harmony")
		// 	score.parts.add(part)
		// 	part.voices.add(new Voice())
		// 	part.voices[0].elements = []
		// }
	}

	@Override
	public void endSong(String filename) { 
		if (score != null) {
			saveMidiFile(filename)
		}
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
	public void chordSymbol(ChordSymbol chordSymbol) { }

	@Override
	public void rest(Rest rest) {
		score.parts[0].voices[0].elements << rest
	}

	@Override
	public void endReading() { }

}