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

package io.github.mcolletta.mircomposer

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

import groovy.transform.CompileStatic

@CompileStatic
public class UtilsFx {

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
