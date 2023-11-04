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

import static java.lang.Math.*

class MirSynth {
	
	Boolean isFrequencySetted = false
	float f0
	float Fs = 44100.0
	double Ts = 2.2675736961451248E-5

	MirSynth() {}
	
	void setSampleRate(float samplerate) {
		Fs = samplerate
		Ts = 1/Fs
	}
	
	void setup(Map params) {
		// initialize
	}
	
	void setFrequency(float f0) {
		if (!isFrequencySetted) {
			this.f0 = f0
			isFrequencySetted = true
		}
	}
	
	float process(int n, double t) {
		// return (float)sin(2.0 * PI * f0 * t)
		return 0.0f
	}
	
}
