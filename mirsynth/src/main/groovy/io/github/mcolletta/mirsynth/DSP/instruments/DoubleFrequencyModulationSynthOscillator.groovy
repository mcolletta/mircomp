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


class DoubleFrequencyModulationOscillator extends MirOscillator {
			
	DoubleFrequencyModulationOscillator() {
		super()
		mirsynth = new DoubleFrequencyModulationSynth()
		mirsynth.setF0(midiToFrequency(getPitch()))
		mirsynth.setSampleRate(getSampleRate())
		mirsynth.setup([operators: 
							[ 
								[A:0.537f, I1:2.373f, I2:1.637f, N1:1, N2:2],
								[A:1.073f, I1:4.379f, I2:0.512f, N1:1, N2:2],
								[A:3.437f, I1:0.747f, I2:0.190f, N1:2, N2:1]
							]
						 ])
	}

}
