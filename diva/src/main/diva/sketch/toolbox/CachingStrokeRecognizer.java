/*
 * $Id: CachingStrokeRecognizer.java,v 1.5 2001/08/28 06:37:12 hwawen Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.toolbox;
import diva.sketch.recognition.RecognitionSet;
import diva.sketch.recognition.StrokeRecognizer;
import diva.sketch.recognition.TimedStroke;
/**
 * A proxy recognizer implementation that caches the result
 * of the target recognizer.  If the same stroke is passed in
 * with the same number of vertices, it returns the cached
 * recognition result rather than calling the target recognizer.
 *
 * This is useful, for example, if multiple "high-level"
 * recognizers (HLRs) want to share a basic recognizer to recognize
 * shapes. Suppose the user draws a square.  Without a cache:
 * <ol>
 * <li>The first HLR calls the basic recognizer, which
 *     performs recognition and returns a "square" recognition.
 * <li>The second HLR calls the basic recognizer, which
 *     performs recognition and returns a "square" recognition.
 * <li>And so on...
 * </ol>
 *
 * <P>
 * The uncached version results in a lot of wasted computation
 * on the part of the basic recognizer.  With a cache, the
 * sequence looks like this:
 *
 * <ol>
 * <li>The first HLR calls the cached recognizer, which
 *     calls its "target" basic recognizer, which returns a
 *     "square" recognition.
 * <li>The second HLR calls the cached recognizer, which
 *     sees that the stroke has not changed since the last
 *     time it was invoked, and returns the cached recognition.
 * <li>And so on...
 * </ol>
 * 
 * In this cached version, the basic recognition is only performed
 * once.
 *
 * <p> Since we expect most users will want to use a single basic
 * recognizer, this should save a lot of unnecessary computation.
 * Having this caching behavior in its own class makes it
 * self-contained and possibly reusable.
 *
 * @author  Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.5 $
 * @rating Red
 */
public class CachingStrokeRecognizer implements StrokeRecognizer {
    /**
     * The target recognizer that actually performs
     * the recognition.
     */
    private StrokeRecognizer _target;

    /**
     * Keep around the method that was called.
     * FIXME - do we need this?
     *
     private static int NO_METHOD = -1;
     private static int STROKE_STARTED_METHOD = 3;
     private static int STROKE_MODIFIED_METHOD = 4;
     private static int STROKE_COMPLETED_METHOD = 4;
     *
     * The previous method that was invoked (so
     * that strokeCompleted() will get called
     *
     private int _prevMethod;
    */

    /**
     * The last stroke that was recognized.
     */
    private TimedStroke _prevStroke;
    
    /**
     * The length (number of vertices) of the last stroke that was
     * recognized.
     */
    private int _prevStrokeLength;

    /**
     * The previous recognition associated with the other cached
     * state.
     */
    private RecognitionSet _prevRecognition;
    

    /**
     * Cache the recognitions of the given target recognizer.
     */
    public CachingStrokeRecognizer(StrokeRecognizer target) {
        _target = target;
        clear();
    }

    /**
     * Manually clear the cache.
     */
    public void clear() {
        _prevStroke = null;
        _prevStrokeLength = -1;
        _prevRecognition = null;
        //  _prevMethod = NO_METHOD;
    }

    /**
     * Return whether or not the given stroke is changed relative to
     * the previous cached version.  This currently returns whether
     * the two strokes reference the same object and whether or not
     * the given stroke has the same number of vertices as the
     * previous version.
     * 
     * <p> This method can be overridden to make the caching more
     * accurate or more sophisticated.  For example it could be
     * configured to perform recognition only after "N" vertices have
     * been added.
     */
    public boolean hasStrokeChanged(TimedStroke s) {
        if(s != _prevStroke) {
            return true;
        }
        else if(s != null) {
            return !(s.getVertexCount() == _prevStrokeLength);
        }
        else { //null strokes
            return false;
        }
    }

    /**
     * Perform recognition in the target if the given stroke hasn't
     * changed since the last time this method was called.
     */
    public RecognitionSet strokeCompleted (TimedStroke s) {
        if(hasStrokeChanged(s)) {
            RecognitionSet r = _target.strokeCompleted(s);
            _prevStroke = s;
            _prevStrokeLength = s.getVertexCount();
            _prevRecognition = r;
        }
        return _prevRecognition;
    }

    /**
     * Perform recognition in the target if the given stroke hasn't
     * changed since the last time this method was called.
     */
    public RecognitionSet strokeModified (TimedStroke s) {
        if(hasStrokeChanged(s)) {
            RecognitionSet r = _target.strokeModified(s);
            _prevStroke = s;
            _prevStrokeLength = s.getVertexCount();
            _prevRecognition = r;
        }
        return _prevRecognition;
    }

    /**
     * Perform recognition in the target if the given stroke hasn't
     * changed since the last time this method was called.
     */
    public RecognitionSet strokeStarted (TimedStroke s) {
        if(hasStrokeChanged(s)) {
            RecognitionSet r = _target.strokeStarted(s);
            _prevStroke = s;
            _prevStrokeLength = s.getVertexCount();
            _prevRecognition = r;
        }
        return _prevRecognition;
    }
}




