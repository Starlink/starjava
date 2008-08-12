package uk.ac.starlink.tptask;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.IntegerParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;

/**
 * Parameter for font selection.
 *
 * @author   Mark Taylor
 * @since    12 Aug 2008
 */
public class FontParameter extends StyleParameter {

    private final IntegerParameter sizeParam_;
    private final ChoiceParameter styleParam_;
    private Font fontValue_;

    /**
     * Constructor.
     *
     * @param  name  parameter base name
     */
    public FontParameter( String name ) {
        super( name, GraphicsEnvironment.getLocalGraphicsEnvironment()
                                        .getAvailableFontFamilyNames() );

        /* Set the default font name.  In J2SE, font "dialog" should always
         * be present, and it ought to be a sensible default. */
        String dflt = "dialog";
        if ( Arrays.asList( getOptionNames() ).contains( dflt ) ) {
            setDefault( dflt );
        }
        else {
            assert false;
            setDefault( getOptionNames()[ 0 ] );
        }
        setPrompt( "Font family name" );
        setUsage( "dialog|serif|..." );
        setDescription( new String[] {
            "<p>Determines the font that will be used for textual annotation",
            "of the plot, including axes etc.",
            "The available names are:",
            getOptionList(),
            "</p>",
        } );

        /* Set up associated font size parameter. */
        sizeParam_ = new IntegerParameter( name + "size" );
        sizeParam_.setPrompt( "Font size" );
        sizeParam_.setDescription( new String[] {
            "<p>Sets the font size used for plot annotations.",
            "</p>",
        } );
        sizeParam_.setDefault( Integer.toString( 12 ) );
        sizeParam_.setMinimum( 1 );

        /* Set up associated font style parameter. */
        styleParam_ = new ChoiceParameter( name + "style" );
        styleParam_.addOption( new Integer( Font.PLAIN ), "plain" );
        styleParam_.addOption( new Integer( Font.BOLD ), "bold" );
        styleParam_.addOption( new Integer( Font.ITALIC ), "italic" );
        styleParam_.addOption( new Integer( Font.BOLD | Font.ITALIC ),
                               "bold-italic" );
        styleParam_.setPrompt( "Font modifier" );
        styleParam_.setDescription( new String[] {
            "<p>Gives a style in which the font is to be applied for",
            "plot annotations.",
            "Options are",
            "<code>plain</code>,",
            "<code>bold</code>,",
            "<code>italic</code> and",
            "<code>bold-italic</code>.",
            "</p>",
        } );
        styleParam_.setDefault( "plain" );
    }

    /**
     * Returns parameters associated with this one which select other
     * font characteristics.
     *
     * @return   array of font parameters
     */
    public Parameter[] getAssociatedParameters() {
        return new Parameter[] {
            sizeParam_,
            styleParam_,
        };
    }

    public void setValueFromString( Environment env, String sval )
            throws TaskException {
        int size = sizeParam_.intValue( env );
        int style = ((Integer) styleParam_.objectValue( env )).intValue();
        super.setValueFromString( env, sval );
        String family = (String) objectValue( env );
        fontValue_ = new Font( family, style, size );
    }

    /**
     * Returns the value of this parameter as a font.
     *
     * @param  env  execution environment
     */
    public Font fontValue( Environment env ) throws TaskException {
        checkGotValue( env );
        return fontValue_;
    }
}
