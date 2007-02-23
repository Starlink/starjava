package uk.ac.starlink.topcat.plot;

/**
 * Enumeration class which describes, for each dimension, what is the
 * error reporting regime.
 *
 * @author   Mark Taylor
 */
public class ErrorMode {

    private final String name_;
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

    private static final ErrorMode[] ALL_OPTIONS = new ErrorMode[] {
        NONE = new ErrorMode( "None", 0, 0 ),
        SYMMETRIC = new ErrorMode( "Symmetric", 1, 1 ),
        LOWER = new ErrorMode( "Lower Only", 1, 0 ),
        UPPER = new ErrorMode( "Upper Only", 0, 1 ),
        BOTH = new ErrorMode( "Lower & Upper", 1, 0.75 ),
    };

    /**
     * Constructor.
     */
    private ErrorMode( String name, double loOff, double hiOff ) {
        name_ = name;
        loOff_ = loOff;
        hiOff_ = hiOff;
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
}
