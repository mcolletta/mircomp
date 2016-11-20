/*
 * Copyright (C) 2016 Mirco Colletta
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

import groovy.transform.CompileStatic

@CompileStatic
class Utils {

	static Fraction f1 = fr(4, 4)
	static Fraction f3 = fr(3, 4)
	static Fraction f2 = fr(2, 4)
	static Fraction f4 = fr(1, 4)
	static Fraction f8 = fr(1, 8)
	static Fraction f16 = fr(1, 16)
	static Fraction f32 = fr(1, 32)
	static Fraction f64 = fr(1, 64)
	static Fraction f128 = fr(1, 128)
	static Fraction f256 = fr(1, 256)

	static Fraction f8dot = f8.add(f16)
	static Fraction f8dotdot = f8dot.add(f32)
	static Fraction f4dot = f4.add(f8)
	static Fraction f4dotdot = f4dot.add(f16)
	static Fraction f2dot = f2.add(f4)
	static Fraction f2dotdot = f2dot.add(f8)

	static Fraction f6 = fr(1, 6)
	static Fraction f12 = fr(1, 12)

	static boolean isPowerOfTwo(long n) {
		return (n != 0) && ((n & (n - 1)) == 0)
	}

	static Map<Float,Fraction> allowedDurations = [
		0.00390625f: f256,
		0.0078125f: f128,
		0.015625f: f64,
		0.0625f: f16,
		0.083f: f12, // triplet?
		0.125f: f8,
		0.17f: f6, // triplet?
		0.1875f: f8dot,
		0.25f: f4,
		0.375f: f4dot,		
		0.5f: f2,
		0.75f: f3,
		1.0f: f1
 	]


	static float nearest(Set<Float> list, float number) {
		return list.sort { (it - number).abs() }.first()
	}

	static Fraction getDurationFromDecimal(float num) {
		float key = nearest(allowedDurations.keySet(), num)
		return allowedDurations[key]
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