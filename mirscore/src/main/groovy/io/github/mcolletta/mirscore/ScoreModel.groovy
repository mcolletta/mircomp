/*
 * Copyright (C) 2016-2017 Mirco Colletta
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

import java.io.File;

import com.xenoage.utils.exceptions.InvalidFormatException
import com.xenoage.utils.document.io.FileOutput
import com.xenoage.utils.math.Units
import com.xenoage.utils.kernel.Tuple2;
import com.xenoage.utils.math.geom.Rectangle2f
import com.xenoage.utils.math.geom.Point2f
import com.xenoage.utils.math.geom.Size2f
import com.xenoage.utils.math.geom.Rectangle2i
import com.xenoage.utils.math.geom.Point2i
import com.xenoage.utils.math.geom.Size2i

import com.xenoage.zong.core.format.LayoutFormat
import com.xenoage.zong.core.format.PageFormat
import com.xenoage.zong.layout.LayoutDefaults
import com.xenoage.zong.layout.Page
import com.xenoage.zong.layout.frames.ScoreFrame
import com.xenoage.zong.layout.frames.ScoreFrameChain
import com.xenoage.zong.musiclayout.ScoreLayout
import com.xenoage.zong.musiclayout.layouter.ScoreLayoutArea;
import com.xenoage.zong.musiclayout.layouter.ScoreLayouter
import com.xenoage.zong.musiclayout.layouter.Target

import com.xenoage.zong.core.Score
import com.xenoage.zong.core.music.MusicElement
import com.xenoage.zong.core.position.MP
import com.xenoage.zong.core.position.MPElement

import com.xenoage.utils.math.Fraction;
import static com.xenoage.utils.math.Fraction._0
import static com.xenoage.zong.core.position.MP.mp0
// import static com.xenoage.zong.core.position.MP.mp
import static com.xenoage.zong.core.position.MP.atBeat;
import static com.xenoage.zong.core.position.MP.unknown;
import static com.xenoage.zong.core.position.MP.unknownMp;

import static com.xenoage.utils.collections.CollectionUtils.getFirst;
import static com.xenoage.utils.collections.CollectionUtils.getLast;
import static com.xenoage.utils.kernel.Range.range;

import static com.xenoage.utils.jse.JsePlatformUtils.jsePlatformUtils

import com.xenoage.zong.desktop.io.midi.out.SynthManager
import com.xenoage.zong.desktop.utils.JseZongPlatformUtils
import com.xenoage.zong.desktop.io.DocumentIO
import com.xenoage.zong.desktop.io.midi.out.MidiScoreDocFileOutput
import com.xenoage.zong.desktop.io.musicxml.in.MusicXmlScoreDocFileInput
import com.xenoage.zong.desktop.io.wav.out.WavScoreDocFileOutput
import com.xenoage.zong.desktop.io.ogg.out.OggScoreDocFileOutput
import com.xenoage.zong.desktop.io.pdf.out.PdfScoreDocFileOutput
import com.xenoage.zong.desktop.io.png.out.PngScoreDocFileOutput
import com.xenoage.zong.documents.ScoreDoc
import com.xenoage.zong.io.midi.out.PlaybackListener
import com.xenoage.zong.layout.Layout
import com.xenoage.zong.layout.frames.ScoreFrame
import com.xenoage.zong.musiclayout.ScoreFrameLayout
import com.xenoage.zong.musiclayout.layouter.PlaybackLayouter
import com.xenoage.zong.musiclayout.stampings.Stamping
import com.xenoage.zong.musiclayout.stampings.StaffStamping;
import com.xenoage.zong.musiclayout.spacing.ColumnSpacing
import com.xenoage.zong.musiclayout.spacing.SystemSpacing;

import static com.xenoage.zong.core.format.LayoutFormat.defaultLayoutFormat
import static com.xenoage.zong.musiclayout.settings.LayoutSettings.defaultLayoutSettings
import static com.xenoage.zong.util.ZongPlatformUtils.zongPlatformUtils

import groovy.beans.*
import groovy.transform.CompileStatic


@CompileStatic
class ScoreModel implements PlaybackListener {

    float DEFAULT_ZOOM = 2.0f

    @Bindable MP currentMP
    @Bindable int currentPage = 0
    @Bindable float currentZoom = DEFAULT_ZOOM

	Layout layout
    PlaybackLayouter playbackLayouter

    ScoreDoc scoreDoc

    ScoreLayout scoreLayout = null
    boolean followPlayback = true
    int lastMeasureIndex = -1
    int lastFrameIndex = -1
    int lastSystemIndex = -1

    @Bindable Rectangle2i systemRectPx

    int pageWidthMm
    int pageHeightMm
    int pageWidthPx
    int pageHeightPx

    Rectangle2i viewRect = new Rectangle2i(0,0,0,0)
    boolean needScroll = false

	ScoreModel(boolean needInit=true) {
        if (needInit) {
            String appName = "MirScore"
    		JseZongPlatformUtils.init(appName)
            SynthManager.init(false)
        }
		Playback.registerListener(this)
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

    void loadScore(File scoreFile) {
    	try {
            ScoreDoc scoreDoc = DocumentIO.read(scoreFile, new MusicXmlScoreDocFileInput())
            loadScore(scoreDoc)
        }
        catch (Exception ex) {
            println ex.getMessage()
            ex.printStackTrace()
        }
    }

    void loadScore(String filePath) {
        try {
            ScoreDoc scoreDoc = DocumentIO.read(new File(filePath), new MusicXmlScoreDocFileInput())
            loadScore(scoreDoc)
        }
        catch (Exception ex) {
            println ex.getMessage()
            ex.printStackTrace()
        }
    }

    void loadScore(Score score) {
        try {
            ScoreDoc scoreDoc = read(score)
            loadScore(scoreDoc)
        }
        catch (Exception ex) {
            println ex.getMessage()
            ex.printStackTrace()
        }
    }
    
    void loadScore(ScoreDoc doc) {
        setCurrentPage(0)
        Playback.stop()
        scoreDoc = doc
        setLayout(scoreDoc.getLayout())
        Score score = scoreDoc.getScore()
        layout.updateScoreLayouts(score)
        scoreLayout = layout.getScoreFrameChain(score).getScoreLayout()
        playbackLayouter = new PlaybackLayouter(scoreLayout)
        Playback.openScore(scoreDoc.getScore())
        // updatePageDimensions
        setCurrentZoom(DEFAULT_ZOOM)
    }

    void saveAs(File file) {
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
			try {
				DocumentIO.write(scoreDoc, file, out);
				println "" + file + " saved."
			} catch (Exception ex) {
				println ex.getMessage()
            	ex.printStackTrace()
			}
		}
	}

	public void pickMP(Point2f positionPx) {
		if (getLayout().getScoreFrames().size() == 0)
			return
		//get the layout of first score frame
		ScoreFrame frame = getLayout().getScoreFrames().get(getCurrentPage())
		ScoreFrameLayout frameLayout = frame.getScoreFrameLayout()
		//convert position from screen space to page space, then from page space
		//to frame space, and them from frame space to score frame space
		Point2f positionMm = positionPx.scale(Units.pxToMm(1, getCurrentZoom()))
		Point2f framePositionMm = positionMm.sub(frame.getAbsolutePosition())
		Point2f scorePositionMm = frame.getScoreLayoutPosition(framePositionMm)

        try {
            // MP scorePositionMP = frameLayout.computeMP(scorePositionMm)
            MP scorePositionMP = unknownMp
            StaffStamping staff = frameLayout.getStaffStampingAt(scorePositionMm)
            if (staff != null) {
                float xMm = scorePositionMm.x - staff.positionMm.x
                SystemSpacing system =  staff.system
                int staffIndex = staff.staffIndex
                List<ColumnSpacing> columns = system.columns
                // measureIndex is relative to the selected system
                int measureIndex = getSystemMeasureIndexAt(xMm, columns)
                // systemMeasure is relative to the score
                int systemMeasure = measureIndex + system.getStartMeasureIndex()
                float xMmInMeasure = xMm - system.getMeasureStartMm(systemMeasure) - columns.get(measureIndex).getLeadingWidthMm()
                if (measureIndex != unknown) {
                    Fraction beat = columns.get(measureIndex).getBeatAt(xMmInMeasure, staffIndex)
                    scorePositionMP = atBeat(staffIndex, systemMeasure, unknown, beat)
                }
                if (scorePositionMP != unknownMp) {
                    setCurrentMP(scorePositionMP)
                    playbackLayouter.setCursorAt(getCurrentMP())
                }
            }
        } catch(Exception ex) {
            println "Exception in ScoreModel::pickMP: " + ex
        }
	}

    private int getSystemMeasureIndexAt(float xMm, List<ColumnSpacing> columns) {
        if (xMm < 0)
            return unknown;
        float x = 0;
        for (int iMeasure : range(columns)) {
            x += columns.get(iMeasure).getWidthMm();
            if (xMm < x)
                return iMeasure;
        }
        return unknown;
    }

    int getNumPages() {
    	return layout.getPages().size()
    }

    int getLastPage() {
    	return layout.getPages().size() - 1
    }

    // Playback

    void play() {
        lastSystemIndex = -1
        Playback.start(getCurrentMP())
    }

    void pause() {
        Playback.pause()
    }

    void stop() {
        Playback.stop()
    }

    // Playback utils

    void setLayout(Layout lo) {
        layout = lo
        def pages = layout.getPages()
        pageWidthMm = (int)pages.get(0).getFormat().getSize().width
        pageHeightMm = (int)pages.get(0).getFormat().getSize().height
        setSystemRectPx(new Rectangle2i(0,0,0,0))
    }

    void setCurrentZoom(float zoom) {
        currentZoom = zoom
        updatePageDimensions()
        setSystemRectPx(new Rectangle2i(0,0,0,0))
    }

    void updatePageDimensions() {
        pageWidthPx = Units.mmToPxInt(pageWidthMm, currentZoom)
        pageHeightPx = Units.mmToPxInt(pageHeightMm, currentZoom)
    }


    Point2i computePositionPx(Point2f pos)
    {
        Point2i ret = new Point2i(
            Units.mmToPxInt(pos.x, currentZoom),
            Units.mmToPxInt(pos.y, currentZoom));
        ret = ret.add((int)(pageWidthPx / 2), (int)(pageHeightPx / 2));
        return ret;
    }
    
    Rectangle2i computeRectangleMm(Rectangle2f rect)
    {
        Point2f nw = rect.position;
        Point2f se = nw.add(rect.size);
        Point2i nwPx = computePositionPx(nw);
        Point2i sePx = computePositionPx(se);
        return new Rectangle2i(nwPx, new Size2i(sePx.sub(nwPx)));
    }


    // PlaybackListener methods

    @Override public void playbackAtMP(MP mp, long ms) {
		playbackLayouter.setCursorAt(mp)
		setCurrentMP(mp)
		getCurrentMP() // fire event

		//follow playback adapted from the Zong! viewer
        if (followPlayback)
        {
            if (mp.getMeasure() != lastMeasureIndex)
            {
                lastMeasureIndex = mp.getMeasure()
                Tuple2<Integer, Integer> pos = scoreLayout.getFrameAndSystemIndex(lastMeasureIndex);
                if (pos != null && (lastFrameIndex != pos.get1() || lastSystemIndex != pos.get2()))
                {
                    //next system was reached.
                    lastFrameIndex = pos.get1();
                    lastSystemIndex = pos.get2();
                    //system completely visible on screen?
                    Tuple2<Integer, Rectangle2f> systemRect = layout.getSystemBoundingRect(
                            scoreLayout, lastFrameIndex, lastSystemIndex);
                    setCurrentPage(systemRect.get1())
                    Rectangle2f systemRect2 = systemRect.get2()
                    systemRectPx = computeRectangleMm(systemRect2)

                    needScroll = false
                    if (!viewRect.contains(systemRectPx)) { // system not completely visible
                        needScroll = true
                        if (systemRectPx.size.width >= viewRect.size.width ||
                            systemRectPx.size.height >= viewRect.size.height) // Need zoom
                        {
                            //zoom out as much as needed (+10% more)
                            float factor = Math.min((float)(viewRect.size.width / systemRectPx.size.width),
                                                    (float)(viewRect.size.height / systemRectPx.size.height)) 
                            factor = (float)(factor / 1.1)
                            setCurrentZoom((float)(currentZoom * factor))
                            systemRectPx = computeRectangleMm(systemRect2)
                        }
                    }
                }
            }
        }
	}

	@Override public void playbackAtMs(long ms) {
	}

	@Override public void playbackStarted() {
	}

	@Override public void playbackPaused() {
        needScroll = false
	}

	@Override public void playbackStopped() {
        needScroll = false
	}

	@Override public void playbackAtEnd() {
        setCurrentMP(mp0)
        playbackLayouter.setCursorAt(getCurrentMP())
	}

}
