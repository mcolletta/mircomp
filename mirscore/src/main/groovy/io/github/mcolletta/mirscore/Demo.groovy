/*
 * Copyright (C) 2016 Mirco Colletta
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

package io.github.mcolletta.mirscore


import java.io.IOException
import javafx.application.Platform
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.layout.StackPane
import javafx.stage.Stage


public class Demo extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        StackPane root = new StackPane()

        ScoreViewer viewer = new ScoreViewer()

        root.getChildren().add(viewer)
        
        Scene scene = new Scene(root, 950, 700)

        stage.setScene(scene)
        stage.show()
    }

    public static void main(String[] args) throws IOException {
        Application.launch(Demo, args)
        Platform.exit()
        println "stopped"
        System.exit(0)
    }

}
