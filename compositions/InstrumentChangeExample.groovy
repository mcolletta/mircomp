// first measures of Frère Jacques (the Italian version is known as "Fra Martino")
builder.score() {
	part(id:"1") {
		voice(id:"1") {
			clef(type:ClefType.Treble)
			key(fifths:0, mode:KeyMode.Major)
			time(time:fr(4,4))
			tempo(baseBeat:fr(1,4), beatsPerMinute:90)
			(1..2).each {
				chord(midiPitch:60, duration:f4)
				chord(midiPitch:62, duration:f4)
				chord(midiPitch:64, duration:f4)
				chord(midiPitch:60, duration:f4)
			}
			instrument(id:"Violin 1", midiProgram:41)
			(1..2).each {
				chord(midiPitch:64, duration:f4)
				chord(midiPitch:65, duration:f4)
				chord(midiPitch:67, duration:f2)
			}
		}
	}
}