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

package io.github.mcolletta.mirchord.core

import groovy.transform.*

import com.xenoage.utils.math.Fraction


class ScoreBuilder extends FactoryBuilderSupport {

    def nodeReferences = [:]
    
    def ScoreBuilder() {
        registerFactories()
    }
    
    def registerFactories(){
        registerFactory("score", new ScoreNode())
        registerFactory("part", new PartNode())
        registerFactory("voice", new VoiceNode())
        registerFactory("chord", new ChordNode())
        registerFactory("pitch", new PitchNode())
        registerFactory("rest", new RestNode())
        registerFactory("phrase", new PhraseNode())
        registerFactory("anchor", new AnchorNode())
        registerFactory("repeat", new RepeatNode())
        registerFactory("instrument", new InstrumentNode())
        registerFactory("clef", new ClefNode())
        registerFactory("key", new KeyNode())
        registerFactory("time", new TimeNode())
        registerFactory("tempo", new TempoNode())
        registerFactory("tuplet", new TupletNode())
    }
    
    def build(){}
}

class ScoreNode extends AbstractFactory {
    public Object newInstance(FactoryBuilderSupport builder, Object nodeName, Object nodeArgs, Map nodeAttribs) {
        Score score = new Score()
        score.parts = [:]
        return score
    }

    public boolean isLeaf() {
        return false
    }
}

class PartNode extends AbstractFactory {
    public Object newInstance(FactoryBuilderSupport builder, Object nodeName, Object nodeArgs, Map nodeAttribs) {
        Part part = new Part()
        part.voices = [:]
        return part
    }

    public void setParent(FactoryBuilderSupport builder, Object parentNode, Object childNode) {
        parentNode.parts[childNode.id] = childNode
    }
    
    public boolean isLeaf() {
        return false
    }
}

class VoiceNode extends AbstractFactory {
    public Object newInstance(FactoryBuilderSupport builder, Object nodeName, Object nodeArgs, Map nodeAttribs) {
        Voice voice = new Voice()
        voice.elements = []
        return voice
    }

    public void setParent(FactoryBuilderSupport builder, Object parentNode, Object childNode) {
        parentNode.voices[childNode.id] = childNode
    }
    
    public boolean isLeaf() {
        return false
    }
}

class ChordNode extends AbstractFactory {
    public Object newInstance(FactoryBuilderSupport builder, Object nodeName, Object nodeArgs, Map nodeAttribs) {
        //return new Chord(nodeAttribs) // nodeAttribs=[midiPitch:72, duration:1/2]
        // nodeAttribs are set on onHandleNodeAttributes
        return new Chord()
    }

    public boolean onHandleNodeAttributes(FactoryBuilderSupport builder, Object node, Map attributes) {
        if (attributes.containsKey("refId")) {
            def refId = attributes.remove("refId")
            def instrument = builder.nodeReferences[refId]
            def pitch = instrument.displayPitch
            attributes['pitch'] = pitch
            attributes ['unpitched'] = true
        }
    	if (attributes.containsKey('midiPitch')) {
    		def midiPitch = attributes.remove('midiPitch')    		
    		Pitch pitch = new Pitch()
    		pitch.setMidiValue(midiPitch)
    		attributes['pitch'] = pitch
    	}
        super.onHandleNodeAttributes(builder, node, attributes)
    }

    public void setParent(FactoryBuilderSupport builder, Object parentNode, Object childNode) {
        parentNode.elements.add(childNode)
    }
    
    public boolean isLeaf() {
        return false
    }
}

class PitchNode extends AbstractFactory {
    public Object newInstance(FactoryBuilderSupport builder, Object nodeName, Object nodeArgs, Map nodeAttribs) {
        return new Pitch()
    }

    public void setParent(FactoryBuilderSupport builder, Object parentNode, Object childNode) {
        parentNode.pitches.add(childNode)
    }
    
    public boolean isLeaf() {
        return true
    }
}

class RestNode extends AbstractFactory {
    public Object newInstance(FactoryBuilderSupport builder, Object nodeName, Object nodeArgs, Map nodeAttribs) {
        return new Rest()
    }

    public void setParent(FactoryBuilderSupport builder, Object parentNode, Object childNode) {
        parentNode.elements.add(childNode)
    }
    
    public boolean isLeaf() {
        return true
    }
}

class PhraseNode extends AbstractFactory {
    public Object newInstance(FactoryBuilderSupport builder, Object nodeName, Object nodeArgs, Map nodeAttribs) {
        Phrase phrase = new Phrase()
        phrase.elements = []
        return phrase
    }

    public void setParent(FactoryBuilderSupport builder, Object parentNode, Object childNode) {
        parentNode.elements.add(childNode)
    }
    
    public boolean isLeaf() {
        return false
    }
}

class AnchorNode extends AbstractFactory {
    public Object newInstance(FactoryBuilderSupport builder, Object nodeName, Object nodeArgs, Map nodeAttribs) {
        return new Anchor()
    }

    public void setParent(FactoryBuilderSupport builder, Object parentNode, Object childNode) {
        parentNode.elements.add(childNode)
    }
    
    public boolean isLeaf() {
        return true
    }
}

class RepeatNode extends AbstractFactory {
    public Object newInstance(FactoryBuilderSupport builder, Object nodeName, Object nodeArgs, Map nodeAttribs) {
        return new Repeat()
    }

    public void setParent(FactoryBuilderSupport builder, Object parentNode, Object childNode) {
        parentNode.elements.add(childNode)
    }
    
    public boolean isLeaf() {
        return true
    }
}

class InstrumentNode extends AbstractFactory {
    public Object newInstance(FactoryBuilderSupport builder, Object nodeName, Object nodeArgs, Map nodeAttribs) {
        def instrument = new Instrument()
        if (nodeAttribs.containsKey("id")) {
            def id = nodeAttribs["id"]
            builder.nodeReferences[id] = instrument
        }
        return instrument
    }

    public void setParent(FactoryBuilderSupport builder, Object parentNode, Object childNode) {
        parentNode.elements.add(childNode)
    }
    
    public boolean isLeaf() {
        return true
    }
}

class ClefNode extends AbstractFactory {
    public Object newInstance(FactoryBuilderSupport builder, Object nodeName, Object nodeArgs, Map nodeAttribs) {
        return new Clef()
    }

    public void setParent(FactoryBuilderSupport builder, Object parentNode, Object childNode) {
        parentNode.elements.add(childNode)
    }
    
    public boolean isLeaf() {
        return true
    }
}

class KeyNode extends AbstractFactory {
    public Object newInstance(FactoryBuilderSupport builder, Object nodeName, Object nodeArgs, Map nodeAttribs) {
        return new KeySignature()
    }

    public void setParent(FactoryBuilderSupport builder, Object parentNode, Object childNode) {
        parentNode.elements.add(childNode)
    }
    
    public boolean isLeaf() {
        return true
    }
}

class TimeNode extends AbstractFactory {
    public Object newInstance(FactoryBuilderSupport builder, Object nodeName, Object nodeArgs, Map nodeAttribs) {
        return new TimeSignature()
    }

    public void setParent(FactoryBuilderSupport builder, Object parentNode, Object childNode) {
        parentNode.elements.add(childNode)
    }
    
    public boolean isLeaf() {
        return true
    }
}

class TempoNode extends AbstractFactory {
    public Object newInstance(FactoryBuilderSupport builder, Object nodeName, Object nodeArgs, Map nodeAttribs) {
        return new Tempo()
    }

    public void setParent(FactoryBuilderSupport builder, Object parentNode, Object childNode) {
        parentNode.elements.add(childNode)
    }
    
    public boolean isLeaf() {
        return true
    }
}

class TupletNode extends AbstractFactory {
    public Object newInstance(FactoryBuilderSupport builder, Object nodeName, Object nodeArgs, Map nodeAttribs) {
        return new Tuplet()
    }

    public void setParent(FactoryBuilderSupport builder, Object parentNode, Object childNode) {
        parentNode.elements.add(childNode)
    }
    
    public boolean isLeaf() {
        return false
    }
}
