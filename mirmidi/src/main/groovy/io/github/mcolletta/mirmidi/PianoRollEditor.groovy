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

package io.github.mcolletta.mirmidi

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

import javafx.scene.paint.Color;

import javafx.scene.shape.Rectangle;

import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.Cursor;

import javafx.scene.effect.*

import javafx.event.EventHandler
import javafx.scene.input.KeyEvent
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCombination
import javafx.scene.input.KeyCodeCombination

import javafx.application.Platform

import javafx.collections.ListChangeListener;
import javafx.collections.FXCollections;

import javafx.geometry.Point2D;

import javafx.beans.value.ObservableValue
import javafx.beans.value.ChangeListener

import groovy.transform.CompileStatic

@CompileStatic
class PianoRollEditor {

    double clickX
    double clickY
    double dragClickX
    double dragClickY

    Rectangle selection
    Point2D anchor
    Boolean isDragging

    List<MidiNote> selectedNotes
    Map<MidiNote,Rectangle> visibleRects

    float insertNoteType = 1

    Boolean resizingE = false
    Boolean resizingW = false
    long resize = 0
    Cursor cursor = Cursor.MOVE
    Cursor oldCursor

    Boolean moving = false
    long movedX
    int movedY

    long MARGIN_RIGHT = 500

    MidiView midi
    ResizableCanvas canvas
    GraphicsContext g
    GraphicsContext gl

    PianoRollMode mode

    Map<Integer, String> pitchNameMap = [:]

    void setCursor(Cursor c) {
        this.cursor = c 
        repaint()
    }

    PianoRollEditor(MidiView midi, ResizableCanvas canvas) {
    	this.canvas = canvas
    	this.g = canvas.getGraphicsContext2D()
        this.gl = canvas.getLayerGraphicsContext2D()

        this.midi = midi

        canvas.addEventHandler(MouseEvent.MOUSE_CLICKED,
		new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent e) {
				// if (e.getClickCount() >1) { println "clicked" }
				mouseClicked(e)
			}
		});

		canvas.addEventHandler(MouseEvent.MOUSE_PRESSED,
		new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent e) {
				mousePressed(e)
			}
		});

		canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED,
		new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent e) {
				mouseDragged(e)
			}
		});

		canvas.addEventHandler(MouseEvent.MOUSE_RELEASED,
		new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent e) {
				mouseReleased(e)
			}
		});

        canvas.setOnScroll(new EventHandler<ScrollEvent>() {
            @Override
            public void handle(ScrollEvent e) {
                mouseWheelMoved(e)
            }
        });

        final KeyCombination keyCtrZ = new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN)
        final KeyCombination keyCtrY = new KeyCodeCombination(KeyCode.Y, KeyCombination.SHORTCUT_DOWN)
        canvas.setOnKeyPressed(
        new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                if (keyEvent.getCode() == KeyCode.DELETE) {
                    delete()
                } else if (keyCtrZ.match(keyEvent)) {
                    midi.undo()
                    repaint()
                } else if (keyCtrY.match(keyEvent)) {
                    midi.redo()
                    repaint()
                }
            }
        });

        canvas.addEventFilter(MouseEvent.ANY, { canvas.requestFocus() })

        visibleRects = [:]
        selectedNotes = []

        mode = PianoRollMode.PANNING

        // build name map
        for(int pitch=0; pitch < 127; pitch++) {
            pitchNameMap[pitch] = getPitchName(pitch)
        }

        repaint()
    }

    // UTILS

    String getPitchName(int pitch) {
        List<String> PITCH_NAMES_LIST = ["C", "C#", "D", "Eb", "E", "F", "F#", "G", "G#", "A", "Bb", "B"]
        int octave = (int)(pitch / 12)
        String name = PITCH_NAMES_LIST[pitch % 12]
        return name + octave
    }

    // -----

    private void reset() {
		g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight())
	}

    protected void repaint() {
        canvas.setCursor(cursor)
        reset()

        long left = midi.getHorizontalOffset()
        double w = canvas.getWidth()
        long right = (long)( left + w * midi.getResolution() / midi.getCurrentScaleX() )

        // Font
        int fontScale = (int)Math.max(1.0, Math.floor(midi.getCurrentScaleY()))
        int fontSize = Math.max(8, fontScale)

        double note_height = Math.max(1, midi.scaleY(1) - 1)
        
        int screenX = (int)midi.toX(left)
        double h = canvas.getHeight()

        int up = midi.getVerticalOffset() 
        double down = up + h
        
        for(int pitch=0; pitch < 127; pitch++) {
            double y = midi.toY(pitch)
            double endY =  y+note_height
            //if (y < down && endY > up) {
                Boolean isBlack = ((int)(pitch % 12)) in [1, 3, 5, 7, 9]
                if (isBlack)
                    g.setFill(Color.LIGHTGRAY)
                else
                    g.setFill(Color.WHITE)                
                g.fillRect(screenX, y, w, note_height)
            //}
        }

        //measures
        g.setStroke(Color.color(0.4, 0.4, 0.4))
        g.setFont(new Font("Verdana", fontSize))
        long measureLength = midi.getResolution() * 4 // in PPQ midi.getResolution() is the tick in a quarter

        int msrIdx = 0
        long tk = 0
        while (tk < midi.getLength()) {
            tk += midi.getResolution()
            Boolean isMeasure = (tk % measureLength) == 0
            if (isMeasure)
                    msrIdx += 1
            if (tk > left && tk < right) {
                double tkToPx = midi.toX(tk)
                if (isMeasure) {
                    g.setStroke(Color.color(0.4, 0.4, 0.4))
                    g.strokeText(msrIdx.toString(), tkToPx+5, 10)
                } else {
                    g.setStroke(Color.color(0.55, 0.55, 0.55))
                }
                g.strokeLine(tkToPx, 0, tkToPx, canvas.getHeight())
            }
        }
        //---------

        g.setStroke(Color.color(0.4, 0.4, 0.4))
        for(int pitch=0; pitch < 127; pitch++) {
            double y = midi.toY(pitch)
            String pitchStr = pitchNameMap[pitch] // "C5"
            g.strokeText(pitchStr, screenX+5, y+note_height)
        }

        visibleRects = [:]

        // for (MidiNote note : sortedByEndNotes) {
        int i = Math.abs(midi.getStartNoteIndex(left) + 1)
        long longestDuration = midi.getLongestDuration()
        while (i < midi.sortedByEndNotes.size() && midi.sortedByEndNotes[i].getEnd() < (right + longestDuration)) {
            MidiNote note = midi.sortedByEndNotes[i]
            i++

            double y = midi.toY(note.pitch)
            double endY =  y+note_height
            if (note.start < right && note.end > left) { //&& y < down && endY > up) {

                int track = note.track
                int channel = note.channel
                Boolean mute = midi.sequencer.getTrackMute(track)
                if (!mute) {
                    Color color =  midi.channelColor[channel]

                    double start = midi.toX(note.start)
                    double end = midi.toX(note.end)
                    g.setFill(new Color(color.red, color.green, color.blue, (note.velocity)/127.0))
                    double duration = end - start
                    
                    Rectangle noteShape = new Rectangle(start, y, duration, note_height)
                    if (!selectedNotes.contains(note))
                        g.fillRect(start, y, duration, note_height)
                    else {
                        if (!(resize == 0)) {
                            if (resizingE) {
                                double draggedDuration = midi.toX(note.end + resize) - start
                                g.strokeRect(start, y, draggedDuration, note_height)
                            }
                            if (resizingW) {
                                double draggedStart = midi.toX(note.start + resize)
                                double draggedDuration = midi.toX(note.end) - draggedStart
                                g.strokeRect(draggedStart, y, draggedDuration, note_height)
                            }
                        } else if (moving) {
                            double draggedStart = midi.toX(note.start + movedX)
                            double draggedEnd = midi.toX(note.end + movedX)
                            double draggedDuration = draggedEnd - draggedStart
                            double draggedY = midi.toY(note.pitch + movedY)
                            g.strokeRect(draggedStart, draggedY, draggedDuration, note_height)
                        }
                        else {
                            g.strokeRect(start, y, duration, note_height)
                            /*DropShadow dropShadow = new DropShadow()
                            dropShadow.setInput(new Lighting())*/
                            InnerShadow rectfx = new InnerShadow()
                            rectfx.setOffsetX(4)
                            rectfx.setOffsetY(4)
                            rectfx.setColor(Color.ANTIQUEWHITE)
                            //rectfx.setColor(Color.web("272822"))
                            rectfx.setInput(new Bloom())
                            g.setEffect(rectfx)
                            g.fillRect(start, y, duration, note_height)
                            g.setEffect(null)
                        }
                    }

                    visibleRects[note] = noteShape
                }
            }
        }

        repaintPlayback()

        if (selection!=null){
            g.strokeRect(selection.getX(), selection.getY(), selection.getWidth(), selection.getHeight())
        }
    }

    void repaintPlayback() {
        gl.clearRect(0, 0, canvas.layer.getWidth(), canvas.layer.getHeight())
        double pbPos = midi.toX(midi.getPlaybackPosition())
        gl.setStroke(Color.BLUE)
        gl.setLineWidth(2.0)
        gl.strokeLine(pbPos, 0, pbPos, canvas.layer.getHeight())
        gl.setLineWidth(1.0)
    }

    void humanization(double eps=0.1) {
        midi.startEdit()
        for(MidiNote note : selectedNotes) {
            double sign = (Math.random() > 0.5 ? 1 : -1)
            long factor = (long)(note.duration * eps * sign)
            long start = note.start + factor
            long end = note.end + factor
            MidiNote newNote = midi.createMidiNote(note.channel, note.track, start, end, note.pitch, note.velocity)
            midi.removeMidiNote(note)
            midi.addMidiNote(newNote)
        }
        midi.stopEdit()
        selectedNotes = []
        selection = null
        repaint()
    }

    void quantization() {
        midi.startEdit()
        for(MidiNote note : selectedNotes) {
            long start = note.start + (long)getCorrection(note.start)
            long end = note.end + (long)getCorrection(note.end)
            MidiNote newNote = midi.createMidiNote(note.channel, note.track, start, end, note.pitch, note.velocity)
            midi.removeMidiNote(note)
            midi.addMidiNote(newNote)
        }
        midi.stopEdit()
        selectedNotes = []
        selection = null
        repaint()
    }

    double getCorrection(long tick) {
        // sixteen note midi.getResolution()
        int q = (int)(midi.getResolution() / 4)
        double r = tick % q
        if (r < (q/2))
            r = -r
        else
            r = q - r
        return r
    }


    void delete() {
        if (selectedNotes != null) {
            midi.startEdit()
            selectedNotes.each { note ->
                midi.removeMidiNote(note)
            }
            midi.stopEdit()
            selectedNotes = []
            selection = null
            repaint()
        }
    }

    MidiNote pickNote(long posX, int posY) {
        int note_height = Math.max(1, midi.scaleY(1) - 1)
        MidiNote picked = null
        //for(MidiNote note : midi.sortedByEndNotes) {
        long left = midi.getHorizontalOffset()
        double w = canvas.getWidth()
        long right = (long)( left + w * midi.getResolution() / midi.getCurrentScaleX() )
        int i = Math.abs(midi.getStartNoteIndex(left) + 1)
        long longestDuration = midi.getLongestDuration()
        while (i < midi.sortedByEndNotes.size() && midi.sortedByEndNotes[i].getEnd() < (right + longestDuration)) {
            MidiNote note = midi.sortedByEndNotes[i]
            i++
            if (note.start < posX && note.end > posX && posY == note.pitch) {
                if (selectedNotes == null)
                    selectedNotes = []
                selectedNotes << note
                picked = note
            }
        }
        return picked
    }

    void checkSelected() {
        visibleRects.each { MidiNote note, Rectangle rect ->
            if (selection != null && selection.intersects(rect.getBoundsInLocal())) { // or .contains
                selectedNotes << note
            }
        }
    }

    void mouseClicked(MouseEvent e) { 
        if (mode == PianoRollMode.SET_PLAYBACK_POSITION) {
            midi.setPlaybackPosition(midi.fromX(e.getX()))
            midi.sequencer.setTickPosition(midi.getPlaybackPosition())
            repaint()
        }
        if (mode == PianoRollMode.EDIT) {
            int velocity = 90
            long posX = midi.fromX(e.getX())
            int posY = midi.fromY(e.getY())
            MidiNote picked = pickNote(posX, posY)
            if (picked == null) {
                midi.startEdit()
                int pitch = posY
                long start = posX
                long end = (long)( posX + (midi.getResolution() * insertNoteType) )
                int currentChannel = 0
                MidiNote newNote = midi.createMidiNote(midi.currentChannel, midi.currentTrack, start, end, pitch, velocity)
                midi.addMidiNote(newNote)
                midi.stopEdit()
                repaint()
            }
        }
        if (mode == PianoRollMode.SELECT) {
            long posX = midi.fromX(e.getX())
            int posY = midi.fromY(e.getY())
            pickNote(posX, posY)
            repaint()
        }
    }

    void mousePressed(MouseEvent e) {
        if (mode == PianoRollMode.EDIT) {
            long posX = midi.fromX(e.getX())
            int posY = midi.fromY(e.getY())
            MidiNote note = pickNote(posX, posY)
            if (note != null) {
                clickX = e.getX()
                clickY = e.getY()
                if ((note.end - posX) < (note.duration*0.25)) {
                    resizingE = true
                    oldCursor = this.cursor
                    this.cursor =  Cursor.E_RESIZE
                } else if ((posX - note.start) < (note.duration*0.25)) {
                    resizingW = true
                    oldCursor = this.cursor
                    this.cursor =  Cursor.E_RESIZE
                } else {
                    moving = true
                    oldCursor = this.cursor
                    this.cursor =  Cursor.MOVE
                }
            }
        }
        if (mode == PianoRollMode.SELECT) {
            anchor = new Point2D(e.getX(), e.getY())
            selection = new Rectangle(anchor.getX(), anchor.getY(), 0.0D, 0.0D) // (double x, double y, double width, double height)
            selectedNotes = []
        }
        if (mode == PianoRollMode.PANNING) {
            clickX = e.getX()
            clickY = e.getY()
        }
    }

    void mouseDragged(MouseEvent e) {
        if (mode == PianoRollMode.EDIT) {
            if (resizingE || resizingW) {
                dragClickX = e.getX()
                resize = midi.fromX(dragClickX) - midi.fromX(clickX)
            } else if (moving) {
                dragClickX = e.getX()
                dragClickY = e.getY()
                movedX = midi.fromX(dragClickX) - midi.fromX(clickX)
                movedY = midi.fromY(dragClickY) - midi.fromY(clickY)
            }
        }
        if (mode == PianoRollMode.SELECT) {
            selection = new Rectangle( Math.min(anchor.getX(),e.getX()), Math.min(anchor.getY(),e.getY()),
                                       Math.abs(e.getX() - anchor.getX()), Math.abs(e.getY() - anchor.getY()) )
        }
        if (mode == PianoRollMode.PANNING) {
            dragClickX = e.getX()
            dragClickY = e.getY()
            long scrollX = midi.fromX(clickX) - midi.fromX(dragClickX)
            int scrollY = midi.fromY(clickY) - midi.fromY(dragClickY)

            long newViewX = (midi.getHorizontalOffset() + scrollX)
            long totLength = midi.sequence.tickLength + (long)(MARGIN_RIGHT * midi.getResolution() / midi.getCurrentScaleX())
            long maxMoveX = (long)( totLength - (canvas.getWidth() * midi.getResolution() / midi.getCurrentScaleX()) )
            
            if (newViewX < 0) newViewX = 0
            if (newViewX > maxMoveX) newViewX = maxMoveX
            midi.setHorizontalOffset(newViewX)

            int newViewY = (midi.getVerticalOffset() - scrollY)
            // midi.lengthY is 127
            int maxMoveY = (int)( midi.lengthY - canvas.getHeight() / midi.getCurrentScaleY() )
            if (newViewY < 0) newViewY = 0
            if (newViewY > maxMoveY) newViewY = maxMoveY
            midi.setVerticalOffset(newViewY)

            clickX = dragClickX
            clickY = dragClickY
        } 
        repaint()
    }

    void mouseReleased(MouseEvent e) {
        if (mode == PianoRollMode.EDIT) {
            if (resizingE || resizingW) {
                midi.startEdit()
                selectedNotes.each { note ->
                    long start = note.start
                    long end = note.end
                    if (resizingE)
                        end += resize
                    if (resizingW)
                        start += resize
                    MidiNote newNote = midi.createMidiNote(note.channel, note.track, start, end, note.pitch, note.velocity)
                    midi.removeMidiNote(note)
                    midi.addMidiNote(newNote)
                }
                midi.stopEdit()
                selectedNotes = []
                selection = null
                //--------
                this.cursor = oldCursor
                resizingE = false
                resizingW = false
                resize = 0
            } else if (moving) {
                midi.startEdit()
                selectedNotes.each { note ->
                    long start = note.start + movedX
                    long end = note.end + movedX
                    int y = note.pitch + movedY
                    MidiNote newNote = midi.createMidiNote(note.channel, note.track, start, end, y, note.velocity)
                    midi.removeMidiNote(note)
                    midi.addMidiNote(newNote)
                }
                midi.stopEdit()
                selectedNotes = []
                selection = null
                //--------
                this.cursor = oldCursor
                moving = false
                movedX = 0
                movedY = 0
            }
        }
        if (mode == PianoRollMode.SELECT) {
            checkSelected()
            selection = null
        }
        repaint()
    }

    void mouseWheelMoved(ScrollEvent e) {
        Boolean up = (e.getDeltaY() < 0)
        int note_height = Math.max(1, midi.scaleY(1) - 1)
        int units_to_scroll = 3
        int scrollY = (int)( (note_height * units_to_scroll) / midi.getCurrentScaleY() )
        int newViewY
        if (up) 
            newViewY = (midi.getVerticalOffset() - scrollY)
        else
            newViewY = (midi.getVerticalOffset() + scrollY)
        int maxMoveY = (int)( midi.lengthY - canvas.getHeight() / midi.getCurrentScaleY() )
        if (newViewY < 0) newViewY = 0
        if (newViewY > maxMoveY) newViewY = maxMoveY
        midi.setVerticalOffset(newViewY)
    }

    void playbackAtTick(long tick) {
        midi.setPlaybackPosition(tick)
        long right = (long)( midi.getHorizontalOffset() + canvas.getWidth() * midi.getResolution() / midi.getCurrentScaleX() )
        if (midi.getPlaybackPosition() < midi.getHorizontalOffset() || midi.getPlaybackPosition() > right) {
            midi.setHorizontalOffset(midi.getPlaybackPosition())
            repaint()
        }
    }

}