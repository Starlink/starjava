/*
 * 
 * Copyright 2003 Sun Microsystems, Inc. All Rights Reserved. 
 * 
 * The contents of this file are subject to the Sun Community Source
 * License v3.0/Jini Technology Specific Attachment v 1.0 (the
 * "License"). You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://wwws.sun.com/software/jini/licensing/SCSL3_JiniTSA1.html.
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License. 
 * 
 * The Reference Code is Jini Technology Core Platform code, v2.0. The
 * Developer of the Reference Code is Sun Microsystems, Inc. 
 * 
 * Contributor(s): Sun Microsystems, Inc. 
 * 
 * The contents of this file comply with the Jini Technology Core
 * Platform Compatibility Kit, v2.0A. 
 * 
 * Tester(s): Sun Microsystems, Inc. 
 * 
 * Test Platform(s): 
 * Java 2 SDK, Standard Edition, Version 1.4.1_02 for
 * Solaris(TM)SPARC/x86
 * Java 2 SDK, Standard Edition, Version 1.4.1_02 for Linux (Intel x86)
 * Java 2 SDK, Standard Edition, Version 1.4.1_02 for Windows (Intel
 * Platform)
 * 
 */

package uk.ac.starlink.startask;

import net.jini.security.AccessPermission;

/**
 * Represents permissions used to express the access control policy for the
 * Server class. The name specifies the names of the method which you have
 * permission to call using the matching rules provided by AccessPermission.
 *
 * @author Sun Microsystems, Inc.
 * 
 */
public class ServerPermission extends AccessPermission {

    /**
     * Creates an instance with the specified target name.
     *
     * @param name the target name
     */
    public ServerPermission(String name) {
	super(name);
    }
}
