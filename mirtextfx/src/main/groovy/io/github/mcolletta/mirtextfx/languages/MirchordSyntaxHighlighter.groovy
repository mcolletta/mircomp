/*
 * Copyright (C) 2016-2022 Mirco Colletta
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

package io.github.mcolletta.mirtextfx


import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.application.Platform;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import org.reactfx.collection.ListModification;
import org.fxmisc.richtext.GenericStyledArea;
import static org.fxmisc.richtext.model.TwoDimensional.Bias.*;


public class MirChordSyntaxHighlighter implements SyntaxHighlighter {

    String name = "Mirchord"

    String toString() {
        return name
    }

    private FilteredList<Snippet> snippetList;

    public MirChordSyntaxHighlighter() {
        ObservableList<Snippet> hintList = FXCollections.observableArrayList();
        for(Map.Entry<String, List<String>> e : hints.entrySet()) {
            hintList.addAll(new Snippet(e.getKey(), e.getValue()[0], e.getValue()[1]))
        }
        snippetList = new FilteredList<Snippet>(hintList);
        snippetList.setPredicate(t -> false);
    }

     private enum MarkerType {
        OPEN_COMMENT, CLOSE_COMMENT, STRING, NONE;
    }

    private class Marker {
        int position;
        MarkerType type;

        Marker(int p, MarkerType t) {
            position = p;
            type = t;
        }
    }

    private ConcurrentSkipListSet<Marker> markers = new ConcurrentSkipListSet<Marker>(
        new Comparator<Marker>() {
        @Override
        public int compare(Marker m1, Marker m2) {
            return m1.position.compareTo(m2.position);
        }
    });

    private static final String[] KEYWORDS = new String[] {
            "def", "define", "key", "mode", "clef", "time", "tempo", "keySignature",
            "tp", "tuplet", "unpitched", "stemUp", "stemDown", "stemAuto",
            "instr", "instrument", "chordsMode",
            "call", "relative", 
            "transpose", "transposeDiatonic", "invert", "invertDiatonic",
            "augment", "diminuition", "retrograde",
            "copyTimes", "callSymbol", "zip", "chain",
            "info", "title", "composer", "poet", "lyrics", "label", "name"
    };

    
    private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
    private static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"";
    private static final String NUMBER_PATTERN =  /[0-9]+/   //  /\s[0-9]+\s/    
    private static final String OPERATOR_PATTERN = /[!|\$|%|\*|\-|\+|=|<|>|^|~]+/
    private static final String IDENTIFIER_PATTERN = /:[^()\[\]{}'"\^%`,;\s]+/
    private static final String NOTE_PATTERN = /\b(do|re|mi|fa|so|sol|la|si|ti|a|b|c|d|e|f|g|_|x|o){1}[\#|\&|§]*[,|']*[1-9]{0,2}(?![\dA-Za-z\#\&§,'])/
    private static final String CHORD_PATTERN = /\b(I|II|III|IV|V|VI|VII|A|B|C|D|E|F|G){1}[\#|\&|§]*(M|maj|m|min|mM|minMaj|\+|aug|°|dim|sus){0,1}[0-9]{0,2}[\(]{0,1}(add|sub){0,1}[\#\&§]*[0-9]{0,2}[\)]{0,1}(?![\dA-Za-z\#\&,'])/
    private static final String COMMENT_PATTERN = /;[^\n]*/;

    // Multiline
    private static final Pattern MULTILINE_PATTERN = Pattern.compile("(?<OPENCOMMENT>/\\*)|(?<CLOSECOMMENT>\\*/)")

    private static final Pattern PATTERN = Pattern.compile(
            //"(?<FUNCTION>" + FUNCTION_PATTERN + ")"
            "(?<KEYWORD>" + KEYWORD_PATTERN + ")"
            + "|(?<=\\(\\s*)(?<FUNCTION>[a-zA-Z0-9]+\\s+)"
            + "|(?<STRING>" + STRING_PATTERN + ")"
            + "|(?<NUMBER>" + NUMBER_PATTERN + ")"
            + "|(?<OPERATOR>" + OPERATOR_PATTERN + ")"
            + "|(?<IDENTIFIER>" + IDENTIFIER_PATTERN + ")"
            + "|(?<NOTE>" + NOTE_PATTERN + ")"
            + "|(?<CHORD>" + CHORD_PATTERN + ")"
            + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
    );

    private static final Map<String, List<String>> hints = [
        // parts
        "=1": [
            "=1",
            "Part 1"
        ],
        "=2": [
            "=2",
            "Part 2"
        ],
        "=3": [
            "=3",
            "Part 3"
        ],
        "=4": [
            "=4",
            "Part 4"
        ],
        "=5": [
            "=5",
            "Part 5"
        ],
        "=6": [
            "=6",
            "Part 6"
        ],
        "=7": [
            "=7",
            "Part 7"
        ],
        "=8": [
            "=8",
            "Part 8"
        ],
        "=9": [
            "=9",
            "Part 9"
        ],
        "=10": [
            "=10",
            "Part 10"
        ],
        // voices
        "~1": [
            "~1",
            "Voice 1"
        ],
        "~2": [
            "~2",
            "Voice 2"
        ],
        "~3": [
            "~3",
            "Voice 3"
        ],
        "~4": [
            "~4",
            "Voice 4"
        ],
        // Instruments
        "instr acoustic grand piano": [
                "(instr \"acoustic grand piano\")",
                "Instrument \"Acoustic Grand Piano\" with General Midi value 1"
            ],
        "instr bright acoustic piano": [
                "(instr \"bright acoustic piano\")",
                "Instrument \"Bright Acoustic Piano\" with General Midi value 2"
            ],
        "instr electric grand piano": [
                "(instr \"electric grand piano\")",
                "Instrument \"Electric Grand Piano\" with General Midi value 3"
            ],
        "instr honky-tonk piano": [
                "(instr \"honky-tonk piano\")",
                "Instrument \"Honky-tonk Piano\" with General Midi value 4"
            ],
        "instr electric piano 1": [
                "(instr \"electric piano 1\")",
                "Instrument \"Electric Piano 1\" with General Midi value 5"
            ],
        "instr electric piano 2": [
                "(instr \"electric piano 2\")",
                "Instrument \"Electric Piano 2\" with General Midi value 6"
            ],
        "instr harpsichord": [
                "(instr \"harpsichord\")",
                "Instrument \"Harpsichord\" with General Midi value 7"
            ],
        "instr clav": [
                "(instr \"clav\")",
                "Instrument \"Clavi\" with General Midi value 8"
            ],
        "instr celesta": [
                "(instr \"celesta\")",
                "Instrument \"Celesta\" with General Midi value 9"
            ],
        "instr glockenspiel": [
                "(instr \"glockenspiel\")",
                "Instrument \"Glockenspiel\" with General Midi value 10"
            ],
        "instr music box": [
                "(instr \"music box\")",
                "Instrument \"Music Box\" with General Midi value 11"
            ],
        "instr vibraphone": [
                "(instr \"vibraphone\")",
                "Instrument \"Vibraphone\" with General Midi value 12"
            ],
        "instr marimba": [
                "(instr \"marimba\")",
                "Instrument \"Marimba\" with General Midi value 13"
            ],
        "instr xylophone": [
                "(instr \"xylophone\")",
                "Instrument \"Xylophone\" with General Midi value 14"
            ],
        "instr tubular bells": [
                "(instr \"tubular bells\")",
                "Instrument \"Tubular Bells\" with General Midi value 15"
            ],
        "instr dulcimer": [
                "(instr \"dulcimer\")",
                "Instrument \"Dulcimer\" with General Midi value 16"
            ],
        "instr drawbar organ": [
                "(instr \"drawbar organ\")",
                "Instrument \"Drawbar Organ\" with General Midi value 17"
            ],
        "instr percussive organ": [
                "(instr \"percussive organ\")",
                "Instrument \"Percussive Organ\" with General Midi value 18"
            ],
        "instr rock organ": [
                "(instr \"rock organ\")",
                "Instrument \"Rock Organ\" with General Midi value 19"
            ],
        "instr church organ": [
                "(instr \"church organ\")",
                "Instrument \"Church Organ\" with General Midi value 20"
            ],
        "instr reed organ": [
                "(instr \"reed organ\")",
                "Instrument \"Reed Organ\" with General Midi value 21"
            ],
        "instr accordion": [
                "(instr \"accordion\")",
                "Instrument \"Accordion\" with General Midi value 22"
            ],
        "instr harmonica": [
                "(instr \"harmonica\")",
                "Instrument \"Harmonica\" with General Midi value 23"
            ],
        "instr concertina": [
                "(instr \"concertina\")",
                "Instrument \"Tango Accordion\" with General Midi value 24"
            ],
        "instr acoustic guitar (nylon)": [
                "(instr \"acoustic guitar (nylon)\")",
                "Instrument \"Acoustic Guitar (nylon)\" with General Midi value 25"
            ],
        "instr acoustic guitar (steel)": [
                "(instr \"acoustic guitar (steel)\")",
                "Instrument \"Acoustic Guitar (steel)\" with General Midi value 26"
            ],
        "instr electric guitar (jazz)": [
                "(instr \"electric guitar (jazz)\")",
                "Instrument \"Electric Guitar (jazz)\" with General Midi value 27"
            ],
        "instr electric guitar (clean)": [
                "(instr \"electric guitar (clean)\")",
                "Instrument \"Electric Guitar (clean)\" with General Midi value 28"
            ],
        "instr electric guitar (muted)": [
                "(instr \"electric guitar (muted)\")",
                "Instrument \"Electric Guitar (muted)\" with General Midi value 29"
            ],
        "instr overdriven guitar": [
                "(instr \"overdriven guitar\")",
                "Instrument \"Overdriven Guitar\" with General Midi value 30"
            ],
        "instr distorted guitar": [
                "(instr \"distorted guitar\")",
                "Instrument \"Distortion Guitar\" with General Midi value 31"
            ],
        "instr guitar harmonics": [
                "(instr \"guitar harmonics\")",
                "Instrument \"Guitar harmonics\" with General Midi value 32"
            ],
        "instr acoustic bass": [
                "(instr \"acoustic bass\")",
                "Instrument \"Acoustic Bass\" with General Midi value 33"
            ],
        "instr electric bass (finger)": [
                "(instr \"electric bass (finger)\")",
                "Instrument \"Electric Bass (finger)\" with General Midi value 34"
            ],
        "instr electric bass (pick)": [
                "(instr \"electric bass (pick)\")",
                "Instrument \"Electric Bass (pick)\" with General Midi value 35"
            ],
        "instr fretless bass": [
                "(instr \"fretless bass\")",
                "Instrument \"Fretless Bass\" with General Midi value 36"
            ],
        "instr slap bass 1": [
                "(instr \"slap bass 1\")",
                "Instrument \"Slap Bass 1\" with General Midi value 37"
            ],
        "instr slap bass 2": [
                "(instr \"slap bass 2\")",
                "Instrument \"Slap Bass 2\" with General Midi value 38"
            ],
        "instr synth bass 1": [
                "(instr \"synth bass 1\")",
                "Instrument \"Synth Bass 1\" with General Midi value 39"
            ],
        "instr synth bass 2": [
                "(instr \"synth bass 2\")",
                "Instrument \"Synth Bass 2\" with General Midi value 40"
            ],
        "instr violin": [
                "(instr \"violin\")",
                "Instrument \"Violin\" with General Midi value 41"
            ],
        "instr viola": [
                "(instr \"viola\")",
                "Instrument \"Viola\" with General Midi value 42"
            ],
        "instr cello": [
                "(instr \"cello\")",
                "Instrument \"Cello\" with General Midi value 43"
            ],
        "instr contrabass": [
                "(instr \"contrabass\")",
                "Instrument \"Contrabass\" with General Midi value 44"
            ],
        "instr tremolo strings": [
                "(instr \"tremolo strings\")",
                "Instrument \"Tremolo Strings\" with General Midi value 45"
            ],
        "instr pizzicato strings": [
                "(instr \"pizzicato strings\")",
                "Instrument \"Pizzicato Strings\" with General Midi value 46"
            ],
        "instr orchestral harp": [
                "(instr \"orchestral harp\")",
                "Instrument \"Orchestral Harp\" with General Midi value 47"
            ],
        "instr timpani": [
                "(instr \"timpani\")",
                "Instrument \"Timpani\" with General Midi value 48"
            ],
        "instr string ensemble 1": [
                "(instr \"string ensemble 1\")",
                "Instrument \"String Ensemble 1\" with General Midi value 49"
            ],
        "instr string ensemble 2": [
                "(instr \"string ensemble 2\")",
                "Instrument \"String Ensemble 2\" with General Midi value 50"
            ],
        "instr synthstrings 1": [
                "(instr \"synthstrings 1\")",
                "Instrument \"SynthStrings 1\" with General Midi value 51"
            ],
        "instr synthstrings 2": [
                "(instr \"synthstrings 2\")",
                "Instrument \"SynthStrings 2\" with General Midi value 52"
            ],
        "instr choir aahs": [
                "(instr \"choir aahs\")",
                "Instrument \"Choir Aahs\" with General Midi value 53"
            ],
        "instr voice oohs": [
                "(instr \"voice oohs\")",
                "Instrument \"Voice Oohs\" with General Midi value 54"
            ],
        "instr synth voice": [
                "(instr \"synth voice\")",
                "Instrument \"Synth Voice\" with General Midi value 55"
            ],
        "instr orchestra hit": [
                "(instr \"orchestra hit\")",
                "Instrument \"Orchestra Hit\" with General Midi value 56"
            ],
        "instr trumpet": [
                "(instr \"trumpet\")",
                "Instrument \"Trumpet\" with General Midi value 57"
            ],
        "instr trombone": [
                "(instr \"trombone\")",
                "Instrument \"Trombone\" with General Midi value 58"
            ],
        "instr tuba": [
                "(instr \"tuba\")",
                "Instrument \"Tuba\" with General Midi value 59"
            ],
        "instr muted trumpet": [
                "(instr \"muted trumpet\")",
                "Instrument \"Muted Trumpet\" with General Midi value 60"
            ],
        "instr french horn": [
                "(instr \"french horn\")",
                "Instrument \"French Horn\" with General Midi value 61"
            ],
        "instr brass section": [
                "(instr \"brass section\")",
                "Instrument \"Brass Section\" with General Midi value 62"
            ],
        "instr synthbrass 1": [
                "(instr \"synthbrass 1\")",
                "Instrument \"SynthBrass 1\" with General Midi value 63"
            ],
        "instr synthbrass 2": [
                "(instr \"synthbrass 2\")",
                "Instrument \"SynthBrass 2\" with General Midi value 64"
            ],
        "instr soprano sax": [
                "(instr \"soprano sax\")",
                "Instrument \"Soprano Sax\" with General Midi value 65"
            ],
        "instr alto sax": [
                "(instr \"alto sax\")",
                "Instrument \"Alto Sax\" with General Midi value 66"
            ],
        "instr tenor sax": [
                "(instr \"tenor sax\")",
                "Instrument \"Tenor Sax\" with General Midi value 67"
            ],
        "instr baritone sax": [
                "(instr \"baritone sax\")",
                "Instrument \"Baritone Sax\" with General Midi value 68"
            ],
        "instr oboe": [
                "(instr \"oboe\")",
                "Instrument \"Oboe\" with General Midi value 69"
            ],
        "instr english horn": [
                "(instr \"english horn\")",
                "Instrument \"English Horn\" with General Midi value 70"
            ],
        "instr bassoon": [
                "(instr \"bassoon\")",
                "Instrument \"Bassoon\" with General Midi value 71"
            ],
        "instr clarinet": [
                "(instr \"clarinet\")",
                "Instrument \"Clarinet\" with General Midi value 72"
            ],
        "instr piccolo": [
                "(instr \"piccolo\")",
                "Instrument \"Piccolo\" with General Midi value 73"
            ],
        "instr flute": [
                "(instr \"flute\")",
                "Instrument \"Flute\" with General Midi value 74"
            ],
        "instr recorder": [
                "(instr \"recorder\")",
                "Instrument \"Recorder\" with General Midi value 75"
            ],
        "instr pan flute": [
                "(instr \"pan flute\")",
                "Instrument \"Pan Flute\" with General Midi value 76"
            ],
        "instr blown bottle": [
                "(instr \"blown bottle\")",
                "Instrument \"Blown Bottle\" with General Midi value 77"
            ],
        "instr shakuhachi": [
                "(instr \"shakuhachi\")",
                "Instrument \"Shakuhachi\" with General Midi value 78"
            ],
        "instr whistle": [
                "(instr \"whistle\")",
                "Instrument \"Whistle\" with General Midi value 79"
            ],
        "instr ocarina": [
                "(instr \"ocarina\")",
                "Instrument \"Ocarina\" with General Midi value 80"
            ],
        "instr lead 1 (square)": [
                "(instr \"lead 1 (square)\")",
                "Instrument \"Lead 1 (square)\" with General Midi value 81"
            ],
        "instr lead 2 (sawtooth)": [
                "(instr \"lead 2 (sawtooth)\")",
                "Instrument \"Lead 2 (sawtooth)\" with General Midi value 82"
            ],
        "instr lead 3 (calliope)": [
                "(instr \"lead 3 (calliope)\")",
                "Instrument \"Lead 3 (calliope)\" with General Midi value 83"
            ],
        "instr lead 4 (chiff)": [
                "(instr \"lead 4 (chiff)\")",
                "Instrument \"Lead 4 (chiff)\" with General Midi value 84"
            ],
        "instr lead 5 (charang)": [
                "(instr \"lead 5 (charang)\")",
                "Instrument \"Lead 5 (charang)\" with General Midi value 85"
            ],
        "instr lead 6 (voice)": [
                "(instr \"lead 6 (voice)\")",
                "Instrument \"Lead 6 (voice)\" with General Midi value 86"
            ],
        "instr lead 7 (fifths)": [
                "(instr \"lead 7 (fifths)\")",
                "Instrument \"Lead 7 (fifths)\" with General Midi value 87"
            ],
        "instr lead 8 (bass+lead)": [
                "(instr \"lead 8 (bass+lead)\")",
                "Instrument \"Lead 8 (bass + lead)\" with General Midi value 88"
            ],
        "instr pad 1 (new age)": [
                "(instr \"pad 1 (new age)\")",
                "Instrument \"Pad 1 (new age)\" with General Midi value 89"
            ],
        "instr pad 2 (warm)": [
                "(instr \"pad 2 (warm)\")",
                "Instrument \"Pad 2 (warm)\" with General Midi value 90"
            ],
        "instr pad 3 (polysynth)": [
                "(instr \"pad 3 (polysynth)\")",
                "Instrument \"Pad 3 (polysynth)\" with General Midi value 91"
            ],
        "instr pad 4 (choir)": [
                "(instr \"pad 4 (choir)\")",
                "Instrument \"Pad 4 (choir)\" with General Midi value 92"
            ],
        "instr pad 5 (bowed)": [
                "(instr \"pad 5 (bowed)\")",
                "Instrument \"Pad 5 (bowed)\" with General Midi value 93"
            ],
        "instr pad 6 (metallic)": [
                "(instr \"pad 6 (metallic)\")",
                "Instrument \"Pad 6 (metallic)\" with General Midi value 94"
            ],
        "instr pad 7 (halo)": [
                "(instr \"pad 7 (halo)\")",
                "Instrument \"Pad 7 (halo)\" with General Midi value 95"
            ],
        "instr pad 8 (sweep)": [
                "(instr \"pad 8 (sweep)\")",
                "Instrument \"Pad 8 (sweep)\" with General Midi value 96"
            ],
        "instr fx 1 (rain)": [
                "(instr \"fx 1 (rain)\")",
                "Instrument \"FX 1 (rain)\" with General Midi value 97"
            ],
        "instr fx 2 (soundtrack)": [
                "(instr \"fx 2 (soundtrack)\")",
                "Instrument \"FX 2 (soundtrack)\" with General Midi value 98"
            ],
        "instr fx 3 (crystal)": [
                "(instr \"fx 3 (crystal)\")",
                "Instrument \"FX 3 (crystal)\" with General Midi value 99"
            ],
        "instr fx 4 (atmosphere)": [
                "(instr \"fx 4 (atmosphere)\")",
                "Instrument \"FX 4 (atmosphere)\" with General Midi value 100"
            ],
        "instr fx 5 (brightness)": [
                "(instr \"fx 5 (brightness)\")",
                "Instrument \"FX 5 (brightness)\" with General Midi value 101"
            ],
        "instr fx 6 (goblins)": [
                "(instr \"fx 6 (goblins)\")",
                "Instrument \"FX 6 (goblins)\" with General Midi value 102"
            ],
        "instr fx 7 (echoes)": [
                "(instr \"fx 7 (echoes)\")",
                "Instrument \"FX 7 (echoes)\" with General Midi value 103"
            ],
        "instr fx 8 (sci-fi)": [
                "(instr \"fx 8 (sci-fi)\")",
                "Instrument \"FX 8 (sci-fi)\" with General Midi value 104"
            ],
        "instr sitar": [
                "(instr \"sitar\")",
                "Instrument \"Sitar\" with General Midi value 105"
            ],
        "instr banjo": [
                "(instr \"banjo\")",
                "Instrument \"Banjo\" with General Midi value 106"
            ],
        "instr shamisen": [
                "(instr \"shamisen\")",
                "Instrument \"Shamisen\" with General Midi value 107"
            ],
        "instr koto": [
                "(instr \"koto\")",
                "Instrument \"Koto\" with General Midi value 108"
            ],
        "instr kalimba": [
                "(instr \"kalimba\")",
                "Instrument \"Kalimba\" with General Midi value 109"
            ],
        "instr bagpipe": [
                "(instr \"bagpipe\")",
                "Instrument \"Bag pipe\" with General Midi value 110"
            ],
        "instr fiddle": [
                "(instr \"fiddle\")",
                "Instrument \"Fiddle\" with General Midi value 111"
            ],
        "instr shanai": [
                "(instr \"shanai\")",
                "Instrument \"Shanai\" with General Midi value 112"
            ],
        "instr tinkle bell": [
                "(instr \"tinkle bell\")",
                "Instrument \"Tinkle Bell\" with General Midi value 113"
            ],
        "instr agogo": [
                "(instr \"agogo\")",
                "Instrument \"Agogo\" with General Midi value 114"
            ],
        "instr steel drums": [
                "(instr \"steel drums\")",
                "Instrument \"Steel Drums\" with General Midi value 115"
            ],
        "instr woodblock": [
                "(instr \"woodblock\")",
                "Instrument \"Woodblock\" with General Midi value 116"
            ],
        "instr taiko drum": [
                "(instr \"taiko drum\")",
                "Instrument \"Taiko Drum\" with General Midi value 117"
            ],
        "instr melodic tom": [
                "(instr \"melodic tom\")",
                "Instrument \"Melodic Tom\" with General Midi value 118"
            ],
        "instr synth drum": [
                "(instr \"synth drum\")",
                "Instrument \"Synth Drum\" with General Midi value 119"
            ],
        "instr reverse cymbal": [
                "(instr \"reverse cymbal\")",
                "Instrument \"Reverse Cymbal\" with General Midi value 120"
            ],
        "instr guitar fret noise": [
                "(instr \"guitar fret noise\")",
                "Instrument \"Guitar Fret Noise\" with General Midi value 121"
            ],
        "instr breath noise": [
                "(instr \"breath noise\")",
                "Instrument \"Breath Noise\" with General Midi value 122"
            ],
        "instr seashore": [
                "(instr \"seashore\")",
                "Instrument \"Seashore\" with General Midi value 123"
            ],
        "instr bird tweet": [
                "(instr \"bird tweet\")",
                "Instrument \"Bird Tweet\" with General Midi value 124"
            ],
        "instr telephone ring": [
                "(instr \"telephone ring\")",
                "Instrument \"Telephone Ring\" with General Midi value 125"
            ],
        "instr helicopter": [
                "(instr \"helicopter\")",
                "Instrument \"Helicopter\" with General Midi value 126"
            ],
        "instr applause": [
                "(instr \"applause\")",
                "Instrument \"Applause\" with General Midi value 127"
            ],
        "instr gunshot": [
                "(instr \"gunshot\")",
                "Instrument \"Gunshot\" with General Midi value 128"
            ],
        // unpitched
            "unpitched acousticbassdrum": [
                "(unpitched \"acousticbassdrum\" \"C\" 5)",
                "Unpitched Instrument \"Acoustic Bass Drum\" with General Midi value 35"
            ],
        "unpitched bassdrum": [
                "(unpitched \"bassdrum\" \"D\" 5)",
                "Unpitched Instrument \"Bass Drum 1\" with General Midi value 36"
            ],
        "unpitched hisidestick": [
                "(unpitched \"hisidestick\" \"E\" 5)",
                "Unpitched Instrument \"Side Stick\" with General Midi value 37"
            ],
        "unpitched sidestick": [
                "(unpitched \"sidestick\" \"E\" 5)",
                "Unpitched Instrument \"Side Stick\" with General Midi value 37"
            ],
        "unpitched losidestick": [
                "(unpitched \"losidestick\" \"E\" 5)",
                "Unpitched Instrument \"Side Stick\" with General Midi value 37"
            ],
        "unpitched acousticsnare": [
                "(unpitched \"acousticsnare\" \"F\" 5)",
                "Unpitched Instrument \"Acoustic Snare\" with General Midi value 38"
            ],
        "unpitched snare": [
                "(unpitched \"snare\" \"F\" 5)",
                "Unpitched Instrument \"Acoustic Snare\" with General Midi value 38"
            ],
        "unpitched handclap": [
                "(unpitched \"handclap\" \"G\" 5)",
                "Unpitched Instrument \"Hand Clap\" with General Midi value 39"
            ],
        "unpitched electricsnare": [
                "(unpitched \"electricsnare\" \"A\" 5)",
                "Unpitched Instrument \"Electric Snare\" with General Midi value 40"
            ],
        "unpitched lowfloortom": [
                "(unpitched \"lowfloortom\" \"B\" 5)",
                "Unpitched Instrument \"Low Floor Tom\" with General Midi value 41"
            ],
        "unpitched closedhihat": [
                "(unpitched \"closedhihat\" \"C\" 5)",
                "Unpitched Instrument \"Closed Hi Hat\" with General Midi value 42"
            ],
        "unpitched hihat": [
                "(unpitched \"hihat\" \"C\" 5)",
                "Unpitched Instrument \"Closed Hi Hat\" with General Midi value 42"
            ],
        "unpitched highfloortom": [
                "(unpitched \"highfloortom\" \"D\" 5)",
                "Unpitched Instrument \"High Floor Tom\" with General Midi value 43"
            ],
        "unpitched pedalhihat": [
                "(unpitched \"pedalhihat\" \"E\" 5)",
                "Unpitched Instrument \"Pedal Hi-Hat\" with General Midi value 44"
            ],
        "unpitched lowtom": [
                "(unpitched \"lowtom\" \"F\" 5)",
                "Unpitched Instrument \"Low Tom\" with General Midi value 45"
            ],
        "unpitched openhihat": [
                "(unpitched \"openhihat\" \"G\" 5)",
                "Unpitched Instrument \"Open Hi-Hat\" with General Midi value 46"
            ],
        "unpitched halfopenhihat": [
                "(unpitched \"halfopenhihat\" \"G\" 5)",
                "Unpitched Instrument \"Open Hi-Hat\" with General Midi value 46"
            ],
        "unpitched lowmidtom": [
                "(unpitched \"lowmidtom\" \"A\" 5)",
                "Unpitched Instrument \"Low-Mid Tom\" with General Midi value 47"
            ],
        "unpitched himidtom": [
                "(unpitched \"himidtom\" \"B\" 5)",
                "Unpitched Instrument \"Hi-Mid Tom\" with General Midi value 48"
            ],
        "unpitched crashcymbala": [
                "(unpitched \"crashcymbala\" \"C\" 5)",
                "Unpitched Instrument \"Crash Cymbal 1\" with General Midi value 49"
            ],
        "unpitched crashcymbal": [
                "(unpitched \"crashcymbal\" \"C\" 5)",
                "Unpitched Instrument \"Crash Cymbal 1\" with General Midi value 49"
            ],
        "unpitched hightom": [
                "(unpitched \"hightom\" \"D\" 5)",
                "Unpitched Instrument \"High Tom\" with General Midi value 50"
            ],
        "unpitched ridecymbala": [
                "(unpitched \"ridecymbala\" \"E\" 5)",
                "Unpitched Instrument \"Ride Cymbal 1\" with General Midi value 51"
            ],
        "unpitched ridecymbal": [
                "(unpitched \"ridecymbal\" \"E\" 5)",
                "Unpitched Instrument \"Ride Cymbal 1\" with General Midi value 51"
            ],
        "unpitched chinesecymbal": [
                "(unpitched \"chinesecymbal\" \"F\" 5)",
                "Unpitched Instrument \"Chinese Cymbal\" with General Midi value 52"
            ],
        "unpitched ridebell": [
                "(unpitched \"ridebell\" \"G\" 5)",
                "Unpitched Instrument \"Ride Bell\" with General Midi value 53"
            ],
        "unpitched tambourine": [
                "(unpitched \"tambourine\" \"A\" 5)",
                "Unpitched Instrument \"Tambourine\" with General Midi value 54"
            ],
        "unpitched splashcymbal": [
                "(unpitched \"splashcymbal\" \"B\" 5)",
                "Unpitched Instrument \"Splash Cymbal\" with General Midi value 55"
            ],
        "unpitched cowbell": [
                "(unpitched \"cowbell\" \"C\" 5)",
                "Unpitched Instrument \"Cowbell\" with General Midi value 56"
            ],
        "unpitched crashcymbalb": [
                "(unpitched \"crashcymbalb\" \"D\" 5)",
                "Unpitched Instrument \"Crash Cymbal 2\" with General Midi value 57"
            ],
        "unpitched vibraslap": [
                "(unpitched \"vibraslap\" \"E\" 5)",
                "Unpitched Instrument \"Vibraslap\" with General Midi value 58"
            ],
        "unpitched ridecymbalb": [
                "(unpitched \"ridecymbalb\" \"F\" 5)",
                "Unpitched Instrument \"Ride Cymbal 2\" with General Midi value 59"
            ],
        "unpitched mutehibongo": [
                "(unpitched \"mutehibongo\" \"G\" 5)",
                "Unpitched Instrument \"Hi Bongo\" with General Midi value 60"
            ],
        "unpitched hibongo": [
                "(unpitched \"hibongo\" \"G\" 5)",
                "Unpitched Instrument \"Hi Bongo\" with General Midi value 60"
            ],
        "unpitched openhibongo": [
                "(unpitched \"openhibongo\" \"G\" 5)",
                "Unpitched Instrument \"Hi Bongo\" with General Midi value 60"
            ],
        "unpitched mutelobongo": [
                "(unpitched \"mutelobongo\" \"A\" 5)",
                "Unpitched Instrument \"Low Bongo\" with General Midi value 61"
            ],
        "unpitched lobongo": [
                "(unpitched \"lobongo\" \"A\" 5)",
                "Unpitched Instrument \"Low Bongo\" with General Midi value 61"
            ],
        "unpitched openlobongo": [
                "(unpitched \"openlobongo\" \"A\" 5)",
                "Unpitched Instrument \"Low Bongo\" with General Midi value 61"
            ],
        "unpitched mutehiconga": [
                "(unpitched \"mutehiconga\" \"B\" 5)",
                "Unpitched Instrument \"Mute Hi Conga\" with General Midi value 62"
            ],
        "unpitched muteloconga": [
                "(unpitched \"muteloconga\" \"B\" 5)",
                "Unpitched Instrument \"Mute Hi Conga\" with General Midi value 62"
            ],
        "unpitched openhiconga": [
                "(unpitched \"openhiconga\" \"C\" 5)",
                "Unpitched Instrument \"Open Hi Conga\" with General Midi value 63"
            ],
        "unpitched hiconga": [
                "(unpitched \"hiconga\" \"C\" 5)",
                "Unpitched Instrument \"Open Hi Conga\" with General Midi value 63"
            ],
        "unpitched openloconga": [
                "(unpitched \"openloconga\" \"D\" 5)",
                "Unpitched Instrument \"Low Conga\" with General Midi value 64"
            ],
        "unpitched loconga": [
                "(unpitched \"loconga\" \"D\" 5)",
                "Unpitched Instrument \"Low Conga\" with General Midi value 64"
            ],
        "unpitched hitimbale": [
                "(unpitched \"hitimbale\" \"E\" 5)",
                "Unpitched Instrument \"High Timbale\" with General Midi value 65"
            ],
        "unpitched lotimbale": [
                "(unpitched \"lotimbale\" \"F\" 5)",
                "Unpitched Instrument \"Low Timbale\" with General Midi value 66"
            ],
        "unpitched hiagogo": [
                "(unpitched \"hiagogo\" \"G\" 5)",
                "Unpitched Instrument \"High Agogo\" with General Midi value 67"
            ],
        "unpitched loagogo": [
                "(unpitched \"loagogo\" \"A\" 5)",
                "Unpitched Instrument \"Low Agogo\" with General Midi value 68"
            ],
        "unpitched cabasa": [
                "(unpitched \"cabasa\" \"B\" 5)",
                "Unpitched Instrument \"Cabasa\" with General Midi value 69"
            ],
        "unpitched maracas": [
                "(unpitched \"maracas\" \"C\" 5)",
                "Unpitched Instrument \"Maracas\" with General Midi value 70"
            ],
        "unpitched shortwhistle": [
                "(unpitched \"shortwhistle\" \"D\" 5)",
                "Unpitched Instrument \"Short Whistle\" with General Midi value 71"
            ],
        "unpitched longwhistle": [
                "(unpitched \"longwhistle\" \"E\" 5)",
                "Unpitched Instrument \"Long Whistle\" with General Midi value 72"
            ],
        "unpitched shortguiro": [
                "(unpitched \"shortguiro\" \"F\" 5)",
                "Unpitched Instrument \"Short Guiro\" with General Midi value 73"
            ],
        "unpitched longguiro": [
                "(unpitched \"longguiro\" \"G\" 5)",
                "Unpitched Instrument \"Long Guiro\" with General Midi value 74"
            ],
        "unpitched guiro": [
                "(unpitched \"guiro\" \"G\" 5)",
                "Unpitched Instrument \"Long Guiro\" with General Midi value 74"
            ],
        "unpitched claves": [
                "(unpitched \"claves\" \"A\" 5)",
                "Unpitched Instrument \"Claves\" with General Midi value 75"
            ],
        "unpitched hiwoodblock": [
                "(unpitched \"hiwoodblock\" \"B\" 5)",
                "Unpitched Instrument \"Hi Wood Block\" with General Midi value 76"
            ],
        "unpitched lowoodblock": [
                "(unpitched \"lowoodblock\" \"C\" 5)",
                "Unpitched Instrument \"Low Wood Block\" with General Midi value 77"
            ],
        "unpitched mutecuica": [
                "(unpitched \"mutecuica\" \"D\" 5)",
                "Unpitched Instrument \"Mute Cuica\" with General Midi value 78"
            ],
        "unpitched opencuica": [
                "(unpitched \"opencuica\" \"E\" 5)",
                "Unpitched Instrument \"Open Cuica\" with General Midi value 79"
            ],
        "unpitched mutetriangle": [
                "(unpitched \"mutetriangle\" \"F\" 5)",
                "Unpitched Instrument \"Mute Triangle\" with General Midi value 80"
            ],
        "unpitched triangle": [
                "(unpitched \"triangle\" \"G\" 5)",
                "Unpitched Instrument \"Open Triangle\" with General Midi value 81"
            ],
        "unpitched opentriangle": [
                "(unpitched \"opentriangle\" \"G\" 5)",
                "Unpitched Instrument \"Open Triangle\" with General Midi value 81"
            ],
        // key signature
        // sharps
        "key C major": [
                "(key \"C\" \"maj\")",
                "Key signature \"C major\" with 0 sharps"
            ],
        "key A minor": [
                "(key \"A\" \"min\")",
                "Key signature \"A minor\" with 0 sharps"
            ],
        "key G major": [
                "(key \"G\" \"maj\")",
                "Key signature \"G major\" with 1 sharps"
            ],
        "key E minor": [
                "(key \"E\" \"min\")",
                "Key signature \"E minor\" with 1 sharps"
            ],
        "key D major": [
                "(key \"D\" \"maj\")",
                "Key signature \"D major\" with 2 sharps"
            ],
        "key B minor": [
                "(key \"B\" \"min\")",
                "Key signature \"B minor\" with 2 sharps"
            ],
        "key A major": [
                "(key \"A\" \"maj\")",
                "Key signature \"A major\" with 3 sharps"
            ],
        "key F# minor": [
                "(key \"F#\" \"min\")",
                "Key signature \"F\u266F; minor\" with 3 sharps"
            ],
        "key E major": [
                "(key \"E\" \"maj\")",
                "Key signature \"E major\" with 4 sharps"
            ],
        "key C# minor": [
                "(key \"C#\" \"min\")",
                "Key signature \"C\u266F; minor\" with 4 sharps"
            ],
        "key B major": [
                "(key \"B\" \"maj\")",
                "Key signature \"B major\" with 5 sharps"
            ],
        "key G# minor": [
                "(key \"G#\" \"min\")",
                "Key signature \"G\u266F; minor\" with 5 sharps"
            ],
        "key F# major": [
                "(key \"F#\" \"maj\")",
                "Key signature \"F\u266F; major\" with 6 sharps"
            ],
        "key D# minor": [
                "(key \"D#\" \"min\")",
                "Key signature \"D\u266F; minor\" with 6 sharps"
            ],
        "key C# major": [
                "(key \"C#\" \"maj\")",
                "Key signature \"C\u266F; major\" with 7 sharps"
            ],
        "key A# minor": [
                "(key \"A#\" \"min\")",
                "Key signature \"A\u266F; minor\" with 7 sharps"
            ],
        // flats
        "key F major": [
                "(key \"F\" \"maj\")",
                "Key signature \"F major\" with 1 flats"
            ],
        "key D minor": [
                "(key \"D\" \"min\")",
                "Key signature \"D minor\" with 1 flats"
            ],
        "key Bb major": [
                "(key \"B&\" \"maj\")",
                "Key signature \"B\u266D; major\" with 2 flats"
            ],
        "key G minor": [
                "(key \"G\" \"min\")",
                "Key signature \"G minor\" with 2 flats"
            ],
        "key Eb major": [
                "(key \"E&\" \"maj\")",
                "Key signature \"E\u266D; major\" with 3 flats"
            ],
        "key C minor": [
                "(key \"C\" \"min\")",
                "Key signature \"C minor\" with 3 flats"
            ],
        "key Ab major": [
                "(key \"A&\" \"maj\")",
                "Key signature \"A\u266D; major\" with 4 flats"
            ],
        "key F minor": [
                "(key \"F\" \"min\")",
                "Key signature \"F minor\" with 4 flats"
            ],
        "key Db major": [
                "(key \"D&\" \"maj\")",
                "Key signature \"D\u266D; major\" with 5 flats"
            ],
        "key Bb minor": [
                "(key \"B&\" \"min\")",
                "Key signature \"B\u266D; minor\" with 5 flats"
            ],
        "key Gb major": [
                "(key \"G&\" \"maj\")",
                "Key signature \"G\u266D; major\" with 6 flats"
            ],
        "key Eb minor": [
                "(key \"E&\" \"min\")",
                "Key signature \"E\u266D; minor\" with 6 flats"
            ],
        "key Cb major": [
                "(key \"C&\" \"maj\")",
                "Key signature \"C\u266D; major\" with 7 flats"
            ],
        "key Ab minor": [
                "(key \"A&\" \"min\")",
                "Key signature \"A\u266D; minor\" with 7 flats"
            ],
        // clef
        "clef treble": [
            "(clef \"treble\")",
            "Treble Clef"
        ],
        "clef bass": [
            "(clef \"bass\")",
            "Bass Clef"
        ],
        "clef percussion": [
            "(clef \"percussion\")",
            "Percussion Clef"
        ],
        // time
        "time 4/4": [
            "(time 4 4)",
            "Time signature 4/4"
        ],
        "time 2/4": [
            "(time 2 4)",
            "Time signature 2/4"
        ],
        "time 6/8": [
            "(time 6 8)",
            "Time signature 6/8"
        ],
        // transformations
        "transpose": [
            "(transpose 2 {})",
            "Phrase transpose(int halfSteps, Phrase phrase)"
        ],
        "transposeDiatonic": [
            "(transposeDiatonic 2 \"maj\" {})",
            "Phrase transposeDiatonic(int diatonicSteps, String modeText, Phrase phrase)"
        ],
        "invert": [
            "(invert {})",
            "Phrase invert(Phrase phrase)"
        ],
        "invertDiatonic": [
            "(invertDiatonic \"maj\" {})",
            "Phrase invertDiatonic(String modeText, Phrase phrase)"
        ],
        "retrograde": [
            "(retrograde {})",
            "Phrase retrograde(Phrase phrase)"
        ],
        "augment": [
            "(augment \"2/1\" {})",
            "Phrase augment(String ratio, Phrase phrase)"
        ],
        "diminuition": [
            "(diminuition \"2/1\" {})",
            "Phrase diminuition(String ratio, Phrase phrase)"
        ],
        "zip": [
            "(zip {} {})",
            "Phrase zip(Phrase phrase, Phrase pattern)"
        ]
    ]

    public FilteredList<Snippet> getSnippets() {
        snippetList.setPredicate( t -> false )
        return snippetList;
    }

    public void updateSnippets(String filter) {
        if (filter != null && !filter.isEmpty())
            snippetList.setPredicate( t -> (t != null) ? t.getId().contains(filter) : false );
        else
            snippetList.setPredicate( t -> false )
    }

    public void highlight(CodeArea codeArea, int position, String inserted, String removed) {

        if (position < 0) { // full highlight
            codeArea.setStyleSpans(0, computeHighlighting(codeArea.getContent().getText()));
            return
        }

        int startPos = 0
        int endPos = 0

        if (inserted.contains("\n")) {
            int startPar = codeArea.offsetToPosition(position, Forward).getMajor();
            int endPar = codeArea.offsetToPosition(position + inserted.length(), Backward).getMajor();
            startPos = codeArea.getAbsolutePosition(startPar, 0);
            endPos = codeArea.getAbsolutePosition(endPar, codeArea.getParagraphLength(endPar));
        } else {
            int currentParagraph = codeArea.getCurrentParagraph();
            startPos = codeArea.getAbsolutePosition( currentParagraph , 0 );
            endPos = codeArea.getAbsolutePosition( currentParagraph, codeArea.getParagraphLength( currentParagraph ) );
        }

        String text = codeArea.getText(startPos, endPos);
        codeArea.setStyleSpans(startPos, computeHighlighting(text));
    }

    private StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        while(matcher.find()) {
            String styleClass =
                    matcher.group("KEYWORD") != null ? "keyword" :
                    matcher.group("FUNCTION") != null ? "function" :
                    matcher.group("STRING") != null ? "string" :
                    matcher.group("NUMBER") != null ? "numeric" :
                    matcher.group("OPERATOR") != null ? "parameter" :
                    matcher.group("IDENTIFIER") != null ? "type" :
                    matcher.group("NOTE") != null ? "parameter" :
                    matcher.group("CHORD") != null ? "function" :
                    matcher.group("COMMENT") != null ? "comment" :
                    "";
            spansBuilder.add([], matcher.start() - lastKwEnd);
            if (!styleClass.isEmpty())
                spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            else
                spansBuilder.add([], matcher.end() - matcher.start());
            lastKwEnd = matcher.end();            
        }
        spansBuilder.add([], text.length() - lastKwEnd);
        return spansBuilder.create();
    }

    void comment(CodeArea codeArea) {
        def selection = codeArea.getCaretSelectionBind();
        int startVisibleParIdx = selection.getStartParagraphIndex()
        int endVisibleParIdx = startVisibleParIdx + selection.getParagraphSpan()

        if ( endVisibleParIdx-startVisibleParIdx > 1 ) {
            for (int p = startVisibleParIdx; p < endVisibleParIdx; p++) {
                int startPos = codeArea.getAbsolutePosition( p , 0 );
                int endPos = codeArea.getAbsolutePosition( p, codeArea.getParagraphLength( p ) );
                String text = codeArea.getText(p)
                if (text.trim().startsWith(";"))
                    codeArea.replaceText(startPos, endPos, text.replaceAll(/;[\s]?/, ""));
                else
                    codeArea.replaceText(startPos, endPos, "; " + text);
                String newText = codeArea.getText(p)
                codeArea.setStyleSpans(startPos, computeHighlighting(newText));
            }
        }
    }

}