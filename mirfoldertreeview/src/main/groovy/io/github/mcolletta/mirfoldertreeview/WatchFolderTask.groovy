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

package io.github.mcolletta.mirfoldertreeview

import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.SimpleFileVisitor
import java.nio.file.FileVisitResult
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import java.nio.file.WatchService
import java.nio.file.attribute.BasicFileAttributes
import static java.nio.file.StandardWatchEventKinds.*

import javafx.concurrent.Task


public class WatchFolderTask extends Task<Void> {

    Path root
    FolderTreeView owner

    WatchService watcher
    Map<WatchKey,Path> watchKeys

    public WatchFolderTask(Path root, FolderTreeView owner) {
        this.root = root
        this.owner = owner
        watchKeys = [:]
    }
    
    @Override
    protected Void call() throws Exception {
        watcher = FileSystems.getDefault().newWatchService();
        startWatchingFolderTree(root)

        while (true) {
            WatchKey key
            try {
                key = watcher.take()
            } catch (InterruptedException ex) {
                println ex.getMessage()
                break
            }
            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind kind = event.kind()
                if (kind == OVERFLOW) {
                    continue
                }
                Path context = (Path)event.context()
                Path parent = null
                if (watchKeys.containsKey(key)) {
                    parent = watchKeys.get(key)
                    Path path = parent.resolve(context)
                    if (kind == ENTRY_CREATE) {
                        try {
                            if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS))
                                startWatchingFolderTree(path)
                        } catch (IOException ex) {
                            println ex.getMessage()
                        }
                    }
                    owner.FolderTreeUpdated(path, kind)
                }
            }
            boolean valid = key.reset()
            if (!valid) {
                watchKeys.remove(key)
                if (watchKeys.isEmpty()) {
                    break
                }
            }
        }
        return null
    }

    private void startWatchingFolder(Path path) {
        WatchKey watchKey = path.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
        watchKeys.put(watchKey, path)
    }

    private void startWatchingFolderTree(Path path) {
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path folder, BasicFileAttributes attrs) throws IOException
            {
                startWatchingFolder(folder)
                return FileVisitResult.CONTINUE
            }
        })
    }

    @Override
    protected void cancelled() {
        println "WatchFolderTask cancelled"
    }    
}