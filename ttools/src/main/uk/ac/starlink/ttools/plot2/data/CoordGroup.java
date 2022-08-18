package uk.ac.starlink.ttools.plot2.data;

import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.util.IntList;

/**
 * Expresses the content of a set of coordinates used for a plot layer,
 * and how to find the values of these coordinates from a corresponding
 * DataSpec.  A given CoordGroup instance is tied to a particular
 * arrangement of corresponding DataSpec objects.
 *
 * <p>This abstraction is defined in a somewhat ad hoc way at present;
 * features have been introduced according to what is required from
 * existing plotters.  It may be changed or rationalised in the future.
 * That is one reason this functionality is split out into its own class
 * rather than being part of the Plotter interface itself, and also why
 * implementation of this class is controlled (instances only available
 * from factory methods of this class).
 *
 * @author   Mark Taylor
 * @since    20 Jan 2014
 */
public abstract class CoordGroup {

    /**
     * Private constructor prevents subclassing.
     * This guarantees that the only instances are those dispensed by
     * factory methods of this class, making it safer to redefine the
     * contract in future if required.
     */
    private CoordGroup() {
    }

    /**
     * Returns the number of basic data positions per tuple.
     * For instance
     * a scatter plot would use 1,
     * a plot linking pairs of positions in the same table would use 2,
     * and an analytic function would use 0.
     * Each of these can be represented by standard positional coordinates
     * as appropriate for the geometry, and is turned into a
     * data space position by use of the DataGeom presented
     * at layer creation time.
     * A position corresponds to a (fixed) number of coordinate values.
     *
     * @return   number of sets of basic positional coordinates
     */
    public abstract int getBasicPositionCount();

    /*
     * Returns any coordinates used by this plotter additional to the
     * basic positional ones.
     *
     * @return  coordinates other than basic positional ones
     */
    public abstract Coord[] getExtraCoords();

    /**
     * Returns a count of the extra coordinates that can
     * be considered to represent data positions.
     * These can be turned into positions by the DataGeom presented at
     * layer creation time, but they are not suitable for representation
     * by standard positional coordinates.
     *
     * <p>These positional coordinates are assumed to come at the
     * start of the list of extra coordinates.
     *
     * @return  number of sets of positional coordinates in the extras
     */
    public abstract int getExtraPositionCount();

    /**
     * Returns the starting coordinate index in a DataSpec at which
     * a given one of the basic positional coordinates represented
     * by this coord group will appear.
     *
     * @param  ipos  index of basic position supplied by this group
     *               (first position is zero)
     * @param  geom  data geom with which index will be used
     * @return  index of starting coordinate for given position in dataspec
     */
    public abstract int getPosCoordIndex( int ipos, DataGeom geom );

    /**
     * Returns the coordinate index in a DataSpec at which
     * a given one of the extra coordinates represented
     * by this coord group will appear.
     *
     * @param  iExtra  index of extra coordinate
     *                 (first extra coord is zero)
     * @param  geom  data geom with which index will be used
     * @return  index of given extra coordinate in dataspec
     */
    public abstract int getExtraCoordIndex( int iExtra, DataGeom geom );

    /**
     * Returns a list of the coordinate indices in a DataSpec of
     * those coordinates whose change should trigger a re-range of
     * the plot surface.
     *
     * @param  geom  data geom with which indices will be used
     * @return   array of indices into DataSpec coordinates
     */
    public abstract int[] getRangeCoordIndices( DataGeom geom );

    /**
     * Indicates whether this group deals with "partial" positions.
     * That is to say that the coordinates represent data positions,
     * but that those data position arrays have at least one element
     * equal to NaN, indicating for instance a line rather than a
     * point in the data space.
     *
     * @return  true iff this group represents a single partial position
     */
    public abstract boolean isSinglePartialPosition();

    /**
     * Returns a coord group which contains only a single data space position.
     *
     * @return  new coord group
     */
    public static CoordGroup createSinglePositionCoordGroup() {
        return new BasicCoordGroup( 1, new Coord[ 0 ], 0 );
    }

    /**
     * Returns a coord group which contains zero or more basic positions and
     * zero or more additional non-positional ("extra") coordinates.
     *
     * @param  nBasicPos  number of basic positional coordinates
     * @param  extras  non-positional coordinates
     * @return  new coord group
     */
    public static CoordGroup createCoordGroup( int nBasicPos, Coord[] extras ) {
        return new BasicCoordGroup( nBasicPos, extras, 0 );
    }

    /**
     * Returns a coord group with zero or more basic positions and
     * zero or more extra coordinates, some of which may be positional.
     *
     * @param  nBasicPos  number of basic positional coordinates
     * @param  extras   additional coordinates
     * @param  nExtraPos  number of extra coordinates which are positional
     */
    public static CoordGroup createCoordGroup( int nBasicPos, Coord[] extras,
                                               int nExtraPos ) {
        return new BasicCoordGroup( nBasicPos, extras, nExtraPos );
    }

    /**
     * Returns a coord group with no basic positional coordinates.
     *
     * @param  coords  all coordinates
     * @param  nExtraPos  number of the extra coordinates which can be
     *                    considered to represent data positions
     * @param  rangeCoordFlags  array of flags corresponding to the
     *         <code>coords</code> array, true for any coord whose change
     *         should cause a re-range
     */
    public static CoordGroup
            createNoBasicCoordGroup( Coord[] coords, int nExtraPos,
                                     boolean[] rangeCoordFlags ) {
        return new ExtraPosCoordGroup( coords, nExtraPos, rangeCoordFlags,
                                       false );
    }

    /**
     * Returns a coord group which contains a single partial position.
     *
     * @param   coords   all coordinates, starting with those constituting
     *                   the partial position
     * @param  rangeCoordFlags  array of flags corresponding to the
     *         <code>coords</code> array, true for any coord whose change
     *         should cause a re-range
     * @return  new coord group
     */
    public static CoordGroup createPartialCoordGroup(
                                 Coord[] coords, boolean[] rangeCoordFlags ) {
        return new ExtraPosCoordGroup( coords, 0, rangeCoordFlags, true );
    }

    /**
     * Returns a coord group with no coordinates.
     *
     * @return  new coord group
     */
    public static CoordGroup createEmptyCoordGroup() {
        return new BasicCoordGroup( 0, new Coord[ 0 ], 0 );
    }

    /**
     * Returns the number of coordinates used to store a single point position,
     * for a given DataGeom.
     *
     * @param  geom   data geom
     * @return    geom.getPosCoords().length;
     */
    private static int getPosCoordCount( DataGeom geom ) {
        return geom == null ? 0 : geom.getPosCoords().length;
    }

    /**
     * CoordGroup implementation with positional and extra coordinates.
     */
    private static class BasicCoordGroup extends CoordGroup {
        final int nBasicPos_;
        final Coord[] extraCoords_;
        final int nExtraPos_;

        /**
         * Constructor.
         *
         * @param  nBasicPos  number of basic data positions
         * @param  extraCoords  non-positional coordinates
         * @param  nExtraPos  number of extra coords that are positional
         */
        BasicCoordGroup( int nBasicPos, Coord[] extraCoords, int nExtraPos ) {
            nBasicPos_ = nBasicPos;
            extraCoords_ = extraCoords;
            nExtraPos_ = nExtraPos;
        }
        public int getBasicPositionCount() {
            return nBasicPos_;
        }
        public Coord[] getExtraCoords() {
            return extraCoords_;
        }
        public int getExtraPositionCount() {
            return nExtraPos_;
        }
        public int getPosCoordIndex( int ipos, DataGeom geom ) {
            return getPosCoordCount( geom ) * ipos;
        }
        public int getExtraCoordIndex( int iExtra, DataGeom geom ) {
            return getPosCoordCount( geom ) * nBasicPos_ + iExtra;
        }
        public int[] getRangeCoordIndices( DataGeom geom ) {
            int nRange = getPosCoordCount( geom ) * nBasicPos_ + nExtraPos_;
            int[] ixs = new int[ nRange ];
            for ( int i = 0; i < nRange; i++ ) {
                ixs[ i ] = i;
            }
            return ixs;
        }
        public boolean isSinglePartialPosition() {
            return false;
        }
    }

    /**
     * CoordGroup implementation representing a single partial position.
     */
    private static class ExtraPosCoordGroup extends CoordGroup {
        final Coord[] coords_;
        final int nExtraPos_;
        final int[] rangeCoordIndices_;
        final boolean isPartial_;

        /**
         * Constructor.
         *
         * @param   coords   all coordinates
         * @param   nExtraPos  number of coordinates considered positional
         * @param  rangeCoordFlags  array of flags corresponding to the
         *         <code>coords</code> array, true for any coord whose change
         *         should cause a re-range
         * @param  isPartial  indicates whether positions are partial
         */
        ExtraPosCoordGroup( Coord[] coords, int nExtraPos,
                            boolean[] rangeCoordFlags, boolean isPartial ) {
            coords_ = coords;
            nExtraPos_ = nExtraPos;
            IntList ilist = new IntList();
            for ( int i = 0; i < coords.length; i++ ) {
                if ( rangeCoordFlags[ i ] ) {
                    ilist.add( i );
                }
            }
            rangeCoordIndices_ = ilist.toIntArray();
            isPartial_ = isPartial;
        }
        public int getBasicPositionCount() {
            return 0;
        }
        public Coord[] getExtraCoords() {
            return coords_;
        }
        public int getExtraPositionCount() {
            return nExtraPos_;
        }
        public int getPosCoordIndex( int ipos, DataGeom geom ) {
            return -1;
        }
        public int getExtraCoordIndex( int iExtra, DataGeom geom ) {
            return iExtra;
        }
        public int[] getRangeCoordIndices( DataGeom geom ) {
            return rangeCoordIndices_;
        }
        public boolean isSinglePartialPosition() {
            return isPartial_;
        }
    }
}
