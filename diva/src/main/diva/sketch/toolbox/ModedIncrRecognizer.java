/*
 * $Id: ModedIncrRecognizer.java,v 1.3 2001/08/28 06:37:12 hwawen Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.toolbox;
import diva.sketch.recognition.StrokeRecognizer;
import diva.sketch.recognition.RecognitionSet;
import diva.sketch.recognition.TimedStroke;
/**
 * An abstract class for "moded" incremental recognizers that examines
 * the first part of a gesture to see if it matches its starting
 * signal and thereafter responds to events in a moded interaction.
 * For example, a "zoom" recognizer will first look for the zoom
 * signal and output nothing until it sees this signal.  If it
 * recognizes the zoom signal it will switch into a zooming mode where
 * every event effects the zoom output.
 *
 * @author  Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.3 $
 * @rating Red
 */
public abstract class ModedIncrRecognizer implements StrokeRecognizer {
    /**
     * A mode which is modified as the stroke progresses.
     */
    private int _mode = IDLE;

    /**
     * The recognizer is IDLE, and should ignore all events except for
     * STROKE_STARTED.
     */
    protected static int IDLE = 0;

    /**
     * The recognizer has started processing events but still doesn't
     * know whether or not the the gesture matches the action signal.
     * If the gesture matches the action signal, set the mode to
     * ACTION.  If the gesture is recognized as NOT zooming, set the
     * mode to IDLE.
     */
    protected static int UNKNOWN = 1;

    /**
     * The recognizer has recognized the gesture as the signal and is
     * processing incoming events as an indicators to perform the specified
     * action.
     */
    protected static int ACTION = 2;

    /**
     * Invoked when a stroke starts.  Assert that the mode is IDLE,
     * because if not something is wrong.  Set the mode of the
     * recognizer to UNKNOWN, which means that the gesture has
     * started, but the nature of the gesture is not yet known.
     */
    public RecognitionSet strokeStarted(TimedStroke s) {
        if(_mode != IDLE) {
            String err = "Non-idle zooming recognizer should be idle!";
            throw new IllegalStateException(err);
        }
        _mode = UNKNOWN;
        return RecognitionSet.NO_RECOGNITION;
    }

    /**
     * Process an "action" event, performing the appropriate action
     * behavior (e.g. zooming).  This method can also return an
     * integer "mode" as an escape hatch in case for some reason it
     * completes an action and wants to revert to the IDLE state.
     * Most implementations should probably return ACTION.
     */
    protected abstract RecognitionSet processActionStroke(TimedStroke s);

    /**
     * Recognize the action signal (it is still unknown) and return an
     * indication (to the template algorithm) of what mode the
     * algorithm should revert to.  In other words, return ACTION if
     * the gesture is recognized as an action signal, IDLE if the
     * gesture is recognized as NOT being the action signal, and
     * UNKNOWN if the form nature of the gesture is still
     * undetermined.
     */
    protected abstract int recognizeActionSignal(TimedStroke s);

    /**
     * Invoked when a stroke is completed. This occurs when the mouse
     * up event has been detected.  If the mode is not IDLE, set
     * the mode to IDLE, because we don't want to process multi-stroke
     * gestures.
     */
    public RecognitionSet strokeCompleted(TimedStroke s){
        if(_mode != IDLE) {
            _mode = IDLE;
        }
        return RecognitionSet.NO_RECOGNITION;
    }

    /**
     * Invoked when a stroke has been modified, for example, points
     * have been added to the stroke.  If the current mode is idle,
     * we ignore these events.  Otherwise this is where the bulk of
     */
    public RecognitionSet strokeModified(TimedStroke s){
        if(_mode == IDLE) {
            return RecognitionSet.NO_RECOGNITION;
        }
        else if(_mode == ACTION) {
            _mode = ACTION;
            return processActionStroke(s);
        }
        else {
            _mode = recognizeActionSignal(s);
        }
        return RecognitionSet.NO_RECOGNITION;
    }
}

