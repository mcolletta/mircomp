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

import io.github.mcolletta.mirsynth.*
import static io.github.mcolletta.mirsynth.Utils.midiToFrequency


class FrequencyModulationOscillator extends MirOscillator {
			
	FrequencyModulationOscillator() {
		super()
		mirsynth = new FrequencyModulationSynth()
		mirsynth.setF0(midiToFrequency(getPitch()))
		mirsynth.setSampleRate(getSampleRate())
		mirsynth.setup([A: 1.5, envelope:[ [1.0, 200], [0.9, 100], [0.7, 300], [0.0, 100]  ], Ic:1.5f, N1:3, N2:1])
	}

}