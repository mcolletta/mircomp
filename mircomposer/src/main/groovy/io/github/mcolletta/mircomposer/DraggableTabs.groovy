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

import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty

import javafx.scene.control.Label
import javafx.scene.control.TabPane
import javafx.scene.control.Tab
import javafx.scene.input.ClipboardContent
import javafx.scene.input.Dragboard
import javafx.scene.input.DragEvent
import javafx.scene.input.DataFormat
import javafx.scene.input.MouseEvent
import javafx.scene.input.TransferMode
import javafx.scene.SnapshotParameters
import javafx.scene.image.WritableImage
import javafx.event.EventHandler


class DraggableTabs {

    private static final String DRAG_TAB_KEY = "TAB"
    static final DataFormat tabDataFormat = new DataFormat(DRAG_TAB_KEY)
    static final ObjectProperty<Tab> draggingTab = new SimpleObjectProperty<>()

    static Tab createTab(String text="untitled")
    {
        final Tab tab = new Tab()
        final Label label = new Label(text)
        tab.setGraphic(label)
        label.setOnDragDetected(new EventHandler<MouseEvent>()
        {
            @Override
            public void handle(MouseEvent event)
            {
                WritableImage snapshot = tab.getContent().snapshot(new SnapshotParameters(), null)
                Dragboard dragboard = label.startDragAndDrop(TransferMode.MOVE)
                dragboard.setDragView(snapshot)
                Map<DataFormat, Object> dragContent = [:]
                dragContent.put(tabDataFormat, "")
                dragboard.setContent(dragContent)
                draggingTab.set(tab)
                event.consume()
            }
        })
        label.setOnDragOver(new EventHandler<DragEvent>()
        {
            @Override
            public void handle(DragEvent event)
            {
                final Dragboard dragboard = event.getDragboard()
                if (dragboard.hasContent(tabDataFormat))
                {
                    event.acceptTransferModes(TransferMode.MOVE)
                    event.consume()
                }
            }
        })
        label.setOnDragDropped(new EventHandler<DragEvent>()
        {
            @Override
            public void handle(DragEvent event)
            {
                TabPane tabPane = tab.getTabPane()
                int tabIndex = -1
                if (tabPane != null)
                    tabIndex = tabPane.getTabs().indexOf(tab)
                if (tabIndex >= 0) {
                    final Dragboard dragboard = event.getDragboard()
                    if (dragboard.hasContent(tabDataFormat)) {
                        final Tab dTab = draggingTab.get()
                        if (tab != dTab) {
                            dTab.getTabPane().getTabs().remove(dTab)
                            tabPane.getTabs().add(tabIndex, dTab)
                            tabPane.getSelectionModel().select(dTab)
                            event.setDropCompleted(true)
                            event.consume()
                        }
                    }
                }
            }
        })
        label.setOnDragDone(new EventHandler<DragEvent>()
        {
            @Override
            public void handle(DragEvent event)
            {
                draggingTab.set(null)
                event.consume()
            }
        })
        return tab
    }

    static void setTabLabelText(Tab tab, String text)
    {
        Label label = (Label) tab.getGraphic()
        label.setText(text)
    }

}