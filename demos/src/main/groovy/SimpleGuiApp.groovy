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

package io.github.mcolletta.mircomp.demos

import static javax.swing.JFrame.EXIT_ON_CLOSE  

import groovy.transform.CompileStatic

import groovy.swing.SwingBuilder
import groovy.beans.Bindable
import java.io.File;
import java.awt.image.BufferedImage;
import javax.swing.filechooser.FileFilter
import javax.swing.JFileChooser
import com.xenoage.zong.desktop.utils.JseZongPlatformUtils;
import com.xenoage.zong.core.Score;
import com.xenoage.zong.desktop.io.DocumentIO;
import com.xenoage.zong.desktop.io.musicxml.in.MusicXmlScoreDocFileInput;
import com.xenoage.zong.documents.ScoreDoc;
import com.xenoage.zong.layout.Layout;
import com.xenoage.zong.layout.frames.ScoreFrame;
import com.xenoage.zong.renderer.awt.AwtLayoutRenderer;
import javax.sound.midi.MidiUnavailableException;
import com.xenoage.zong.core.position.MP;
import com.xenoage.zong.io.midi.out.PlaybackListener;
import com.xenoage.zong.musiclayout.layouter.PlaybackLayouter;
import com.xenoage.zong.desktop.io.midi.out.MidiScorePlayer;
import com.xenoage.zong.desktop.io.midi.out.SynthManager

import com.xenoage.zong.musiclayout.ScoreLayout;
import com.xenoage.utils.kernel.Tuple2;
import com.xenoage.utils.math.geom.Rectangle2f;

import com.xenoage.utils.math.geom.Rectangle2i;
import com.xenoage.utils.math.geom.Point2f;
import com.xenoage.utils.math.geom.Size2f;
import com.xenoage.utils.math.Units;

import com.xenoage.utils.math.geom.Point2i;
import com.xenoage.utils.math.geom.Size2i;


// ---------------------------------------------------

import static com.xenoage.utils.collections.CollectionUtils.addOrNew;
import static com.xenoage.utils.collections.CollectionUtils.alist;
import static com.xenoage.utils.math.Fraction._0;
import static com.xenoage.utils.math.Fraction.fr;
import static com.xenoage.zong.core.music.Pitch.A;
import static com.xenoage.zong.core.music.Pitch.B;
import static com.xenoage.zong.core.music.Pitch.C;
import static com.xenoage.zong.core.music.Pitch.D;
import static com.xenoage.zong.core.music.Pitch.E;
import static com.xenoage.zong.core.music.Pitch.F;
import static com.xenoage.zong.core.music.Pitch.G;
import static com.xenoage.zong.core.music.Pitch.pi;
import static com.xenoage.zong.core.music.format.SP.sp;
import static com.xenoage.zong.core.position.MP.mp;
import static com.xenoage.zong.core.text.UnformattedText.ut;

import java.util.ArrayList;

import com.xenoage.utils.math.Fraction;
import com.xenoage.zong.commands.core.music.ColumnElementWrite;
import com.xenoage.zong.commands.core.music.PartAdd;
import com.xenoage.zong.commands.core.music.direction.DirectionAdd;
import com.xenoage.zong.commands.core.music.group.BarlineGroupAdd;
import com.xenoage.zong.commands.core.music.group.BracketGroupAdd;
import com.xenoage.zong.commands.core.music.slur.SlurAdd;
import com.xenoage.zong.core.Score;
import com.xenoage.zong.core.format.StaffLayout;
import com.xenoage.zong.core.instrument.Instrument;
import com.xenoage.zong.core.instrument.UnpitchedInstrument;
import com.xenoage.zong.core.music.ColumnElement;
import com.xenoage.zong.core.music.MeasureElement;
import com.xenoage.zong.core.music.MeasureSide;
import com.xenoage.zong.core.music.Part;
import com.xenoage.zong.core.music.Pitch;
import com.xenoage.zong.core.music.annotation.Annotation;
import com.xenoage.zong.core.music.annotation.Articulation;
import com.xenoage.zong.core.music.annotation.ArticulationType;
import com.xenoage.zong.core.music.barline.Barline;
import com.xenoage.zong.core.music.barline.BarlineStyle;
import com.xenoage.zong.core.music.chord.Chord;
import com.xenoage.zong.core.music.chord.Note;
import com.xenoage.zong.core.music.clef.Clef;
import com.xenoage.zong.core.music.clef.ClefType;
import com.xenoage.zong.core.music.direction.Dynamics;
import com.xenoage.zong.core.music.direction.DynamicsType;
import com.xenoage.zong.core.music.direction.Tempo;
import com.xenoage.zong.core.music.direction.Wedge;
import com.xenoage.zong.core.music.direction.WedgeType;
import com.xenoage.zong.core.music.format.BezierPoint;
import com.xenoage.zong.core.music.format.Position;
import com.xenoage.zong.core.music.group.BarlineGroup;
import com.xenoage.zong.core.music.group.BracketGroup;
import com.xenoage.zong.core.music.group.StavesRange;
import com.xenoage.zong.core.music.key.TraditionalKey;
import com.xenoage.zong.core.music.key.TraditionalKey.Mode;
import com.xenoage.zong.core.music.rest.Rest;
import com.xenoage.zong.core.music.slur.Slur;
import com.xenoage.zong.core.music.slur.SlurType;
import com.xenoage.zong.core.music.slur.SlurWaypoint;
import com.xenoage.zong.core.music.time.Time;
import com.xenoage.zong.core.music.time.TimeType;
import com.xenoage.zong.core.position.MP;
import com.xenoage.zong.io.selection.Cursor;

import static com.xenoage.zong.core.format.LayoutFormat.defaultLayoutFormat;
import static com.xenoage.zong.musiclayout.settings.LayoutSettings.defaultLayoutSettings;
import static com.xenoage.zong.util.ZongPlatformUtils.zongPlatformUtils;

import java.io.IOException;
import java.util.List;

import com.xenoage.utils.async.AsyncProducer;
import com.xenoage.utils.async.AsyncResult;
import com.xenoage.utils.exceptions.InvalidFormatException;
import com.xenoage.utils.filter.AllFilter;
import com.xenoage.utils.io.InputStream;
import com.xenoage.utils.math.geom.Point2f;
import com.xenoage.utils.math.geom.Size2f;
import com.xenoage.zong.core.Score;
import com.xenoage.zong.core.format.LayoutFormat;
import com.xenoage.zong.core.format.PageFormat;
import com.xenoage.zong.documents.ScoreDoc;
import com.xenoage.zong.layout.Layout;
import com.xenoage.zong.layout.LayoutDefaults;
import com.xenoage.zong.layout.Page;
import com.xenoage.zong.layout.frames.ScoreFrame;
import com.xenoage.zong.layout.frames.ScoreFrameChain;
import com.xenoage.zong.musiclayout.ScoreLayout;
import com.xenoage.zong.musiclayout.layouter.Context;
import com.xenoage.zong.musiclayout.layouter.ScoreLayoutArea;
import com.xenoage.zong.musiclayout.layouter.ScoreLayouter;
import com.xenoage.zong.musiclayout.layouter.Target;
import com.xenoage.zong.musiclayout.settings.LayoutSettings;
import com.xenoage.zong.musicxml.types.MxlScorePartwise;
import com.xenoage.zong.symbols.SymbolPool;

import static com.xenoage.utils.jse.async.Sync.sync;
import static com.xenoage.utils.jse.JsePlatformUtils.jsePlatformUtils;
import com.xenoage.zong.io.symbols.SymbolPoolReader;
import com.xenoage.zong.io.musiclayout.LayoutSettingsReader;


import org.codehaus.groovy.control.customizers.ImportCustomizer
import org.codehaus.groovy.control.CompilerConfiguration

import io.github.mcolletta.mirconverter.ZongConverter
import io.github.mcolletta.mirchord.core.ScoreBuilder
import io.github.mcolletta.mirchord.core.Score as MirScore

import javax.sound.midi.Soundbank
import javax.sound.midi.Synthesizer
// import com.sun.media.sound.ModelAbstractOscillator


@CompileStatic
class ZongModel implements PlaybackListener  {
    @Bindable int page = 0
    @Bindable float zoom = 2
    float zoomFactor = 2
    @Bindable BufferedImage awtImage
    Layout layout
    PlaybackLayouter playbackLayouter

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
    
    ZongModel(String appName = "demo") {
        JseZongPlatformUtils.init(appName);
        SynthManager.init(false)
        // SynthManager.loadSoundbank('path/to')
        // Synthesizer synth = SynthManager.getSynthesizer()
        // synth.unloadAllInstruments(synth.getDefaultSoundbank())
        // synth.loadAllInstruments(new MyOscillator())
        Playback.registerListener(this)
    }

    public Score createScore(String path) {
        ZongConverter zconverter = new ZongConverter()
        File file = new File(path)
        // Add imports for script.
        def importCustomizer = new ImportCustomizer()
        importCustomizer.addStaticStars 'com.xenoage.utils.math.Fraction'
        importCustomizer.addImports 'com.xenoage.utils.math.Fraction'
        importCustomizer.addStaticStars 'io.github.mcolletta.mircomp.utils.Utils'
        importCustomizer.addStarImports 'io.github.mcolletta.mirchord.core'
        def configuration = new CompilerConfiguration()
        configuration.addCompilationCustomizers(importCustomizer)
        def binding = new Binding()
        binding.setProperty('builder', new ScoreBuilder()) 
        def mirscore = new GroovyShell(binding, configuration).evaluate(file)
        /*def engine = new GroovyScriptEngine(['src/main/groovy/demos'.toURI().toURL()] as URL[])
        def mirscore = engine.run('ScoreExample1.groovy', binding)  */
        Score score =zconverter.convert((MirScore)mirscore)
        return score
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

    void loadScore(String filePath) {
        try {
            ScoreDoc scoreDoc
            String fileExt = filePath.split("\\.").last()
            if (fileExt == 'groovy')
                scoreDoc = read( createScore(filePath) )
            else // xml
                scoreDoc = DocumentIO.read(new File(filePath), new MusicXmlScoreDocFileInput());
            loadScore(scoreDoc);
        }
        catch (Exception ex) {
            println ex.getMessage()
            ex.printStackTrace()
        }
    }
    
    void loadScore(ScoreDoc doc) {
        setPage(0)
        Playback.stop()
        ScoreDoc scoreDoc = doc
        setLayout(scoreDoc.getLayout())
        Score score = scoreDoc.getScore()
        layout.updateScoreLayouts(score)
        scoreLayout = layout.getScoreFrameChain(score).getScoreLayout()
        playbackLayouter = new PlaybackLayouter(scoreLayout)
        Playback.openScore(scoreDoc.getScore())
        renderLayout()        
    }

    void setLayout(Layout lo) {
        layout = lo
        def pages = layout.getPages()
        pageWidthMm = (int)pages.get(0).getFormat().getSize().width
        pageHeightMm = (int)pages.get(0).getFormat().getSize().height
        setSystemRectPx(new Rectangle2i(0,0,0,0))
    }
    
    void renderLayout() {
        setAwtImage(AwtLayoutRenderer.paintToImage(layout, page, zoom))
    }

    void updatePageDimensions() {
        pageWidthPx = Units.mmToPxInt(pageWidthMm, zoom)
        pageHeightPx = Units.mmToPxInt(pageHeightMm, zoom)
    }

    void zoomIn() { zoomTo((float)zoomFactor) }
    
    void zoomOut() { zoomTo((float)(1 / zoomFactor)) }
    
    private void zoomTo(float zoomFactor) {
        zoom *= zoomFactor
        updatePageDimensions()
        renderLayout()
    }

    void setZoom(float zoomParm) {
        zoom = zoomParm
        updatePageDimensions()
    }

    Point2i computePositionPx(Point2f pos)
    {
        Point2i ret = new Point2i(
            Units.mmToPxInt(pos.x, zoom),
            Units.mmToPxInt(pos.y, zoom));
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

    @Override public void playbackAtMP(MP mp, long ms) {
        playbackLayouter.setCursorAt(mp)
        //follow playback adapted from the Zong! viewer
        if (followPlayback)
        {
            if (mp.getMeasure() != lastMeasureIndex)
            {
                lastMeasureIndex = mp.getMeasure();
                Tuple2<Integer, Integer> pos = scoreLayout.getFrameAndSystemIndex(lastMeasureIndex);
                if (pos != null && (lastFrameIndex != pos.get1() || lastSystemIndex != pos.get2()))
                {
                    //next system was reached.
                    lastFrameIndex = pos.get1();
                    lastSystemIndex = pos.get2();
                    //system completely visible on screen?
                    Tuple2<Integer, Rectangle2f> systemRect = layout.getSystemBoundingRect(
                            scoreLayout, lastFrameIndex, lastSystemIndex);
                    setPage(systemRect.get1())
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
                            setZoom((float)(zoom * factor))
                            systemRectPx = computeRectangleMm(systemRect2)
                        }
                    }
                }
            }
        }
        
        renderLayout()
    }

    @Override public void playbackAtMs(long ms) {}
    @Override public void playbackStarted() {}
    @Override public void playbackPaused() {}
    @Override public void playbackStopped() {}
    @Override public void playbackAtEnd() {}
}

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
    
    static void start() {
        if (player != null) {
            player.setMetronomeEnabled(false);
            player.start();
        }
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

def model = new ZongModel()

def openScore = {    
    def dialog = new JFileChooser(dialogTitle: "Choose a MusicXml file",fileSelectionMode: JFileChooser.FILES_ONLY, 
                                  fileFilter: [getDescription: {-> "*.xml, *.groovy"}, accept:{file-> file ==~ /.*?\.xml/ || file ==~ /.*?\.groovy/ || file.isDirectory() }] as FileFilter)
    def openResult = dialog.showOpenDialog()
    if (JFileChooser.APPROVE_OPTION == openResult) {
        model.loadScore(dialog.selectedFile.toString())
    }
}

def showAbout = {
     def pane = swing.optionPane(message:'MircComp simple notation demo')
     def dialog = pane.createDialog(frame, 'About')
     dialog.show()
}

swing = new SwingBuilder()
frame = swing.frame(title: 'Frame', size: [700, 700], show: true, defaultCloseOperation: EXIT_ON_CLOSE) {
     menuBar {
        menu(text:'File') {
            menuItem(text:'Open...', actionPerformed:openScore)
            menuItem(text: 'Exit', actionPerformed: { dispose() })
        }
        menu(text:'View') {
            menuItem(text:'Zoom in', actionPerformed:{ model.with { zoomIn() } })
            menuItem(text:'Zoom out', actionPerformed:{ model.with { zoomOut() } })
        }
        menu(text:'Playback') {
            menuItem(text:'Start', actionPerformed:{ model.with {Playback.start(); renderLayout() } })
            menuItem(text:'Stop', actionPerformed:{ model.with {Playback.stop(); renderLayout() } })
        }
        menu(text:'About') {
             menuItem(text:'About', actionPerformed: showAbout)
        }
    }
    borderLayout()
    vbox {
        hbox {
            button('<<', enabled: bind { model.page > 0 },  actionPerformed: { model.with {page--; renderLayout() } })
            label(text: 'Page: ')
            label(text: bind(source: model, sourceProperty: 'page'))
            button('>>', enabled: bind { model.page < model.layout.pages.size()-1 }, actionPerformed: { model.with {page++; renderLayout() } })
        }
        scrollPane(id: "imageScrollPane") {
            layeredPane(id:"layered", preferredSize: bind(source: model, sourceProperty: 'awtImage', 
                                                          converter: { [it.width, it.height]} )) {
                label(id: 'scoreImage', 
                      bounds: bind(source: model, sourceProperty: 'awtImage', 
                                   converter: { [it.width, it.height]} ),
                      icon:  bind(source: model, sourceProperty: 'awtImage', 
                                  converter: {  if (model.needScroll) {
                                                    def modelRect = model.systemRectPx
                                                    def pos = modelRect.position
                                                    scrollRect = new java.awt.Rectangle(pos.x,pos.y,
                                                                                        (int)modelRect.size.width,
                                                                                        (int)modelRect.size.height)
                                                    scoreImage.scrollRectToVisible(scrollRect)
                                                }
                                                imageIcon(it) })
                     )
                widget(new GlassPanel(model), id:"glass", 
                       bounds:bind(source: model, sourceProperty: 'awtImage', converter: { [0, 0, it.width, it.height]}))
            }
        }
    }
}

swing.imageScrollPane.viewport.addChangeListener({ 
    def r = it.source.viewRect 
    model.with { viewRect = new Rectangle2i((int)r.x, (int)r.y, (int)r.width, (int)r.height) }
    })

swing.layered.setLayer(swing.glass, new Integer(1))

class GlassPanel extends javax.swing.JComponent {

    ZongModel model
    Rectangle2i rect

    GlassPanel(ZongModel model) { this.model = model } 

    @Override
    protected void paintComponent(java.awt.Graphics g) {
        super.paintComponent(g)
        rect = model.systemRectPx
        if (rect != null) {
            g.setColor(java.awt.Color.RED)
            g.setStroke(new java.awt.BasicStroke(2))
            g.drawRect(rect.position.x,
                       rect.position.y,
                       (int)rect.size.width,
                       (int)rect.size.height)
        }
    }
} 

frame.show() 

println "Loading score..."
model.loadScore("../compositions/ScoreExample1.groovy")


