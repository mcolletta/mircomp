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

import static Utils.*
import static java.lang.Math.*

import com.sun.media.sound.ModelAbstractOscillator


class MirOscillator extends ModelAbstractOscillator {
	
	int last_n = 0
	MirSynth mirsynth
	
	public int read(float[][] buffers, int offset, int len) throws IOException {
		double Ts = 1/getSampleRate()
		float f0 = midiToFrequency(getPitch())
		mirsynth.setSampleRate(getSampleRate())
		if (!mirsynth.isFrequencySetted)
			mirsynth.setFrequency(f0)
		// Grab channel 0 buffer from buffers
		float[] buffer = buffers[0];

		for (int i = 0; i < len; i++) {
			int n = i + last_n
			double t = n * Ts
			buffer[i] = (mirsynth != null) ? mirsynth.process(n, t) : 0.0f
		}
		last_n += len
		
		return len
	}

}
