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

import javafx.scene.chart.LineChart
import javafx.scene.chart.BarChart
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.CategoryAxis
import javafx.scene.chart.XYChart

import javafx.scene.Node

import javafx.scene.layout.VBox

import javafx.scene.control.SplitPane
import javafx.scene.control.Dialog
import javafx.scene.control.DialogPane
import javafx.scene.control.ButtonType
import javafx.scene.control.ButtonBar.ButtonData

import javafx.stage.StageStyle

import javafx.geometry.Pos
import javafx.geometry.Orientation



public class UtilsFx {

	public static LineChart<Number, Number> createLineChart(float[][] XY, String title="", 
											      						  String xaxis="x", String yaxis="y",
                                                  						  String seriesName="f(x)") {
        final NumberAxis xAxis = new NumberAxis()
        final NumberAxis yAxis = new NumberAxis()
        xAxis.setLabel(xaxis)
        yAxis.setLabel(yaxis)
        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis)
        chart.setCreateSymbols(false)
        chart.setTitle(title)

        XYChart.Series series = new XYChart.Series()
        series.setName(seriesName)
        
        for (int n = 0; n < XY.length; n++) {
            float x = XY[n][0]
            float y = XY[n][1]
            series.getData().add(new XYChart.Data(x, y))
        }
                    
        chart.getData().add(series)

        return chart
    }

	public static BarChart<String, Number> createBarChart(float[][] XY, String title="", 
											      						String xaxis="x", String yaxis="y",
                                                  						String seriesName="f(x)") {
        final CategoryAxis xAxis = new CategoryAxis()
        final NumberAxis yAxis = new NumberAxis()
        xAxis.setLabel(xaxis)
        yAxis.setLabel(yaxis)
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis)
        chart.setTitle(title)

        XYChart.Series series = new XYChart.Series();
        series.setName(seriesName)
        
        for (int n = 0; n < XY.length; n++) {
            float x = XY[n][0]
            float y = XY[n][1]
            series.getData().add(new XYChart.Data(x.toString(), y))
        }
                    
        chart.getData().add(series)

        return chart
    }

	public static void showPlot(List<float[][]> data, int width=700, int height=500) {
        Dialog<ButtonType> dialog = new Dialog<>()
        DialogPane dialogPane = dialog.getDialogPane()
        dialog.initStyle(StageStyle.UNDECORATED)

		SplitPane splitPane = new SplitPane()
		splitPane.setOrientation(Orientation.VERTICAL)
		for(float[][] XY: data) {
			Node chart = createLineChart(XY)
			splitPane.getItems().add(chart)
		}
        
        VBox vbox = new VBox()
        vbox.setPrefWidth(width)
        vbox.setPrefHeight(height)
        vbox.setAlignment(Pos.CENTER)
        vbox.getChildren().add(splitPane)

        dialogPane.setContent(vbox)
        ButtonType closeBtn = new ButtonType("Close", ButtonData.CANCEL_CLOSE)
        dialogPane.getButtonTypes().addAll(closeBtn)
        Optional<ButtonType> result = dialog.showAndWait()
    }

	public static void showDialogFx(Node node, int width=700, int height=500) {
        Dialog<ButtonType> dialog = new Dialog<>()
        DialogPane dialogPane = dialog.getDialogPane()
        dialog.initStyle(StageStyle.UNDECORATED)
        
        VBox vbox = new VBox()
        vbox.setPrefWidth(width)
        vbox.setPrefHeight(height)
        vbox.setAlignment(Pos.CENTER)
        vbox.getChildren().add(node)

        dialogPane.setContent(vbox)
        ButtonType closeBtn = new ButtonType("Close", ButtonData.CANCEL_CLOSE)
        dialogPane.getButtonTypes().addAll(closeBtn)
        Optional<ButtonType> result = dialog.showAndWait()
    }

}
