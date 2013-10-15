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
     * @param   dlo        minimum axis data value
     * @param   dhi        maximum axis data value
     * @param   withMinor  if true minor axes are included,
     *                     if false only major (labelled) ones are
     * @param   captioner  caption painter
     * @param   orient     label orientation
     * @param   npix       number of pixels along the axis
     * @param   crowding   1 for normal tick density on the axis,
     *                     lower for fewer labels, higher for more
     * @return  tick array
     */
    Tick[] getTicks( double dlo, double dhi, boolean withMinor,
                     Captioner captioner, Orientation orient, int npix,
                     double crowding );
}
