File f = new File( projectPath.resolve("musicxml/chroma").toString() )
def reader = new MusicXmlLeadSheetReader()
def score_subscr = new MirchordScoreBuilder()
reader.addLeadSheetListener(score_subscr)
reader.read(f)

List<Chord> notes = []
for(Score score: score_subscr.scores) {
    notes.addAll( score.parts[0].voices[0].getChords() )
}

Comparator<Chord> comparator = [
			compare:{ Chord a, Chord b -> a.pitches[0].getMidiValue()  <=> b.pitches[0].getMidiValue() }
		] as Comparator<Chord>


var fo = new FactorOracle<Chord>(notes, comparator)
var nav = new OracleNavigator(fo)
var seq = nav.navigate(0, 330)

var score = new Score()
var part = new Part("Melody")
score.parts.add(part)
var voice = new Voice()
part.voices.add(voice)
part.voices[0].elements = []

for (int i = 0; i < seq.size(); i++) {
    voice.elements << seq[i]
}

def midiFile = projectPath.resolve('midi/fo.mid').toFile()
saveAs(score, midiFile)

score
