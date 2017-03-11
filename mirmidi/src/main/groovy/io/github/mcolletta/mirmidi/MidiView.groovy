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

package io.github.mcolletta.mirmidi

import javax.sound.midi.Synthesizer
import java.nio.file.Path

import javafx.beans.property.IntegerProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.LongProperty
import javafx.beans.property.SimpleLongProperty

import javafx.scene.paint.Color

import groovy.transform.CompileStatic

enum TempoEditMode {
    EDITED, AXIS
}

enum ControllerEditMode {
    LINE, CURVE
}

enum Mode {
    EDIT, SELECT, PANNING, SET_PLAYBACK_POSITION
}

@CompileStatic
class MidiView extends MidiManager {

    Mode mode = Mode.EDIT

    List<Color> channelColor = []
    int currentChannel = 0
    int currentController = 7  // VOLUME
    int currentInstrument = 0  // PIANO

    int NOTE_PIXEL_WIDTH = 50   // width of a note: how many pixel
    int NOTE_PIXEL_HEIGHT = 10  // height of a note: how many pixel

    IntegerProperty currentScaleX = new SimpleIntegerProperty(NOTE_PIXEL_WIDTH)  
    IntegerProperty currentScaleY = new SimpleIntegerProperty(NOTE_PIXEL_HEIGHT)
    
    LongProperty horizontalOffset  = new  SimpleLongProperty(0L)
    IntegerProperty verticalOffset = new SimpleIntegerProperty(50)
    
    long lengthX = 0L
    int lengthY = 127

    MidiView(Path path=null, Synthesizer synth=null) {
        super(synth)
        if (path != null) {
            loadMidi(path.toFile())
        }
        List<String> hexColors = ["#3ad900", "#f92672", "#70d9ef", "#a6e22e", 
                                  "#ae81ff", "#C8C800", "#ff628c", "#80ffbb", 
                                  "#ff80e1", "#0088ff", "#ff9d00", "#e64637",  
                                  "#ffdd00", "#ff00ff", "#ff0000", "#ffee80"]

        for(String hexColor : hexColors) {
            channelColor << Color.web(hexColor)
        }
    }

    final long getHorizontalOffset() { return horizontalOffset.get() }
    final void setHorizontalOffset(long value) { horizontalOffset.set(value) }
    LongProperty horizontalOffsetProperty() { return horizontalOffset }

    final int getVerticalOffset() { return verticalOffset.get() }
    final void setVerticalOffset(int value) { verticalOffset.set(value) }
    IntegerProperty verticalOffsetProperty() { return verticalOffset }

    final int getCurrentScaleX() { return currentScaleX.get() }
    final void setCurrentScaleX(int value) { currentScaleX.set(value) }
    IntegerProperty currentScaleXProperty() { return currentScaleX }

    final int getCurrentScaleY() { return currentScaleY.get() }
    final void setCurrentScaleY(int value) { currentScaleY.set(value) }
    IntegerProperty currentScaleYProperty() { return currentScaleY }

    double toX(long tick) {
        // (tick - getHorizontalOffset()) translation to new coord system
        return (double)Math.floor((tick - getHorizontalOffset()) * getCurrentScaleX() / getResolution())
    }

    double toY(int pitch) {
        // (lengthY - pitch) --> downward verse of y axis; ex. (127 - 72)
        // ( (lengthY - pitch) - getVerticalOffset() ) --> translation to new coord system
        return (double)Math.floor((lengthY - pitch - getVerticalOffset()) * getCurrentScaleY())
    }

    long fromX(double x) {
        return (long)Math.ceil(getHorizontalOffset() + x * getResolution() / getCurrentScaleX())
    }

    int fromY(double y) {
        return (int)Math.ceil(lengthY - getVerticalOffset() - y / getCurrentScaleY())
    }

    long scaleX(long x) {
        return (long)Math.round( (double)(getCurrentScaleX() * x / getResolution()) )
    }

    int scaleY(int y) {
        return (int)Math.floor(getCurrentScaleY() * y)
    }

    // ZOOMING

    void setCurrentZoom(float zoom) {
        int x = (int)(NOTE_PIXEL_WIDTH * zoom)
        int y = (int)((x / NOTE_PIXEL_WIDTH) * NOTE_PIXEL_HEIGHT)
        setCurrentScaleX(x)
        setCurrentScaleY(y)
    }

    float getCurrentZoom() {
        return getCurrentScaleX() / NOTE_PIXEL_WIDTH
    }

}

@CompileStatic
class MidiInstrument {
    String name
    int program

    String toString() {
        return name + " (GM $program)"
    }
}
