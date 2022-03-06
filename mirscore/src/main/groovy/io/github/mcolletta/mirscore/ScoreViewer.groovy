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

package io.github.mcolletta.mirscore

import java.beans.PropertyChangeListener
import java.beans.PropertyChangeEvent

import javafx.geometry.Bounds

import javafx.application.Platform
import javafx.stage.Stage
import javafx.stage.WindowEvent
import javafx.stage.FileChooser
import javafx.stage.FileChooser.ExtensionFilter
import javafx.scene.layout.VBox
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.control.ScrollPane
import javafx.scene.control.Button
import javafx.scene.control.ToggleButton
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import javafx.scene.input.MouseEvent
import javafx.scene.input.KeyEvent
import javafx.scene.input.KeyCode
import javafx.scene.image.ImageView
import javafx.scene.image.WritableImage
import javafx.scene.Cursor
import javafx.event.EventHandler

import javafx.scene.control.ButtonType
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType

import javafx.animation.KeyFrame
import javafx.animation.KeyValue
import javafx.animation.Timeline
import javafx.util.Duration

import javafx.fxml.FXML
import javafx.fxml.FXMLLoader

import javafx.beans.binding.Bindings
import javafx.beans.value.ObservableValue
import javafx.beans.value.ChangeListener

import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty

import javafx.embed.swing.SwingFXUtils

import javax.sound.midi.Synthesizer;

import com.xenoage.utils.math.geom.Rectangle2i
import com.xenoage.utils.math.geom.Point2i
import com.xenoage.utils.math.geom.Point2f

import com.xenoage.zong.core.Score

import com.xenoage.zong.renderer.javafx.JfxLayoutRenderer
import com.xenoage.zong.renderer.awt.AwtLayoutRenderer
import java.awt.image.BufferedImage


enum ScoreMode { SELECT, PANNING }

class ScoreViewer  extends VBox { 

    ScoreModel scoreModel
    WritableImage scoreImage = null
    boolean SHOW_CURRENT_SYSTEM = true

    @FXML private TextField currentPageField
    @FXML private Label currentPageLabel
    @FXML private TextField currentZoomField
    @FXML private ScrollPane scrollPane
    @FXML private ImageView scoreImageView
    @FXML private Button nextpageButton
    @FXML private Button prevpageButton
    @FXML private Canvas canvas
    @FXML private ToggleButton followPlaybackButton
    @FXML private ToggleButton metronomeButton

    
    ObjectProperty<ScoreMode> mode = new SimpleObjectProperty<>()
    final ScoreMode getMode() { return mode.get() }
    final void setMode(ScoreMode value) { mode.set(value) }
    ObjectProperty<ScoreMode> modeProperty() { return mode }

	public ScoreViewer(Synthesizer synthesizer=null) {
		loadControl()
        scoreModel = new ScoreModel(synthesizer)

        /*scoreImageView.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
             @Override
             public void handle(MouseEvent event) {
                scoreImageClick(event)
                event.consume()
             }
        })*/

        currentPageField.textProperty().addListener(new ChangeListener(){
            @Override public void changed(ObservableValue o,Object oldVal, Object newVal){
                String newPageText = newVal as String
                currentPageLabel.setText("(" + newPageText + " of " + scoreModel.getNumPages() + ")")
            }
        })

        currentPageField.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                if (keyEvent.getCode() == KeyCode.ENTER)  {
                    String text = currentPageField.getText()
                    int newPage = scoreModel.getCurrentPage() + 1
                    try {
                        newPage = Integer.parseInt(text)
                        newPage--
                    } catch(Exception ex) {
                        newPage = -1
                    }
                    if (newPage >= 0 && newPage < scoreModel.getNumPages()) {
                        scoreModel.setCurrentPage(newPage)
                        draw()
                    }
                    currentPageField.setText("" + (scoreModel.getCurrentPage()+1))
                }
            }
        })

        currentZoomField.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                if (keyEvent.getCode() == KeyCode.ENTER)  {
                    String text = currentZoomField.getText()
                    float newZoom = (float)(scoreModel.getCurrentZoom() / 100f)
                    try {
                        newZoom = Float.parseFloat(text)
                        newZoom = (float)(newZoom / 100f)
                    } catch(Exception ex) {
                        newZoom = -1
                    }
                    if (newZoom > 0f && newZoom <= 3f) {
                        scoreModel.setCurrentZoom(newZoom)
                        draw()
                    }
                    currentZoomField.setText("" + (int)(scoreModel.getCurrentZoom() * 100))
                }
            }
        })

        PropertyChangeListener scoreModelListener = {PropertyChangeEvent evt -> 
            if (evt.propertyName == "currentPage") {
                int newPage = evt.newValue as int
                // println "currentPage changed with value " + newPage
                Platform.runLater( {
                    currentPageField.setText("" + (newPage+1))
                    })
            }
            if (evt.propertyName == "currentZoom") {
                float newZoom = evt.newValue as float
                // println "currentZoom changed with value " + newZoom
                Platform.runLater( {
                    currentZoomField.setText("" + (int)(newZoom * 100))
                    })
            }
            draw()
        } as PropertyChangeListener
        scoreModel.addPropertyChangeListener(scoreModelListener)


        modeProperty().addListener(new ChangeListener(){
            @Override public void changed(ObservableValue o,Object oldVal, Object newVal){
                ScoreMode newMode = newVal as ScoreMode
                scrollPane.setPannable(newMode == ScoreMode.PANNING)
                if (newMode == ScoreMode.PANNING)
                    scrollPane.setCursor(Cursor.OPEN_HAND)
                else
                    scrollPane.setCursor(Cursor.DEFAULT)
            }
        })

        scrollPane.viewportBoundsProperty().addListener(new ChangeListener(){
            @Override public void changed(ObservableValue o,Object oldVal, Object newVal){
                Bounds bounds = newVal as Bounds
                scoreModel.viewRect = new Rectangle2i((int)bounds.getMinX(), (int)bounds.getMinY(), 
                                                      (int)bounds.getWidth(), (int)bounds.getHeight())
            }
        })
	}

	public loadControl() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(getClass().getSimpleName() + ".fxml"))

        fxmlLoader.setRoot(this)
        fxmlLoader.setController(this)

        try {
            fxmlLoader.load()
        } catch (IOException exception) {
            throw new RuntimeException(exception)
        }
    }

    public void onClose() {
        Stage stage = (Stage)getScene().getWindow()
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                Platform.exit()
                System.exit(0)
            }
        })
    }

    void draw() {
        BufferedImage awtImage = null
        try {
            awtImage = AwtLayoutRenderer.paintToImage(scoreModel.getLayout(), 
                                                      scoreModel.getCurrentPage(), 
                                                      scoreModel.getCurrentZoom())

        } catch(Exception ex) {
            println "Exception during awtImage creation: " + ex.getMessage()
            // ex.printStackTrace()
        }

        if (awtImage == null)
            return

        Platform.runLater( () -> {
        // Platform.runLater( new Runnable() {

        //     @Override
        //     public void run() {
            try {
                // scoreImage = JfxLayoutRenderer.paintToImage(scoreModel.getLayout(), 
                //                                             scoreModel.getCurrentPage(), 
                //                                             scoreModel.getCurrentZoom())

                scoreImage = SwingFXUtils.toFXImage(awtImage, scoreImage)                               
                scoreImageView.setImage(scoreImage)
                scoreImageView.setFitWidth(scoreImage.getWidth())
                scoreImageView.setFitHeight(scoreImage.getHeight())

                if (scoreModel.needScroll) {
                    Rectangle2i rect = scoreModel.systemRectPx
                    Point2i position = rect.position

                    // System drawing
                    if (SHOW_CURRENT_SYSTEM) {
                        canvas.setWidth(scoreImage.getWidth())
                        canvas.setHeight(scoreImage.getHeight())
                        GraphicsContext gc = canvas.getGraphicsContext2D()
                        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight())
                        gc.setStroke(Color.RED)
                        gc.strokeRect(position.x, position.y,
                                      rect.size.width, rect.size.height)
                    }

                    double MARGIN_X = 20
                    double MARGIN_Y = 20
                    double x = position.x - MARGIN_X
                    double y = position.y - MARGIN_Y
                    // (hvalue - hmin) / (hmax - hmin) = hoffset / freeHspace
                    // freeHspace = contentWidth - viewportWidth                    
                    double hmin = scrollPane.getHmin()
                    double hmax = scrollPane.getHmax();
                    //double hvalue = scrollPane.getHvalue()
                    double contentWidth = scrollPane.getContent().getLayoutBounds().getWidth()
                    double viewportWidth = scrollPane.getViewportBounds().getWidth()
                    double freeHspace = Math.max(0,  contentWidth - viewportWidth)
                    // (vvalue - vmin) / (vmax - vmin) = voffset / freeVspace
                    // freeVspace = contentHeight - viewportHeight
                    double vmin = scrollPane.getVmin();
                    double vmax = scrollPane.getVmax();
                    //double vvalue = scrollPane.getVvalue();
                    double contentHeight = scrollPane.getContent().getLayoutBounds().getHeight()
                    double viewportHeight = scrollPane.getViewportBounds().getHeight()
                    double freeVspace = Math.max(0,  contentHeight - viewportHeight)

                    double hvalue = (x / freeHspace) * ((hmax - hmin) + hmin)
                    double vvalue = (y / freeVspace) * ((vmax - vmin) + vmin)

                    final Timeline timeline = new Timeline()

                    final KeyValue hkv = new KeyValue(scrollPane.hvalueProperty(), hvalue);
                    final KeyFrame hkf = new KeyFrame(Duration.millis(500), hkv)
                    timeline.getKeyFrames().add(hkf)

                    final KeyValue vkv = new KeyValue(scrollPane.vvalueProperty(), vvalue);
                    final KeyFrame vkf = new KeyFrame(Duration.millis(500), vkv)
                    timeline.getKeyFrames().add(vkf)

                    timeline.play()                    
                }
            } catch(Exception ex) {
                println "Exception in draw: " + ex.getMessage()
            }
            //} // end run
        } )
    }

    // utils

    void loadScore(Score score) {
        scoreModel.loadScore(score)
        initScore()
        draw()
    }

    void initScore() {
        scrollPane.setPannable(true)
        scrollPane.setCursor(Cursor.OPEN_HAND)
        setMode(ScoreMode.PANNING)
        initScoreButtons()
    }

    void initScoreButtons() {
        currentPageField.setText("" + (scoreModel.getCurrentPage()+1))
        currentZoomField.setText("" + (int)(scoreModel.getCurrentZoom() * 100))
        handlePagesButtons()
    }

    // actions

    @FXML void scoreImageClick(MouseEvent event) {
        // println "clicked at $event"
        if (getMode() == ScoreMode.SELECT) {
            scoreModel.pickMP(new Point2f((float) event.getX(), (float) event.getY()))
            draw()
        }
    }

    void fileopen() {
        FileChooser fileChooser = new FileChooser()
        fileChooser.setTitle("Open Score File")
        fileChooser.getExtensionFilters().addAll(
             new ExtensionFilter("MusicXML Files", "*.xml"),
             new ExtensionFilter("ZongXML Files", "*.zml"),
             new ExtensionFilter("All Files", "*.*"))
        fileChooser.setInitialDirectory(
            new File(System.getProperty("user.home"))
        )
        Stage stage = (Stage)getScene().getWindow()
        File selectedFile = fileChooser.showOpenDialog(stage)
        if (selectedFile != null) {
            try {
                scoreModel.loadScore(selectedFile)
                initScoreButtons()
             } catch(Exception ex) {
                println "Exception: " + ex.getMessage()
            }
            draw()
        }
    }

    /*import com.thoughtworks.xstream.XStream
    import java.io.OutputStream
    import java.nio.file.Files
    import java.nio.file.Path

    public static Score loadFromXML(String xml) {
        XStream xstream = new XStream()        
        xstream.alias("score", Score.class) 
        Score score = (Score)xstream.fromXML(xml)       
        return score
    }

    public static void saveToXML(Score score, OutputStream xmlStream) {
        XStream xstream = new XStream()        
        xstream.alias("score", Score.class)
        // String xml = xstream.toXML(score)
        OutputStream xmlStream = Files.newOutputStream(path, StandardOpenOption.CREATE,
                                                             StandardOpenOption.WRITE,
                                                             StandardOpenOption.TRUNCATE_EXISTING)      
        xstream.toXML(score, xmlStream)
    }*/

    void filesave() {
        // TODO
        // saveToXML(scorePath.zml)
    }

    void filesaveas() {
        FileChooser fileChooser = new FileChooser()
        fileChooser.setTitle("Save Score as...")        
        fileChooser.getExtensionFilters().addAll(
            new ExtensionFilter("MIDI Files", "*.mid"),
            new ExtensionFilter("WAV Files", "*.wav"),
            //new ExtensionFilter("OGG Files", "*.ogg"),
            //new ExtensionFilter("PDF Files", "*.pdf"),
            new ExtensionFilter("PNG Files", "*.png"))
        fileChooser.setInitialDirectory(
            new File(System.getProperty("user.home"))
        )
        fileChooser.setInitialFileName("score")
        Stage stage = (Stage)getScene().getWindow()
        File file = fileChooser.showSaveDialog(stage)
        if (file != null) {
            try {
                ExtensionFilter selectedExtf = fileChooser.getSelectedExtensionFilter()
                if (!file.getName().contains(".") && selectedExtf.getExtensions().size() > 0) {
                    String extf = selectedExtf.getExtensions()[0]
                    String ext = extf.substring(extf.lastIndexOf("."))
                    File f = new File(file.getAbsolutePath() + ext)
                    if (f.exists() && !f.isDirectory()) { 
                        Alert alert = new Alert(AlertType.CONFIRMATION, "File chosen without extension and ${f.getName()} exist. \nDo you want to replace it?")
                        Optional<ButtonType> result = alert.showAndWait();
                        if (result.isPresent() && result.get() == ButtonType.OK) {
                            scoreModel.saveAs(f)
                        }
                    }
                } else
                    scoreModel.saveAs(file)
            } catch (IOException ex) {
                println(ex.getMessage())
            }
        }
    }



    void replay() {
        scoreModel.stop()
        scoreModel.play()
    }

    void play() {
        scoreModel.play()
    }

    void pause() {
        scoreModel.pause()
    }

    void stop() {
        scoreModel.stop()
        canvas.setWidth(0)
        canvas.setHeight(0)
        GraphicsContext gc = canvas.getGraphicsContext2D()
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight())
    }

    void followPlayback() {
        scoreModel.followPlayback = followPlaybackButton.selected
    }

    void metronome() {
        // TODO
    }

    void panMode() {
        setMode(ScoreMode.PANNING)
    }

    void selectMode() {
        setMode(ScoreMode.SELECT)
    }

    void firstpage() {
        scoreModel.setCurrentPage(0)
        handlePagesButtons()
    }

    void lastpage() {
        scoreModel.setCurrentPage(scoreModel.getLastPage())
        handlePagesButtons()
    }

    void prevpage() {
        scoreModel.setCurrentPage(scoreModel.getCurrentPage()-1)
        handlePagesButtons()
    }

    void nextpage() {
        scoreModel.setCurrentPage(scoreModel.getCurrentPage()+1)
        handlePagesButtons()
    }

    void handlePagesButtons() {
        nextpageButton.setDisable(scoreModel.getCurrentPage() >= scoreModel.getLastPage())
        prevpageButton.setDisable(scoreModel.getCurrentPage() == 0)
    }

    void zoom100() {
        scoreModel.setCurrentZoom(1)
    }

    void zoomin() {
        scoreModel.setCurrentZoom(scoreModel.getCurrentZoom() + 0.25f)
    }

    void zoomout() {
        scoreModel.setCurrentZoom(scoreModel.getCurrentZoom() - 0.25f)
    }

    // free resources
    void close() {
        scoreModel.playback.close()
    }
}