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

package io.github.mcolletta.mircoracle

import java.io.IOException
import java.util.Random


class OracleNavigator {
	
	FactorOracle oracle
	int continuity
	Random rand
	
	OracleNavigator(FactorOracle oracle, int continuity = 3) {
		this.oracle = oracle
		this.continuity = continuity
		rand = new Random()
	}
	
	List navigate(int start, int length) {
		int size = oracle.sequence.size()
		List sequence = []
		int current = start
		int count = 0
		while (count < length) {
			//println "count $count"
			
			if (current % continuity == 1 || (current + 1) >= size) {
				Set<Integer> candidates = []
				candidates << (current + 1) % size
				if (oracle.getSuffixLink(current) >= 0)
					candidates << oracle.getSuffixLink(current)
				oracle.getReverseSuffixLink(current).each { int k ->
					candidates << k
				}
				//println "candidates= $candidates"
				if (candidates.size() > 0) {
					int choice = rand.nextInt(candidates.size())
					current = candidates[choice]
					//println "chosen $current"
				}
			} else {
				current = (current + 1) % size
			}
			
			def symbol = oracle.sequence[current]
			if (symbol == null)
				println "" + current + " size " + oracle.sequence.size()
			sequence << symbol
			count += 1
		}
		return sequence
	}
	
}