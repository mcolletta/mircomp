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

package io.github.mcolletta.mirsynth

import io.github.mcolletta.mirsynth.*
import static io.github.mcolletta.mirsynth.Utils.*

import javax.sound.midi.*


class ExampleOscillators {
	
	public static void main(String[] args) throws Exception
	{
		MirOscillator oscillator = new DoubleFrequencyModulationOscillator()
		//MirOscillator oscillator = new FrequencyModulationOscillator()
		//MirOscillator oscillator = new SubtractiveOscillator()
		//MirOscillator oscillator = new AdditiveOscillator()
			
		//testSingleTone(60, oscillator)
		testSong(ExampleOscillators.class.getResource("resources/midi/Invention.mid").getPath(), oscillator)
		//testSong("/home/user/music/MIDI/song.mid", oscillator)
	}
	
	static void testSingleTone(int pitch=60, MirOscillator oscillator) {
		Synthesizer synthesizer = MidiSystem.getSynthesizer();
		synthesizer.open();
		
		synthesizer.unloadAllInstruments(synthesizer.getDefaultSoundbank());
		synthesizer.loadAllInstruments(oscillator);

		/*
		* Play midi note 60 on channel 1 for 1 sec.
		*/
		ShortMessage msg = new ShortMessage();
		Receiver recv = synthesizer.getReceiver();
		msg.setMessage(ShortMessage.PROGRAM_CHANGE, 0, 48, 0);
		recv.send(msg, 0);
		msg.setMessage(ShortMessage.NOTE_ON, 0, pitch, 80);
		recv.send(msg, 0);
		msg.setMessage(ShortMessage.NOTE_ON, 0, pitch, 0);
		recv.send(msg, 1000000);
		 
		Thread.sleep(2000)
		/*
		* Close all resources.
		*/
		synthesizer.close();
		
		System.exit(0)
	}
	
	static void testSong(String filePath, MirOscillator oscillator) {
		Synthesizer synth = MidiSystem.getSynthesizer();
		synth.open();
		synth.unloadAllInstruments(synth.getDefaultSoundbank());
		synth.loadAllInstruments(oscillator);
		
		Sequence seq = MidiSystem.getSequence(new File(filePath));
		
		Sequencer seqr = MidiSystem.getSequencer(false);
		seqr.open();
		seqr.getTransmitter().setReceiver(synth.getReceiver());
		seqr.setSequence(seq);
		seqr.start();
		
		System.out.println();
		System.out.println("Is active, press enter to stop");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		br.readLine();
		System.out.println("Stop...");
		
		seqr.stop();
		seqr.close();
		synth.close();
		
		System.exit(0);
	}

}
