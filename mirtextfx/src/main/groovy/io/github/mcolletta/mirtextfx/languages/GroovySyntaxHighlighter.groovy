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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

// import java.util.concurrent.ConcurrentSkipListSet;
// import java.util.Comparator;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import static org.fxmisc.richtext.model.TwoDimensional.Bias.*;

import org.fxmisc.richtext.model.Paragraph;


public class GroovySyntaxHighlighter implements SyntaxHighlighter {

    String name = "Groovy"
    
    String toString() {
        return name
    }

    private FilteredList<Snippet> snippetList;
    private ObservableList<Snippet> hintList;

    public GroovySyntaxHighlighter() {
        hintList = FXCollections.observableArrayList();
        snippetList = new FilteredList<Snippet>(hintList);
        snippetList.setPredicate(t -> false);
        initRegExps();
    }

    private static final String[] KEYWORDS = new String[] {
            "abstract", "assert", "break",
            "case", "catch", "char", "class", 
            "continue", "default", "do", "else",
            "enum", "extends", "final", "finally",
            "for", "goto", "if", "implements", "import",
            "instanceof", "interface", "native",
            "new", "package", "private", "protected", "public",
            "return", "static", "strictfp", "super",
            "switch", "synchronized", "this", "throw", "throws",
            "transient", "try", "void", "volatile", "while"
    };

    private static final String[] TYPES = new String[] {
            "def", "var", "boolean", "byte", "const",
            "double", "float", "int", "long", "short", 
            "Integer", "String", "Float", "Double",
            "Long", "Short", "Pattern", "Set", "Map",
            "SortedSet", "SortedMap"
    };

    
    private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
    private static final String TYPES_PATTERN = "\\b(" + String.join("|", TYPES) + ")\\b";

    List<String> linestate = []

    LinkedHashMap<String, LinkedHashMap<String, LinkedHashMap<String,String>>> states =  [
            "start": [
                // multiline
                "opencomment": [
                    "regex": "/\\*",
                    "styleClass": "comment"
                ],
                "qqstring": [
                    "regex": '"""',
                    "styleClass": "string"
                ],
                "qstring": [
                    "regex": "'''",
                    "styleClass": "string"
                ],
                // single line
                "linecomment": [
                    "regex": "//[^\n]*",
                    "styleClass": "comment"
                ],
                "lineqqstring": [
                    "regex": /"(?:[^"\\]|\\.)*"/,
                    "styleClass": "string"
                ],
                "lineqstring": [
                    "regex": /'(?:[^'\\]|\\.)*'/,
                    "styleClass": "string"
                ],
                "keyword": [
                    "regex": "\\b(" + String.join("|", KEYWORDS) + ")\\b",
                    "styleClass": "keyword"
                ],
                "type": [
                    "regex": "\\b(" + String.join("|", TYPES) + ")\\b",
                    "styleClass": "type"
                ],
                "numeric": [
                    "regex": /\b[+-]?([0-9]*[.])?[0-9]+\b/,
                    "styleClass": "numeric"
                ],
                "boolean": [
                    "regex": "(?:true|false)\\b",
                    "styleClass": "numeric"
                ],
                "constants": [
                    "regex": "null",
                    "styleClass": "numeric"
                ]
            ],
            "comment": [
                "closecomment": [
                    "regex": "\\*/",
                    "styleClass": "comment"
                ]
            ],
            "qqstring": [
               "qqstring": [
                    "regex" : '"""',
                    "styleClass": "string"
                ]
            ],
            "qstring": [
               "qstring": [
                    "regex" : "'''",
                    "styleClass": "string"
                ]
            ]
        ]
    LinkedHashMap<String, LinkedHashMap<String,String>> transitions = [:] 
    LinkedHashMap<String, Pattern> patterns = [:]
    LinkedHashMap<String, String> stateStyleDefaults = ["start": "",
                                                        "comment": "comment",
                                                        "qqstring": "string",
                                                        "qstring": "string"]

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

    private initRegExps() {
        for( Map.Entry<String, Map<String,Map<String,String>>> entry : states.entrySet() ) {
            var stateName = entry.getKey();
            var tokens = entry.getValue();
            String state_regex = "";
            for(Map.Entry<String,Map<String,String>> token: tokens) {
                String tokenName = token.getKey();
                var tokenInfo = token.getValue();
                if (state_regex.size() > 0)
                    state_regex += "|";
                state_regex += "(?<${tokenName}>${tokenInfo.regex})";
            }
            Pattern STATE_PATTERN = Pattern.compile(state_regex);
            patterns[stateName] = STATE_PATTERN;
        }

        transitions["start"] = [:]
        transitions["start"]["opencomment"] = "comment";
        transitions["start"]["qqstring"] = "qqstring";
        transitions["start"]["qstring"] = "qstring";
        transitions["comment"] = [:]
        transitions["comment"]["closecomment"] = "start";
        transitions["qqstring"] = [:]
        transitions["qqstring"]["qqstring"] = "start";
        transitions["qstring"] = [:]
        transitions["qstring"]["qstring"] = "start";
    }


    public void highlight(CodeArea codeArea, int position, String inserted, String removed) {
        int currentParagraph

        // TODO: need to cache the state of each par to start the loop only from current paragraph
        var paragraphs = codeArea.getParagraphs();

        // currentParagraph = codeArea.getCurrentParagraph();
        // if (inserted!= null && inserted.contains("\n"))
        //     currentParagraph = codeArea.offsetToPosition(position, Forward).getMajor();

        // XXX Full highlight semantic
        if (position < 0) {
            currentParagraph = 0
            position = 0
        }
        currentParagraph = codeArea.offsetToPosition(position, Forward).getMajor();
        
        int startPos = codeArea.getAbsolutePosition(currentParagraph , 0);
        String state = linestate?[currentParagraph]
        //println "LINE STATE: " + state
        if (state == null || state.isEmpty())
            state = "start";

        //String state = "start";        
        //int startPos = 0
        int num_paragraphs = paragraphs.size()
        //for (Paragraph par : paragraphs) {
        for (int i = currentParagraph; i < num_paragraphs; i++) {
            linestate[i] = state  // we save the initial state of the paragraph
            Paragraph par = paragraphs[i]

            StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
            String styleDefault = stateStyleDefaults[state];
            String line = par.getText();
            int lastKwEnd = 0;
            Matcher matcher = patterns[state].matcher(line);
            while(matcher.find()) {
                String styleClass = "";
                String curToken = "";
                var tokens = states[state]
                for(Map.Entry<String,Map<String,String>> token: tokens) {
                    String tokenName = token.getKey();
                    var tokenInfo = token.getValue();
                    if (matcher.group(tokenName) != null) {
                        curToken = tokenName
                        styleClass = tokenInfo['styleClass']
                    }
                }
                //println "curToken: " + curToken
                spansBuilder.add(Collections.singleton(styleDefault), matcher.start() - lastKwEnd);

                if (!styleClass.isEmpty())
                    spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
                else
                    spansBuilder.add(Collections.singleton(styleDefault), matcher.end() - matcher.start());
                lastKwEnd = matcher.end();

                if (transitions[state].containsKey(curToken)) {
                    state = transitions[state][curToken]
                    Pattern pat = patterns[state]
                    matcher.usePattern(pat)
                    matcher.region(lastKwEnd, matcher.regionEnd())
                    styleDefault = stateStyleDefaults[state]
                    //println "STATE CHANGED with PATTERN: " + pat
                }
            }

            spansBuilder.add(Collections.singleton(styleDefault), line.length() - lastKwEnd);
            codeArea.setStyleSpans(startPos, spansBuilder.create());
            startPos += line.length() + 1;            
        }
        int num_state = linestate.size()
        if(num_state > num_paragraphs) {
            linestate = linestate[0..num_paragraphs-1]
        }
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
                if (text.trim().startsWith("//"))
                    codeArea.replaceText(startPos, endPos, text.replaceAll("//[\\s]?", ""));
                else
                    codeArea.replaceText(startPos, endPos, "// " + text);
            }
            // XXX TODO
            // full highlight
            highlight(codeArea, codeArea.getAbsolutePosition( startVisibleParIdx, 0), null, null) 
        }
    }

}