/*
 * Copyright (C) 2016-2023 Mirco Colletta
 *
 * This file is part of MirComp.
 *
 * MirComp is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MirComp is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MirComp.  If not, see <http://www.gnu.org/licenses/>.
*/

/**
 * @author Mirco Colletta
 */


package io.github.mcolletta.mirsynth

import java.util.Map;

import io.github.mcolletta.mirsynth.MirSynth
import static io.github.mcolletta.mirsynth.Utils.*
import static java.lang.Math.*


class DoubleFrequencyModulationSynth extends MirSynth {
	
	/*DoubleFrequencyModulationSynth(float freq, float samplerate)  {
		super(freq, samplerate)
	}*/
	
	DoubleFrequencyModulationSynth() {}
	
	List<Float> A = []
	List<Float> I1 = []
	List<Float> I2 = []
	List<Float> N1 = []
	List<Float> N2 = []
	
	int numOperators = 0
	
	void setup(Map params) {
		List operators = (List)params['operators']
		numOperators = operators.size()
		operators.each { p ->
			A.add((float)p['A']) 
			I1.add((float)p['I1'])
			I2.add((float)p['I2'])
			N1.add((float)p['N1'])
			N2.add((float)p['N2'])
		}
	}
	
	float process(int n, double t) {
		float sum = 0.0f
		(0..numOperators-1).each { i ->
			float f1 = f0 * N1[i]
			float f2 = f0 * N2[i]
			sum += dfm(t, A[i], I1[i], I2[i], f1, f2)
		}
		return sum
	}

}
