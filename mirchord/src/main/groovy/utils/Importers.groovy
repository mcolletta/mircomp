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

import java.util.concurrent.ConcurrentSkipListMap

import com.xenoage.utils.math.Fraction
import static com.xenoage.utils.math.Fraction.fr
import static com.xenoage.utils.math.Fraction._0

class Importer {

	
	static def importAll(String pathMel, String pathHar, boolean includeEmptyMeasures = false) {

		def events = new ConcurrentSkipListMap().withDefault { [mel:null, har:null] }

		// Import Melody
		def chord
		def lastTime = 0.0f
		new File(pathMel).splitEachLine("\\s+") {items ->
		    float time = items[1].toFloat()
		    int  measure = (int)time
		    /*if (lastTime == 0.0f && includeEmptyMeasures) {
			    Fraction rd = Utils.getFractionFromDouble(time - measure)
			    events[lastTime].mel = [midiPitch: -1, duration: rd, measure: measure, transposed: -1] // rest
			}*/
		    float fduration = time - lastTime
		    lastTime = time
		    int pitch = items[2].toInteger()
		    Fraction duration = Utils.getDurationFromDecimal(fduration)
		    if (chord != null) { 
		    	// update since now (noteoff offset) we have the duration of the previous chord 
		    	chord.duration = duration
			    events[lastTime].mel = chord
			}
			int key = ( pitch - items[3].toInteger() ) % 12
			int transposed = pitch - key // transpose to C
			// TODO: 
		    // map key to fifths
		    Fraction tmpDuration = Utils.getDurationFromDecimal((measure + 1) - time)
		    chord = [midiPitch: pitch, duration: tmpDuration, measure: measure, key: key, transposed: transposed]
		}

		// Import Harmony
		def PATTERN = ~/([b|\#]*)([i|I|v|V]+).*/

		chord = null
		lastTime = 0.0f
		new File(pathHar).splitEachLine("\\s+") {items ->
			if (items.size() > 3) { // END case
			    float time = items[1].toFloat()
			    int  measure = (int)time
			    /*if (lastTime == 0.0f && includeEmptyMeasures) {
				    Fraction rd = Utils.getFractionFromDouble(time - measure)
				    events[lastTime].har = [root: -1, duration: rd, measure: measure] // rest
				    println rd
				}*/
			    float fduration = time - lastTime
			    lastTime = time
			    def symbol = items[2]
			    int chromaticRoot = items[3].toInteger()
			    // int diatonicRoot = items[4].toInteger()  NOT USED
			    int key = items[5].toInteger()
			    int absoluteRoot = items[6].toInteger()

			    int pitch // TODO
			    def modeSym = ""			    
			    def m = PATTERN.matcher(symbol)		    
			    if (m.matches() &&  m.groupCount() > 0) {
				    def accidentals =  m.group(1) // accidentals.collect()
				    modeSym = m.group(2)
				}
			    // modeSym: 
			    // lowercase => minor (root+4,root+7)    
			    // uppercase => major (root+3,root+7)
			    def mode = (modeSym.equals(modeSym.toUpperCase())) ? KeyMode.Major : KeyMode.Minor

			    Fraction duration = Utils.getDurationFromDecimal(fduration)
			    if (chord != null) { 
			    	// update since now (noteoff offset) we have the duration of the previous chord 
			    	chord.duration = duration
				    events[lastTime].har = chord 
				}
				// TODO: 
			    // map key to fifths
			    Fraction tmpDuration = Utils.getDurationFromDecimal(measure + 1 - time)
			    chord = [root: chromaticRoot, key:key, absoluteRoot:absoluteRoot, mode:mode, duration: tmpDuration, measure: measure]
			}
		}

		return events
	}
	
}

