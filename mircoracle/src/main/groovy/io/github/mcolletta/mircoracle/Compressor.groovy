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
// import com.xenoage.utils.kernel.Tuple2

import groovy.lang.Tuple2
import groovy.json.JsonBuilder

import java.util.stream.Collectors


@groovy.transform.CompileDynamic
public class Compressor<T> implements Serializable  {
    
    PList<List<T>> dict
    boolean encode
    PList<Tuple2<Integer,T>> encoded
    int len
    
    int ptr = 0
    List<T> phrase

    Compressor(Compressor<T> orig, List<T> seq) {
        dict = orig.dict
        encode = orig.encode
        if (encode)
            encoded = orig.encoded.minus(orig.encoded.size()-1) as PList<Tuple2<Integer,T>>
        len = orig.len
        phrase = orig.phrase
        ptr = orig.ptr
        compress(seq)
    }
    
    Compressor(List<T> seq, boolean enc=false) {
        dict = new PList<>()
        dict = dict.plus([])
        len = 0
        encode = enc
        encoded = new PList<>()
        phrase = []
        compress(seq)
    }
    
    void compress(List<T> seq) {
        for(int i=0; i < seq.size(); i++) {
            phrase << seq[i]
            int index = dict.lastIndexOf(phrase)

            if(index == -1) {
                dict = dict.plus(phrase)
                if (encode)
                    encoded = encoded.plus(new Tuple2<Integer, T>(ptr, seq[i]))
                phrase = []
                ptr = 0
                len += 1
            }
            else {
                ptr = index
                if(encode && (i == seq.size() - 1)){
                  encoded = encoded.plus(new Tuple2<Integer, T>(ptr, null))
                }
            }
        }
    }

    String toJson(boolean pp=false) {
        if (pp)
            return new JsonBuilder(this).toPrettyString()
        return new JsonBuilder(this).toString()
    }
}
