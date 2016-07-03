// first measures of a simplified C.P.E. Bach Marche with added drums 
builder.score() {
	part(id:"1") {
		voice(id:"1") {
			clef(type:ClefType.Treble)
			key(fifths:1, mode:KeyMode.Major)
			time(time:fr(4,4)) // PROBLEM
			tempo(baseBeat:fr(1,4), beatsPerMinute:90, text:"Allegro.")	// PROBLEM	
			// 1
			chord(midiPitch:74, duration:f8, stem:StemDirection.Down)
			(1..3).each { chord(midiPitch:67, duration:f8, stem:StemDirection.Down) }
			anchor(id:"1")
			chord(midiPitch:67, duration:f8, stem:StemDirection.Down)			
			chord(midiPitch:69, duration:f8, stem:StemDirection.Down)
			chord(midiPitch:71, duration:f8, stem:StemDirection.Down)
			chord(midiPitch:72, duration:f8, stem:StemDirection.Down)
			// 2
			chord(midiPitch:71, duration:f8, stem:StemDirection.Down)
			(1..4).each { chord(midiPitch:67, duration:f8, stem:StemDirection.Down) }
			chord(midiPitch:71, duration:f8, stem:StemDirection.Down)
			chord(midiPitch:72, duration:f8, stem:StemDirection.Down)
			chord(midiPitch:69, duration:f8, stem:StemDirection.Down)
			// 3
			chord(midiPitch:67, duration:f8, stem:StemDirection.Down)
			chord(midiPitch:67, duration:f8, stem:StemDirection.Down)
			chord(midiPitch:71, duration:f8, stem:StemDirection.Down)
			chord(midiPitch:74, duration:f8, stem:StemDirection.Down)
			chord(midiPitch:79, duration:f8, stem:StemDirection.Down)
			chord(midiPitch:74, duration:f8, stem:StemDirection.Down)
			chord(midiPitch:79, duration:f8, stem:StemDirection.Down)
			chord(midiPitch:81, duration:f16, stem:StemDirection.Down)
			chord(midiPitch:83, duration:f16, stem:StemDirection.Down)
		}
		voice(id:"2") {
			// 1
			anchor(id:"1")
			rest(duration:f8)
			chord(midiPitch:78, duration:f8, stem:StemDirection.Up)
			chord(midiPitch:79, duration:f8, stem:StemDirection.Up)
			chord(midiPitch:76, duration:f8, stem:StemDirection.Up)
			// 2
			chord(midiPitch:74, duration:f8, stem:StemDirection.Up)
			rest(duration:f8, hidden:true)
			rest(duration:f4, hidden:true)

			rest(duration:f8, hidden:true)
			chord(midiPitch:74, duration:f8, stem:StemDirection.Up)
			chord(midiPitch:76, duration:f8, stem:StemDirection.Up)
			chord(midiPitch:72, duration:f8, stem:StemDirection.Up)
			// 3
			chord(midiPitch:71, duration:f8, stem:StemDirection.Up)
			rest(duration:f8, hidden:true)
			rest(duration:f3, hidden:true)
		}
	}

	part(id:"2") {
		voice(id:"1") {
			clef(type:ClefType.Bass)
			key(fifths:1, mode:KeyMode.Major)
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
			instrument(id:"Drums 1", midiProgram:35, unpitched:true)
			clef(type:ClefType.Percussion)
			(1..3).each {
				(1..16).each {
					chord(midiPitch:67, duration:f16, unpitched:true)
				}
			}
		}
		voice(id:"2") {
			instrument(id:"Drums 2", midiProgram:51, unpitched:true)
			(1..3).each {
				(1..4).each {
					chord(midiPitch:60, duration:f4, unpitched:true)
				}
			}
		}
	}	
}

