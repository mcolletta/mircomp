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

package io.github.mcolletta.mirscore

import javax.sound.midi.MidiUnavailableException

import com.xenoage.zong.core.Score
import com.xenoage.zong.core.position.MP
import com.xenoage.zong.io.midi.out.PlaybackListener
import com.xenoage.zong.desktop.io.midi.out.MidiScorePlayer

import groovy.transform.CompileStatic


@CompileStatic
class Playback {
    
    private static MidiScorePlayer player = null;
    
    static void openScore(Score score) {
        initPlayer()
        player.openScore(score)
    }
    
    private static void initPlayer() {
        if (player == null) {
            try {
                MidiScorePlayer.init()
                player = MidiScorePlayer.midiScorePlayer()
            } catch (MidiUnavailableException ex) {
                println "MIDI not available"
            }
        }
    }
    
    static void start(MP position=null) {
        if (player != null) {
            player.setMetronomeEnabled(false);
            if (position != null)
                player.setMP(position)
            player.start()
        }
    }

    static void pause() {
        if (player != null)
            player.pause()
    }

    static void stop() {
        if (player != null)
            player.stop()
    }
    
    public static void registerListener(PlaybackListener listener) {
        initPlayer()
        player.addPlaybackListener(listener)
    }

}