/*
 * $Id: SingleRule.java,v 1.3 2001/07/22 22:01:51 johnr Exp $
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
 * A parse rule implementation that matches the RHS
 * of a production and generates the LHS.  Subclasses
 * fill in the match() method to impose the constraints
 * of the rule.
 * 
 * @author  Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.3 $
 */
public class SingleRule implements Rule {
    /**
     * The resultant type of the rule if the rule matches some input.
     */
    private Type _lhsType;

    /**
     * The array of rhs types to try to match.
     */
    private Type[] _rhsTypes;

    /**
     * The array of rhs names if there is a match.
     */
    private String[] _rhsNames;

    /**
     * A utility constructor which simply takes strings
     * with single words separated by whitespace, separates
     * these strings into arrays, and calls the standard
     * array constructor.
     */	
    public SingleRule (String lhsType,
            String rhsName, String rhsType) {
        _lhsType = Type.makeType(lhsType);
        _rhsTypes = new Type[1];
        _rhsTypes[0] = Type.makeType(rhsType);
        _rhsNames = new String[1];
        _rhsNames[0] = rhsName;
		
    }
	
    /**
	 * Return the LHS type of the rule.
	 */
    public Type getLHSType() {
        return _lhsType;
    }
	
    /**
	 * Return the RHS types of the rule.
	 */
    public Type[] getRHSTypes() {
        return _rhsTypes;
    }
	
    /**
	 * Return the RHS names of the rule.
	 */
    public String[] getRHSNames() {
        return _rhsNames;
    }	
	
    /**
	 * Match the given scene elements and return a resulting
	 * element, or return null if there is no match.
	 */	
    public CompositeElement match(CompositeElement[] rhs, Scene db) {
        //FIXME
        return null;	
    }
}

