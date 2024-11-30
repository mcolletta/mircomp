/*
 * Copyright (C) 2016-2024 Mirco Colletta
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

import java.io.IOException
import java.nio.file.DirectoryStream
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.WatchEvent
import java.nio.file.StandardCopyOption
import static java.nio.file.StandardWatchEventKinds.*

import javafx.application.Platform

import javafx.collections.FXCollections
import javafx.collections.ObservableList

import javafx.fxml.FXML
import javafx.fxml.FXMLLoader

import javafx.scene.layout.VBox
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.control.TreeCell

import javafx.scene.input.MouseEvent
import javafx.scene.input.ClipboardContent
import javafx.scene.input.Dragboard
import javafx.scene.input.DragEvent
import javafx.scene.input.KeyCode
import javafx.scene.input.TransferMode
import javafx.event.EventHandler

import javafx.scene.control.Dialog
import javafx.scene.control.DialogPane
import javafx.scene.control.ButtonType
import javafx.scene.control.ButtonBar.ButtonData

import javafx.scene.SnapshotParameters
import javafx.scene.image.WritableImage

import javafx.concurrent.Task


public class FolderTreeView extends VBox implements FolderTreeListenerList {

	@FXML private TreeView treeView
	private Task watchTask
	Path rootPath
	TreeItem<Path> root

	private volatile Thread  watchThread

	public FolderTreeView(String strPath=null) {

		loadControl()

		if (strPath != null)
			setRoot(strPath)
		else {
			TreeItem<String> noRoot = new TreeItem<String>("<NO FOLDER>")
			treeView.setRoot(noRoot)
		}
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

    public setRoot(String strPath) {
    	rootPath = (strPath != null) ? Paths.get(strPath) : Paths.get(System.getProperty("user.home"))
		root = getPathTreeItem(rootPath) 
		root.setExpanded(true) 
		treeView.setRoot(root)
		treeView.setCellFactory({ p -> 
                var treeCell = new PathTreeCell(this)
                makeDraggable(treeCell)
                return (TreeCell<Path>)treeCell
            })
		// watch service
		stopWatching()
        watchTask = new WatchFolderTask(rootPath, this)
        watchThread = new Thread(watchTask)
        watchThread.start()
    }

    public void stopWatching() {
    	if (watchTask != null && watchTask.isRunning()) {
            watchTask.cancel()
        }
    }

    TreeItem<Path> getPathTreeItem(Path path) {
		try {
			TreeItem<Path> pathTreeItem = new TreeItem<Path>(path)
			if (path != null && Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
				DirectoryStream<Path> dirs = Files.newDirectoryStream(path)
				ObservableList<TreeItem<Path>> children = FXCollections.observableArrayList();
			    for (Path dir : dirs) {
			        children.add(getPathTreeItem(dir))
			    }
			    pathTreeItem.getChildren().setAll(children)
			} else {
			   ObservableList<TreeItem<Path>> children = FXCollections.emptyObservableList()
			   pathTreeItem.getChildren().setAll(children)
			}		
		    return pathTreeItem
		} catch (IOException ex) {
		    println ex.getMessage()
		}
		return null
	}

    void TreeItemUpdater(TreeItem root, Path path, WatchEvent.Kind kind) {
		def toVisit = new Stack<TreeItem<Path>>()
		def visited = new Stack<TreeItem<Path>>()
		toVisit.push(root)
		while (toVisit.size() > 0) {
	        TreeItem<Path> node = toVisit.peek()
            //println "kind=" + kind + " node.getValue()=" + node.getValue()  + " path.getParent()=" + path.getParent()
	        if (kind == ENTRY_MODIFY && node.getValue() == path) {
	        	// println "Modified  node= " + node.getValue()
				return
	        }
	        if (kind == ENTRY_CREATE && node.getValue() == path.getParent()) {
	        	TreeItem<Path> newItem = new TreeItem<Path>(path)
	        	node.getChildren() << newItem
	        	//println "Added  node= " + newItem + " to node= " + node.getValue()
				return
	        }
	        if (node.getChildren().size() > 0) {
	            if (visited.size() == 0 || visited.peek() != node) {
	                visited.push(node)
	                def children = node.getChildren()
	                for(int i = children.size()-1; i >= 0; i--) {
	                	TreeItem<Path> child = children[i]
	                	if (kind == ENTRY_DELETE && child.getValue() == path) {
	                		node.getChildren().remove(child)
					        //println "Deleted  child= " + child.getValue() + " from parent= " + node.getValue()
					        return
					    }
	                	toVisit.push(child)
	                }
	                continue
	            }
	            visited.pop()
	        }
	        toVisit.pop()
	    }
	}

	void IterativeTraverse(TreeItem root) {
		def toVisit = new Stack<TreeItem>()
		def visited = new Stack<TreeItem>()
		toVisit.push(root)
		while (toVisit.size() > 0)
	    {
	        def node = toVisit.peek()
	        // Pre-Order
	        //println "Node= " + node.getValue()
	        if (node.getChildren().size() > 0)
	        {
	            if (visited.size() == 0 || visited.peek() != node)
	            {
	                visited.push(node)
	                def children = node.getChildren()
	                children.reverseEach {
	                	toVisit.push((TreeItem)it)
	                }
	                continue
	            }
	            visited.pop()
	        }
	        // Post-Order
	        //println "Node= " + node.getValue()
	        toVisit.pop()
	    }
	}


	// WatchFolder methods
	void FolderTreeUpdated(Path path, WatchEvent.Kind kind) {
		TreeItemUpdater(root, path, kind)
		PathRequestType requestType = getRequestTypeFromKind(kind)

		this.fireFolderTreeUpdated(new FolderTreeViewEvent([origin: this,
        											  path: path,
        											  requestType: requestType,
        											  fileType: ""]))
	}

	PathRequestType getRequestTypeFromKind(WatchEvent.Kind kind) {
		switch(kind) {
            case ENTRY_CREATE :
                return PathRequestType.NEW
                break
            case ENTRY_DELETE:
            	return PathRequestType.DELETE
                break
              case ENTRY_MODIFY:
            	return PathRequestType.MODIFY
                break  
            default:
                return PathRequestType.MODIFY
                break
        }
	}

	// Draggable TreeCell
    private TransferMode DRAG_COPY_OR_MOVE = TransferMode.MOVE

	void makeDraggable(PathTreeCell cell) {
        cell.setOnDragDetected(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event)
            { 
                TreeItem<Path> item = cell.getTreeItem()
                if (item != null && item.isLeaf()) {
                    WritableImage snapshot = cell.snapshot(new SnapshotParameters(), null)
                    Dragboard dragboard = cell.startDragAndDrop(TransferMode.COPY_OR_MOVE)
                    dragboard.setDragView(snapshot)
                    ClipboardContent clipboardContent = new ClipboardContent()
                    List<File> files = Arrays.asList(cell.getTreeItem().getValue().toFile())
                    clipboardContent.putFiles(files)
                    dragboard.setContent(clipboardContent)
                    event.consume()
                }
            }
        })
        cell.setOnDragOver(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event)
            {
                Dragboard dragboard = event.getDragboard()
                TreeItem<Path> item = cell.getTreeItem()
                if (item != null) {
                    boolean isDirectory = Files.isDirectory(item.getValue())
                    if (isDirectory
                        && event.getGestureSource() != cell
                        && dragboard.hasFiles()) {
                        Path targetPath = item.getValue()
                        PathTreeCell source = (PathTreeCell) event.getGestureSource()
                        Path sourceParentPath = source.getTreeItem().getValue().getParent()
                        if (sourceParentPath != targetPath) {
                            event.acceptTransferModes(TransferMode.COPY_OR_MOVE)
                        }
                    }
                }
                event.consume()
            }
        })
        cell.setOnDragDropped(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event)
            {
                Dragboard dragboard = event.getDragboard()
                TreeItem<Path> item = cell.getTreeItem()
                boolean success = false
                if (dragboard.hasFiles()) {
                    Path source = dragboard.getFiles()[0].toPath()
                    Path target = Paths.get(item.getValue().toString(), source.getFileName().toString())
                    if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                        Platform.runLater({
                            boolean ok = showDialog("File with same name in folder. Do you want to replace it?")
                            if (ok) {
	                            if (DRAG_COPY_OR_MOVE == TransferMode.COPY)
	                               Files.copy(source,target, StandardCopyOption.REPLACE_EXISTING)
                                else if (DRAG_COPY_OR_MOVE == TransferMode.MOVE)
                                   Files.move(source,target, StandardCopyOption.REPLACE_EXISTING)

	                        }
                        })
                    } else {
                        if (DRAG_COPY_OR_MOVE == TransferMode.COPY)
                           Files.copy(source,target)
                        else if (DRAG_COPY_OR_MOVE == TransferMode.MOVE)
                           Files.move(source,target)
                    }
                    success = true
                }
                event.setDropCompleted(success)
                event.consume()
            }

            public boolean showDialog(String text) {
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
        })
    }


	// Listeners

	public void openFile(Path path) {
        this.fireFileRequest(new FolderTreeViewEvent([origin: this,
        											  path: path,
        											  requestType: PathRequestType.OPEN]))
    }

    public void newFile(Path path, String fileType) {
        this.fireFileRequest(new FolderTreeViewEvent([origin: this,
        											  path: path,
        											  requestType: PathRequestType.NEW,
        											  fileType: fileType]))
    }

}


public trait FolderTreeListenerList {

    private List<FolderTreeViewListener> listeners = []

    public void addFolderTreeViewListener(FolderTreeViewListener listener) {
        if (listener != null) {
            listeners.add(listener)
        }
    }

    public void removeFolderTreeViewListener(FolderTreeViewListener listener) {
        if (listener != null) {
            listeners.remove(listener)
        }
    }

    // public FolderTreeViewListener[] getFolderTreeViewListener() {
    //     def _result = []
    //     _result.addAll(listeners)
    //     return _result as FolderTreeViewListener[]
    // }

    public void fireFolderTreeUpdated(FolderTreeViewEvent evt) {
        if (listeners != null && listeners.size() > 0) {
            def _list = new ArrayList<FolderTreeViewListener>(listeners)
            for (FolderTreeViewListener listener : _list ) {
                listener.folderTreeUpdated(evt)
            }
        }
    }

    public void fireFileRequest(FolderTreeViewEvent evt) {
        if (listeners != null && listeners.size() > 0) {
            def _list = new ArrayList<FolderTreeViewListener>(listeners)
            for (FolderTreeViewListener listener : _list ) {
                listener.fileRequest(evt)
            }
        }
    }
}

public interface FolderTreeViewListener {
	void folderTreeUpdated(FolderTreeViewEvent evt)
	void fileRequest(FolderTreeViewEvent evt)
}

enum PathRequestType {
	OPEN, NEW, DELETE, MODIFY
}

class FolderTreeViewEvent {
    Object origin
    Path path
	PathRequestType requestType
    String fileType
}