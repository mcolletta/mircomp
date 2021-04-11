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

import javax.sound.midi.Synthesizer
import javax.sound.midi.MidiUnavailableException

import static com.xenoage.zong.core.position.MP.mp0

import com.xenoage.zong.core.Score
import com.xenoage.zong.core.position.MP
import com.xenoage.zong.io.midi.out.PlaybackListener


class Playback {
    
    private Synthesizer synthesizer
    private MidiScorePlayer player = null;
    
    void openScore(Score score) {
        player.openScore(score)
    }
    
    public Playback(Synthesizer synthesizer) {
        this.synthesizer = synthesizer
        try {
            player = new MidiScorePlayer(synthesizer)
        } catch (MidiUnavailableException ex) {
            println "MIDI not available"
        }
    }
    
    void start(MP position=null) {
        if (player != null) {
            player.setMetronomeEnabled(false);
            if (position != null)
                player.setMP(position)
            player.start()
        }
    }

    void pause() {
        if (player != null)
            player.pause()
    }

    void stop() {
        if (player != null) {
            player.stop()
        }
    }
    
    public void registerListener(PlaybackListener listener) {
        player.addPlaybackListener(listener)
    }

    void close() {
        player.close()
    }

}