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

package io.github.mcolletta.mirmidi

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

import javafx.scene.shape.Rectangle

import javafx.geometry.Orientation

import javafx.fxml.FXML
import javafx.fxml.FXMLLoader

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.ListChangeListener

import javafx.beans.binding.NumberBinding
import javafx.beans.binding.StringBinding
import javafx.beans.binding.Bindings
import javafx.beans.value.ObservableValue
import javafx.beans.value.ChangeListener

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javafx.concurrent.Task

import org.controlsfx.control.CheckComboBox

import javax.sound.midi.*

import io.github.mcolletta.mirchord.core.Instrument

import groovy.transform.CompileStatic
import groovy.transform.Canonical

@Canonical
class TrackItem {

    int index
    Track track

    String toString() {
        return "Track " + index
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

@CompileStatic
class MidiEditor  extends VBox implements MidiPlaybackListener {

    MidiView midi
    
    ObservableList tracks
    ObservableList channels
    ObservableList controllers
    ObservableList instruments

    PianoRollEditor pianoRollEditor
    ControllerEditor controllerEditor
    InstrumentsEditor instrumentsEditor

    @FXML private ResizableCanvas pianoCanvas
    @FXML private ResizableCanvas controllerCanvas
    @FXML private ResizableCanvas instrumentsCanvas
    @FXML private ComboBox selectNoteDuration
    @FXML private ComboBox selectTrack
    @FXML private ComboBox selectChannel
    @FXML private ComboBox selectController
    @FXML private ComboBox selectInstrument
    @FXML private CheckComboBox selectMuteTracks
    @FXML private ScrollBar scrollBarX
    @FXML private Button undoButton
    @FXML private Button redoButton
    @FXML private Button filesaveButton
    @FXML private Label channelLabel
    @FXML private TextField currentZoomField

    Path filePath
    String suggestedOpenSaveFolder = System.getProperty("user.home")
    String suggestedOpenSaveFileName = "newfile.mid"

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

        initComboBoxes()

        pianoRollEditor = new PianoRollEditor(midi, pianoCanvas)
        controllerEditor = new ControllerEditor(midi, controllerCanvas)
        instrumentsEditor = new InstrumentsEditor(midi, instrumentsCanvas)
        pianoCanvas.repaint = pianoRollEditor.&repaint
        controllerCanvas.repaint = controllerEditor.&repaint
        instrumentsCanvas.repaint = instrumentsEditor.&repaint

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

        updateScrollBar()
        Bindings.bindBidirectional(scrollBarX.valueProperty(), midi.horizontalOffsetProperty())
        scrollBarX.valueProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue<? extends Number> ov, Number old_val, Number new_val) {
                draw()
            }
        })

        selectNoteDuration.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                float duration = newValue as float
                pianoRollEditor.insertNoteType = duration
            }
        })


        midi.playbackPositionProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue<? extends Number> ov, Number old_val, Number new_val) {
                draw()
            }
        })

        filesaveButton.disableProperty().bind(midi.cleanProperty())

        this.filePath = path
        if (filePath != null) {
            //midi.loadMidi(filePath.toFile())
            updateScrollBar()
            draw()
        }

        final KeyCombination keyCombinationSave = new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN)
        this.addEventFilter(KeyEvent.KEY_PRESSED, { KeyEvent evt ->
            if (keyCombinationSave.match(evt)) {
                filesave()
            }
        })      
	}

	public loadControl() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(getClass().getSimpleName() + ".fxml"))

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

    static class ColorRectCell extends ListCell<ChannelItem> {
        @Override
        public void updateItem(ChannelItem item, boolean empty) {
            super.updateItem(item, empty)
            Rectangle rect = new Rectangle(100, 16)
            if (item != null) {
                rect.setFill(item.color)
                setGraphic(rect)
                setText(item.toString())
            }
        }
    }

    void initComboBoxes() {
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
        setChannelLabelColor(midi.channelColor[0])

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

        loadSelectTrack()
        loadSelectChannel()
        loadSelectController()
        loadSelectMuteTracks()
        loadSelectInstrument()
    }

    private void loadSelectTrack() {
        selectTrack.setItems(tracks)
        selectTrack.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                if (newValue != null && oldValue != newValue) {
                    TrackItem item = selectTrack.getSelectionModel().getSelectedItem() as TrackItem
                    midi.currentTrack = item.index
                    draw()
                }
            }
        })
    }

     private void loadSelectChannel() {
        selectChannel.setItems(channels)
        selectChannel.setCellFactory({ListView<ChannelItem> l -> new ColorRectCell()})
        selectChannel.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                if (newValue != null && oldValue != newValue) {
                    ChannelItem item = selectChannel.getSelectionModel().getSelectedItem() as ChannelItem
                    midi.currentChannel = item.index
                    setChannelLabelColor(item.color)
                    draw()
                }
            }
        })
    }

    private setChannelLabelColor(Color color) {
        Rectangle rect = new Rectangle(100, 16)
        rect.setFill(color)
        channelLabel.setGraphic(rect)
    }

    private void loadSelectController() {
        selectController.setItems(controllers)
        selectController.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                if (newValue != null && oldValue != newValue) {
                    MidiControllerInfo item = selectController.getSelectionModel().getSelectedItem() as MidiControllerInfo
                    midi.currentController = item.value
                    draw()
                }
            }
        })
    }

    private void loadSelectMuteTracks() {
        selectMuteTracks.getItems().clear()
        selectMuteTracks.getItems().addAll(tracks)
        selectMuteTracks.getCheckModel().getCheckedItems().addListener(new ListChangeListener<TrackItem>() {
            public void onChanged(ListChangeListener.Change<? extends TrackItem> change) {
                while (change.next()) {
                    if (change.wasPermutated()) {
                        for (int i = change.getFrom(); i < change.getTo(); ++i) {
                            //println "permutate"
                        }
                    } else if (change.wasUpdated()) {
                        //println "update"
                    } else if (change.wasRemoved()) {
                        for (TrackItem item : change.getRemoved()) {
                            midi.sequencer.setTrackMute(item.index, false)
                        }
                    } else if (change.wasAdded()) {
                        for (TrackItem item : change.getAddedSubList()) {
                            midi.sequencer.setTrackMute(item.index, true)
                        }
                    }
                }
                draw()
            }
        })
    }

    private void loadSelectInstrument() {
        selectInstrument.setItems(instruments)
        selectInstrument.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                if (newValue != null && oldValue != newValue) {
                    MidiInstrument item = selectInstrument.getSelectionModel().getSelectedItem() as MidiInstrument
                    midi.currentInstrument = item.program
                    draw()
                }
            }
        })
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
        pianoRollEditor.repaint()
        controllerEditor.repaint()
        instrumentsEditor.repaint()
    }

    void drawPlayback() {
        pianoRollEditor.repaintPlayback()
        controllerEditor.repaintPlayback()
        instrumentsEditor.repaint()
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
        Stage dialog = new Stage(StageStyle.TRANSPARENT)
        dialog.initModality(Modality.WINDOW_MODAL)
        dialog.initOwner(((Node)event.getSource()).getScene().getWindow())

        VBox vbox = new VBox()
        Label label = new Label("Select how many tracks:")
        vbox.getChildren().add(label)
        Spinner spinner = new Spinner(1,16,1,1)
        vbox.getChildren().add(spinner)
        Button button = new Button("OK")
        button.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                dialog.close()
            }
        })
        vbox.getChildren().add(button)
        dialog.setScene(new Scene(vbox))
        dialog.showAndWait()
        // ---------------------------------------------
        midi.loadSequence(null, (int)spinner.getValue())
        midi.setHorizontalOffset(0L)
        updateScrollBar()
        initComboBoxes()
        draw()
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
            midi.loadMidi(selectedFile)
            filePath = selectedFile.toPath()
            updateScrollBar()
            draw()
        }
    }

    void filesave() {
        if (filePath != null) {
            File file = filePath.toFile()
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
                midi.markClean()
            } catch (IOException ex) {
                println(ex.getMessage())
            }
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

    // MIDI Playback Listener

    @Override 
    void playbackAtTick(long tick) {
        Platform.runLater({
            try {
                pianoRollEditor.playbackAtTick(tick)
                controllerEditor.playbackAtTick(tick)
                instrumentsEditor.playbackAtTick(tick)
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
    }

    void erase() {
        pianoRollEditor.delete()
        controllerEditor.delete()
        instrumentsEditor.delete()
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
        Stage dialog = new Stage(StageStyle.TRANSPARENT)
        dialog.initModality(Modality.WINDOW_MODAL)
        dialog.initOwner(((Node)event.getSource()).getScene().getWindow())

        VBox vbox = new VBox()
        Label label = new Label("Select humanization factor:")
        vbox.getChildren().add(label)
        // Spinner(double min, double max, double initialValue, double amountToStepBy)
        Spinner spinner = new Spinner(0.01D,2.0D,0.01D,0.01D)
        vbox.getChildren().add(spinner)
        Button button = new Button("OK")
        button.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                dialog.close()
            }
        })
        vbox.getChildren().add(button)
        dialog.setScene(new Scene(vbox))
        dialog.showAndWait()
        // ---------------------------------------------
        pianoRollEditor.humanization((double)spinner.getValue())
    }

    void quantization() {
        pianoRollEditor.quantization()
    }


    // show Table View

    void showMidiTable(ActionEvent event) {
        Stage stage = new Stage()
        FXMLLoader loader = new FXMLLoader(getClass().getResource("MidiTableView.fxml"))
        //Pane root = (Pane)FXMLLoader.load(getClass().getResource("MidiTableView.fxml"))
        Pane root = (Pane)loader.load()
        MidiTableViewController ctrl = (MidiTableViewController)loader.getController()
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