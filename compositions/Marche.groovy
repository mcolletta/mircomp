// first measures of a simplified C.P.E. Bach Marche with added drums 
def f16 = fr(1,16)
def f8 = fr(1,8)
def f4 = fr(1,4)
def f3 = fr(3,4)
def f2 = fr(1,2)
def f1 = fr(1,1)
def marche = new ScoreBuilder().score() {
	part(id:"1") {
		voice(id:"1") {
		    key(fifths:1, mode:KeyMode.MAJOR)
			clef(type:ClefType.TREBLE)
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
		    key(fifths:1, mode:KeyMode.MAJOR)
			clef(type:ClefType.BASS)
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
			chord(duration:f4) {
				pitch(symbol:'G', octave:2)
			}
		}
	}

	part(id:"3") {
		voice(id:"1") {
		    key(fifths:1, mode:KeyMode.MAJOR)
			clef(type:ClefType.PERCUSSION)
			time(time:fr(4,4))
			instrument(id:"Drums 1", program:35, displayPitch: new Pitch('G'), unpitched:true)
			(1..3).each {
				(1..16).each {
					chord(duration:f16, refId:"Drums 1")
				}
			}
		}
		voice(id:"2") {
			key(fifths:1, mode:KeyMode.MAJOR)
			instrument(id:"Drums 2", program:51, unpitched:true)
			(1..3).each {
				(1..4).each {
					chord(midiPitch:60, duration:f4, unpitched:true)
				}
			}
		}
	}	
}

