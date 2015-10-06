package uk.ac.starlink.ttools.plot2.layer;

import uk.ac.starlink.ttools.plot2.Drawing;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.paper.Paper;

/**
 * Convenience partial implementation of Drawing where no plan is used.
 * Concrete impleentations have to provide an implementation of the
 * plan-less {@link #paintData(Paper,DataStore)} method.
 *
 * @author   Mark Taylor
 * @since    15 Feb 2013
 */
public abstract class UnplannedDrawing implements Drawing {

    /**
     * Returns null.
     */
    public Object calculatePlan( Object[] knownPlans, DataStore dataStore ) {
        return null;
    }

    /**
     * Calls {@link #paintData(Paper,DataStore)}.
     */
    public void paintData( Object plan, Paper paper, DataStore dataStore ) {
        assert plan == null;
        paintData( paper, dataStore );
    }

    public ReportMap getReport( Object plan ) {
        return null;
    }

    /**
     * Performs the drawing.
     * Invoked by {@link #paintData(Object,Paper,DataStore)}.
     *
     * @param  paper   graphics destination
     * @param  dataStore  data-bearing object
     */
    protected abstract void paintData( Paper paper, DataStore dataStore );
}
