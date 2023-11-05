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

import static java.lang.Math.*

import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer


enum FitnessType { MAX, MIN }

abstract class FitnessFunction {
	
	FitnessType type = FitnessType.MIN
	
	abstract float rate(Individual individual)
}

class SymbolicRegression extends FitnessFunction {

	Closure target
	IntRange range
	Closure safeDivision = { float n, float d -> 
		if (d == 0)
			return Float.MAX_VALUE
		if (n == 0)
			return 0.0f
		(n / d)
	}

	Map<Float,Float> data

    SymbolicRegression(Map<Float,Float> data, FitnessType type=FitnessType.MIN) {
		this.type = type
		this.data = data
    }
	

    float rate(Individual individual) {
    	String program = individual.blueprint
    	float fitness = (type == FitnessType.MAX) ? Float.MIN_VALUE : Float.MAX_VALUE
    	if (program in ["<ABORTED>", "<NULL>"])
    		return fitness
    	float total = 0.0
    	try {
			def importCustomizer = new ImportCustomizer()
			// importCustomizer.addStarImports('java.util.concurrent')
			importCustomizer.addStaticStars('java.lang.Math')
			def configuration = new CompilerConfiguration()
			configuration.addCompilationCustomizers(importCustomizer)
			def shell = new GroovyShell(configuration)
			Script script = shell.parse(program)
			Binding binding = new Binding([div: safeDivision])
			script.setBinding(binding)		
			Closure clos = (Closure) script.run()

			data.each { float distance, float target ->
				float d = (float)clos(distance)
			    total += (float) log(1 + abs( d - target ) )
			}
			float N = data.keySet().size()
			fitness = (float) (total / N)
		} catch(Exception ex) {
			println "abort ${program} with exception ${ex.message}"
		}
		return fitness
    }

}

