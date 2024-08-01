package uk.ac.starlink.ttools.plottask;

import java.awt.Color;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.TaskException;

public class ColorParameter extends NamedObjectParameter<Color> {

    /**
     * Constructor.
     *
     * @param  name  parameter name
     */
    @SuppressWarnings("this-escape")
    public ColorParameter( String name ) {
        super( name, Color.class );
        setUsage( "<rrggbb>|red|blue|..." );
        addOption( "red", new Color( 0xf00000 ) );
        addOption( "blue", new Color( 0x0000f0 ) );
        addOption( "green", Color.green.darker() );
        addOption( "grey", Color.gray );
        addOption( "magenta", Color.magenta );
        addOption( "cyan", Color.cyan.darker() );
        addOption( "orange", Color.orange );
        addOption( "pink", Color.pink );
        addOption( "yellow", Color.yellow );
        addOption( "black", Color.black );
        addOption( "white", Color.white );
    }

    /**
     * Returns the value of this parameter as a Color object.
     *
     * @param  env  execution environment
     */
    public Color colorValue( Environment env ) throws TaskException {
        return objectValue( env );
    }

    /**
     * Sets the default value of this parameter to a given color object.
     * 
     * @param  color   colour
     */
    public void setDefaultColor( Color color ) {
        setDefaultOption( color );
    }

    public String toString( Color option ) {
        return Integer.toString( option.getRGB() & 0x00ffffff, 16 );
    }

    public Color fromString( String name ) {
        return new Color( Integer.parseInt( name, 16 ) );
    }

    /**
     * Returns an XML string, suitable for inclusion in a parameter description,
     * which explains the format of values accepted by this parameter.
     * The returned string is not enclosed in a &lt;p&gt; element.
     *
     * @return   format description XML string
     */
    public String getFormatDescription() {
        StringBuffer sbuf = new StringBuffer()
            .append( "The value may be a 6-digit hexadecimal number giving\n" )
            .append( "red, green and blue intensities,\n" )
            .append( "e.g. \"<code>ff00ff</code>\" for magenta.\n" )
            .append( "Alternatively it may be the name of one of the\n" )
            .append( "pre-defined colours.\n" )
            .append( "These are currently\n" );
        String[] names = getNames();
        for ( int i = 0; i < names.length; i++ ) {
            if ( i == names.length - 1 ) {
                sbuf.append( " and " );
            }
            else if ( i > 0 ) {
                sbuf.append( ", " );
            }
            sbuf.append( names[ i ] );
        }
        sbuf.append( "." );
        return sbuf.toString();
    }
}
