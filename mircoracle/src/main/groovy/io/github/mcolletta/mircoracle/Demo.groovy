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

import io.github.mcolletta.mircoracle.*

import java.io.IOException


public class Demo {

	static List rotate(List items) {
		 items = items[1..-1] + items[0]
		 return items
	 }

	public static void main(String[] args) throws IOException {
		exampleOracle()
		exampleIP()
	}

	static exampleOracle() {
		println "testOracle: --------------------------------------------------"

		def seq = "aabbabbabbab".toList()
		
		Comparator<String> comparator = [
			compare:{ String a, String b-> b.toLowerCase() <=> a.toLowerCase() }
		] as Comparator<String>

		def fo = new FactorOracle(seq,comparator)

		println fo.toString()

		def navigator = new OracleNavigator(fo)
		List generatedSeq = navigator.navigate(0,100)
		println "GENERATED SEQUENCE"
		println generatedSeq
	}

	static exampleIP(boolean rotate_seq = false) {
		println "testIP: --------------------------------------------------"

		def seq = ['a', 'b', 'a', 'b', 'a', 'b', 'c', 'a', 'b', 'd', 'a', 'b', 'c', 'd', 'a', 'b', 'c', 'e']

		Comparator <String>comparator = [
			compare:{ String a, String b-> b.toLowerCase() <=> a.toLowerCase() }
		] as Comparator<String>


		if (rotate_seq) {
			int num_rotate = 4
			for(int i=0; i<num_rotate; i++) {
				List<String> rot_seq = rotate(seq)
				seq += rot_seq
			}
		}

		def ip = new IncrementalParser(seq, comparator)
		println "MOTIF"
		println seq
		println "MOTIF DICTIONARY"
		println ip.dict
		println "CONTINUATION DICTIONARY"
		println ip.tree

		/*
		MOTIF
		[a, b, a, b, a, b, c, a, b, d, a, b, c, d, a, b, c, e]
		MOTIF DICTIONARY
		[a] = 6
		[a, b] = 5
		[a, b, c] = 3
		[a, b, c, d] = 1
		[a, b, c, e] = 1
		[a, b, d] = 1
		[b] = 1
		null
		CONTINUATION DICTIONARY
		null^[]
		----a^[[sym: b, prob: 1.0]]
		----b^[]
		--------a^[[sym: c, prob: 0.75], [sym: d, prob: 0.25]]
		----c^[]
		--------b^[]
		------------a^[[sym: d, prob: 0.5], [sym: e, prob: 0.5]]
		*/

		List domainValues = ['a', 'b', 'c', 'd', 'e']
		List generatedSeq = ip.generate('a', 50, 3, domainValues)
		println "GENERATED SEQUENCE"
		println generatedSeq
	}
}


     

