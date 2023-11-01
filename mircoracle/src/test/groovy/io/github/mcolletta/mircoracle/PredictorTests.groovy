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

import groovy.test.GroovyTestCase


class PredictorTests extends GroovyTestCase {

	void testIP1() {
		def seq = "abababcabdabcdabce".toList()


		def ip = new IncrementalParser(seq)

		def dict = ip.dict

		assert dict.getAt(['a']) == 6
		assert dict.getAt(['a', 'b']) == 5
		assert dict.getAt(['a', 'b', 'c']) == 3
		assert dict.getAt(['a', 'b', 'c', 'd']) == 1
		assert dict.getAt(['a', 'b', 'c', 'e']) == 1
		assert dict.getAt(['a', 'b', 'd']) == 1
		assert dict.getAt(['b']) == 1

		def tree = ip.tree
		
		assert tree.children[0].content == 'a'
		assert tree.children[0].continuations[0].sym == 'b' 
		assert tree.children[0].continuations[0].prob == 1.0f

		assert tree.children[1].content == 'b'
		assert tree.children[1].continuations.size() == 0
		def node = tree.children[1]
		assert node.children[0].content == 'a'
		assert node.children[0].continuations[0].sym == 'c' 
		assert node.children[0].continuations[0].prob == 0.75f
		assert node.children[0].continuations[1].sym == 'd' 
		assert node.children[0].continuations[1].prob == 0.25f

		assert tree.children[2].content == 'c'
		assert tree.children[2].continuations.size() == 0
		node = tree.children[2]
		assert node.children[0].content == 'b'
		assert node.children[0].continuations.size() == 0

		node = node.children[0]
		assert node.children[0].continuations[0].sym == 'd' 
		assert node.children[0].continuations[0].prob == 0.5f
		assert node.children[0].continuations[1].sym == 'e' 
		assert node.children[0].continuations[1].prob == 0.5f
	}
}