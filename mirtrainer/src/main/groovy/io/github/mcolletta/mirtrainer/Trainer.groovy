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

package io.github.mcolletta.mirtrainer

import javafx.application.Platform
import javafx.scene.layout.VBox
import javafx.scene.layout.HBox
import javafx.scene.control.Button
import javafx.scene.control.Separator
import javafx.scene.control.TextField
import javafx.scene.control.Label
import javafx.scene.paint.Color

import javafx.event.EventHandler
import javafx.scene.input.KeyEvent
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCombination

import java.nio.file.Path

import io.github.mcolletta.mirsynth.SynthManager
import io.github.mcolletta.mirsynth.SimpleMidiPlayer

import io.github.mcolletta.mirutils.TabContent


final class EarTrainer extends VBox implements TabContent {

    SimpleMidiPlayer player
    int baseKey = 60
    List<Integer> major = [0, 2, 4, 5, 7, 9, 11]
    List<Integer> scale = []

    List<Integer> durations = [500, 1000, 2000]

    int lastKey = -1
    int trials = 0
    int success = 0

    Random random = new Random()

    Label score
    Label answer

    public EarTrainer(SimpleMidiPlayer player) {
        this.player = player
        for(int i: major) {
            scale << (baseKey + i)
        }
        loadControl()        
    }

    public loadControl() {
        var content = new VBox()
        content.setMaxWidth(Double.MAX_VALUE)
        content.setMaxHeight(Double.MAX_VALUE)

        var newTonicButton = new Button("New Tonic");
        newTonicButton.setOnAction( {e -> setTonic()} )

        var replayTonicButton = new Button("Play Tonic");
        replayTonicButton.setOnAction( {e -> playTonic()} )

        var playMelodyButton = new Button("Melody");
        playMelodyButton.setOnAction( {e -> playMelody(12)} )

        var playScaleButton = new Button("Scale");
        playScaleButton.setOnAction( {e -> playScale()} )

        var playButton = new Button("Play");
        //playButton.getStyleClass().add("play-button");
        playButton.setOnAction( {e -> play()} )
        // playButton.mnemonicParsing = false

        var replayButton = new Button("Replay");
        replayButton.setOnAction( {e -> replay()} )

        var stopButton = new Button("Stop");
        stopButton.setOnAction( {e -> stop()} )

        content.getChildren().add(newTonicButton)
        content.getChildren().add(replayTonicButton)
        content.getChildren().add(playMelodyButton)
        content.getChildren().add(playScaleButton)
        content.getChildren().add(playButton)
        content.getChildren().add(replayButton)
        content.getChildren().add(stopButton)


        var numbers = new HBox()
        numbers.setMaxWidth(Double.MAX_VALUE)
        for(int i = 1; i <= 7; i++) {
            var btn = new Button(i.toString())
            var n = major[i-1]
            btn.setOnAction( {e -> check(n)} )
            numbers.getChildren().add(btn)
        }

        content.getChildren().add(numbers)

        var progress = new HBox()
        score = new Label("0/0")
        progress.getChildren().add(score)
        answer = new Label()
        progress.getChildren().add(answer)
        
        content.getChildren().add(progress)


        newTonicButton.requestFocus()
        content.addEventHandler(KeyEvent.KEY_PRESSED, { evt -> 
            if (evt.getCode().isDigitKey()) {
                var i = Integer.parseInt(evt.getText())
                var n = major[i-1]
                check(n)
            } else {
                play()
            }
            evt.consume()
        })

        this.getChildren().add(content)
    }

    void setTonic() {
        lastKey = -1
        trials = 0
        success = 0
        var max = 72
        var min = 60
        baseKey = random.nextInt(max - min + 1) + min
        scale = []
        for(int i: major) {
            scale << (baseKey + i)
        }
        player.playChord([baseKey], 90, 0, 3000)
    }

    void playTonic() {
        player.playChord([baseKey], 90, 0, 3000)
    }

    void playScale() {
        for (int p: scale) {
            player.playChord([p], 90, 0, 1000)
        }
    }

    void playMelody(int num) {
        setTonic()
        for(int i=0; i < num; i++) {
            int key = scale[random.nextInt(scale.size())]
            int dur = durations[random.nextInt(durations.size())]
            player.playChord([key], 90, 0, dur)
        }
    }

    void play() {
        if (lastKey > 0) {
            player.stopChord([lastKey], 0)
        }
        int key = scale[random.nextInt(scale.size())]
        player.playChord([key], 90, 0, 1000)
        lastKey = key
    }

    void replay() {
        player.playChord([lastKey], 90, 0, 3000)
    }

    void stop() {
        if (lastKey > 0) {
            player.stopChord([lastKey], 0)
        }
    }

    def check(i) {
        trials += 1
        if (lastKey > 0) {
            var d = lastKey - baseKey
            // println("${d} - ${i}")
            if (d == i) {
                success += 1
                answer.setTextFill(Color.color(0, 1, 0))
                answer.setText("Correct!")
            } else {
                answer.setTextFill(Color.color(1, 0, 0))
                answer.setText("Wrong!")
            }
        }
        score.setText("${success}/${trials}")
    }

    String getTabType() { return "MirTrainer"; }

    // boolean isClean() { return true; }

    // Path getFilePath() { return null; }

    // void setFilePath(Path path) { }

    // void close() { }
}

