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


public class Compressor<T> {
    
    PList<List<T>> dict
    int len
    
    Compressor(Compressor orig, List<T> seq) {
        dict = orig.dict
        len = orig.len
        compress(seq)
    }
    
    Compressor(List<T> seq) {
        dict = new PList<>()
        dict.plus([])
        len = 0
        compress(seq)
    }
    
    void compress(List<T> seq) {
        List<T> phrase = []
        int ptr = 0

        for(int i=0; i < seq.size(); i++) {
            phrase << seq[i]
            int index = dict.indexOf(phrase)

            if(index == -1) {
                dict = dict.plus(phrase)
                phrase = []
                ptr = 0
                len += 1
            }
            else
                ptr=index
        }
    }
}
