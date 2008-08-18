package uk.ac.starlink.ttools.plot;

/**
 * Enumeration class which describes, for each dimension, what is the
 * error reporting regime.
 *
 * @author   Mark Taylor
 */
public class ErrorMode {

    private final String name_;
    private final Extent[] extents_;
    private final double loOff_;
    private final double hiOff_;

    /** No error bounds. */
    public static final ErrorMode NONE;

    /** The same error value in both directions. */
    public static final ErrorMode SYMMETRIC;

    /** Lower error bound only. */
    public static final ErrorMode LOWER;

    /** Upper error bound only. */
    public static final ErrorMode UPPER;

    /** Both lower and upper bounds specified independently. */
    public static final ErrorMode BOTH;

    /** Extent describing errors only lower than the point value. */
    public static final Extent LOWER_EXTENT = new Extent( "Lower", "-" );

    /** Extent describing errors only higher than the point value. */
    public static final Extent UPPER_EXTENT = new Extent( "Upper", "+" );

    /** Extent describing errors symmetrically lower and higher than 
     *  the point value. */
    public static final Extent BOTH_EXTENT = new Extent( "Both", "+/-" );

    /** All known modes. */
    private static final ErrorMode[] ALL_OPTIONS = new ErrorMode[] {
        NONE =
            new ErrorMode( "None", new Extent[ 0 ], 0, 0 ),
        SYMMETRIC =
            new ErrorMode( "Symmetric", new Extent[] { BOTH_EXTENT, }, 1, 1 ),
        LOWER =
            new ErrorMode( "Lower Only", new Extent[] { LOWER_EXTENT, }, 1, 0 ),
        UPPER =
            new ErrorMode( "Upper Only", new Extent[] { UPPER_EXTENT, }, 0, 1 ),
        BOTH =
            new ErrorMode( "Lower & Upper",
                           new Extent[] { LOWER_EXTENT, UPPER_EXTENT, },
                           1, 0.75 ),
    };

    /**
     * Constructor.
     *
     * @param  name   mode name
     * @param  extents   array of extents
     * @param  loOff    example lower offset (0..1)
     * @param  hiOff    example upper offset (0..1)
     */
    private ErrorMode( String name, Extent[] extents,
                       double loOff, double hiOff ) {
        name_ = name;
        extents_ = (Extent[]) extents.clone();
        loOff_ = loOff;
        hiOff_ = hiOff;
    }

    /**
     * Returns the extent objects which characterise this mode.
     *
     * @return  extent array
     */
    public Extent[] getExtents() {
        return (Extent[]) extents_.clone();
    }

    /**
     * Returns a value between 0 and 1 which represents an example lower
     * bound for this mode, for instance 1 if the bound is in use and 0 
     * if not.
     *
     * @return  exemplary lower bound
     */
    public double getExampleLower() {
        return loOff_;
    }

    /**
     * Returns a value between 0 and 1 which represents an example upper
     * bound for this mode, for instance 1 if the bound is in use and 0
     * if it is not.
     *
     * @return   examplary upper bound
     */
    public double getExampleUpper() {
        return hiOff_;
    }

    public String toString() {
        return name_;
    }

    /**
     * Returns a list of all the error bar options.
     *
     * @return   error bar count list
     */
    public static ErrorMode[] getOptions() {
        return (ErrorMode[]) ALL_OPTIONS.clone();
    }

    /**
     * Indicates whether an array of error modes represents drawing no
     * error information at all.
     * 
     * @param  modes  list of error modes, one per dimension
     * @return  true if none of the dimensions contains error information
     */
    public static boolean allBlank( ErrorMode[] modes ) {
        if ( modes == null ) {
            return true;
        }
        else {
            for ( int idim = 0; idim < modes.length; idim++ ) {
                ErrorMode mode = modes[ idim ];
                if ( mode != null && ! ErrorMode.NONE.equals( mode ) ) {
                    return false; 
                } 
            }
            return true;
        }
    }

    /**
     * Enumeration class which lists the distance quantities used by different
     * error modes.  Each error mode will have zero or more of these,
     * corresponding to scalar values constituting the described error.
     */
    public static class Extent {

        private final String name_;
        private final String label_;

        /**
         * Constructor.
         *
         * @param  name   name of this extent
         * @param  label  label suitable for labelling a control for this extent
         */
        private Extent( String name, String label ) {
            name_ = name;
            label_ = label;
        }

        /**
         * Returns the name of this extent.
         *
         * @return  extent name
         */
        public String getName() {
            return name_;
        }

        /**
         * Returns a name suitable for labelling a control for this extent.
         *
         * @return  extent control label
         */
        public String getLabel() {
            return label_;
        }
    }
}
