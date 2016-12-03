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

import io.github.mcolletta.mirtext.Mode
import io.github.mcolletta.mirtext.TextEditor
import io.github.mcolletta.mirscore.ScoreViewer

import io.github.mcolletta.mirconverter.ZongConverter
import io.github.mcolletta.mirchord.core.Score as MirScore
import io.github.mcolletta.mirchord.interpreter.MirChordInterpreter

import io.github.mcolletta.mirchord.core.ScoreBuilder

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
        //editor.setMode(Mode.MirChord)
        viewer = new ScoreViewer()

        Button convertBtn = new Button("Convert")
        convertBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                updateScore()
            }
        })

        //splitPane.setOrientation(Orientation.HORIZONTAL)
        splitPane.setOrientation(Orientation.VERTICAL)
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
        Score score = null
        Mode mode = editor.getMode()
        String source = editor.getValue()
        if (mode == Mode.MirChord)
            score = createScoreFromMirchord(source)
        if (mode == Mode.Groovy)
            score = createScoreFromGroovyBuilder(source)
        viewer.loadScore(score)
    }

    public Score createScoreFromMirchord(String source) {
        ZongConverter zconverter = new ZongConverter()
        MirChordInterpreter interpreter = new MirChordInterpreter()
        MirScore mirscore = interpreter.evaluate(source)
        Score score = zconverter.convert(mirscore)
        return score
    }

    public Score createScoreFromGroovyBuilder(String source) {
        ZongConverter zconverter = new ZongConverter()
        // Add imports for script.
        def importCustomizer = new ImportCustomizer()
        importCustomizer.addStaticStars 'com.xenoage.utils.math.Fraction'
        importCustomizer.addImports 'com.xenoage.utils.math.Fraction'
        importCustomizer.addStaticStars 'io.github.mcolletta.mirchord.core.Utils'
        importCustomizer.addStaticStars 'io.github.mcolletta.mirconverter.Utils'
        importCustomizer.addStarImports 'io.github.mcolletta.mirchord.core'
        def configuration = new CompilerConfiguration()
        configuration.addCompilationCustomizers(importCustomizer)
        def binding = new Binding()
        binding.setProperty('builder', new ScoreBuilder()) 
        def mirscore = new GroovyShell(binding, configuration).evaluate(source)
        Score score =zconverter.convert((MirScore)mirscore)
        return score
    }

}
