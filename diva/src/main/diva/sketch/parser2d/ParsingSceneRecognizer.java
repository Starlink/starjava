/*
 * $Id: ParsingSceneRecognizer.java,v 1.13 2001/07/22 22:01:50 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.parser2d;
import diva.sketch.recognition.Scene;
import diva.sketch.recognition.SceneDelta;
import diva.sketch.recognition.SceneDeltaSet;
import diva.sketch.recognition.SceneElement;
import diva.sketch.recognition.SceneRecognizer;
import diva.sketch.recognition.StrokeElement;
import diva.sketch.recognition.CompositeElement;
import diva.sketch.recognition.Type;
import diva.util.Filter;
import diva.util.FilteredIterator;
import diva.util.ArrayIterator;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.BitSet;

/**
 * A grammar-driven scene recognizer that incrementally parses the
 * scene as new tokens are added.  Unlike standard parsers, this
 * parser understands the ambiguity in drawings that are imperfectly
 * recognized and simultaneously considers all possible
 * interpretations of the scene.  This makes the parser less efficient
 * than a typical string parser, but the parser utilizes
 * incrementality and smart data structures help with the performance.
 *
 * <p> The grammar is expressed as an array of rules, and the rules
 * are indexed inside the parser according to the types of tokens that
 * they consume.  This means that every time a token is added to the
 * scene, the appropriate rules are retrieved from the index and the
 * rest of the required tokens are searched for in the database.  All
 * permutations of possible matches are fed to a rule, and if the rule
 * matches, the resulting token is fed back into the process.
 *
 * @see Grammar2D
 * @author  Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.13 $
 * @rating Red
 */
public class ParsingSceneRecognizer implements SceneRecognizer {
    /**
     * Perform some lower level recognition.
     */
    private SceneRecognizer _child;
	
    /**
     * Store user-defined grammar rules.
     */
    private Grammar2D _grammar;

    /**
     * Store a mapping from a type to the rules
     * that "consume" this type on their RHS.
     */
    private HashMap _map;
	
    /**
     * Construct a new parser with the specified grammar rules, that
     * uses the given child recognizer to perform lower-level
     * recognition.  The child recognizer can perform any type of
     * recognition, ranging from single-stroke recognition to
     * handwriting recognition to sub-languages defined in another
     * parsing recognizer.
     */
    public ParsingSceneRecognizer(SceneRecognizer child, Grammar2D grammar) {
        _child = child;
        _grammar = grammar;
        _map = new HashMap();
        Rule[] rules = _grammar.getRules();
        for(int i = 0; i < rules.length; i++) {
            Rule rule = rules[i];
            Type[] rhsTypes = rule.getRHSTypes(); 
            for(int j = 0; j < rhsTypes.length; j++) {
                Type type = rhsTypes[j]; 
                ArrayList l = (ArrayList)_map.get(type);
                if(l == null) {
                    l = new ArrayList();
                    _map.put(type, l);
                }
                if(!l.contains(rule)) {
                    l.add(rule);
                }
            }
        }
    }

    /**
     * Return the grammar that is used to perform the parsing.
     * The grammar consists of a set of rules and token types.
     */
    public Grammar2D getGrammar() {
        return _grammar;
    }

    /**
     * Pass the "stroke started" information to the child recognizer
     * and handle the results if there are changes in the scene.
     */
    public SceneDeltaSet strokeStarted(StrokeElement stroke, Scene db) {
        SceneDeltaSet deltas = _child.strokeStarted(stroke, db);
        return handleDeltas(deltas, db);
    }
	
    /**
     * Pass the "stroke modified" information to the child recognizer
     * and handle the results if there are changes in the scene.
     */
    public SceneDeltaSet strokeModified(StrokeElement stroke, Scene db) {
        SceneDeltaSet deltas = _child.strokeModified(stroke, db);
        return handleDeltas(deltas, db);
    }
	
    /**
     * Pass the "stroke completed" information to the child recognizer
     * and handle the results if there are changes in the scene.
     */
    public SceneDeltaSet strokeCompleted(StrokeElement stroke, Scene db) {
        SceneDeltaSet deltas = _child.strokeCompleted(stroke, db);
        return handleDeltas(deltas, db);
    }
	
    /**
     * Pass the "session started" information to the child recognizer
     * and handle the results if there are changes in the scene.
     */
    public SceneDeltaSet sessionCompleted(StrokeElement[] session, Scene db) {
        SceneDeltaSet deltas = _child.sessionCompleted(session, db);
        return handleDeltas(deltas, db);
    }
	
    /**
     * Handle any deltas that may have occurred in the scene from the
     * addition, modification, or completion of strokes/sessions to
     * the scene database.  The changes cause rules to fire, which
     * are used to recursively compute more rules to fire, etc.
     */
    protected SceneDeltaSet handleDeltas(SceneDeltaSet deltas, Scene db) {
        if(deltas == SceneDeltaSet.NO_DELTA) {
            return SceneDeltaSet.NO_DELTA;
        }
        SceneDeltaSet out = new SceneDeltaSet();
        for(Iterator i = deltas.deltas(); i.hasNext(); ) {
            SceneDelta d = (SceneDelta)i.next();
            CompositeElement e = d.getRoot();
            out.addAll(parse(e, db));
        }
        out.addAll(deltas);
        return out;
    }
	
    /**
     * Perform a recursive, incremental parse of the element
     * in the given scene, adding the results to the given
     * delta set.
     */
    public SceneDeltaSet parse(CompositeElement e, Scene db) {
        ArrayList potentialRules = (ArrayList)_map.get(e.getData().getType());
        System.out.println("parsing: " + e);
        if(e.getData().getType().getID().equals("hrect")) {
            System.out.println("hrect: " + ((potentialRules == null) ? 0 : potentialRules.size()) + " rules");
        }
        if(e.getData().getType().getID().equals("blankWindow")) {
            System.out.println("blankWindow: " + ((potentialRules == null) ? 0 : potentialRules.size()) + " rules");
        }
        if(potentialRules == null) {
            return SceneDeltaSet.NO_DELTA;
        }
        SceneDeltaSet out = new SceneDeltaSet();
        for(Iterator rs = potentialRules.iterator(); rs.hasNext(); ) {
            Rule rule = (Rule)rs.next();
            Type[] rhs = rule.getRHSTypes();
            if(rule.getLHSType().getID().equals("titleBarWindow")) {
                System.out.println("titleBarWindow");
            }
            for(int i = 0; i < rhs.length; i++) {
                boolean full = true;
                if(rhs[i].equals(e.getData().getType())) {

                    //If the given element equals the i'th element of
                    //the rule, gather all of the other elements out
                    //of the database into an array of lists, and then
                    //try to all possible permutations of that list.
                    //es is an array of Lists.  Each List contains
                    //elements of the same type from the scene
                    //database.  And the types are the types of the
                    //right hand side.  These elements must be
                    //consistent with 'e', meaning they don't share
                    //the same stroke elements.
                    List[] es = new List[rhs.length];
                    for(int j = 0; j < i; j++) {
                        es[j] = db.elementsOfType(rhs[j], e);
                        if(es[j] == null || es[j].size() == 0) {
                            full = false;
                            break;
                        }
                    }
                    if(full) {
                        es[i] = new ArrayList(1);
                        es[i].add(e);
                        for(int j = i+1; j < es.length; j++) {
                            es[j] = db.elementsOfType(rhs[j], e);
                            if(es[j] == null || es[j].size() == 0) {
                                full = false;
                                break;
                            }
                        }
                    }

                    // If there are es, generate all of the
                    // permutations and try to them.
                    if(full) {
                        CompositeElement[][] permuted = permute(es);
                        // debug(" PERMUTE OUT: " + print2D(permuted));
                        makeConsistent(permuted, db);
                        // debug("===================================");
                        for(int j = 0; j < permuted.length; j++) {
                            if(permuted[j] != null) {
                                CompositeElement newElt =
                                    rule.match(permuted[j], db);
                                if(newElt != null) {
                                    //  debug(" HIT: " + newElt);
                                    out.addAll(parse(newElt, db));
                                    // FIXME: if(newElt.parents().size() == 0) { ????
                                    out.addDelta(new
                                        SceneDelta.Subtractive(db,newElt));
                                }
                            }
                        }
                    }
                }
                if(!full) {
                    break;
                }
            }
        }
        return out;
    }
	
    /**
     * Make the given set of permutations consistent by checking
     * consistency between each element and setting the i'th array
     * to null if the i'th permutation is not consistent.  Note that
     * this modifies the input array.
     */
    private void makeConsistent(SceneElement[][] in, Scene db) {	
        for(int i = 0; i < in.length; i++) {
            SceneElement[] match = in[i];
            for(int j = 0; j < match.length; j++) {
                SceneElement ref = match[j];
                for(int k = j+1; k < match.length; k++) {
                    if(!db.isConsistent(match[k], ref)) {
                        in[i] = null;
                        break;
                    }
                }
                if(in[i] == null) {
                    break;
                }
            }
        }
    }
	
    /**
     * Return a permutation set of the given elements.
     * 
     * For example:
     * <pre>
     * permute([[a b c] [d e]]) => [[a d] [a e] [b d] [b e]
     *                              [c d] [c e]]
     * </pre>
     */
    private static CompositeElement[][] permute(List[] in) {
        //debug("PERMUTE================================");
        //debug("  in = " + print2D(in));
		
        int numSlots = in.length;
        int numPermutes = 1;
        for(int i = 0; i < numSlots; i++) {
            numPermutes *= in[i].size();
        }
        //debug("  numPermutes = " + numPermutes);
        
        int[] stride = new int[numSlots];
        stride[0] = numPermutes/in[0].size();
        for(int i = 1; i < numSlots; i++) {
            stride[i] = stride[i-1]/in[i].size();
        }
        //        debug("  numContig = " + numContig);
        
        CompositeElement[][] out = new CompositeElement[numPermutes][numSlots];
        for(int i = 0; i < numPermutes; i++) {
            for(int j = 0; j < numSlots; j++) {
                out[i][j] = (CompositeElement)(in[j].get((i / stride[j])
                        % in[j].size()));
            }
        }
		
		
        //debug("  out = " + print2D(out));
		
        //debug("END===================================");
        return out;
    }
    
    /**
     * Print an array of array-lists; utility function.
     */
    private static String print2D(List[] in) {
        String out = "[ ";
        for(int i = 0; i < in.length; i++) {
            out = out + "[ ";
            for(Iterator j = in[i].iterator(); j.hasNext(); ) {
                out = out + j.next() + " ";
            }
            out = out + "] ";
        }
        out = out + "]";
        return out;
    }	
	
    /**
     * Print a 2D array; utility function.
     */
    private static String print2D(Object[][] in) {
        String out = "[ ";
        for(int i = 0; i < in.length; i++) {
            if(in[i] == null) {
                out = out + "NULL ";
            }
            else {
                out = out + "[ ";
                for(int j = 0; j < in[i].length; j++) {
                    out = out + in[i][j] + " ";
                }
                out = out + "] ";
            }
        }
        out = out + "]";
        return out;
    }
	
    /**
     * Debugging output.
     */
    public static void debug (String s) {
        System.out.println(s);
    }	
}



