package io.github.mcolletta.mirmidi

import javafx.fxml.FXML
import javafx.scene.control.TableView

import javafx.beans.property.StringProperty
import javafx.beans.property.IntegerProperty
import javafx.beans.property.LongProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleLongProperty

import javafx.collections.FXCollections
import javafx.collections.ObservableList

import javax.sound.midi.*

import groovy.transform.CompileStatic
import groovy.transform.Canonical

@CompileStatic
class MidiEventItem {

	LongProperty tick = new SimpleLongProperty(0L)
    IntegerProperty track = new SimpleIntegerProperty(0)
    IntegerProperty channel = new SimpleIntegerProperty(0)
    StringProperty command = new SimpleStringProperty("")
    IntegerProperty data1 = new SimpleIntegerProperty(0)
    IntegerProperty data2 = new SimpleIntegerProperty(0)

    public long getTick() {
        return tick.get()
    }

    public void setTick(long tk) {
        tick.set(tk);
    }

    public int getTrack() {
        return track.get()
    }

    public void setTrack(int tr) {
        track.set(tr)
    }

    public int getChannel() {
        return channel.get()
    }

    public void setChannel(int ch) {
        channel.set(ch)
    }

    public String getCommand() {
        return command.get()
    }

    public void setCommand(String cmd) {
        command.set(cmd)
    }

    public int getData1() {
        return data1.get()
    }

    public void setData1(int data) {
        data1.set(data);
    }

    public int getData2() {
        return data2.get()
    }

    public void setData2(int data) {
        data2.set(data);
    }
}

@CompileStatic
class MidiTableViewController {

	MidiView midi
	long startTick = Long.MIN_VALUE
	long endTick = Long.MAX_VALUE

	@FXML private TableView<MidiEventItem> tableView
	ObservableList<MidiEventItem> events

	MidiTableViewController() {}

	void setMidiView(MidiView midi) {
		this.midi = midi

		events = FXCollections.observableArrayList()
		
		Sequence sequence = midi.getSequence()
		for(int idx = 0; idx < sequence.getTracks().size(); idx++) {
            Track track = sequence.getTracks()[idx]
            for(int i = 0; i < track.size(); i++) {
            	MidiEventItem item = new MidiEventItem()
                MidiEvent event = track.get(i)
                long tick = event.getTick()
                item.setTrack(i)
                item.setTick(tick)
                
                if (event.getMessage() instanceof ShortMessage) {
                    ShortMessage message = event.getMessage() as ShortMessage
                    int channel = message.getChannel()
                    item.setChannel(channel)
                    item.setData1((int)message.getData1())
                    item.setData2((int)message.getData2())
                    switch (message.getCommand()) {
                        case ShortMessage.PROGRAM_CHANGE:   // 0xC0, 192
                            item.setCommand("PROGRAM_CHANGE")
                            break
                        case ShortMessage.CONTROL_CHANGE:       // 0xB0, 176
                            item.setCommand("CONTROL_CHANGE")
                            break
                        case ShortMessage.NOTE_ON:              // 0x90, 144
                            item.setCommand("NOTE_ON")
                            break;
                        case ShortMessage.NOTE_OFF:    // 0x80, 128
                            item.setCommand("NOTE_OFF")
                            break;
                        default : 
                            item.setCommand("" + message.getCommand())
                            break
                    }
                }
                if (item.getTick() >= startTick && item.getTick() <= endTick)
                	events.add(item)
            }
        }
        tableView.setItems(events)
	}

}