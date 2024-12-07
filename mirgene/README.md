# mirgene

**MirGene** is a Grammatical Evolution library

The library has several components.

**MirGram** is the component that generates programs based on novel _stochastic weighted and adaptable_ attribute grammars created by Mirco Colletta for musical purposes. 
The weights and the mechanism to control the derivation length borrows ideas from the software _ImproVisor_ (updating arithmetic attributes) but it is introduced the concept of "guards" to avoid undesirable matches. 
A MirGram has semantic actions that allows to:
 - modify the current grammar (by adding new rules) during the derivation
 - generate unique names for symbol/variable definition (global symbol table)
Moreover, inside an action the results from previous NonTerminals (NT) within the current production rule are available and can be used.
While the implementation of the adaptability is based on the concept of _Christiansen Grammars_ in MirGram the grammars are not explicit as attributes (which are reserved to control the derivation lenght) but they are managed implicity. The _cut_ action is instead inspired by the _Prolog_ language.


**MirGene** is the genetic programming component based on sexual reproduction and that uses _female choice selection_.

See the _References_ section for some of the papers that have inspired this work.


MirGene is part of MirComp and is released under the GPL3 License, see the LICENSE file for details.

Copyright (c) 2016-2024 Mirco Colletta


## References

_A Grammatical Approach to Automatic Improvisation_
Robert M. Keller, David R. Morrison

_Christiansen Grammar Evolution: Grammatical Evolution With Semantics_
Alfonso Ortega, Marina de la Cruz, Manuel Alfonseca

_Sexual Selection for Genetic Algorithms_
Kai Song Goh, Andrew Lim, Brian Rodrigues 

_Grammatical Evolution: Evolving Programs for an Arbitrary Language_
Conor Ryan, J.J. Collins, Michael O'Neill

_Grammar based function definition in Grammatical Evolution_
Michael O'Neill, Conor Ryan

_Grammatical Evolution: A tutorial using gramEval_
Farzad Noorian, Anthony M. de Silva, Philip H.W. Leong

