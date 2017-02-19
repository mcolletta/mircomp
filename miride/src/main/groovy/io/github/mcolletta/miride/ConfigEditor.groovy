/*
 * Copyright (C) 2016-2017 Mirco Colletta
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

import javafx.stage.Stage
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.FileChooser.ExtensionFilter

import javafx.scene.layout.VBox
import javafx.scene.layout.HBox
import javafx.scene.control.Button
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.control.Label

import javafx.beans.binding.BooleanBinding

import javafx.event.ActionEvent
import javafx.event.EventHandler

import javax.sound.midi.MidiSystem

//import groovy.xml.MarkupBuilder
import groovy.xml.XmlUtil
import groovy.xml.StreamingMarkupBuilder 

import static io.github.mcolletta.miride.Utils.*


class ConfigEditor extends VBox {

    Path projectPath
	Map<String,Path> config = [:]

    ConfigEditor(Path prjPath, Map<String,Path> configuration) {
        projectPath = prjPath
    	config = configuration

        setSpacing(10)
        setPrefSize(400, 250)

        for (String item : ["lib"]) {
        	this.getChildren().add(createFieldNode(item, "Folder path for $item:", true))
        }
    	this.getChildren().add(createFieldNode("soundbank", "Soundbank path", false))

        Button saveConfigBtn = new Button("Apply")
        saveConfigBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
            	// SAVE
            	ConfigurationManager.write(config, projectPath.resolve("config.xml"))
            }
        })
        this.getChildren().add(saveConfigBtn)
    }

    VBox createFieldNode(String name, String title, boolean isFolder) {
    	Path path = config[name]
    	HBox hbox = new HBox(5)    	    	
    	TextField txtField = new TextField()
    	txtField.setPrefWidth(400)
        if (path != null)
    	   txtField.setText(projectPath.relativize(path).toString())
    	Button btn = new Button()
    	btn.getStyleClass().add("editfield-button")
        btn.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                Path newPath
                if (isFolder)
                	newPath = dirChooserDialog("Choose Folder")
                else
                	newPath = fileChooserDialog("Choose File")
                if (newPath != null) {
                    println newPath
                    txtField.setText(projectPath.relativize(newPath).toString())
                    config[name] = newPath
                    println "config[name]=" + config[name]
                }
            }
        })
        hbox.getChildren().add(txtField)
        hbox.getChildren().add(btn)
        VBox vbox = new VBox(5)
        Label label = new Label(title)
        vbox.getChildren().add(label)
        vbox.getChildren().add(hbox)
        return vbox
    }

    Path dirChooserDialog(String title) {
    	DirectoryChooser dirChooser = new DirectoryChooser()
        dirChooser.setTitle(title)
        dirChooser.setInitialDirectory(
            new File(System.getProperty("user.home"))
        )
        Stage stage = (Stage) getScene().getWindow()
        File selectedFolder = dirChooser.showDialog(stage)
        if (selectedFolder != null) {
            return selectedFolder.toPath()
        }
        return null
    }

    Path fileChooserDialog(String title) {
    	FileChooser fileChooser = new FileChooser()
        fileChooser.setTitle("Open File")
        fileChooser.getExtensionFilters().addAll(
             new ExtensionFilter("Soundfont Files", "*.sf2"),
             new ExtensionFilter("All Files", "*.*"))
        fileChooser.setInitialDirectory(
            new File(System.getProperty("user.home"))
        )
        Stage stage = (Stage)getScene().getWindow()
        File selectedFile = fileChooser.showOpenDialog(stage)
        if (selectedFile != null) {
            return selectedFile.toPath()
        }
        return null
    }
}


