package uk.ac.starlink.vo;

import java.awt.BorderLayout;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
    private final JComboBox<VersionedLanguage> langSelector_;
    private final JTextField uploadField_;
    private final JComboBox<Object> maxrecSelector_;
    private VersionedLanguage lastLang_;

    /** Name of property associated with currently selected language. */
    public static final String LANGUAGE_PROPERTY = "language";

    private static final VersionedLanguage[] ADQLS =
        createDefaultVersionedLanguages();
    private static final VersionedLanguage ADQL =
        getDefaultLanguage( ADQLS );

    /**
     * Constructor.
     */
    public TapCapabilityPanel() {
        super( new BorderLayout() );
        langSelector_ = new JComboBox<>();
        langSelector_.setToolTipText( "Selects which supported query "
                                    + "language/version to use" );
        langSelector_.addActionListener( evt -> {
            int ix = langSelector_.getSelectedIndex();
            VersionedLanguage lang = ix > 0 ? langSelector_.getItemAt( ix )
                                            : null;
            firePropertyChange( LANGUAGE_PROPERTY, lastLang_, lang );
            lastLang_ = lang;
        } );
        uploadField_ = new JTextField();
        uploadField_.setEditable( false );
        uploadField_.setToolTipText( "Indicates whether the service supports "
                                   + "table uploads and if so "
                                   + "what limits apply" );
        maxrecSelector_ = new JComboBox<Object>();
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
        final DefaultComboBoxModel<Object> maxrecModel;

        /* No capability to display. */
        if ( capability == null ) {
            langSelector_.setModel(
                 new DefaultComboBoxModel<VersionedLanguage>() );
            uploadField_.setText( null );
            langSelector_.setEnabled( false );
            maxrecModel = new DefaultComboBoxModel<>( new String[ 1 ] );
        }

        /* Capability object exists, but looks like it is very sparsely
         * populated (missing mandatory elements). */
        else if ( capability.getLanguages().length == 0 ) {
            VersionedLanguage[] vlangs = ADQLS.clone();
            langSelector_.setModel( new DefaultComboBoxModel<VersionedLanguage>
                                                            ( vlangs ) );
            langSelector_.setSelectedItem( getDefaultLanguage( vlangs ) );
            uploadField_.setText( null );
            langSelector_.setEnabled( true );
            maxrecModel = new DefaultComboBoxModel<>( new String[ 1 ] );
        }

        /* Apparently healthy capability object. */
        else {
            VersionedLanguage[] vlangs = getVersionedLanguages( capability );
            langSelector_.setModel( new DefaultComboBoxModel<VersionedLanguage>
                                                            ( vlangs ) );
            langSelector_.setSelectedItem( getDefaultLanguage( vlangs ) );
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
            maxrecModel = new DefaultComboBoxModel<>();
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
     * Returns the currently selected VersionedLanguage.
     * If none is selected, one representing a suitable version of ADQL
     * will be returned.
     *
     * @return  selected language, not null
     */
    public VersionedLanguage getSelectedLanguage() {
        VersionedLanguage selected =
            langSelector_.getItemAt( langSelector_.getSelectedIndex() );
        return selected == null ? ADQL : selected;
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
              .contains( TapCapability.TAPREGEXT_STD_URI + "#upload-inline" );
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

    /**
     * Returns the default query language to use given a list of possibles.
     *
     * @param  langs  query language options
     * @return   favoured option
     */
    static VersionedLanguage
            getDefaultLanguage( VersionedLanguage[] vlangs ) {
        for ( AdqlVersion adqlVers :
              new AdqlVersion[] { AdqlVersion.V21, AdqlVersion.V20 } ) {
            for ( VersionedLanguage vlang : vlangs ) { 
                 if ( adqlVers.equals( vlang.getAdqlVersion() ) ) {
                     return vlang;
                 }
            }
        }
        return vlangs.length > 0 ? vlangs[ 0 ] : ADQL;
    }

    /**
     * Returns an array of language-version specifiers for languages
     * supported by a given TapCapability object.
     *
     * @param   tcap  capability object
     * @return   array of language identifiers
     */
    private static VersionedLanguage[]
            getVersionedLanguages( TapCapability tcap ) {
        List<VersionedLanguage> vlangList = new ArrayList<VersionedLanguage>();
        for ( TapLanguage lang : tcap.getLanguages() ) {
            for ( String vers : lang.getVersions() ) {
                vlangList.add( new VersionedLanguage( lang, vers ) );
            }
        }
        return vlangList.toArray( new VersionedLanguage[ 0 ] );
    }

    /**
     * Returns an array of standard VersionedLanguage instances.
     * This currently corresponds to the supported versions of ADQL,
     * which is mandatory for TAP.
     *
     * @return  default language instance array
     */
    static VersionedLanguage[] createDefaultVersionedLanguages() {
        AdqlVersion[] versions = { AdqlVersion.V20, AdqlVersion.V21 };
        TapLanguage lang = new TapLanguage() {
            public String getName() {
                return "ADQL";
            }
            public String getDescription() {
                return "Astronomical Data Query Language";
            }
            public String[] getVersionIds() {
                return Arrays.stream( versions )
                             .map( AdqlVersion::getIvoid )
                             .collect( Collectors.toList() )
                             .toArray( new String[ 0 ] );
            }
            public String[] getVersions() {
                return Arrays.stream( versions )
                             .map( AdqlVersion::getNumber )
                             .collect( Collectors.toList() )
                             .toArray( new String[ 0 ] );
            }
            public Map<String,TapLanguageFeature[]> getFeaturesMap() {
                return new HashMap<String,TapLanguageFeature[]>();
            }
        };
        return Arrays.stream( versions )
                     .map( v -> new VersionedLanguage( lang, v.getNumber() ) )
                     .collect( Collectors.toList() )
                     .toArray( new VersionedLanguage[ 0 ] );
    }
}
