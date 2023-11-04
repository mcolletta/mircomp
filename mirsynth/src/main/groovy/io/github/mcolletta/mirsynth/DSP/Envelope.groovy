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


// inspired from Beads code for Envelope UGen
class Envelope {
	
	float Fs
	List<Segment> segments
	Segment currentSegment
	float currentSample
	float currentValue
	float currentStartValue
	
	
	Envelope(segmentList, float curvature=1.0f, float samplerate=44100.0f) {
		Fs = samplerate
		segments = []
		segmentList.each { List seg ->
			segments.add(new Segment((float)seg[0], (float)seg[1], curvature))
		}
		currentSegment = segments[0]
	}
	
	class Segment {
		
		float duration // in samples
		float curvature
		float end
		
		Segment(float duration, float end, float curvature=1.0f) {
			this.duration = (float)Utils.millisToSamples(duration, Fs)
			this.end = end
			this.curvature = curvature
		}
		
	}
	
	void getNextSegment() {
		if(currentSegment != null) {
			currentStartValue = currentSegment.end
			currentValue = currentStartValue
			segments.remove(currentSegment)
		} else {
			currentStartValue = currentValue
		}
		if(segments.size() > 0) {
			currentSegment = segments.get(0)
		} else {
			currentSegment = null
		}
		currentSample = 0
	}
	
	float tick() {
		if(currentSegment == null) {
			getNextSegment()
        } else if(currentSegment.duration == 0) {
            getNextSegment()
		} else {
			float ratio
			if(currentSegment.curvature != 1.0f) 
				ratio = (float)Math.pow((double)currentSample / (double)currentSegment.duration, (double)currentSegment.curvature)
			else 
				ratio = (float) (currentSample / currentSegment.duration)
			currentValue = (1.0f - ratio) * currentStartValue + ratio * currentSegment.end
			currentSample += 1
			if(currentSample > currentSegment.duration)
				getNextSegment()
		}
		return currentValue
	}

}


