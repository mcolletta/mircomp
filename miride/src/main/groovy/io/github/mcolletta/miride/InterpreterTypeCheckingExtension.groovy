/*
 * Copyright (C) 2016-2021 Mirco Colletta
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

package io.github.mcolletta.miride

import io.github.mcolletta.mirsynth.SimpleMidiPlayer

import java.nio.file.Path

import org.codehaus.groovy.ast.ClassNode
//import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.transform.stc.AbstractTypeCheckingExtension
import org.codehaus.groovy.transform.stc.StaticTypeCheckingVisitor

import org.codehaus.groovy.ast.GenericsType
import static org.codehaus.groovy.ast.ClassHelper.make
import static org.codehaus.groovy.ast.tools.GenericsUtils.makeClassSafeWithGenerics

import groovy.transform.CompileStatic

@CompileStatic
public class InterpreterTypeCheckingExtension extends AbstractTypeCheckingExtension {

	Map<String, ClassNode> typesMap = new HashMap<String, ClassNode>()                 

    public InterpreterTypeCheckingExtension(final StaticTypeCheckingVisitor typeCheckingVisitor) {       
        super(typeCheckingVisitor)
        typesMap.put("projectPath", make(Path))
        ClassNode configNode = makeClassSafeWithGenerics(make(Map.class), new GenericsType(make(String.class)), new GenericsType(make(Path.class)))
        typesMap.put("config", configNode)
        typesMap.put("MidiPlayer", make(SimpleMidiPlayer))
    }

    /*@Override
    void onMethodSelection(Expression expression, MethodNode target) { }*/

    @Override
    public boolean handleUnresolvedVariableExpression(final VariableExpression vexp) {
        if (typesMap.containsKey(vexp.getName())) {
            storeType(vexp, typesMap[vexp.getName()])
            setHandled(true)
            return true
        }
        return false
    }

}