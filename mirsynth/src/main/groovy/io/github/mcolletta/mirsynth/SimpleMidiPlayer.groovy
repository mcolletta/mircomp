/*
 * Copyright (C) 2016-2021 Mirco Colletta
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

package io.github.mcolletta.mirsynth

import javax.sound.midi.*


public class SimpleMidiPlayer {

    private static float A4_FREQ = 440f
    private static int A4_KEY = 69

    private Synthesizer synthesizer
    private Sequencer sequencer
    private int resolution
    private float bpm
    private float ticksPerMs

    public SimpleMidiPlayer(Synthesizer synth, int res=480) {
        synthesizer = synth;
        sequencer = MidiSystem.getSequencer(false);
        sequencer.getTransmitter().setReceiver(synthesizer.getReceiver());
        resolution = res;
        sequencer.open();
        bpm = sequencer.getTempoInBPM()
        ticksPerMs = (float) ( resolution * (bpm / 60000.0f) );
    }

    public void play(List<MidiEvent> events) {
        Sequence sequence = new Sequence(Sequence.PPQ, resolution)
        Track track = sequence.createTrack()
        for(MidiEvent event: events) {
            track.add(event)
        }
        stop()
        sequencer.setSequence(sequence);
        sequencer.start();
        println("SimpleMidiPlayer.play started");
    }

    public void playFrequencies(List<Float> frequencies, List<Float> durations) {
        int dur_size = durations.size()
        Sequence sequence = new Sequence(Sequence.PPQ, resolution)
        Track track = sequence.createTrack()

        int channel = 0
        int velocity = 90

        long tick = 0
        for(int i = 0; i < frequencies.size(); i++) {
            float freq = frequencies[i]
            int j = Math.min(i, dur_size-1)
            long duration = convertMsToTicks(durations[j])

            int key = getBaseKey(freq)
            float baseFreq = getFrequency(key)
            float centsInterval = getCentsInterval(baseFreq, freq)
            // 16384
            int tuning = (int) (8192.0d + (centsInterval * 8192.0f / 100d))
            ShortMessage CC = getPitchBendMessage(tuning)
            track.add(new MidiEvent(CC, tick));
            ShortMessage noteOn = new ShortMessage(ShortMessage.NOTE_ON, channel, key, velocity);
            track.add(new MidiEvent(noteOn, tick))
            tick += duration
            ShortMessage noteOff = new ShortMessage(ShortMessage.NOTE_OFF, channel, key, 0)
            track.add(new MidiEvent(noteOff, tick))
        }
        stop()
        sequencer.setSequence(sequence);
        sequencer.start();
        println("SimpleMidiPlayer.playFrequencies started");
    }

    void stop() {
        if (sequencer != null) {
            if (sequencer.isRunning())
			    sequencer.stop();
		}
    }

    void close() {
        if (sequencer != null) {
            if (sequencer.isRunning())
			    sequencer.stop();
			sequencer.close();
		}
    }

    // utils

    private long convertMsToTicks(float ms) {
        return (long) (ms * ticksPerMs)
    }

    static double log2(double x) {
        return Math.log(x) / Math.log(2)
    }

    static int getBaseKey(float freq) {
        return (int) Math.round((12f * log2(freq / A4_FREQ) + A4_KEY))
    }

    static float getFrequency(int keyNumber) {
        return (float) (A4_FREQ * Math.pow(2, (keyNumber - A4_KEY) / 12f))
    }

    // interval between f1 and f2 in cents
    // 100 cents is a semitone
    static float getCentsInterval(float f1, float f2) {        
        return (float) ( 1200f * log2(f2 / f1) )
    }

    byte[] getPitchBend(int value) {
        byte [] pitchBend = new byte [2];
        pitchBend[0] = (byte)(value % 128);
        pitchBend[1] = (byte)(value / 128);
        return pitchBend
    }

    ShortMessage getPitchBendMessage(int value) {
        var pitchBend = getPitchBend(value)
        return new ShortMessage(ShortMessage.PITCH_BEND, pitchBend[0], pitchBend[1]);
    }
}