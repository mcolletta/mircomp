/*
 * Copyright (C) 2016-2021 Mirco Colletta
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


class FastFourierTransform {

    static Complex[] FFT(Complex[] x) {
        int N = x.length
		int N2 = (int) (N/2)
        if (N == 1)
            return x
        def WN = new Complex((float) cos(2 * PI / N), (float) -sin(2 * PI / N))
        def W = new Complex(1.0f, 0.0f)
        Complex[] x0 = new Complex[N2]
        Complex[] x1 = new Complex[N2]
		int i0 = 0
		int i1 = 0
		for(int i = 0; i < x.length; i++) {
			Complex c = x[i]
             if (i % 2 == 0)
                x0[i0++] = c
             else
                x1[i1++] = c
        }
        Complex[] X0 = FFT(x0)
        Complex[] X1 = FFT(x1)
        Complex[] X = new Complex[x.length]
		for(int i = 0; i < X.length; i++) {
			X[i] = new Complex(0.0f, 0.0f)
		}

		for(int k = 0; k < N2; k++) {
            X[k] = X0[k] + W * X1[k]
            X[k + N2] = X0[k] - W * X1[k]
            W = W * WN
        }
        return X
    }

	static Map<String, float[]> getMagnitudeAndPhase(float[] reals) {
        Complex[] x = new Complex[reals.length]
        for(int i = 0; i < reals.length; i++) {
			float real = reals[i]
            x[i] = new Complex(real, 0.0f)
        }

        Complex[] X = FFT(x)

		Map<String, float[]> dict = [:]

        float[] magnitude = new float[X.length]
        float[] phase = new float[X.length]
		for(int i = 0; i < X.length; i++) {
        	Complex c = X[i]
			magnitude[i] = c.abs()
			phase[i] = (float) atan2(c.Im, c.Re)
		}
        dict['magnitude'] = magnitude
        dict['phase'] = phase
		return dict
    }
    
    static float[] getMagnitudeFromReals(float[] reals, boolean squared=false) {
        Complex[] x = new Complex[reals.length]
        for(int i = 0; i < reals.length; i++) {
			float real = reals[i]
            x[i] = new Complex(real, 0.0f)
        }

        Complex[] X = FFT(x)

        float[] magnitude = new float[X.length]
		for(int i = 0; i < X.length; i++) {
        	Complex c = X[i]
			magnitude[i] = c.abs()
		}
        return magnitude
    }
}
