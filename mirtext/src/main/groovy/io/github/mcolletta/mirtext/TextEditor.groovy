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

package io.github.mcolletta.mirtext

import java.io.IOException
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.layout.StackPane
import javafx.scene.web.WebEngine
import javafx.scene.web.WebView
import javafx.stage.Stage
import javafx.stage.FileChooser
import javafx.stage.FileChooser.ExtensionFilter

import javafx.collections.FXCollections
import javafx.concurrent.Worker
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.control.TextArea
import javafx.scene.control.Button
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.input.DataFormat
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import javafx.scene.input.KeyEvent

import javafx.beans.value.ChangeListener

import javafx.scene.control.ComboBox
import javafx.beans.value.ObservableValue
import javafx.event.ActionEvent
import javafx.event.EventHandler


import javafx.scene.layout.VBox

import org.w3c.dom.Document
import org.w3c.dom.Element
import netscape.javascript.JSObject



class TextEditor extends VBox {

	@FXML private WebView editor
	WebEngine engine
	JSObject jsEditor

	@FXML private ComboBox selectFontSize
	@FXML private ComboBox selectTheme
	@FXML private ComboBox selectMode

	@FXML private Button undoButton
	@FXML private Button redoButton

	public TextEditor() {

		loadControl()

		engine = editor.getEngine()
        engine.setJavaScriptEnabled(true)
        engine.getLoadWorker().stateProperty().addListener({observable, oldValue, newValue -> 
            if (newValue == Worker.State.SUCCEEDED) {
                initializeHTML()
            }
		} as ChangeListener)

		editor.setContextMenuEnabled(false)
		engine.load(getClass().getResource("resources/ace/editor.html").toExternalForm())

		selectFontSize.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observableValue, Object oldValue, Object newValue) {
                if (newValue != null) {
                    engine.executeScript('setFontSize("' + newValue + '")');
                    
                }
            }
        })

        loadThemes()
        loadModes()

		registerCopyPasteEvents()
              
    }

    private void loadControl() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(getClass().getSimpleName() + ".fxml"))

        fxmlLoader.setRoot(this)
        fxmlLoader.setController(this)

        try {
            fxmlLoader.load()
        } catch (IOException exception) {
            throw new RuntimeException(exception)
        }
    }

    private void initializeHTML() {
        Document document = engine.getDocument()
        Element element = document.getElementById("editor")
        engine.executeScript("initEditor()")
		jsEditor = (JSObject)engine.executeScript("editor")
	}

    public registerCopyPasteEvents() {
        final KeyCombination keyCombinationCopy = new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN)
        final KeyCombination keyCombinationPaste = new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_DOWN)
        this.addEventFilter(KeyEvent.KEY_PRESSED, { evt ->
            if (keyCombinationCopy.match(evt)) {
                copy()
            }
            if (keyCombinationPaste.match(evt)) {
                paste()
            }
        })
    }

    private void HandleUndoRedoButtons() {
    	undoButton.setDisable(!hasUndo())
        redoButton.setDisable(!hasRedo())
    }

    private void loadThemes() {
        selectTheme.setItems(FXCollections.observableArrayList(Theme.AVAILABLE_THEMES));
        selectTheme.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                if (newValue != null && oldValue != newValue) {
                    Theme theme = selectTheme.getSelectionModel().getSelectedItem() as Theme
                    engine.executeScript("setTheme('" + theme.path + "')")
                }
            }
        })
    }

    private void loadModes() {
        selectMode.setItems(FXCollections.observableArrayList(Mode.AVAILABLE_MODES));
        selectMode.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                if (newValue != null && oldValue != newValue) {
                    Mode mode = selectMode.getSelectionModel().getSelectedItem() as Mode
                    engine.executeScript("setMode('" + mode.path + "')")
                }
            }
        })
	}

    Mode getMode() {
        return (Mode)selectMode.getSelectionModel().getSelectedItem()
    }

    String getValue() {
        return (String)engine.executeScript("getValue()")
    }

    void setValue(String content) {
        jsEditor.call("setValue", content)
    }

	// actions

    void fileopen() {
        FileChooser fileChooser = new FileChooser()
        fileChooser.setTitle("Open Source Code File")
        fileChooser.getExtensionFilters().addAll(
             new ExtensionFilter("Mirchord Files", "*.mirchord"),
             new ExtensionFilter("Groovy Files", "*.groovy"),
             new ExtensionFilter("XML Files", "*.xml"),
             new ExtensionFilter("JSON Files", "*.json"),
             new ExtensionFilter("All Files", "*.*"))
        fileChooser.setInitialDirectory(
            new File(System.getProperty("user.home"))
        )
        Stage stage = (Stage)getScene().getWindow()
        File selectedFile = fileChooser.showOpenDialog(stage)
        if (selectedFile != null) {
            String fileContent = selectedFile.getText('UTF-8') // or .text
            setValue(fileContent)
            String filename = selectedFile.getName() 
            String fileExt = filename[filename.lastIndexOf('.')..-1]
            // TODO: engine.executeScript("setMode('" + mode.path + "')")
            switch (fileExt) {
                case ".mirchord":
                    engine.executeScript("setMode('ace/mode/mirchord')")
                    break
                case '.groovy':
                    engine.executeScript("setMode('ace/mode/groovy')")
                    break
                case ".java":
                    engine.executeScript("setMode('ace/mode/java')")
                case ".xml":
                    engine.executeScript("setMode('ace/mode/xml')")
                    break
                case ".json":
                    engine.executeScript("setMode('ace/mode/json')")
                    break
                default:
                    engine.executeScript("setMode('ace/mode/text')")
                    break
            }    
        }
    }

    void filesaveas() {
        FileChooser fileChooser = new FileChooser()
        fileChooser.setTitle("Save Source Code as...")        
        fileChooser.getExtensionFilters().addAll(
             new ExtensionFilter("Mirchord Files", "*.mirchord"),
             new ExtensionFilter("Groovy Files", "*.groovy"),
             new ExtensionFilter("XML Files", "*.xml"),
             new ExtensionFilter("JSON Files", "*.json"))
        fileChooser.setInitialFileName("song.mirchord")
        Stage stage = (Stage)getScene().getWindow()
        File file = fileChooser.showSaveDialog(stage)
        if (file != null) {
            try {
                file.text = getValue()
            } catch (IOException ex) {
                println(ex.getMessage())
            }
        }
    }

	void cut() {
		copy()
		engine.executeScript("cut()")
	}

	void copy() {
        String text = (String) engine.executeScript("copy()")
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent()
        content.putString(text)
        clipboard.setContent(content)
    }

    void paste() {
        Clipboard clipboard = Clipboard.getSystemClipboard()
        String content = (String) clipboard.getContent(DataFormat.PLAIN_TEXT);
        if (content != null) {
            // engine.executeScript("paste('" + content +  "')")
            // multiline paste
            jsEditor.call("insert", content)
        }
    }

    void comment() {
        engine.executeScript("toggleBlockComment()")
    }

    void find() {
    	engine.executeScript("find()")
    }

    void replace() {
    	engine.executeScript("replace()")
    }

    boolean hasUndo() {
    	return engine.executeScript("hasUndo()") as boolean
    }

    boolean hasRedo() {
    	return engine.executeScript("hasRedo()") as boolean
    }

    void undo() {
    	engine.executeScript("undo(false)")
    	HandleUndoRedoButtons()
    }

    void redo() {
    	engine.executeScript("redo(false)")
    	HandleUndoRedoButtons()
    }

}