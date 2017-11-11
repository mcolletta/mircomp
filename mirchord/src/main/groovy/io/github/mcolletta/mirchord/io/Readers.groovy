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

package io.github.mcolletta.mirchord.io

import static java.lang.Math.*

import com.xenoage.utils.math.Fraction
import static com.xenoage.utils.math.Fraction.fr
import static com.xenoage.utils.math.Fraction._0

import io.github.mcolletta.mirchord.core.*
import static io.github.mcolletta.mirchord.core.Utils.*

import groovy.util.slurpersupport.NodeChildren
import groovy.util.slurpersupport.NodeChild

import java.util.zip.ZipFile
import groovy.xml.*
import groovy.util.XmlSlurper


trait LeadSheetReader {

	void addLeadSheetListener(LeadSheetListener subscriber) {
		leadSheetListeners << subscriber
	}

	void fireNewSong() {
		leadSheetListeners.each { it.newSong() }
	}

	void fireChordSymbol(ChordSymbol event) {
		leadSheetListeners.each { it.chordSymbol(event) }
	}

	void fireChord(Chord event) {
		leadSheetListeners.each { it.chord(event) }
	}

	void fireRest(Rest event) {
		leadSheetListeners.each { it.rest(event) }
	}
}


class MusicXmlLeadSheetReader implements LeadSheetReader {

	List<LeadSheetListener> leadSheetListeners = []
	Map<String, Map<Integer, Integer>> chordStats = [:].withDefault{[:].withDefault{0}}
	
	void read(File folder) {
		
		def slurper = new XmlSlurper()
		slurper.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		slurper.setFeature("http://xml.org/sax/features/namespaces", false)
		slurper.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false)
		
		folder.eachFileMatch(~/.*\.mxl/) { File f ->
			def zipFile = new ZipFile(f)
			def files = [:]
			def mxml
			zipFile.entries().each { zipEntry ->
				def content = zipFile.getInputStream(zipEntry).text
				files[zipEntry.name] = content
				if (zipEntry.name == "META-INF/container.xml") {
					 List<String> paths = slurper.parseText(content).'**'.grep{ it.'@full-path' != '' }.'@full-path'*.text()
					 if (paths && paths.size() == 1) {
						 mxml = slurper.parseText(files[paths[0]])
					 }
				}
			}
			processXml(mxml)
			normalizeChordStats()
		}

		folder.eachFileMatch(~/.*\.xml/) { File f ->
			processXml(slurper.parseText(f.text))
		}
		
		normalizeChordStats()
	}
	
	void normalizeChordStats() {
		chordStats.each { chord, pitch_histogram ->
			def sum = pitch_histogram.values().sum()
			pitch_histogram.each { k, v ->
				pitch_histogram[k] = v/sum
			}
		}
	}
	
	void processXml(xml) {
		fireNewSong()

		def keysig = new KeySignature()
		List<Integer> sig = [0,0]
		ChordSymbol currentChord
		Fraction currentChordDuration = _0
		int tonic
		int transposition = 0
		int measureLength = 4
		boolean tieStart = false

		def measures = xml.'**'.grep{ it.name() == 'measure' }
		measures.each { m->
			// find for signature and divisions
			if (m.attributes.divisions.size() > 0) {
				int divisions = Integer.parseInt(m.attributes.divisions.text())
				measureLength = divisions * 4
			}

			// find key in measure
			if (m.attributes.key.size() > 0) {
				keysig.fifths = Integer.parseInt(m.attributes.key.fifths.text())
				String scaleName = m.attributes.key.mode.text()
				if (scaleName) 
					keysig.mode = Utils.ModeFromName(scaleName)
				else
				keysig.mode = 0
				//possibly fire keysig
				sig = [keysig.mode, keysig.fifths]
				tonic = Utils.TonicFromKeySignature[sig]
				transposition = (keysig.fifths * 7) % 12
			}
			
			NodeChildren children = m.children()
			children.each { NodeChild item ->
				if (item.name() == 'backup' || item.name() == 'forward')
					throw new Exception("Not a Leadsheet: element '${item.name()}' found.")

				if (item.name() == 'harmony') {
					if (currentChord != null && currentChordDuration != _0) {
						currentChord.duration = currentChordDuration
						currentChordDuration = _0
						fireChordSymbol(currentChord)
					}
					String _step = item.root.'root-step'.text()
					int _alteration = 0
					if (item.root.'root-alter'.size() > 0)
						_alteration = Integer.parseInt(item.root.'root-alter'.text())
					int _octave = 3
					def root = new Pitch(_step, _octave, _alteration)
					if (transposition > 0)
							root.midiValue += transposition
					String kindText = item.kind.toString()
					
					if (kindText != '') {
						ChordKind kind = Utils.getChordSymboKind(kindText)
						if (kind) {
							def chordSymbol = new ChordSymbol(root, kind)
							if (item.bass.size() > 0) {
								String _bass_step = item.bass.'bass-step'.text()
								int _bass_alteration = 0
								if (item.bass.'bass-alter'.size() > 0)
									_bass_alteration = Integer.parseInt(item.bass.'bass-alter'.text())
								int _bass_octave = 3
								chordSymbol.bass =  new Pitch(_bass_step, _bass_octave, _bass_alteration)
							}
							currentChord  = chordSymbol
						}
						else
							println "Chord kind of type $kindText not recognized"
					}
				}
				if (item.name() == 'note') {
					if (item.rest.size() > 0) {
						def rest = new Rest()
						if (item.duration.size() > 0)
							rest.duration = fr(Integer.parseInt(item.duration.text()), measureLength)
						currentChordDuration = currentChordDuration.add(rest.duration)
						fireRest(rest)
					}
					if (item.pitch.size() > 0) {
						def note = new Chord()
						String _step = item.pitch.step.text()
						int _alteration = 0
						if (item.pitch.alter.size() > 0)
							_alteration = Integer.parseInt(item.pitch.alter.text())
						int _octave = Integer.parseInt(item.pitch.octave.text())
						note.pitch =new Pitch(_step, _octave, _alteration)
						if (item.duration.size() > 0)
							note.duration = fr(Integer.parseInt(item.duration.text()), measureLength)
						if (item.dot.size() > 0) {
							note.duration = note.duration.add(note.duration.mult(fr(1,2)))
						}
						currentChordDuration = currentChordDuration.add(note.duration)

						if (transposition > 0)
							note.pitch.midiValue += transposition

						if (tieStart) {
							note.tieEnd = true
							tieStart = false
						}
						if (item.tie.size() > 0) {
							def tieType = item.tie.@type
							if (tieType == "start") {
								note.tieStart = true
								tieStart = true
							}
						}

						if (currentChord != null) {
							int curChordPitchClass = currentChord.root.midiValue % 12
							chordStats[curChordPitchClass.toString() + "(" + currentChord.kind + ")"][note.pitch.midiValue % 12] += 1
						}
						fireChord(note)
					}
				}
			}
		}

		if (currentChord != null && currentChordDuration != _0) {
			currentChord.duration = currentChordDuration
			fireChordSymbol(currentChord)
		}

	}
}
