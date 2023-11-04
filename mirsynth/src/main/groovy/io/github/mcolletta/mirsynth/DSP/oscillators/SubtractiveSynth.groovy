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

import static io.github.mcolletta.mirsynth.Utils.*
import static java.lang.Math.*


class SubtractiveSynth extends MirSynth {
	
	SubtractiveSynth() {}
	String osc
	Filter filter
	float Q
	float cutoff
	
	void setup(Map params) {
		osc = ((String)params['osc']).toLowerCase()
		if (!osc in ['saw', 'triangle', 'square', 'pulse'])
			throw new Exception("The type $osc is not allowed. Try: saw, triangle, square or pulse") 
		Q = (float)params['Q']
		cutoff = (float)params['cutoff']
		filter = new BiquadLP(cutoff, Q, Fs)
	}
	
	void setFrequency(float f0) {
		if (!isFrequencySetted) {
			this.f0 = f0
			isFrequencySetted = true
			
			filter = new BiquadLP(cutoff, Q, Fs)
		}
	}
	
	float process(int n, double t) {
		double T = 1 / f0
		float x
		switch(osc) {
			case "saw":
				x = saw(t,T)
				break
			case "triangle":
				x = triangle(t,T)
				break
			case "square":
				x = square(t,T)
				break
			case "pulse":
				x = pulse(t,T,0.25f)
				break
		}
		
		return filter.tick(x)
	}

}
