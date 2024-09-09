package uk.ac.starlink.ttools.plot2;

/**
 * Provides tick marks to label plot axes.
 *
 * @author   Mark Taylor
 * @since    17 Oct 2013
 */
public interface Ticker {

    /**
     * Generates tick marks for labelling a plot axis.
     *
     * <p>The supplied list of orientations is attempted in order;
     * if the required crowding can be satisfied by any of them,
     * that orientation will be used.  If it can't be supplied by any
     * (because of unavoidable label overlap) a lower crowding value
     * may be used.
     *
     * @param   dlo        minimum axis data value
     * @param   dhi        maximum axis data value
     * @param   withMinor  if true minor axes are included,
     *                     if false only major (labelled) ones are
     * @param   captioner  caption painter
     * @param   orients    array of label orientations in order of preference,
     *                     must contain at least one element
     * @param   npix       number of pixels along the axis
     * @param   crowding   1 for normal tick density on the axis,
     *                     lower for fewer labels, higher for more
     * @return  tick array along with orientation actually used
     */
    TickRun getTicks( double dlo, double dhi, boolean withMinor,
                      Captioner captioner, Orientation[] orients, int npix,
                      double crowding );
}
