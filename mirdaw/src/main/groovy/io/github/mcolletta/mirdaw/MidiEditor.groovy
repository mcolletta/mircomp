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

package io.github.mcolletta.mirdaw

import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

import javafx.application.Platform
import javafx.stage.Stage
import javafx.stage.WindowEvent
import javafx.stage.FileChooser
import javafx.stage.FileChooser.ExtensionFilter
import javafx.stage.Modality
import javafx.stage.StageStyle

import javafx.event.EventHandler
import javafx.event.ActionEvent

import javafx.scene.Scene
import javafx.scene.Node
import javafx.scene.Cursor
import javafx.scene.paint.Color

import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import javafx.scene.input.KeyEvent

import javafx.scene.layout.VBox
import javafx.scene.layout.Pane

import javafx.scene.control.TextField
import javafx.scene.control.ScrollBar
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ComboBox
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.Spinner

import javafx.scene.control.Menu
import javafx.scene.control.MenuButton
import javafx.scene.control.MenuItem
import javafx.scene.control.RadioMenuItem
import javafx.scene.control.CustomMenuItem
import javafx.scene.control.ToggleGroup
import javafx.scene.control.Toggle
import javafx.scene.control.CheckBox
import javafx.scene.control.Spinner

import javafx.scene.control.Dialog
import javafx.scene.control.DialogPane
import javafx.scene.control.ButtonType
import javafx.scene.control.ButtonBar.ButtonData
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType

import javafx.scene.shape.Rectangle

import javafx.scene.input.KeyEvent
import javafx.scene.input.Dragboard
import javafx.scene.input.DragEvent
import javafx.scene.input.TransferMode

import javafx.geometry.Orientation

import javafx.fxml.FXML
import javafx.fxml.FXMLLoader

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.ListChangeListener

import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.binding.NumberBinding
import javafx.beans.binding.StringBinding
import javafx.beans.binding.Bindings
import javafx.beans.value.ObservableValue
import javafx.beans.value.ChangeListener

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javafx.concurrent.Task

import javax.sound.midi.*

import io.github.mcolletta.mirdaw.Instrument

import groovy.transform.Canonical

@Canonical
class TrackItem {

    int index
    Track track

    String toString() {
        return "Track " + (index+1)
    }
}

@Canonical
class ChannelItem {

    int index
    Color color

    String toString() {
        return "Channel " + (index+1)
    }
}

class MidiEditor extends VBox implements MidiPlaybackListener {

    MidiView midi
    
    ObservableList<TrackItem> tracks
    ObservableList<ChannelItem> channels
    ObservableList<MidiControllerInfo> controllers
    ObservableList<MidiInstrument> instruments

    PianoKeyboard pianoKeyboard
    PianoRollEditor pianoRollEditor
    ControllerEditor controllerEditor
    InstrumentsEditor instrumentsEditor
    TempoEditor tempoEditor

    @FXML private ResizableRegion keyboardCanvas
    @FXML private ResizableRegion pianoCanvas
    @FXML private ResizableRegion controllerCanvas
    @FXML private ResizableRegion instrumentsCanvas
    @FXML private ResizableRegion tempoCanvas

    @FXML private MenuButton tracksMenu
    @FXML private Label trackLabel
    @FXML private MenuButton channelsMenu
    @FXML private Label channelLabel
    @FXML private MenuButton notesMenu
    @FXML private Label noteLabel
    @FXML private MenuButton controllersMenu
    @FXML private Label controllerLabel
    @FXML private MenuButton instrumentsMenu
    @FXML private Label instrumentLabel
    @FXML private MenuButton muteTracksMenu
    @FXML private ScrollBar scrollBarX
    @FXML private Button undoButton
    @FXML private Button redoButton
    @FXML private Button filesaveButton
    @FXML private TextField currentZoomField

    @FXML private Spinner<Integer> tempoSpinner

    ObjectProperty<Path> filePath = new SimpleObjectProperty<>()
    Path getFilePath() {
        return filePath.get()
    }
    void setFilePath(Path path) {
        filePath.set(path)
    }

    String suggestedOpenSaveFolder = System.getProperty("user.home")
    String suggestedOpenSaveFileName = "newfile.mid"

    String getTabType() { return "MidiEditor"; }

	public MidiEditor(Path path=null, Synthesizer synth=null) {
		loadControl()

        midi = new MidiView(path,synth)
        midi.registerListener(this)
        midi.channelMask.addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue o,Object oldVal, Object newVal) {
                draw()
            }
        })

        initMenus()

        pianoKeyboard = new PianoKeyboard(midi, keyboardCanvas)
        pianoRollEditor = new PianoRollEditor(midi, pianoCanvas)
        controllerEditor = new ControllerEditor(midi, controllerCanvas)
        instrumentsEditor = new InstrumentsEditor(midi, instrumentsCanvas)
        tempoEditor = new TempoEditor(midi, tempoCanvas)

        currentZoomField.setText("" + 100)
        currentZoomField.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                if (keyEvent.getCode() == KeyCode.ENTER)  {
                    String text = currentZoomField.getText()
                    float newZoom = 1.0f
                    try {
                        newZoom = Float.parseFloat(text)
                        newZoom = (float)(newZoom / 100f)
                    } catch(Exception ex) {
                        newZoom = -1
                    }
                    if (newZoom > 0f && newZoom <= 3f) {
                        midi.setCurrentZoom(newZoom)
                        draw()
                    }
                    currentZoomField.setText("" + (int)(newZoom * 100))
                }
            }
        })

        tempoSpinner.valueProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue<? extends Number> ov, Number old_val, Number new_val) {
                tempoEditor.currentBPM = new_val as int
                draw()
            }
        })
        // tempoEditor.currentBPM = tempoSpinner.getValue()

        updateScrollBar()
        Bindings.bindBidirectional(scrollBarX.valueProperty(), midi.horizontalOffsetProperty())
        // scrollBarX.valueProperty().addListener(new ChangeListener<Number>() {
        //     public void changed(ObservableValue<? extends Number> ov, Number old_val, Number new_val) {
        //         draw()
        //     }
        // })

        midi.playbackPositionProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue<? extends Number> ov, Number old_val, Number new_val) {
                draw()
            }
        })

        // re-draw on every midiview property change
        midi.horizontalOffsetProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue<? extends Number> ov, Number old_val, Number new_val) {
                draw()
            }
        })
        midi.verticalOffsetProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue<? extends Number> ov, Number old_val, Number new_val) {
                draw()
            }
        })
        midi.currentScaleXProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue<? extends Number> ov, Number old_val, Number new_val) {
                draw()
            }
        })
        midi.currentScaleYProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue<? extends Number> ov, Number old_val, Number new_val) {
                draw()
            }
        })
        // -------------------------------

        filesaveButton.disableProperty().bind(midi.cleanProperty())

        if (path != null) {
            setFilePath(path)
            updateScrollBar()
            draw()
        }

        // filePath.addListener(new ChangeListener(){
        //     @Override public void changed(ObservableValue o,Object oldVal, Object newVal){
        //         if (newVal != null) {
        //             Path newPath = newVal as Path
        //             fireFolderTreeUpdated(new FolderTreeViewEvent([origin: this,
        //                                                            path: newPath,
        //                                                            requestType: PathRequestType.MODIFY,
        //                                                            fileType: ""]))
        //         }
        //     }
        // }) 

        final KeyCombination keyCombinationSave = new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN)
        this.addEventFilter(KeyEvent.KEY_PRESSED, { KeyEvent evt ->
            if (keyCombinationSave.match(evt)) {
                filesave()
            }
        })


        // Manage drop from dragging midi file
        this.setOnDragOver(new EventHandler<DragEvent>() {
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
        this.setOnDragDropped(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event)
            {
                Dragboard dragboard = event.getDragboard()
                boolean success = false
                if (dragboard.hasFiles()) {
                    File file = dragboard.getFiles()[0]
                    boolean loaded = midi.loadMidi(file)
                    tempoSpinner.getValueFactory().setValue( (int) midi.getSequencerBPM())
                    if (loaded) {
                        draw()
                        success = true
                    } else {
                        showAlert(AlertType.ERROR, "Could not get sequence from file")
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

	public loadControl() {
        // println "" + getClass().getSimpleName() + ".fxml"
        // println "" + getClass().getResource(getClass().getSimpleName() + ".fxml")
        // println "" + getClass().getClassLoader().getResource(getClass().getSimpleName() + ".fxml")
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(getClass().getSimpleName() + ".fxml"))
        // FXMLLoader fxmlLoader = new FXMLLoader(getClass().getClassLoader().getResource(getClass().getSimpleName() + ".fxml"))

        fxmlLoader.setRoot(this)
        fxmlLoader.setController(this)

        try {
            fxmlLoader.load()
        } catch (IOException exception) {
            throw new RuntimeException(exception)
        }
    }

    void setSynthesizer(Synthesizer synthesizer) {
        if (midi != null)
            midi.setSynthesizer(synthesizer)
    }

    boolean isClean() {
        return midi.isClean()
    }

    void initMenus() {
        List<TrackItem> trackList = []
        for(int i = 0; i < midi.sequence.tracks.size(); i++) {
            Track track = midi.sequence.tracks[i]
            TrackItem item = new TrackItem(i, track)
            trackList.add(item)
        } 
        tracks = FXCollections.observableArrayList(trackList)

        List<ChannelItem> channelList = []
        for(int i = 0; i < midi.channelColor.size(); i++) {
            Color color = midi.channelColor[i]
            ChannelItem item = new ChannelItem(i, color)
            channelList.add(item)
        }
        channels = FXCollections.observableArrayList(channelList)

        List<MidiControllerInfo> controllerList = []
        for(int i = 0; i < midi.controllersInfo.values().size(); i++) {
            MidiControllerInfo controller = midi.controllersInfo.values()[i]
            controllerList.add(controller)
        }        
        controllers = FXCollections.observableArrayList(controllerList)

        List<MidiInstrument> instrumentList = []
        for(Map.Entry<String,Integer> e : Instrument.GM.entrySet()) {
            MidiInstrument instrument = new MidiInstrument([name:e.getKey(),program:e.getValue()])
            instrumentList.add(instrument)
        }        
        instruments = FXCollections.observableArrayList(instrumentList)

        loadTracksMenu()
        loadChannelsMenu()
        loadControllersMenu()
        loadMuteTracksMenu()
        loadInstrumentsMenu()
        loadNotesMenu()
    }

    private void loadTracksMenu() {
        tracksMenu.getItems().clear()
        ToggleGroup toggleGroup = new ToggleGroup()
        for(int i=0; i<tracks.size(); i++) {
            TrackItem item = tracks[i]
            String label = item.toString()
            RadioMenuItem radioItem = new RadioMenuItem(label)
            radioItem.setOnAction(new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent event) {
                    midi.currentTrack = item.getIndex()
                    trackLabel.setText(label)
                    draw()
                }
            })
            if (item.getIndex() == midi.currentTrack) {
                radioItem.setSelected(true)
                trackLabel.setText(label)
            }
            radioItem.setToggleGroup(toggleGroup)
            tracksMenu.getItems().add(radioItem)
        }
    }

    private void loadChannelsMenu() {
        channelsMenu.getItems().clear()
        ToggleGroup toggleGroup = new ToggleGroup()
        for(int i=0; i<channels.size(); i++) {
            ChannelItem item = channels[i]
            String label = item.toString()
            RadioMenuItem radioItem = new RadioMenuItem(label)
            Rectangle rect = new Rectangle(100, 16)
            rect.setFill(item.getColor())
            radioItem.setGraphic(rect)
            radioItem.setOnAction(new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent event) {
                    midi.currentChannel = item.getIndex()
                    setChannelLabelColor(item.getColor())
                    channelLabel.setText(label)
                    draw()
                }
            })
            if (item.getIndex() == midi.currentChannel) {
                radioItem.setSelected(true)
                setChannelLabelColor(item.getColor())
                channelLabel.setText(label)
            }
            radioItem.setToggleGroup(toggleGroup)
            channelsMenu.getItems().add(radioItem)
        }
    }

    private setChannelLabelColor(Color color) {
        Rectangle rect = new Rectangle(100, 16)
        rect.setFill(color)
        channelLabel.setGraphic(rect)
    }

    private void loadMuteTracksMenu() {
        muteTracksMenu.getItems().clear()
        for(int i=0; i<tracks.size(); i++) {
            TrackItem item = tracks[i]
            String label = item.toString()
            CheckBox cb = new CheckBox(label)

            cb.selectedProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) {
                    // println cb.getText() + " " + cb.isSelected()
                    midi.sequencer.setTrackMute(item.getIndex(), cb.isSelected())
                    draw()
                }
            })

            CustomMenuItem menuItem = new CustomMenuItem(cb)
            // menuItem.setUserData(item)
            menuItem.setHideOnClick(false)
            muteTracksMenu.getItems().add(menuItem)
        }
    }

    private void loadControllersMenu() {
        controllersMenu.getItems().clear()
        ToggleGroup toggleGroup = new ToggleGroup()
        Menu submenuContinuos = new Menu("0-127")
        Menu submenuSoundControllers = new Menu("Sound Controllers")
        Menu submenuOnOff = new Menu("On/Off")        
        for(int i=0; i<controllers.size(); i++) {
            MidiControllerInfo item = controllers[i]
            String label = "${item.getValue()}: ${item.getInfo()}"
            RadioMenuItem radioItem = new RadioMenuItem(label)
            radioItem.setOnAction(new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent event) {
                    midi.currentController = item.getValue()
                    controllerLabel.setText(item.getInfo())
                    draw()
                }
            })
            if (item.getValue() == 7)
                radioItem.setSelected(true)
            radioItem.setToggleGroup(toggleGroup)
            if (item.getValue() in (70..78))
                submenuSoundControllers.getItems().add(radioItem)
            else {
                if (item.getCtype() == 1)
                    submenuOnOff.getItems().add(radioItem)
                else
                    submenuContinuos.getItems().add(radioItem)
            }
        }
        submenuContinuos.getItems().add(submenuSoundControllers)
        controllersMenu.getItems().add(submenuContinuos)
        controllersMenu.getItems().add(submenuOnOff)
    }

    private void loadInstrumentsMenu() {
        instrumentsMenu.getItems().clear()
        Map<String, IntRange> instrumentGroups = [
            "Piano": (1..8),
            "Chromatic Percussion": (9..16),
            "Organ": (10..24),
            "Guitar": (25..32),
            "Bass": (33..40),
            "Strings": (41..48),
            "Ensemble": (49..56),
            "Brass": (57..64),
            "Reed": (65..72),
            "Pipe": (73..80),
            "Synth Lead": (81..88),
            "Synth Pad": (89..96),
            "Synth Effects": (97..104),
            "Ethnic": (105..112),
            "Percussive": (113..120),
            "Sound effects": (121..128)
        ]  
        
        ToggleGroup toggleGroup = new ToggleGroup()
        for(Map.Entry<String,IntRange> e : instrumentGroups.entrySet()) {
            Menu submenu = new Menu(e.getKey())
            for(int i : e.getValue()) {
                MidiInstrument item = instruments[i-1]
                String label = "GM $i: ${item.getName()}"
                RadioMenuItem radioItem = new RadioMenuItem(label)
                radioItem.setOnAction(new EventHandler<ActionEvent>() {
                    @Override public void handle(ActionEvent event) {
                        midi.currentInstrument = item.getProgram()
                        instrumentLabel.setText(label)
                    }
                })
                if (item.getProgram() == midi.currentInstrument) {
                    radioItem.setSelected(true)
                    instrumentLabel.setText(label)
                }
                radioItem.setToggleGroup(toggleGroup)
                submenu.getItems().add(radioItem)
            }
            instrumentsMenu.getItems().add(submenu)
        }
    }

    private void loadNotesMenu() {
        notesMenu.getItems().clear()
        //Menu submenu = new Menu("Triplet")
        ToggleGroup toggleGroup = new ToggleGroup()
        Map<String, Float> durations = [
            "whole": 4f,
            "half": 2f,
            "quarter": 1f,
            "eighth": 0.5f,
            "sixteenth": 0.25f
        ]
        for(Map.Entry<String,Float> e : durations.entrySet()) {
            String label = e.getKey()
            float duration = e.getValue()
            RadioMenuItem radioItem = new RadioMenuItem(label)
            radioItem.setOnAction(new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent event) {                    
                    pianoRollEditor.insertNoteType = duration
                    noteLabel.setText(label)
                }
            })
            if (duration == 1) {
                radioItem.setSelected(true)
                noteLabel.setText(label)
            }
            radioItem.setToggleGroup(toggleGroup)
            notesMenu.getItems().add(radioItem)
        }
    }

    public void onClose() {
        Stage stage = (Stage)getScene().getWindow()
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                Platform.exit()
                System.exit(0)
            }
        })
    }

    void draw() {
        pianoKeyboard.repaint()
        pianoRollEditor.repaint()
        controllerEditor.repaint()
        instrumentsEditor.repaint()
        tempoEditor.repaint()
    }

    void drawPlayback() {
        pianoKeyboard.repaintLayer()
        pianoRollEditor.repaintLayer()
        controllerEditor.repaintLayer()
        instrumentsEditor.repaintLayer()
        tempoEditor.repaintLayer()
    }

    void updateScrollBar() {
        long lengthX = midi.getLength()
        scrollBarX.setMin(0)
        scrollBarX.setMax(lengthX)
        scrollBarX.setBlockIncrement(midi.fromX(pianoCanvas.getWidth()))
        scrollBarX.setUnitIncrement(midi.fromX(pianoCanvas.getWidth() / 12))
    }

    // actions

    void newsequence(ActionEvent event) {
        // GUI dialog for choosing how many tracks
        Dialog<ButtonType> dialog = new Dialog<>()
        DialogPane dialogPane = dialog.getDialogPane()
        
        VBox vbox = new VBox()
        Label label = new Label("Select how many tracks:")
        vbox.getChildren().add(label)
        Spinner spinner = new Spinner(1,16,1,1)
        vbox.getChildren().add(spinner)

        dialogPane.setContent(vbox)
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL)
        dialog.getDialogPane().lookupButton(ButtonType.OK)
        Optional<ButtonType> result = dialog.showAndWait()
        if (result.isPresent() && result.get() == ButtonType.OK) {
            midi.loadSequence(null, (int)spinner.getValue())
            midi.setHorizontalOffset(0L)
            updateScrollBar()
            initMenus()
            draw()
        }
    }

    void fileopen() {
        FileChooser fileChooser = new FileChooser()
        fileChooser.setTitle("Open Midi File")
        fileChooser.getExtensionFilters().addAll(
             new ExtensionFilter("Midi Files", "*.mid"),
             new ExtensionFilter("All Files", "*.*"))
        fileChooser.setInitialDirectory(
            new File(System.getProperty("user.home"))
        )
        Stage stage = (Stage)getScene().getWindow()
        File selectedFile = fileChooser.showOpenDialog(stage)
        if (selectedFile != null) {
            boolean loaded = midi.loadMidi(selectedFile)
            tempoSpinner.getValueFactory().setValue( (int) midi.getSequencerBPM())
            if (loaded) {
                setFilePath(selectedFile.toPath())
                updateScrollBar()
                draw()
            } else {
                showAlert(AlertType.ERROR, "Could not get sequence from file")
            }
        }
    }

    void filesave() {
        if (getFilePath() != null) {
            File file = getFilePath().toFile()
            try {
                midi.saveAs(file)
                midi.markClean()
            } catch (IOException ex) {
                println(ex.getMessage())
            }
        } else {
            filesaveas()
        }
    }

    void filesaveas() {
        FileChooser fileChooser = new FileChooser()
        fileChooser.setTitle("Save Midi Sequence as...")        
        fileChooser.getExtensionFilters().addAll(
             new ExtensionFilter("Midi Files", "*.mid"))
        fileChooser.setInitialDirectory(
            new File(suggestedOpenSaveFolder)
        )
        fileChooser.setInitialFileName("midifile.mid")
        Stage stage = (Stage)getScene().getWindow()
        File file = fileChooser.showSaveDialog(stage)
        if (file != null) {
            try {
                midi.saveAs(file)
                setFilePath(file.toPath())
                midi.markClean()
            } catch (IOException ex) {
                println(ex.getMessage())
            }
        }
    }

    public reloadfile() {
        if (getFilePath() != null) {
            midi.loadMidi(getFilePath().toFile())
            tempoSpinner.getValueFactory().setValue( (int) midi.getSequencerBPM())
            draw()
        }
    }

    void replay() {
        midi.stop()
        midi.play()
    }

    void play() {
        midi.play()
    }

    void pause() {
        midi.pause()
    }

    void stop() {
        midi.stop()
    }

    void close() {
        midi.close()
    }

    // MIDI Playback Listener

    @Override 
    void playbackAtTick(long tick) {
        Platform.runLater({
            try {
                pianoRollEditor.playbackAtTick(tick)
                controllerEditor.playbackAtTick(tick)
                instrumentsEditor.playbackAtTick(tick)
                tempoEditor.playbackAtTick(tick)
                drawPlayback()
            } catch(Exception ex) {
                println "Exception: " + ex.getMessage()
            }
        })
    }

    @Override 
    void playbackStarted() {
    }

    @Override 
    void playbackPaused() {
    }

    @Override 
    void playbackStopped() {
    }

    @Override 
    void playbackAtEnd() {
    }

    // -------------------------------------------

    void setCanvasesCursor(Cursor c) {
        pianoRollEditor.setCursor(c)
        controllerEditor.setCursor(c)
        instrumentsEditor.setCursor(c)
        tempoEditor.setCursor(c)
    }

    void addtrack() {
        midi.addTrackToSequence()
        initMenus()
        draw()
        showAlert(AlertType.INFORMATION, "New track added to sequence")
    }

    void erase() {
        pianoRollEditor.delete()
        controllerEditor.delete()
        instrumentsEditor.delete()
        tempoEditor.delete()
    }

    void editMode() {
        midi.mode = Mode.EDIT
        setCanvasesCursor(Cursor.CROSSHAIR)
    }

    void selectMode() {
        midi.mode = Mode.SELECT
        setCanvasesCursor(Cursor.DEFAULT)
    }

    void panMode() {
       midi.mode = Mode.PANNING
       setCanvasesCursor(Cursor.MOVE)
    }

    void playbackPosMode() {
        midi.mode = Mode.SET_PLAYBACK_POSITION
        setCanvasesCursor(Cursor.DEFAULT)
    }

    void lineMode() {
        controllerEditor.pencilMode = ControllerEditMode.LINE
    }

    void curveMode() {
        controllerEditor.pencilMode = ControllerEditMode.CURVE
    }

    void tempoEditedMode() {
        tempoEditor.tempoMode = TempoEditMode.EDITED
    }

    void tempoAxisMode() {
        tempoEditor.tempoMode = TempoEditMode.AXIS
    }


    void undo() {
        midi.undo()
        HandleUndoRedoButtons()
        draw()
    }

    void redo() {
        midi.redo()
        HandleUndoRedoButtons()
        draw()
    }

    private void HandleUndoRedoButtons() {
        undoButton.setDisable(!hasUndo())
        redoButton.setDisable(!hasRedo())
    }

    boolean hasUndo() {
        return midi.hasUndo()
    }

    boolean hasRedo() {
        return midi.hasRedo()
    }

    void zoom100() {
        midi.setCurrentZoom(1.0f)
        updateZoomTextField()
        draw()
    }

    void zoomin() {
        midi.setCurrentZoom(midi.getCurrentZoom() + 0.25f)
        updateZoomTextField()
        draw()
    }

    void zoomout() {
        midi.setCurrentZoom(midi.getCurrentZoom() - 0.25f)
        updateZoomTextField()
        draw()
    }

    void updateZoomTextField() {
        currentZoomField.setText("" + (int)(midi.getCurrentZoom() * 100))
    }

    void humanization(ActionEvent event) {
        // GUI dialog for eps
        Dialog<ButtonType> dialog = new Dialog<>()
        DialogPane dialogPane = dialog.getDialogPane()
        
        VBox vbox = new VBox()
        Label label = new Label("Select humanization factor:")
        vbox.getChildren().add(label)
        // Spinner(double min, double max, double initialValue, double amountToStepBy)
        Spinner spinner = new Spinner(0.01D,2.0D,0.01D,0.01D)
        vbox.getChildren().add(spinner)

        dialogPane.setContent(vbox)
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL)
        dialog.getDialogPane().lookupButton(ButtonType.OK)
        Optional<ButtonType> result = dialog.showAndWait()
        if (result.isPresent() && result.get() == ButtonType.OK) {
            pianoRollEditor.humanization((double)spinner.getValue())
        }
    }

    void quantization() {
        Dialog<ButtonType> dialog = new Dialog<>()
        DialogPane dialogPane = dialog.getDialogPane()
        
        VBox vbox = new VBox()
        Label label = new Label("Quantization of selected notes?")
        vbox.getChildren().add(label)

        dialogPane.setContent(vbox)
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL)
        dialog.getDialogPane().lookupButton(ButtonType.OK)
        Optional<ButtonType> result = dialog.showAndWait()
        if (result.isPresent() && result.get() == ButtonType.OK) {
            pianoRollEditor.quantization()
        }        
    }


    // show Table View

    void showMidiTable(ActionEvent event) {
        Stage stage = new Stage()
        FXMLLoader loader = new FXMLLoader(getClass().getResource("MidiTableView.fxml"))
        //Pane root = (Pane)FXMLLoader.load(getClass().getResource("MidiTableView.fxml"))
        Pane root = (Pane)loader.load()
        MidiTableView ctrl = (MidiTableView)loader.getController()
        if (pianoRollEditor.selectedNotes.size() > 0) {
            long startTick = Long.MAX_VALUE
            long endTick = Long.MIN_VALUE
            for(MidiNote note : pianoRollEditor.selectedNotes) {
                if (note.getStart() < startTick)
                    startTick = note.getStart()
                if (note.getEnd() > endTick)
                    endTick = note.getEnd()
            }
            ctrl.setStartTick(startTick)
            ctrl.setEndTick(endTick)
        }
        ctrl.setMidiView(midi)
        stage.setScene(new Scene(root))
        stage.setTitle("MidiEvent List View")
        stage.initModality(Modality.WINDOW_MODAL)
        stage.initOwner(((Node)event.getSource()).getScene().getWindow())
        stage.show()
    }
}