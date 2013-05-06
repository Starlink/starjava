package uk.ac.starlink.ttools.plot2.data;

import java.util.Iterator;

/**
 * Contains all the actual data required for a plot.
 * To extract usable data for a plot, a suitable {@link DataSpec} object
 * is also required.
 *
 * <p>Because instances of this class manage the data, by keeping track of
 * the <code>DataStore</code>s in an application you can control where
 * the memory is used.  All the other objects connected with a plot,
 * for instance the {@link uk.ac.starlink.ttools.plot2.PlotLayer}s
 * and {@link DataSpec}s, are cheap
 * to produce and have small memory footprints, so can be created and
 * destroyed as required.
 * As a rule therefore, DataStore objects should be managed by a single class,
 * and references not kept to them by any other potentially long-lived objects.
 *
 * <p>Obtain an instance of this class from a {@link DataStoreFactory}.
 * 
 * @author   Mark Taylor
 * @since    6 Feb 2013
 */
public interface DataStore {

    /**
     * Indicates whether this store has the data described by a given
     * DataSpec.
     *
     * @param   spec   plot data specification object
     */
    boolean hasData( DataSpec spec );

    /**
     * Returns the data described by a given DataSpec as a sequence of
     * tuples.
     * Must only be called if {@link #hasData} returns true for the
     * given DataSpec; if not, behaviour is undefined.
     *
     * @param   spec   plot data specification object
     * @return  sequence of values which can be used to perform a plot
     */
    TupleSequence getTupleSequence( DataSpec spec );
}
