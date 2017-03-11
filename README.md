# MirComp

Computer music composition framework

MirComp is implemented with the [Groovy programming language](http://www.groovy-lang.org/) and is currently a work in progress.

## Uncomplete list of Features

### MirChord
A powerful but easy-to-learn textual notation language with a very specific design goal in mind: assisted/algorithmic music composition. 
Crafting a score with MirChord is very simple thanks to the suggestions (autocomplete) provided by the (syntax highlighted) editor and its special traits like relative octave entry, sticky duration, leadsheet-style chord symbols, etc...  
In addition, it is quite easy to extend language functionalities by means of Groovy scripting thorough annotated methods injection.  
Moreover, the framework includes a common practice notation viewer and midi player for graphical preview of scores (with automatic note splitting for measures, automatic beaming, point and click feature, pagination and zooming, ect...)

### MirMidi
A versatile midi application that supports midi notes entry and manipulation in a graphical piano roll editor together with a control changes (CCs) automation curves editor.

**...many more features to come!**

Copyright (C) 2016-2017 Mirco Colletta

This project is released under the GPLv3 license, see the LICENSE file for details


![MirComp demo screenshot 1](/resources/images/Screenshot1.png)

![MirComp demo screenshot 2](/resources/images/Screenshot2.png)

![MirComp demo screenshot 3](/resources/images/Screenshot3.png)