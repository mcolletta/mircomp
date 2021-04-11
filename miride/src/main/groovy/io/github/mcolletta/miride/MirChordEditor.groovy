/*
 * Copyright (C) 2016-2021 Mirco Colletta
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

package io.github.mcolletta.miride

import java.nio.file.Path
import java.nio.file.Paths

import javax.sound.midi.Synthesizer

import javafx.geometry.Orientation

import javafx.scene.layout.VBox
import javafx.scene.control.SplitPane

import io.github.mcolletta.mirutils.TabContent

import io.github.mcolletta.mirtext.Mode
import io.github.mcolletta.mirtext.TextEditor
import io.github.mcolletta.mirscore.ScoreViewer

import io.github.mcolletta.mirmidi.MidiEditor


class MirChordEditor extends VBox implements TabContent {

	SplitPane splitPane
    TextEditor editor
    ScoreViewer viewer

    Path filePath

    MirChordEditor(Path path=null, Synthesizer synthesizer=null) {
    	splitPane = new SplitPane()
    	editor = new TextEditor(path)
        editor.setMode(Mode.MirChord)
        viewer = new ScoreViewer(synthesizer)
        splitPane.setOrientation(Orientation.VERTICAL)
        splitPane.getItems().add(editor)
        splitPane.getItems().add(viewer)
        this.getChildren().add(splitPane)
    }

    String getValue() {
        return editor.getValue()
    }

    Mode getMode() {
        return editor.getMode()
    }

    boolean isClean() {
        return editor.isClean()
    }

    Path getFilePath() {
        return editor.getFilePath()
    }

    String getTabType() { return "MirChordEditor"; }
}
