/*
 * $Id: Grammar2D.java,v 1.4 2001/07/22 22:01:50 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.parser2d;
import diva.sketch.recognition.Scene;
import diva.sketch.recognition.SceneElement;
import diva.sketch.recognition.Type;
import java.util.HashMap;

/**
 * A grammar is a collection of rules that implement productions and a
 * collection of tokens that are expected from the low-level
 * recognizer in order for these rules to make sense.
 * 
 * @author  Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.4 $
 * @rating Red
 */
public class Grammar2D {
    /**
     * The rules that define the grammar.
     */
    private Rule[] _rules;

    /**
     * The token types that the grammar expects.
     */
    private Type[] _tokenTypes;

    /**
     * Construct a grammar using the given set of rules and token
     * types.  The rules define the body of the grammar, and the token
     * types define its inputs from the low-level recognizer.  The
     * types used in the right-hand side (RHS) of each rule are
     * expected to be defined in the LHS of other rules, or in the
     * token types.  This invariant is checked by the constructor and
     * an illegal argument exception is thrown if this fails.
     */
    public Grammar2D(Rule[] rules, Type[] tokenTypes) {
        _rules = rules;
        _tokenTypes = tokenTypes;
        HashMap map = new HashMap();
        for(int i = 0; i < rules.length; i++) {
            map.put(rules[i].getLHSType(), rules[i]);
        }
        for(int i = 0; i < tokenTypes.length; i++) {
            map.put(tokenTypes[i], tokenTypes[i]);
        }
        for(int i = 0; i < rules.length; i++) {
            Type[] rhsTypes = rules[i].getRHSTypes();
            for(int j = 0; j < rhsTypes.length; j++) {
                if(map.get(rhsTypes[j]) == null) {
                    String err = "Type not defined in rules or tokens: " + rhsTypes[j];
                    throw new IllegalArgumentException(err);
                }
            }
        }
    }
    
    /**
     * Return the set of rules that comprises this grammar.  Rules
     * are in no particular order.
     */
    public Rule[] getRules() {
        return _rules;
    }
	
    /**
     * Return the set of token types that the grammar expects from the
     * low-level recognizer in order to function properly.
     */
    public Type[] getTokenTypes() {
        return _tokenTypes;
    }
}



