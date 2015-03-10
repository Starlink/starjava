package uk.ac.starlink.topcat.plot2;

import java.awt.Dimension;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.config.ConfigException;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.Specifier;
import uk.ac.starlink.ttools.plot2.config.SpecifierPanel;

/**
 * Specifier for a PlotPosition.
 *
 * @author   Mark Taylor
 * @since    18 Dec 2014
 */
public class PlotPositionSpecifier extends SpecifierPanel<PlotPosition> {

    private final ConfigSpecifier configSpecifier_;

    private static final ConfigKey<Integer> XPIX_KEY =
        createIntegerKey( new ConfigMeta( "xpix", "Outer width" ) );
    private static final ConfigKey<Integer> YPIX_KEY =
        createIntegerKey( new ConfigMeta( "ypix", "Outer height" ) );
    private static final ConfigKey<Integer> TOP_KEY =
        createIntegerKey( new ConfigMeta( "top", "Top border" ) );
    private static final ConfigKey<Integer> LEFT_KEY =
        createIntegerKey( new ConfigMeta( "left", "Left border" ) );
    private static final ConfigKey<Integer> BOTTOM_KEY =
        createIntegerKey( new ConfigMeta( "bottom", "Bottom border" ) );
    private static final ConfigKey<Integer> RIGHT_KEY =
        createIntegerKey( new ConfigMeta( "right", "Right border" ) );

    /**
     * Constructor.
     */
    public PlotPositionSpecifier() {
        super( false );
        List<ConfigKey> keyList = new ArrayList();
        keyList.add( XPIX_KEY );
        keyList.add( YPIX_KEY );
        keyList.add( TOP_KEY );
        keyList.add( LEFT_KEY );
        keyList.add( BOTTOM_KEY );
        keyList.add( RIGHT_KEY );
        ConfigKey[] keys = keyList.toArray( new ConfigKey[ 0 ] );
        configSpecifier_ = new ConfigSpecifier( keys );
        configSpecifier_.addActionListener( getActionForwarder() );
    }

    public PlotPosition getSpecifiedValue() {
        ConfigMap config = configSpecifier_.getSpecifiedValue();
        Integer xpix = config.get( XPIX_KEY );
        Integer ypix = config.get( YPIX_KEY );
        Dimension size = xpix == null && ypix == null
                       ? null
                       : new Dimension( xpix == null ? -1 : xpix.intValue(),
                                        ypix == null ? -1 : ypix.intValue() );
        Integer top = config.get( TOP_KEY );
        Integer left = config.get( LEFT_KEY );
        Integer bottom = config.get( BOTTOM_KEY );
        Integer right = config.get( RIGHT_KEY );
        Insets insets =
              top == null && left == null && bottom == null && right == null
            ? null
            : new Insets( top == null ? -1 : top.intValue(),
                          left == null ? -1 : left.intValue(),
                          bottom == null ? -1 : bottom.intValue(),
                          right == null ? -1 : right.intValue() );
        return new PlotPosition( size, insets );
    }

    public void setSpecifiedValue( PlotPosition plotpos ) {
        ConfigMap config = new ConfigMap();
        Dimension size = plotpos.getPlotSize();
        if ( size != null ) {
            config.put( XPIX_KEY, size.width );
            config.put( YPIX_KEY, size.height );
        }
        Insets insets = plotpos.getPlotInsets();
        if ( insets != null ) {
            config.put( TOP_KEY, insets.top );
            config.put( LEFT_KEY, insets.left );
            config.put( BOTTOM_KEY, insets.bottom );
            config.put( RIGHT_KEY, insets.right );
        }
        configSpecifier_.setSpecifiedValue( config );
    }

    public void submitReport( ReportMap report ) {
    }

    public JComponent createComponent() {
        return configSpecifier_.createComponent();
    }

    /**
     * Returns a ConfigKey for an Integer value.  The default is null,
     * so make sure to acquire the result as an Integer object not
     * using auto-unboxing to get an int primitive.
     *
     * @param  meta  key metadata
     * @return   Integer key
     */
    private static ConfigKey<Integer> createIntegerKey( ConfigMeta meta ) {
        return new ConfigKey<Integer>( meta, Integer.class, null ) {
            public String valueToString( Integer value ) {
                return value == null ? "" : value.toString();
            }
            public Integer stringToValue( String txt ) throws ConfigException {
                if ( txt == null || txt.trim().length() == 0 ) {
                    return null;
                }
                try {
                    return Integer.decode( txt.trim() );
                }
                catch ( NumberFormatException e ) {
                    throw new ConfigException( this,
                                               "\"" + txt + "\" not integer",
                                               e );
                }
            }
            public Specifier<Integer> createSpecifier() {
                return new SpecifierPanel<Integer>( false ) {
                    final JTextField txtField_ = new JTextField( 6 );
                    protected JComponent createComponent() {
                        txtField_.addActionListener( getActionForwarder() );
                        JComponent box = Box.createHorizontalBox();
                        box.add( txtField_ );
                        box.add( Box.createHorizontalStrut( 5 ) );
                        box.add( new JLabel( "pixels" ) );
                        return box;
                    }
                    public Integer getSpecifiedValue() {
                        try {
                            return stringToValue( txtField_.getText() );
                        }
                        catch ( ConfigException e ) {
                            JOptionPane
                           .showMessageDialog( txtField_, e.getMessage(),
                                               "Bad Value",
                                               JOptionPane.ERROR_MESSAGE );
                            txtField_.setText( "" );
                            return null;
                        }
                    }
                    public void setSpecifiedValue( Integer value ) {
                        txtField_.setText( valueToString( value ) );
                        fireAction();
                    }
                    public void submitReport( ReportMap report ) {
                    }
                };
            }
        };
    }
}
