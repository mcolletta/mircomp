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

import groovy.transform.Canonical

@Canonical
class Mode {

	String name
    List<String> extensions

    String toString() {
        return name
    }

    SyntaxHighlighter getHighlighter() {
        return null;
    }

    static final Mode Groovy = new GroovyMode("Groovy", ["groovy"])
    static final Mode MirChord = new MirChordMode("MirChord", ["mirchord"])

	
    static final Mode[] AVAILABLE_MODES = [ Groovy, MirChord ]

}

class GroovyMode extends Mode {

    GroovyMode(String name, List<String> extensions) {
        this.name = name
        this.extensions = extensions
    }

    @Override
    SyntaxHighlighter getHighlighter() {
        return new GroovySyntaxHighlighter();
    }
}

class MirChordMode extends Mode {

    MirChordMode(String name, List<String> extensions) {
        this.name = name
        this.extensions = extensions
    }

    @Override
    SyntaxHighlighter getHighlighter() {
        return new MirChordSyntaxHighlighter();
    }
}