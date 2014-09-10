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

    public Specifier<Shader> createSpecifier() {
        JComboBox comboBox = new JComboBox( getOptions() );
        comboBox.setSelectedItem( getDefaultValue() );
        comboBox.setRenderer( new ShaderListCellRenderer( comboBox ) );
        return new ComboBoxSpecifier<Shader>( comboBox );
    }
}
