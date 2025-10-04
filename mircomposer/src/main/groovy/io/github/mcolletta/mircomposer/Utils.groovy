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

package io.github.mcolletta.mircomposer

import java.io.File
import java.io.IOException
import java.nio.file.Path


class Utils {

    protected static final File makeDir(File parentFolder, String name) {
        File folder
        if (parentFolder.isDirectory()) {
            folder = new File(parentFolder,name)
            boolean res = false
            try {
                res = folder.mkdir()
            } catch (IOException ex) {
                println ex.getMessage()
                ex.printStackTrace()
            }
        }
        return folder
    }

    protected static String getFileExt(Path path) {
        String ext = ""
        String fileName = path.getFileName()
        int i = fileName.toString().lastIndexOf('.')
        if (i > 0) {
            ext = fileName.substring(i+1);
        }
        return ext
    }

}