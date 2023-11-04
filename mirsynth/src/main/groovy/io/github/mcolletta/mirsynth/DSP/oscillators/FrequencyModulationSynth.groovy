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

import java.util.List;
import java.util.Map;

import static io.github.mcolletta.mirsynth.Utils.*
import static java.lang.Math.*


class FrequencyModulationSynth extends MirSynth {
	
	FrequencyModulationSynth() {}
	
	Envelope amplitude
	float A
	float Ic
	float fc
	float ratio
	
	
	void setup(Map params) {
		A = (float)params['A']
		amplitude = new Envelope((List)params['envelope'])
		Ic = (float)params['Ic']
		ratio = ((int)params['N2']) / ((int)params['N1'])
	}
	
	void setFrequency(float f0) {
		if (!isFrequencySetted) {
			this.f0 = f0
			isFrequencySetted = true
			this.fc = (ratio != 0.0f) ? (float)(f0 * ratio) : 0.0f
			// TODO: new amplitude
		}
	}
	
	float process(int n, double t) {
		float _A = A
		if (amplitude != null)
			_A *= amplitude.tick()
		return fm(t, _A, f0, Ic, fc)
	}

}
