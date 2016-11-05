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

package io.github.mcolletta.mircomp.demos

import java.io.IOException

import javafx.application.Platform
import javafx.application.Application

import javafx.scene.Scene
import javafx.scene.layout.VBox

import javafx.scene.control.SplitPane
import javafx.scene.control.Button

import javafx.stage.Stage

import javafx.geometry.Orientation

import javafx.event.ActionEvent
import javafx.event.EventHandler

import io.github.mcolletta.mirtext.TextEditor
import io.github.mcolletta.mirscore.ScoreViewer

import io.github.mcolletta.mircomp.converter.ZongConverter
import io.github.mcolletta.mircomp.mirchord.ScoreBuilder
import io.github.mcolletta.mircomp.mirchord.Score as MirScore

import com.xenoage.zong.core.Score

import org.codehaus.groovy.control.customizers.ImportCustomizer
import org.codehaus.groovy.control.CompilerConfiguration


public class SimpleNotationEditor extends Application {

    SplitPane splitPane
    TextEditor editor
    ScoreViewer viewer

    @Override
    public void start(Stage stage) throws Exception {
        VBox root = new VBox()

        splitPane = new SplitPane()

        editor = new TextEditor()
        viewer = new ScoreViewer()

        Button convertBtn = new Button("Convert")
        convertBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                updateScore()
            }
        })

        /*root.getChildren().add(editor)
        root.getChildren().add(viewer)*/
        splitPane.setOrientation(Orientation.HORIZONTAL)
        splitPane.getItems().add(editor)
        splitPane.getItems().add(viewer)
        root.getChildren().add(splitPane)
        root.getChildren().add(convertBtn)
        
        Scene scene = new Scene(root, 1000, 700)

        stage.setScene(scene)
        stage.show()
    }

    public static void main(String[] args) throws IOException {
        Application.launch(SimpleNotationEditor.class, args)
        Platform.exit()
        println "stopped"
        System.exit(0)
    }

    public void updateScore() {
        Score score = createScore(editor.getValue())
        viewer.loadScore(score)
    }

    public Score createScore(String source) {
        ZongConverter zconverter = new ZongConverter()
        // Add imports for script.
        def importCustomizer = new ImportCustomizer()
        importCustomizer.addStaticStars 'com.xenoage.utils.math.Fraction'
        importCustomizer.addImports 'com.xenoage.utils.math.Fraction'
        importCustomizer.addStaticStars 'io.github.mcolletta.mircomp.utils.Utils'
        importCustomizer.addStarImports 'io.github.mcolletta.mircomp.mirchord'
        def configuration = new CompilerConfiguration()
        configuration.addCompilationCustomizers(importCustomizer)
        def binding = new Binding()
        binding.setProperty('builder', new ScoreBuilder()) 
        def mirscore = new GroovyShell(binding, configuration).evaluate(source)
        Score score =zconverter.convert((MirScore)mirscore)
        return score
    }

}
