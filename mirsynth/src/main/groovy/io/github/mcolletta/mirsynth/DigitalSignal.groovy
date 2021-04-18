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


abstract class DigitalSignal {

    float[][] XY
    float[][] XYfft
    float[][] XYfft_phase

	int N = 512       // Must be a power of two
    float Ts = 1.0f
	float Fs = 1.0f   // Sampling rate: 1/T

	float pi = (float)PI
	float one_on_2pi = (float)(1.0f/(2*PI))
    
	DigitalSignal() { }

    DigitalSignal(int N, float Ts) {
    	this.N = N
		this.Ts = Ts
		this.Fs = (float) (1/Ts)
	}

	abstract float sample(int n)
    
    void process() {
        float[] data = new float[N]
        
        XY = new float[N][2]
        for (int n = 0; n < XY.length; n++) {
                XY[n][0] = n
                XY[n][1] = sample(n)
                data[n] =  XY[n][1]
        }
        
		Map<String, float[]> dict = FastFourierTransform.getMagnitudeAndPhase(data)
		float[] M = dict["magnitude"]
		float[] P = dict["phase"]
        // float[] M = FastFourierTransform.getMagnitudeFromReals(data)
        
        XYfft = new float[N][2]
		XYfft_phase = new float[N][2]
        for (int k = 0; k < XYfft.length; k++) {
			float time_step = (float) (k / N) * Fs
			// magnitude
	        XYfft[k][0] = time_step
            XYfft[k][1] = M[k]
			// phase
			XYfft_phase[k][0] = time_step
            XYfft_phase[k][1] = P[k]
        }
    }

	static float[][] convolution(float[][] XY1, float[][] XY2) {
		assert XY1.length == XY2.length
		assert XY1[0].length == 2
		assert XY2[0].length == 2
		int N = XY1.length
		float[][] XYconv = new float[N][2]
		for (int k = 0; k < N; k++) {
	        XYconv[k][0] = XY1[k][0]
			float y = 0.0f
			for (int m = 0; m < N; m++) {
            	y += XY1[m][1] * XY2[k - m][1]
			}
			XYconv[k][1] = (float) (y / N)
        }
		return XYconv
	}  
}


// SIGNALS
class RectangularWindowSignal extends DigitalSignal {
	float sample(int n) {    
       return (n <= (N/2)) ? 1.0f : 0.0f
    }
}

class CosineSignal extends DigitalSignal {

	float A = 1.0f     // Sinusoidal amplitude
    float phi = 0f     // Sinusoidal phase
    float f = 0.25f    // Frequency (cycles/sample)

	CosineSignal() { process() }

	float sample(int n) { 
		return A * cos( 2*PI*f * n*Ts + phi)
    }
}

class WindowedCosineSignal extends DigitalSignal {

	float A = 1.0f
    float phi = 0f
    float f = 0.25f

	WindowedCosineSignal() { process() }

	float sample(int n) {    
		if (n <= (N/2))
            return A * cos( 2*PI*f * n*Ts + phi) 
        else
            return 0
    }
}

class HannWindowedCosineSignal extends DigitalSignal {

	float A = 1.0f
    float phi = 0f
    float f = 0.25f

	HannWindowedCosineSignal() { process() }
	
	float sample(int n) {
		// Hann window: 0.5 (cos(\frac{2\pi n}{N-1}))
        float w = 0.5 * (1 - cos( (2 * PI * n) / (N/2) - 1))
        if (n <= (N/2)) {
            return A * cos( 2*PI*f * n*Ts + phi) * w
        }
        else
            return 0.0f
	}
}

class DoubleFrequencyModulation extends DigitalSignal {
	float A
    float I1
	float I2
    float f
	float N1
	float N2

	DoubleFrequencyModulation(float A, float I1, float I2, float f, int N1, int N2, 
							  int N=512, float Ts=(float)(1.0f/(2*PI))) {
		super(N, Ts)
		this.A = A
		this.I1 = I1
		this.I2 = I2
		this.f = f
		this.N1 = N1
		this.N2 = N2

		pi =  (float)PI
	}

	float sample(int n) {
		float t = n * Ts
		float x = (float) ( A * sin( I1 * sin(2 * pi * f * N1 * t) + I2 * sin(2 * pi * f * N2 * t) ) )
		return x
	}

}

//FILTERS
class AverageFilter extends DigitalSignal {
	float prev = 0.0f

	float sample(int n) {
	   float x_n = (n == 0) ? 1.0f : 0.0f // impulse
	   float avg = (float) ((x_n +  prev) / 2)
       prev = x_n
       return avg
    }
}

class TwoPolesFilter extends DigitalSignal {
	float b_0
	float R
    float fc
	float omega_0
	float a_1
	float a_2
	float[] y = [ 0.0f, 0.0f ] as float[]

	TwoPolesFilter(int N=512, float Ts=(float)(1.0f/(2*PI)), 
				   float b_0=1.0f, float R=0.9f, float fc=0.0f) {
		super(N, Ts)
		this.b_0 = b_0
		this.R = R
		this.fc = fc
		if (fc == 0.0f) {
			this.fc = (float) (Fs/4)
			omega_0 = (float)(PI/4) 
		} else {
			omega_0 = (float) (2 * PI * fc * Ts)
		}
		a_1 = -2 * R * (float)Math.cos(omega_0)
		a_2 = R * R
	}

	float sample(int n) {
	   float x_n = (n == 0) ? 1.0f : 0.0f // impulse
	   x_n *= Ts
	   float y_n = b_0 * x_n - a_1 * y[1] - a_2 * y[0]
	   y[0] = y[1]
	   y[1] = y_n
       return y_n
    }
}

class TwoZerosFilter extends DigitalSignal {
	float b_0
	float b_1
	float b_2
	float R
    float fc
	float omega_0	
	float[] x = [ 0.0f, 0.0f ] as float[]

	TwoZerosFilter(int N=512, float Ts=(float)(1.0f/(2*PI)), 
				   float b_0=1.0f, float R=0.9f, float fc=0.0f) {
		super(N, Ts)
		this.b_0 = b_0
		this.R = R
		this.fc = fc
		if (fc == 0.0f) {
			this.fc = (float) (Fs/4)
			omega_0 = (float)(PI/4) 
		} else {
			omega_0 = (float) (2 * PI * fc * Ts)
		}
		b_1 = -2 * R * (float)Math.cos(omega_0) * b_0
		b_2 = R * R * b_0
	}

	float sample(int n) {
	   float x_n = (n == 0) ? 1.0f : 0.0f // impulse
	   x_n *= Ts
	   float y_n = b_0 * x_n + b_1 * x[1] + b_2 * x[0]
	   x[0] = x[1]
	   x[1] = x_n
       return y_n
    }
}

