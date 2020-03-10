package uk.ac.starlink.ttools.plot2;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.scilab.forge.jlatexmath.ParseException;
import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXFormula;
import org.scilab.forge.jlatexmath.TeXIcon;

/**
 * Captioner implementation based on LaTeX text rendering.
 * The hard work is done by the rather wonderful
 * <a href="http://forge.scilab.org/index.php/p/jlatexmath/">jLaTeXMath</a>.
 *
 * @author   Mark Taylor
 * @since    12 Feb 2013
 */
public class LatexCaptioner implements Captioner {

    private final int style_;
    private final float size_;
    private final int type_;
    public static final float DEFAULT_SIZE = 16f;
    public static final int STYLE_TEXT = TeXConstants.STYLE_TEXT;
    public static final int STYLE_DISPLAY = TeXConstants.STYLE_DISPLAY;
    public static final int STYLE_SCRIPT = TeXConstants.STYLE_SCRIPT;
    public static final int STYLE_SCRIPT_SCRIPT =
            TeXConstants.STYLE_SCRIPT_SCRIPT;
    public static final int TYPE_SERIF = TeXFormula.SERIF;
    public static final int TYPE_SANSSERIF = TeXFormula.SANSSERIF;
    public static final int TYPE_BOLD = TeXFormula.BOLD;
    public static final int TYPE_ITALIC = TeXFormula.ITALIC;
    public static final int TYPE_ROMAN = TeXFormula.ROMAN;
    public static final int TYPE_TYPEWRITER = TeXFormula.TYPEWRITER;
    
    private final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.plot2" );
    static {
        // Avoids a warning.  Don't think it does anything useful unless
        // you're using FOP, but I'm not certain.
        TeXFormula.registerFonts( false );
    }

    /**
     * Constructs a captioner with a default font.
     */
    public LatexCaptioner() {
        this( DEFAULT_SIZE );
    }

    /**
     * Constructs a captioner with the default font in a given size.
     *
     * @param  size   font size
     */
    public LatexCaptioner( float size ) {
        this( size, TYPE_SANSSERIF );
    }

    /**
     * Constructs a captioner with a given font size and type.
     * Font type is as per the <code>jlatexmath.TeXFormula</code>
     * constants
     *
     * @param  size   font size
     * @param  type   font type; to some extent these can be ORed together
     */
    public LatexCaptioner( float size, int type ) {
        this( size, type, STYLE_TEXT );
    }

    /**
     * Constructs a captioner with a given font size, type and style
     *
     * @param  size   font size
     * @param  type   font type, one of the <code>TYPE_*</code> constants;
     *                to some extent these can be ORed together
     * @param  style  TeX presentation style,
     *                one of the <code>STYLE_*</code> constants
     */
    public LatexCaptioner( float size, int type, int style ) {
        size_ = size;
        type_ = type;
        style_ = style;
    }

    public void drawCaption( Caption label, Graphics g ) {
        String latex = label.toLatex();
        TeXIcon ti;
        try {
            ti = createTeXIcon( latex, g.getColor() );
        }
        catch ( ParseException e ) {
            getErrorCaptioner().drawCaption( getErrorCaption( latex, e ), g );
            logger_.log( Level.WARNING, "Bad LaTeX: \"" + latex + "\" ("
                       + e.getMessage() + ")", e );
            return;
        }
        Insets insets = ti.getInsets();
        ti.paintIcon( null, g.create(),
                      -insets.left, -ti.getIconHeight() + insets.top );
    }

    public Rectangle getCaptionBounds( Caption label ) {
        String latex = label.toLatex();
        TeXIcon ti;
        try {
            ti = createTeXIcon( latex, Color.BLACK );
        }
        catch ( ParseException e ) {
            return getErrorCaptioner()
                  .getCaptionBounds( getErrorCaption( latex, e ) );
        }
        Insets insets = ti.getInsets();
        Rectangle bounds =
            new Rectangle( -insets.left,
                           -ti.getIconHeight() + insets.top + insets.bottom,
                           (int) Math.ceil( ti.getTrueIconWidth() ),
                           (int) Math.ceil( ti.getTrueIconHeight() ) );
        return bounds;
    }

    public int getPad() {
        return (int) size_ / 2;
    }

    /**
     * Constructs a TeXIcon object from the given text.
     *
     * @param   latex  label source code
     * @param   color  paint colour
     * @return   icon
     */
    private TeXIcon createTeXIcon( String latex, Color color )
            throws ParseException {
        return new TeXFormula( adjustLabel( latex ) )
              .createTeXIcon( style_, size_, type_, color );
    }

    /**
     * Massages the label text to make it work better for LaTeX formatting.
     * This is a hack.
     *
     * @param  label   input label text
     * @return   text to pass to LaTeX
     */
    private String adjustLabel( String label ) {
        return label.replaceFirst( "^-", "\\$-\\$" );
    }

    /**
     * Text used as display in case of a LaTeX parse error.
     *
     * @param  latex  input latex source
     * @param  err  latex parse error
     * @retrun   error message string
     */
    private Caption getErrorCaption( String latex, ParseException err ) {
        return Caption.createCaption( "Bad TeX: " + latex );
    }

    /**
     * Returns the captioner used for presenting errors.
     * A non-LaTeX one is used here, partly to ensure that another parse
     * error doesn't happen during error presentation.
     * Non-LaTeX formatting is probably sensible for error messages
     * in any case though.
     *
     * @return   error formatter
     */
    private Captioner getErrorCaptioner() {
        return new BasicCaptioner();
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof LatexCaptioner ) {
            LatexCaptioner other = (LatexCaptioner) o;
            return this.size_ == other.size_
                && this.type_ == other.type_
                && this.style_ == other.style_;
        }
        else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int code = 90125;
        code = 23 * code + Float.floatToIntBits( size_ );
        code = 23 * code + type_;
        code = 23 * code + style_;
        return code;
    }
}
