package uk.ac.starlink.vo;

import java.awt.BorderLayout;
import java.util.Arrays;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * Panel displaying the capability information retrieved from a TAP service.
 *
 * @author   Mark Taylor
 * @since    8 Mar 2011
 */
public class TapCapabilityPanel extends JPanel {

    private TapCapability capability_;
    private final JComboBox langSelector_;
    private final JCheckBox uploadFlagger_;
    private final JComboBox maxrecSelector_;

    /**
     * Constructor.
     */
    public TapCapabilityPanel() {
        super( new BorderLayout() );
        langSelector_ = new JComboBox();
        uploadFlagger_ = new JCheckBox();
        uploadFlagger_.setEnabled( false );
        maxrecSelector_ = new JComboBox();
        maxrecSelector_.setEditable( true );
        JComponent line = Box.createHorizontalBox();
        line.add( new JLabel( "Query Language: " ) );
        line.add( new ShrinkWrapper( langSelector_ ) );
        line.add( Box.createHorizontalStrut( 10 ) );
        line.add( new JLabel( "Max Rows: " ) );
        line.add( new ShrinkWrapper( maxrecSelector_ ) );
        line.add( Box.createHorizontalStrut( 10 ) );
        line.add( new JLabel( "Uploads: " ) );
        line.add( uploadFlagger_ );
        line.add( Box.createHorizontalGlue() );
        add( line, BorderLayout.NORTH );
        setCapability( null );
    }

    /**
     * Configures this panel to display a given capability object.
     *
     * @param  capability  capability object; may be null
     */
    public void setCapability( TapCapability capability ) {
        capability_ = capability;
        final DefaultComboBoxModel maxrecModel;

        /* No capability to display. */
        if ( capability == null ) {
            langSelector_.setModel( new DefaultComboBoxModel() );
            uploadFlagger_.setSelected( false );
            langSelector_.setEnabled( false );
            maxrecModel = new DefaultComboBoxModel( new String[ 1 ] );
        }

        /* Capability object exists, but looks like it is very sparsely
         * populated (missing mandatory elements). */
        else if ( capability.getLanguages().length == 0 ) {
            langSelector_
               .setModel( new DefaultComboBoxModel( new String[] { "ADQL" } ) );
            langSelector_.setSelectedIndex( 0 );
            uploadFlagger_.setSelected( false );
            langSelector_.setEnabled( false );
            maxrecModel = new DefaultComboBoxModel( new String[ 1 ] );
        }

        /* Apparently healthy capability object. */
        else {
            String[] langs = capability.getLanguages();
            langSelector_.setModel( new DefaultComboBoxModel( langs ) );
            langSelector_.setSelectedItem( getDefaultLanguage( langs ) );
            langSelector_.setEnabled( true );

            boolean canUpload =
                Arrays.asList( capability.getUploadMethods() )
               .indexOf( TapCapability.UPLOADS_URI + "#inline" ) >= 0;
            uploadFlagger_.setSelected( canUpload );

            TapLimit[] outLimits = capability.getOutputLimits();
            maxrecModel = new DefaultComboBoxModel();
            maxrecModel.addElement( "" );
            for ( int il = 0; il < outLimits.length; il++ ) {
                final TapLimit limit = outLimits[ il ];
                if ( TapLimit.ROWS.equals( limit.getUnit() ) ) {
                    final String slimit =
                        limit.getValue()
                        + " (" + ( limit.isHard() ? "max" : "default" ) + ")";
                    TapLimit tlimit = new TapLimit( limit.getValue(),
                                                    limit.isHard(),
                                                    limit.getUnit() ) {
                        public String toString() {
                            return slimit;
                        }
                    };
                    maxrecModel.addElement( tlimit );
                    if ( ! tlimit.isHard() ) {
                        maxrecModel.setSelectedItem( tlimit );
                    }
                }
            }
        }
        maxrecSelector_.setModel( maxrecModel );
    }

    /**
     * Returns the capability object currently displayed by this panel.
     *
     * @return    current capability
     */
    public TapCapability getCapability() {
        return capability_;
    }

    /**
     * Returns the query language currently selected in this panel.
     * If none has been explicitly selected, "ADQL" will be returned.
     *
     * @return  selected query languagte
     */
    public String getQueryLanguage() {
        Object lang = langSelector_.getSelectedItem();
        return lang instanceof String ? (String) lang : "ADQL";
    }

    /**
     * Returns the maximum record value selected in this panel.
     * If none has been explicitly selected, null is returned.
     *
     * @return   maxrec value, or null
     */
    public Long getMaxrec() {
        Object oMaxrec = maxrecSelector_.getSelectedItem();
        if ( oMaxrec instanceof TapLimit ) {
            TapLimit limit = (TapLimit) oMaxrec;
            return limit.isHard() ? new Long( limit.getValue() )
                                  : null;
        }
        else if ( oMaxrec instanceof String ) {
            try {
                return Long.parseLong( (String) oMaxrec );
            }
            catch ( NumberFormatException e ) {
                return null;
            }
        }
        else {
            return null;
        }
    }

    /**
     * Returns the default query language to use given a list of possibles.
     *
     * @param  langs  query language options
     * @return   favoured option
     */
    private static String getDefaultLanguage( String[] langs ) {
        for ( int i = 0; i < langs.length; i++ ) {
            if ( langs[ i ].equalsIgnoreCase( "adql-2.0" ) ) {
                return langs[ i ];
            }
        }
        for ( int i = 0; i < langs.length; i++ ) {
            if ( langs[ i ].toLowerCase().startsWith( "adql" ) ) {
                return langs[ i ];
            }
        }
        return langs.length > 0 ? langs[ 0 ] : "ADQL";
    }
}
