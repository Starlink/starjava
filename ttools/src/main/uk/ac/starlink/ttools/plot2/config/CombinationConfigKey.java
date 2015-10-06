package uk.ac.starlink.ttools.plot2.config;

import java.util.Arrays;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import uk.ac.starlink.ttools.plot2.ReportMap;

/**
 * Config key that can select zero or more items from a short fixed list.
 * The string representation is an unordered list of the first letters
 * (lowercased) of each of the given option labels.
 * So the labels had better have different initial letters.
 *
 * <p>Typically this is used for axes.
 *
 * @author   Mark Taylor
 * @since    5 Oct 2013
 */
public class CombinationConfigKey extends ConfigKey<boolean[]> {

    private final int nopt_;
    private final String[] optNames_;
    private final String nullLabel_;
    private final boolean nullPermitted_;

    /**
     * Constructs an instance with a specified default.
     *
     * @param   meta  metadata
     * @param   dflt  default array of selection flags
     * @param   optNames  labels for each of the options that may be selected,
     *                    same length as <code>dflt</code>
     * @param   nullLabel  label for a specifer option indicating the null
     *                     value; null is a permitted value only if this
     *                     parameter is non-null
     */
    public CombinationConfigKey( ConfigMeta meta, boolean[] dflt,
                                 String[] optNames, String nullLabel ) {
        super( meta, boolean[].class, dflt );
        optNames_ = optNames;
        nullLabel_ = nullLabel;
        nullPermitted_ = nullLabel != null;
        nopt_ = optNames.length;
        if ( dflt == null ) {
            if ( ! nullPermitted_ ) {
                throw new NullPointerException();
            }
        }
        else {
            if ( dflt.length != nopt_ ) {
                throw new IllegalArgumentException( "Array length mismatch" );
            }
        }
    }

    /**
     * Constructs an instance where all the default inclusion flags are true
     * and a null value is not permitted.
     *
     * @param   meta  metadata
     * @param   optNames  labels for each of the options that may be selected,
     */
    public CombinationConfigKey( ConfigMeta meta, String[] optNames ) {
        this( meta, createTrueArray( optNames.length ), optNames, null );
    }

    public boolean[] stringToValue( String txt ) throws ConfigException {
        if ( nullPermitted_ && ( txt == null || txt.trim().length() == 0 ) ) {
            return null;
        }
        else {
            boolean[] value = new boolean[ nopt_ ];
            for ( int ic = 0; ic < txt.length(); ic++ ) {
                value[ optCharToIndex( txt.charAt( ic ) ) ] = true;
            }
            return value;
        }
    }

    public String valueToString( boolean[] opts ) {
        StringBuffer sbuf = new StringBuffer();
        if ( opts != null ) {
            for ( int io = 0; io < nopt_; io++ ) {
                if ( opts[ io ] ) {
                    sbuf.append( optIndexToChar( io ) );
                }
            }
        }
        return sbuf.toString();
    }

    public Specifier<boolean[]> createSpecifier() {
        Specifier<boolean[]> basicSpecifier = new CheckBoxesSpecifier();
        return nullLabel_ == null
             ? basicSpecifier
             : new ToggleSpecifier<boolean[]>( basicSpecifier, null,
                                               nullLabel_ );
    }

    /**
     * Gets the option index from an initial character.
     *
     * @param   c  label character, case unimportant
     * @return  option index
     * @throws  ConfigException  if no index is indicated (unknown letter)
     */
    public int optCharToIndex( char c ) throws ConfigException {
        char lc = Character.toLowerCase( c );
        for ( int io = 0; io < nopt_; io++ ) {
            if ( lc == optIndexToChar( io ) ) {
                return io;
            }
        }
        StringBuffer sbuf = new StringBuffer()
            .append( "Unknown option letter '" )
            .append( c )
            .append( "'; expecting one of " );;
        for ( int io = 0; io < nopt_; io++ ) {
            if ( io > 0 ) {
                sbuf.append( ", " );
            }
            sbuf.append( "'" )
                .append( optIndexToChar( io ) )
                .append( "'" );
        }
        throw new ConfigException( this, sbuf.toString() );
    }

    /**
     * Gets the initial letter from the option index.
     *
     * @param   io  option index
     * @return  lowercased initial letter
     */
    public char optIndexToChar( int io ) {
        return Character.toLowerCase( optNames_[ io ].charAt( 0 ) );
    }

    /**
     * Utility method to return a fixed-length boolean array with all
     * true elements.
     *
     * @param   n  element count
     * @return   array of <code>n</code> true elements
     */
    private static boolean[] createTrueArray( int n ) {
        boolean[] a = new boolean[ n ];
        Arrays.fill( a, true );
        return a;
    }

    /**
     * Specifier implementation for this key.
     * It uses a row of check boxes, so there had better not be too many
     * options.
     */
    private class CheckBoxesSpecifier extends SpecifierPanel<boolean[]> {

        private final JCheckBox[] checkBoxes_;

        /**
         * Constructor.
         */
        CheckBoxesSpecifier() {
            super( false );
            checkBoxes_ = new JCheckBox[ nopt_ ];
            boolean[] dflt = getDefaultValue();
            for ( int io = 0; io < nopt_; io++ ) {
                JCheckBox checkBox =
                    new JCheckBox( optNames_[ io ],
                                   dflt == null ? true : dflt[ io ] );
                checkBox.addActionListener( getActionForwarder() );
                checkBoxes_[ io ] = checkBox;
            }
        }

        public JComponent createComponent() {
            JComponent box = new Box( BoxLayout.X_AXIS ) {
                @Override
                public void setEnabled( boolean enabled ) {
                    super.setEnabled( enabled );
                    for ( int io = 0; io < nopt_; io++ ) {
                        checkBoxes_[ io ].setEnabled( enabled );
                    }
                }
            };
            for ( int io = 0; io < nopt_; io++ ) {
                if ( io > 0 ) {
                    box.add( Box.createHorizontalStrut( 10 ) );
                }
                box.add( checkBoxes_[ io ] );
            }
            return box;
        }

        public boolean[] getSpecifiedValue() {
            boolean[] flags = new boolean[ nopt_ ];
            for ( int io = 0; io < nopt_; io++ ) {
                flags[ io ] = checkBoxes_[ io ].isSelected();
            }
            return flags;
        }

        public void setSpecifiedValue( boolean[] flags ) {
            boolean change = false;
            for ( int io = 0; io < nopt_; io++ ) {
                JCheckBox checkBox = checkBoxes_[ io ];
                change = change || ( flags[ io ] ^ checkBox.isSelected() );
                checkBox.setSelected( flags[ io ] );
            }
            if ( change ) {
                fireAction();
            }
        }

        public void submitReport( ReportMap report ) {
        }
    }
}
