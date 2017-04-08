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

package io.github.mcolletta.miride

import java.security.Policy
import java.security.Permission
import java.security.Permissions
import java.security.PermissionCollection
import java.security.ProtectionDomain
import java.security.AllPermission
import java.io.FilePermission
import java.util.PropertyPermission

import java.lang.SecurityException

import groovy.transform.CompileStatic

@CompileStatic
public class InterpreterPolicy extends Policy {

	String filePath

	InterpreterPolicy(String filePath=null) {
		this.filePath = filePath
	}
    
    @Override
    public PermissionCollection getPermissions(ProtectionDomain domain) {
        if (domain.getClassLoader() instanceof GroovyClassLoader) {
            return restrictedPermissions()
        }
        else {
            return applicationPermissions()
        }        
    }

    private PermissionCollection restrictedPermissions() {
        Permissions permissions = new Permissions()
        if (filePath != null) {
        	String path = filePath + "/-"
        	permissions.add(new FilePermission(path, "read,write"))
        	permissions.add(new PropertyPermission("file.encoding", "read,write"))

        }
        return permissions
    }

    private PermissionCollection applicationPermissions() {
        Permissions permissions = new Permissions()
        permissions.add(new AllPermission())
        return permissions
    }
}

@CompileStatic
class InterpreterSecurityManager extends SecurityManager {
	@Override public void checkExit(int status) {
        try {
            checkExec("java -version")
        } catch(Exception ex) {
            throw new SecurityException("Cannot call method exit in this context")
        }
        super.checkExit(status)
	}
}