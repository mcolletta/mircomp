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

package io.github.mcolletta.mircomp.demos

import io.github.mcolletta.mirchord.core.*
import io.github.mcolletta.mirchord.io.*
import static io.github.mcolletta.mirconverter.Helper.saveAs

import com.xenoage.zong.desktop.utils.JseZongPlatformUtils

import groovy.transform.CompileStatic

@CompileStatic
class LeadSheetReaderDemo {

	public static void main(String[] args) {
		println args
		if (args.size() < 2)
			throw new Exception("Not enough arguments")
		println args[0]
		println args[1]
		File f = new File(args[0])
		def reader = new MusicXmlLeadSheetReader()
		def subscriber = new LeadSheetSubscriber()
		def intSubscriber = new MelodicIntervalsSubscriber()
		reader.addLeadSheetListener(subscriber)
		reader.addLeadSheetListener(intSubscriber)
		reader.read(f)

		println "CHORD STATS----------------------------------------"
		println reader.chordStats
		println "---------------------------------------------------"

		println "MELODIC INTERVALS----------------------------------"
		println intSubscriber.intervals
		println "---------------------------------------------------"

		def midiFile = new File(args[1])

		String appName = "LeadSheetReaderDemo"
    	JseZongPlatformUtils.init(appName)
    	Score score = createLeadSheetScore(subscriber.melody, subscriber.harmony)
		saveAs(score, midiFile)
	}

	public static Score createLeadSheetScore(List<MusicElement> melody, List<MusicElement> harmony) {
		def score = new Score()

		def melodyPart = new Part("1", "Melody")
		def melodyVoice = new Voice("1", [])
		melodyPart.voices[melodyVoice.id] = melodyVoice
		score.parts[melodyPart.id] = melodyPart
		for(MusicElement el: melody) {
			score.parts["1"].voices["1"].elements << el
		}

		def harmonyPart = new Part("2", "Harmony")
		def harmonyVoice = new Voice("1", [])
		harmonyPart.voices[harmonyVoice.id] = harmonyVoice
		score.parts[harmonyPart.id] = harmonyPart
		for(MusicElement chordSym: harmony) {
			score.parts["2"].voices["1"].elements << chordSym
		}

		return score
	}
}