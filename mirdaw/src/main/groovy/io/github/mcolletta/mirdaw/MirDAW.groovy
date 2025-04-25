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


import java.io.IOException
import javafx.application.Platform
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.layout.StackPane
import javafx.stage.Stage


public class MirDAW extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        StackPane root = new StackPane()

        MidiEditor editor = new MidiEditor()

        root.getChildren().add(editor)
        
        Scene scene = new Scene(root, 1000, 700)
        setNordDark(scene)

        stage.setScene(scene)
        stage.show()
    }

    private String nordDarkUrl = getClass().getResource("resources/themes/nord-dark.css").toExternalForm();

    void setNordDark(Scene scene) {
        scene.getStylesheets().add(nordDarkUrl)
    }

    public static void main(String[] args) throws IOException {
        Application.launch(MirDAW, args)
        Platform.exit()
        println "stopped"
        System.exit(0)
    }

}
