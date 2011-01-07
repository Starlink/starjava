package uk.ac.starlink.ttools.plottask;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
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
        List alwaysList = Arrays.asList( new String[] {
            "serif", "sansserif", "monospaced", "dialog", "dialoginput",
        } );
        List otherList = new ArrayList( Arrays.asList( getOptionNames() ) );
        otherList.removeAll( alwaysList );
        otherList = otherList.subList( 0, Math.min( otherList.size(), 24 ) );
        setDescription( new String[] {
            "<p>Determines the font that will be used for textual annotation",
            "of the plot, including axes etc.",
            "At least the following fonts will be available:",
            xmlList( alwaysList ),
            "as well as a range of system-dependent fonts,",
            "possibly including",
            xmlList( otherList ),
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

    /**
     * Turns a list of strings into an XML UL element.
     *
     * @param  sList  list of strings
     * @return   UL list
     */
    private static String xmlList( List sList ) {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( "<ul>\n" );
        for ( Iterator it = sList.iterator(); it.hasNext(); ) {
            sbuf.append( "<li>" )
                .append( "<code>" )
                .append( "<![CDATA[" )
                .append( (String) it.next() )
                .append( "]]>" )
                .append( "</code>" )
                .append( "</li>" )
                .append( "\n" );
        }
        sbuf.append( "</ul>" );
        return sbuf.toString();
    }
}
