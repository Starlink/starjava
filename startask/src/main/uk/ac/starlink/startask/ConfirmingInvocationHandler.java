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

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.util.Collection;
import javax.swing.JOptionPane;
import net.jini.core.constraint.MethodConstraints;
import net.jini.jeri.BasicInvocationHandler;
import net.jini.jeri.ObjectEndpoint;

/** Defines an invocation handler that confirms calls. */
public class ConfirmingInvocationHandler extends BasicInvocationHandler {

    /**
     * Create a confirming invocation handler from a basic handler and
     * constraints.
     */
    public ConfirmingInvocationHandler(ConfirmingInvocationHandler other,
				MethodConstraints serverConstraints) {
	super(other, serverConstraints);
    }

    /**
     * Create a confirming invocation handler for the object endpoint and
     * server constraints.
     */
    public ConfirmingInvocationHandler(ObjectEndpoint oe,
                                MethodConstraints serverConstraints) {
	super(oe, serverConstraints);
    }

    /**
     * Asks whether the call should be made, then writes a call identifier
     * before the arguments.
     */
    protected void marshalArguments(Object proxy,
				    Method method,
				    Object[] args,
				    ObjectOutputStream out,
				    Collection context)
	throws IOException
    {
	long callId = System.currentTimeMillis();
	int result = JOptionPane.showConfirmDialog(
	    null,
	    "Make remote call?" +
	    "\n  Object: " + proxy +
	    "\n  Method: " + method.getName() +
	    "\n  Call id: " + callId,
	    "Make remote call?",
	    JOptionPane.OK_CANCEL_OPTION);
	if (result != JOptionPane.OK_OPTION) {
	    throw new RuntimeException("Client cancelled call");
	}
	out.writeLong(callId);
	super.marshalArguments(proxy, method, args, out, context);
    }
}
