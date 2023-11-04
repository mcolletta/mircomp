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

/**
 * For all BiquadFilter: coefficients calculated from the equations presented in:
 * "Cookbook formulae for audio EQ biquad filter coefficients" - Robert Bristow-Johnson
 */

/*


            b0 + b1*z^-1 + b2*z^-2
    H(z) = ------------------------                                  (Eq 1)
            a0 + a1*z^-1 + a2*z^-2


            (b0/a0) + (b1/a0)*z^-1 + (b2/a0)*z^-2
    H(z) = ---------------------------------------                   (Eq 2)
               1 + (a1/a0)*z^-1 + (a2/a0)*z^-2



    y[n] = (b0/a0)*x[n] + (b1/a0)*x[n-1] + (b2/a0)*x[n-2]
                        - (a1/a0)*y[n-1] - (a2/a0)*y[n-2]            (Eq 4)



 */

class Biquad extends Filter {
	
	float b0, b1, b2
	float a0, a1, a2
	
	float Fs
	float f0
	float Q = 1
	float alpha
	float w0
	float cosw0
	float sinw0
	
	
	Biquad(float f0, float Q=1.0f, float Fs=44100.0f) {
		super(2, 2)
		this.f0 = f0
		this.Q = Q
		this.Fs = Fs
		
		w0 = (float) ( (2 * PI * f0) / Fs )
		cosw0 = (float) cos(w0)
		sinw0 = (float) sin(w0)
		alpha = (float) ( sin(w0) / (2 * Q) )
		
	}
	
	void setupCoefficents() {
		if (a0 != 0) {
			b[0] = (float) ( b0 / a0 )
			b[1] = (float) ( b1 / a0 )
			b[2] = (float) ( b2 / a0 )
			
			a[1] = (float) ( a1 / a0 )
			a[2] = (float) ( a2 / a0 )
		}
	}
	
}

class BiquadLP extends Biquad {
	
	
	
	BiquadLP(float f0, float Q, float Fs=44100.0f) {	
		super(f0, Q, Fs)	
		b0 =  (float) ( (1 - cosw0) / 2 )
		b1 =   1 - cosw0
		b2 =  (float) ( (1 - cosw0) / 2 )
		a0 =   1 + alpha
		a1 =  -2 * cosw0
		a2 =   1 - alpha
		setupCoefficents()
	}
	
}

class BiquadHP extends Biquad {
	
	BiquadHP(float f0, float Q, float Fs=44100.0f) {	
		super(f0, Q, Fs)
		b0 =  (float) ( (1 + cosw0) / 2 )
		b1 = -(1 + cosw0)
		b2 =  (float) ( (1 + cosw0) / 2 )
		a0 =   1 + alpha
		a1 =  -2 * cosw0
		a2 =   1 - alpha
		setupCoefficents()
	}
}


class BiquadBP1 extends Biquad {
	
	BiquadBP1(float f0, float Q, float Fs=44100.0f) {
		super(f0, Q, Fs)
		b0 =   (float) ( sinw0 / 2 )
		b1 =   0
		b2 =  (float) ( -sinw0 / 2 )
		a0 =   1 + alpha
		a1 =  -2 * cosw0
		a2 =   1 - alpha
		setupCoefficents()
	}
}


class BiquadBP2 extends Biquad {
	
	BiquadBP2(float f0, float Q, float Fs=44100.0f) {
		super(f0, Q, Fs)
		b0 =   alpha
		b1 =   0
		b2 =  -alpha
		a0 =   1 + alpha
		a1 =  -2 * cosw0
		a2 =   1 - alpha
		setupCoefficents()
	}
}

class BiquadNotch extends Biquad {
	
	BiquadNotch(float f0, float Q, float Fs=44100.0f) {
		super(f0, Q, Fs)
		b0 =   1
		b1 =  -2 * cosw0
		b2 =   1
		a0 =   1 + alpha
		a1 =  -2 * cosw0
		a2 =   1 - alpha
		setupCoefficents()
	}
}
