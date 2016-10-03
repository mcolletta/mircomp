/*
 * Copyright (C) 2016 Mirco Colletta
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
 
package io.github.mcolletta.mirtext

import groovy.transform.Canonical

@Canonical
class Mode {

	String name
	String path
    List<String> extensions

    String toString() {
        return name
    }

    static final Mode ABC = new Mode("ABC", "ace/mode/abc", ["abc"])
    static final Mode Clojure = new Mode("Clojure", "ace/mode/clojure", ["clj", "cljs"])
    static final Mode CSS = new Mode("CSS", "ace/mode/css", ["css"])
    static final Mode Dot = new Mode("Dot", "ace/mode/dot", ["dot"])
    static final Mode Groovy = new Mode("Groovy", "ace/mode/groovy", ["groovy"])
    static final Mode Java = new Mode("Java", "ace/mode/java", ["java"])
    static final Mode JSON = new Mode("JSON", "ace/mode/json", ["json"])
    static final Mode LaTeX = new Mode("LaTeX", "ace/mode/latex", ["latex"])
    static final Mode MirChord = new Mode("MirChord", "ace/mode/mirchord", ["mirchord"])
    static final Mode Tex = new Mode("Tex", "ace/mode/tex", ["tex"])
    static final Mode Text = new Mode("Text", "ace/mode/text", ["txt"])
    static final Mode XML = new Mode("XML", "ace/mode/xml", ["xml", "mxml"])

	
    static final Mode[] AVAILABLE_MODES = [ Groovy, MirChord, Text, XML ]

}