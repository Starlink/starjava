/*
 * $Id: Rule.java,v 1.9 2001/07/22 22:01:50 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.parser2d;
import diva.sketch.recognition.Scene;
import diva.sketch.recognition.SceneElement;
import diva.sketch.recognition.CompositeElement;
import diva.sketch.recognition.Type;

/**
 * A parse rule interface that matches the RHS
 * of a production and generates the LHS.
 * 
 * @author  Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.9 $
 */
public interface Rule {
	/**
	 * Return the LHS type of the rule.
	 */
	public Type getLHSType();
	
	/**
	 * Return the RHS types of the rule.
	 */
	public Type[] getRHSTypes();
	
	/**
	 * Return the RHS names of the rule.
	 */
	public String[] getRHSNames();
	
	/**
	 * Match the given scene elements and return a resulting
	 * element, or return null if there is no match.
	 */	
	public CompositeElement match(CompositeElement[] rhs, Scene db);
}

