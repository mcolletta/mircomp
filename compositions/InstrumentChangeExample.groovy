// first measures of Frère Jacques (the Italian version is known as "Fra Martino")
def f4 = fr(1,4)
def f2 = fr(1,2)
def f1 = fr(1,1)
def fra_martino = new ScoreBuilder().score() {
	part(name:"P1") {
		voice() {
			clef(type:ClefType.TREBLE)
			key(fifths:0, mode:KeyMode.MAJOR)
			time(time:fr(4,4))
			tempo(baseBeat:fr(1,4), bpm:90)
			(1..2).each {
				chord(midiPitch:60, duration:f4)
				chord(midiPitch:62, duration:f4)
				chord(midiPitch:64, duration:f4)
				chord(midiPitch:60, duration:f4)
			}
			instrument(id:"Violin 1", program:41)
			(1..2).each {
				chord(midiPitch:64, duration:f4)
				chord(midiPitch:65, duration:f4)
				chord(midiPitch:67, duration:f2)
			}
		}
	}
}