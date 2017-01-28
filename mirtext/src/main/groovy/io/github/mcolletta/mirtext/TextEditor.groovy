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

import java.util.Map

import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths

import javafx.application.Platform
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
import javafx.scene.input.Dragboard
import javafx.scene.input.DragEvent
import javafx.scene.input.TransferMode

import javafx.beans.value.ChangeListener

import javafx.scene.control.ComboBox
import javafx.beans.value.ObservableValue
import javafx.event.ActionEvent
import javafx.event.EventHandler

import javafx.scene.layout.VBox

import org.w3c.dom.Document
import org.w3c.dom.Element
import netscape.javascript.JSObject

import groovy.transform.CompileStatic

//import com.xenoage.utils.jse.io.JseStreamUtils
//import groovy.text.GStringTemplateEngine


@CompileStatic
class TextEditor extends VBox {

    Path filePath
    String suggestedOpenSaveFolder = System.getProperty("user.home")
    String suggestedOpenSaveFileName = "newfile.txt"

	@FXML private WebView editor
	WebEngine engine
	JSObject jsEditor
    JSObject jsEditorSession
    JSObject jsUndoManager

    Map<String, String> pendingEditorCalls = [:]
    Map<String, String> pendingEditorSessionCalls = [:]
    Map<String, String> pendingUndoManagerCalls = [:]

	@FXML private ComboBox selectFontSize
	@FXML private ComboBox selectTheme
	@FXML private ComboBox selectMode

    @FXML private Button filesaveButton
	@FXML private Button undoButton
	@FXML private Button redoButton

	public TextEditor(Path path=null) {
		loadControl()

		engine = editor.getEngine()
        engine.setJavaScriptEnabled(true)
        engine.getLoadWorker().stateProperty().addListener({observable, oldValue, newValue -> 
            if (newValue == Worker.State.SUCCEEDED) {
                if (engine.getDocument() != null)
                    initializeHTML()
            }
		} as ChangeListener)

		editor.setContextMenuEnabled(false)
        
        /*def binding = ["ace_js": getClass().getResource("resources/ace/third-party/ace.js"), 
                       "language_tools_js": getClass().getResource("resources/ace/third-party/ext-language_tools.js")]
        String text = JseStreamUtils.readToString(getClass().getResourceAsStream("resources/ace/editor.template"))
        def templateEngine = new groovy.text.GStringTemplateEngine()
        def template = templateEngine.createTemplate(text).make(binding)
        String html = template.toString()
        engine.loadContent(html)*/

        Platform.runLater( {
            try {
                engine.load(getClass().getResource("resources/ace/editor.html").toExternalForm())
            } catch(Exception ex) {
                println "Exception: " + ex.getMessage()
            }
        } )

        // HACK for BUG https://bugs.openjdk.java.net/browse/JDK-8157413        
        def timer = new Timer()
        def task = timer.runAfter(10000) {
            Platform.runLater( {
                try {
                    ensureLoadedDOM()
                } catch(Exception ex) {
                    println "Exception: " + ex.getMessage()
                }
            } )
        }
        //-------------        
        

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

        editor.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                ensureLoadedDOM()
            }
        })

        this.filePath = path
        if (filePath != null) {
            filesaveButton.setDisable(true)
            setValue(filePath.getText())
        }
        else {
            filesaveButton.setDisable(false)
        }
              
    }

    public ensureLoadedDOM() {        
        if (jsEditor == null)
            initializeHTML()
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
        //Document document = engine.getDocument()
        //Element element = document.getElementById("editor")
        engine.executeScript("initEditor()")
		jsEditor = (JSObject)engine.executeScript("editor")
        jsEditorSession = (JSObject)jsEditor.call("getSession")
        jsUndoManager = (JSObject)jsEditorSession.call("getUndoManager")
        for(Map.Entry<String, String> e : pendingEditorCalls.entrySet()) {
            jsEditor.call(e.getKey(), e.getValue())
        }
        for(Map.Entry<String, String> e : pendingEditorSessionCalls.entrySet()) {
            jsEditorSession.call(e.getKey(), e.getValue())
        }
        for(Map.Entry<String, String> e : pendingUndoManagerCalls.entrySet()) {
            jsUndoManager.call(e.getKey(), e.getValue())
        }
        pendingEditorCalls = [:]
        pendingEditorSessionCalls = [:]
        pendingUndoManagerCalls = [:]        
        registerUpCallEvents()
        HandleUndoRedoButtons()

        // Manage drop from dragging file
        editor.setOnDragOver(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event)
            {
                Dragboard dragboard = event.getDragboard()
                if (dragboard.hasFiles()) {
                    event.acceptTransferModes(TransferMode.COPY)
                }
                event.consume()
            }
        })
        editor.setOnDragDropped(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event)
            {
                Dragboard dragboard = event.getDragboard()
                boolean success = false
                if (dragboard.hasFiles()) {
                    File file = dragboard.getFiles()[0]
                    String txt = file.getText()
                    setValue(txt)
                    success = true
                }
                event.setDropCompleted(success)
                event.consume()
            }
        })
	}

    public registerCopyPasteEvents() {
        final KeyCombination keyCombinationCopy = new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN)
        final KeyCombination keyCombinationPaste = new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_DOWN)
        this.addEventFilter(KeyEvent.KEY_PRESSED, { KeyEvent evt ->
            if (keyCombinationCopy.match(evt)) {
                copy()
            }
            if (keyCombinationPaste.match(evt)) {
                paste()
            }
        })
    }

    public registerUpCallEvents() {
        jsEditor.setMember("jsToJavaEventHandler", this)
        engine.executeScript("editor.on('input', function() { editor.jsToJavaEventHandler.onInput(); });")
    }

    public void onInput() {
        HandleUndoRedoButtons()
    }

    private void HandleUndoRedoButtons() {
        filesaveButton.setDisable(isClean())
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
                    setModeSafe(mode)
                }
            }
        })
	}

    Mode getMode() {
        return (Mode)selectMode.getSelectionModel().getSelectedItem()
    }

    void setMode(Mode mode) {
        selectMode.setValue(mode)
    }

    void setModeSafe(Mode mode) {
        if (jsEditorSession != null) {
            jsEditorSession.call("setMode", mode.path)
        } else {
            if (!pendingEditorSessionCalls.containsKey("setMode"))
                pendingEditorSessionCalls.put("setMode", mode.path)
        }
    }

    String getValue() {
        return (String)engine.executeScript("getValue()")
    }

    void setValue(String content) {
        // https://github.com/ajaxorg/ace/issues/1243
        if (jsEditorSession != null) {
            jsEditorSession.call("setValue", content)
        } else {
            if (!pendingEditorSessionCalls.containsKey("setValue"))
                pendingEditorSessionCalls.put("setValue", content)
        }
        undoReset()
        markClean()
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
            new File(suggestedOpenSaveFolder)
        )
        Stage stage = (Stage)getScene().getWindow()
        File selectedFile = fileChooser.showOpenDialog(stage)
        if (selectedFile != null) {
            String fileContent = selectedFile.getText('UTF-8') // or .text
            setValue(fileContent)
            String filename = selectedFile.getName() 
            String fileExt = filename[filename.lastIndexOf('.')..-1]
            Mode fileMode
            switch (fileExt) {
                case ".mirchord":
                    fileMode = Mode.MirChord
                    break
                case '.groovy':
                    fileMode = Mode.Groovy
                    break
                case ".java":
                    fileMode = Mode.Java
                case ".xml":
                    fileMode = Mode.XML
                    break
                case ".json":
                    fileMode = Mode.JSON
                    break
                default:
                    fileMode = Mode.Text
                    break
            }
            jsEditorSession.call("setMode", fileMode)  
        }
    }

    void filesave() {
        if (filePath != null) {
            File file = filePath.toFile()
            try {
                file.text = getValue()
                markClean()
                HandleUndoRedoButtons()
            } catch (IOException ex) {
                println(ex.getMessage())
            }
        } else {
            filesaveas()
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
        fileChooser.setInitialDirectory(
            new File(suggestedOpenSaveFolder)
        )
        fileChooser.setInitialFileName(suggestedOpenSaveFileName)
        Stage stage = (Stage)getScene().getWindow()
        File file = fileChooser.showSaveDialog(stage)
        if (file != null) {
            try {
                file.text = getValue()
                filePath = file.toPath()
                markClean()
                HandleUndoRedoButtons()
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

    void undoReset() {
        if (jsUndoManager != null) {
            // engine.executeScript("reset()")
            jsUndoManager.call("reset", null)
        }
        else
            pendingUndoManagerCalls.put("reset", null)
    }

    void undo() {
        engine.executeScript("undo(false)")
        HandleUndoRedoButtons()
    }

    void redo() {
        engine.executeScript("redo(false)")
        HandleUndoRedoButtons()
    }

    void markClean() {
        if (jsUndoManager != null) {
            //engine.executeScript("markClean()")
            jsUndoManager.call("markClean", null)
        }
        else
            pendingUndoManagerCalls.put("markClean", null)
    }

    boolean isClean() {
        return engine.executeScript("isClean()") as boolean
    }    

}