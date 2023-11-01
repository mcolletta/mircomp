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


class IncrementalParser {
    TreeDictionary<List,Integer> dict
    List motif
    Comparator comparator
    TreeDictionaryNode tree
     
    IncrementalParser(Iterable seq, Comparator cmp=null) {
        motif = []
        comparator = cmp
        dict = new TreeDictionary<>(comparator)
        dict.withDefault{1}
        build(seq)
    }

    private void build(Iterable seq) {
        for(Object item: seq) {
            plus(item)
        }
        tree = generateTree()
    }
     
    void leftShift(Object item) { plus(item) }

    void plus(Object item) {
        motif += item
        if (dict.containsKey(motif)) {
            //dict[motif] += 1
            dict[motif] = dict.getAt(motif) + 1
        }
        else {
            dict[motif] = 1
            motif = []
        }
    }
     
    TreeDictionary continuationDictionary() {
        TreeDictionary<List,List<Continuation>> dict2 = new TreeDictionary<>(comparator)
        for(Map.Entry<List,Integer> e: dict) {
            List key = e.getKey() as List
            int value = e.getValue() as Integer
            List W = key.take(key.size()-1)
            def k = key.last()
            int counter = value
            def c = new Continuation(k, counter) 
            if (dict2.containsKey(W)) {
                List<Continuation> continuations = dict2[W] as List<Continuation>
                continuations << c
            } else 
                dict2[W] = [c]
        }
        normalize(dict2)
        return dict2
    }
     
    void normalize(TreeDictionary dictionary) {
        for(Map.Entry<List,List<Continuation>> e: dictionary) {
            List key = e.getKey() as List
            List<Continuation> value = e.getValue() as List<Continuation>
            float total = (int) value.prob?.sum()
            for (Continuation c: value) {
                c.prob = (float) (c.prob / total)
            }
        }
    }
     
    TreeDictionaryNode<List<Continuation>> generateTree() {         
        TreeDictionary dictionary = continuationDictionary()
        def root = new TreeDictionaryNode()
        for(Map.Entry<List,List<Continuation>> e: dictionary) {
            List motif = e.getKey() as List
            List<Continuation> continuations = e.getValue() as List<Continuation>
            TreeDictionaryNode pointer = root
            int i = 0
            motif.reverseEach { Object item ->
                i += 1
                TreeDictionaryNode node = pointer.getChild(item, comparator)
                if (node != null) {
                    pointer = node
                }
                else {
                    node = new TreeDictionaryNode()
                    node.content = item
                    pointer.children << node
                    pointer = node
                }
                if (i == motif.size()) {
                    node.continuations = continuations
                }
            }
        }
        return root
    }

    def findContinuation(List context, float rng) {
        List<Continuation> continuations = []
        TreeDictionaryNode pointer = tree
        for (int i = context.size() - 1; i >= 0; i--) {
            def item = context[i]    
            TreeDictionaryNode node = pointer.getChild(item, comparator)
            if (node != null) {
                pointer = node
                if (node.continuations.size() > 0)
                    continuations = node.continuations
            }
            else
                break
        }
        return rouletteWheel(continuations, rng)
    }

    def rouletteWheel(List<Continuation> choices, float rng) {
        float cursor = 0.0f
        for (Continuation continuation: choices) {
            cursor += continuation.prob
            if (cursor > rng)
                return continuation.sym
        }
        return null
    }

    List generate(Object startSymbol, int sequenceLength, int contextLength, List domainValues=[]) {
        int dlen = domainValues.size()
        Random rand = new Random()
        List sequence = [startSymbol]
        int count = 0
        while (count < sequenceLength) {
            float rng = rand.nextFloat()
            int end = sequence.size()
            int start = Math.max(0, end - contextLength)
            List context = sequence.subList(start, end)
            def symbol = findContinuation(context, rng)
            if (symbol != null)
                sequence << symbol
            else if (dlen > 0) {
                symbol = domainValues[rand.nextInt(dlen-1)]
                println "random symbol $symbol context $context"
                sequence << symbol
            }
            count += 1
        }
        return sequence
    }
}


public class Continuation {
    def sym
    float prob

    Continuation(Object sym, float prob) {
        this.sym = sym
        this.prob = prob
    }

    public String toString() {
        return "[sym: " + sym + ", prob: " + prob + "]";
    }
}

 
      
