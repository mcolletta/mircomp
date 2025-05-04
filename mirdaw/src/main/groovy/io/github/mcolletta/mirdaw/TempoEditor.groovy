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

import java.util.Map
import javafx.collections.ObservableMap

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import javafx.event.EventHandler
import javafx.scene.input.KeyEvent
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCombination
import javafx.scene.input.KeyCodeCombination

import javafx.geometry.Point2D;

import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;

import javafx.scene.Cursor;

import javafx.beans.value.ObservableValue
import javafx.beans.value.ChangeListener


class TempoEditor extends ResizableRegion {

    final int MAX_BPM = 300

    int currentBPM
    int startBPM

    double clickX
    double clickY
    double dragClickX
    double dragClickY
    double endCurveX
    double endCurveY

    //Boolean isDragging

    Rectangle selection

    MidiView midi

    // Cursor cursor

    TempoEditMode tempoMode = TempoEditMode.EDITED

    TempoEditor(MidiView midi) {
        this.midi = midi
        currentBPM = startBPM = (int) midi.getSequencerBPM()
		// repaint()
    }

    // void setCursor(Cursor c) {
    //     this.cursor = c 
    //     repaint()
    // }

    void editTempo(long x, int y) {        
        ObservableMap<Long, MidiTempo> data = midi.tempi
        int track = 0
        data[x] = midi.createMidiTempo(track, x, y)
    }

    void select() {
        selection = new Rectangle( Math.min(clickX,dragClickX), Math.min(clickY,dragClickY),
                                   Math.abs(dragClickX - clickX), Math.abs(dragClickY - clickY) )
    }

    @Override
    protected void delete() {
        if (selection != null) {
            long startX = midi.fromX(selection.getX())
            long endX = midi.fromX(selection.getX() + selection.getWidth())
            ObservableMap<Long, MidiTempo> data = midi.tempi
            Set<Long> keys = data.keySet()
            midi.startEdit()
            keys.each { key ->
                if (key >= startX && key <= endX)
                    data.remove(key)
            }
            midi.stopEdit()
            selection = null
            repaint()
        }
    }

    @Override
    protected void mouseClicked(MouseEvent event) {
        if (midi.mode == Mode.SET_PLAYBACK_POSITION) {
            midi.setPlaybackPosition(midi.fromX(event.getX()))
            midi.sequencer.setTickPosition(midi.getPlaybackPosition())
            repaint()
        }
    }

    @Override
    protected void mousePressed(MouseEvent event) {
        clickX = event.getX()
        clickY = event.getY()
    }

    @Override
    protected void mouseDragged(MouseEvent event) {
        dragClickX = event.getX()
        dragClickY = event.getY()  
        isDragging = true  
        repaint()    
    }

    @Override
    protected void mouseReleased(MouseEvent event) {
        //isDragging = false
        double h = getHeight()
        long startX = midi.fromX(clickX)
        long endX = midi.fromX(dragClickX)
        int startY = (int)( MAX_BPM * (h - clickY) / h )
        int endY = (int)( MAX_BPM * (h - event.getY()) / h )

        if (midi.mode == Mode.SELECT) {
            select()
            repaint()
        } else if (midi.mode == Mode.EDIT) {
            midi.startEdit()
            if (tempoMode == TempoEditMode.EDITED)
                editTempo(startX, currentBPM)
            else // TempoEditMode.AXIS
                editTempo(startX, startY)
            midi.stopEdit()
            repaint()
        }
    }

    void playbackAtTick(long tick) {
        midi.setPlaybackPosition(tick)
        long right = (long)( midi.getHorizontalOffset() + getWidth() * midi.getResolution() / midi.getCurrentScaleX() )
        if (midi.getPlaybackPosition() < midi.getHorizontalOffset() || midi.getPlaybackPosition() > right) {
            midi.setHorizontalOffset(midi.getPlaybackPosition())
            repaint()
        }
    }

    // Region

    @Override
    protected void undo() {
        midi.undo()
    }

    @Override
    protected void redo() {
        midi.redo()
    }

    @Override
    protected void reset() {
		// g.clearRect(0, 0, getWidth(), getHeight())
        // monokai bg
        g.setFill(Color.web("272822"))
        g.fillRect(0, 0, getWidth(), getHeight())
	}

    @Override
    protected void repaint() {
        //setCursor(cursor)
        reset()

        int resolution = midi.getResolution()
        long left = midi.getHorizontalOffset()
        double w = getWidth()
        double h = getHeight()
        long right = (long)( left + w * midi.getResolution() / midi.getCurrentScaleX() )

        //measures
        g.setStroke(Color.color(0.4d, 0.4d, 0.4d))
        long measureLength = resolution * 4 // in PPQ resolution is the tick in a quarter

        int msrIdx = 0
        long tk = 0
        while (tk < midi.sequence.tickLength) {
            tk += resolution
            Boolean isMeasure = (tk % measureLength) == 0
            if (isMeasure)
                    msrIdx += 1
            if (tk > left && tk < right) {
                double tkToPx = midi.toX(tk)
                if (isMeasure) {
                    g.setStroke(Color.color(0.4d, 0.4d, 0.4d))
                } else {
                    g.setStroke(Color.color(0.55d, 0.55d, 0.55d))
                }
                g.strokeLine(tkToPx, 0, tkToPx, getHeight())
            }
        }

        double label_height = Math.max(1, midi.scaleY(1) - 1)
        int leftX = (int) midi.toX(left)

        g.setFill(Color.BLACK)
        if (isDragging) {
            g.strokeLine(clickX, clickY, dragClickX, dragClickY)
        }

        long startX = -1
        long endX = -1
        if (selection != null) {
            startX = midi.fromX(selection.getX())
            endX = midi.fromX(selection.getX() + selection.getWidth())
        }

        ObservableMap<Long, MidiTempo> data = midi.tempi

        List<Long> keys = data.keySet() as List<Long>
        int lastIndex = keys.size() - 1
        int idx = Math.abs(midi.getStartTempoIndex(left, data) + 1)
        if (idx > 0)
            idx -= 1
        for(int i=idx; i<keys.size() && (keys[i] == null || keys[i] <= right); i++) {          
            long key = keys[i]
            MidiTempo tempo = data[keys[i]]
            int val = tempo.getBPM()

            double rectHeight = ((val * -h) / MAX_BPM) + h
            double screenX = midi.toX(key)
            double duration = 0
            double endLineX = 0
            if (i != lastIndex) {
                long nextKey = keys[i+1]
                duration = midi.toX(nextKey)-midi.toX(key)
                endLineX = screenX + duration
            } else {
                duration = getWidth()-screenX
                endLineX = getWidth()
            }
            g.setStroke(Color.WHITE)
            g.strokeLine(screenX, rectHeight, endLineX, rectHeight)
            // Text
            g.setStroke(Color.color(0.4d, 0.4d, 0.4d))
            String label = val.toString()
            double y = rectHeight - 1
            double x = Math.max(screenX, leftX)
            g.strokeText(label, x+5, y-label_height)

            if (i == 0 && key > midi.getHorizontalOffset()) {
                int default_val = startBPM
                if (default_val > 0) {
                    double default_tempo_duration = Math.min(screenX, getWidth())                
                    label = default_val.toString()
                    rectHeight = ((default_val * -h) / MAX_BPM) + h
                    // Line
                    g.setStroke(Color.WHITE)
                    g.strokeLine(leftX, rectHeight, default_tempo_duration, rectHeight)
                    // Text
                    g.setStroke(Color.color(0.4d, 0.4d, 0.4d))
                    y = rectHeight - 1
                    g.strokeText(label, leftX+5, y-label_height)
                }
            }
        }

        if (keys.size() == 0) {
            int default_val = startBPM
            if (default_val > 0) {              
                String label = default_val.toString()
                double rectHeight = ((default_val * -h) / MAX_BPM) + h
                // Line
                g.setStroke(Color.WHITE)
                g.strokeLine(leftX, rectHeight, getWidth(), rectHeight)
                // Text
                g.setStroke(Color.color(0.4d, 0.4d, 0.4d))
                double y = rectHeight - 1
                g.strokeText(label, leftX+5, y-label_height)
            }
        }

        repaintLayer()
    }

    @Override
    protected void repaintLayer() {
        gl.clearRect(0, 0, getWidth(), getHeight())
        double pbPos = midi.toX(midi.getPlaybackPosition())
        if (pbPos > 0) {
            gl.setStroke(Color.BLUE)
            gl.setLineWidth(2.0d)
            gl.strokeLine(pbPos, 0, pbPos, getHeight())
            gl.setLineWidth(1.0d)
        }
    }

}
