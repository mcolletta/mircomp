/*
 * Copyright (C) 2016-2025 Mirco Colletta
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

package io.github.mcolletta.mirdaw

import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext

import javafx.scene.paint.Color

import javafx.scene.shape.Rectangle

import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.scene.text.Text
import javafx.scene.text.TextAlignment

import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent
import javafx.scene.Cursor

import javafx.scene.effect.*

import javafx.event.EventHandler
import javafx.scene.input.KeyEvent
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCombination
import javafx.scene.input.KeyCodeCombination

import javafx.application.Platform

import javafx.collections.ListChangeListener
import javafx.collections.FXCollections

import javafx.geometry.Point2D
import javafx.geometry.VPos

import javafx.beans.value.ObservableValue
import javafx.beans.value.ChangeListener


class PianoKeyboard extends ResizableRegion {

    MidiView midi

    Map<Integer, String> pitchNameMap = [:]

    PianoKeyboard(MidiView midi) {
    	
        this.midi = midi

        // midi.mode = Mode.PANNING

        // build name map
        for(int pitch=0; pitch < 127; pitch++) {
            pitchNameMap[pitch] = getPitchName(pitch)
        }

        repaint()
    }

     String getPitchName(int pitch) {
        List<String> PITCH_NAMES_LIST = ["C", "C#", "D", "Eb", "E", "F", "F#", "G", "G#", "A", "Bb", "B"]
        int octave = (int)(pitch / 12)
        String name = PITCH_NAMES_LIST[pitch % 12]
        return name + octave
    }

    // Region

    @Override
    protected void reset() {
		g.clearRect(0, 0, getWidth(), getHeight())
	}

    @Override
    protected void repaint() {
        //println "repaint at " + midi.getVerticalOffset()
        reset()

        double w = getWidth()
        double h = getHeight()

        // Font
        int fontScale = (int)Math.max(1.0d, Math.floor(midi.getCurrentScaleY()))
        int fontSize = Math.max(8, fontScale)

        double note_height = Math.max(1, midi.scaleY(1) - 1)        

        int up = midi.getVerticalOffset() 
        double down = up + h
        
        for(int pitch=0; pitch < 127; pitch++) {
            double y = midi.toY(pitch)
            double endY =  y+note_height
            //if (y < down && endY > up) {
                Boolean isBlack = ((int)(pitch % 12)) in [1, 3, 6, 8, 10]
                String pitchStr = pitchNameMap[pitch] // "C5"
                // g.setTextAlign(TextAlignment.CENTER)
                // g.setTextBaseline(VPos.CENTER)
                if (isBlack) {
                    g.setFill(Color.BLACK)
                    g.fillRect(0, y, w, note_height)
                    g.setStroke(Color.WHITE)
                    g.strokeText(pitchStr, 5, y+note_height)
                } else {
                    g.setFill(Color.WHITE)
                    g.fillRect(0, y, w, note_height)
                    g.setStroke(Color.BLACK)
                    g.strokeText(pitchStr, 5, y+note_height)
                }               
            //}
        }        

        repaintLayer()
    }

    void repaintLayer() {
        gl.clearRect(0, 0, getWidth(), getHeight())
    }

}