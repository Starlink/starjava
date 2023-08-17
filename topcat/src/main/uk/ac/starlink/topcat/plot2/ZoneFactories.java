package uk.ac.starlink.topcat.plot2;

import java.util.Arrays;
import java.util.Comparator;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import uk.ac.starlink.ttools.plot2.Ganger;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.config.Specifier;
import uk.ac.starlink.ttools.plot2.config.SpecifierPanel;
import uk.ac.starlink.ttools.plot2.geom.StackGanger;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * Utility class containing zone factory implementations.
 *
 * @author   Mark Taylor
 * @since    5 Feb 2016
 */
public class ZoneFactories {

    /** Single-zone factory. */
    public static final ZoneFactory FIXED =
        createSingleZoneFactory( new ZoneId() {
            public int getZoneIndex( Ganger<?,?> ganger ) {
                return 0;
            }
            public String toString() {
                return "FIXED";
            }
        } );

    /**
     * Private constructor prevents instantiation.
     */
    private ZoneFactories() {
    }

    /**
     * Returns a factory suitable for single-zone use.
     *
     * @param    zid0  sole zone ID
     * @return    fixed zone factory
     */
    public static ZoneFactory createSingleZoneFactory( final ZoneId zid0 ) {
        final Comparator<ZoneId> comparator = new Comparator<ZoneId>() {
            public int compare( ZoneId zid1, ZoneId zid2 ) {
                return zid1.toString().compareTo( zid2.toString() );
            }
        };
        return new AbstractZoneFactory( true, zid0, comparator ) {
            public ZoneId nameToId( String name ) {
                return zid0.toString().equals( name ) ? zid0 : null;
            }
            public Specifier<ZoneId> createZoneSpecifier() {
                return new SpecifierPanel<ZoneId>( false ) {
                    protected JComponent createComponent() {
                        return new JLabel( zid0.toString() );
                    }
                    public ZoneId getSpecifiedValue() {
                        return zid0;
                    }
                    public void setSpecifiedValue( ZoneId z ) {
                        throw new UnsupportedOperationException();
                    }
                    public void submitReport( ReportMap report ) {
                    }
                };
            }
        };
    }

    /**
     * Returns a factory that works with integer-based zone ids.
     * Optionally, the default value increments for each subsequent
     * call of getItem.
     *
     * @param  autoIncrement  true to force increment of default zone id
     *                        for each specifier in sequence
     * @return   factory dispensing integer-based zone ids
     */
    public static ZoneFactory
            createIntegerZoneFactory( final boolean autoIncrement ) {
        Comparator<ZoneId> comparator = new Comparator<ZoneId>() {
            public int compare( ZoneId zid1, ZoneId zid2 ) {
                return ((IntZoneId) zid1).ival_ - ((IntZoneId) zid2).ival_;
            }
        };
        return new AbstractZoneFactory( false, new IntZoneId( 0 ),
                                        comparator ) {
            private int index_;
            public ZoneId nameToId( String name ) {
                return IntZoneId.fromString( name );
            }
            public Specifier<ZoneId> createZoneSpecifier() {
                return new SpecifierPanel<ZoneId>( false ) {
                    private final SpinnerNumberModel model_ =
                        new SpinnerNumberModel( 0, Integer.MIN_VALUE,
                                                Integer.MAX_VALUE, 1 );
                    protected JComponent createComponent() {
                        if ( autoIncrement ) {
                            index_++;
                        }
                        model_.setValue( new Integer( index_ ) );
                        JComponent box = Box.createHorizontalBox();
                        box.add( new ShrinkWrapper( new JSpinner( model_ ) ) );
                        model_.addChangeListener( getChangeForwarder() );
                        return box;
                    }
                    public ZoneId getSpecifiedValue() {
                        return new IntZoneId( model_.getNumber().intValue() );
                    }
                    public void setSpecifiedValue( ZoneId zid ) {
                        model_.setValue( new Integer( ((IntZoneId) zid)
                                                     .ival_ ) );
                    }
                    public void submitReport( ReportMap report ) {
                    }
                };
            }
        };
    }

    /**
     * ZoneId implementation based on integer values.
     */
    private static class IntZoneId implements ZoneId {
        final int ival_;

        /**
         * Constructor.
         *
         * @param  ival  identity
         */
        IntZoneId( int ival ) {
            ival_ = ival;
        }

        public int getZoneIndex( Ganger<?,?> ganger ) {
            if ( ganger instanceof StackGanger ) {
                String[] zoneNames = ((StackGanger) ganger).getZoneNames();
                int izone = Arrays.asList( zoneNames ).indexOf( toString() );
                if ( izone >= 0 ) {
                    return izone;
                }
            }

            /* This shouldn't be necessary, but it seems that it is
             * before the plot is properly started. */
            return ival_ >= 0 & ival_ < ganger.getZoneCount()
                 ? ival_
                 : 0;
        }

        @Override
        public int hashCode() {
            return ival_;
        }

        @Override
        public boolean equals( Object o ) {
            return o instanceof IntZoneId
                && ((IntZoneId) o).ival_ == this.ival_;
        }

        @Override
        public String toString() {
            return Integer.toString( ival_ );
        }

        /**
         * Returns an IntZoneId corresponding to the given string.
         *
         * @param  txt  zone name
         * @return  zoneId
         */
        static IntZoneId fromString( String txt ) {
            try {
                return new IntZoneId( Integer.parseInt( txt ) );
            }
            catch ( RuntimeException e ) {
                return null;
            }
        }
    }

    /**
     * Utility class providing a partial implemntatio of ZoneFactory.
     */
    private static abstract class AbstractZoneFactory implements ZoneFactory {
        private final boolean isSingle_;
        private final ZoneId dfltZid_;
        private final Comparator<ZoneId> comparator_;

        /**
         * Constructor.
         *
         * @param  isSingle  true for single-zone factory
         * @param  dfltZid   default zone id
         * @param  comparator  comparator
         */
        AbstractZoneFactory( boolean isSingle, ZoneId dfltZid,
                             Comparator<ZoneId> comparator ) {
            isSingle_ = isSingle;
            dfltZid_ = dfltZid;
            comparator_ = comparator;
        }
        public boolean isSingleZone() {
            return isSingle_;
        }
        public ZoneId getDefaultZone() {
            return dfltZid_;
        }
        public Comparator<ZoneId> getComparator() {
            return comparator_;
        }
    }
}
