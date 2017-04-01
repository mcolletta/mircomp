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

import groovy.transform.CompileStatic

import com.xenoage.utils.math.Fraction

@CompileStatic
class ScoreBuilder {

    Score score

    Map<String,Instrument> nodeReferences
    
    def ScoreBuilder() {
        nodeReferences = [:]
    }
    
    Score score(@DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=ScoreNode) Closure cl) {
        def node = new ScoreNode()
        def code = cl.rehydrate(node, this, this)
        code.resolveStrategy = Closure.DELEGATE_ONLY
        code()
        return node.score
    }

    class ScoreNode {
        Score score

        ScoreNode() {
            score = new Score()
            score.parts = [:]
        }

        def part(Map attributes, @DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=PartNode) Closure cl) {
            def node = new PartNode(attributes)
            node.setParent(score)
            def code = cl.rehydrate(node, this, this)
            code.resolveStrategy = Closure.DELEGATE_ONLY
            code()
        }
    }

    class PartNode {
        Part part
        PartNode(Map attributes) {
            part = attributes as Part
            part.voices = [:]
        }

        def voice(Map attributes, @DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=VoiceNode) Closure cl) {
            def node = new VoiceNode(attributes)
            node.setParent(part)
            def code = cl.rehydrate(node, this, this)
            code.resolveStrategy = Closure.DELEGATE_ONLY
            code()
        }

        void setParent(Score score) {
            score.parts[part.id] = part
        }
    }

    class VoiceNode {
        Voice voice

        VoiceNode(Map attributes) {
            voice = attributes as Voice
            voice.elements = []
        }

        def rest(Map attributes) {
            def node = new RestNode(attributes)
            node.setParent(voice)
        }

        def chord(Map attributes, @DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=ChordNode) Closure cl={}) {
            def node = new ChordNode(attributes)
            node.setParent(voice)
            def code = cl.rehydrate(node, this, this)
            code.resolveStrategy = Closure.DELEGATE_ONLY
            code()
        }

        def instrument(Map attributes) {
            def node = new InstrumentNode(attributes)
            node.setParent(voice)
        }

        def key(Map attributes) {
            def node = new KeySignatureNode(attributes)
            node.setParent(voice)
        }

        def clef(Map attributes) {
            def node = new ClefNode(attributes)
            node.setParent(voice)
        }

        def time(Map attributes) {
            def node = new TimeSignatureNode(attributes)
            node.setParent(voice)
        }

        def tempo(Map attributes) {
            def node = new TempoNode(attributes)
            node.setParent(voice)
        }

        def anchor(Map attributes) {
            def node = new AnchorNode(attributes)
            node.setParent(voice)
        }

        def repeat(Map attributes) {
            def node = new RepeatNode(attributes)
            node.setParent(voice)
        }

        void setParent(Part part) {
            part.voices[voice.id] = voice
        }
    }

    class RestNode {
        Rest rest

        RestNode(Map attributes) {
            rest = attributes as Rest
        }

        void setParent(Voice voice) {
            voice.elements.add(rest)
        }
    }

    class ChordNode {
        Chord chord

        ChordNode(Map attributes) {
            if (attributes.containsKey("refId")) {
                String refId = attributes.remove("refId")
                Instrument instrument = nodeReferences[refId]
                Pitch pitch = instrument.displayPitch
                attributes['pitch'] = pitch
                attributes ['unpitched'] = true
            }
            if (attributes.containsKey('midiPitch')) {
                int midiPitch = attributes.remove('midiPitch')          
                Pitch pitch = new Pitch()
                pitch.setMidiValue(midiPitch)
                attributes['pitch'] = pitch
            }
            chord = attributes as Chord
        }

        void pitch(Map attributes) {
            def node = new PitchNode(attributes)
            node.setParent(chord)
        }

        void setParent(Voice voice) {
            voice.elements.add(chord)
        }

        void setPhrase(Phrase phrase) {
            phrase.elements.add(chord)
        }

        void setTuplet(Tuplet tuplet) {
            tuplet.chords.add(chord)
        }
    }

    class PitchNode {
        Pitch pitch

        PitchNode(Map attributes) {
            pitch = attributes as Pitch
        }

        void setParent(Chord chord) {
            chord.pitches.add(pitch)
        }
    }

    class InstrumentNode {
        Instrument instrument

        InstrumentNode(Map attributes) {
            instrument = attributes as Instrument
            if (attributes.containsKey("id")) {
                String id = attributes["id"]
                nodeReferences[id] = instrument
            }
        }

        void setParent(Voice voice) {
            voice.elements.add(instrument)
        }
    }

    class PhraseNode {
        Phrase phrase

        PhraseNode(Map attributes) {
            phrase = attributes as Phrase
            phrase.elements = []
        }

        def chord(Map attributes, @DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=ChordNode) Closure cl={}) {
            def node = new ChordNode(attributes)
            node.setPhrase(phrase)
            def code = cl.rehydrate(node, this, this)
            code.resolveStrategy = Closure.DELEGATE_ONLY
            code()
        }

        void setParent(Voice voice) {
            voice.elements.add(phrase)
        }
    }

    class ClefNode {
        Clef clef

        ClefNode(Map attributes) {
            clef = attributes as Clef
        }

        void setParent(Voice voice) {
            voice.elements.add(clef)
        }
    }

    class KeySignatureNode {
        KeySignature keySignature

        KeySignatureNode(Map attributes) {
            keySignature = attributes as KeySignature
        }

        void setParent(Voice voice) {
            voice.elements.add(keySignature)
        }
    }

    class TimeSignatureNode {
        TimeSignature timeSignature

        TimeSignatureNode(Map attributes) {
            timeSignature = attributes as TimeSignature
        }

        void setParent(Voice voice) {
            voice.elements.add(timeSignature)
        }
    }

    class TempoNode {
        Tempo tempo

        TempoNode(Map attributes) {
            tempo = attributes as Tempo
        }

        void setParent(Voice voice) {
            voice.elements.add(tempo)
        }
    }

    class TupletNode {
        Tuplet tuplet

        TupletNode(Map attributes) {
            tuplet = attributes as Tuplet
        }

        def chord(Map attributes, @DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=ChordNode) Closure cl={}) {
            def node = new ChordNode(attributes)
            node.setTuplet(tuplet)
            def code = cl.rehydrate(node, this, this)
            code.resolveStrategy = Closure.DELEGATE_ONLY
            code()
        }

        void setParent(Voice voice) {
            voice.elements.add(tuplet)
        }
    }

    class AnchorNode {
        Anchor anchor

        AnchorNode(Map attributes) {
            anchor = attributes as Anchor
        }

        void setParent(Voice voice) {
            voice.elements.add(anchor)
        }
    }

    class RepeatNode {
        Repeat repeat

        RepeatNode(Map attributes) {
            repeat = attributes as Repeat
        }

        void setParent(Voice voice) {
            voice.elements.add(repeat)
        }
    }

}

