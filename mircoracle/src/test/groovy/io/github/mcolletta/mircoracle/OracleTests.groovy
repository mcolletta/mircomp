/*
 * Copyright (C) 2016-2024 Mirco Colletta
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

import groovy.test.GroovyTestCase


public class LowerCaseStringComparator implements Comparator<String> {
	public int compare(String s1, String s2) {
		return s1.toUpperCase().compareTo(s2.toUpperCase())
	}
}

class OracleTests extends GroovyTestCase {

	void testOracle1() {
		def seq = "aabbabbabbab".toList()
		Comparator comparator = new LowerCaseStringComparator()
		def fo = new FactorOracle<String>(seq,comparator)

		assert fo.transitions == [0:[a:1, b:3], 1:[a:2, b:3], 2:[b:3], 3:[a:5, b:4], 4:[a:5], 5:[b:6], 6:[b:7], 7:[a:8], 8:[b:9], 9:[b:10], 10:[a:11], 11:[b:12]]
		assert fo.suffixLinks == [1:0, 2:1, 3:0, 4:3, 5:1, 6:3, 7:4, 8:5, 9:6, 10:7, 11:8, 12:9]
		assert fo.lrs == [1:0, 2:1, 3:0, 4:1, 5:1, 6:2, 7:3, 8:4, 9:5, 10:6, 11:7, 12:8]
		assert fo.reverseSuffixLinks == [0:[1, 3], 1:[2, 5], 3:[4, 6], 4:[7], 5:[8], 6:[9], 7:[10], 8:[11], 9:[12]]
		assert fo.encoded == ['a', [1, 1], 'b', [3, 1], [2, 8]]
	}

	void testOracle2() {
		def seq = "abbcabcdabc".toList()
		def fo = new FactorOracle<String>(seq)
		
		assert fo.transitions == [0:[a:1, b:2, c:4, d:8], 1:[b:2], 2:[b:3, c:4], 3:[c:4], 4:[a:5, d:8], 5:[b:6], 6:[c:7], 7:[d:8], 8:[a:9], 9:[b:10], 10:[c:11]]
		assert fo.suffixLinks == [1:0, 2:0, 3:2, 4:0, 5:1, 6:2, 7:4, 8:0, 9:1, 10:2, 11:4]
		assert fo.lrs == [1:0, 2:0, 3:1, 4:0, 5:1, 6:2, 7:2, 8:0, 9:1, 10:2, 11:2]
		assert fo.reverseSuffixLinks == [0:[1, 2, 4, 8], 1:[5, 9], 2:[3, 6, 10], 4:[7, 11]]
		assert fo.encoded == ['a', 'b', [2, 1], 'c', [1, 2], [4, 1], 'd', [1, 2], [4, 1]]
	}

	void testOracle3() {
		def seq = "abAaBacba".toList()
		
		Comparator comparator = [
			compare:{ String a, String b-> b.toLowerCase() <=> a.toLowerCase() }
		] as Comparator
		def fo = new FactorOracle<String>(seq,comparator)

		assert fo.transitions == [0:[a:1, b:2, c:7], 1:[a:4, b:2, c:7], 2:[A:3], 3:[a:4, c:7], 4:[B:5], 5:[a:6], 6:[c:7], 7:[b:8], 8:[a:9]]
		assert fo.suffixLinks == [1:0, 2:0, 3:1, 4:1, 5:2, 6:3, 7:0, 8:2, 9:3]
		assert fo.lrs == [1:0, 2:0, 3:1, 4:1, 5:2, 6:3, 7:0, 8:1, 9:2]
		assert fo.reverseSuffixLinks == [0:[1, 2, 7], 1:[3, 4], 2:[5, 8], 3:[6, 9]]
		assert fo.encoded == ['a', 'b', [1, 1], [1, 3], 'c', [2, 2]]
	}

	void testOracle4() {
		def seq = "abbbaab".toList()
		def fo = new FactorOracle<String>(seq)

		assert fo.transitions == [0:[a:1, b:2], 1:[a:6, b:2], 2:[a:5, b:3], 3:[a:5, b:4], 4:[a:5], 5:[a:6], 6:[b:7]]
		assert fo.suffixLinks == [1:0, 2:0, 3:2, 4:3, 5:1, 6:1, 7:2]
	}

	void testOracle5() {
		def seq1 = "abaaba".toList()
		def fo1 = new FactorOracle<String>(seq1)

		def seq2 = "cba".toList()
		def fo2 = fo1.extend(seq2)

		// FO2
		assert fo2.transitions == [0:[a:1, b:2, c:7], 1:[a:4, b:2, c:7], 2:[a:3], 3:[a:4, c:7], 4:[b:5], 5:[a:6], 6:[c:7], 7:[b:8], 8:[a:9]]
		assert fo2.suffixLinks == [1:0, 2:0, 3:1, 4:1, 5:2, 6:3, 7:0, 8:2, 9:3]
		assert fo2.lrs == [1:0, 2:0, 3:1, 4:1, 5:2, 6:3, 7:0, 8:1, 9:2]
		assert fo2.reverseSuffixLinks == [0:[1, 2, 7], 1:[3, 4], 2:[5, 8], 3:[6, 9]]
		assert fo2.encoded == ['a', 'b', [1, 1], [1, 3], 'c', [2, 2]]

		// FO1 not mutated
		assert fo1.transitions == [0:[a:1, b:2], 1:[a:4, b:2], 2:[a:3], 3:[a:4], 4:[b:5], 5:[a:6]]
		assert fo1.suffixLinks == [1:0, 2:0, 3:1, 4:1, 5:2, 6:3]
		assert fo1.reverseSuffixLinks == [0:[1, 2], 1:[3, 4], 2:[5], 3:[6]]
	}

}