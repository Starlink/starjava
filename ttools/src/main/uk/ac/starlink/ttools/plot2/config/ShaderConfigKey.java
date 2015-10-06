package uk.ac.starlink.ttools.plot2.config;

import javax.swing.JComboBox;
import uk.ac.starlink.ttools.gui.ShaderListCellRenderer;
import uk.ac.starlink.ttools.plot.Shader;

/**
 * ConfigKey for selecting shader objects.
 *
 * @author   Mark Taylor
 * @since    9 Sep 2014
 */
public class ShaderConfigKey extends OptionConfigKey<Shader> {

    /**
     * Constructor.
     *
     * @param  meta  metadata
     * @param  shaders  list of options
     * @param  dflt  default value
     */
    public ShaderConfigKey( ConfigMeta meta, Shader[] shaders, Shader dflt ) {
        super( meta, Shader.class, shaders, dflt );
        if ( meta.getStringUsage() == null ) {
            setOptionUsage();
        }
    }

    @Override
    public String valueToString( Shader shader ) {
        if ( shader == null ) {
            return null;
        }
        String name = shader.getName();
        if ( name.startsWith( "-" ) ) {
            name = name.substring( 1 );
        }
        return name.toLowerCase().replaceAll( " ", "_" );
    }

    public String getXmlDescription( Shader shader ) {
        return null;
    }

    public Specifier<Shader> createSpecifier() {
        JComboBox comboBox = new JComboBox( getOptions() );
        comboBox.setSelectedItem( getDefaultValue() );
        comboBox.setRenderer( new ShaderListCellRenderer( comboBox ) );
        return new ComboBoxSpecifier<Shader>( comboBox );
    }

    /**
     * Appends a list of the available shaders to the end of the existing
     * XML documentation for this key.
     *
     * @return  this object, as a convenience
     */
    public ShaderConfigKey appendShaderDescription() {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( "<p>A mixed bag of colour ramps are available:\n" );
        Shader[] shaders = getOptions();
        int ns = shaders.length;
        for ( int is = 0; is < ns; is++ ) {
            sbuf.append( "<code>" )
                .append( valueToString( shaders[ is ] ) )
                .append( "</code>" )
                .append( is < ns - 1 ? "," : "." )
                .append( "\n" );
        }
        sbuf.append( "<em>Note:</em>\n" )
            .append( "many of these, including rainbow-like ones,\n" )
            .append( "are frowned upon by the visualisation community.\n" );
        sbuf.append( "</p>\n" );
        getMeta().appendXmlDescription( new String[] { sbuf.toString() } );
        return this;
    }
}
