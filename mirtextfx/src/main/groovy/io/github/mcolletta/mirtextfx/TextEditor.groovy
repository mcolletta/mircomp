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

package io.github.mcolletta.mirtextfx

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.ConcurrentSkipListSet;

import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files

import javafx.util.Callback;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import javafx.geometry.Bounds;
import javafx.scene.layout.Priority;
import javafx.stage.Popup;
import javafx.scene.control.Label;
import javafx.scene.control.ComboBox;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.FXCollections;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ToggleButton
import javafx.scene.control.Button
import javafx.scene.control.TextField
import javafx.scene.control.ButtonType
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType
import javafx.beans.property.SimpleStringProperty;

import javafx.beans.value.ObservableValue;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.text.Text;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.stage.FileChooser
import javafx.stage.FileChooser.ExtensionFilter

import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty

import javafx.event.ActionEvent
import javafx.event.EventHandler

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.reactfx.Subscription;
import org.reactfx.collection.ListModification;
import org.fxmisc.richtext.model.Paragraph;
import org.fxmisc.richtext.model.TwoDimensional.Position;
import org.fxmisc.undo.UndoManager;
import org.fxmisc.wellbehaved.event.Nodes;
import static org.fxmisc.wellbehaved.event.EventPattern.*;
import static org.fxmisc.wellbehaved.event.InputMap.*;

import static org.fxmisc.richtext.model.TwoDimensional.Bias.*;

import io.github.mcolletta.mirfoldertreeview.FolderTreeListenerList
import io.github.mcolletta.mirfoldertreeview.FolderTreeViewListener
import io.github.mcolletta.mirfoldertreeview.FolderTreeViewEvent
import io.github.mcolletta.mirfoldertreeview.PathRequestType

import io.github.mcolletta.mirutils.TabContent


public class TextEditor extends VBox implements FolderTreeListenerList, TabContent {

    ObjectProperty<Path> filePath = new SimpleObjectProperty<>()
    Path getFilePath() {
        return filePath.get()
    }
    void setFilePath(Path path) {
        filePath.set(path)
    }

    String suggestedOpenSaveFolder = System.getProperty("user.home")
    String suggestedOpenSaveFileName = "newfile.mirchord"

    @FXML private VirtualizedScrollPane vScrollPane;
    @FXML private CodeArea codeArea;

    @FXML private ComboBox selectFontSize
    @FXML private ComboBox selectTheme
    @FXML private ComboBox selectMode

    @FXML private Button filesaveButton
    @FXML private Button undoButton
    @FXML private Button redoButton

    private UndoManager undoManager

    // Find&Replace Popup
    @FXML private ToggleButton showSearchButton
    @FXML private VBox searchBox;
    @FXML private TextField findText
    @FXML private Button findButton
    @FXML private Button prevButton
    @FXML private Button nextButton
    @FXML private TextField replaceText
    @FXML private Button replaceButton
    @FXML private Button replaceAllButton
    private int currentSearchIndex = -1

    private ConcurrentSkipListSet<Integer> searchTracker

    boolean GUESS_TEXT_FILE = false
    final int MAX_FILE_SIZE = 500000
    
    private SyntaxHighlighter codeSyntax;
    private Popup autocompletePopup;
    private ListView<Snippet> listView;
    private FilteredList<Snippet> snippets;

    // autocomplete
    private String filter;
    private boolean autocompletePerformed = false

    private int position = 0;
    private String inserted = "";
    private String removed = "";

    private String TAB_SPACES = "    ";
    public setNumSpacesTab(int num=4) {
        TAB_SPACES = ""
        for(int i = 0; i < num; i++)
            TAB_SPACES += " "
    }

    final KeyCombination keyCombinationCtrlSpace = new KeyCodeCombination(KeyCode.SPACE, KeyCodeCombination.CONTROL_DOWN);
    final KeyCombination keyCombinationFind = new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN);

    String getTabType() { return "TextEditor"; }

    public TextEditor(Mode mode, Path path=null, boolean selectModeDisabled=true) {
        loadControl();
        codeSyntax = mode.getHighlighter();
        setMode(mode);
        initEditor();

        selectMode.setDisable(selectModeDisabled);

        Nodes.addInputMap(codeArea, consume(keyPressed(KeyCode.Z, KeyCombination.CONTROL_DOWN), { event ->  if (hasUndo()) undo(); } ));
        Nodes.addInputMap(codeArea, consume(keyPressed(KeyCode.Y, KeyCombination.CONTROL_DOWN), { event -> if (hasRedo()) redo(); } ));
       
        showSearchButton.selectedProperty().bindBidirectional(searchBox.visibleProperty())
        searchBox.managedProperty().bind(searchBox.visibleProperty())
        searchBox.addEventHandler( KeyEvent.KEY_PRESSED, keyEvent ->
        {
            if ( keyEvent.getCode() == KeyCode.ESCAPE ) {
                hideSearch()
            }
        });
        searchBox.setVisible(false)
        searchTracker = new ConcurrentSkipListSet<Integer>()

        if (path != null) {
            boolean validPath = true
            validPath = checkFile(path.toFile())
            if (validPath) {
                setFilePath(path)
                if (getFilePath() != null) {
                    filesaveButton.setDisable(true)
                    setValue(getFilePath().getText())
                    markClean()
                }
                else {
                    filesaveButton.setDisable(false)
                }
            }
        }

        filePath.addListener(new ChangeListener(){
            @Override public void changed(ObservableValue o,Object oldVal, Object newVal){
                if (newVal != null) {
                    Path newPath = newVal as Path
                    fireFolderTreeUpdated(new FolderTreeViewEvent([origin: this,
                                                                   path: newPath,
                                                                   requestType: PathRequestType.MODIFY,
                                                                   fileType: ""]))
                }
            }
        }) 
    }

    private void loadControl() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(getClass().getSimpleName() + ".fxml"))

        fxmlLoader.setRoot(this)
        fxmlLoader.setController(this)

        try {
            fxmlLoader.load()
        } catch (IOException exception) {
            throw new RuntimeException(exception)
        }
    }

    public initEditor() {
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea))
        undoManager = codeArea.getUndoManager()

        HandleUndoRedoButtons();

        // Font size =================================================================

        codeArea.setStyle("-fx-font-size:12pt");

        selectFontSize.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observableValue, Object oldValue, Object newValue) {
                if (newValue != null) {
                    codeArea.setStyle("-fx-font-size:" + newValue);
                }
            }
        })

        // =================================================================
        

        final KeyCombination keyCombinationComment = new KeyCodeCombination(KeyCode.SLASH, KeyCombination.CONTROL_DOWN)
        final KeyCombination keyCombinationSave = new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN)
        this.addEventFilter(KeyEvent.KEY_PRESSED, { KeyEvent evt ->
            if (keyCombinationComment.match(evt)) {
                comment()
            }
            if (keyCombinationSave.match(evt)) {
                filesave()
            }
        })

        initAutoComplete()

        initCodeUpdate()

        final Pattern whiteSpace = Pattern.compile( "^\\s+" );
        codeArea.addEventHandler( KeyEvent.KEY_PRESSED, keyEvent ->
        {
            if ( keyEvent.getCode() == KeyCode.ENTER ) {
                int caretPosition = codeArea.getCaretPosition();
                int currentParagraph = codeArea.getCurrentParagraph();
                String paragraphText = codeArea.getParagraph( currentParagraph ).getSegments().get( 0 );
                String spaces = "";
                if (paragraphText.size() > 0 && paragraphText[-1] == "{")
                    spaces += TAB_SPACES;
                Matcher m0 = whiteSpace.matcher( paragraphText );
                if ( m0.find() )
                    spaces += m0.group();
                if (spaces.size() > 0)
                    Platform.runLater( () -> codeArea.insertText( caretPosition+1, spaces ) );
            }
            else if ( keyEvent.getCode() == KeyCode.TAB ) {
                int caretPosition = codeArea.getCaretPosition();
                Platform.runLater( () -> codeArea.replaceText(caretPosition, caretPosition+1, TAB_SPACES) );
            }
            else if (keyCombinationCtrlSpace.match(keyEvent))
            {
                computeAutoComplete()
            }
            else if (keyCombinationFind.match(keyEvent)) {
                showSearch()
            }
            else if ( keyEvent.getCode() == KeyCode.ESCAPE ) {
                hideSearch()
            }
        });

        loadThemes()   
        selectTheme.getSelectionModel().select(0)
        loadModes()
    }

    public void changeTheme(String newTheme, String oldTheme) {
        if (oldTheme != null)
            this.getStylesheets().remove(getClass().getResource("resources/${oldTheme}.css").toExternalForm());
        this.getStylesheets().add(getClass().getResource("resources/${newTheme}.css").toExternalForm());
    }

    private void loadThemes() {
        selectTheme.setItems(FXCollections.observableArrayList(Theme.AVAILABLE_THEMES));
        selectTheme.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                if (newValue != null && oldValue != newValue) {
                    Theme oldTheme = oldValue as Theme
                    Theme theme = newValue as Theme
                    changeTheme(theme.path, oldTheme?.path)
                }
            }
        })
    }

    private void loadModes() {
        selectMode.setItems(FXCollections.observableArrayList(Mode.AVAILABLE_MODES));
        selectMode.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                if (newValue != null && oldValue != newValue) {
                    Mode mode = selectMode.getSelectionModel().getSelectedItem() as Mode
                    setMode(mode)
                    codeSyntax = mode.getHighlighter()
                    listView.setItems(codeSyntax.getSnippets())
                    // full highlight
                    codeSyntax.highlight(codeArea, -1, null, null)                }
            }
        })
    }

    Mode getMode() {
        return (Mode)selectMode.getSelectionModel().getSelectedItem()
    }

    void setMode(Mode mode) {
        selectMode.setValue(mode)
    }

    private initCodeUpdate() {
        Subscription cleanupWhenNoLongerNeedIt = codeArea
                        .plainTextChanges()
                        .successionEnds(Duration.ofMillis(50))
                        .subscribe(tc -> {
                            codeSyntax.highlight(codeArea, tc.getPosition(), tc.getInserted(), tc.getRemoved());
                            computeAutoComplete();
                            if (searchTracker.size() > 0)
                                hideSearch();
                            HandleUndoRedoButtons()
                        });
    }

    private initAutoComplete() {
        autocompletePopup = new Popup();
        autocompletePopup.setAutoHide(true);
        
        listView = new ListView<Snippet>(codeSyntax.getSnippets());
        
        listView.setCellFactory(new Callback<ListView<Snippet>, ListCell<Snippet>>() {
            @Override
            public ListCell<Snippet> call(ListView<Snippet> snippets) {
                ListCell listCell =  new ListCell<Snippet>() {
                    @Override
                    protected void updateItem(Snippet item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item != null) {
                            setText(item.getSuggestion());
                        } else {
                            setText(null);
                        }
                    }
                };
                return listCell;
           }
        });

        listView.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                int caretPosition = codeArea.getCaretPosition();
                codeArea.replaceText(caretPosition - filter.length(), caretPosition,
                                     listView.getSelectionModel().getSelectedItem().getSnippet());
                autocompletePopup.hide();
                autocompletePerformed = true;
            }
        });

        listView.addEventHandler( KeyEvent.KEY_PRESSED, keyEvent ->
        {
            if ( keyEvent.getCode() == KeyCode.ENTER && filter != null) {
                int caretPosition = codeArea.getCaretPosition();
                codeArea.replaceText(caretPosition - filter.length(), caretPosition,
                                     listView.getSelectionModel().getSelectedItem().getSnippet());
                autocompletePopup.hide();
                autocompletePerformed = true;
            }
            else if ( keyEvent.getCode() == KeyCode.ESCAPE ) {
                autocompletePopup.hide()
            }
        });

        autocompletePopup.getContent().add(listView);

        codeArea.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                autocompletePopup.hide();
            }
        });
    }

    private computeAutoComplete() {
        Optional<Bounds> opt = codeArea.getCaretBounds();
        if (opt.isPresent()) {

            def sb = new StringBuilder();

            Bounds caretBounds = opt.get();
            int currentParagraph = codeArea.getCurrentParagraph();
            String paragraph = codeArea.getParagraph(currentParagraph).getSegments().get(0);

            int startPos = codeArea.getAbsolutePosition( currentParagraph , 0 );
            int caretPosition = codeArea.getCaretPosition();
            for(int i = (caretPosition - startPos - 1); i >= 0 ; i--) {
                char ch = (char)paragraph.charAt(i);
                if (!Character.isWhitespace(ch))
                    sb.insert(0, ch);
                else
                    break;
            }
            filter = sb.toString();
            codeSyntax.updateSnippets(filter)

            if (filter.length() >= 3 && listView.getItems().size() > 0) {
                double minWidth = listView.getMinWidth();
                double prefHeight = codeArea.getLayoutBounds().getHeight();
                double currentHeight = 0D
                double textHeight = 0D
                for(Snippet item: listView.getItems()) {
                    Text text = new Text(item.getSuggestion());
                    text.applyCss();
                    minWidth = Math.max( minWidth, text.getLayoutBounds().getWidth() + 20 );
                    if (textHeight == 0)
                        textHeight = text.getLayoutBounds().getHeight()
                    currentHeight += textHeight + 15;
                }
                listView.setMinWidth(minWidth);
                currentHeight = Math.min( prefHeight, currentHeight )
                listView.setPrefHeight(currentHeight);

                autocompletePopup.show(codeArea, caretBounds.getMaxX(), caretBounds.getMaxY())
                listView.getSelectionModel().select(0);
                autocompletePopup.requestFocus()
            }
            else
                autocompletePopup.hide()
        }
        else
            autocompletePopup.hide()
    }


    // -------------------------------------

    void showAlert(AlertType atype, String text) {
        Alert alert = new Alert(atype, text)
        Optional<ButtonType> result = alert.showAndWait()
    }

    boolean checkFile(File file) {
        Path path = file.toPath()
        if (Files.size(path) > MAX_FILE_SIZE) {
            showAlert(AlertType.ERROR, "File size exceeds limit")
            return false
        }
        if (GUESS_TEXT_FILE) {
            if (!isTextFile(file)) {
                showAlert(AlertType.ERROR, "Not a text file")
                return false
            }
        }
        return true
    }

    boolean isTextFile(File file) {
        int ascii = 0
        int tot = 0
        for (byte b : file.getBytes()) { // getBytes("UTF-8")
            if ((b >= 0x20  &&  b <= 0x7E) || (b in [0x09, 0x0A, 0x0C, 0x0D])) {
                ascii++
            }
            tot++
        }
        // println (ascii/tot)
        return (ascii/tot) >= 0.9D
    }

    String getValue() {
        return codeArea.getContent().getText()
    }

    void setValue(String content) {
        codeArea.replaceText(0, getValue().length(), content);
        undoReset()
    }


    private void HandleUndoRedoButtons() {
        filesaveButton.setDisable(isClean())
        undoButton.setDisable(!hasUndo())
        redoButton.setDisable(!hasRedo())
    }

    void replaceTabswithSpaces() {
        def selection = codeArea.getCaretSelectionBind();
        int startVisibleParIdx = selection.getStartParagraphIndex()
        int endVisibleParIdx = startVisibleParIdx + selection.getParagraphSpan()

        if ( endVisibleParIdx-startVisibleParIdx > 0 ) {
            for (int p = startVisibleParIdx; p < endVisibleParIdx; p++) {
                int startPos = codeArea.getAbsolutePosition( p , 0 );
                int endPos = codeArea.getAbsolutePosition( p, codeArea.getParagraphLength( p ) );
                String text = codeArea.getText(p)
                codeArea.replaceText(startPos, endPos, text.replaceAll("\t", TAB_SPACES));
                // String newText = codeArea.getText(p)
                // codeArea.setStyleSpans(startPos, computeHighlighting(newText));
            }
            codeSyntax.highlight(codeArea, -1, null, null)
        }        
    }

    // actions

    void fileopen() {
        FileChooser fileChooser = new FileChooser()
        fileChooser.setTitle("Open Source Code File")
        fileChooser.getExtensionFilters().addAll(
            new ExtensionFilter("All Files", "*.*"),
            new ExtensionFilter("Mirchord Files", "*.mirchord"))
            // new ExtensionFilter("Groovy Files", "*.groovy"),
            // new ExtensionFilter("XML Files", "*.xml"),
            // new ExtensionFilter("JSON Files", "*.json"))
        fileChooser.setInitialDirectory(
            new File(suggestedOpenSaveFolder)
        )
        Stage stage = (Stage)getScene().getWindow()
        File selectedFile = fileChooser.showOpenDialog(stage)
        if (selectedFile != null) {
            if (checkFile(selectedFile)) {
                String fileContent = selectedFile.getText('UTF-8') // or .text
                setValue(fileContent)
                String filename = selectedFile.getName() 
                String fileExt = filename[filename.lastIndexOf('.')..-1]
                switch (fileExt) {
                    case ".mirchord":
                        setMode(Mode.MirChord)
                        listView.setItems(codeSyntax.getSnippets())
                        break
                    case '.groovy':
                        setMode(Mode.Groovy)
                        listView.setItems(codeSyntax.getSnippets())
                        break
                    default:
                        break
                }
                setFilePath(selectedFile.toPath())
                undoReset()
                markClean()
            }
        }
    }

    void filesave() {
        if (getFilePath() != null) {
            File file = getFilePath().toFile()
            try {
                file.setText(getValue())
                markClean()
                HandleUndoRedoButtons()
            } catch (IOException ex) {
                println(ex.getMessage())
            }
        } else {
            filesaveas()
        }
    }

    void filesaveas() {
        FileChooser fileChooser = new FileChooser()
        fileChooser.setTitle("Save Source Code as...")        
        fileChooser.getExtensionFilters().addAll(
            new ExtensionFilter("All Files", "*.*"),
            new ExtensionFilter("Mirchord Files", "*.mirchord"))
            // new ExtensionFilter("Groovy Files", "*.groovy"),
            // new ExtensionFilter("XML Files", "*.xml"),
            // new ExtensionFilter("JSON Files", "*.json"))
        fileChooser.setInitialDirectory(
            new File(suggestedOpenSaveFolder)
        )
        fileChooser.setInitialFileName(suggestedOpenSaveFileName)
        Stage stage = (Stage)getScene().getWindow()
        File file = fileChooser.showSaveDialog(stage)
        if (file != null) {
            try {
                file.setText(getValue())
                setFilePath(file.toPath())
                markClean()
                HandleUndoRedoButtons()
            } catch (IOException ex) {
                println(ex.getMessage())
            }
        }
    }

    public reloadfile() {
        if (getFilePath() != null) {
            // TODO
            codeArea.replaceText(0, getValue().length()-1, getFilePath().getText());
            setValue(getFilePath().getText())
        }
    }

    void cut() {
        codeArea.cut()
    }

    void copy() {
        codeArea.copy()
    }

    void paste() {
        codeArea.paste()
    }

    void comment() {
        codeSyntax.comment(codeArea)
    }


    boolean hasUndo() {
        return undoManager.isUndoAvailable();
    }

    boolean hasRedo() {
        return undoManager.isRedoAvailable();
    }

    void undoReset() {
        undoManager.forgetHistory()
    }

    void undo() {
        codeArea.undo()
        HandleUndoRedoButtons()
    }

    void redo() {
        codeArea.redo()
        HandleUndoRedoButtons()
    }

    void markClean() {
        undoManager.mark();
    }

    boolean isClean() {
        return undoManager.isAtMarkedPosition();
    }

    // Find & Replace ============================================

    void showSearch() {
        searchBox.setVisible(true)
        if (searchBox.isVisible())
            findText.requestFocus()
    }

    void hideSearch() {
        searchBox.setVisible(false)
        findText.setText("")
        replaceText.setText("")
        searchTracker.clear()
        handleSearchButtons()
        codeSyntax.highlight(codeArea, -1, null, null)
    }

    void handleSearchButtons() {
        prevButton.setDisable(searchTracker.size() == 0 || !(currentSearchIndex > 0))
        nextButton.setDisable(searchTracker.size() == 0 || !(currentSearchIndex < searchTracker.size()-1))
    }

    void find() {
        String searched = findText.getText()
        String text = getValue()

        searchTracker.clear()
        Pattern searchedPattern = Pattern.compile(searched)
        Matcher matcher = searchedPattern.matcher(text)
        while(matcher.find()) {
            searchTracker.add(matcher.start())
        }
        if (searchTracker.size() > 0) {
            currentSearchIndex = 0
            int pos = searchTracker[currentSearchIndex]
            codeArea.moveTo(pos);
            int paragraphIndex = codeArea.getCurrentParagraph()
            codeArea.showParagraphInViewport( paragraphIndex );
            int len = searched.length()
            codeArea.selectRange(pos, pos + len);

            for(int i=0; i < searchTracker.size(); i++) {
                int p = searchTracker[i]
                StyleSpans<Collection<String>> spans = codeArea.getStyleSpans(p, p + len);
                StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
                spansBuilder.add(Collections.singleton("searched"), len);
                def searchedStyle = spansBuilder.create()
                def updated = spans.overlay( searchedStyle, (original, added) -> original + added );
                codeArea.setStyleSpans(p, updated);
            }
        }

        handleSearchButtons()
    }

    void prev() {
        String searched = findText.getText()
        String text = getValue()
        currentSearchIndex -= 1
        int pos = searchTracker[currentSearchIndex]
        codeArea.moveTo(pos);
        int paragraphIndex = codeArea.getCurrentParagraph()
        codeArea.showParagraphInViewport( paragraphIndex );
        codeArea.selectRange(pos, pos + searched.length());

        handleSearchButtons()
    }

    void next() {
        String searched = findText.getText()
        String text = getValue()
        currentSearchIndex += 1
        int pos = searchTracker[currentSearchIndex]
        codeArea.moveTo(pos);
        int paragraphIndex = codeArea.getCurrentParagraph()
        codeArea.showParagraphInViewport( paragraphIndex );
        codeArea.selectRange(pos, pos + searched.length());

        handleSearchButtons()
    }

    void replace() {
        String searched = findText.getText()
        String replacement = replaceText.getText()
        String text = getValue()
        int pos = searchTracker[currentSearchIndex]
        int len = searched.length()
        codeArea.replaceText(pos, pos + len, replacement);
        find()
    }

    void replaceAll() {
        // TODO
        String searched = findText.getText()
        String replacement = replaceText.getText()
        String text = getValue()
        codeArea.replaceText(0, text.size()-1, text.replaceAll(searched, replacement))
    }

    // ==========================================================
}