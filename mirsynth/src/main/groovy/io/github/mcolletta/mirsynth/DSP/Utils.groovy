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

class Utils {
	
	// Generators------------------
	
	static float saw(double t, double T) {
		double _t = t % T
		return (_t / T) * 2.0f - 1.0f
	}
	
	static float triangle(double t, double T) {
		double _t = t % T
		double T2 = T / 2
		if (_t < T2)
			return (_t / T2) * 2.0f - 1.0f
		else
			return 1.0 - ( (_t - T2) / T2 * 2 - 1 )
	}
	
	static float square(double t, double T) {
		double _t = t % T
		double T2 = T / 2
		if (_t < T2)
			return 1.0
		else
			return -1.0
	}
	
	static float pulse(double t, double T, float ratio=0.5f) {
		double _t = t % T
		double TR = T * ratio
		if (_t < TR)
			return 1.0
		else
			return -1.0
	}
	
	static float impulse(double t) {
		if (t == 0)
			return 1.0
		return 0.0
	}
	
	static float fm(double t, float A, float f0, float Ic, float fc) {
		return A * sin( 2 * PI * f0 * t + (Ic * sin( 2 * PI * fc * t)) )
	}
	
	static float dfm(double t, float A, float I1, float I2, float f1, float f2) {
		return A * sin( (I1 * sin(2 * PI * f1 * t)) + (I2 * sin(2 * PI * f2 * t)) )
	}
	
	
	
	//-----------------------------
	
	static float midiToFrequency(double d) {
		return pow(2, (d - 6900) / 1200) * 440
	}
	
	static double millisToSamples(double millis, float sampleRate) {
		return millis * (sampleRate / 1000.0)
	}
	
	static double samplesToMs(double samples, float sampleRate) {
		return (samples / sampleRate) * 1000.0
	}
	
	static double root(double num, double root)
	{
		Math.pow(Math.exp (1/root),Math.log(num))
	}
	
	static int log(double x, double base)
	{
		return (int) (Math.log(x) / Math.log(base));
	}

}
