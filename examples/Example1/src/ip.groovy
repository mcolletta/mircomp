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
var ip = new IncrementalParser<Chord>(notes, comparator)


var seq = ip.generate(new Chord(new Pitch('E'), fr(1,8)), 330, 16)


var score = new Score()
var part = new Part("Melody")
score.parts.add(part)
var voice = new Voice()
part.voices.add(voice)
part.voices[0].elements = []

for (int i = 0; i < seq.size(); i++) {
    voice.elements << seq[i]
}

def midiFile = projectPath.resolve('midi/ip.mid').toFile()
saveAs(score, midiFile)

score
