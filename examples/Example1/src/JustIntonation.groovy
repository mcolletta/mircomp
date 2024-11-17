// 1:1 	9:8 	5:4 	4:3 	3:2 	5:3 	15:8 	2:1 

float[] ratios = [ 1,
                   9/8,
                   5/4,
                   4/3,
                   3/2,
                   5/3,
                   15/8,
                   2
                 ]

float f0 = 261.62558

List<Float> frequencies = []
for (int i = 0; i < 8; i++) {
    frequencies[i] = f0 * ratios[i]
}
println frequencies
MidiPlayer.playFrequencies(frequencies, [1000f])