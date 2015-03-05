package uk.ac.starlink.topcat.plot2;

import java.awt.event.ActionListener;
import java.util.Map;
import javax.swing.Box;
import javax.swing.JComponent;
import uk.ac.starlink.topcat.AuxWindow;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.SpecifierPanel;

/**
 * ConfigMap specifier that groups sub-specifiers into named sub-panels.
 * This can be used as a drop-in replacement for ConfigSpecifier.
 *
 * @author   Mark Taylor
 * @since    5 Mar 2015
 */
public class GroupedConfigSpecifier extends SpecifierPanel<ConfigMap> {

    private final String[] groupNames_;
    private final ConfigSpecifier[] specifiers_;

    /**
     * Constructor.
     *
     * @param  groupedKeys  map of group name to config key list
     *                      defining keys to acquire values for
     */
    public GroupedConfigSpecifier( Map<String,ConfigKey[]> groupedKeys ) {
        super( true );
        ActionListener forwarder = getActionForwarder();
        int ng = groupedKeys.size();
        groupNames_ = new String[ ng ];
        specifiers_ = new ConfigSpecifier[ ng ];
        int ig = 0;
        for ( Map.Entry<String,ConfigKey[]> entry : groupedKeys.entrySet() ) {
            groupNames_[ ig ] = entry.getKey();
            ConfigSpecifier specifier = new ConfigSpecifier( entry.getValue() );
            specifier.addActionListener( forwarder );
            specifiers_[ ig ] = specifier;
            ig++;
        }
        assert ig == ng;
    }

    protected JComponent createComponent() {
        JComponent box = Box.createVerticalBox();
        for ( int ig = 0; ig < specifiers_.length; ig++ ) {
            JComponent gPanel = specifiers_[ ig ].createComponent();
            gPanel.setBorder( AuxWindow.makeTitledBorder( groupNames_[ ig ] ) );
            box.add( gPanel );
        }
        return box;
    }

    public ConfigMap getSpecifiedValue() {
        ConfigMap config = new ConfigMap();
        for ( ConfigSpecifier specifier : specifiers_ ) {
            config.putAll( specifier.getSpecifiedValue() );
        }
        return config;
    }

    public void setSpecifiedValue( ConfigMap config ) {
        for ( ConfigSpecifier specifier : specifiers_ ) {
            specifier.setSpecifiedValue( config );
        }
    }
}
