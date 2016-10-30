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

import javafx.application.Platform
import javafx.stage.Stage
import javafx.stage.WindowEvent
import javafx.stage.FileChooser
import javafx.stage.FileChooser.ExtensionFilter
import javafx.event.EventHandler
import javafx.event.ActionEvent

import javafx.scene.Cursor
import javafx.scene.paint.Color

import javafx.scene.input.KeyEvent
import javafx.scene.input.KeyCode

import javafx.scene.layout.VBox

import javafx.scene.control.TextField
import javafx.scene.control.ScrollBar
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ComboBox
import javafx.scene.control.ListCell
import javafx.scene.control.ListView

import javafx.scene.shape.Rectangle

import javafx.geometry.Orientation

import javafx.fxml.FXML
import javafx.fxml.FXMLLoader

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.ListChangeListener

import javafx.beans.binding.NumberBinding
import javafx.beans.binding.Bindings
import javafx.beans.value.ObservableValue
import javafx.beans.value.ChangeListener

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javafx.concurrent.Task

import org.controlsfx.control.CheckComboBox

import javax.sound.midi.*

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

    PianoRollEditor pianoRollEditor
    ControllerEditor controllerEditor

    @FXML private ResizableCanvas pianoCanvas
    @FXML private ResizableCanvas controllerCanvas
    @FXML private ComboBox selectNoteDuration
    @FXML private ComboBox selectTrack
    @FXML private ComboBox selectChannel
    @FXML private ComboBox selectController
    @FXML private CheckComboBox selectMuteTracks
    @FXML private ScrollBar scrollBarX
    @FXML private Button undoButton
    @FXML private Button redoButton
    @FXML private Label channelLabel
    @FXML private TextField currentZoomField

	public MidiEditor() {
		loadControl()

        midi = new MidiView()
        midi.registerListener(this)

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

        loadSelectTrack()
        loadSelectChannel()
        loadSelectController()
        loadSelectMuteTracks()

        pianoRollEditor = new PianoRollEditor(midi, pianoCanvas)
        controllerEditor = new ControllerEditor(midi, controllerCanvas)
        pianoCanvas.repaint = pianoRollEditor.&repaint
        controllerCanvas.repaint = controllerEditor.&repaint

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
    }

    void drawPlayback() {
        pianoRollEditor.repaintPlayback()
        controllerEditor.repaintPlayback()
    }

    void updateScrollBar() {
        long lengthX = midi.getLength()
        scrollBarX.setMin(0)
        scrollBarX.setMax(lengthX)
        scrollBarX.setBlockIncrement(midi.fromX(pianoCanvas.getWidth()))
        scrollBarX.setUnitIncrement(midi.fromX(pianoCanvas.getWidth() / 12))
    }

    // actions

    void newsequence() {
        // TODO: GUI dialog for choosing how many tracks
        midi.loadSequence(null, 16)
        midi.setHorizontalOffset(0L)
        updateScrollBar()
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
            updateScrollBar()
            draw()
        }
    }

    void filesaveas() {
        FileChooser fileChooser = new FileChooser()
        fileChooser.setTitle("Save Midi Sequence as...")        
        fileChooser.getExtensionFilters().addAll(
             new ExtensionFilter("Midi Files", "*.mid"))
        fileChooser.setInitialFileName("midifile.mid")
        Stage stage = (Stage)getScene().getWindow()
        File file = fileChooser.showSaveDialog(stage)
        if (file != null) {
            try {
                midi.saveAs(file)
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

    void editMode() {
        pianoRollEditor.mode = PianoRollMode.EDIT
        pianoRollEditor.setCursor(Cursor.CROSSHAIR)
    }

    void selectMode() {
        pianoRollEditor.mode = PianoRollMode.SELECT
        pianoRollEditor.setCursor(Cursor.DEFAULT)
    }

    void panMode() {
       pianoRollEditor.mode = PianoRollMode.PANNING
       pianoRollEditor.setCursor(Cursor.MOVE)
    }

    void playbackPosMode() {
        pianoRollEditor.mode = PianoRollMode.SET_PLAYBACK_POSITION
        pianoRollEditor.setCursor(Cursor.DEFAULT)
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

    void humanization() {
        // TODO GUI dialog fro choosing eps
        pianoRollEditor.humanization(0.3D)
    }

    void quantization() {
        pianoRollEditor.quantization()
    }
}