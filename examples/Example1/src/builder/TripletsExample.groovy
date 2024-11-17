new ScoreBuilder().score() {
	part(name: "P1") {
		voice() {
			clef(type:ClefType.TREBLE)
			key(fifths:0, mode:KeyMode.MAJOR)
			time(time:fr(4,4))
			tempo(baseBeat:fr(1,4), bpm:90)
			chord(midiPitch:60, duration:f2)
			tuplet(fraction:fr(3,2)) {
				chord(midiPitch:62, duration:f4)
				chord(midiPitch:64, duration:f4)
				chord(midiPitch:60, duration:f4)
			}			
		}
	}
}