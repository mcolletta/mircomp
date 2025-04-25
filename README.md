![MirComp Logo](/docs/assets/images/logo.png)


**MirComp** is a framework for assisted and algorithmic music composition.


See the official website for details:

[https://mcolletta.github.io/mircomp](https://mcolletta.github.io/mircomp)


## FEATURES


### MirIDE

The MirComp Integrated Development Environment is a software application that provides a comprehensive set of tools to manage a music project the same way developers organize their products.
It consists of several components: syntax highlighted code editor with autocomplete, graphical score viewer, MIDI editor, folder viewer, scripting capabilities, soundfonts selector.

![MirIDE](/docs/assets/images/screenshots/miride-hp.png)


### MirChord

MirChord is a powerful but easy-to-learn textual notation language tailored for assisted/algorithmic music composition.
Crafting a score with MirChord is very simple thanks to its peculiar traits like: relative octave entry, sticky duration, leadsheet-style chord symbols, unpitched notes and percussion instruments, automatic note splitting for measures and automatic beaming, universal key with solfeggio syllables and Roman numerals, etc...

![MirChord](/docs/assets/images/screenshots/mirtext-hp.png)


### MirMidi

MirMidi is a MIDI editor and sequencer. It is bundled up with a piano roll editor that supports midi notes entry and manipulation together with a control changes (CCs) curves automation editor, instruments and tempo changes components.


![MirMidi](/docs/assets/images/screenshots/mirmidi-hp.png)

### MirScore

MirScore is a common practice notation viewer and midi player for graphical preview of scores. It allows to navigate a score just by dragging the mouse (panning mode) and supports pagination and zooming. Moreover, you can control the playback position by point and click on a staff and visualize the progression of the MIDI execution with the automatic update of a graphical control element (follow playback mode).

![MirScore](/docs/assets/images/screenshots/mirscore.png)


### MirGene

MirGene is a Grammatical Evolution (GE) library that uses powerful context sensitive grammars (MirGram) that are stochastic, weighted and adaptable (i.e. it can add new rules during the derivation using the results from previous NonTerminals).
Moreover, it can control the length of the results through arithmetic attributes and guards.

![MirGene](/docs/assets/images/screenshots/genetic.png)


### MircOracle

MircOracle is library that implements popular machine learning algorithms like dictionary based predictors and factor oracles.


![MircOracle](/docs/assets/images/screenshots/automaton.png)

### MirSynth

MirSynth is a library for sound synthesis that allows to build virtual MIDI instruments.

![MirSynth](/docs/assets/images/screenshots/synth.png)



## LICENSE

Copyright (C) 2016-2025 Mirco Colletta

MirComp is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

MirComp is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with MirComp.  If not, see <http://www.gnu.org/licenses/>.


## Citation

Suggested BibTex entry:

```
@misc{mcolletta2025,
  author = {Colletta, Mirco},
  title = {MirComp: a framework for assisted and algorithmic music composition},
  year = {2016--2025},
  publisher = {GitHub},
  journal = {GitHub repository},
  howpublished = {\url{https://github.com/mcolletta/mircomp}}
}
```
