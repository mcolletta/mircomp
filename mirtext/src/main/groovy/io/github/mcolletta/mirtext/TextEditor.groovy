/*
 * Copyright (C) 2016-2023 Mirco Colletta
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
import java.nio.file.Files

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
import javafx.scene.control.ComboBox
import javafx.scene.control.Label

import javafx.scene.control.ButtonType
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType

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

import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue

import javafx.event.ActionEvent
import javafx.event.EventHandler

import javafx.scene.layout.VBox

import org.w3c.dom.Document
import org.w3c.dom.Element
import netscape.javascript.JSObject

//import groovy.transform.CompileDynamic

//import NioGroovyMethods

//import com.xenoage.utils.jse.io.JseStreamUtils
//import groovy.text.GStringTemplateEngine

import io.github.mcolletta.mirfoldertreeview.FolderTreeListenerList
import io.github.mcolletta.mirfoldertreeview.FolderTreeViewListener
import io.github.mcolletta.mirfoldertreeview.FolderTreeViewEvent
import io.github.mcolletta.mirfoldertreeview.PathRequestType

import io.github.mcolletta.mirutils.TabContent


//@CompileDynamic
class TextEditor extends VBox implements FolderTreeListenerList, TabContent  {

    ObjectProperty<Path> filePath = new SimpleObjectProperty<>()
    Path getFilePath() {
        return filePath.get()
    }
    void setFilePath(Path path) {
        filePath.set(path)
    }

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
    @FXML private Label themeIcon
	@FXML private ComboBox selectTheme
	@FXML private ComboBox selectMode

    @FXML private Button filesaveButton
	@FXML private Button undoButton
	@FXML private Button redoButton

    boolean GUESS_TEXT_FILE = false
    final int MAX_FILE_SIZE = 500000

    String getTabType() { return "TextEditor"; }

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
        // def timer = new Timer()
        // def task = timer.runAfter(10000) {
        //     Platform.runLater( {
        //         try {
        //             ensureLoadedDOM()
        //         } catch(Exception ex) {
        //             println "Exception: " + ex.getMessage()
        //         }
        //     } )
        // }
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

        /*  On Windows 10 cut/paste works without registerCopyPasteEvents, 
            otherwise it paste the text twice!!
            Ex.
            System.getProperty("os.name") ===> Windows 10
            System.getProperty("os.name") ===> Linux
        */
        String os = System.getProperty("os.name");
        if (os.toLowerCase().indexOf("windows") < 0) {
            registerCopyPasteEvents()
        }

        editor.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                ensureLoadedDOM()
            }
        })

        boolean validPath = true
        if (GUESS_TEXT_FILE && path != null)
            validPath = checkFile(path.toFile())
        if (validPath) {
            setFilePath(path)
            if (getFilePath() != null) {
                filesaveButton.setDisable(true)
                setValue(getFilePath().getText())
                markClean()
            }
            else {
                filesaveButton.setDisable(false)
            }
        }

        filePath.addListener(new ChangeListener(){
            @Override public void changed(ObservableValue o,Object oldVal, Object newVal){
                if (newVal != null) {
                    Path newPath = newVal as Path
                    // println "Listener " + getFolderTreeViewListener()
                    fireFolderTreeUpdated(new FolderTreeViewEvent([origin: this,
                                                                   path: newPath,
                                                                   requestType: PathRequestType.MODIFY,
                                                                   fileType: ""]))
                }
            }
        })

        themeIcon.setVisible(false)
        themeIcon.managedProperty().bind(themeIcon.visibleProperty())
        selectTheme.setVisible(false)
        selectTheme.managedProperty().bind(selectTheme.visibleProperty())
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
                    if (checkFile(file)) {
                        String txt = file.getText()
                        jsEditor.call("setValue", txt)
                        //filePath = file.toPath()
                        success = true
                    }
                }
                event.setDropCompleted(success)
                event.consume()
            }
        })
	}

    void showAlert(AlertType atype, String text) {
        Alert alert = new Alert(atype, text)
        Optional<ButtonType> result = alert.showAndWait()
    }

    boolean checkFile(File file) {
        Path path = file.toPath()
        if (Files.size(path) > MAX_FILE_SIZE) {
            showAlert(AlertType.ERROR, "File size exceeds limit")
            return false
        }
        if (GUESS_TEXT_FILE) {
            if (!isTextFile(file)) {
                showAlert(AlertType.ERROR, "Not a text file")
                return false
            }
        }
        return true
    }

    boolean isTextFile(File file) {
        int ascii = 0
        int tot = 0
        for (byte b : file.getBytes()) { // getBytes("UTF-8")
            if ((b >= 0x20  &&  b <= 0x7E) || (b in [0x09, 0x0A, 0x0C, 0x0D])) {
                ascii++
            }
            tot++
        }
        // println (ascii/tot)
        return (ascii/tot) >= 0.9D
    }

    public registerCopyPasteEvents() {
        final KeyCombination keyCombinationCopy = new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN)
        final KeyCombination keyCombinationCut = new KeyCodeCombination(KeyCode.X, KeyCombination.CONTROL_DOWN)
        //final KeyCombination keyCombinationPaste = new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_DOWN)
        final KeyCombination keyCombinationSave = new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN)
        this.addEventFilter(KeyEvent.KEY_PRESSED, { KeyEvent evt ->
            if (keyCombinationCopy.match(evt)) {
                copy()
                evt.consume()
            }
            if (keyCombinationCut.match(evt)) {
                cut()
                evt.consume()
            }
            // if (keyCombinationPaste.match(evt)) {
            //     paste()
            // }
            if (keyCombinationSave.match(evt)) {
                filesave()
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
                    //engine.executeScript("setTheme('" + theme.path + "')")
                    setThemeSafe(theme)
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

    void setTheme(Theme theme) {
        selectTheme.setValue(theme)
    }

    void setThemeSafe(Theme theme) {
        if (jsEditor != null) {
            jsEditor.call("setTheme", theme.path)
        } else {
            if (!pendingEditorCalls.containsKey("setTheme"))
                pendingEditorCalls.put("setTheme", theme.path)
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
    }

	// actions

    void fileopen() {
        FileChooser fileChooser = new FileChooser()
        fileChooser.setTitle("Open Source Code File")
        fileChooser.getExtensionFilters().addAll(
            new ExtensionFilter("All Files", "*.*"),
            new ExtensionFilter("Mirchord Files", "*.mirchord"),
            new ExtensionFilter("Groovy Files", "*.groovy"),
            new ExtensionFilter("XML Files", "*.xml"),
            new ExtensionFilter("JSON Files", "*.json"))
        fileChooser.setInitialDirectory(
            new File(suggestedOpenSaveFolder)
        )
        Stage stage = (Stage)getScene().getWindow()
        File selectedFile = fileChooser.showOpenDialog(stage)
        if (selectedFile != null) {
            if (checkFile(selectedFile)) {
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
                setFilePath(selectedFile.toPath())
                markClean()
            }
        }
    }

    void filesave() {
        if (getFilePath() != null) {
            File file = getFilePath().toFile()
            try {
                //file.text = getValue()
                file.setText(getValue())
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
            new ExtensionFilter("All Files", "*.*"),
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
                //file.text = getValue()
                file.setText(getValue())
                setFilePath(file.toPath())
                markClean()
                HandleUndoRedoButtons()
            } catch (IOException ex) {
                println(ex.getMessage())
            }
        }
    }

    public reloadfile() {
        if (getFilePath() != null) {
            setValue(getFilePath().getText())
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