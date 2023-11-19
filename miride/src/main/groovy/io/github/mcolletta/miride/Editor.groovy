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

package io.github.mcolletta.miride

//import java.lang.reflect.Method

import java.security.Policy

import java.io.IOException
import java.nio.file.Paths
import java.nio.file.Path
import java.nio.file.Files
import java.nio.file.LinkOption

import javax.sound.midi.Soundbank
import javax.sound.midi.MidiSystem
import javax.sound.midi.Synthesizer

import javafx.application.Platform
import javafx.application.Application

import javafx.scene.Scene
import javafx.scene.Node
import javafx.scene.layout.VBox
import javafx.scene.layout.StackPane
import javafx.scene.layout.Region

import javafx.scene.control.SplitPane
import javafx.scene.control.TabPane
import javafx.scene.control.Tab
import javafx.scene.control.Button
import javafx.scene.control.ToggleButton
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.control.Label
import javafx.scene.control.MenuItem
import javafx.scene.control.CheckMenuItem

import javafx.scene.control.Dialog
import javafx.scene.control.DialogPane
import javafx.scene.control.ButtonType
import javafx.scene.control.ButtonBar.ButtonData
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType

import javafx.stage.Stage
import javafx.scene.Cursor
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.FileChooser.ExtensionFilter
import javafx.stage.StageStyle

import javafx.geometry.Orientation

import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.event.Event

import javafx.geometry.Pos

import javafx.fxml.FXML
import javafx.fxml.FXMLLoader

import javafx.beans.value.ObservableValue
import javafx.beans.value.ChangeListener
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.binding.Bindings
import javafx.beans.binding.BooleanBinding

import io.github.mcolletta.mirtext.Mode
import io.github.mcolletta.mirtext.TextEditor
import io.github.mcolletta.mirscore.ScoreViewer
import io.github.mcolletta.mirmidi.MidiEditor
import io.github.mcolletta.mirfoldertreeview.FolderTreeView
import io.github.mcolletta.mirfoldertreeview.FolderTreeViewListener
import io.github.mcolletta.mirfoldertreeview.FolderTreeViewEvent
import io.github.mcolletta.mirfoldertreeview.PathRequestType

import io.github.mcolletta.mirconverter.ZongConverter
import io.github.mcolletta.mirchord.core.Score as MirScore
import io.github.mcolletta.mirchord.interpreter.MirChordInterpreter
import io.github.mcolletta.mirchord.interpreter.GroovyScriptInterpreter

import io.github.mcolletta.mirchord.core.ScoreBuilder

import io.github.mcolletta.mirsynth.SynthManager
import io.github.mcolletta.mirsynth.SimpleMidiPlayer

import io.github.mcolletta.mirutils.TabContent

import com.xenoage.zong.core.Score
import com.xenoage.zong.desktop.utils.JseZongPlatformUtils
import com.xenoage.utils.exceptions.InvalidFormatException

import static io.github.mcolletta.miride.Utils.getFileExt
import static io.github.mcolletta.miride.DraggableTabs.*

import groovy.console.ui.SystemOutputInterceptor

import groovy.transform.CompileDynamic


public class Editor implements FolderTreeViewListener {

    private SynthManager synthManager
    public SimpleMidiPlayer midiPlayer

    private Map<Tab,Path> openedTabs = [:]
    private Map<Tab,Path> tabsFileRemoved = [:]

    private ProjectInterpreter interpreter

    @FXML private CheckMenuItem treeMenu
    @FXML private CheckMenuItem consoleMenu
    @FXML private CheckMenuItem typecheckMenu
    @FXML private MenuItem runMenu
    @FXML private MenuItem stopMenu

    @FXML private ToggleButton treeButton
    @FXML private ToggleButton consoleButton
    @FXML private ToggleButton typecheckButton

    @FXML private MenuItem propertiesMenu

    @FXML private Button propertiesButton
    @FXML private Button runButton
    @FXML private Button stopButton
    
    @FXML private SplitPane splitPane
    @FXML private StackPane tabPaneContainer
    @FXML protected FolderTreeView folderTreeView
    @FXML private TabPane tabPane

    @FXML private TabPane tabConsole
    @FXML private TextArea outputConsole
    @FXML private TextArea errorConsole

    private volatile Thread  runThread = null
    private SystemOutputInterceptor systemOutInterceptor
    private SystemOutputInterceptor systemErrorInterceptor

    private ObjectProperty<File> projectFolder = new SimpleObjectProperty<>()

    private Map<String,Path> config = [:]

    private boolean needAgreement = true
    

    @FXML public void initialize() {
        if (needAgreement)
            showLicenseAgreementDialog()
        tabConsole.managedProperty().bind(tabConsole.visibleProperty())
        tabConsole.setVisible(false)
        folderTreeView.addFolderTreeViewListener(this)
        treeMenu.selectedProperty().bindBidirectional(treeButton.selectedProperty())
        consoleMenu.selectedProperty().bindBidirectional(consoleButton.selectedProperty())
        typecheckMenu.selectedProperty().bindBidirectional(typecheckButton.selectedProperty())
        BooleanBinding fileExists = new BooleanBinding() {
            {
                super.bind(projectFolder)
            }

            @Override
            protected boolean computeValue() {
                return (projectFolder.get() != null && projectFolder.get().exists())
            }
        }
        propertiesButton.disableProperty().bind(fileExists.not())
        propertiesMenu.disableProperty().bind(fileExists.not())
        showtree()

        runMenu.disableProperty().bind(runButton.disableProperty())
        stopMenu.disableProperty().bind(stopButton.disableProperty())
        tabPane.getSelectionModel().selectedItemProperty().addListener(
            new ChangeListener<Tab>() {
                @Override
                public void changed(ObservableValue<? extends Tab> ov, Tab t, Tab tab) {
                    runButton.setDisable(true)
                    if (tab != null) {
                        TabContent tabContent = (TabContent) tab.getContent()
                        Mode mode = null
                        if (tabContent.getTabType() == "TextEditor")
                            mode = ((TextEditor)tabContent).getMode()
                        if (tabContent.getTabType() == "MirChordEditor")
                            mode = ((MirChordEditor)tabContent).getMode()
                        if (mode != null && mode in [Mode.MirChord, Mode.Groovy])
                            runButton.setDisable(false)
                    }
                }
            }
        )

        initMidi()
        interpreter = new ProjectInterpreter(null, typecheckButton.selected, new Binding(["MidiPlayer":midiPlayer]))
    }

    void showLicenseAgreementDialog() {
        Dialog<ButtonType> dialog = new Dialog<>()
        DialogPane dialogPane = dialog.getDialogPane()
        dialog.initStyle(StageStyle.UNDECORATED)
        
        VBox vbox = new VBox()
        vbox.setPrefWidth(700)
        vbox.setPrefHeight(500)
        vbox.setAlignment(Pos.CENTER)
        Label label = new Label()
        /*Path infoPath = Paths.get( getClass().getResource("resources/info.txt").getPath() )
        label.setText(infoPath.toFile().getText())*/
        label.setText(getClass().getResourceAsStream("resources/info.txt").getText())
        vbox.getChildren().add(label)

        dialogPane.setContent(vbox)
        ButtonType agreeBtn = new ButtonType("I agree, open MirIDE", ButtonData.OK_DONE)
        ButtonType exitBtn = new ButtonType("Quit", ButtonData.CANCEL_CLOSE)
        dialogPane.getButtonTypes().addAll(agreeBtn, exitBtn)
        dialog.getDialogPane().lookupButton(agreeBtn)
        Optional<ButtonType> result = dialog.showAndWait()
        if (!(result.isPresent() && result.get() == agreeBtn)) { 
            onExit()
        }
    }

    void showAlert(AlertType atype, String text) {
        Alert alert = new Alert(atype, text)
        Optional<ButtonType> result = alert.showAndWait()
    }

    void initMidi() {
        String appName = "MirIDE"
        JseZongPlatformUtils.init(appName)
        synthManager = new SynthManager()
        midiPlayer = new SimpleMidiPlayer(synthManager.getSynthesizer())

    }

    void loadSoundbank() {
        if (config.containsKey("soundbank") && config["soundbank"] != null) {
            File sbk = config["soundbank"].toFile()
            loadSoundbank(sbk)
        }
    }

    void loadSoundbank(File file) throws InvalidFormatException {
        try {
            FileInputStream fis = new FileInputStream(file)
            Soundbank newSB
            try {
                newSB = MidiSystem.getSoundbank(new BufferedInputStream(fis))
            } finally {
                fis.close()
            }
            Soundbank soundbank = synthManager.getSoundbank()
            Synthesizer synthesizer = synthManager.getSynthesizer()
            if (soundbank != null)
                synthesizer.unloadAllInstruments(soundbank)
            soundbank = newSB
            synthesizer.loadAllInstruments(soundbank)
        } catch (Exception ex) {
            throw new InvalidFormatException("Invalid soundbank: " + file, ex)
        }
    }

    Scene getScene() {
        return splitPane.getScene()
    }

    void startWait() {
        getScene().setCursor(Cursor.WAIT)
    }

    void endWait() {
        getScene().setCursor(Cursor.DEFAULT)
    }

    boolean canCloseTab(Tab tab) {
        boolean clean = false

        if (openedTabs[tab] != null) {
            TabContent tabContent = (TabContent) tab.getContent()
            clean = tabContent.isClean()
        } 

        if (!clean) {
            Dialog<ButtonType> dialog = new Dialog<>()
            dialog.setContentText("Content not saved. Close anyway?")
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL)
            dialog.getDialogPane().lookupButton(ButtonType.OK)
            Optional<ButtonType> result = dialog.showAndWait()
            if (result.isPresent() && result.get() == ButtonType.OK) {
                return false
            }
        }

        return !clean
    }

    void removeTabHandler(Tab tab) {
        openedTabs.remove(tab)
        if (tabPane.getTabs().size() == 0) {
            tabPaneContainer.getStyleClass().add("background-logo")
        }
    }

    void addTab(Tab tab) {
        if (tabPane.getTabs().size() == 0) {
            tabPaneContainer.getStyleClass().clear()
            tabPaneContainer.setStyle(null)
        }

        tabPane.getTabs().add(tab)
        tab.setOnCloseRequest(new EventHandler<Event>() {
            @Override
            public void handle(Event e) 
            {
                if (canCloseTab(tab))
                    e.consume()
            }
        })
        tab.setOnClosed(new EventHandler<Event>() {
            @Override public void handle(Event e) {
                // println e.getTarget()
                TabContent tabContent = (TabContent) tab.getContent()
                tabContent.close()
                removeTabHandler(tab)
                if (tabsFileRemoved.containsKey(tab))
                    tabsFileRemoved.remove(tab)
            }
        })
    }

    void updateOpenedTabs(FolderTreeViewEvent evt) {
        Path changedPath = evt.path
        for(Map.Entry<Tab,Path> e : openedTabs) {
            Tab tab = e.getKey()
            Path path = e.getValue()
            TabContent tabContent = (TabContent) tab.getContent()

            // println "path=$path    changedPath=$changedPath    ${evt.requestType}"

            if (path == changedPath) {
                if (evt.requestType == PathRequestType.DELETE) {
                    tabsFileRemoved[tab] = path
                    Platform.runLater( {
                        setTabLabelText(tab, "untitled")
                    })
                    tabContent.setFilePath(null)
                    e.setValue(null)
                }
                if (evt.requestType == PathRequestType.MODIFY) {
                    String title = changedPath.getFileName().toString()
                    if (tab.getText() != title) {
                        Platform.runLater( {
                            setTabLabelText(tab, title)
                        })
                    }
                }
            }

            if (path == null) {
                if (evt.requestType == PathRequestType.NEW 
                    && tabsFileRemoved.containsKey(tab)) {
                    if (changedPath == tabsFileRemoved[tab]) {
                       tabContent.setFilePath(changedPath)
                        Platform.runLater( {
                            setTabLabelText(tab, changedPath.getFileName().toString())
                        })  
                        tabsFileRemoved.remove(tab)
                        e.setValue(changedPath)
                    }
                } else {
                    Path filePath = tabContent.getFilePath()
                    if (filePath != null && filePath == changedPath) {
                        Platform.runLater( {
                            setTabLabelText(tab, filePath.getFileName().toString())
                        })                    
                        openedTabs[tab] = filePath
                    }
                }
            }
        }
    }

    void folderTreeUpdated(FolderTreeViewEvent evt) {
        // println "folderTreeUpdated " + evt.path + "   " + evt.requestType
        if (Files.isDirectory(evt.path, LinkOption.NOFOLLOW_LINKS)) {
            // folder added, deleted or modified, re-create roots for GSE
            if (interpreter != null)
                interpreter.createEngine()
        }
        updateOpenedTabs(evt)
    }

    void fileRequest(FolderTreeViewEvent evt) {
        //println "" + evt.requestType + " " + evt.path + " " + evt.fileType
        startWait()

        Platform.runLater({
            try {
                Path path = evt.path
                boolean open = (evt.requestType == PathRequestType.OPEN)
                String fileType = evt.fileType
                openNewTab(path, fileType, open)
            } 
            catch(Exception ex) {
                println ex.getMessage()
            }
            finally {
                endWait()
            }
        })
    }

    void openNewTab(Path path, String fileType, boolean open) {
        if (open && openedTabs.containsValue(path)) {
            for(Map.Entry<Tab,Path> e : openedTabs) {
                if (e.getValue() == path) {
                    tabPane.getSelectionModel().select(e.getKey())
                }
            }
            return
        }
        
        Tab tab = null            
        
        if (open && fileType == null) {
            fileType = getFileExt(path)
        }

        Mode mode = null
        switch(fileType) {
            case "mirchord":
                mode = Mode.MirChord
                tab = newTabMirChord(path, mode, open)
                break
            case "groovy":
                mode = Mode.Groovy
                tab = newTabTextFile(path, mode, open)
                break
            case "xml":
            case "mxml":
                mode = Mode.XML
                tab = newTabTextFile(path, mode, open)
                break
            case "json":
                mode = Mode.JSON
                tab = newTabTextFile(path, mode, open)
                break
            case "abc":
                mode = Mode.ABC
                tab = newTabTextFile(path, mode, open)
                break
            case "mid":
                tab = newTabMidi(path, open)
                break
            case "txt":
                mode = Mode.Text
                tab = newTabTextFile(path, mode, open)
                break
            default:
                showAlert(AlertType.ERROR, "File not supported")
                break
        }

        if (tab != null) {
            addTab(tab)
            if (open)
                openedTabs.put(tab, path)
            else {
                openedTabs.put(tab, null)
            }
            tabPane.getSelectionModel().select(tab)
        }
    }

    Tab newTabTextFile(Path path, Mode mode, boolean open) {
        Tab tab = createTab()
        TextEditor editor
        if (open) {
            editor = new TextEditor(path)
            setTabLabelText(tab, path.getFileName().toString())
        }
        else {
            editor = new TextEditor()
            editor.setSuggestedOpenSaveFolder(path.toString())
            editor.setSuggestedOpenSaveFileName("untitled")
        }
        editor.setMode(mode)
        editor.addFolderTreeViewListener(this)
        tab.setContent(editor)
        return tab
    }

    Tab newTabMirChord(Path path, Mode mode, boolean open) {
        Tab tab = createTab()
        MirChordEditor mirchordEditor
        if (open) {
            mirchordEditor = new MirChordEditor(path, synthManager.getSynthesizer())
            setTabLabelText(tab, path.getFileName().toString())
        }
        else {
            mirchordEditor = new MirChordEditor(null, synthManager.getSynthesizer())
            TextEditor editor = mirchordEditor.getEditor()
            editor.setSuggestedOpenSaveFolder(path.toString())
            editor.setSuggestedOpenSaveFileName("untitled")
            editor.setValue("=1 ~1 ; Part 1 Voice 1 \n")
        }
        mirchordEditor.getEditor().addFolderTreeViewListener(this) 
        tab.setContent(mirchordEditor)
        return tab
    }

    Tab newTabMidi(Path path, boolean open) {
        Tab tab = createTab()
        MidiEditor midiEditor
        if (open) {
            midiEditor = new MidiEditor(path,synthManager.getSynthesizer())
            setTabLabelText(tab, path.getFileName().toString())
        }
        else {
            midiEditor = new MidiEditor(null,synthManager.getSynthesizer())
            midiEditor.setSuggestedOpenSaveFolder(path.toString())
            midiEditor.setSuggestedOpenSaveFileName("untitled")
        }
        midiEditor.addFolderTreeViewListener(this)  
        tab.setContent(midiEditor)
        return tab
    }

    // action

    void showtree() {
        int idx = splitPane.getItems().indexOf(folderTreeView)
        if (treeButton.selected) {
            if (idx < 0) {
                splitPane.getItems().add(0, folderTreeView)
                splitPane.setDividerPosition(0, 0.20D)
            }
        } else {
            if (idx >= 0)
                splitPane.getItems().remove(folderTreeView)
        }
    }

    void showInfoDialog() {
        Dialog<ButtonType> dialog = new Dialog<>()
        DialogPane dialogPane = dialog.getDialogPane()
        
        VBox vbox = new VBox()
        vbox.setPrefWidth(700)
        vbox.setPrefHeight(500)
        vbox.setAlignment(Pos.CENTER)
        Label label = new Label()
        label.setText(getClass().getResourceAsStream("resources/info.txt").getText())
        vbox.getChildren().add(label)

        dialogPane.setContent(vbox)
        dialogPane.getButtonTypes().addAll(ButtonType.OK)
        dialog.getDialogPane().lookupButton(ButtonType.OK)
        Optional<ButtonType> result = dialog.showAndWait()
    }

    void editproperties() {
        if (projectFolder.get() != null) {
            Dialog<ButtonType> dialog = new Dialog<>()
            DialogPane dialogPane = dialog.getDialogPane()
            dialogPane.getStylesheets().add(getClass().getResource("styles.css").toExternalForm())
            ConfigEditor configDialog = new ConfigEditor(projectFolder.get().toPath(), config)
            dialogPane.setContent(configDialog)
            ButtonType reloadBtn = new ButtonType("Reload", ButtonData.OK_DONE)
            ButtonType closeBtn = new ButtonType("Close", ButtonData.CANCEL_CLOSE)
            dialogPane.getButtonTypes().addAll(reloadBtn, closeBtn)
            dialog.getDialogPane().lookupButton(reloadBtn)
            Optional<ButtonType> result = dialog.showAndWait()
            if (result.isPresent() && result.get() == reloadBtn) {
                setupProject()
            }
        }
    }

    void newmirchordfile() {
        File suggestedDir = projectFolder.get()
        if (suggestedDir == null)
            suggestedDir = new File(System.getProperty("user.home"))
        openNewTab(suggestedDir.toPath(), "mirchord", false)
    }

    void newgroovyfile() {
        File suggestedDir = projectFolder.get()
        if (suggestedDir == null)
            suggestedDir = new File(System.getProperty("user.home"))
        openNewTab(suggestedDir.toPath(), "groovy", false)
    }

    void newmidifile() {
        File suggestedDir = projectFolder.get()
        if (suggestedDir == null)
            suggestedDir = new File(System.getProperty("user.home"))
        openNewTab(suggestedDir.toPath(), "mid", false)
    }

    void openfile() {
        File initialDir = projectFolder.get()
        if (initialDir == null)
            initialDir = new File(System.getProperty("user.home"))
        FileChooser fileChooser = new FileChooser()
        fileChooser.setTitle("Open File")
        fileChooser.getExtensionFilters().addAll(
            new ExtensionFilter("All Files", "*.*"),
            new ExtensionFilter("Mirchord files", "*.mirchord"),
            new ExtensionFilter("Groovy files", "*.groovy"),
            new ExtensionFilter("Midi files", "*.mid")
            )
        fileChooser.setInitialDirectory(
            initialDir
        )
        Stage stage = (Stage)getScene().getWindow()
        File selectedFile = fileChooser.showOpenDialog(stage)
        if (selectedFile != null) {
            openNewTab(selectedFile.toPath(), null, true)
        }
    }

    void openproject() {
        DirectoryChooser dirChooser = new DirectoryChooser()
        dirChooser.setTitle("Open Project Folder")
        dirChooser.setInitialDirectory(
            new File(System.getProperty("user.home"))
        )
        Stage stage = (Stage) getScene().getWindow()
        File selectedFolder = dirChooser.showDialog(stage)
        if (selectedFolder != null) {
            projectFolder.set(selectedFolder)
            openprojectFolder()
        }
    }

    void reloadproject() {
        if (projectFolder.get() != null) {
            openprojectFolder()
        }
    }

    @CompileDynamic
    void setupProject() {
        if (projectFolder.get() != null) {
            File configFile = new File(projectFolder.get(), "config.xml")
            config = ConfigurationManager.read(configFile.toPath())
            if (config.containsKey("lib")) {
                File lib = config.lib.toFile()
                if (interpreter != null) {
                    interpreter.addLib(lib)
                }
            }
            loadSoundbank()
        }
    }

    void openprojectFolder() {
        startWait()
        treeButton.setSelected(true)
        showtree()
        try {
            folderTreeView.setRoot(projectFolder.get().getPath())
            interpreter = new ProjectInterpreter(projectFolder.get().getPath(), typecheckButton.selected, new Binding(["MidiPlayer": midiPlayer]))
            setupProject()
            // copy the config for the binding
            Map<String,Path> configBinding = [:]
            config.each { String k, Path v ->
                configBinding[k] = Paths.get(v.toString())
            }
            interpreter.setBindingProperty("projectPath", projectFolder.get().toPath())
            interpreter.setBindingProperty("config", configBinding)
        } catch(Exception ex) {
            println "Exception: " + ex.getMessage()
        }
        finally {
                endWait()
            }
    }

    void newproject() {
        Dialog<ButtonType> dialog = new Dialog<>()
        DialogPane dialogPane = dialog.getDialogPane()        
        NewProjectDialog newprjdialog = new NewProjectDialog()
        dialogPane.setContent(newprjdialog)
        dialogPane.getButtonTypes().add(ButtonType.CANCEL)
        dialogPane.lookupButton(ButtonType.CANCEL)
        Optional<ButtonType> result = dialog.showAndWait()
        if (!(result.isPresent() && result.get() == ButtonType.CANCEL)) {
            if (newprjdialog.projectFolder != null) {
                projectFolder.set(newprjdialog.projectFolder)
                openprojectFolder()
            }
        }
    }

    void runscript() {
        outputConsole.clear()
        errorConsole.clear()
        Tab tab = tabPane.getSelectionModel().getSelectedItem()
        Path path = openedTabs[tab]

        TabContent tabContent = (TabContent) tab.getContent()

        if (tabContent.getTabType() == "MirChordEditor") {
            MirChordEditor editor = (MirChordEditor) tabContent
            String source = editor.getValue()
            Path codePath = projectFolder.get() != null ? projectFolder.get().toPath() : null
            if (codePath == null && path != null)
                codePath = path
            runMirChord(source, codePath, editor)
        }

        if (tabContent.getTabType() ==  "TextEditor") {
            TextEditor editor = (TextEditor) tabContent
            String source = editor.getValue()
            String scriptName = (path != null) ? path.toString() : null
            runGroovyScript(source, scriptName)
        }
    }

    void runMirChord(String source, Path codePath, MirChordEditor editor) {
        runThread = Thread.start {
            try {
                installInterceptor()
                stopButton.setDisable(false)
                Score result = interpreter.createScore(source, codePath)
                // it is here to show message related to the score in console
                if (result) {
                    Platform.runLater( {
                        editor.getViewer().loadScore(result)
                    })
                } else {
                    showErrorConsole(true)
                }
            } catch(Throwable t) {
                System.err.println t.message
                t.printStackTrace()
                showErrorConsole(true)
            } finally {
                unIinstallInterceptor()
                runThread = null
                stopButton.setDisable(true)
            }
        }
    }

    void runGroovyScript(String source, String scriptName) {
        runThread = Thread.start {
            try {
                installInterceptor()
                showErrorConsole(false)
                stopButton.setDisable(false)
                def result = interpreter.executeScriptSource(source, scriptName)
                if (!(result instanceof InterpreterException)) {
					if (result instanceof Node)
						Platform.runLater( { UtilsFx.showDialogFx((Node)result) })
					println "\nRESULT: " + result.toString()
                } else {
                    showErrorConsole(true)
                }
            } catch(Throwable t) {
                System.err.println t.message
                t.printStackTrace()
                showErrorConsole(true)
            } finally {
                unIinstallInterceptor()
                runThread = null
                stopButton.setDisable(true)
            }
        }
    }

    void stopscript() {
        if(runThread ) {
          runThread.interrupt()
          showErrorConsole(true)
          stopButton.setDisable(true)
      }
    }

    void showErrorConsole(boolean error=false) {
        int index = (error) ? 1 : 0
        consoleButton.setSelected(true)
        tabConsole.setVisible(true)
        tabConsole.getSelectionModel().select(index)
    }

    void showconsole() {
        tabConsole.setVisible(consoleButton.selected)
    }

    void typecheck() {
        if (interpreter != null) {
            interpreter.setStaticCompile(typecheckButton.selected)
            interpreter.createEngine()
        }
    }

    // adapted from groovyconsole
    void installInterceptor() {
        systemOutInterceptor = new SystemOutputInterceptor(this.&notifySystemOut, true)
        systemOutInterceptor.start()
        systemErrorInterceptor = new SystemOutputInterceptor(this.&notifySystemErr, false)
        systemErrorInterceptor.start()
    }

    boolean notifySystemOut(int consoleId, String str) {
        Platform.runLater( {
            try {
                outputConsole.appendText(str)
            } catch(Exception ex) {
                println "Exception: " + ex.getMessage()
            }
        } )
        return false
    }

    boolean notifySystemErr(int consoleId, String str) {
        Platform.runLater( {
            try {
                errorConsole.appendText(str)
            } catch(Exception ex) {
                println "Exception: " + ex.getMessage()
            }
        } )
        return false
    }

    void unIinstallInterceptor() {
        systemOutInterceptor.stop()
        systemErrorInterceptor.stop()
    }

    // Theme
    protected String currentThemeUrl;
    private String cupertinoLightUrl = getClass().getResource("resources/themes/cupertino-light.css").toExternalForm();
    private String cupertinoDarkUrl = getClass().getResource("resources/themes/cupertino-dark.css").toExternalForm();
    private String nordLightUrl = getClass().getResource("resources/themes/nord-light.css").toExternalForm();
    private String nordDarkUrl = getClass().getResource("resources/themes/nord-dark.css").toExternalForm();
    private String primerLightUrl = getClass().getResource("resources/themes/primer-light.css").toExternalForm();
    private String primerDarkUrl = getClass().getResource("resources/themes/primer-dark.css").toExternalForm();

    void setCupertinoLight() {
        Scene scene = getScene()
        if (currentThemeUrl != null && !currentThemeUrl.isEmpty())
            scene.getStylesheets().remove(currentThemeUrl)
        if (!scene.getStylesheets().contains(cupertinoLightUrl)) {
            scene.getStylesheets().add(cupertinoLightUrl)
            currentThemeUrl = cupertinoLightUrl
        }
    }

    void setCupertinoDark() {
        Scene scene = getScene()
        if (currentThemeUrl != null && !currentThemeUrl.isEmpty())
            scene.getStylesheets().remove(currentThemeUrl)
        if (!scene.getStylesheets().contains(cupertinoDarkUrl)) {
            scene.getStylesheets().add(cupertinoDarkUrl)
            currentThemeUrl = cupertinoDarkUrl
        }
    }

    void setNordLight() {
        Scene scene = getScene()
        if (currentThemeUrl != null && !currentThemeUrl.isEmpty())
            scene.getStylesheets().remove(currentThemeUrl)
        if (!scene.getStylesheets().contains(nordLightUrl)) {
            scene.getStylesheets().add(nordLightUrl)
            currentThemeUrl = nordLightUrl
        }
    }

    void setNordDark() {
        Scene scene = getScene()
        if (currentThemeUrl != null && !currentThemeUrl.isEmpty())
            scene.getStylesheets().remove(currentThemeUrl)
        if (!scene.getStylesheets().contains(nordDarkUrl)) {
            scene.getStylesheets().add(nordDarkUrl)
            currentThemeUrl = nordDarkUrl
        }
    }

    void setPrimerLight() {
        Scene scene = getScene()
        if (currentThemeUrl != null && !currentThemeUrl.isEmpty())
            scene.getStylesheets().remove(currentThemeUrl)
        if (!scene.getStylesheets().contains(primerLightUrl)) {
            scene.getStylesheets().add(primerLightUrl)
            currentThemeUrl = primerLightUrl
        }
    }

    void setPrimerDark() {
        Scene scene = getScene()
        if (currentThemeUrl != null && !currentThemeUrl.isEmpty())
            scene.getStylesheets().remove(currentThemeUrl)
        if (!scene.getStylesheets().contains(primerDarkUrl)) {
            scene.getStylesheets().add(primerDarkUrl)
            currentThemeUrl = primerDarkUrl
        }
    }

    // Exit
    void onExit() {
        // folderTreeView.stopWatching()
        System.exit(0)
    }

    void close() {
        folderTreeView.stopWatching()
        synthManager.close()
        midiPlayer.close()
    }

    boolean confirmClosing() {
        boolean clean = true
        for(Tab tab : tabPane.getTabs()) {
            TabContent tabContent = (TabContent) tab.getContent()
            try {
                clean = tabContent.isClean()
                if (!clean) {
                    Alert alert = new Alert(AlertType.CONFIRMATION, "Changes in tabs not saved. Close anyway?")
                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        return true
                    } else 
                        return false
                }
            } catch (Exception e) { }
        }
        return clean
    }
}

