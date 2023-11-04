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

class Filter {
	
	int na, nb
	float x, y
	float[] xn, yn, b, a
	float b1
	
	public Filter(int nb, int na) {
		this.nb = nb
		this.na = na
		if (nb > 0)
			xn = new float[nb]
		else
			xn = []
		if (na > 0)
			yn = new float[na]
		else
			xn = []
		Arrays.fill(xn, 0)
		Arrays.fill(yn, 0)
		b = new float[nb + 1]
		a = new float[na + 1]
	}
	
	public float tick(float x) {
		y = 0
		int k = 0
	  
		// process
		for(k = 0; k < nb; k++)
		  y += b[k + 1] * xn[k]
		y += b[0] * x
		for(k = 0; k < na; k++)
		  y -= a[k + 1] * yn[k]
		
			  
		// update for next tick
		for(k = 1; k < nb; k++)
		  xn[k] = xn[k-1]
		xn[0] = x
		for(k = 1; k < na; k++)
		  yn[k] = yn[k-1]
		yn[0] = y
		
		return y
	}
	
}


class LPAverageFilter {
	float[] xn
	float y
	
	LPAverageFilter() {
		xn = new float[2]
		Arrays.fill(xn, 0)
		y = 0
	}
	
	public float tick(float x) {
		xn[1] = xn[0]
		xn[0] = x
		y = 0.5f * (xn[0] + xn[1])
		return y
	}
}

class OnePoleFilter extends Filter {
	// http://en.wikipedia.org/wiki/Digital_filter
	// https://ccrma.stanford.edu/~jos/fp/One_Pole.html
	// y[n] = b[0]x[n] - a[1]y[n-1]
	OnePoleFilter(float coeff) {
		super(1, 2)
		a[0] = 1
		a[1] = coeff
		b[0] = 1 + coeff
	}
	
}


