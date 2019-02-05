package uk.ac.starlink.table;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Defines how to store metadata in a table so that STIL knows it
 * contains a HEALPix map.
 *
 * @author   Mark Taylor
 */
public class HealpixTableInfo {

    private final int level_;
    private final boolean isNest_;
    private final String ipixColName_;
    private final HpxCoordSys csys_;

    /**
     * Metadata element for HEALPix level (=log2(nside)).
     * Name "<code>STIL_HPX_LEVEL</code>", class Integer.
     */
    public static final ValueInfo HPX_LEVEL_INFO =
        new DefaultValueInfo( "STIL_HPX_LEVEL", Integer.class,
                              "Level of HEALPix pixels contained in the table"
                            + " (nside=2^level)" );

    /**
     * Metadata element * for HEALPix ordering (true=NESTED, false=RING).
     * Name "<code>STIL_HPX_ISNEST</code>", class Boolean.
     */
    public static final ValueInfo HPX_ISNEST_INFO =
        new DefaultValueInfo( "STIL_HPX_ISNEST", Boolean.class,
                              "True for NEST indexation scheme, "
                            + "False for RING" );

    /**
     * Metadata element for name of column storing pixel index.
     * If blank, indexing is implicit (determined by row index).
     * Name "<code>STIL_HPX_COLNAME</code>", class String.
     */
    public static final ValueInfo HPX_COLNAME_INFO =
        new DefaultValueInfo( "STIL_HPX_COLNAME", String.class,
                              "Name of the table column containing "
                            + "HEALPix index; null value or empty string "
                            + "indicates implicit" );

    /**
     * Metadata element for character indicating sky system: C, G or E.
     * Name "<code>STIL_HPX_CSYS</code>", class String.
     */
    public static final ValueInfo HPX_CSYS_INFO =
        new DefaultValueInfo( "STIL_HPX_CSYS", String.class,
                              "'C'=celestial/equatorial, 'G'=galactic, "
                            + "'E'=ecliptic" );

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.table" );

    /**
     * Constructor.
     *
     * @param  level  healpix level; negative means not defined
     * @param  isNest  true for nested, false for ring
     * @param  ipixColName  name of column containing pixel index,
     *                      or null for implicit pixel indices
     * @param  csys   healpix coordinate system variant
     */
    public HealpixTableInfo( int level, boolean isNest, String ipixColName,
                             HpxCoordSys csys ) {
        level_ = level;
        isNest_ = isNest;
        ipixColName_ = ipixColName;
        csys_ = csys;
    }

    /**
     * Returns the HEALPix level.
     *
     * @return  log2(nside), or negative value if not defined
     */
    public int getLevel() {
        return level_;
    }

    /**
     * Indicates pixel ordering scheme.
     *
     * @return  true for NESTED, false for RING
     */
    public boolean isNest() {
        return isNest_;
    }

    /**
     * Returns the name of the table column containing the HEALPix pixel index.
     * If blank, pixel index is assumed equal to row index.
     *
     * @return   pixel column name, or null
     */
    public String getPixelColumnName() {
        return ipixColName_;
    }

    /**
     * Returns the HEALPix coordinate system variant used by this table.
     * May be null if none specified.
     *
     * @return   coordinate system object, or null
     */
    public HpxCoordSys getCoordSys() {
        return csys_;
    }

    /**
     * Exports the contents of this object to a list of DescribedValue
     * objects that can be attached to a table's parameter list,
     * to declare the organisation of HEALPix information in that table.
     *
     * @return   list of table parameters
     */
    public DescribedValue[] toParams() {
        List<DescribedValue> dvals = new ArrayList<DescribedValue>();
        dvals.add( new DescribedValue( HPX_LEVEL_INFO,
                                       Integer.valueOf( level_ ) ) );
        dvals.add( new DescribedValue( HPX_ISNEST_INFO,
                                       Boolean.valueOf( isNest_ ) ) );
        dvals.add( new DescribedValue( HPX_COLNAME_INFO, ipixColName_ ) );
        if ( csys_ != null ) {
            dvals.add( new DescribedValue( HPX_CSYS_INFO,
                                           csys_.getCharString() ) );
        }
        return dvals.toArray( new DescribedValue[ 0 ] );
    }

    @Override
    public int hashCode() {
        int code = -3351;
        code = 23 * code + level_;
        code = 23 * code + ( isNest_ ? 11 : 19 );
        code = 23 * code + ( ipixColName_ == null ? 0
                                                  : ipixColName_.hashCode() );
        code = 23 * code + ( csys_ == null ? ' ' : csys_.character_ );
        return code;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof HealpixTableInfo ) {
            HealpixTableInfo other = (HealpixTableInfo) o;
            return this.level_ == other.level_
                && this.isNest_ == other.isNest_
                && ( this.ipixColName_ == null
                         ? other.ipixColName_ == null
                         : this.ipixColName_.equals( other.ipixColName_ ) )
                && ( this.csys_ == null ? other.csys_ == null
                                        : this.csys_ == other.csys_ );
        }
        else {
            return false;
        }
    }

    @Override
    public String toString() {
        return new StringBuffer()
            .append( "HEALPix(" )
            .append( "level=" )
            .append( level_ )
            .append( "," )
            .append( "order=" )
            .append( isNest_ ? "nest" : "ring" )
            .append( "," )
            .append( "pixcol=" )
            .append( ipixColName_ == null ? "<implicit>" : ipixColName_ )
            .append( "," )
            .append( "csys=" )
            .append( csys_ == null ? "null" : csys_.getCharString() )
            .append( ")" )
            .toString();
    }

    /**
     * Indicates whether a list of table parameters appears to be
     * from a table with HEALPix annotations as expected by this class.
     * This method currently just looks to see whether any of the
     * <code>HPX_*</code> ValueInfos appears in the list, and returns
     * true if so.
     *
     * <p>This method may be useful to determine whether it's worth while
     * to call {@link #fromParams}.
     *
     * @param   params   list of DescribedValue objects,
     *                   as obtained from Table.getParameters
     * @return  true if the table appears to be a healpix table
     */
    public static boolean isHealpix( List<DescribedValue> params ) {
        for ( DescribedValue param : params ) {
            String pname = param.getInfo().getName();
            if ( pname.equals( HPX_LEVEL_INFO.getName() ) ||
                 pname.equals( HPX_ISNEST_INFO.getName() ) ||
                 pname.equals( HPX_COLNAME_INFO.getName() ) ||
                 pname.equals( HPX_CSYS_INFO.getName() ) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Imports HEALPix information from a list of table parameters,
     * and turns it into an instance of this class.
     * This should always succeed, but the returned instance is
     * not guaranteed to have very complete information.
     * If parameters that this class knows about seem to have wrong
     * or surprising values, messages may be reported
     * through the logging system.
     *
     * @param   params   list of DescribedValue objects,
     *                   as obtained from Table.getParameters
     * @return   an instance of this class
     */
    public static HealpixTableInfo fromParams( List<DescribedValue> params ) {

        /* Extract information from parameters. */
        int level = -1;
        Boolean isNest = null;
        String ipixColName = null;
        Character csysChar = null;
        for ( DescribedValue dval : params ) {
            ValueInfo info = dval.getInfo();
            String pname = info.getName();
            Object value = dval.getValue();
            if ( pname.equals( HPX_LEVEL_INFO.getName() ) ) {
                if ( value instanceof Number ) {
                    level = ((Number) value).intValue();
                }
                else {
                    logger_.warning( "Wrong type for " + dval );
                }
            }
            if ( pname.equals( HPX_COLNAME_INFO.getName() ) ) {
                if ( value == null ) {
                    ipixColName = "";
                }
                else if ( value instanceof String ) {
                    ipixColName = (String) value;
                }
                else {
                    logger_.warning( "Wrong type for " + dval );
                }
            }
            if ( pname.equals( HPX_ISNEST_INFO.getName() ) ) {
                if ( value instanceof Boolean ) {
                    isNest = (Boolean) value;
                }
                else {
                    logger_.warning( "Wrong type for " + dval );
                }
            }
            if ( pname.equals( HPX_CSYS_INFO.getName() ) ) {
                if ( value instanceof Character ) {
                    csysChar = (Character) value;
                }
                else if ( value instanceof String &&
                          ((String) value).length() == 1 ) {
                    csysChar = Character
                              .valueOf( ((String) value).charAt( 0 ) );
                }
                else {
                    logger_.warning( "Wrong type for " + dval );
                }
            }
        }
    
        /* Validate values and emit logging message as applicable. */
        if ( ipixColName == null ) {
            logger_.warning( "Missing HEALPix index column name parameter "
                           + HPX_COLNAME_INFO + " - assume implicit" );
        }
        else if ( ipixColName.length() == 0 ) {
            ipixColName = null;
        }
        if ( isNest == null ) {
            logger_.warning( "Missing HEALPix ordering parameter "
                           + HPX_ISNEST_INFO + " - assume NEST" );
            isNest = Boolean.TRUE;
        }
        HpxCoordSys csys = null;
        if ( csysChar != null ) {
            csys = HpxCoordSys.fromCharacter( csysChar.charValue() );
            if ( csys == null ) {
                logger_.warning( "Ignoring unknown HEALPix COORDSYS value "
                               + "'" + csysChar + "'" );
            }
        }

        /* Turn the extracted values into a HealpixTableInfo. */
        return new HealpixTableInfo( level, isNest.booleanValue(), ipixColName,
                                     csys );
    }
    
    /**
     * Characterises the coordinate systems defined by the HEALpix-FITS
     * serialization convention.  These are the values permitted for the
     * HEALPix-FITS COORDSYS keyword.
     *
     * @see <a
     *href="https://healpix.sourceforge.io/data/examples/healpix_fits_specs.pdf"
     * >HEALPix-FITS convention</a>
     */
    public enum HpxCoordSys {

        /** Galactic. */
        GALACTIC( 'G', "Galactic" ),

        /** Ecliptic. */
        ECLIPTIC( 'E', "Ecliptic" ),

        /** Equatorial, also called celestial. */
        CELESTIAL( 'C', "Celestial/Equatorial" );

        private final char character_;
        private final String word_;

        /**
         * Constructor.
         *
         * @param  character   character used in FITS serialization
         * @param  word     short description of this system
         */
        HpxCoordSys( char character, String word ) {
            character_ = character;
            word_ = word;
        }

        /**
         * Returns the 1-character string used to label this system
         * in the FITS serialization.
         *
         * @return  1-character string
         */
        public String getCharString() {
            return new String( new char[] { character_ } );
        }

        /**
         * Returns a human-readable short description of this system.
         *
         * @return  description
         */
        public String getWord() {
            return word_;
        }

        /**
         * Returns the instance of this class corresponding to a character
         * label (as used in the FITS serialization).
         * Null is returned if the character is not known.
         *
         * @param  c  coordinate system label
         * @return  coordinate system object, or null
         */
        public static HpxCoordSys fromCharacter( char c ) {
            for ( HpxCoordSys csys : values() ) {
                if ( csys.character_ == c ) { 
                    return csys;
                }   
            }
            return null;
        }
    }
}
