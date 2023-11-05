/*
 * Copyright (C) 2016-2023 Mirco Colletta
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

package io.github.mcolletta.mirgene

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

import java.lang.Math
import java.util.Random
import org.pcollections.*

import com.xenoage.utils.pdlib.PList

import groovy.transform.ToString
import groovy.transform.EqualsAndHashCode

enum Gender {MALE, FEMALE}

import static Gender.*

@ToString
@EqualsAndHashCode
class Individual {
	Gender gender
	PList<Integer> dna
	int genomeSize
	int codonSize
	String blueprint
	// T phenotype
	
	Individual(Gender gender, PList dna, int maxVal=255, int size=200, String blueprint) {
		this.gender = gender
		this.dna = dna		
		this.blueprint = blueprint
		codonSize = maxVal		
		genomeSize = size		
	}

}

@ToString
@EqualsAndHashCode
class IndividualFitness {
	float fitness
	Individual individual
}

class MinFitnessComparator implements Comparator<IndividualFitness> {
	public int compare(IndividualFitness if1, IndividualFitness if2) {
		return if1.fitness <=> if2.fitness
	}
}

class MaxFitnessComparator implements Comparator<IndividualFitness> {
	public int compare(IndividualFitness if1, IndividualFitness if2) {
		return if2.fitness <=> if1.fitness
	}
}
