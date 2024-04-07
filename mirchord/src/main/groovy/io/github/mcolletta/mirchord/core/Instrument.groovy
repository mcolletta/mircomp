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

import groovy.transform.ToString

@ToString
class Instrument implements MusicElement {
	String id = "acoustic grand"
	int program = 0
	int bank = 0
	boolean unpitched = false

	Pitch displayPitch

	public Instrument() {}
	
	public Instrument(String name, boolean unpitched=false) {
		if (GM.containsKey(name)) {
			this.id = name
			this.program = GM[name]
			this.unpitched = unpitched
		}
		else
			throw new Exception("The instrument $name is unknown")
	}
	
	public Instrument(Integer program, boolean unpitched=false) {
		if (program in (0..127)) {
			this.program = program
			this.id = GM.find { it.value == program }?.key
			if (unpitched && !(program in (0..81)))
				throw new Exception("The program $program in MIDI for DRUMS should be in the range (35..81)")
			this.unpitched = unpitched
		}
		else
			throw new Exception("The program $program in MIDI should be in the range (0..127)")
	}
	
	public Instrument(String name, int program, int bank, boolean unpitched) {
		this.id = name
		this.unpitched = unpitched
		this.program = program
		this.bank = bank
	}

	String getMusicElementType() {
		return "Instrument"
	}

	void setProgram(int midiProgram) {
		if (midiProgram < 0 || midiProgram > 128)
			throw new IllegalArgumentException("MIDI program must be between 0 and 127");
		program = midiProgram;
	}
	
	static Map<String,Integer> GM = [
		// INSTRUMENTS
		"acoustic grand piano": 0,
		"bright acoustic piano": 1,
		"electric grand piano": 2,
		"honky-tonk piano": 3,
		"electric piano 1": 4,
		"electric piano 2": 5,
		"harpsichord": 6,
		"clav": 7,
		"celesta": 8,
		"glockenspiel": 9,
		"music box": 10,
		"vibraphone": 11,
		"marimba": 12,
		"xylophone": 13,
		"tubular bells": 14,
		"dulcimer": 15,
		"drawbar organ": 16,
		"percussive organ": 17,
		"rock organ": 18,
		"church organ": 19,
		"reed organ": 20,
		"accordion": 21,
		"harmonica": 22,
		"concertina": 23,
		"acoustic guitar (nylon)": 24,
		"acoustic guitar (steel)": 25,
		"electric guitar (jazz)": 26,
		"electric guitar (clean)": 27,
		"electric guitar (muted)": 28,
		"overdriven guitar": 29,
		"distorted guitar": 30,
		"guitar harmonics": 31,
		"acoustic bass": 32,
		"electric bass (finger)": 33,
		"electric bass (pick)": 34,
		"fretless bass": 35,
		"slap bass 1": 36,
		"slap bass 2": 37,
		"synth bass 1": 38,
		"synth bass 2": 39,
		"violin": 40,
		"viola": 41,
		"cello": 42,
		"contrabass": 43,
		"tremolo strings": 44,
		"pizzicato strings": 45,
		"orchestral harp": 46,
		"timpani": 47,
		"string ensemble 1": 48,
		"string ensemble 2": 49,
		"synthstrings 1": 50,
		"synthstrings 2": 51,
		"choir aahs": 52,
		"voice oohs": 53,
		"synth voice": 54,
		"orchestra hit": 55,
		"trumpet": 56,
		"trombone": 57,
		"tuba": 58,
		"muted trumpet": 59,
		"french horn": 60,
		"brass section": 61,
		"synthbrass 1": 62,
		"synthbrass 2": 63,
		"soprano sax": 64,
		"alto sax": 65,
		"tenor sax": 66,
		"baritone sax": 67,
		"oboe": 68,
		"english horn": 69,
		"bassoon": 70,
		"clarinet": 71,
		"piccolo": 72,
		"flute": 73,
		"recorder": 74,
		"pan flute": 75,
		"blown bottle": 76,
		"shakuhachi": 77,
		"whistle": 78,
		"ocarina": 79,
		"lead 1 (square)": 80,
		"lead 2 (sawtooth)": 81,
		"lead 3 (calliope)": 82,
		"lead 4 (chiff)": 83,
		"lead 5 (charang)": 84,
		"lead 6 (voice)": 85,
		"lead 7 (fifths)": 86,
		"lead 8 (bass+lead)": 87,
		"pad 1 (new age)": 88,
		"pad 2 (warm)": 89,
		"pad 3 (polysynth)": 90,
		"pad 4 (choir)": 91,
		"pad 5 (bowed)": 92,
		"pad 6 (metallic)": 93,
		"pad 7 (halo)": 94,
		"pad 8 (sweep)": 95,
		"fx 1 (rain)": 96,
		"fx 2 (soundtrack)": 97,
		"fx 3 (crystal)": 98,
		"fx 4 (atmosphere)": 99,
		"fx 5 (brightness)": 100,
		"fx 6 (goblins)": 101,
		"fx 7 (echoes)": 102,
		"fx 8 (sci-fi)": 103,
		"sitar": 104,
		"banjo": 105,
		"shamisen": 106,
		"koto": 107,
		"kalimba": 108,
		"bagpipe": 109,
		"fiddle": 110,
		"shanai": 111,
		"tinkle bell": 112,
		"agogo": 113,
		"steel drums": 114,
		"woodblock": 115,
		"taiko drum": 116,
		"melodic tom": 117,
		"synth drum": 118,
		"reverse cymbal": 119,
		"guitar fret noise": 120,
		"breath noise": 121,
		"seashore": 122,
		"bird tweet": 123,
		"telephone ring": 124,
		"helicopter": 125,
		"applause": 126,
		"gunshot": 127,
		
		// DRUMS
		"acousticbassdrum": 35,
		"bassdrum": 36,
		"hisidestick": 37,
		"sidestick": 37,
		"losidestick": 37,
		"acousticsnare": 38,
		"snare": 38,
		"handclap": 39,
		"electricsnare": 40,
		"lowfloortom": 41,
		"closedhihat": 42,
		"hihat": 42,
		"highfloortom": 43,
		"pedalhihat": 44,
		"lowtom": 45,
		"openhihat": 46,
		"halfopenhihat": 46,
		"lowmidtom": 47,
		"himidtom": 48,
		"crashcymbala": 49,
		"crashcymbal": 49,
		"hightom": 50,
		"ridecymbala": 51,
		"ridecymbal": 51,
		"chinesecymbal": 52,
		"ridebell": 53,
		"tambourine": 54,
		"splashcymbal": 55,
		"cowbell": 56,
		"crashcymbalb": 57,
		"vibraslap": 58,
		"ridecymbalb": 59,
		"mutehibongo": 60,
		"hibongo": 60,
		"openhibongo": 60,
		"mutelobongo": 61,
		"lobongo": 61,
		"openlobongo": 61,
		"mutehiconga": 62,
		"muteloconga": 62,
		"openhiconga": 63,
		"hiconga": 63,
		"openloconga": 64,
		"loconga": 64,
		"hitimbale": 65,
		"lotimbale": 66,
		"hiagogo": 67,
		"loagogo": 68,
		"cabasa": 69,
		"maracas": 70,
		"shortwhistle": 71,
		"longwhistle": 72,
		"shortguiro": 73,
		"longguiro": 74,
		"guiro": 74,
		"claves": 75,
		"hiwoodblock": 76,
		"lowoodblock": 77,
		"mutecuica": 78,
		"opencuica": 79,
		"mutetriangle": 80,
		"triangle": 81,
		"opentriangle": 81
	]
	
	static def abbr = [
		"acousticbassdrum": "bda",
		"bassdrum": "bd",
		"hisidestick": "ssh",
		"sidestick": "ss",
		"losidestick": "ssl",
		"acousticsnare": "sna",
		"snare": "sn",
		"handclap": "hc",
		"electricsnare": "sne",
		"lowfloortom": "tomfl",
		"closedhihat": "hhc",
		"hihat": "hh",
		"highfloortom": "tomfh",
		"pedalhihat": "hhp",
		"lowtom": "toml",
		"openhihat": "hho",
		"halfopenhihat": "hhho",
		"lowmidtom": "tomml",
		"himidtom": "tommh",
		"crashcymbala": "cymca",
		"crashcymbal": "cymc",
		"hightom": "tomh",
		"ridecymbala": "cymra",
		"ridecymbal": "cymr",
		"chinesecymbal": "cymch",
		"ridebell": "rb",
		"tambourine": "tamb",
		"splashcymbal": "cyms",
		"cowbell": "cb",
		"crashcymbalb": "cymcb",
		"vibraslap": "vibs",
		"ridecymbalb": "cymrb",
		"mutehibongo": "bohm",
		"hibongo": "boh",
		"openhibongo": "boho",
		"mutelobongo": "bolm",
		"lobongo": "bol",
		"openlobongo": "bolo",
		"mutehiconga": "cghm",
		"muteloconga": "cglm",
		"openhiconga": "cgho",
		"hiconga": "cgh",
		"openloconga": "cglo",
		"loconga": "cgl",
		"hitimbale": "timh",
		"lotimbale": "timl",
		"hiagogo": "agh",
		"loagogo": "agl",
		"cabasa": "cab",
		"maracas": "mar",
		"shortwhistle": "whs",
		"longwhistle": "whl",
		"shortguiro": "guis",
		"longguiro": "guil",
		"guiro": "gui",
		"claves": "cl",
		"hiwoodblock": "wbh",
		"lowoodblock": "wbl",
		"mutecuica": "cuim",
		"opencuica": "cuio",
		"mutetriangle": "trim",
		"triangle": "tri",
		"opentriangle": "trio"
	]
	
}

