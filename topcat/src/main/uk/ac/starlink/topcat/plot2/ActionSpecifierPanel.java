package uk.ac.starlink.topcat.plot2;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import uk.ac.starlink.topcat.BasicAction;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.Specifier;
import uk.ac.starlink.ttools.plot2.config.SpecifierPanel;

/**
 * Decorates a SpecifierPanel with Clear and Submit buttons.
 * Clear resets all items to their default, and submit invokes
 * this object's <code>doSubmit</code> method.
 *
 * @author   Mark Taylor
 * @since    13 Mar 2013
 */
public class ActionSpecifierPanel extends SpecifierPanel<ConfigMap> {

    private final Specifier<ConfigMap> baseSpecifier_;
    private final Action clearAction_;
    private final Action submitAction_;

    /**
     * Constructor.
     *
     * @param   baseSpecifier  specifier on which this is based
     */
    public ActionSpecifierPanel( Specifier<ConfigMap> baseSpecifier ) {
        super( true );
        baseSpecifier_ = baseSpecifier;
        final ActionListener forwarder = getActionForwarder();

        /* Action to clear the map to default values. */
        final ConfigMap clearMap = new ConfigMap();
        for ( ConfigKey<?> key : baseSpecifier.getSpecifiedValue().keySet() ) {
            putDefaultValue( clearMap, key );
        }
        clearAction_ = new BasicAction( "Clear", null,
                                        "Reset values in this panel" ) {
            public void actionPerformed( ActionEvent evt ) {
                baseSpecifier_.setSpecifiedValue( clearMap );
            }
        };

        /* Action to configure the plot with the currently filled in values. */
        submitAction_ = new BasicAction( "Submit", null,
                                         "Use the values in this panel "
                                       + "to configure the plot" ) {
            public void actionPerformed( ActionEvent evt ) {
                doSubmit( evt );
                forwarder.actionPerformed( evt );
            }
        };
        baseSpecifier_.addActionListener( submitAction_ );
    }

    /**
     * Invoked when the submit action is performed.
     * Default implementation does nothing, but subclasses may override it.
     *
     * @param  evt  submission event
     */
    protected void doSubmit( ActionEvent evt ) {
    }

    public JComponent createComponent() {
        JComponent buttLine = Box.createHorizontalBox();
        buttLine.add( Box.createHorizontalGlue() );
        buttLine.add( new JButton( clearAction_ ) );
        buttLine.add( Box.createHorizontalStrut( 10 ) );
        buttLine.add( new JButton( submitAction_ ) );
        JComponent panel = Box.createVerticalBox();
        panel.add( baseSpecifier_.getComponent() );
        panel.add( Box.createVerticalStrut( 10 ) );
        panel.add( buttLine );
        return panel;
    }

    public ConfigMap getSpecifiedValue() {
        return baseSpecifier_.getSpecifiedValue();
    }
    
    public void setSpecifiedValue( ConfigMap config ) {
        baseSpecifier_.setSpecifiedValue( config );
    }

    public void submitReport( ReportMap report ) {
        baseSpecifier_.submitReport( report );
    }

    /**
     * Invokes the clear action on this panel.
     */
    public void clear() {
        clearAction_.actionPerformed( null );
    }

    /**
     * Enters the default value for a given key into a config map.
     *
     * @param  map  config map
     * @param  key  config key
     */
    private static <T> void putDefaultValue( ConfigMap map, ConfigKey<T> key ) {
        map.put( key, key.getDefaultValue() );
    }
}
