package uk.ac.starlink.ast;

/**
 * Aggregates flags which influence how Mapping.resample* methods operate.
 *
 * <p>The flags are
 *
 * <dl>
 * <dt>noBad</dt>
 * <dd>Indicates that any output array elements for which no
 *     resampled value could be obtained should be left set to the value
 *     they had on entry to this function. If this flag is not supplied,
 *     such output array elements are set to the value supplied for
 *     parameter <code>badval</code>.
 *     Note, this flag cannot be used in conjunction
 *     with the <code>conserveFlux</code> flag 
 *     (an error will be reported if both
 *     flags are specified).
 *     </dd>
 * <dt>useBad</dt>
 * <dd>Indicates that there may be bad pixels in the
 *     input array(s) which must be recognised by comparing with the
 *     value given for <code>badval</code>
 *     and propagated to the output array(s).
 *     If this flag is not set, all input values are treated literally
 *     and the <code>badval</code> value is only used for flagging output array
 *     values.
 *     </dd>
 * <dt>conserveFlux</dt>
 * <dd>Indicates that the output pixel values should
 *     be scaled in such a way as to preserve (approximately) the total data
 *     value in a feature on the sky. Without this flag, each output pixel
 *     value represents an instantaneous sample of the input data values at
 *     the corresponding input position. This is appropriate if the input
 *     data represents the spatial density of some quantity (e.g. surface
 *     brightness in Janskys per square arc-second) because the output
 *     pixel values will have the same normalisation and units as the
 *     input pixel values. However, if the input data values represent
 *     flux (or some other physical quantity) per pixel, then the
 *     <code>conserveFlux</code> flag could be used. This causes each output
 *     pixel value to be scaled by the ratio of the output pixel size to
 *     the input pixel size.
 *     <br/>
 *     This flag can only be used if the Mapping is succesfully approximated
 *     by one or more linear transformations. Thus an error will be reported
 *     if it used when the
 *     <code>tol</code> parameter
 *     is set to zero (which stops the use of linear approximations), or
 *     if the Mapping is too non-linear to be approximated by a piece-wise
 *     linear transformation. The ratio of output to input pixel size is
 *     evaluated once for each panel of the piece-wise linear approximation to
 *     the Mapping, and is assumed to be constant for all output pixels in the
 *     panel. The scaling factors for adjacent panels will in general
 *     differ slightly, and so the joints between panels may be visible when
 *     viewing the output image at high contrast. If this is a problem,
 *     reduce the value of the
 *     <code>tol</code> parameter
 *     until the difference between adjacent panels is sufficiently small
 *     to be insignificant.
 *     <br/>
 *     Note, this flag cannot be used in conjunction with the <code>noBad</code>
 *     flag (an error will be reported if both flags are specified).
 *     Flux conservation can only be approximate when using a resampling
 *     algorithm. For accurate flux conservation use the
 *     {@link Mapping#rebin rebin} method
 *     instead.
 * </dl>
 *
 * @author   Mark Taylor
 * @since    26 Feb 2009
 */
public class ResampleFlags {
    private boolean noBad_;
    private boolean useBad_;
    private boolean conserveFlux_;

    private static final int AST__NOBAD =
        AstObject.getAstConstantI( "AST__NOBAD" );
    private static final int AST__USEBAD =
        AstObject.getAstConstantI( "AST__USEBAD" );
    private static final int AST__CONSERVEFLUX =
        AstObject.getAstConstantI( "AST__CONSERVEFLUX" );

    /**
     * Constructs an ResampleFlags with all flags set false.
     */
    ResampleFlags() {
    }

    /**
     * Sets the nobad flag.
     */
    public void setNoBad( boolean noBad ) {
        noBad_ = noBad;
    }

    /**
     * Returns the value of the noBad flag.
     */
    public boolean getNoBad() {
        return noBad_;
    }

    /**
     * Sets the useBad flag.
     */
    public void setUseBad( boolean useBad ) {
        useBad_ = useBad;
    }

    /**
     * Returns the value of the usebad flag.
     */
    public boolean getUseBad() {
        return useBad_;
    }

    /**
     * Sets the conserveFlux flag.
     */
    public void setConserveFlux( boolean conserveFlux ) {
        conserveFlux_ = conserveFlux;
    }

    /**
     * Returns the value of the conserveFlux flag.
     */
    public boolean getConserveFlux() {
        return conserveFlux_;
    }

    /**
     * Returns the value of this object as an integer, suitable for supplying
     * to one of the <code>astResampleX()</code> functions.
     * If necessary, this method may be overridden to supply flags which
     * are undefined by this java class.
     *
     * @return   packed int representing resampling flags
     */
    public int getFlagsInt() {
        int flag = 0;
        if ( getNoBad() ) {
            flag |= AST__NOBAD;
        }
        if ( getUseBad() ) {
            flag |= AST__USEBAD;
        }
        if ( getConserveFlux() ) {
            flag |= AST__CONSERVEFLUX;
        }
        return flag;
    }
}
