/*
 * Copyright (C) 2016-2017 Mirco Colletta
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

package io.github.mcolletta.mirchord.interpreter

import java.util.Map

import groovy.transform.CompileStatic
import org.codehaus.groovy.control.customizers.ImportCustomizer
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer
import org.codehaus.groovy.control.CompilerConfiguration

import io.github.mcolletta.mirchord.core.*
import static io.github.mcolletta.mirchord.core.Utils.*


@CompileStatic
class GroovyScriptInterpreter {
    boolean staticCompileScript = true
    GroovyShell shell
    String scriptName = "MirGroovyScript.groovy"
    Map<String,GroovyScriptImportType> imports

    GroovyScriptInterpreter(String name, Map<String,GroovyScriptImportType> importsType=[:]) {
        scriptName = name
        imports = importsType
        def importCustomizer = new ImportCustomizer()
        setDefaultImports(importCustomizer)
        if (imports.size() > 0)
        	addImports(importCustomizer)
        def configuration = new CompilerConfiguration()
        configuration.addCompilationCustomizers(importCustomizer)
        if (staticCompileScript) {
            configuration.addCompilationCustomizers(
                new ASTTransformationCustomizer(CompileStatic.class))
        }
        shell = new GroovyShell(configuration)
    }

    void setDefaultImports(ImportCustomizer importCustomizer) {
    	importCustomizer.addImports 'io.github.mcolletta.mirchord.interpreter.MirChord'
        importCustomizer.addStarImports 'io.github.mcolletta.mirchord.core'
        importCustomizer.addStaticStars 'io.github.mcolletta.mirchord.core.Utils'
        importCustomizer.addImports 'com.xenoage.utils.math.Fraction'
    	importCustomizer.addStaticStars 'com.xenoage.utils.math.Fraction'
    }

    void addImports(ImportCustomizer importCustomizer) {
    	for(Map.Entry<String,GroovyScriptImportType> e : imports.entrySet()) {
            String k = e.getKey()
            GroovyScriptImportType itype = e.getValue()
            switch(itype) {
            	case GroovyScriptImportType.IMPORTS:
            		importCustomizer.addImports(k)
            		break
            	case GroovyScriptImportType.IMPORTS_STAR:
            		importCustomizer.addStarImports(k)
            		break
            	case GroovyScriptImportType.IMPORT_STATIC_STARS:
            		importCustomizer.addStaticStars(k)
            		break
            	default:
            		break
            }
        }
    }

    Script getScript(String source) {
        return shell.parse(source, scriptName)
    }
}


@CompileStatic
enum GroovyScriptImportType {
		IMPORTS,
		IMPORTS_STAR, 
		IMPORT_STATIC_STARS
	}