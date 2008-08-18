package uk.ac.starlink.ttools.plot;

/**
 * StyleSet which wraps an existing one to give it a different error renderer.
 *
 * @author   Mark Taylor
 * @since    20 Mar 2007
 */
public class ErrorMarkStyleSet implements StyleSet {

    private final StyleSet base_;
    private final ErrorRenderer errorRenderer_;

    /**
     * Constructor.
     *
     * @param  base  base style set
     * @param  errorRenderer  renderer which each style from this set will have
     */
    public ErrorMarkStyleSet( StyleSet base, ErrorRenderer errorRenderer ) {
        base_ = base;
        errorRenderer_ = errorRenderer;
    }

    public String getName() {
        return base_.getName();
    }

    public Style getStyle( int index ) {
        Style style = base_.getStyle( index );
        if ( style instanceof MarkStyle ) {
            ((MarkStyle) style).setErrorRenderer( errorRenderer_ );
        }
        else {
            assert false;
        }
        return style;
    }
}
