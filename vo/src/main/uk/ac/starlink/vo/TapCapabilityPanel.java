package uk.ac.starlink.vo;

import java.awt.BorderLayout;
import java.util.Arrays;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
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
    private final JTextField uploadField_;
    private final JComboBox maxrecSelector_;

    /**
     * Constructor.
     */
    public TapCapabilityPanel() {
        super( new BorderLayout() );
        langSelector_ = new JComboBox();
        langSelector_.setToolTipText( "Selects which supported query "
                                    + "language/version to use" );
        uploadField_ = new JTextField();
        uploadField_.setEditable( false );
        uploadField_.setToolTipText( "Indicates whether the service supports "
                                   + "table uploads and if so "
                                   + "what limits apply" );
        maxrecSelector_ = new JComboBox();
        maxrecSelector_.setEditable( true );
        maxrecSelector_.setToolTipText( "Indicates and allows to set MAXREC, "
                                      + "the maximum row count for result "
                                      + "tables" );
        JComponent line = Box.createHorizontalBox();
        line.add( createJLabel( "Query Language: ", langSelector_ ) );
        line.add( new ShrinkWrapper( langSelector_ ) );
        line.add( Box.createHorizontalStrut( 10 ) );
        line.add( createJLabel( "Max Rows: ", maxrecSelector_ ) );
        line.add( new ShrinkWrapper( maxrecSelector_ ) );
        line.add( Box.createHorizontalStrut( 10 ) );
        line.add( createJLabel( "Uploads: ", uploadField_ ) );
        line.add( uploadField_ );
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
            uploadField_.setText( null );
            langSelector_.setEnabled( false );
            maxrecModel = new DefaultComboBoxModel( new String[ 1 ] );
        }

        /* Capability object exists, but looks like it is very sparsely
         * populated (missing mandatory elements). */
        else if ( capability.getLanguages().length == 0 ) {
            langSelector_
               .setModel( new DefaultComboBoxModel( new String[] { "ADQL" } ) );
            langSelector_.setSelectedIndex( 0 );
            uploadField_.setText( null );
            langSelector_.setEnabled( false );
            maxrecModel = new DefaultComboBoxModel( new String[ 1 ] );
        }

        /* Apparently healthy capability object. */
        else {
            String[] langs = capability.getLanguages();
            langSelector_.setModel( new DefaultComboBoxModel( langs ) );
            langSelector_.setSelectedItem( getDefaultLanguage( langs ) );
            langSelector_.setEnabled( true );

            if ( canUpload( capability ) ) {
                StringBuffer sbuf = new StringBuffer();
                sbuf.append( getUploadLimitString( TapLimit.ROWS ) )
                    .append( sbuf.length() > 0 ? "/" : "" )
                    .append( getUploadLimitString( TapLimit.BYTES ) );
                String limitString = sbuf.toString();
                uploadField_.setText( limitString.length() > 0 ? limitString
                                                               : "available" );
            }
            else {
                uploadField_.setText( "unavailable" );
            }

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
     * @return  selected query language
     */
    public String getQueryLanguage() {
        Object lang = langSelector_.getSelectedItem();
        return lang instanceof String ? (String) lang : "ADQL";
    }

    /**
     * Returns the maximum record value selected in this panel.
     * If none has been explicitly selected, -1 is returned.
     *
     * @return   maxrec value, or -1
     */
    public long getMaxrec() {
        Object oMaxrec = maxrecSelector_.getSelectedItem();
        if ( oMaxrec instanceof TapLimit ) {
            TapLimit limit = (TapLimit) oMaxrec;
            return limit.isHard() ? limit.getValue()
                                  : -1;
        }
        else if ( oMaxrec instanceof String ) {
            try {
                return Long.parseLong( (String) oMaxrec );
            }
            catch ( NumberFormatException e ) {
                return -1;
            }
        }
        else {
            return -1;
        }
    }

    /**
     * Returns an upload limit for the currently displayed capability.
     * A particular unit is specified
     * (normally {@link TapLimit#ROWS} or {@link TapLimit#BYTES})
     * and the corresponding value is returned.
     * If no limit with the given unit has been specified
     * (including if no capability is currently displayed), -1 is returned.
     *
     * @param   units  limit unit string
     * @return   limit value, or -1
     */
    public long getUploadLimit( String units ) {
        TapLimit[] limits = capability_ == null
                          ? null
                          : capability_.getUploadLimits();
        if ( limits == null ) {
            return -1;
        }
        for ( int i = 0; i < limits.length; i++ ) {
            TapLimit limit = limits[ i ];
            if ( limit.isHard() && units.equals( limit.getUnit() ) ) {
                return limit.getValue();
            }
        }
        return -1;
    }

    /**
     * Returns a reasonably compact string indicating an upload limit for
     * the currently displayed capability; some indication of the unit
     * is included in the result.
     * A particular unit is specified
     * (normally {@link TapLimit#ROWS} or {@link TapLimit#BYTES})
     * and the corresponding value is returned.
     * If no limit with the given unit has been specified
     * (including if no capability is currently displayed),
     * an empty string is returned.
     *
     * @param   units  limit unit string
     * @return   limit string, or ""
     */
    private String getUploadLimitString( String units ) {
        long value = getUploadLimit( units );
        if ( value < 0 ) {
            return "";
        }
        else {
            int kilo = 1000;
            int mega = 1000 * kilo;
            int giga = 1000 * mega;
            final String snum;
            if ( value >= giga * 10 ) {
                snum = ( value / giga ) + "G";
            }
            else if ( value >= 1e7 ) {
                snum = ( value / mega ) + "M";
            }
            else if ( value >= 1e4 ) {
                snum = ( value / kilo ) + "k";
            }
            else {
                snum = Long.toString( value );
            }
            final String u;
            if ( TapLimit.ROWS.equals( units ) ) {
                u = "row";
            }
            else if ( TapLimit.BYTES.equals( units ) ) {
                u = "b";
            }
            else {
                u = "";
            }
            return snum + u;
        }
    }

    /**
     * Indicates whether the TAP load dialogue is capable of uploads
     * for a given capability object.
     *
     * @param   tcap  capability metadata
     * @return  true iff tcap permits inline uploads
     */
    public static boolean canUpload( TapCapability tcap ) {
        String[] upMethods = tcap == null ? null : tcap.getUploadMethods();
        return upMethods != null
            && Arrays.asList( upMethods )
                     .contains( TapCapability.UPLOADS_URI + "inline" );
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

    /**
     * Creates and returns a label associated with a given component.
     * Tool tip text is copied.
     *
     * @param  text  label text
     * @param  comp  associated component
     * @return   new JLabel
     */
    private static JLabel createJLabel( String text, JComponent comp ) {
        JLabel label = new JLabel( text );
        label.setToolTipText( comp.getToolTipText() );
        return label;
    }
}
