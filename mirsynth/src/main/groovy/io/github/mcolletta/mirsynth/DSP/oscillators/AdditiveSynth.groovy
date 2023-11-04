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

import io.github.mcolletta.mirsynth.*
import static io.github.mcolletta.mirsynth.Utils.*
import static java.lang.Math.*


class AdditiveSynth extends MirSynth {
	
	AdditiveSynth() {}
	
	Map<Integer, Float> amplitudes
	
	void setup(Map params) {
		amplitudes = [:]
		int overtone = 1
		params['amplitudes'].each { float I ->
			amplitudes.put(overtone, I)
		}
		 
	}
		
	float process(int n, double t) {
		float sum = 0
		amplitudes.each { ot, I ->
			sum += I * (float)sin( 2 * PI * ot * f0 * t)
		}
		return sum
	}

}

