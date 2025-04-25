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


class ControllerEditor {

    double clickX
    double clickY
    double dragClickX
    double dragClickY
    double endCurveX
    double endCurveY

    Boolean isDragging
    Boolean isEditingCurve

    ControllerEditMode pencilMode = ControllerEditMode.LINE

    Rectangle selection

    MidiView midi
    ResizableRegion region
    GraphicsContext g
    GraphicsContext gl

    Cursor cursor	

    ControllerEditor(MidiView midi, ResizableRegion region) {
        this.midi = midi
        setupRegion(region)
		repaint()
    }

    void setCursor(Cursor c) {
        this.cursor = c 
        repaint()
    }

    /*
              y2 - y1
    y - y1 = _________ (x - x1)

              x2 - x1

                        (y - y1)
    x = x1 + (x2 - x1) ___________

                        (y2 - y1) 


    since y is linear monotone we can step on y by one and get x
    */
    void editLine(long startX, long endX, int startY, int endY, double h) {
        
        ObservableMap<Long, MidiCC> data = midi.controllers[midi.currentController][midi.currentChannel] // controllers[7][0]
        Set<Long> keys = data.keySet()
        keys.each { key ->
            if (key >= startX && key <= endX)
                data.remove(key)
        }

        long startEvt
        int y = startY
        Boolean stop = false
        while (!stop) {
            int val = Math.min(127, Math.max(0, y))
            if (startY == endY) {
                startEvt = startX
            } else {
                startEvt = (long)( startX + (endX - startX) * ((y - startY) / (endY - startY)) )
            }

            /*ShortMessage msg = new ShortMessage()
            msg.setMessage(ShortMessage.CONTROL_CHANGE, 0, 7, val)
            MidiEvent midiEvt = new MidiEvent(msg, startEvt)*/
            int track = 0
            data[startEvt] = midi.createMidiCC(midi.currentChannel, track, startEvt, midi.currentController, val)

            if (startY <= endY) {
                y += 1
                if (y >= endY) stop = true 
            } else {
                y -= 1
                if (y <= endY) stop = true
            }
        }

    }

    /*
    B(t) = (1-t)^2*p0+2(1-t)t*p1+(t^2)*p2
    */
    void editCurve() {
        double h = region.getHeight()
        long startX = midi.fromX(clickX)
        long endX = midi.fromX(endCurveX)

        ObservableMap<Long, MidiCC> data = midi.controllers[midi.currentController][midi.currentChannel]
        Set<Long> keys = data.keySet()
        keys.each { key ->
            if (key >= startX && key <= endX)
                data.remove(key)
        }

        double step = 0.01

        Map<Long,Integer> tmp = [:]

        for(double t=0.0;t<=1;t+=step) {
            int x = (int) ( (1-t)*(1-t)*clickX + 2*(1-t)*t*dragClickX+t*t*endCurveX);
            int y = (int) ( (1-t)*(1-t)*clickY + 2*(1-t)*t*dragClickY+t*t*endCurveY);
            long startEvt = midi.fromX(x)
            int val = (int)( 127 * (h - y) / h )
            
            // data[startEvt] = midi.createMidiCC(midi.currentChannel, track, startEvt, midi.currentController, val)
            // avoiding propertyChange in case of same startEvt
            tmp[startEvt] = val
        }

        int track = 0
        tmp.each { startEvt, val ->
            data[startEvt] = midi.createMidiCC(midi.currentChannel, track, startEvt, midi.currentController, val)
        }
    }

    void editHorizontalLine(long startX, long endX, int startY, int endY) {
        
        ObservableMap<Long, MidiCC> data = midi.controllers[midi.currentController][midi.currentChannel] // controllers[7][0]
        Set<Long> keys = data.keySet()
        keys.each { key ->
            if (key >= startX && key <= endX)
                data.remove(key)
        }

        int startVal, endVal
        if (startY < endY) {
            startVal = 127
            endVal = 0
        } else {
            startVal = 0
            endVal = 127
        }

        int track = 0
        data[startX] = midi.createMidiCC(midi.currentChannel, track, startX, midi.currentController, startVal)
        data[endX] = midi.createMidiCC(midi.currentChannel, track, endX, midi.currentController, endVal) 
    }

    void select() {
        selection = new Rectangle( Math.min(clickX,dragClickX), Math.min(clickY,dragClickY),
                                   Math.abs(dragClickX - clickX), Math.abs(dragClickY - clickY) )
    }

    void delete() {
        if (selection != null) {
            long startX = midi.fromX(selection.getX())
            long endX = midi.fromX(selection.getX() + selection.getWidth())
            ObservableMap<Long, MidiCC> data = midi.controllers[midi.currentController][midi.currentChannel]
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

    void mouseClicked(MouseEvent event) {
        if (midi.mode == Mode.SET_PLAYBACK_POSITION) {
            midi.setPlaybackPosition(midi.fromX(event.getX()))
            midi.sequencer.setTickPosition(midi.getPlaybackPosition())
            repaint()
        }
        if (midi.mode == Mode.EDIT) {
            if (pencilMode == ControllerEditMode.CURVE) {
                endCurveX = event.getX()
                endCurveY = event.getY()
                isEditingCurve = false

                midi.startEdit()
                editCurve()
                midi.stopEdit()
                repaint()
            }
        }
    }

    void mousePressed(MouseEvent event) {
        if (!isEditingCurve) {
            clickX = event.getX()
            clickY = event.getY()
        }
    }

    void mouseDragged(MouseEvent event) {
        dragClickX = event.getX()
        dragClickY = event.getY()  
        isDragging = true  
        repaint()    
    }

    void mouseReleased(MouseEvent event) {
        //isDragging = false

        double h = region.getHeight()
        long startX = midi.fromX(clickX)
        long endX = midi.fromX(dragClickX)
        int startY = (int)( 128 * (h - clickY) / h )
        int endY = (int)( 128 * (h - event.getY()) / h )

        if (midi.mode == Mode.SELECT) {
            select()
            repaint()
        } else if (midi.mode == Mode.EDIT) {
            if (midi.controllersInfo[midi.currentController].ctype == 0) {
                if (pencilMode == ControllerEditMode.LINE) {
                    midi.startEdit()
                    editLine(startX, endX, startY, endY, h)
                    midi.stopEdit()
                    repaint()
                } else {
                    isEditingCurve = true
                    endCurveX = event.getX()
                    endCurveY = event.getY()
                }
            } else {
                midi.startEdit()
                editHorizontalLine(startX, endX, startY, endY)
                midi.stopEdit()
                repaint()
            }
        }
    }

    void mouseEntered(MouseEvent event) { }

    void mouseExited(MouseEvent event) { }

    void mouseMoved(MouseEvent event) {
        if (isEditingCurve) {
            endCurveX = event.getX()
            endCurveY = event.getY() 
            repaint()
        }
    }

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
				//if (e.getClickCount() == 1)
				if (isDragging)
					isDragging = false
				else {
					mouseClicked(e)
				}
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
		// g.clearRect(0, 0, region.getWidth(), region.getHeight())
        // monokai bg
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
                g.strokeLine(tkToPx, 0, tkToPx, region.getHeight())
            }
        }


        g.setFill(Color.BLACK)
        if (isDragging) {
            g.strokeLine(clickX, clickY, dragClickX, dragClickY)
        }
        if (isEditingCurve) {
            g.strokeLine(clickX, clickY, dragClickX, dragClickY)
            g.strokeOval(dragClickX, dragClickY, 3, 3)
            g.beginPath()
            g.bezierCurveTo(clickX, clickY, dragClickX, dragClickY, endCurveX, endCurveY)
            g.stroke()
            g.closePath()
        }

        long startX = -1
        long endX = -1
        if (selection != null) {
            startX = midi.fromX(selection.getX())
            endX = midi.fromX(selection.getX() + selection.getWidth())
        }

        for(Map.Entry<Integer,ObservableMap<Long, MidiCC>> e : midi.controllers[midi.currentController].entrySet()) {
            int channel = e.getKey()
            ObservableMap<Long, MidiCC> data = e.getValue()

            Color color =  midi.channelColor[channel]
            g.setFill(color)

            double h = region.getHeight()
            List<Long> keys = data.keySet() as List<Long>
            int lastIndex = keys.size() - 1
            //for(int i=0; i<keys.size(); i++) {
            // ---------------------------------------------------------------------------------
            int idx = Math.abs(midi.getStartCCIndex(left, data) + 1) // (-(insertion point) - 1)
            if (idx > 0)
                idx -= 1
            for(int i=idx; i<keys.size() && (keys[i] == null || keys[i] <= right); i++) {
            // ---------------------------------------------------------------------------------            
                long key = keys[i]
                MidiCC cc = data[keys[i]]
                int val = cc.getValue()

                double rectHeight = ((val * -h) / 128) + h
                double screenX = midi.toX(key)
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
                g.setStroke(Color.color(color.red, color.green, color.blue))
                g.strokeLine(screenX, rectHeight, endLineX, rectHeight)
                if (channel == midi.currentChannel) {
                    boolean selected = (startX > 0 && endX > 0) && (key >= startX && key <= endX)
                    if (selected) {
                        g.setFill(new Color(color.red, color.green, color.blue, 0.9d))
                    } else {
                        g.setFill(new Color(color.red, color.green, color.blue, 0.5d))                        
                    }
                    g.fillRect(screenX, rectHeight, duration, h-rectHeight)
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
