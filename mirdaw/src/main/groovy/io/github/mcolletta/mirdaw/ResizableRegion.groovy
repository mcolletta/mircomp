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

import javafx.beans.value.ObservableValue
import javafx.beans.value.ChangeListener


class ResizableRegion extends Region {  
  
    Canvas drawing = new Canvas()
    Canvas layer = new Canvas()

    ResizableRegion() {  
        getChildren().add(drawing)
        getChildren().add(layer)
    }  
  
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
        drawing.relocate(contentX, contentY) 
        drawing.setWidth(contentWith) 
        drawing.setHeight(contentHeight)  
        // layer
        layer.relocate(contentX, contentY) 
        layer.setWidth(contentWith) 
        layer.setHeight(contentHeight)  
    }   

    GraphicsContext getGraphicsContext2D() {
		return drawing.getGraphicsContext2D()
	}

    GraphicsContext getLayerGraphicsContext2D() {
        return layer.getGraphicsContext2D()
    }
}

