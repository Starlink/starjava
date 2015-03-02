package uk.ac.starlink.ttools.plot2.layer;

/**
 * Factory class for Kernel1d smoothing functions.
 *
 * @author   Mark Taylor
 * @since    2 Mar 2015
 */
public abstract class Kernel1dShape {

    private final String name_;
    private final String description_;

    /** Rectangular kernel. */
    public static final Kernel1dShape SQUARE =
            new Kernel1dShape( "square", "uniform value over a fixed range" ) {
        public Kernel1d createKernel( double width ) {
            return Kernel1ds.createSquareKernel( (int) Math.ceil( width ) );
        }
    };

    /** Cosine kernel truncated at PI/2. */
    public static final Kernel1dShape COS =
            new Kernel1dShape( "cos", "cosine function truncated at PI/2" ) {
        public Kernel1d createKernel( double width ) {
            return Kernel1ds.createCosKernel( width );
        }
    };

    /** Cosine<sup>2</sup> kernel truncated at PI/2. */
    public static final Kernel1dShape COS2 =
            new Kernel1dShape( "cos2",
                               "cosine squared function truncated at PI/2" ) {
        public Kernel1d createKernel( double width ) {
            return Kernel1ds.createCos2Kernel( width );
        }
    };

    /** Gaussian kernel truncated at 3 sigma. */
    public static final Kernel1dShape GAUSS3 = createGaussShape( 3 );

    /** Gaussian kernel truncated at 6 sigma. */
    public static final Kernel1dShape GAUSS6 = createGaussShape( 6 );

    private static final Kernel1dShape[] STANDARD_OPTIONS = {
        SQUARE, COS2, COS, GAUSS3, GAUSS6,
    };

    /**
     * Constructor.
     *
     * @param  name  kernel shape name
     * @param  description   short description
     */
    protected Kernel1dShape( String name, String description ) {
        name_ = name;
        description_ = description;
    }

    /**
     * Returns a one-word name for this shape.
     *
     * @return  name
     */
    public String getName() {
        return name_;
    }

    /**
     * Returns a short description for this shape.
     *
     * @return  description
     */
    public String getDescription() {
        return description_;
    }

    /**
     * Creates a kernel with a given nominal width.
     * The width is some kind of characteristic width in one direction
     * of the smoothing function.  It would generally be less than or
     * equal to the kernel's extent.
     *
     * @param  width  half-width
     * @return  new kernel
     */
    public abstract Kernel1d createKernel( double width );

    @Override
    public String toString() {
        return name_;
    }

    /**
     * Returns an array of the generally recommended kernel shape options.
     *
     * @return  kernel shape options
     */
    public static Kernel1dShape[] getStandardOptions() {
        return STANDARD_OPTIONS.clone();
    }

    /**
     * Creates a truncated Gaussian kernel shape, with truncation at
     * a given number of standard deviations from the mean.
     *
     * @param  truncSigma  truncation distance in units of SD
     * @return  new kernel shape
     */
    private static final Kernel1dShape
            createGaussShape( final int truncSigma ) {
        return new Kernel1dShape( "gauss" + truncSigma,
                                  "Gaussian function truncated at "
                                 + truncSigma + " sigma" ) {
            public Kernel1d createKernel( double width ) {
                int extent = (int) Math.ceil( truncSigma * width );
                return Kernel1ds.createTruncatedGaussianKernel( width, extent );
            }
        };
    }
}
