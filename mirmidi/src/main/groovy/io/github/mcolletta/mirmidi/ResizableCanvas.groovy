/*
 * Copyright (C) 2016-2022 Mirco Colletta
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

import javafx.geometry.Insets
import javafx.scene.layout.Region
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext

import javafx.beans.value.ObservableValue
import javafx.beans.value.ChangeListener


class ResizableCanvas extends Region {  
  
    Canvas canvas = new Canvas()
    Canvas layer = new Canvas()
    
    Closure repaint = {
    	double width = canvas.getWidth() 
        double height = canvas.getHeight() 
        GraphicsContext gc = canvas.getGraphicsContext2D()
        gc.clearRect(0, 0, width, height)
    }

    Closure repaintLayer = {
        double width = layer.getWidth() 
        double height = layer.getHeight() 
        GraphicsContext gc = layer.getGraphicsContext2D()
        gc.clearRect(0, 0, width, height)
    }
  
    ResizableCanvas() {  
        getChildren().add(canvas) 
        canvas.widthProperty().addListener(new ChangeListener<Number>() {
		        @Override
		        public void changed(ObservableValue<? extends Number> observableValue, Number number, Number number2) {
		            repaint.call()
		        }
		    })  
        canvas.heightProperty().addListener(new ChangeListener<Number>() {
	        @Override
		        public void changed(ObservableValue<? extends Number> observableValue, Number number, Number number2) {
		            repaint.call()
		        }
		    })
        // layer
        getChildren().add(layer)
        layer.widthProperty().addListener(new ChangeListener<Number>() {
                @Override
                public void changed(ObservableValue<? extends Number> observableValue, Number number, Number number2) {
                    repaintLayer.call()
                }
            })  
        layer.heightProperty().addListener(new ChangeListener<Number>() {
            @Override
                public void changed(ObservableValue<? extends Number> observableValue, Number number, Number number2) {
                    repaintLayer.call()
                }
            })
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
        canvas.relocate(contentX, contentY) 
        canvas.setWidth(contentWith) 
        canvas.setHeight(contentHeight)  
        // layer
        layer.relocate(contentX, contentY) 
        layer.setWidth(contentWith) 
        layer.setHeight(contentHeight)  
    }   

    GraphicsContext getGraphicsContext2D() {
		return canvas.getGraphicsContext2D()
	}

    GraphicsContext getLayerGraphicsContext2D() {
        return layer.getGraphicsContext2D()
    }
}
