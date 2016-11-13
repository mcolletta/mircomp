// first measures of a simplified C.P.E. Bach Marche with added drums 
builder.score() {
	part(id:"1") {
		voice(id:"1") {
			clef(type:ClefType.TREBLE)
			key(fifths:1, mode:KeyMode.MAJOR)
			time(time:fr(4,4))
			tempo(baseBeat:fr(1,4), bpm:90, text:"Allegro.")	// PROBLEM WITH TEXT
			// 1
			chord(midiPitch:74, duration:f8, stem:StemDirection.DOWN)
			(1..3).each { chord(midiPitch:67, duration:f8, stem:StemDirection.DOWN) }
			anchor(id:"1")
			chord(midiPitch:67, duration:f8, stem:StemDirection.DOWN)			
			chord(midiPitch:69, duration:f8, stem:StemDirection.DOWN)
			chord(midiPitch:71, duration:f8, stem:StemDirection.DOWN)
			chord(midiPitch:72, duration:f8, stem:StemDirection.DOWN)
			// 2
			chord(midiPitch:71, duration:f8, stem:StemDirection.DOWN)
			(1..4).each { chord(midiPitch:67, duration:f8, stem:StemDirection.DOWN) }
			chord(midiPitch:71, duration:f8, stem:StemDirection.DOWN)
			chord(midiPitch:72, duration:f8, stem:StemDirection.DOWN)
			chord(midiPitch:69, duration:f8, stem:StemDirection.DOWN)
			// 3
			chord(midiPitch:67, duration:f8, stem:StemDirection.DOWN)
			chord(midiPitch:67, duration:f8, stem:StemDirection.DOWN)
			chord(midiPitch:71, duration:f8, stem:StemDirection.DOWN)
			chord(midiPitch:74, duration:f8, stem:StemDirection.DOWN)
			chord(midiPitch:79, duration:f8, stem:StemDirection.DOWN)
			chord(midiPitch:74, duration:f8, stem:StemDirection.DOWN)
			chord(midiPitch:79, duration:f8, stem:StemDirection.DOWN)
			chord(midiPitch:81, duration:f16, stem:StemDirection.DOWN)
			chord(midiPitch:83, duration:f16, stem:StemDirection.DOWN)
		}
		voice(id:"2") {
			// 1
			anchor(id:"1")
			rest(duration:f8)
			chord(midiPitch:78, duration:f8, stem:StemDirection.UP)
			chord(midiPitch:79, duration:f8, stem:StemDirection.UP)
			chord(midiPitch:76, duration:f8, stem:StemDirection.UP)
			// 2
			chord(midiPitch:74, duration:f8, stem:StemDirection.UP)
			rest(duration:f8, hidden:true) // hidden rest non curretly supported
			rest(duration:f4, hidden:true)

			rest(duration:f8, hidden:true)
			chord(midiPitch:74, duration:f8, stem:StemDirection.UP)
			chord(midiPitch:76, duration:f8, stem:StemDirection.UP)
			chord(midiPitch:72, duration:f8, stem:StemDirection.UP)
			// 3
			chord(midiPitch:71, duration:f8, stem:StemDirection.UP)
			rest(duration:f8, hidden:true)
			rest(duration:f3, hidden:true)
		}
	}

	part(id:"2") {
		voice(id:"1") {
			clef(type:ClefType.BASS)
			key(fifths:1, mode:KeyMode.MAJOR)
			time(time:fr(4,4))		
			(1..2).each {
				rest(duration:f8)  // REST not shown at the start of a measure
				(1..4).each { chord(midiPitch:43, duration:f8) }
				rest(duration:f8)
				rest(duration:f4)
			}
			rest(duration:f4)
			chord(midiPitch:43, duration:f4)
			chord(midiPitch:47, duration:f4)
			chord(midiPitch:43, duration:f4)
		}
	}

	part(id:"3") {
		voice(id:"1") {
			instrument(id:"Drums 1", program:35, unpitched:true)
			clef(type:ClefType.PERCUSSION)
			(1..3).each {
				(1..16).each {
					chord(midiPitch:67, duration:f16, unpitched:true)
				}
			}
		}
		voice(id:"2") {
			instrument(id:"Drums 2", program:51, unpitched:true)
			(1..3).each {
				(1..4).each {
					chord(midiPitch:60, duration:f4, unpitched:true)
				}
			}
		}
	}	
}

