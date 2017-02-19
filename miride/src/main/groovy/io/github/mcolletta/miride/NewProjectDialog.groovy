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

import java.nio.file.Path

import javafx.stage.Stage
import javafx.stage.DirectoryChooser
import javafx.scene.layout.VBox
import javafx.scene.control.Button
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.control.Label

import javafx.beans.binding.BooleanBinding

import javafx.event.ActionEvent
import javafx.event.EventHandler

import groovy.xml.MarkupBuilder

import static io.github.mcolletta.miride.Utils.*


class NewProjectDialog extends VBox {

    TextField projectNameField
    Label projectFolderLabel

    String projectName = ""
    String workspacePath = ""
    File workspaceFolder
    File projectFolder

    NewProjectDialog() {
        setSpacing(10)
        setPrefSize(400, 250)
    	projectFolderLabel = new Label()
    	Button chooseFolderBtn = new Button("Choose Project folder...")
        chooseFolderBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                DirectoryChooser dirChooser = new DirectoryChooser()
                dirChooser.setTitle("Choose Project Folder")
                dirChooser.setInitialDirectory(
                    new File(System.getProperty("user.home"))
                )
                Stage stage = (Stage) getScene().getWindow()
                File selectedFolder = dirChooser.showDialog(stage)
                if (selectedFolder != null) {
                    println selectedFolder
                    workspaceFolder = selectedFolder
            		workspacePath = workspaceFolder.canonicalPath
            		projectFolderLabel.setText(workspacePath)
                }
            }
        })
        this.getChildren().add(chooseFolderBtn)
        this.getChildren().add(projectFolderLabel)
        this.getChildren().add(new Label("Enter project name:"))
        projectNameField = new TextField()
        this.getChildren().add(projectNameField)

        Button createProjectBtn = new Button("Create Project")
        createProjectBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
            	projectName = projectNameField.text
            	createProject()
                Stage stage = (Stage) getScene().getWindow()
                stage.close()
            }
        })
        this.getChildren().add(createProjectBtn)

        BooleanBinding bb = new BooleanBinding() {
            {
                super.bind(projectNameField.textProperty(),
                           projectFolderLabel.textProperty());
            }

            @Override
            protected boolean computeValue() {
                return (projectNameField.getText().isEmpty()
                        || projectFolderLabel.getText().isEmpty())
            }
        }

        createProjectBtn.disableProperty().bind(bb)
    }

    void createProject() {
        projectFolder = makeDir(workspaceFolder, projectName)
        makeDir(projectFolder, 'src')
        makeDir(projectFolder, 'lib')
        makeDir(projectFolder, 'mirchords')
        makeDir(projectFolder, 'xml')
        makeDir(projectFolder, 'midi')
        makeDir(projectFolder, 'soundfonts')

        Path configFilePath = new File(projectFolder, "config.xml").toPath()
        Map<String,Path> config = [lib:projectFolder.toPath().resolve("lib")]
        ConfigurationManager.write(config, configFilePath)
    }
}