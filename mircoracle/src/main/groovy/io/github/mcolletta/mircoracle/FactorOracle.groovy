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
import com.xenoage.utils.pdlib.PMap


@groovy.transform.CompileDynamic
class FactorOracle<T> {
    PList<T> sequence
    private int i = 0
    private int l = 0
    PMap<Integer,PMap<T,Integer>> transitions = new PMap()
    PMap<Integer,Integer> suffixLinks = new PMap()
    PMap<Integer,PList<Integer>> reverseSuffixLinks = new PMap()
    PMap<Integer,Integer> lrs = new PMap()
    private Comparator comparator
    PList encoded = new PList()

    private FactorOracle() {}

    FactorOracle(List<T> seq, Comparator cmp=null) {
        comparator = cmp
        sequence = new PList<T>(seq)
        build()
    }

    private setTransitions(PMap<Integer,PMap<T,Integer>> tr) {
        transitions = tr;
    }

    private setSuffixLinks(PMap<Integer,Integer> sl) {
        suffixLinks = sl
    }

    private setReverseSuffixLinks(PMap<Integer,PList<Integer>> rsl) {
        reverseSuffixLinks = rsl
    }

    private setLrs(PMap<Integer,Integer> l) {
        lrs = l
    }

    private setEncoded(PList enc) {
        encoded = enc
    }

    private void build() {
        int m = sequence.size()
        for (int k = 1 + i; k <= m; k++) {
            def item = sequence[k-1]
            this << item
            if (lrs[k] < k - l) {
                if (l != k - 1) {
                    encoded += new PList( [suffixLinks[k - 1] - (k - 1 - l) + 1, k - 1 - l] )
                    l = k - 1
                }
                if (lrs[k] == 0) {
                    encoded += item
                    l = k
                }
            }
        }
        if (l < m)
            encoded += new PList( [suffixLinks[m] - (m - l) + 1, m - l] )
    }

    FactorOracle extend(List seq) {
        FactorOracle fo = new FactorOracle()
        fo.transitions = transitions
        fo.suffixLinks = suffixLinks
        fo.reverseSuffixLinks = reverseSuffixLinks
        fo.lrs = lrs
        fo.encoded = encoded.subList(0,encoded.size()-1)
        fo.comparator = comparator
        int len = sequence.size()
        fo.sequence = sequence.plusAll(seq)
        fo.i = i
        fo.l = l
        fo.build()
        return fo
    }

    private void addTransition(int k, T symbol, int j) {
        PMap tr
        if (transitions.containsKey(k)) {
            tr = transitions[k]
            transitions = transitions.minus(k)
        } else
            tr = new PMap()
        tr = tr.plus(symbol, j)
        transitions = transitions.plus(k,tr)
    }

    int getSuffixLink(int k) {
        if (suffixLinks.containsKey(k))
            return suffixLinks[k]
        return -1
    }

    private void setSuffixLink(int k, int j) {
        suffixLinks = suffixLinks.plus(k,j)
    }

    int get_lrs(int k) {
        if (lrs.containsKey(k))
            return lrs[k]
        return 0
    }

    private void set_lrs(int k, int j) {
        lrs = lrs.plus(k,j)
    }

    private void addReverseSuffixLink(int k, int j) {
        PList ls
        if (reverseSuffixLinks.containsKey(k)) {
            ls = reverseSuffixLinks[k]
            reverseSuffixLinks.minus(k)
        } else
            ls = new PList()
        ls = ls.plus(j)
        reverseSuffixLinks = reverseSuffixLinks.plus(k,ls)
    }

    PList<Integer> getReverseSuffixLink(int k) {
        if (reverseSuffixLinks.containsKey(k))
            return reverseSuffixLinks[k]
        return new PList()
    }

    private void leftShift(item) { plus(item) }

    private void plus(item) {
        i += 1
        //transitions[i-1][item] = i
        addTransition(i-1,item,i)
        int pi1 = i-1
        int k = getSuffixLink(i-1)
        while (k > -1 && !transitionsContains(k,item)) {
            //transitions[k][item] = i
            addTransition(k,item,i)
            pi1 = k
            k = getSuffixLink(k)
        }
        int sfxLink = 0
        if (k == -1) {
            // suffixLinks[i] = 0
            setSuffixLink(i,0)
            set_lrs(i,0)
        }
        else {
            if (!comparator)
                sfxLink = transitions[k][item]
            else
                sfxLink = findTransition(k,item)
            //suffixLinks[i] = sfxLink
            setSuffixLink(i,sfxLink)
            set_lrs(i,lengthCommonSuffix(pi1,getSuffixLink(i)-1) + 1)
        }
        k = findBetter(i,sequence[i-get_lrs(i)])
        if (k != 0) {
            //lrs[i] += 1
            set_lrs(i, get_lrs(i) + 1)
            //suffixLinks[i] = k
            setSuffixLink(i,k)
        }
        addReverseSuffixLink(sfxLink, i)
    }

    private int findBetter(int k, T a) {
        for(int j: getReverseSuffixLink(k).sort()) {
            boolean eq
            if (comparator)
                eq = (comparator.compare(sequence[j-get_lrs(k)] , a) == 0)
            else
                eq = sequence[j-get_lrs(k)] == a
            if (get_lrs(j) == get_lrs(k) && eq)
                return j
        }
        return 0
    }

    int lengthCommonSuffix(int pi1, int pi2) {
        if (getSuffixLink(pi1) == pi2)
            return get_lrs(pi1)
        else
            while (getSuffixLink(pi2) != getSuffixLink(pi1))
                pi2 = getSuffixLink(pi2)
        return Math.min(get_lrs(pi1),get_lrs(pi2))
    }

    private int findTransition(int k, T item) {
        int retVal
        if (comparator) {
            for(T key: transitions[k].keySet()) {
                if (comparator.compare(key,item) == 0) {
                    retVal = transitions[k][key]
                    break
                }
            }
        }
        else
            retVal = transitions[k][item]
        
        return retVal
    }
    
    boolean transitionsContains(int k, T item) {
        boolean found = false
        if (comparator) {
            for(T key: transitions[k].keySet()) {
                if (comparator.compare(key,item) == 0) {
                    found = true
                    break
                }  
            }
        }
        else
            found = transitions[k].containsKey(item)
        return found
    }

    String toString() {
        String str = ""
        str += "transitions: " + transitions + "\n"
        str += "suffixLinks: " + suffixLinks + "\n"
        str += "lrs: " + lrs + "\n"
        str += "reverseSuffixLinks: " +  reverseSuffixLinks + "\n"
        str += "encoded: " + encoded
        return str
    }

}

