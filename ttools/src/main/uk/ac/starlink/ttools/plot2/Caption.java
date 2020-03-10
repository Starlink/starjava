package uk.ac.starlink.ttools.plot2;

import java.util.function.UnaryOperator;

/**
 * Content of textual item to be rendered somewhere on a plot.
 * It currently contains the text and LaTeX representation.
 *
 * <p>Concrete subclasses must supply the LaTeX representation;
 * a number of <code>createCaption</code> factory methods are provided
 * to facilitate this.
 *
 * <p>Caption equality is assessed on the basis of the primary text value only,
 * not the LaTeX value.  In practice, this is likely to be the relevant
 * criterion.
 *
 * @author   Mark Taylor
 * @since    10 Mar 2020
 */
@Equality
public abstract class Caption {

    private final String txt_;

    /**
     * Constructor.
     *
     * @param  txt  plain text caption content
     */
    protected Caption( String txt ) {
        txt_ = txt;
    }

    /**
     * Returns the plain text representation of the caption.
     *
     * @return   plain text content
     */
    public String toText() {
        return txt_;
    }

    /**
     * Returns the LaTeX representation of the caption.
     *
     * @return   latex content
     */
    public abstract String toLatex();

    /**
     * Concatenates another caption following this one.
     * The plain text and latex representations are both just concatenated.
     *
     * @param  other  second caption
     * @return  this caption followed by <code>other</code>
     */
    public Caption append( final Caption other ) {
        final Caption first = this;
        return new Caption( first.txt_ + other.txt_ ) {
            public String toLatex() {
                return first.toLatex() + other.toLatex();
            }
        };
    }

    @Override
    public int hashCode() {
        return PlotUtil.hashCode( txt_ );
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof Caption ) {
            Caption other = (Caption) o;
            return PlotUtil.equals( this.txt_, other.txt_ );
        }
        else {
            return false;
        }
    }

    /**
     * Constructs a caption for which the LaTeX representation is the
     * same as the plain text representation.
     * Currently not quoting is performed, so it is the responsibility
     * of the caller to ensure that the plain text does not contain
     * any LaTeX markup.
     *
     * @param  txt  caption content (plain text and latex)
     * @return   new caption
     */
    public static Caption createCaption( String txt ) {
        return createCaption( txt, txt );
    }

    /**
     * Constructs a caption for which the LaTeX representation is supplied
     * explicitly.
     *
     * @param  txt   plain text representation
     * @param  latex  LaTeX representation
     * @return   new caption
     */
    public static Caption createCaption( String txt, final String latex ) {
        return new Caption( txt ) {
            public String toLatex() {
                return latex;
            }
        };
    }

    /**
     * Constructs a caption for which the LaTeX representation will be
     * generated lazily from the plain text.
     *
     * @param  txt  plain text caption
     * @param  toLatexFunc  function that maps plain text to latex
     * @return   new caption
     */
    public static Caption
            createCaption( final String txt,
                           final UnaryOperator<String> toLatexFunc ) {
        return new Caption( txt ) {
            public String toLatex() {
                return toLatexFunc.apply( txt );
            }
        };
    }
}
