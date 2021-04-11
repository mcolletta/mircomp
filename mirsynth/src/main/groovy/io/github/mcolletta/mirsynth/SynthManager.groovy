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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sound.midi.ControllerEventListener;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiDevice.Info;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

import com.sun.media.sound.AudioSynthesizer;
import com.xenoage.utils.exceptions.InvalidFormatException;

/**
 * This class is based on the Zong! SynthManager
 * see the Zong! project for details
 */
public class SynthManager {


	private AudioSynthesizer synthesizer = null;
	private Soundbank soundbank = null;

	private SourceDataLine line = null;
	private AudioFormat format = null;

	public Soundbank getSoundbank() {
		return soundbank;
	}

	public Synthesizer getSynthesizer() {
		return synthesizer;
	}

	public SynthManager(float sampleRate=44100, int sampleSizeInBits=16,
                        int channels=2, int latency=100, int polyphony=64,
                        String deviceName=null, String interpolation="linear")
		    throws MidiUnavailableException {


		format = new AudioFormat(sampleRate, sampleSizeInBits, channels, true, false);

		Map<String, Object> ainfo = new HashMap<String, Object>();
		ainfo.put("format", format);
		ainfo.put("max polyphony", polyphony);
		ainfo.put("latency", latency * 1000L);

		ainfo.put("interpolation", interpolation);
		ainfo.put("large mode", true);

		var synth = MidiSystem.getSynthesizer()
		if (synth instanceof AudioSynthesizer)
			synthesizer =  (AudioSynthesizer) synth
		else {
			var availableSynths = MidiSystem.getMidiDeviceInfo()
											.stream()
											.filter( { Info info ->  MidiSystem.getMidiDevice(info) instanceof AudioSynthesizer } )
											.map( { Info info -> (AudioSynthesizer) (MidiSystem.getMidiDevice(info)) } )
											.toArray() as AudioSynthesizer[]
			if (availableSynths.size() > 0) {
				synthesizer = availableSynths[0]
			}
		}

		line = AudioSystem.getSourceDataLine(format)
		synthesizer.open(line, ainfo)

		if (soundbank == null) {
			soundbank = synthesizer.getDefaultSoundbank();
		}
	}

    public void close() {
		if (synthesizer != null) {
			synthesizer.close();
		}
	}

	public void loadSoundbank(File file)
		throws InvalidFormatException {
		try {
			FileInputStream fis = new FileInputStream(file);
			Soundbank newSB;
			try {
				newSB = MidiSystem.getSoundbank(new BufferedInputStream(fis));
			} finally {
				fis.close();
			}
			if (soundbank != null)
				synthesizer.unloadAllInstruments(soundbank);
			soundbank = newSB;
			synthesizer.loadAllInstruments(soundbank);
		} catch (Exception ex) {
			throw new InvalidFormatException("Invalid soundbank: " + file, ex);
		}
	}
	
}