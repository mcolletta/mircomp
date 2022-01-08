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

import java.nio.file.Paths
import java.nio.file.Path

import javax.xml.xpath.*

import java.io.File
import java.io.FileOutputStream

import org.w3c.dom.Document
import org.w3c.dom.Element

import com.xenoage.utils.jse.xml.XMLReader
import com.xenoage.utils.jse.xml.XMLWriter


class ConfigurationManager {

    private static XPath xpath = XPathFactory.newInstance().newXPath()

    static Element getElementFromXPath(Element element, String path) {
        return xpath.evaluate(path, element, XPathConstants.NODE) as Element
    }

    static NodeList getElementListFromXPath(Element element, String path) {
        return xpath.evaluate(path, element, XPathConstants.NODESET) as NodeList
    }

    static void updateElement(Element element, String text) {
        element.setTextContent(text)
    }

    static Map<String,Path> read(Path configFilePath) {
        Map<String,Path> config = [:]
        Path parent = configFilePath.getParent()
        Document doc = XMLReader.read(configFilePath.toFile().getText())
        Element root = XMLReader.root(doc)
        Element soundbank = getElementFromXPath(root, "//soundbank")
        if (soundbank != null) {
            String pathAttr = XMLReader.attribute(soundbank,"path")
            if (pathAttr != null && pathAttr != "")
                config["soundbank"] = Paths.get(pathAttr)
                //config["soundbank"] = parent.resolve(Paths.get(pathAttr))
        }
        return config
    }

    static void write(Map<String,Path> config, Path configFilePath) {
        Path parent = configFilePath.getParent()
        Document doc = XMLWriter.createEmptyDocument()
        Element root = XMLWriter.addElement("configuration", doc)
        Element soundbank = XMLWriter.addElement("soundbank", root)
        if (config.containsKey("soundbank"))
            soundbank.setAttribute("path", config["soundbank"].toString())
            //soundbank.setAttribute("path", parent.relativize(config["soundbank"]).toString())
        else
            soundbank.setAttribute("path", "")
        XMLWriter.writeFile(doc, new FileOutputStream(configFilePath.toFile()))
    }

}