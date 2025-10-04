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


class Complex {
    float Re
    float Im
    static final j = new Complex(0.0f, 1.0f)
     
    Complex(float re, float im) { 
		Re = re
		Im = im 
	}
    
    Complex plus(Complex c) {
		return new Complex(Re + c.Re, Im + c.Im) 
    }

    Complex minus(Complex c) {
		return new Complex(Re - c.Re, Im - c.Im)
	}
    
    Complex multiply(Complex c) {
		return new Complex(Re * c.Re - Im * c.Im, Re * c.Im + Im * c.Re)
	}
    
    Complex div(Complex c) {
        float denom = (float) (c.Re ** 2 + c.Im ** 2)
        return new Complex((float) ((Re * c.Re + Im * c.Im) / denom), (float) ((Im * c.Re - Re * c.Im) / denom))
    }
    
    float abs() {
		return (float)Math.sqrt((float) (Re ** 2 + Im ** 2))
	}

    String toString() { 
		return Re + (Im >= 0 ? "+" : "") + Im + "j"
	}
}

