ace.define("ace/mode/mirchord_highlight_rules",["require","exports","module","ace/lib/oop","ace/mode/text_highlight_rules"], function(require, exports, module) {
"use strict";

var oop = require("../lib/oop");
var TextHighlightRules = require("./text_highlight_rules").TextHighlightRules;



var MirchordHighlightRules = function() {

    var builtinFunctions = (
        'key mode clef time tempo instr instrument tp tuplet define call relative unpitched ' +
        'keySignature copyTimes callSymbol ' +
        'info title composer poet chordsMode lyrics label ' +
        'stemUp stemDown stemAuto ' +
        'transpose transposeDiatonic invert invertDiatonic augment diminuition retrograde chain ' +        
        'zero? list zipmap'
    );

    var keywords = ('throw try var ' +
        'def do fn if let loop new quote recur set!'
    );

    var buildinConstants = ("true false nil");

    var keywordMapper = this.createKeywordMapper({
        "keyword": keywords,
        "constant.language": buildinConstants,
        "support.function": builtinFunctions
    }, "identifier", false, " ");

    this.$rules = {
        "start" : [
            {
                token : "constant.language", 
                regex : /[A-G]{1}[\#\&]*(M|maj|m|min|mM|minMaj|\+|aug|°|dim|sus){0,1}[0-9]{0,2}[\(]{0,1}(add|sub){0,1}[\#\&]*[0-9]{0,2}[\)]{0,1}/
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
            }
        ],
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
};

oop.inherits(MirchordHighlightRules, TextHighlightRules);

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

    this.$id = "ace/mode/mirchord";
}).call(Mode.prototype);

exports.Mode = Mode;
});
