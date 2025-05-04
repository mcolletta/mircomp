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

import javafx.geometry.Insets
import javafx.scene.layout.Region
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext

import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent
import javafx.scene.Cursor

import javafx.event.EventHandler
import javafx.scene.input.KeyEvent
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCombination
import javafx.scene.input.KeyCodeCombination

import javafx.beans.value.ObservableValue
import javafx.beans.value.ChangeListener


abstract class ResizableRegion extends Region {

    final KeyCombination keyCtrZ = new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN)
    final KeyCombination keyCtrY = new KeyCodeCombination(KeyCode.Y, KeyCombination.SHORTCUT_DOWN)
  
    private Canvas drawing = new Canvas()
    private Canvas layer = new Canvas()
    protected GraphicsContext g
    protected GraphicsContext gl

    protected Boolean isDragging

    ResizableRegion() {  
        getChildren().add(drawing)
        getChildren().add(layer)
        g = drawing.getGraphicsContext2D()
        gl = layer.getGraphicsContext2D()
        setupRegion()
    }

    // Events
    
    protected void setupRegion() {

        // widthProperty().addListener(new ChangeListener<Number>() {
        //     @Override
        //     public void changed(ObservableValue<? extends Number> observableValue, Number number, Number number2) {
        //         repaint()
        //     }
        // })

        // heightProperty().addListener(new ChangeListener<Number>() {
        //     @Override
        //     public void changed(ObservableValue<? extends Number> observableValue, Number number, Number number2) {
        //         repaint()
        //     }
        // })

        drawing.widthProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number number2) {
                repaint()
            }
        })

        drawing.heightProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number number2) {
                repaint()
            }
        })

        layer.widthProperty().addListener(new ChangeListener<Number>() {
                @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number number2) {
                repaintLayer()
            }
        })

        layer.heightProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number number2) {
                repaintLayer()
            }
        })

        setFocusTraversable(true)

        addEventHandler(MouseEvent.MOUSE_CLICKED,
        new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent e) {
                if (isDragging)
                    isDragging = false
                else {
                    mouseClicked(e)
                }
            }
        });

        addEventHandler(MouseEvent.MOUSE_PRESSED,
        new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent e) {
                mousePressed(e)
            }
        });

        addEventHandler(MouseEvent.MOUSE_DRAGGED,
        new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent e) {
                mouseDragged(e)
            }
        });

        addEventHandler(MouseEvent.MOUSE_RELEASED,
        new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent e) {
                mouseReleased(e)
            }
        });

        addEventHandler(MouseEvent.MOUSE_ENTERED,
        new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent e) {
                mouseEntered(e)
            }
        });

        addEventHandler(MouseEvent.MOUSE_EXITED,
        new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent e) {
                mouseExited(e)
            }
        });

        addEventHandler(MouseEvent.MOUSE_MOVED,
        new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent e) {
                mouseMoved(e)
            }
        });

        setOnScroll(new EventHandler<ScrollEvent>() {
            @Override
            public void handle(ScrollEvent e) {
                mouseWheelMoved(e)
            }
        });

        setOnKeyPressed(
        new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                if (keyEvent.getCode() == KeyCode.DELETE) {
                    delete()
                } else if (keyCtrZ.match(keyEvent)) {
                    undo()
                } else if (keyCtrY.match(keyEvent)) {
                    redo()
                }
                repaint()
            }
        });

        addEventFilter(MouseEvent.ANY, { requestFocus() })
    }

    protected void reset() {
        g.clearRect(0, 0, getWidth(), getHeight())
    }

    protected void repaint() {
        g.clearRect(0, 0, getWidth(), getHeight())
    }

    protected void repaintLayer() {
        gl.clearRect(0, 0, getWidth(), getHeight())
    }

    protected void undo() {}

    protected void redo() {}

    protected void delete() {}

    protected void mouseClicked(MouseEvent e) {}

    protected void mousePressed(MouseEvent e) {}

    protected void mouseDragged(MouseEvent e) {}

    protected void mouseReleased(MouseEvent e) {}

    protected void mouseEntered(MouseEvent event) {}

    protected void mouseExited(MouseEvent event) {}

    protected void mouseMoved(MouseEvent event) {}

    protected void mouseWheelMoved(ScrollEvent e) {}

    // -----------------------------------------------------
  
    @Override
    protected void layoutChildren() {
        super.layoutChildren()
        final double width = getWidth()
        final double height = getHeight()
        final Insets insets = getInsets()
        final double contentX = insets.getLeft()
        final double contentY = insets.getTop()
        final double contentWith = Math.max(0, width - (insets.getLeft() + insets.getRight()))
        final double contentHeight = Math.max(0, height - (insets.getTop() + insets.getBottom()))
        // drawing
        drawing.relocate(contentX, contentY)
        drawing.setWidth(contentWith)
        drawing.setHeight(contentHeight)
        // layer
        layer.relocate(contentX, contentY)
        layer.setWidth(contentWith)
        layer.setHeight(contentHeight)
    }

}

