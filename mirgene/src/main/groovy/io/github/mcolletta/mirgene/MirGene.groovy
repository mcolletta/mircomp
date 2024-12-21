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

package io.github.mcolletta.mirgene

import java.lang.Math
import java.util.Random
import java.util.concurrent.ConcurrentMap

import java.util.stream.Collectors

import com.xenoage.utils.pdlib.PList

import static Gender.*


class MirGene {
	List<IndividualFitness> population
	int populationSize = 10
	int eliteSize = 3
	float mutationProbability = 0.05f
	int codonSize = 255
	int genomeSize = 100
	float fit_min_value = 0.01f
	float fit_max_value = 1000f
	MirGram grammar
	FitnessFunction fitnessFunction
	int maxGenerations = 50
	Comparator byFitness
	float defaultFitness
	IndividualFitness best
	Random rng
	Boolean parallel

	MirGene(MirGram grammar, FitnessFunction fitnessFunction, long seed=-1, boolean pars=false) {
		this.grammar = grammar
		this.fitnessFunction = fitnessFunction
		this.maxGenerations = maxGenerations
		if (fitnessFunction.type == FitnessType.MAX) {
			byFitness = byFitnessMAX
			defaultFitness = Float.MIN_VALUE
		}
		else {
			byFitness = byFitnessMIN
			defaultFitness = Float.MAX_VALUE
		}
		if (seed>0)
			rng = new Random(seed)
		else
			rng = new Random()
		parallel = pars
		population = []
	}

	int randRange(IntRange range) {
		rng.nextInt(range.to - range.from + 1) + range.from
	}

	Comparator<IndividualFitness> byFitnessMIN = new MinFitnessComparator()

	Comparator<IndividualFitness> byFitnessMAX = new MaxFitnessComparator()

	Individual createIndividual(PList genotype=null) {
		if (genotype == null) {
			genotype = new PList()
			(1..genomeSize).each {
				genotype += rng.nextInt(codonSize)
			}
		}
		float genderProb=0.5f
		Gender gender = (rng.nextFloat() < genderProb) ? FEMALE : MALE
		String blueprint = ""
		if (grammar != null)
			blueprint = grammar.translate(genotype)
		return new Individual(gender, genotype, codonSize, genomeSize, blueprint)
	}

	void initPopulation() {
		population = rate( (1..populationSize).collect {
			 createIndividual()
		} )
	}

	List<IndividualFitness> mate(Individual ind1, Individual ind2) {
        PList<Integer> dna1 = ind1.dna
        PList<Integer> dna2 = ind2.dna
		PList<Integer> child_dna1, child_dna2
		def cr = crossover(dna1,dna2)
		child_dna1 = cr[0]
		child_dna2 = cr[1]
		child_dna1 = flipMutation(child_dna1)
		child_dna2 = flipMutation(child_dna2)
		def child1 = createIndividual(child_dna1)
		def child2 = createIndividual(child_dna2)
		List<IndividualFitness> offspring = rate([child1,child2])
		return offspring
    }

	List<PList<Integer>> crossover(PList<Integer> dna1, PList<Integer> dna2) {
		PList<Integer> child_dna1, child_dna2
		int min = Math.min(dna1.size(),dna2.size())
		if (rng.nextDouble() < 0.5) { //two point crossover?
			int mid = (int)(min / 2)
			List<Integer> cps = [randRange((1..mid)),randRange((mid..min-2))]
			child_dna1 = dna1.subList(0,cps[0]).plusAll( dna2.subList(cps[0],cps[1]).plusAll( dna1.subList(cps[1],dna1.size()) ) )
			child_dna2 = dna2.subList(0,cps[0]).plusAll( dna1.subList(cps[0],cps[1]).plusAll( dna2.subList(cps[1],dna2.size()) ) )
		}
		else { //one point crossover
			int cp = randRange((1..min-1))
			child_dna1 = dna1.subList(0,cp).plusAll( dna2.subList(cp,dna2.size()) )
			child_dna2 = dna2.subList(0,cp).plusAll( dna1.subList(cp,dna1.size()) )
		}
		assert child_dna1.size() == dna1.size()
		assert child_dna2.size() == dna2.size()
		assert child_dna1.size() == genomeSize
		assert child_dna2.size() == genomeSize
		return [child_dna1, child_dna2]
	}

	PList<Integer> flipMutation(PList<Integer> dna) {
		for(int i=0; i < dna.size(); i++) {
			int codon = dna[i]
			if (rng.nextDouble() < mutationProbability) {
				int x = rng.nextInt(codonSize)
				if (i == 0)
					dna = (new PList<Integer>([x])).plusAll( dna.subList(1,dna.size()) )
				else if (i == (dna.size() - 1))
					dna = dna.subList(0,dna.size()-1) + x
				else
					dna = (dna.subList(0,i-1) + x).plusAll( dna.subList(i,dna.size()) )
			}
		}
		assert dna.size() == genomeSize
		return dna
	}

	IndividualFitness tournamentSelection(List<IndividualFitness> contestants, int tournamentSize=2) {
		List<IndividualFitness> competitors = []
		for(int i = 1; i <=tournamentSize; i++) {
			competitors << contestants[rng.nextInt(contestants.size()-1)]
		}
		competitors.sort(byFitness)
		return competitors.first()
	}

	List<IndividualFitness> generationalReplacement(List<IndividualFitness> newPopulation) {
		population.sort(byFitness)
		population[0..eliteSize-1].each {
			newPopulation << it
		}
		newPopulation.sort(byFitness)
		if (newPopulation.size() > populationSize)
			return newPopulation[0..populationSize-1]
		return newPopulation
	}

	List<IndividualFitness> rate(List<Individual> individuals) {
		return individuals.collect { Individual ind ->
			return [fitness: fitnessFunction.rate(ind), individual: ind] as IndividualFitness
		}
	}

	IndividualFitness step() {
		List<IndividualFitness> newPopulation
		if (parallel) {
			List<IndividualFitness> females = population
												.parallelStream()
												.filter({ IndividualFitness it ->
														  it.individual.gender == FEMALE })
												.collect(Collectors.toList())  as List<IndividualFitness>
			List<IndividualFitness> males = population
												.parallelStream()
												.filter({ IndividualFitness it ->
														  it.individual.gender == MALE })
												.collect(Collectors.toList())  as List<IndividualFitness>
			if (females.size() == 0)
				throw new Exception("No more females")
			if (males.size() < 2)
				throw new Exception("Not enough males")
			newPopulation = females.parallelStream().collect({ IndividualFitness it ->
				Individual parent1 = it.individual
				Individual parent2 = tournamentSelection(males).individual
				mate(parent1,parent2)
			}).flatten() as List<IndividualFitness>
		}
		else {
			List<IndividualFitness> females = population.findAll { it.individual.gender == FEMALE }
			List<IndividualFitness> males = population.findAll { it.individual.gender == MALE }
			if (females.size() == 0)
				throw new Exception("No more females")
			if (males.size() < 2)
				throw new Exception("Not enough males")
			newPopulation = females.collect {
				Individual parent1 = it.individual
				Individual parent2 = tournamentSelection(males).individual
				mate(parent1,parent2)
			}.flatten() as List<IndividualFitness>
		}
		population = generationalReplacement(newPopulation)
		IndividualFitness generationBest
		if (best == null)
			generationBest = population.first()
		else
			generationBest = [best, population.first()].min(byFitness)
		return generationBest
	}

	boolean checkTermination() {
		return ( best != null &&
			( (fitnessFunction.type == FitnessType.MIN && best.fitness <= fit_min_value)
			|| (fitnessFunction.type == FitnessType.MAX && best.fitness >= fit_max_value) )
			)
	}

	IndividualFitness runGE() {
		if (population.size() == 0)
			initPopulation()
		else if (population.size() != populationSize)
			throw new Exception("wrong population size")
		for(int i=1; i <= maxGenerations && !checkTermination(); i++) {
			best = step()
			println "Generation: $i"
			println "Best Individual with fitness ${best.fitness}"
		}
		return best
	}

	int hamming (PList<Integer> s1, PList<Integer> s2) {
		if(s1.size() != s2.size()) return -1
		int count = 0
		for (int i = 0; i < s1.size(); ++i)
			if(s1[i] != s2[i]) ++count
		return count
	}

}