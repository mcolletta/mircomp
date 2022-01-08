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

import java.util.regex.Pattern;
import javafx.collections.transformation.FilteredList;
import org.fxmisc.richtext.CodeArea;

import groovy.transform.EqualsAndHashCode


public interface SyntaxHighlighter {

	public FilteredList<Snippet> getSnippets();

	public void updateSnippets(String filter);

	public void highlight(CodeArea codeArea, int position, String inserted, String removed);

	public void comment(CodeArea codeArea);

}


@EqualsAndHashCode
public class Snippet {
	String id;
	String snippet;
	String suggestion;

	Snippet(String id, String snippet, String suggestion) {
		this.id = id;
		this.snippet = snippet;
		this.suggestion = suggestion;
	}
}
