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

import com.xenoage.utils.pdlib.PList

class IncrementalParser<T> {
    TreeDictionary<T,Integer> dict
    List<T> motif
    Comparator comparator
    TreeDictionaryNode<T,Integer> tree

    IncrementalParser(Iterable seq, Comparator cmp=null) {
        motif = []
        comparator = cmp
        dict = new TreeDictionary<>(comparator)
        dict.withDefault(1)
        build(seq)
    }

    private void build(Iterable seq) {
        for(T item: seq) {
            plus(item)
        }
        tree = generateTree()
    }

    void leftShift(T item) { plus(item) }

    void plus(T item) {
        motif += item
        if (dict.containsKey(motif)) {
            dict[motif] += 1
            //dict[motif] = dict.getAt(motif) + 1
        }
        else {
            dict[motif] = 1
            motif = []
        }
    }

    TreeDictionary<T,List<Continuation<T>>> continuationDictionary() {
        TreeDictionary<T,List<Continuation<T>>> dict2 = new TreeDictionary<>(comparator)
        for(Map.Entry<List<T>,Integer> e: dict) {
            List<T> key = e.getKey()
            int counter = e.getValue()
            List<T> W = key.take(key.size()-1)
            T k = key.last()
            var c = new Continuation<T>(k, counter)
            if (dict2.containsKey(W)) {
                List<Continuation<T>> continuations = dict2[W]
                continuations << c
            } else
                dict2[W] = [c]
        }
        normalize(dict2)
        return dict2
    }

    void normalize(TreeDictionary<T,List<Continuation<T>>> dictionary) {
        for(Map.Entry<List<T>,List<Continuation>> e: dictionary) {
            List<T> key = e.getKey()
            List<Continuation<T>> continuations = e.getValue()
            float total = (int) continuations.prob?.sum()
            for (Continuation<T> c: continuations) {
                c.prob = (float) (c.prob / total)
            }
        }
    }

    TreeDictionaryNode<T, PList<Continuation>> generateTree() {
        var con_dictionary = continuationDictionary()
        var root = new TreeDictionaryNode<T,List<Continuation>>()
        for(Map.Entry<List<T>,List<Continuation>> e: con_dictionary) {
            List<T> motif = e.getKey()
            List<Continuation> continuations = e.getValue()
            var pointer = root
            int i = 0
            motif.reverseEach { T item ->
                i += 1
                TreeDictionaryNode<T,List<Continuation>> node = pointer.getChild(item, comparator)
                if (node != null) {
                    pointer = node
                }
                else {
                    node = new TreeDictionaryNode<>()
                    node.content = item
                    pointer.children = pointer.children.plus(node)
                    pointer = node
                }
                if (i == motif.size()) {
                    node.continuations = new PList(continuations)
                }
            }
        }
        return root
    }

    T findContinuation(List context, float rng) {
        List<Continuation<T>> continuations = []
        TreeDictionaryNode<T,Integer> pointer = tree
        for (int i = context.size() - 1; i >= 0; i--) {
            T item = context[i]
            TreeDictionaryNode<T,Integer> node = pointer.getChild(item, comparator)
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

    T rouletteWheel(List<Continuation<T>> choices, float rng) {
        float cursor = 0.0f
        for (Continuation<T> continuation: choices) {
            cursor += continuation.prob
            if (cursor > rng)
                return continuation.sym
        }
        return null
    }

    List<T> generate(T startSymbol, int sequenceLength, int contextLength, List<T> domainValues=[]) {
        int dlen = domainValues.size()
        Random rand = new Random()
        List<T> sequence = [startSymbol]
        int count = 0
        while (count < sequenceLength) {
            float rng = rand.nextFloat()
            int end = sequence.size()
            int start = Math.max(0, end - contextLength)
            List<T> context = sequence.subList(start, end)
            T symbol = findContinuation(context, rng)
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


public class Continuation<T> {
    T sym
    float prob

    Continuation(T sym, float prob) {
        this.sym = sym
        this.prob = prob
    }

    public String toString() {
        return "[sym: " + sym + ", prob: " + prob + "]";
    }
}
