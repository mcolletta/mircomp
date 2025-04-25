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

package io.github.mcolletta.mirmidi

import javax.sound.midi.*

import java.util.concurrent.ConcurrentSkipListMap

import javafx.beans.property.IntegerProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.LongProperty
import javafx.beans.property.SimpleLongProperty
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.ReadOnlyObjectProperty;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import javafx.collections.transformation.SortedList
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

import javafx.event.EventHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import groovy.transform.TupleConstructor
import groovy.transform.ToString


class MidiManager {

    final static int MESSAGE_TEMPO_TYPE = 0x51  // decimal 81

    Sequencer sequencer
    Sequence sequence
    Synthesizer synthesizer
    
    //long playbackPosition = 0L
    LongProperty playbackPosition = new SimpleLongProperty(0L)
    final long getPlaybackPosition() { return playbackPosition.get() }
    final void setPlaybackPosition(long value) { playbackPosition.set(value) }
    LongProperty playbackPositionProperty() { return playbackPosition }

    int timerRate = 50
    private volatile PlaybackThread  playbackThread
    private List<MidiPlaybackListener> listeners = []
    
    IntegerProperty resolution = new SimpleIntegerProperty(480)

    final int getResolution() { return resolution.get() }
    final void setResolution(int value) { resolution.set(value) }
    IntegerProperty resolutionProperty() { return resolution }

    ObservableMap<Integer,Integer> usedChannels = FXCollections.observableMap( [:].withDefault() { 0 } )
    StringProperty channelMask = new SimpleStringProperty("0000000000000000")
    
    ObservableList<MidiNote> notes
    SortedList<MidiNote> sortedByEndNotes
    SortedList<MidiNote> sortedByDurationNotes  // longest duration in O(1)
    Map<Integer,Map<Integer,ObservableMap<Long,MidiCC>>> controllers  // [controller][channel][tick] = MidiCC
    Map<Integer,ObservableMap<Long,MidiPC>> programs  // [channel][tick] = MidiPC
    ObservableMap<Long,MidiTempo> tempi

    long length = 0

    MidiEdit currentEdit
    List<MidiEdit> editHistory = []

    int savedEditHistoryHashCode
    BooleanProperty clean = new SimpleBooleanProperty(true)
    final boolean isClean() { return clean.get() }
    final void setClean(boolean value) { clean.set(value) }
    BooleanProperty cleanProperty() { return clean }

    void markClean() {
        savedEditHistoryHashCode = editHistory[0..getEditIndex()].hashCode()
        //println savedEditHistoryHashCode
        setClean(true)
    }

    void checkCleanState() {
        int hashc = 0
        int idx = getEditIndex()
        if (idx >= 0)
            hashc =  editHistory[0..idx].hashCode()
        /*println "editIndex " + getEditIndex() + " savedEditHistoryHashCode " + savedEditHistoryHashCode
        println "editHistory[0..idx].hashCode() " + editHistory[0..idx].hashCode()*/
        if (savedEditHistoryHashCode == hashc)
            setClean(true)
        else
            setClean(false)
    }

    IntegerProperty editIndex = new SimpleIntegerProperty(-1)
    final int getEditIndex() { return editIndex.get() }
    final void setEditIndex(int value) { 
        editIndex.set(value)
        checkCleanState()
    }
    IntegerProperty editIndexProperty() { return editIndex }

    boolean isParsing

    int currentTrack = 0
    int currentChannel = 0
    int currentController = 7

    Map<Integer,MidiControllerInfo> controllersInfo = [:]

    Comparator<MidiNote> sortByEndComparator = { MidiNote left, MidiNote right -> return (int)(left.getEnd() - right.getEnd())} as Comparator<MidiNote> 
    Comparator<MidiNote>  sortByDurationComparator = { MidiNote left, MidiNote right -> return (int)(left.getDuration() - right.getDuration())} as Comparator<MidiNote> 


    ListChangeListener<MidiNote> noteListener = { ListChangeListener.Change<? extends MidiNote> change ->
                if (!isParsing) {
                    while (change.next()) {
                        if (change.wasPermutated()) {
                            for (int i = change.getFrom(); i < change.getTo(); ++i) {
                                //println "permutate"
                            }
                        } else if (change.wasUpdated()) {
                            //println "update"
                        } else if (change.wasRemoved()) {
                            for (MidiNote note : change.getRemoved()) {
                                updateSequenceRemovedNote(note)
                                if (currentEdit) 
                                    currentEdit.noteRemoved.add(note)
                            }
                        } else if (change.wasAdded()) {
                            for (MidiNote note : change.getAddedSubList()) {
                                updateSequenceAddedNote(note)
                                if (currentEdit) 
                                    currentEdit.noteInserted.add(note)
                            }
                        }
                    }
                }
            } as ListChangeListener<MidiNote>

    MapChangeListener<Long, MidiCC> controllerListener = new MapChangeListener<Long, MidiCC>() {
                        @Override
                        public void onChanged(MapChangeListener.Change<? extends Long, ? extends MidiCC> change) {
                            if (!isParsing) {
                                if (change.wasAdded()) {
                                    updateSequenceAddedCC(change.getValueAdded())
                                    if (currentEdit) 
                                        currentEdit.ccInserted.add(change.getValueAdded())
                                }
                                if (change.wasRemoved()) {
                                    updateSequenceRemovedCC(change.getValueRemoved())
                                    if (currentEdit) 
                                        currentEdit.ccRemoved.add(change.getValueRemoved())
                                }
                            }
                        }
                    }

    MapChangeListener<Long, MidiPC> programListener = new MapChangeListener<Long, MidiPC>() {
                        @Override
                        public void onChanged(MapChangeListener.Change<? extends Long, ? extends MidiPC> change) {
                            if (!isParsing) {
                                if (change.wasAdded()) {
                                    updateSequenceAddedPC(change.getValueAdded())
                                    if (currentEdit) 
                                        currentEdit.pcInserted.add(change.getValueAdded())
                                }
                                if (change.wasRemoved()) {
                                    updateSequenceRemovedPC(change.getValueRemoved())
                                    if (currentEdit) 
                                        currentEdit.pcRemoved.add(change.getValueRemoved())
                                }
                            }
                        }
                    }


    MapChangeListener<Long, MidiTempo> tempoListener = new MapChangeListener<Long, MidiTempo>() {
                        @Override
                        public void onChanged(MapChangeListener.Change<? extends Long, ? extends MidiTempo> change) {
                            if (!isParsing) {
                                if (change.wasAdded()) {
                                    updateSequenceAddedTempo(change.getValueAdded())
                                    if (currentEdit) 
                                        currentEdit.tempoInserted.add(change.getValueAdded())
                                }
                                if (change.wasRemoved()) {
                                    updateSequenceRemovedTempo(change.getValueRemoved())
                                    if (currentEdit) 
                                        currentEdit.tempoRemoved.add(change.getValueRemoved())
                                }
                            }
                        }
                    }

    MapChangeListener<Integer,Integer> usedChannelsListener = new MapChangeListener<Integer,Integer>() {
                        @Override
                        public void onChanged(MapChangeListener.Change<? extends Integer, ? extends Integer> change) {
                            int newValue = -1
                            int oldValue = -1
                            if (change.wasAdded()) {
                                newValue = change.getValueAdded()
                            }
                            if (change.wasRemoved()) {
                                 oldValue = change.getValueRemoved()
                            }
                            if ((newValue > 0 && oldValue == 0) || (newValue == 0 && oldValue > 0)) {
                                int k = (int) change.getKey()
                                String mask = channelMask.get()
                                String newChar = (newValue == 0) ? '0' : '1'
                                String newMask = mask.substring(0,k)+ newChar + mask.substring(k+1)
                                channelMask.set(newMask)
                            }
                        }
                    }


    MidiManager(Synthesizer synth=null){
        initMidi(synth)
        loadSequence()
        // ctype: 0 curve, 1 on/off
        controllersInfo[1] = new MidiControllerInfo(info:"Modulation", value:1, ctype: 0)
        controllersInfo[7] = new MidiControllerInfo(info:"Volume", value:7, ctype: 0)
        controllersInfo[8] = new MidiControllerInfo(info:"Balance", value:8, ctype: 0)
        controllersInfo[10] = new MidiControllerInfo(info:"Pan", value:10, ctype: 0)
        controllersInfo[11] = new MidiControllerInfo(info:"Expression", value:11, ctype: 0)
        controllersInfo[64] = new MidiControllerInfo(info:"Sustain", value:64, ctype: 1)
        controllersInfo[65] = new MidiControllerInfo(info:"Portamento", value:65, ctype: 1)
        controllersInfo[66] = new MidiControllerInfo(info:"Sostenuto", value:66, ctype: 1)
        controllersInfo[67] = new MidiControllerInfo(info:"Soft Pedal", value:67, ctype: 1)
        controllersInfo[68] = new MidiControllerInfo(info:"Legato", value:68, ctype: 1)
        // Sound Controllers
        controllersInfo[70] = new MidiControllerInfo(info:"Sound Variation", value:70, ctype: 0)
        controllersInfo[71] = new MidiControllerInfo(info:"Timbre Intensity", value:71, ctype: 0)
        controllersInfo[72] = new MidiControllerInfo(info:"Release Time", value:72, ctype: 0)
        controllersInfo[73] = new MidiControllerInfo(info:"Attack Time", value:73, ctype: 0)
        controllersInfo[74] = new MidiControllerInfo(info:"Brightness", value:74, ctype: 0)
        controllersInfo[75] = new MidiControllerInfo(info:"Decay Time", value:75, ctype: 0)
        controllersInfo[76] = new MidiControllerInfo(info:"Vibrato Rate", value:76, ctype: 0)
        controllersInfo[77] = new MidiControllerInfo(info:"Vibrato Depth", value:77, ctype: 0)
        controllersInfo[78] = new MidiControllerInfo(info:"Vibrato Delay", value:78, ctype: 0)
    }

    // TODO get synthesizer from outside
    void initMidi(Synthesizer synth=null) {
        try {
            sequencer =  MidiSystem.getSequencer(false)
            if (sequencer == null) {
                println("Sequencer not found from MidiSystem")
                System.exit(0)
            }            
            if (synth == null)
                synthesizer = MidiSystem.getSynthesizer()
            else
                synthesizer = synth
            sequencer.open()
            if (!synthesizer.isOpen())
                synthesizer.open()            
            Receiver synthReceiver = synthesizer.getReceiver()
            Transmitter seqTransmitter = sequencer.getTransmitter()
            seqTransmitter.setReceiver(synthReceiver)
            
        } catch(MidiUnavailableException e) {
            println("No sequencer available")
            System.exit(0)
        } catch(Exception e) {
            e.printStackTrace()
        }
    }

    void setSynthesizer(Synthesizer synth) {
        initMidi(synth)
    }

    boolean loadMidi(String path) {
        File midiFile = new File(path)
        return loadMidi(midiFile)
    }

    boolean loadMidi(File midiFile) {
        try {
            Sequence sequence = MidiSystem.getSequence(midiFile)
            loadSequence(sequence)
            return true
        } catch (InvalidMidiDataException ex) {
            return false
        }
    }

    void loadSequence(Sequence sequence=null, int tracksCount=1) {
        try {
            if (sequence == null) {
                try {
                    sequence = new Sequence(Sequence.PPQ, getResolution())
                } catch (InvalidMidiDataException e) {
                    e.printStackTrace()
                }
                for (int i = 0; i < tracksCount; i++) {
                    sequence.createTrack()
                }
            }
            this.sequence = sequence
            sequencer.setSequence(sequence)
            parseEvents()
            setSequenceResolution()
        } catch(Exception e) {
            e.printStackTrace()
        }
    }

    void addTrackToSequence() {
        if (sequence != null) {
            sequence.createTrack()
            sequencer.setSequence(sequence)
        }
    }

    void setSequenceResolution()
    {
        if (sequence != null) {
            setResolution(sequence.getResolution())
        }
    }

    void setSequencerBPM(float bpm) {
        if (sequencer != null)
            sequencer.setTempoInBPM(bpm)
    }

    float getSequencerBPM() {
        if (sequencer != null) {
            return sequencer.getTempoInBPM()
        }
        return 120f
    }

    void updateSequenceAddedNote(MidiNote note) {
        Track t = sequence.tracks[note.track]
        t.add(note.startEvent)
        t.add(note.endEvent)
        usedChannels[note.getChannel()] += 1
    }

    void updateSequenceRemovedNote(MidiNote note) {
        Track t = sequence.tracks[note.track]
        t.remove(note.startEvent)
        t.remove(note.endEvent)
        usedChannels[note.getChannel()] -= 1
    }

    void updateSequenceAddedCC(MidiCC cc) {
        Track t = sequence.tracks[cc.track]
        t.add(cc.midiEvent)
        usedChannels[cc.getChannel()] += 1
    }

    void updateSequenceRemovedCC(MidiCC cc) {
        if (cc != null) {
            Track t = sequence.tracks[cc.track]
            t.remove(cc.midiEvent)
            usedChannels[cc.getChannel()] -= 1
        }
    }

    void updateSequenceAddedPC(MidiPC pc) {
        Track t = sequence.tracks[pc.track]
        t.add(pc.midiEvent)
        usedChannels[pc.getChannel()] += 1
    }

    void updateSequenceRemovedPC(MidiPC pc) {
        if (pc != null) {
            Track t = sequence.tracks[pc.track]
            t.remove(pc.midiEvent)
            usedChannels[pc.getChannel()] -= 1
        }
    }

    void updateSequenceAddedTempo(MidiTempo tempo) {
        Track t = sequence.tracks[tempo.track]
        t.add(tempo.midiEvent)
    }

    void updateSequenceRemovedTempo(MidiTempo tempo) {
        if (tempo != null) {
            Track t = sequence.tracks[tempo.track]
            t.remove(tempo.midiEvent)
        }
    }

    MidiNote createMidiNote(int channel, int track, long start, long end, int pitch, int attachVelocity, int decayVelocity=0) {
        ShortMessage msg = new ShortMessage()
        msg.setMessage(ShortMessage.NOTE_ON, channel, pitch, attachVelocity)
        MidiEvent startEvt = new MidiEvent(msg, start)
        
        msg = new ShortMessage()
        if (decayVelocity > 0)
            msg.setMessage(ShortMessage.NOTE_OFF, channel, pitch, decayVelocity)
        else
            msg.setMessage(ShortMessage.NOTE_ON, channel, pitch, 0)
        MidiEvent endEvt = new MidiEvent(msg, end)

        MidiNote note = new MidiNote(startEvent:startEvt, endEvent:endEvt, track:track)
        return note
    }

    void addMidiNote(MidiNote note) {
        notes.add(note)
    }

    void removeMidiNote(MidiNote note) {
        notes.remove(note)
    }

    MidiCC createMidiCC(int channel, int track, long tick, int data1, int data2) {
        Track t = sequence.tracks[track]
        ShortMessage msg = new ShortMessage()
        //msg.setMessage(ShortMessage.CONTROL_CHANGE, channel, 7, val) // 7 is coarse volume
        int command = ShortMessage.CONTROL_CHANGE
        msg.setMessage(command, channel, data1, data2)
        MidiEvent midiEvt = new MidiEvent(msg, tick)
        MidiCC cc = new MidiCC(midiEvent:midiEvt, track:track)
        return cc
    }

    void addMidiCC(MidiCC cc) {
        this.controllers[cc.controller][cc.channel][cc.tick] = cc
    }

    void removeMidiCC(MidiCC cc) {
        this.controllers[cc.controller][cc.channel].remove(cc.tick)
    }

    MidiPC createMidiPC(int channel, int track, long tick, int data1) {
        Track t = sequence.tracks[track]
        ShortMessage msg = new ShortMessage()
        int command = ShortMessage.PROGRAM_CHANGE
        msg.setMessage(command, channel, data1, 0)
        MidiEvent midiEvt = new MidiEvent(msg, tick)
        MidiPC pc = new MidiPC(midiEvent:midiEvt, track:track)
        return pc
    }

    void addMidiPC(MidiPC pc) {
        this.programs[pc.channel][pc.tick] = pc
    }

    void removeMidiPC(MidiPC pc) {
        this.programs[pc.channel].remove(pc.tick)
    }

    MidiTempo createMidiTempo(int track, long tick, int bpm) {
        int mpq = (int) (60000000.0D / bpm)
        byte[] data = new byte[3]
        data[0] = (byte) ((mpq >> 16) & 0xFF)
        data[1] = (byte) ((mpq >> 8) & 0xFF)
        data[2] = (byte) (mpq & 0xFF)
        MetaMessage msg = new MetaMessage()
        msg.setMessage(MESSAGE_TEMPO_TYPE, data, data.length)
        MidiEvent midiEvt = new MidiEvent(msg, tick)
        MidiTempo tempo = new MidiTempo(midiEvent:midiEvt, track:track)
        return tempo
    }

    void addMidiTempo(MidiTempo tempo) {
        this.tempi[tempo.tick] = tempo
    }

    void removeMidiTempo(MidiTempo tempo) {
        this.tempi.remove(tempo.tick)
    }

    long getLength() {
        if (sequence != null) {
            return sequence.getTickLength()
        }
        return 0
    }

    long getLongestDuration() {
        if (sortedByDurationNotes.size() > 0)
            return sortedByDurationNotes[sortedByDurationNotes.size()-1].getDuration()
        return 0
    }

    int getStartNoteIndex(long x) {
        // dummy event for search
        ShortMessage msg = new ShortMessage()
        msg.setMessage(ShortMessage.NOTE_OFF, 0, 0, 0)
        MidiEvent event = new MidiEvent(msg, x)
        MidiNote needle = new MidiNote(startEvent:event, endEvent:event, track:0)
        int idx = Collections.binarySearch(sortedByEndNotes, needle, sortByEndComparator)
        return idx
    }

    int getStartCCIndex(long x, ObservableMap<Long, MidiCC> om) {
        List<Long> keys = om.keySet() as List<Long>
        int idx = Collections.binarySearch(keys, x)
        return idx
    }

    int getStartPCIndex(long x, ObservableMap<Long, MidiPC> om) {
        List<Long> keys = om.keySet() as List<Long>
        int idx = Collections.binarySearch(keys, x)
        return idx
    }

    int getStartTempoIndex(long x, ObservableMap<Long, MidiTempo> om) {
        List<Long> keys = om.keySet() as List<Long>
        int idx = Collections.binarySearch(keys, x)
        return idx
    }

    void parseEvents() {
        isParsing = true

        notes = FXCollections.observableArrayList()
        notes.addListener(noteListener)
        sortedByEndNotes = new SortedList<>(notes)
        sortedByEndNotes.comparatorProperty().set(sortByEndComparator)
        sortedByDurationNotes = notes.sorted(sortByDurationComparator)

        // controllers[7][0][1200] = createMidiCC(<127>)  set the volume (CC#7) of channel 0 at tick 1200 to 127
        controllers = [:].withDefault { // controller, ex. 7 is volume
            [:].withDefault { // channel
                ObservableMap<Long, MidiCC> map = FXCollections.observableMap( new ConcurrentSkipListMap<Long, MidiCC>() )
                map.addListener(controllerListener)
                return map
            } 
        }

        programs = [:].withDefault { // channel
                ObservableMap<Long, MidiPC> map = FXCollections.observableMap( new ConcurrentSkipListMap<Long, MidiPC>() )
                map.addListener(programListener)
                return map
            }

        tempi = FXCollections.observableMap( new ConcurrentSkipListMap<Long, MidiTempo>() )
        tempi.addListener(tempoListener)

        usedChannels = FXCollections.observableMap( [:].withDefault() { 0 } )
        usedChannels.addListener(usedChannelsListener)
        channelMask.set("0000000000000000")

        Map<Integer,Map<Integer,MidiEvent>> cache = [:].withDefault() { [:] }
        for(int idx = 0; idx < sequence.getTracks().size(); idx++) {
            Track track = sequence.getTracks()[idx]
            for(int i = 0; i < track.size(); i++) {
                MidiEvent event = track.get(i)
                long tick = event.getTick()
                //MidiMessage message = event.getMessage()
                if (event.getMessage() instanceof ShortMessage) {
                    ShortMessage message = event.getMessage() as ShortMessage
                    int channel = message.getChannel()
                    usedChannels[channel] += 1
                    switch (message.getCommand()) {
                        case ShortMessage.PROGRAM_CHANGE:   // 0xC0, 192
                            MidiPC pc = new MidiPC(midiEvent:event, track:idx)
                            programs[pc.channel][pc.tick] = pc
                            break
                        case ShortMessage.CONTROL_CHANGE:   // 0xB0, 176
                            MidiCC cc = new MidiCC(midiEvent:event, track:idx)
                            controllers[cc.controller][cc.channel][cc.tick] = cc
                            break
                        case ShortMessage.NOTE_ON:   // 0x90, 144
                            if (message.getData2() == 0) {
                                // A velocity of zero in a note-on event is a note-off event
                                MidiEvent evt = cache[channel][message.getData1()]
                                MidiNote note = new MidiNote(startEvent:evt, endEvent:event, track:idx)
                                notes.add(note)
                            } else {
                                cache[channel][message.getData1()] = event
                            }
                            break;
                        case ShortMessage.NOTE_OFF:    // 0x80, 128
                            MidiEvent evt = cache[channel][message.getData1()]
                            if (evt != null) {
                                MidiNote note = new MidiNote(startEvent:evt, endEvent:event, track:idx)
                                notes.add(note)
                            } else {
                                throw new Exception("NOTE_OFF event without NOTE_ON during the parsing of midi file")
                            }
                            break;
                        default : 
                            //println "Unparsed message: " + message.command
                            break
                    }
                }
                if (event.getMessage() instanceof MetaMessage) {
                    MetaMessage message = event.getMessage() as MetaMessage
                    switch (message.getType()) {
                        case MESSAGE_TEMPO_TYPE:
                            MidiTempo mt = new MidiTempo(midiEvent:event, track:idx)
                            break
                        default:
                            break
                    }
                }
            }
        }
        
        isParsing = false
    }

    // SAVE

    void saveAs(File selectedFile) {
        int type = getPreferredMidiType(sequence)
        MidiSystem.write(sequencer.getSequence(), type, selectedFile)
    }

    static int getPreferredMidiType(Sequence sequence) {
        int[] types = MidiSystem.getMidiFileTypes(sequence)
        int type = 0;
        if (types.length != 0) {
            type = types[types.length - 1]
        }
        return type;
    }

    // PLAYBACK

    void registerListener(MidiPlaybackListener listener) {
        listeners.add(listener)
    }

    void play() {
        if (sequencer.getSequence() != null) {
            sequencer.start()
            // println "sequence started"
            if (playbackThread != null)
                playbackThread.stopPlayback()
            playbackThread = new PlaybackThread()
            playbackThread.start()
            for (MidiPlaybackListener listener : listeners) {
                listener.playbackStarted()
            }
        }
    }

    void pause() {
        if (sequencer.isRunning()) {
            sequencer.stop()
            if (playbackThread != null)
                playbackThread.stopPlayback()
            for (MidiPlaybackListener listener : listeners) {
                listener.playbackPaused()
            }
        }
    }

    void stop() {
        if (sequencer.isRunning()) {
            sequencer.stop()
            if (playbackThread != null)
                playbackThread.stopPlayback()
        }
        setPlaybackPosition(0L)
        sequencer.setTickPosition(getPlaybackPosition())
        for (MidiPlaybackListener listener : listeners) {
            listener.playbackStopped()
        }
    }

    void close() {
		if (sequencer != null) {
            if (sequencer.isRunning())
			    sequencer.stop();
			sequencer.close();
		}
	}

    private class PlaybackThread extends Thread {

        private boolean stop = false
        
        public PlaybackThread() {}

        @Override public void run() {
            try {
                sequencer.setTickPosition(getPlaybackPosition())
                while (sequencer.isRunning() && !stop) {
                    long tick = sequencer.getTickPosition()
                    for (MidiPlaybackListener listener : listeners) {
                        listener.playbackAtTick(tick)
                    }
                    Thread.sleep((int)(1000 / timerRate))
                }
                sequencer.stop()
            } catch (InterruptedException e) {
            }
        }

        public void stopPlayback() {
            stop = true
        }
    }


    // UNDOABLE

    private class MidiEdit {

        List<MidiNote> noteInserted = []
        List<MidiNote> noteRemoved = []
        List<MidiCC> ccInserted = []
        List<MidiCC> ccRemoved = []
        List<MidiPC> pcInserted = []
        List<MidiPC> pcRemoved = []
        List<MidiTempo> tempoInserted = []
        List<MidiTempo> tempoRemoved = []

        void undo() {
            // ORDER IS IMPORTANT: reverse respect to the action
            for(MidiNote note : noteInserted) {
                removeMidiNote(note)
            }
            for(MidiNote note : noteRemoved) {
                addMidiNote(note)
            }
            for(MidiCC cc : ccInserted) {
                removeMidiCC(cc)
            }
            for(MidiCC cc : ccRemoved) {
                addMidiCC(cc)
            }
            for(MidiPC pc : pcInserted) {
                removeMidiPC(pc)
            }
            for(MidiPC pc : pcRemoved) {
                addMidiPC(pc)
            }
            for(MidiTempo tempo : tempoInserted) {
                removeMidiTempo(tempo)
            }
            for(MidiTempo tempo : tempoRemoved) {
                addMidiTempo(tempo)
            }
        }

        void redo() {
            for(MidiNote note : noteRemoved) {
                removeMidiNote(note)
            }
            for(MidiNote note : noteInserted) {
                addMidiNote(note)
            }
            for(MidiCC cc : ccRemoved) {
                removeMidiCC(cc)
            }
            for(MidiCC cc : ccInserted) {
                addMidiCC(cc)
            }
            for(MidiPC pc : pcRemoved) {
                removeMidiPC(pc)
            }
            for(MidiPC pc : pcInserted) {
                addMidiPC(pc)
            }
            for(MidiTempo tempo : tempoRemoved) {
                removeMidiTempo(tempo)
            }
            for(MidiTempo tempo : tempoInserted) {
                addMidiTempo(tempo)
            }
        }
    }

    void undo() {
        if (editHistory.size() > 0 && getEditIndex() > -1) {
            editHistory[getEditIndex()].undo()
            setEditIndex(getEditIndex() - 1)
        }
    }

    boolean hasUndo() {
        return (editHistory.size() > 0 && getEditIndex() > -1)
    }

    void redo() {
        if (getEditIndex() < editHistory.size()-1) {
            setEditIndex(getEditIndex() + 1)
            editHistory[getEditIndex()].redo()
        }
    }

    boolean hasRedo() {
        return (editHistory.size() > 0 && getEditIndex() < (editHistory.size()-1))
    }

    void startEdit() {
        currentEdit = new MidiEdit()
        if (editHistory.size() > 0 && getEditIndex() < (editHistory.size()-1))
            editHistory = editHistory - editHistory[getEditIndex()+1..editHistory.size()-1]
    }

    void stopEdit() {
        editHistory.add(currentEdit)
        setEditIndex(getEditIndex() + 1)
        currentEdit = null
    }
}


//@ToString(includeNames=true, includeFields=true, excludes='startEvent,endEvent')
@TupleConstructor // in addition to default name-arg constructor
class MidiNote {

    MidiEvent startEvent 
    MidiEvent endEvent   

    int track
    int pitch
    int attachVelocity
    int decayVelocity

    long getStart() {
        return startEvent.getTick()
    }

    long getEnd() {
        return endEvent.getTick()
    }

    int getPitch() {
        ShortMessage message = startEvent.getMessage() as ShortMessage
        int pitch = message.getData1()
        return pitch
    }

    int getVelocity() {
        ShortMessage message = startEvent.getMessage() as ShortMessage
        int velocity = message.getData2()
        return velocity
    }

    int getChannel() {
        ShortMessage message = startEvent.getMessage() as ShortMessage
        int channel = message.getChannel()
        return channel
    }

    long getDuration() {
        return getEnd() - getStart()
    }

    String toString() {
        return "pitch:" + getPitch() + "   start:" + getStart() + "   end:" + getEnd()
    }

}

@ToString(includeNames=true, includeFields=true, excludes='midiEvent')
@TupleConstructor
class MidiData {

    MidiEvent midiEvent

    int getChannel() {
        ShortMessage message = midiEvent.getMessage() as ShortMessage
        int channel = message.getChannel()
        return channel
    }

    long getTick() {
        return midiEvent.getTick()
    }

    int track
}

class MidiCC extends MidiData {

    int getController() {
        ShortMessage message = midiEvent.getMessage() as ShortMessage
        int data1 = message.getData1()
        return data1
    }

    int getValue() {
        ShortMessage message = midiEvent.getMessage() as ShortMessage
        int data2 = message.getData2()
        return data2
    }

    String toString() {
        return "CC#" + getController() + "=" + getValue() + "[" + getTick() + "] channel=" + getChannel()
    }

}

class MidiPC extends MidiData {

    int getInstrument() {
        ShortMessage message = midiEvent.getMessage() as ShortMessage
        int data1 = message.getData1()
        return data1
    }

    String toString() {
        return "Program=" + getInstrument() + "[" + getTick() + "] channel=" + getChannel()
    }

}

@TupleConstructor
class MidiControllerInfo {
    String info
    int value
    int ctype

    String toString() {
        return info
    }
}

@ToString(includeNames=true, includeFields=true, excludes='midiEvent')
@TupleConstructor
class MidiMeta {

    MidiEvent midiEvent

    int getType() {
        MetaMessage message = midiEvent.getMessage() as MetaMessage
        int type = message.getType()
        return type
    }

    long getTick() {
        return midiEvent.getTick()
    }

    int track
}

class MidiTempo extends MidiMeta {

    int getBPM() {
        MetaMessage message = midiEvent.getMessage() as MetaMessage
        byte[] data = message.getData()
        // microseconds per quarter
        int mpq = (data[0] & 0xff) << 16 | (data[1] & 0xff) << 8 | (data[2] & 0xff)
        // beats per minute
        int bpm = (int) (60000000.0D / mpq)
        return bpm
    }

    String toString() {
        return "BPM=" + getBPM() + "[" + getTick() + "]"
    }

}

interface MidiPlaybackListener {

    static final int timerRate = 50

    void playbackAtTick(long tick)

    void playbackStarted()

    void playbackPaused()

    void playbackStopped()

    void playbackAtEnd()

}
