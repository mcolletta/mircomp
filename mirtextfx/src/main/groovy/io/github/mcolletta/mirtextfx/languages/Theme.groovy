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
class Theme {

	String name
	String path

	String toString() {
		return name
	}

    static final Theme Monokai = new Theme("Monokai", "monokai")

    static final Theme Bright = new Theme("Bright", "bright")
    

    static final Theme[] AVAILABLE_THEMES = [
        Monokai, Bright
	]

}