package uk.ac.starlink.ttools.plot2.config;

import java.awt.Color;
import javax.swing.JComboBox;
import uk.ac.starlink.ttools.gui.ShaderListCellRenderer;
import uk.ac.starlink.ttools.plot.Shader;
import uk.ac.starlink.ttools.plot.Shaders;

/**
 * ConfigKey for selecting shader objects.
 *
 * @author   Mark Taylor
 * @since    9 Sep 2014
 */
public class ShaderConfigKey extends ChoiceConfigKey<Shader> {

    private Shader[] shaders_;

    /**
     * Constructor.
     *
     * @param  meta  metadata
     * @param  shaders  list of options
     * @param  dflt  default value
     */
    @SuppressWarnings("this-escape")
    public ShaderConfigKey( ConfigMeta meta, Shader[] shaders, Shader dflt ) {
        super( meta, Shader.class, dflt, false );
        shaders_ = shaders;
        for ( Shader shader : shaders ) {
            addOption( shader );
        }
    }

    public Shader decodeString( String sval ) {
        for ( Shader shader : shaders_ ) {
            if ( stringifyValue( shader ).equalsIgnoreCase( sval ) ) {
                return shader;
            }
        }
        String[] elements = sval.split( "-", -1 );
        int nel = elements.length;
        if ( nel > 1 ) {
            Color[] colors = new Color[ nel ];
            for ( int i = 0; i < nel; i++ ) {
                Color color = ColorConfigKey.decodeColorName( elements[ i ] );
                if ( color == null ) {
                    return null;
                }
                colors[ i ] = color;
            }
            return Shaders.createInterpolationShader( sval, colors );
        }
        else {
            return null;
        }
    }

    public String stringifyValue( Shader value ) {
        return value.getName()
                    .toLowerCase()
                    .replaceAll( " ", "_" )
                    .replaceFirst( "^-", "" );
    }

    public Specifier<Shader> createSpecifier() {
        JComboBox<Shader> comboBox = new JComboBox<>( shaders_ );
        comboBox.setSelectedItem( getDefaultValue() );
        comboBox.setRenderer( new ShaderListCellRenderer( comboBox ) );
        return new ComboBoxSpecifier<Shader>( Shader.class, comboBox );
    }

    /**
     * Appends a list of the available shaders, as well as rules for
     * naming custom ones, to the end of the existing XML documentation
     * for this key.
     *
     * @return  this object, as a convenience
     */
    public ShaderConfigKey appendShaderDescription() {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( "<p>A mixed bag of colour ramps are available\n" )
            .append( "as listed in <ref id='Shaders'/>:\n" );
        int ns = shaders_.length;
        for ( int is = 0; is < ns; is++ ) {
            sbuf.append( "<code>" )
                .append( valueToString( shaders_[ is ] ) )
                .append( "</code>" )
                .append( is < ns - 1 ? "," : "." )
                .append( "\n" );
        }
        sbuf.append( "<em>Note:</em>\n" )
            .append( "many of these, including rainbow-like ones,\n" )
            .append( "are frowned upon by the visualisation community.\n" )
            .append( "</p>\n" )
            .append( "<p>You can also construct your own custom colour map\n" )
            .append( "by giving a sequence of colour names separated by\n" )
            .append( "minus sign (\"<code>-</code>\") characters.\n" )
            .append( "In this case the ramp is a linear interpolation\n" )
            .append( "between each pair of colours named,\n" )
            .append( "using the same syntax as when specifying\n" )
            .append( "a colour value.\n" )
            .append( "So for instance\n" )
            .append( "\"<code>yellow-hotpink-#0000ff</code>\"\n" )
            .append( "would shade from yellow via hot pink to blue.\n" )
            .append( "</p>\n" );
        getMeta().appendXmlDescription( new String[] { sbuf.toString() } );
        return this;
    }

    /**
     * Creates a key description suitable for a colour map applied to
     * a named axis.
     *
     * @param  shortName  metadata short name
     * @param  longName   metadata long name
     * @param  axName   user-readable name of axis to which this applies
     * @return   new metadata object
     */
    public static ConfigMeta createAxisMeta( String shortName, String longName,
                                             String axName ) {
        ConfigMeta meta = new ConfigMeta( shortName, longName );
        meta.setShortDescription( "Color map for " + axName + " shading" );
        meta.setXmlDescription( new String[] {
            "<p>Color map used for",
            axName,
            "axis shading.",
            "</p>",
        } );
        meta.setStringUsage( "<map-name>|<color>-<color>[-<color>...]" );
        return meta;
    }
}
