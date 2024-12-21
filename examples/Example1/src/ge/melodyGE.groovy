public class MelodyFitness extends FitnessFunction {

    MirChordInterpreter interpreter
    Compressor<Integer> dict_intervals
    Compressor<Fraction> dict_ratios

    MelodyFitness(String intervalsPath, String ratiosPath, FitnessType type=FitnessType.MIN) {
        this.type = type
        interpreter = new MirChordInterpreter([])
        dict_intervals = loadObjectFromBinaryFile( intervalsPath ) as Compressor<Integer>
        dict_ratios = loadObjectFromBinaryFile( ratiosPath ) as Compressor<Fraction>
    }

    float rate(Individual individual) {
        String source = individual.blueprint
        float fitness = (type == FitnessType.MAX) ? Float.MIN_VALUE : Float.MAX_VALUE
        if (source in ["<ABORTED>", "<NULL>"])
            return fitness

        try {
            Score score = interpreter.evaluate(source)
            List<Chord> chords = score.parts[0].voices[0].getChords()
            var score_intervals = [Integer.MIN_VALUE] 
            int lastValue = -1
            for(Chord item: chords) {
                int midiValue = item.pitch.getMidiValue()
        		if (lastValue > 0)
        			score_intervals <<  midiValue - lastValue
        		lastValue = midiValue
            }
            var score_ratios = [_0]
            Fraction currDuration = _0
            for(Chord item: chords) {
                Fraction duration = item.duration
        		if (currDuration > _0)
        			score_ratios << duration.divideBy(currDuration)
        		currDuration = duration
            }

            var xy_i = new Compressor<Integer>(dict_intervals, score_intervals)
            var xy_r = new Compressor<Fraction>(dict_ratios, score_ratios)
            
             var compr_intervals = new Compressor<Integer>(score_intervals)
            var compr_ratios = new Compressor<Fraction>(score_ratios)
            
            float NCD_ints = (xy_i.len - Math.min(dict_intervals.len, compr_intervals.len)) / Math.max(dict_intervals.len, compr_intervals.len)
            float NCD_ratios = (xy_r.len - Math.min(dict_ratios.len, compr_ratios.len)) / Math.max(dict_ratios.len, compr_ratios.len)
            fitness = 0.5 * NCD_ints + 0.5 * NCD_ratios
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
def fit = new MelodyFitness(projectPath.resolve("data/intcompr.ser").toString(), projectPath.resolve("data/durcompr.ser").toString())
def ge = new MirGene(gr,fit,100L).with {
	populationSize = 50
	genomeSize = 1000
	maxGenerations = 1
	it
}
File f = new File( projectPath.resolve("data/population.ser").toString() );
if(f.exists() && !f.isDirectory()) {
    println "Load population"
    ge.population = loadObjectFromBinaryFile(projectPath.resolve("data/population.ser").toString() ) as List<IndividualFitness>
}
def best = ge.runGE()
println best.individual.blueprint.trim()
println best.fitness
saveObjectToBinaryFile(ge.population, projectPath.resolve("data/population.ser").toString() )