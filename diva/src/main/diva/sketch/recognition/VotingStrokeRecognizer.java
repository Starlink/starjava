/*
 * $Id: VotingStrokeRecognizer.java,v 1.7 2001/08/28 06:34:12 hwawen Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.recognition;
import java.util.Iterator;
import java.util.List;

/**
 * Voting gesture recognizer is a composite recognizer which allows
 * multiple sub-recognizers to vote and interact with one another to
 * classify a given gesture. <p>
 *
 * For each gesture, the voting recognizer can produce one or more
 * classifications.  The algorithm works as follows:
 *
 * <ol>
 *
 * <li>There is an incoming stroke event.
 *
 * <li>The recognizer directs this event to all of its sub-
 *     recognizers, which may or may not produce recognitions.
 *
 * <li>The recognitions that are produced are buffered internally, and
 *     when all recognizers finish, the recognitions are combined
 *     using a voting policy described below.  The resulting
 *     recognition set is returned.
 *
 * </ol>
 *
 * This class also contains a number of heuristics for preserving 
 * only the N-highest recognitions, filtering out recognitions with
 * low-confidence values, etc.
 *
 * @author  Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.7 $
 * @rating Red
 */
public class VotingStrokeRecognizer implements StrokeRecognizer {
    /**
     * The sub-recognizers.
     */
    private StrokeRecognizer[] _children = null;

    /**
     * A buffer to store the results as they
     * are generated.
     */
    private RecognitionSet[] _buffer = null;

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
     * children recognizers.
     */
    public VotingStrokeRecognizer (StrokeRecognizer[] children) {
        _children = children;
        _buffer = new RecognitionSet[_children.length];
        clearBuffer();
    }

    /** Return the children as a list.
     */
    public List children() {
        return java.util.Arrays.asList(_children);
    }
 
    /**
     * Clear the buffer after every stroke finishes by
     * setting every entry in the buffer to NO_RECOGNITION.
     */
    public void clearBuffer () {
        for(int i = 0; i < _buffer.length; i++) {
            _buffer[i] = RecognitionSet.NO_RECOGNITION;
        }
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
    };

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
    public RecognitionSet strokeCompleted (TimedStroke s) {
        for(int i = 0; i < _children.length; i++) {
            RecognitionSet rs = _children[i].strokeCompleted(s);
            _buffer[i] = rs;
        }
        RecognitionSet out = vote();
        clearBuffer();
        return out;
    }

    /**
     * Pass the event to the child recognizers, tally the vote,
     * and return the consensus.
     */
    public RecognitionSet strokeModified (TimedStroke s) {
        for(int i = 0; i < _children.length; i++) {
            RecognitionSet rs = _children[i].strokeModified(s);
            _buffer[i] = rs;
        }
        return vote();
    }

    /**
     * Pass the event to the child recognizers, tally the vote,
     * and return the consensus.
     */
    public RecognitionSet strokeStarted (TimedStroke s) {
        for(int i = 0; i < _children.length; i++) {
            RecognitionSet rs = _children[i].strokeStarted(s);
            _buffer[i] = rs;
        }
        return vote();
    }
    
    /**
     * Tally all of the votes of the sub-recognizers and emit them all
     * into a recognition.  Subclasses can override this method to
     * change the voting behavior/strategy.  Return <i>NO_RECOGNITION</i>
     * if the vote comes up empty.  This method implements the N-highest
     * and min-confidence heuristics, described in the getNHighest() and
     * getMinConfidence() method descriptions.
     *
     * @see #getNHighest()
     * @see #getMinConfidence()
     */
    protected RecognitionSet vote () {
        RecognitionSet out = RecognitionSet.NO_RECOGNITION;

        for(int i = 0; i < _buffer.length; i++) {
            RecognitionSet in = _buffer[i];

            for(Iterator j = in.recognitions(); j.hasNext(); ) {
                Recognition rin = (Recognition)j.next();
                double inConfidence = rin.getConfidence();

                Recognition rout = out.getRecognitionOfType(rin.getType());
                double outConfidence = (rout == null) ? 0 : rout.getConfidence();

                if((outConfidence > 0) && (inConfidence > outConfidence)) {
                    out.removeRecognition(rout);
                }
                if(inConfidence >= _minConfidence) { //min-confidence heuristic
                    if(out == RecognitionSet.NO_RECOGNITION) {
                        out = new RecognitionSet(); // lazy construction
                    }
                    out.addRecognition(rin);
                }
                else {
                    debug("VotingStrokeRecognizer: type "
                            + rin.getType()
                            + " filtered by min-confidence (conf ="
                            + inConfidence + ")");
                }
            }
        }

        //n-highest heuristic
        if(out.getRecognitionCount() > _nHighest) {
            Recognition[] toRemove =
                new Recognition[out.getRecognitionCount()-_nHighest];
            int cnt = 0;
            for(Iterator rs = out.recognitions(); rs.hasNext(); ) {
                Recognition r = (Recognition)rs.next();
                if(cnt >= _nHighest) {
                    toRemove[cnt-_nHighest] = r;
                }
                cnt++;
            }
            
            for(int i = 0; i < toRemove.length; i++) {
                debug("VotingStrokeRecognizer: type "
                        + toRemove[i] + " filtered by N-highest");
                out.removeRecognition(toRemove[i]);
            }
        }
        
        return out;
    }
}


