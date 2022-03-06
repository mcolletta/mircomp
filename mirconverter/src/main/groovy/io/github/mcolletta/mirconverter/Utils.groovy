/*
 * Copyright (C) 2016-2022 Mirco Colletta
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

package io.github.mcolletta.mirconverter

import com.xenoage.zong.core.music.time.Time
import com.xenoage.zong.core.music.time.TimeType

import com.xenoage.utils.math.Fraction
import static com.xenoage.utils.math.Fraction.fr
import static com.xenoage.utils.math.Fraction._0

import static io.github.mcolletta.mirchord.core.Utils.*


class Utils {

	static boolean isPowerOfTwo(long n) {
		return (n != 0) && ((n & (n - 1)) == 0)
	}

	// AUTOMATIC BEAMING
	static Map<Integer, Map<String, Fraction>> pulses_4_4 = (Map<Integer, Map<String, Fraction>>)[1: ['l': _0, 'r': f4], 
																							      2: ['l': f4, 'r': f2], 
																							      3: ['l': f2, 'r': f3], 
																							      4: ['l': f3, 'r': f1]]

	static Map<Integer, Map<String, Fraction>> pulses_2_4 = (Map<Integer, Map<String, Fraction>>)[1: ['l': _0, 'r': f4], 
						     								 									  2: ['l': f4, 'r': f2]]

	static Map<Integer, Map<String, Fraction>> pulses_3_4 = (Map<Integer, Map<String, Fraction>>)[1: ['l': _0, 'r': f4dot], 
						     								 									  2: ['l': f4dot, 'r': f3]]	


	static Map<Integer, Map<String, Fraction>> getPulses(Time time) {
		Map<Integer, Map<String, Fraction>> pulses = [:]
		TimeType timeType = time.getType()
		float ratio = (float)(timeType.numerator / timeType.denominator)
		switch(timeType) {
			case { timeType == TimeType.time_4_4 || ratio == 1.0f}:
				pulses = pulses_4_4
				break
			case { timeType == TimeType.time_2_4 || ratio == 0.5f}:
				pulses = pulses_2_4
				break
			case { timeType == TimeType.time_3_4 || ratio == 0.75f}:
				pulses = pulses_3_4
				break
			default:
				break
		}
		return pulses
	}

	static int getChordPulse(Fraction startBeat, Fraction endBeat, 
							 Map<Integer, Map<String, Fraction>> pulses) {
		int pulse = -1
		for (Map.Entry<Integer, Map<String, Fraction>> entry : pulses.entrySet()) {
			int k = entry.getKey()
			Map<String, Fraction> v = entry.getValue()
		    if(startBeat >= v['l'] && endBeat <= v['r']) {
		    	pulse = k
		    	continue
		    }
		}
		if (pulse < 0)
			pulse = pulses.keySet().last()
		return pulse
	}

	static int getBeatPulse(Fraction beat, Map<Integer, Map<String, Fraction>> pulses) {
		int pulse = -1
		for (Map.Entry<Integer, Map<String, Fraction>> entry : pulses.entrySet()) {
			int k = entry.getKey()
			Map<String, Fraction> v = entry.getValue()
		    if(beat >= v['l'] && beat < v['r']) {
		    	pulse = k
		    	continue
		    }
		}
		if (pulse < 0)
			pulse = pulses.keySet().last()
		return pulse
	}    
}