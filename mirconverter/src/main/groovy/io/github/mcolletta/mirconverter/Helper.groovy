/*
 * Copyright (C) 2016-2022 Mirco Colletta
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

package io.github.mcolletta.mirconverter

import io.github.mcolletta.mirchord.core.Score as MirScore
import io.github.mcolletta.mirchord.interpreter.MirChordInterpreter

import com.xenoage.zong.documents.ScoreDoc

import com.xenoage.zong.core.Score
import com.xenoage.zong.core.format.PageFormat
import com.xenoage.zong.core.format.LayoutFormat

import static com.xenoage.zong.core.format.LayoutFormat.defaultLayoutFormat

import com.xenoage.zong.layout.Page
import com.xenoage.zong.layout.Layout
import com.xenoage.zong.layout.LayoutDefaults
import com.xenoage.zong.layout.frames.ScoreFrame
import com.xenoage.zong.layout.frames.ScoreFrameChain

import com.xenoage.zong.musiclayout.ScoreLayout
import com.xenoage.zong.musiclayout.layouter.ScoreLayouter
import com.xenoage.zong.musiclayout.layouter.ScoreLayoutArea
import com.xenoage.zong.musiclayout.layouter.Target

import com.xenoage.utils.math.geom.Size2f
import com.xenoage.utils.math.geom.Point2f

import com.xenoage.utils.document.io.FileOutput
import com.xenoage.utils.exceptions.InvalidFormatException

import com.xenoage.zong.desktop.io.wav.out.WavScoreDocFileOutput
import com.xenoage.zong.desktop.io.ogg.out.OggScoreDocFileOutput
import com.xenoage.zong.desktop.io.pdf.out.PdfScoreDocFileOutput
import com.xenoage.zong.desktop.io.png.out.PngScoreDocFileOutput
import com.xenoage.zong.desktop.io.midi.out.MidiScoreDocFileOutput
import com.xenoage.zong.desktop.io.DocumentIO


class Helper {

	static Score convertScore(MirScore mirscore) {
		ZongConverter zconverter = new ZongConverter()
        Score score = zconverter.convert(mirscore)
        return score
	}

	static void saveAs(MirScore mirscore, File file) {		
		try {
			Score score = convertScore(mirscore)
            ScoreDoc scoreDoc = read(score)

			FileOutput<ScoreDoc> out = null
			String fileName = file.getName()
			String format = ""
			int i = fileName.lastIndexOf('.')
			if (i >= 0) {
			    format = fileName.substring(i+1)
			} else
				throw new Exception("File extension required. Supported: .mid .png .wav")
			switch (format) {
				// case "pdf": 
				// 	out = new PdfScoreDocFileOutput()
				// 	break
				case "png": 
					out = new PngScoreDocFileOutput()
					break
				case "mid": 
					out = new MidiScoreDocFileOutput()
					break
				/*case "ogg": 
					out = new OggScoreDocFileOutput()
					break*/
	            case "wav":
	                out = new WavScoreDocFileOutput()
				default:
					break
			}
			if (out != null) {
				DocumentIO.write(scoreDoc, file, out)
			}
		}
		catch (Exception ex) {
            println ex.getMessage()
            ex.printStackTrace()
        }
	}

	// read method from the Zong! project but check frames size > 0 instead of 1
    public static ScoreDoc read(Score score) throws InvalidFormatException, IOException {
        LayoutFormat layoutFormat = defaultLayoutFormat
        Object oLayoutFormat = score.getMetaData().get("layoutformat")
        if (oLayoutFormat instanceof LayoutFormat) {
            layoutFormat = (LayoutFormat) oLayoutFormat
        }
        LayoutDefaults layoutDefaults = new LayoutDefaults(layoutFormat)
        ScoreDoc ret = new ScoreDoc(score, layoutDefaults)
        Layout layout = ret.getLayout()
        PageFormat pageFormat = layoutFormat.getPageFormat(0)
        Size2f frameSize = new Size2f((float)pageFormat.getUseableWidth(), (float)pageFormat.getUseableHeight())
        Point2f framePos = new Point2f((float)(pageFormat.getMargins().getLeft() + frameSize.width / 2),
            (float)(pageFormat.getMargins().getTop() + frameSize.height / 2))
        Target target = Target.completeLayoutTarget(new ScoreLayoutArea(frameSize))
        ScoreLayout scoreLayout = new ScoreLayouter(ret, target).createScoreLayout()
        if (scoreLayout.frames.size() > 0) {
            ScoreFrameChain chain = null
            for (int i = 0; i < scoreLayout.frames.size(); i++) {
                Page page = new Page(pageFormat)
                layout.addPage(page)
                ScoreFrame frame = new ScoreFrame()
                frame.setPosition(framePos)
                frame.setSize(frameSize)
                page.addFrame(frame)
                if (chain == null) {
                    chain = new ScoreFrameChain(score)
                    chain.setScoreLayout(scoreLayout)
                }
                chain.add(frame)
            }
        }
        else {
            println "no frames: create a single empty pageno frames: create a single empty page"
            Page page = new Page(pageFormat)
            layout.addPage(page)
        }
        return ret
    }

}