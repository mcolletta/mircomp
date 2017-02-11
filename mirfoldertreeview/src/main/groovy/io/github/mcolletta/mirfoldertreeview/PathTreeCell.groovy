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

package io.github.mcolletta.mirfoldertreeview

import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.Paths
import static java.nio.file.StandardCopyOption.*

import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty

import javafx.stage.Stage
import javafx.stage.FileChooser

import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent

import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView

import javafx.scene.image.Image
import javafx.scene.image.ImageView

import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent

import javafx.event.Event
import javafx.event.EventHandler
import javafx.event.ActionEvent

import javafx.scene.control.ContextMenu
import javafx.scene.control.MenuItem
import javafx.scene.control.TextField
import javafx.scene.control.TreeCell
import javafx.scene.control.TextInputDialog

import javafx.scene.control.Dialog
import javafx.scene.control.DialogPane
import javafx.scene.control.ButtonType

//import groovy.transform.CompileStatic

/*With CompileStatic got this error:
[Static type checking] - Reference to method is ambiguous. 
Cannot choose between [void javafx.scene.control.Cell <T extends java.lang.Object>#startEdit(), 
void io.github.mcolletta.mirfoldertreeview.PathTreeCell#startEdit(), 
void javafx.scene.control.TreeCell <T extends java.lang.Object>#startEdit()]*/

//@CompileStatic
public class PathTreeCell extends TreeCell<Path> {

    FolderTreeView owner

    private TextField textField
    private Path editingPath
    private ContextMenu folderMenu = new ContextMenu()
    private ContextMenu fileMenu = new ContextMenu()

    boolean isFolder
    boolean isLeaf

    //BooleanProperty cannotPaste = new SimpleBooleanProperty(true)

    Image folderCollapsedImage=new Image(getClass().getResource("resources/icons/folder.png").toURI().toString())
    Image folderExpandedImage=new Image(getClass().getResource("resources/icons/sc_open.png").toURI().toString())
    Image fileImage=new Image(getClass().getResource("resources/icons/file_doc.png").toURI().toString())
    Image groovyImage=new Image(getClass().getResource("resources/icons/groovy.png").toURI().toString())
    Image javaImage=new Image(getClass().getResource("resources/icons/java.png").toURI().toString())
    Image musicImage=new Image(getClass().getResource("resources/icons/music.png").toURI().toString())
    Image midiImage=new Image(getClass().getResource("resources/icons/midi.png").toURI().toString())
    Image waveformImage=new Image(getClass().getResource("resources/icons/mix_audio.png").toURI().toString())
    Image cdaudioImage=new Image(getClass().getResource("resources/icons/cdaudio_mount.png").toURI().toString())
    Image xmlImage=new Image(getClass().getResource("resources/icons/xml.png").toURI().toString())
    Image pdfImage=new Image(getClass().getResource("resources/icons/pdf.png").toURI().toString())
    Image archiveImage=new Image(getClass().getResource("resources/icons/tar.png").toURI().toString())


    public PathTreeCell(FolderTreeView owner) {
    	// Folder Menu
        MenuItem addFolderMenu = new MenuItem("New Folder")
        addFolderMenu.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
            	String folderName = "";
                TextInputDialog dialog = new TextInputDialog()
                dialog.setTitle("Folder name")
                dialog.setHeaderText("New folder")
                dialog.setContentText("Please enter the new folder name:")
                Optional<String> result = dialog.showAndWait()
                if (result.isPresent()) {
                    folderName = result.get()
                    Path path = getTreeItem().getValue()
                    Path newFolder = Paths.get(path.toAbsolutePath().toString(), folderName)
                    if (!Files.exists(newFolder)) {
	                    try {
	                    	Files.createDirectory(newFolder)
		                } catch (Exception ex) {
		                    println ex.getMessage()
		                }
		            }
                }
            }
        })
        MenuItem importMenu = new MenuItem("Import...");
        importMenu.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                Path source = showImportDialog()
                if (source != null) {
                    Path destPath = getTreeItem().getValue()
                    Path target = destPath.resolve(source.getFileName())
                    Files.copy(source, target, REPLACE_EXISTING)
                }
            }
        })
        MenuItem addMirChordFileMenu = new MenuItem("New MirChord File")
        addMirChordFileMenu.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                owner.newFile(getTreeItem().getValue(), "mirchord")
            }
        })
        MenuItem addGroovyFileMenu = new MenuItem("New Groovy File")
        addGroovyFileMenu.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                owner.newFile(getTreeItem().getValue(), "groovy")
            }
        })
        MenuItem addMidiFileMenu = new MenuItem("New MIDI File")
        addMidiFileMenu.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                owner.newFile(getTreeItem().getValue(), "mid")
            }
        })
        MenuItem renameFolderMenu = new MenuItem("Rename...");
        renameFolderMenu.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                startEdit()
            }
        })
        MenuItem pasteMenu = new MenuItem("Paste")
        pasteMenu.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                Clipboard clipboard = Clipboard.getSystemClipboard()
                if (clipboard.hasFiles()) {
                    List<File> files = clipboard.getFiles()
                    if (files.size() > 0) {
                        Path source = files[0].toPath()
                        Path destPath = getTreeItem().getValue()
                        Path target = destPath.resolve(source.getFileName())
                        Files.copy(source, target, REPLACE_EXISTING)
                        //cannotPaste.setValue(!Clipboard.getSystemClipboard().hasFiles())
                    }
                }
            }
        })
        //pasteMenu.disableProperty().bind(cannotPaste)
        MenuItem deleteFolderMenu = new MenuItem("Delete Folder");
        deleteFolderMenu.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                boolean ok = showConfirmDialog("Are you sure to delete " + getTreeItem().getValue().getFileName() + "?")
                if (ok)
                    Files.deleteIfExists(getTreeItem().getValue())
            }
        })

        folderMenu.getItems().addAll(addFolderMenu,
                                     importMenu,
                                     addMirChordFileMenu,
                                     addGroovyFileMenu,
                                     addMidiFileMenu,
                                     renameFolderMenu,
                                     pasteMenu,
                                     deleteFolderMenu)

        // File Menu
        MenuItem openFileMenu = new MenuItem("Open");
        openFileMenu.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                owner.openFile(getTreeItem().getValue())
            }
        })
        MenuItem renameFileMenu = new MenuItem("Rename...")
        renameFileMenu.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                startEdit()
            }
        })
        MenuItem copyMenu = new MenuItem("Copy")
        copyMenu.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                Clipboard clipboard = Clipboard.getSystemClipboard()
                ClipboardContent content = new ClipboardContent()
                content.putFiles(java.util.Collections.singletonList(getTreeItem().getValue().toFile()))
                clipboard.setContent(content)
                //cannotPaste.setValue(!clipboard.hasFiles())
            }
        })        
        MenuItem deleteFileMenu = new MenuItem("Delete File");
        deleteFileMenu.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                boolean ok = showConfirmDialog("Are you sure to delete " + getTreeItem().getValue().getFileName() + "?")
                if (ok)
                    Files.deleteIfExists(getTreeItem().getValue())
            }
        })

        fileMenu.getItems().addAll(openFileMenu,
                                   renameFileMenu,
                                   copyMenu,
                                   deleteFileMenu)

        addEventFilter(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void  handle(MouseEvent event){
                if (event.getClickCount() > 1) {
                    //TableCell cell = (TableCell)event.getSource()
                    isFolder = Files.isDirectory(treeItem.getValue(), LinkOption.NOFOLLOW_LINKS)
                    if (!isFolder)
                        owner.openFile(getTreeItem().getValue())
                }
            }
        })
    }

    boolean showConfirmDialog(String text) {
        Dialog<ButtonType> dialog = new Dialog<>()
        dialog.setContentText(text)
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL)
        dialog.getDialogPane().lookupButton(ButtonType.OK)
        Optional<ButtonType> result = dialog.showAndWait()
        if (result.isPresent() && result.get() == ButtonType.OK) {
            return true
        }
        return false
    }

    Path showImportDialog() {
        FileChooser fileChooser = new FileChooser()
        fileChooser.setTitle("Import File")
        fileChooser.setInitialDirectory(
            new File(System.getProperty("user.home"))
        )
        Stage stage = (Stage)getScene().getWindow()
        File selectedFile = fileChooser.showOpenDialog(stage)
        if (selectedFile != null) {
            return selectedFile.toPath()
        }
        return null
    }

    @Override
    protected void updateItem(Path item, boolean empty) {
        super.updateItem(item, empty)
        if (empty) {
            setText(null)
            setGraphic(null)
        } else {
            String displayText = getDisplayText()
            if (isEditing()) {
                if (textField != null) {
                    textField.setText(displayText)
                }
                setText(null);
                setGraphic(textField)
            } else {
            	TreeItem<Path> treeItem = getTreeItem()
		        isLeaf = treeItem.isLeaf()
		    	isFolder = Files.isDirectory(treeItem.getValue(), LinkOption.NOFOLLOW_LINKS)
                setText(displayText)
                setGraphic(getCellImage())
                if (isFolder) {
                    setContextMenu(folderMenu)
                } else {
                    setContextMenu(fileMenu)
                }
            }
        }
    }

    @Override
    public void startEdit() {
        super.startEdit()
        if (textField == null){
            createTextField()
        }
        setText(null)
        setGraphic(textField)
        textField.selectAll()
        if (getItem() == null) {
            editingPath = null
        } else {
            editingPath =getItem()
        }
    }

    @Override
    public void commitEdit(Path pathItem) {
        // rename the file or directory
        if (editingPath != null) {
            try {
                Files.move(editingPath, pathItem)
            } catch (IOException ex) {
                cancelEdit()
            }
        }
        super.commitEdit(pathItem)
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit()
        setText(getDisplayText())
        setGraphic(getCellImage())
    }

    private void createTextField() {
        textField = new TextField(getDisplayText())
        textField.setOnKeyReleased({ KeyEvent t ->
            if (t.getCode() == KeyCode.ENTER){
                Path path = Paths.get(getItem().getParent().toAbsolutePath().toString(), textField.getText())
                commitEdit(path)
            } else if (t.getCode() == KeyCode.ESCAPE) {
                cancelEdit()
            }
        })
    }

    private String getDisplayText() {
        Path path = getItem()
        if (path.getFileName() == null) {
            return path.toString()
        } else {
            return path.getFileName().toString()
        }
    }

    private ImageView getCellImage() {
        ImageView imageView = new ImageView()
        if (isFolder) {
        	if (getTreeItem().isExpanded())
                imageView.setImage(folderExpandedImage)
            else
            	imageView.setImage(folderCollapsedImage)
        } else {
            String fileType = getFileExt(getTreeItem().getValue())
            switch(fileType) {
                case "mirchord":
                    imageView.setImage(musicImage)
                    break
                case "groovy":
                    imageView.setImage(groovyImage)
                    break
                case "java":
                    imageView.setImage(javaImage)
                    break
                case "mid":
                    imageView.setImage(midiImage)
                    break
                case "sf2":
                case "sf3":
                case "dls":
                    imageView.setImage(cdaudioImage)
                    break
                case "wav":
                    imageView.setImage(waveformImage)
                    break
                case "mxml":
                case "xml":
                    imageView.setImage(xmlImage)
                    break
                case "pdf":
                    imageView.setImage(pdfImage)
                    break
                case "zip": 
                case "gz":
                case "tar":
                case "jar":
                    imageView.setImage(archiveImage)
                    break
                default:
                    imageView.setImage(fileImage)
                    break
            }
        }
        return imageView
    }

    // Utils
    String getFileExt(Path path) {
        String ext = ""
        String fileName = path.getFileName()
        int i = fileName.toString().lastIndexOf('.')
        if (i > 0) {
            ext = fileName.substring(i+1);
        }
        return ext
    }

}