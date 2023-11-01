/*
Author: Mirco Colletta
clojure mode used as a starting point
This file has the same licence of the other ACE modes - see https://github.com/ajaxorg/ace/blob/master/LICENSE
*/

ace.define("ace/mode/doc_comment_highlight_rules",["require","exports","module","ace/lib/oop","ace/mode/text_highlight_rules"], function(require, exports, module) {
"use strict";

var oop = require("../lib/oop");
var TextHighlightRules = require("./text_highlight_rules").TextHighlightRules;

// var DocCommentHighlightRules = function() {
//     this.$rules = {
//         "start" : [ {
//             token : "comment.doc.tag",
//             regex : "@[\\w\\d_]+" // TODO: fix email addresses
//         }, 
//         DocCommentHighlightRules.getTagRule(),
//         {
//             defaultToken : "comment.doc",
//             caseInsensitive: true
//         }]
//     };
// };

// oop.inherits(DocCommentHighlightRules, TextHighlightRules);

// DocCommentHighlightRules.getTagRule = function(start) {
//     return {
//         token : "comment.doc.tag.storage.type",
//         regex : "\\b(?:TODO|FIXME|XXX|HACK)\\b"
//     };
// }

// DocCommentHighlightRules.getStartRule = function(start) {
//     return {
//         token : "comment.doc", // doc comment
//         regex : "\\/\\*(?=\\*)",
//         next  : start
//     };
// };

// DocCommentHighlightRules.getEndRule = function (start) {
//     return {
//         token : "comment.doc", // closing comment
//         regex : "\\*\\/",
//         next  : start
//     };
// };


// exports.DocCommentHighlightRules = DocCommentHighlightRules;

});

ace.define("ace/mode/mirchord_highlight_rules",["require","exports","module","ace/lib/oop","ace/mode/text_highlight_rules"], function(require, exports, module) {
"use strict";

var oop = require("../lib/oop");
// var DocCommentHighlightRules = require("./doc_comment_highlight_rules").DocCommentHighlightRules;
var TextHighlightRules = require("./text_highlight_rules").TextHighlightRules;



var MirchordHighlightRules = function() {

    var builtinFunctions = (
        'key mode clef time tempo instr instrument tp tuplet define call relative unpitched ' +
        'keySignature copyTimes callSymbol ' +
        'info title composer poet chordsMode lyrics label name ' +
        'stemUp stemDown stemAuto ' +
        'transpose transposeDiatonic invert invertDiatonic augment diminuition retrograde zip chain ' +        
        'zero? list zipmap'
    );

    var keywords = ('throw try var ' +
        'def fn if let loop new quote recur set!'
    );

    var buildinConstants = ("true false nil");

    var keywordMapper = this.createKeywordMapper({
        "keyword": keywords,
        "constant.language": buildinConstants,
        "support.function": builtinFunctions
    }, "identifier", false, " ");

    this.$rules = {
        "start" : [
            // {
            //     token : "comment",
            //     regex : "\\/\\/.*$"
            // },
            // DocCommentHighlightRules.getStartRule("doc-start"),            
            // {
            //     token : "comment", // multi line comment
            //     regex : "\\/\\*",
            //     next : "comment"
            // }, 
            // {
            //     token : "support.function", 
            //     regex : /\b[A-G]{1}[\#|\&]*(M|maj|m|min|mM|minMaj|\+|aug|°|dim|sus){0,1}[0-9]{0,2}[\(]{0,1}(add|sub){0,1}[\#\&]*[0-9]{0,2}[\)]{0,1}/
            // },
            // {
            //     token : "variable.parameter",
            //     //regex : /\b[a-grxo]{1}\b[\#|\&]*[1-9]?/
            // },

            {
                token : "support.function", 
                regex : /\b(I|II|III|IV|V|VI|VII|A|B|C|D|E|F|G){1}[\#|\&]*(M|maj|m|min|mM|minMaj|\+|aug|°|dim|sus){0,1}[0-9]{0,2}[\(]{0,1}(add|sub){0,1}[\#\&]*[0-9]{0,2}[\)]{0,1}(?![\dA-Za-z\#\&,'])/
            },
            {
                token : "variable.parameter",
                regex : /\b(do|re|mi|fa|so|sol|la|si|ti|a|b|c|d|e|f|g|r|x|o){1}[\#|\&]*[,|']*[1-9]{0,2}(?![\dA-Za-z\#\&,'])/
            },

            {
                token : "variable.parameter",
                regex : '[\\#|\\&]'
            },
            {
                token : "constant.language",
                regex : '[!|\\$|%|\\*|\\-\\-|\\-|\\+\\+|\\+||=|!=|<=|>=|<>|<|>|^]'
            },
            {
                token : "keyword",
                regex : "[\\{|\\}]"
            },
            // ---------------------------------
            {
                token : "comment",
                regex : ";.*$"
            }, {
                token : "keyword", //parens
                regex : "[\\(|\\)]"
            }, {
                token : "keyword", //vectors
                regex : "[\\[|\\]]"
            }, {
                token : keywordMapper,
                regex : "[a-zA-Z_$][a-zA-Z0-9_$\\-]*\\b"
            }, {
                token : "string", // single line
                regex : '"',
                next: "string"
            }, {
                token : "constant.language", // symbol
                regex : /:[^()\[\]{}'"\^%`,;\s]+/
            },
            {
                token : "constant.numeric", // hex
                regex : /0(?:[xX][0-9a-fA-F]+|[bB][01]+)\b/
            }, {
                token : "constant.numeric", // float
                regex : /[+-]?\d[\d_]*(?:(?:\.\d*)?(?:[eE][+-]?\d+)?)?\b/
            },
            {
                token : "identifier",
                regex : /:[^()\[\]{}'"\^%`,;\s]+/
            },
        ],
        // "comment" : [
        //     {
        //         token : "comment", // closing comment
        //         regex : ".*?\\*\\/",
        //         next : "start"
        //     }, {
        //         token : "comment", // comment spanning whole line
        //         regex : ".+"
        //     }
        // ],
        "string" : [
            {
                token : "constant.language.escape",                
                regex : "\\\\.|\\\\$"
            }, {
                token : "string",                
                regex : '[^"\\\\]+'
            }, {
                token : "string",
                regex : '"',
                next : "start"
            }
        ]
    };

    // this.embedRules(DocCommentHighlightRules, "doc-",
    //     [ DocCommentHighlightRules.getEndRule("start") ]);
};

oop.inherits(MirchordHighlightRules, TextHighlightRules);

// function comments(next) {
//     return [
//         {
//             token : "comment", // multi line comment
//             regex : /\/\*/,
//             next: [
//                 DocCommentHighlightRules.getTagRule(),
//                 {token : "comment", regex : "\\*\\/", next : next || "pop"},
//                 {defaultToken : "comment", caseInsensitive: true}
//             ]
//         }, {
//             token : "comment",
//             regex : "\\/\\/",
//             next: [
//                 DocCommentHighlightRules.getTagRule(),
//                 {token : "comment", regex : "$|^", next : next || "pop"},
//                 {defaultToken : "comment", caseInsensitive: true}
//             ]
//         }
//     ];
// }

exports.MirchordHighlightRules = MirchordHighlightRules;
});

ace.define("ace/mode/matching_parens_outdent",["require","exports","module","ace/range"], function(require, exports, module) {
"use strict";

var Range = require("../range").Range;

var MatchingParensOutdent = function() {};

(function() {

    this.checkOutdent = function(line, input) {
        if (! /^\s+$/.test(line))
            return false;

        return /^\s*\)/.test(input);
    };

    this.autoOutdent = function(doc, row) {
        var line = doc.getLine(row);
        var match = line.match(/^(\s*\))/);

        if (!match) return 0;

        var column = match[1].length;
        var openBracePos = doc.findMatchingBracket({row: row, column: column});

        if (!openBracePos || openBracePos.row == row) return 0;

        var indent = this.$getIndent(doc.getLine(openBracePos.row));
        doc.replace(new Range(row, 0, row, column-1), indent);
    };

    this.$getIndent = function(line) {
        var match = line.match(/^(\s+)/);
        if (match) {
            return match[1];
        }

        return "";
    };

}).call(MatchingParensOutdent.prototype);

exports.MatchingParensOutdent = MatchingParensOutdent;
});

var functionMap = {
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
            "Key signature \"F&#9839; minor\" with 3 sharps"
        ],
    "key E major": [
            "(key \"E\" \"maj\")",
            "Key signature \"E major\" with 4 sharps"
        ],
    "key C# minor": [
            "(key \"C#\" \"min\")",
            "Key signature \"C&#9839; minor\" with 4 sharps"
        ],
    "key B major": [
            "(key \"B\" \"maj\")",
            "Key signature \"B major\" with 5 sharps"
        ],
    "key G# minor": [
            "(key \"G#\" \"min\")",
            "Key signature \"G&#9839; minor\" with 5 sharps"
        ],
    "key F# major": [
            "(key \"F#\" \"maj\")",
            "Key signature \"F&#9839; major\" with 6 sharps"
        ],
    "key D# minor": [
            "(key \"D#\" \"min\")",
            "Key signature \"D&#9839; minor\" with 6 sharps"
        ],
    "key C# major": [
            "(key \"C#\" \"maj\")",
            "Key signature \"C&#9839; major\" with 7 sharps"
        ],
    "key A# minor": [
            "(key \"A#\" \"min\")",
            "Key signature \"A&#9839; minor\" with 7 sharps"
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
            "Key signature \"B&#9837; major\" with 2 flats"
        ],
    "key G minor": [
            "(key \"G\" \"min\")",
            "Key signature \"G minor\" with 2 flats"
        ],
    "key Eb major": [
            "(key \"E&\" \"maj\")",
            "Key signature \"E&#9837; major\" with 3 flats"
        ],
    "key C minor": [
            "(key \"C\" \"min\")",
            "Key signature \"C minor\" with 3 flats"
        ],
    "key Ab major": [
            "(key \"A&\" \"maj\")",
            "Key signature \"A&#9837; major\" with 4 flats"
        ],
    "key F minor": [
            "(key \"F\" \"min\")",
            "Key signature \"F minor\" with 4 flats"
        ],
    "key Db major": [
            "(key \"D&\" \"maj\")",
            "Key signature \"D&#9837; major\" with 5 flats"
        ],
    "key Bb minor": [
            "(key \"B&\" \"min\")",
            "Key signature \"B&#9837; minor\" with 5 flats"
        ],
    "key Gb major": [
            "(key \"G&\" \"maj\")",
            "Key signature \"G&#9837; major\" with 6 flats"
        ],
    "key Eb minor": [
            "(key \"E&\" \"min\")",
            "Key signature \"E&#9837; minor\" with 6 flats"
        ],
    "key Cb major": [
            "(key \"C&\" \"maj\")",
            "Key signature \"C&#9837; major\" with 7 flats"
        ],
    "key Ab minor": [
            "(key \"A&\" \"min\")",
            "Key signature \"A&#9837; minor\" with 7 flats"
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
};

ace.define("ace/mode/mirchord",["require","exports","module","ace/lib/oop","ace/mode/text","ace/mode/mirchord_highlight_rules","ace/mode/matching_parens_outdent"], function(require, exports, module) {
"use strict";

var oop = require("../lib/oop");
var TextMode = require("./text").Mode;
var MirchordHighlightRules = require("./mirchord_highlight_rules").MirchordHighlightRules;
var MatchingParensOutdent = require("./matching_parens_outdent").MatchingParensOutdent;

var Mode = function() {
    this.HighlightRules = MirchordHighlightRules;
    this.$outdent = new MatchingParensOutdent();
};
oop.inherits(Mode, TextMode);

(function() {

    this.lineCommentStart = ";";
    this.blockComment = {start: "/*", end: "*/"};
    this.minorIndentFunctions = ["defn", "defn-", "defmacro", "def", "deftest", "testing"];

    this.$toIndent = function(str) {
        return str.split('').map(function(ch) {
            if (/\s/.exec(ch)) {
                return ch;
            } else {
                return ' ';
            }
        }).join('');
    };

    this.$calculateIndent = function(line, tab) {
        var baseIndent = this.$getIndent(line);
        var delta = 0;
        var isParen, ch;
        for (var i = line.length - 1; i >= 0; i--) {
            ch = line[i];
            if (ch === '(') {
                delta--;
                isParen = true;
            } else if (ch === '(' || ch === '[' || ch === '{') {
                delta--;
                isParen = false;
            } else if (ch === ')' || ch === ']' || ch === '}') {
                delta++;
            }
            if (delta < 0) {
                break;
            }
        }
        if (delta < 0 && isParen) {
            i += 1;
            var iBefore = i;
            var fn = '';
            while (true) {
                ch = line[i];
                if (ch === ' ' || ch === '\t') {
                    if(this.minorIndentFunctions.indexOf(fn) !== -1) {
                        return this.$toIndent(line.substring(0, iBefore - 1) + tab);
                    } else {
                        return this.$toIndent(line.substring(0, i + 1));
                    }
                } else if (ch === undefined) {
                    return this.$toIndent(line.substring(0, iBefore - 1) + tab);
                }
                fn += line[i];
                i++;
            }
        } else if(delta < 0 && !isParen) {
            return this.$toIndent(line.substring(0, i+1));
        } else if(delta > 0) {
            baseIndent = baseIndent.substring(0, baseIndent.length - tab.length);
            return baseIndent;
        } else {
            return baseIndent;
        }
    };

    this.getNextLineIndent = function(state, line, tab) {
        return this.$calculateIndent(line, tab);
    };

    this.checkOutdent = function(state, line, input) {
        return this.$outdent.checkOutdent(line, input);
    };

    this.autoOutdent = function(state, doc, row) {
        this.$outdent.autoOutdent(doc, row);
    };

    this.getCompletions = function(state, session, pos, prefix) {
        var token = session.getTokenAt(pos.row, pos.column);
        if (!token)
            return [];
        return this.getFunctionCompletions(state, session, pos, prefix);
    };

    this.getFunctionCompletions = function(state, session, pos, prefix) {
        var functions = Object.keys(functionMap);
        console.log(functions);
        return functions.map(function(func){
            return {
                caption: func,
                //snippet: func + '($0)',
                snippet: functionMap[func][0],
                meta: "notation function",
                score: Number.MAX_VALUE,
                docHTML: functionMap[func][1]
            };
        });
    };

    this.$id = "ace/mode/mirchord";
}).call(Mode.prototype);

exports.Mode = Mode;
});
