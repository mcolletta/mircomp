/*
 * Copyright (C) 2016-2017 Mirco Colletta
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

package io.github.mcolletta.mircomp.utils

import java.util.Random
//import java.util.concurrent.ConcurrentSkipListMap

import com.xenoage.utils.math.Fraction
import static com.xenoage.utils.math.Fraction.fr
import static com.xenoage.utils.math.Fraction._0

class MChain {

	// ex. matrix[72][74] = 0.5
	def matrix

	MChain(events, elementType='mel', val='transposed', zero=0) {
		matrix = [:].withDefault { [:].withDefault { 0 } }
		def lastValue = null
		events.each { k, v ->
			def el = v[elementType]
			if (el != null) {
				if (lastValue != null && lastValue > zero)
					matrix[lastValue][el[val]] += 1
				lastValue = el[val]
			}
		}
		normalizeMatrix()
		println matrix
	}

	void normalizeMatrix() {
		matrix.each { k, v ->
			// v is a dict ex. v[74] = 0.5
			def tot = v.values().sum()
			v.each { k1, v1 ->
				matrix[k][k1] /= tot
			}
		}
	}

	// TODO lazy iterator
	def generate(n, start=52) {
		Random rand = new Random()
		def prev = start
		def list =[]

		(1..n).each { 
			//float r = rand.nextFloat() // [0;1)
			float r = 1.0f - rand.nextFloat() // (0;1]
			float countWeight = 0.0f
			def el = null
			for(kvp in matrix[prev]) {
				def k = kvp.key
				def v = kvp.value
				countWeight += v	        	
	        	if (countWeight >= r) {
	                el = k
	                prev = k
	                break
	            }	
			}
	        if (el)
	            list << el
	    }
	    // println list
	    return list
	}

}