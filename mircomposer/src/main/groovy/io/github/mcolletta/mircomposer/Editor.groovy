/*
 * Copyright (C) 2016-2022 Mirco Colletta
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

package io.github.mcolletta.mircomposer


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

import groovy.console.ui.SystemOutputInterceptor

import com.xenoage.zong.core.Score
import com.xenoage.zong.desktop.utils.JseZongPlatformUtils
import com.xenoage.utils.exceptions.InvalidFormatException

import io.github.mcolletta.mirtextfx.Mode
import io.github.mcolletta.mirtextfx.TextEditor
import io.github.mcolletta.mirscore.ScoreViewer
import io.github.mcolletta.mirmidi.MidiEditor

import io.github.mcolletta.mirfoldertreeview.FolderTreeView
import io.github.mcolletta.mirfoldertreeview.FolderTreeViewListener
import io.github.mcolletta.mirfoldertreeview.FolderTreeViewEvent
import io.github.mcolletta.mirfoldertreeview.PathRequestType

import io.github.mcolletta.mirconverter.ZongConverter
import io.github.mcolletta.mirchord.core.Score as MirScore
import io.github.mcolletta.mirchord.interpreter.MirChordInterpreter

import io.github.mcolletta.mirchord.core.ScoreBuilder

import io.github.mcolletta.mirsynth.SynthManager
import io.github.mcolletta.mirsynth.SimpleMidiPlayer

import io.github.mcolletta.mirutils.TabContent

import static io.github.mcolletta.mircomposer.Utils.getFileExt
import static io.github.mcolletta.mircomposer.DraggableTabs.*



public class Editor implements FolderTreeViewListener {

    private SynthManager synthManager
    public SimpleMidiPlayer midiPlayer

    private Map<Tab,Path> openedTabs = [:]
    private Map<Tab,Path> tabsFileRemoved = [:]

    //private ProjectInterpreter interpreter

    @FXML private CheckMenuItem consoleMenu
    @FXML private MenuItem runMenu
    @FXML private MenuItem stopMenu

    @FXML private ToggleButton consoleButton

    @FXML private MenuItem propertiesMenu

    @FXML private Button propertiesButton
    @FXML private Button runButton
    @FXML private Button stopButton
    
    @FXML private StackPane tabPaneContainer
    @FXML private TabPane tabPane

    @FXML private TabPane tabConsole
    @FXML private TextArea outputConsole
    @FXML private TextArea errorConsole

    private volatile Thread  runThread = null
    private SystemOutputInterceptor systemOutInterceptor
    private SystemOutputInterceptor systemErrorInterceptor

    private ObjectProperty<File> dataFolder = new SimpleObjectProperty<>()
    private Map<String,Path> config = [:]

    private boolean needAgreement = true
    

    @FXML public void initialize() {
        if (needAgreement)
            showLicenseAgreementDialog()
        tabConsole.managedProperty().bind(tabConsole.visibleProperty())
        tabConsole.setVisible(false)
        consoleMenu.selectedProperty().bindBidirectional(consoleButton.selectedProperty())

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
                        /*if (tabContent.getTabType() == "TextEditor")
                            mode = ((TextEditor)tabContent).getMode()*/
                        if (tabContent.getTabType() == "MirChordEditor")
                            mode = ((MirChordEditor)tabContent).getMode()
                        if (mode != null && mode in [Mode.MirChord, Mode.Groovy])
                            runButton.setDisable(false)
                    }
                }
            }
        )

        initMidi()
        loadConfig()
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
        ButtonType agreeBtn = new ButtonType("I agree, open MirComposer", ButtonData.OK_DONE)
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
        String appName = "MirComposer"
        JseZongPlatformUtils.init(appName)
        synthManager = new SynthManager()
        midiPlayer = new SimpleMidiPlayer(synthManager.getSynthesizer())

    }

    void loadConfig() {
        Path dataPath = Paths.get("data")
        println "dataPath=${dataPath.toAbsolutePath().toString()}"
        if (Files.exists(dataPath)) {
            dataFolder.set(dataPath.toFile())
            File configFile = new File(dataFolder.get(), "config.xml")
            Path configPath = configFile.toPath()
            if (Files.exists(configPath)) {
                config = ConfigurationManager.read(configPath)
                loadSoundbank()
            }          
        }
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
        return tabPaneContainer.getScene()
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
        // if (Files.isDirectory(evt.path, LinkOption.NOFOLLOW_LINKS)) {
        //     // folder added, deleted or modified, re-create roots for GSE
        // }
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
            case "mid":
                tab = newTabMidi(path, open)
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
        if (dataFolder.get() != null) {
            Dialog<ButtonType> dialog = new Dialog<>()
            DialogPane dialogPane = dialog.getDialogPane()
            dialogPane.getStylesheets().add(getClass().getResource("styles.css").toExternalForm())
            ConfigEditor configDialog = new ConfigEditor(dataFolder.get().toPath(), config)
            dialogPane.setContent(configDialog)
            ButtonType okBtn = new ButtonType("Load", ButtonData.OK_DONE)
            ButtonType closeBtn = new ButtonType("Close", ButtonData.CANCEL_CLOSE)
            dialogPane.getButtonTypes().addAll(okBtn, closeBtn)
            dialog.getDialogPane().lookupButton(okBtn)
            Optional<ButtonType> result = dialog.showAndWait()
            if (result.isPresent() && result.get() == okBtn) {
                //File configFile = new File(dataFolder.get(), "config.xml")
                //config = ConfigurationManager.read(configFile.toPath())
                loadSoundbank()
            }
        }
        else {
            showAlert(AlertType.ERROR, "Folder data for preferences not found")
        }
    }

    void newmirchordfile() {
        File suggestedDir = new File(System.getProperty("user.home"))
        openNewTab(suggestedDir.toPath(), "mirchord", false)
    }

    void newmidifile() {
        File suggestedDir = new File(System.getProperty("user.home"))
        openNewTab(suggestedDir.toPath(), "mid", false)
    }

    void openfile() {
        File initialDir = new File(System.getProperty("user.home"))
        FileChooser fileChooser = new FileChooser()
        fileChooser.setTitle("Open File")
        fileChooser.getExtensionFilters().addAll(
            new ExtensionFilter("All Files", "*.*"),
            new ExtensionFilter("Mirchord files", "*.mirchord"),
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

    void runscript() {
        outputConsole.clear()
        errorConsole.clear()
        Tab tab = tabPane.getSelectionModel().getSelectedItem()
        Path path = openedTabs[tab]

        TabContent tabContent = (TabContent) tab.getContent()

        if (tabContent.getTabType() == "MirChordEditor") {
            MirChordEditor editor = (MirChordEditor) tabContent
            String source = editor.getValue()
            /*Path codePath = projectFolder.get() != null ? projectFolder.get().toPath() : null
            if (codePath == null && path != null)
                codePath = path*/
            runMirChord(source, /*codePath,*/ editor)
        }
    }

    void stopscript() {
        if(runThread ) {
          runThread.interrupt()
          showErrorConsole(true)
          stopButton.setDisable(true)
      }
    }

    void runMirChord(String source, /*Path codePath,*/ MirChordEditor editor) {
        runThread = Thread.start {
            try {
                installInterceptor()
                stopButton.setDisable(false)
                //Score result = interpreter.createScore(source, codePath)
                Score result = createScore(source)
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

    boolean PRINT_STACKTRACE = true
    public Score createScore(String source) {
        MirChordInterpreter interpreter = new MirChordInterpreter([])
        try {
            MirScore mirscore = interpreter.evaluate(source)
            ZongConverter zconverter = new ZongConverter()
            Score score = zconverter.convert(mirscore)
            return score
        } catch(Exception ex) {
            System.err.println("Interpreter ERROR: " + ex.getMessage())
            if (PRINT_STACKTRACE) {
                StringWriter stacktrace = new StringWriter()
                ex.printStackTrace(new PrintWriter(stacktrace))
                System.err.println(stacktrace.toString())
            }
        }
        return null
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

    // Exit
    void onExit() {
        // folderTreeView.stopWatching()
        System.exit(0)
    }

    void close() {
        //folderTreeView.stopWatching()
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

