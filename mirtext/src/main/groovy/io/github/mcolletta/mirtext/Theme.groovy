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

package io.github.mcolletta.mirtext

import groovy.transform.Canonical

@Canonical
class Theme {

	String name
	String path

	String toString() {
		return name
	}

	static final Theme Chrome = new Theme("Chrome", "ace/theme/chrome")
    static final Theme Clouds = new Theme("Clouds", "ace/theme/clouds")
    static final Theme Crimson_Editor = new Theme("Crimson Editor", "ace/theme/crimson_editor")
    static final Theme Dawn = new Theme("Dawn", "ace/theme/dawn")
    static final Theme Dreamweaver = new Theme("Dreamweaver", "ace/theme/dreamweaver")
    static final Theme Eclipse = new Theme("Eclipse", "ace/theme/eclipse")
    static final Theme GitHub = new Theme("GitHub", "ace/theme/github")
    static final Theme IPlastic = new Theme("IPlastic", "ace/theme/iplastic")
    static final Theme Solarized_Light = new Theme("Solarized Light", "ace/theme/solarized_light")
    static final Theme TextMate = new Theme("TextMate", "ace/theme/textmate")
    static final Theme Tomorrow = new Theme("Tomorrow", "ace/theme/tomorrow")
    static final Theme XCode = new Theme("XCode", "ace/theme/xcode")
    static final Theme Kuroir = new Theme("Kuroir", "ace/theme/kuroir")
    static final Theme KatzenMilch = new Theme("KatzenMilch", "ace/theme/katzenmilch")
    static final Theme SQL_Server = new Theme("SQL Server", "ace/theme/sqlserver")
    static final Theme Ambiance = new Theme("Ambiance", "ace/theme/ambiance")
    static final Theme Chaos = new Theme("Chaos", "ace/theme/chaos")
    static final Theme Clouds_Midnight = new Theme("Clouds Midnight", "ace/theme/clouds_midnight")
    static final Theme Cobalt = new Theme("Cobalt", "ace/theme/cobalt")
    static final Theme idle_Fingers = new Theme("idle Fingers", "ace/theme/idle_fingers")
    static final Theme krTheme = new Theme("krTheme", "ace/theme/kr_theme")
    static final Theme Merbivore = new Theme("Merbivore", "ace/theme/merbivore")
    static final Theme Merbivore_Soft = new Theme("Merbivore Soft", "ace/theme/merbivore_soft")
    static final Theme Mono_Industrial = new Theme("Mono Industrial", "ace/theme/mono_industrial")
    static final Theme Monokai = new Theme("Monokai", "ace/theme/monokai")
    static final Theme Pastel_on_dark = new Theme("Pastel on dark", "ace/theme/pastel_on_dark")
    static final Theme Solarized_Dark = new Theme("Solarized Dark", "ace/theme/solarized_dark")
    static final Theme Terminal = new Theme("Terminal", "ace/theme/terminal")
    static final Theme Tomorrow_Night = new Theme("Tomorrow Night", "ace/theme/tomorrow_night")
    static final Theme Tomorrow_Night_Blue = new Theme("Tomorrow Night Blue", "ace/theme/tomorrow_night_blue")
    static final Theme Tomorrow_Night_Bright = new Theme("Tomorrow Night Bright", "ace/theme/tomorrow_night_bright")
    static final Theme Tomorrow_Night_80s = new Theme("Tomorrow Night 80s", "ace/theme/tomorrow_night_eighties")
    static final Theme Twilight = new Theme("Twilight", "ace/theme/twilight")
    static final Theme Vibrant_Ink = new Theme("Vibrant Ink", "ace/theme/vibrant_ink")

    static final Theme[] AVAILABLE_THEMES = [
        Ambiance, Chaos, Chrome, Clouds, Clouds_Midnight, Cobalt, Crimson_Editor, Dawn,
        Dreamweaver, Eclipse, GitHub, IPlastic, KatzenMilch, Kuroir, Merbivore, Merbivore_Soft,
        Mono_Industrial, Monokai, Pastel_on_dark, SQL_Server, Solarized_Dark, Solarized_Light, Terminal, TextMate,
        Tomorrow, Tomorrow_Night, Tomorrow_Night_80s, Tomorrow_Night_Blue, Tomorrow_Night_Bright, Twilight, Vibrant_Ink, XCode,
        idle_Fingers, krTheme
	]

}