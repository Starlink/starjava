package uk.ac.starlink.ttools.plot2;

/**
 * Aggregates information required for generating an Aux colour axis.
 *
 * @author  Mark Taylor
 * @since   19 Jul 2023
 */
public class ShadeAxisKit {

    private final ShadeAxisFactory axisFact_;
    private final Span fixSpan_;
    private final Subrange subrange_;

    /**
     * Constructor.
     *
     * @param  axisFact   shade axis factory
     * @param  fixSpan    fixed (typically user-supplied) values bounding
     *                    the axis range; one or both bounds may be NaN or the
     *                    value itself may be null for full/partial auto-ranging
     * @param  subrange   subrange to be applied to the specified or automatic
     *                    data range; may be null for default behaviour
     */
    public ShadeAxisKit( ShadeAxisFactory axisFact, Span fixSpan,
                         Subrange subrange ) {
        axisFact_ = axisFact;
        fixSpan_ = fixSpan;
        subrange_ = subrange;
    }

    /**
     * Constructs a ShadeAxisKit with null members.
     */
    public ShadeAxisKit() {
        this( null, null, null );
    }

    /**
     * Returns the shade axis factory which determines most of the
     * characteristics of the axis and its scaling.
     *
     * @return  shade axis factory
     */
    public ShadeAxisFactory getAxisFactory() {
        return axisFact_;
    }

    /**
     * Returns an object supplying one or both data bounds for the axis
     * if provided (typically user-supplied).
     * Either or both bounds may be NaN, or the result itself may be null,
     * to indicate full or partial auto-ranging.
     *
     * @return  fixed span, or null
     */
    public Span getFixSpan() {
        return fixSpan_;
    }

    /**
     * Returns a subrange to be applied to the fixed or
     * automatically-determined data range.
     * May be null for default behaviour (full range).
     *
     * @return  subrange, or null
     */
    public Subrange getSubrange() {
        return subrange_;
    }
}
