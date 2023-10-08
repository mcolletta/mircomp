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

package io.github.mcolletta.miride

import java.nio.file.Paths
import java.nio.file.Path
import java.nio.file.Files

import javafx.stage.Stage
import javafx.stage.WindowEvent
import javafx.event.EventHandler

import javafx.application.Platform
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.layout.VBox


public class MirIDE extends Application {

	public static Editor editor    

	@Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("Editor.fxml"))
        Parent root = (VBox) fxmlLoader.load()
        editor = (Editor) fxmlLoader.getController()
        stage.setTitle("MirComp IDE")
        Scene scene = new Scene(root, 1000, 700)
        stage.setScene(scene)
        //stage.setFullScreen(true)
        stage.show()

        //theme
        //scene.getStylesheets().add(getClass().getResource("resources/themes/primer-dark.css").toExternalForm());

        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                if (editor.confirmClosing()) {
                    //editor.folderTreeView.stopWatching()
                    editor.close()
                    stop()
                    Platform.exit()
                } else
                    event.consume()
            }
        })
    }

    public static void main(String[] args) throws IOException {
        Application.launch(MirIDE.class, args)
        Platform.exit()
        println "stopped"
        System.exit(0)
    }
}