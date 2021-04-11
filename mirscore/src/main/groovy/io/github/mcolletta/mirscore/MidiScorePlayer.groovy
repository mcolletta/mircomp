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

package io.github.mcolletta.mirscore

import java.util.List;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.ControllerEventListener;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;

import com.xenoage.utils.jse.collections.WeakList;
import com.xenoage.zong.core.Score;
import com.xenoage.zong.core.position.MP;
import com.xenoage.zong.io.midi.out.MidiConverter;
import com.xenoage.zong.io.midi.out.MidiEvents;
import com.xenoage.zong.io.midi.out.MidiSequence;
import com.xenoage.zong.io.midi.out.MidiSettings;
import com.xenoage.zong.io.midi.out.MidiTime;
import com.xenoage.zong.io.midi.out.PlaybackListener;
import com.xenoage.zong.desktop.io.midi.out.JseMidiSequenceWriter


/**
 * This class is based on the Zong! MidiScorePlayer
 * see the Zong! project for details
 */
public class MidiScorePlayer implements ControllerEventListener {

	private Synthesizer synthesizer
    private Sequencer sequencer = null;
	private MidiSequence<Sequence> sequence = null;
	private WeakList<PlaybackListener> listeners = new WeakList<PlaybackListener>();
	private boolean metronomeEnabled;
	private float volume = new MidiSettings().getDefaultVolume();
	private int currentPosition;
	private volatile PlaybackThread  playbackThread;

	public MidiScorePlayer(Synthesizer synthesizer) {
		this.synthesizer = synthesizer

        sequencer = MidiSystem.getSequencer(false);
        sequencer.getTransmitter().setReceiver(synthesizer.getReceiver());
		sequencer.open();

		//controller events to listen for (see MidiEvents doc)
		sequencer.addControllerEventListener(this, new int[]{ MidiEvents.eventPlaybackControl });
		sequencer.addControllerEventListener(this, new int[]{ MidiEvents.eventPlaybackEnd } );

		setVolume(volume);
	}

	public void openScore(Score score) {
		stop();
		this.sequence = MidiConverter.convertToSequence(score, true, true, new JseMidiSequenceWriter());
		try {
			sequencer.setSequence(sequence.getSequence());
		} catch (InvalidMidiDataException ex) {
			println ex.getMessage()
		}
		applyVolume();
	}

	public void addPlaybackListener(PlaybackListener listener) {
		listeners.add(listener);
	}

	public void removePlaybackListener(PlaybackListener listener) {
		listeners.remove(listener);
	}


	public void setMicrosecondPosition(long ms) {
		sequencer.setMicrosecondPosition(ms);
		currentPosition = 0;
	}

	public void setMP(MP bmp) {
		long tickPosition = calculateTickFromMP(bmp, sequence.getMeasureStartTicks(), sequence.getSequence().getResolution());
		sequencer.setTickPosition(tickPosition);
		currentPosition = 0;
	}

	public void start() {
		if (sequencer.getSequence() != null) {
			sequencer.start();
			 if (playbackThread != null)
                playbackThread.stopPlayback()
            playbackThread = new PlaybackThread()
            playbackThread.start()
			for (PlaybackListener listener : listeners.getAll()) {
				listener.playbackStarted();
			}
			applyVolume();
		}
	}

	public void pause() {
		if (sequencer.isRunning()) {
			sequencer.stop();
			if (playbackThread != null)
                playbackThread.stopPlayback()
			for (PlaybackListener listener : listeners.getAll()) {
				listener.playbackPaused();
			}
		}
	}

	public void stop() {
		if (sequencer.isRunning()) {
			sequencer.stop();
			if (playbackThread != null)
                playbackThread.stopPlayback()
		}
		setMicrosecondPosition(0);
		currentPosition = 0;
		for (PlaybackListener listener : listeners.getAll()) {
			listener.playbackStopped();
		}
	}

	public boolean getMetronomeEnabled() {
		return metronomeEnabled;
	}

	public void setMetronomeEnabled(boolean metronomeEnabled) {
		this.metronomeEnabled = metronomeEnabled;
		Integer metronomeTrack = sequence.getMetronomeTrack();
		if (metronomeTrack != null)
			sequencer.setTrackMute(metronomeTrack, !metronomeEnabled);
	}

	private long calculateTickFromMP(MP pos, List<Long> measureTicks, int resolution) {
		if (pos == null) {
			return 0;
		}
		else {
			// needed for setMP in case of Repetitions
			List<MidiTime> timePool = sequence.getTimePool();
			MidiTime mtime = null;
			for(MidiTime mt : timePool) {
				if ((mt.mp.measure > pos.measure) ||
					((mt.mp.measure == pos.measure) && (mt.mp.beat.compareTo(pos.beat) > -1))) {
					mtime = mt;
					break;
				}
			}
			if (mtime != null) {
				if (mtime.mp.measure > pos.measure) {
					long diff = MidiConverter.calculateTickFromFraction(mtime.mp.beat, resolution);
					return mtime.tick - diff;
				} else { // same meaure
					if (mtime.mp.beat.compareTo(pos.beat) == 0) {
						return mtime.tick;
					} else {
						long diff = MidiConverter.calculateTickFromFraction(mtime.mp.beat.sub(pos.beat), resolution);
						return mtime.tick - diff;
					}
				}
			}
			// ---------------------------------------
			return measureTicks.get(pos.measure) +
				MidiConverter.calculateTickFromFraction(pos.beat, resolution);
		}
	}

	@Override public void controlChange(ShortMessage message) {
		List<MidiTime> timePool = sequence.getTimePool();
		if (message.getData1() == MidiEvents.eventPlaybackControl) {
			//calls the listener with the most actual tick
			long currentTick = sequencer.getTickPosition();
			//if playback is ahead: return nothing
			if (timePool.get(0).tick > currentTick) {
				return;
			}
			//if the program hung up but the player continued, there programm would always be to late.
			//So the algorithm deletes all aruments before the current Element.
			while ((timePool.size() > (currentPosition+1)) && timePool.get(currentPosition + 1).tick <= currentTick)
				currentPosition++;
			MP pos = timePool.get(currentPosition).mp;
			for (PlaybackListener listener : listeners.getAll()) {
				long ms = (long) ( sequencer.getMicrosecondPosition() / 1000L );
				listener.playbackAtMP(pos, ms);
			}
		}
		else if (message.getData1() == MidiEvents.eventPlaybackEnd) {
			stop(); //stop to really ensure the end
			for (PlaybackListener listener : listeners.getAll()) {
				listener.playbackAtEnd();
			}
		}
	}

	public float getVolume() {
		return volume;
	}

	public void setVolume(float volume) {
		this.volume = volume;
		applyVolume();
	}

	private void applyVolume() {
		MidiChannel[] channels = synthesizer.getChannels();
		int max = 127; //according to MIDI standard
		for (int i = 0; i < channels.length; i++) {
			channels[i].controlChange(7, Math.round(volume * max));
		}
	}

	public boolean isPlaybackFinished() {
		return sequencer.getMicrosecondPosition() >= sequencer.getMicrosecondLength();
	}

	public long getMicrosecondLength() {
		if (sequence == null)
			return 0;
		return sequence.getSequence().getMicrosecondLength();
	}

	public long getMicrosecondPosition() {
		if (sequence == null)
			return 0;
		return sequencer.getMicrosecondPosition();
	}

	public Sequence getSequence() {
		if (sequence != null)
			return sequence.getSequence();
		else
			return null;
	}

	private class PlaybackThread extends Thread {

        private boolean stop = false
        
        public PlaybackThread() {}

        @Override public void run() {
            try {
                while (!stop) {
                    long ms = (long) ( sequencer.getMicrosecondPosition() / 1000 )
                    for (PlaybackListener listener : listeners.getAll()) {
                        listener.playbackAtMs(ms)
                    }
					long msleep = (long) ( 1000 / PlaybackListener.timerRate )
                    Thread.sleep(msleep)
                }
            } catch (InterruptedException e) {
            }
        }

        public void stopPlayback() {
            stop = true
        }
    }

	void close() {
		if (sequencer != null) {
			if (sequencer.isRunning())
				sequencer.stop();
			sequencer.close();
		}
	}

}