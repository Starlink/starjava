/*
 * $Id: VotingSceneRecognizer.java,v 1.6 2001/07/22 22:01:55 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.recognition;
import java.util.Iterator;
import java.util.List;

/**
 * A composite recognizer which allows multiple sub-recognizers 
 * to vote on interpretations of a given scene.  This recognizer
 * basically utilizes the same heuristics as VotingStrokeRecognizer,
 * but applies to scene elements instead of strokes for high-level
 * recognition.
 *
 * @see VotingStrokeRecognizer
 * @author  Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.6 $
 * @rating Red
 */
public class VotingSceneRecognizer implements SceneRecognizer {
    /**
     * The sub-recognizers.
     */
    private SceneRecognizer[] _children = null;

    /**
     * A buffer to store the results as they
     * are generated.
     */
    private SceneDeltaSet[] _buffer = null;

    /**
     * The minimum confidence value that the recognizer
     * needs to consider a vote (min-confidence heuristic).
     */
    private double _minConfidence = 0;
    
    /**
     * The minimum confidence value that the recognizer
     * needs to consider a vote (n-highest heuristic).
     */
    private int _nHighest = ALL_VOTES;

    /**
     * A constant which says that votes are not filtered 
     * by the "n-highest" rule.
     *
     * @see #setNHighest(int)
     */
    public static int ALL_VOTES = Integer.MAX_VALUE;

    /**
     * Construct a voting recognizer with the following
     * child recognizers.
     */
    public VotingSceneRecognizer (SceneRecognizer[] children) {
        _children = children;
        _buffer = new SceneDeltaSet[_children.length];
        clearBuffer();
    }
 
    /**
     * Clear the buffer after every stroke finishes by
     * setting every entry in the buffer to NO_DELTA.
     */
    public void clearBuffer () {
        for(int i = 0; i < _buffer.length; i++) {
            _buffer[i] = SceneDeltaSet.NO_DELTA;
        }
    }

    /** Return the children as a list.
     */
    public List children() {
        return java.util.Arrays.asList(_children);
    }

    /**
     * Debugging output.
     */
    private void debug (String s) {
        System.err.println(s);
    }

    /**
     * Return the minimum confidence value which is necessary 
     * for a type to get considered in the vote.
     */
    public double getMinConfidence () {
        return _minConfidence;
    }

    /**
     * Return the "n-highest" value, which says that the n-highest
     * classifications will get passed on when the child recognizers
     * vote.
     */
    public int getNHighest () {
        return _nHighest;
    }

    /**
     * Set the minimum confidence classifications that will get passed
     * on when the child recognizers vote.  The default value is 0,
     * meaning that classifications are not filtered.
     */
    public void setMinConfidence (double val) {
        _minConfidence = val;
    }
 
    /**
     * Set the "n-highest" value, which says that the n-highest
     * classifications will get passed on when the child recognizers
     * vote.  The default value is ALL_VOTES, which means that all
     * votes are passed on.  The value N must be greater than or
     * equal to zero.
     */
    public void setNHighest (int n) {
        if(n < 0) {
            String err = "Invalid N-Higest: " + n;
            throw new IllegalArgumentException(err);
        }
        _nHighest = n;
    }

    /**
     * Pass the event to the child recognizers, tally the vote,
     * clear the buffer, and return the consensus.
     */
    public SceneDeltaSet sessionCompleted (StrokeElement[] session, Scene db) {
        for(int i = 0; i < _children.length; i++) {
            SceneDeltaSet rs = _children[i].sessionCompleted(session, db);
            _buffer[i] = rs;
        }
        SceneDeltaSet out = vote();
        clearBuffer();
        return out;
    }	
	
    /**
     * Pass the event to the child recognizers, tally the vote,
     * clear the buffer, and return the consensus.
     */
    public SceneDeltaSet strokeCompleted (StrokeElement s, Scene db) {
        for(int i = 0; i < _children.length; i++) {
            SceneDeltaSet rs = _children[i].strokeCompleted(s, db);
            _buffer[i] = rs;
        }
        SceneDeltaSet out = vote();
        clearBuffer();
        return out;
    }

    /**
     * Pass the event to the child recognizers, tally the vote,
     * and return the consensus.
     */
    public SceneDeltaSet strokeModified (StrokeElement s, Scene db) {
        for(int i = 0; i < _children.length; i++) {
            SceneDeltaSet rs = _children[i].strokeModified(s, db);
            _buffer[i] = rs;
        }
        return vote();
    }

    /**
     * Pass the event to the child recognizers, tally the vote,
     * and return the consensus.
     */
    public SceneDeltaSet strokeStarted (StrokeElement s, Scene db) {
        for(int i = 0; i < _children.length; i++) {
            SceneDeltaSet rs = _children[i].strokeStarted(s, db);
            _buffer[i] = rs;
        }
        return vote();
    }
    
    /**
     * Tally all of the votes of the sub-recognizers and emit them all
     * into a recognition.  Subclasses can override this method to
     * change the voting behavior/strategy.  Return <i>NO_DELTA</i>
     * if the vote comes up empty.  This method implements the N-highest
     * and min-confidence heuristics, described in the getNHighest() and
     * getMinConfidence() method descriptions.
     *
     * @see #getNHighest()
     * @see #getMinConfidence()
     */
    protected SceneDeltaSet vote () {
        SceneDeltaSet out = SceneDeltaSet.NO_DELTA;

        for(int i = 0; i < _buffer.length; i++) {
            SceneDeltaSet in = _buffer[i];

            for(Iterator j = in.deltas(); j.hasNext(); ) {
                SceneDelta din = (SceneDelta)j.next();
                double inConfidence = din.getConfidence();
                
                // FIXME - we only need to go through n-highest.
		// FIXME - we need to commit first. 
                // FIXME - do we want to cast simulataneous
                //       votes for the same result? 

	/*
         SceneDelta dout = out.getRecognitionOfType(rin.getType());
         double outConfidence = (rout == null) ? 0 : rout.getConfidence();
         if((outConfidence > 0) && (inConfidence > outConfidence)) {
            out.removeRecognition(rout);
         }
	*/
                //min-confidence heuristic
                if(inConfidence >= _minConfidence) {
                    if(out == SceneDeltaSet.NO_DELTA) {
                        // lazy construction
                        out = new SceneDeltaSet(); 
                    }
                    out.addDelta(din);
                }
                else {
                    debug("VotingSceneRecognizer: type "
                            + din.getRoot().getData().getType()
                            + " filtered by min-confidence (conf ="
                            + inConfidence + ")");
                }
            }
        }

        //n-highest heuristic
        if(out.getDeltaCount() > _nHighest) {
            SceneDelta[] toRemove = new SceneDelta[out.getDeltaCount()-
                    _nHighest];
            int cnt = 0;
            for(Iterator rs = out.deltas(); rs.hasNext(); ) {
                SceneDelta r = (SceneDelta)rs.next();
                if(cnt >= _nHighest) {
                    toRemove[cnt-_nHighest] = r;
                }
                cnt++;
            }
            
            for(int i = 0; i < toRemove.length; i++) {
                debug("VotingSceneRecognizer: type "
                        + toRemove[i] + " filtered by N-highest");
                out.removeDelta(toRemove[i]);
            }
        }
        
        return out;
    }
}


