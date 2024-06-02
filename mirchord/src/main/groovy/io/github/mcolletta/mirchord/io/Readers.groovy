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

package io.github.mcolletta.mirchord.io

import static java.lang.Math.*

import com.xenoage.utils.math.Fraction
import static com.xenoage.utils.math.Fraction.fr
import static com.xenoage.utils.math.Fraction._0

import io.github.mcolletta.mirchord.core.*
import static io.github.mcolletta.mirchord.core.Utils.*

import java.util.zip.ZipFile
import java.util.zip.ZipEntry

import javax.xml.xpath.*

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList

import static com.xenoage.utils.jse.xml.XMLReader.*
import com.xenoage.utils.jse.xml.XMLReader
import com.xenoage.utils.jse.xml.XMLWriter


interface LeadSheetReader {

	void addLeadSheetListener(LeadSheetListener subscriber);

    void fireNewSong();

    void fireKeySignature(KeySignature event);

    void fireChord(Chord event);

    void fireRest(Rest event);

    void fireEndReading()

}

trait LeadSheetReaderTrait implements LeadSheetReader {

	List<LeadSheetListener> leadSheetListeners = []

	void addLeadSheetListener(LeadSheetListener subscriber) {
		leadSheetListeners << subscriber
	}

	void fireNewSong() {
		leadSheetListeners.each { it.newSong() }
	}

	void fireKeySignature(KeySignature event) {
		leadSheetListeners.each { it.keySignature(event) }
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

	void fireEndReading() {
		leadSheetListeners.each { it.endReading() }
	}
}


class MusicXmlLeadSheetReader implements LeadSheetReader, LeadSheetReaderTrait {

	private static XPath xpath = XPathFactory.newInstance().newXPath()

    static Element getElementFromXPath(Element element, String path) {
        return xpath.evaluate(path, element, XPathConstants.NODE) as Element
    }

    static NodeList getElementListFromXPath(Element element, String path) {
        return xpath.evaluate(path, element, XPathConstants.NODESET) as NodeList
    }

    static void updateElement(Element element, String text) {
        element.setTextContent(text)
    }
	
	void read(File folder) {
		folder.eachFileMatch(~/.*\.mxl/) { File f ->
			def zipFile = new ZipFile(f)
			Map<String,String> files = [:]
			String mxml
			zipFile.entries().each { ZipEntry zipEntry ->
				String content = zipFile.getInputStream(zipEntry).getText()
				files[zipEntry.getName()] = content
				if (zipEntry.name == "META-INF/container.xml") {
					Document doc = XMLReader.read(content)
        			Element root = XMLReader.root(doc)
        			Element rootfile = getElementFromXPath(root, "//rootfile")
        			String pathAttr = XMLReader.attribute(rootfile,"full-path")
					if (pathAttr != null) {
						mxml = files[pathAttr]
					}
				}
			}
			processXml(mxml)
		}

		folder.eachFileMatch(~/.*\.xml/) { File f ->
			processXml(f.text)
		}
		
		fireEndReading()
	}
	
	void processXml(String xml) {
		fireNewSong()

		Document doc = XMLReader.read(xml)
        Element root = XMLReader.root(doc)

		int measureLength = 4
		boolean tieStart = false

        NodeList measures = getElementListFromXPath(root, "//measure")
		if (measures != null) {
			for(int i=0; i < measures.getLength(); i++) {
				Element m = null
				Node node = measures.item(i)
				if (node.getNodeType() == Node.ELEMENT_NODE)
					m = (Element) node
				else
					continue

			    if (m != null) {
					ArrayList<Element> children = elements(m)
					for(Element item: children) {
						if (item.getNodeName() == 'backup' || item.getNodeName() == 'forward')
							throw new Exception("Not a Leadsheet: element '${item.getNodeName()}' found.")

						// find for signature and divisions
						if (item.getNodeName() == "attributes") {
							String divText = elementText(item,"divisions")
							if (divText != null && divText != "") {
							    int divisions = Integer.parseInt(divText)
								measureLength = divisions * 4
							}
							// find key in measure
							Element keyElement = element(item,"key")
							if (keyElement != null) {
								def keysig = new KeySignature()
								keysig.fifths = Integer.parseInt(elementText(keyElement, "fifths"))
								String scaleName = elementText(keyElement, "mode")
								if (scaleName) 
									keysig.mode = Utils.ModeFromName(scaleName)
								fireKeySignature(keysig)
							}
						}

						if (item.getNodeName() == 'harmony') {
							Element harmonyRoot = element(item, "root")
							String _step = elementText(harmonyRoot, 'root-step')
							int _alteration = 0
							String rootAlter = elementText(harmonyRoot, 'root-alter')
							if (rootAlter != null)
								_alteration = Integer.parseInt(rootAlter)
							int _octave = 3
							//println "$_step $_octave $_alteration"
							def pitch = new Pitch(_step, _octave, _alteration)

							Element harmonyKind = element(item, "kind")							
							if (harmonyKind != null) {
								String kindText = harmonyKind.getTextContent()
								ChordKind kind = Utils.getChordSymboKind(kindText.trim())
								if (kind != null) {
									def chordSymbol = new ChordSymbol(pitch, kind)
									Element harmonyBass = element(item, "bass")
									if (harmonyBass != null) {
										String _bass_step = elementText(harmonyBass, "bass-step")
										int _bass_alteration = 0
										String _bass_alter = elementText(harmonyBass, "bass-alter")
										if (_bass_alter != null)
											_bass_alteration = Integer.parseInt(_bass_alter)
										int _bass_octave = 3
										chordSymbol.bass = new Pitch(_bass_step, _bass_octave, _bass_alteration)
									}
									fireChordSymbol(chordSymbol)
								} else 
									println "Chord kind of type $kindText not recognized"
							}
						}

						if (item.getNodeName() == 'note') {
							String _duration = elementText(item, "duration")
							Fraction duration = _0
							if (_duration != null) {
								duration = fr(Integer.parseInt(_duration), measureLength)
								if (element(item, "dot") != null) {
									duration = duration.add(duration.mult(fr(1,2)))
								}
							}
							if (element(item, "rest") != null) {
								def rest = new Rest()
								rest.duration = duration
								fireRest(rest)
								continue
							}
							Element notePitch = element(item, "pitch")
							if (notePitch != null) {
								def note = new Chord()
								String _step = elementText(notePitch, "step")
								int _alteration = 0
								String noteAlter = elementText(notePitch, "alter")
								if (noteAlter != null)
									_alteration = Integer.parseInt(noteAlter)
								int _octave = Integer.parseInt(elementText(notePitch, "octave"))
								note.pitch = new Pitch(_step, _octave, _alteration)
								note.duration = duration

								if (tieStart) {
									note.tieEnd = true
									tieStart = false
								}
								Element noteTie = element(item, "tie")
								if (noteTie != null) {
									String tieType = attribute(noteTie, "type")
									if (tieType == "start") {
										note.tieStart = true
										tieStart = true
									}
								}
								fireChord(note)
							}
						}
					}
				}
			}
		}
	}
}


class MirScoreReader implements LeadSheetReader, LeadSheetReaderTrait {

	void read(Score score) {
		fireNewSong()
		for (Part part: score.parts) {
			for (Voice voice: part.voices) {
				def elements = Score.getElementsByTypes(voice.elements, ["KeySignature", "ChordSymbol", "Chord", "Rest"])
				for (MusicElement el: elements) {
					switch (el) {
						case { it.getMusicElementType() == "KeySignature" }:
							fireKeySignature((KeySignature)el)
							break
						case { it.getMusicElementType() == "ChordSymbol" }:
							fireChordSymbol((ChordSymbol)el)
							break
						case { it.getMusicElementType() == "Chord" }:
							fireChord((Chord)el)
							break
						case { it.getMusicElementType() == "Rest" }:
							fireRest((Rest)el)
							break
						default:
							break
					}
				}
			}
		}
		fireEndReading()
	}

}

