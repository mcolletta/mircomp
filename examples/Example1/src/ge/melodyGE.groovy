public class MelodyFitness extends FitnessFunction {

    MirChordInterpreter interpreter
    Compressor<Integer> dict_chroma

    MelodyFitness(String chromaPath, FitnessType type=FitnessType.MIN) {
        this.type = type
        interpreter = new MirChordInterpreter([])
        dict_chroma = loadObjectFromBinaryFile( chromaPath ) as Compressor<Integer>
    }

    float rate(Individual individual) {
        String source = individual.blueprint
        float fitness = (type == FitnessType.MAX) ? Float.MIN_VALUE : Float.MAX_VALUE
        if (source in ["<ABORTED>", "<NULL>"])
            return fitness

        try {
            Score score = interpreter.evaluate(source)
            List<Chord> chords = score.parts[0].voices[0].getChords()
            var score_chroma = [Integer.MIN_VALUE]
            int transposition = 0  // change if we are not in Cmaj/Amin
            for(Chord item: chords) {
                int midiValue = item.pitch.getMidiValue()
                if (transposition > 0)
                    midiValue -= transposition
                def chroma = midiValue % 12
                score_chroma <<  chroma
            }

            var xy_c = new Compressor<Integer>(dict_chroma, score_chroma)
            var compr_chroma = new Compressor<Integer>(score_chroma)

            fitness = (xy_c.len - Math.min(dict_chroma.len, compr_chroma.len)) / Math.max(dict_chroma.len, compr_chroma.len)
            println "fitness=$fitness"

        } catch(Exception ex) {
            println "==========================================================="
            println "abort ${source}"
            println "==========================================================="
            ex.printStackTrace()
            throw new Exception(ex.message)
        }
        return fitness
    }
}


def grammarText = getClass().getResourceAsStream("mirgrams/score.mirgram").getText()
def gr = new MirGram(grammarText, "score", 3)
def fit = new MelodyFitness(projectPath.resolve("data/chroma.ser").toString())
def ge = new MirGene(gr,fit).with {
    populationSize = 50
    genomeSize = 400
    maxGenerations = 1
    eliteSize = 15
    mutationProbability = 0.1f
    it
}

if (fileExists( projectPath.resolve("data/population.ser").toString() )) {
    println "Load population"
    ge.population = loadObjectFromBinaryFile(projectPath.resolve("data/population.ser").toString() ) as List<IndividualFitness>
}
def best = ge.runGE()
println best.individual.blueprint.trim()
println best.fitness
saveObjectToBinaryFile(ge.population, projectPath.resolve("data/population.ser").toString() )
