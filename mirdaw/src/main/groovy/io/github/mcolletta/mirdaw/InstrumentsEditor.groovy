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

import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

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

import io.github.mcolletta.mirdaw.Instrument


class InstrumentsEditor {

    MidiPC selectedItem
    Map<MidiPC,Rectangle> visibleRects

    MidiView midi
    ResizableRegion region
    GraphicsContext g
    GraphicsContext gl

    Cursor cursor	

    InstrumentsEditor(MidiView midi, ResizableRegion region) {
        this.midi = midi
        setupRegion(region)
        visibleRects = [:]
		repaint()
    }

    void setCursor(Cursor c) {
        this.cursor = c 
        repaint()
    }

    void edit(int channel, long tick, int program) {
    	ObservableMap<Long, MidiPC> data = midi.programs[channel]
    	int track = 0
    	data[tick] = midi.createMidiPC(channel, track, tick, program)
    	// println "tick=" + tick + " channel=" + channel + " program=" + program
    }

    void select(double clickX, double clickY) {
        visibleRects.each { MidiPC pc, Rectangle rect ->
            if (rect.contains(clickX,clickY)) {
                selectedItem = pc
            }
        }
    }

    void delete() {
        if (selectedItem != null) {
            int channel = selectedItem.getChannel()
            ObservableMap<Long, MidiPC> data = midi.programs[channel]
            midi.startEdit()
            data.remove(selectedItem.getTick())
            midi.stopEdit()
            selectedItem = null
            repaint()
        }
    }

    void mouseClicked(MouseEvent event) {
        if (midi.mode == Mode.SET_PLAYBACK_POSITION) {
            midi.setPlaybackPosition(midi.fromX(event.getX()))
            midi.sequencer.setTickPosition(midi.getPlaybackPosition())
            repaint()
        }
        if (midi.mode == Mode.EDIT || midi.mode == Mode.SELECT) {
        	long x = midi.fromX(event.getX())
        	double h = region.getHeight()
            double rectHeight = h / 16
        	int channel = (int) (event.getY() / rectHeight)
            if (channel != 9) {
            	int currentProgram = midi.currentInstrument
                if (midi.mode == Mode.EDIT) {
                    midi.startEdit()
                    edit(channel, x, currentProgram)
                    midi.stopEdit()
                    repaint()
                } else if (midi.mode == Mode.SELECT) {
                    select(event.getX(),event.getY())
                    repaint()
                }
            }
        }
    }

    void mousePressed(MouseEvent event) { }

    void mouseDragged(MouseEvent event) { }

    void mouseReleased(MouseEvent event) { }

    void mouseEntered(MouseEvent event) { }

    void mouseExited(MouseEvent event) { }

    void mouseMoved(MouseEvent event) { }

    void playbackAtTick(long tick) {
        midi.setPlaybackPosition(tick)
        long right = (long)( midi.getHorizontalOffset() + region.getWidth() * midi.getResolution() / midi.getCurrentScaleX() )
        if (midi.getPlaybackPosition() < midi.getHorizontalOffset() || midi.getPlaybackPosition() > right) {
            midi.setHorizontalOffset(midi.getPlaybackPosition())
            repaint()
        }
    }

    // Region

    void setupRegion(ResizableRegion region) {
        this.region = region
        this.g = region.getGraphicsContext2D()
        this.gl = region.getLayerGraphicsContext2D()

        region.drawing.widthProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number number2) {
                repaint()
            }
        })  
        region.drawing.heightProperty().addListener(new ChangeListener<Number>() {
	        @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number number2) {
                repaint()
            }
        })
        region.layer.widthProperty().addListener(new ChangeListener<Number>() {
                @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number number2) {
                repaintLayer()
            }
        })  
        region.layer.heightProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number number2) {
                repaintLayer()
            }
        })

        region.setFocusTraversable(true)

		region.addEventHandler(MouseEvent.MOUSE_CLICKED,
		new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent e) {
				mouseClicked(e)
			}
		});

		region.addEventHandler(MouseEvent.MOUSE_PRESSED,
		new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent e) {
				mousePressed(e)
			}
		});

		region.addEventHandler(MouseEvent.MOUSE_DRAGGED,
		new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent e) {
				mouseDragged(e)
			}
		});

		region.addEventHandler(MouseEvent.MOUSE_RELEASED,
		new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent e) {
				mouseReleased(e)
			}
		});

		region.addEventHandler(MouseEvent.MOUSE_MOVED,
		new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent e) {
				mouseMoved(e)
			}
		});

        final KeyCombination keyCtrZ = new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN)
        final KeyCombination keyCtrY = new KeyCodeCombination(KeyCode.Y, KeyCombination.SHORTCUT_DOWN)
        region.setOnKeyPressed(
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

        region.addEventFilter(MouseEvent.ANY, { region.requestFocus() })
    }

    private void reset() {
        g.setFill(Color.web("272822"))
        g.fillRect(0, 0, region.getWidth(), region.getHeight())
	}

    protected void repaint() {
        region.setCursor(cursor)
        reset()

        int resolution = midi.getResolution()
        long left = midi.getHorizontalOffset()
        double w = region.getWidth()
        long right = (long)( left + w * midi.getResolution() / midi.getCurrentScaleX() )

        double h = region.getHeight()
        double rectHeight = h / 16
        int fontSize = (int) (rectHeight * 0.75)

        g.setFill(Color.web("272822"))

        for (int channel=0; channel<16; channel++) {
            if (midi.usedChannels[channel] > 0) {
                Color color =  midi.channelColor[channel]
                g.setFill(color)
                double lw = g.getLineWidth()
                double screenY = (channel * rectHeight)
                double default_instr_duration = region.getWidth()
                g.setStroke(Color.color(color.red, color.green, color.blue))
                g.strokeRect(midi.toX(left), screenY+lw, default_instr_duration, rectHeight-lw)
                g.setFill(Color.WHITE)              
                g.setFont(Font.font("Verdana", fontSize))
                g.fillText("default instrument", 0, screenY+fontSize, default_instr_duration)
            }
        }

        visibleRects = [:]

        for(Map.Entry<Integer,ObservableMap<Long, MidiPC>> e : midi.programs.entrySet()) {
            int channel = e.getKey()
            ObservableMap<Long, MidiPC> data = e.getValue()

            Color color =  midi.channelColor[channel]
            g.setFill(color)

            List<Long> keys = data.keySet() as List<Long>
            int lastIndex = keys.size() - 1
            int idx = Math.abs(midi.getStartPCIndex(left, data) + 1) // (-(insertion point) - 1)
            if (idx > 0)
                idx -= 1
            for(int i=idx; i<keys.size() && (keys[i] == null || keys[i] <= right); i++) {            
                long key = keys[i]
                MidiPC pc = data[keys[i]]
                int program = pc.getInstrument()
                double screenX = midi.toX(key)
                double screenY = (channel * rectHeight)
                double duration = 0
                double endLineX = 0
                if (i != lastIndex) {
                    long nextKey = keys[i+1]
                    duration = midi.toX(nextKey)-midi.toX(key)
                    endLineX = screenX + duration
                } else {
                    duration = region.getWidth()-screenX
                    endLineX = region.getWidth()
                }
                // clear
                g.setFill(Color.web("272822"))
                g.fillRect(screenX, screenY, duration, rectHeight)
                //g.setLineWidth(1.0) // default
                double lw = g.getLineWidth()
                if (pc != selectedItem) {                    
                    g.setStroke(Color.color(color.red, color.green, color.blue))
                    g.strokeRect(screenX, screenY+lw, duration, rectHeight-lw)
                } else {
                    g.setFill(new Color(color.red, color.green, color.blue, 0.5d))
                    g.fillRect(screenX, screenY, duration, rectHeight)
                }
                // Instrument name
                String name = Instrument.GM.find { it.value == program }?.key
                name += " (GM $program)"
                g.setFill(Color.WHITE)				
				g.setFont(Font.font("Verdana", fontSize))
				g.fillText(name, screenX+lw, screenY+fontSize, duration)
                //----------------
                Rectangle pcShape = new Rectangle(screenX, screenY, duration, rectHeight)
                visibleRects[pc] = pcShape

                if (i == 0 && key > midi.getHorizontalOffset()) {
                    double default_instr_duration = Math.min(screenX, region.getWidth())
                    g.strokeRect(midi.toX(left), screenY+lw, default_instr_duration, rectHeight-lw)
                    g.fillText("default instrument", 0, screenY+fontSize, default_instr_duration)
                }
            }            
        }

        repaintLayer()
    }

    void repaintLayer() {
        gl.clearRect(0, 0, region.getWidth(), region.getHeight())
        double pbPos = midi.toX(midi.getPlaybackPosition())
        if (pbPos > 0) {
            gl.setStroke(Color.BLUE)
            gl.setLineWidth(2.0d)
            gl.strokeLine(pbPos, 0, pbPos, region.layer.getHeight())
            gl.setLineWidth(1.0d)
        }
    }

}
