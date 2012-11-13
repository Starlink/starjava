package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.WrapperRowSequence;
import uk.ac.starlink.table.WrapperStarTable;

/**
 * Wrapper table which makes replacements of named values with other 
 * named values in some of its columns.
 *
 * @author   Mark Taylor
 * @since    20 Sep 2005
 */
public class ReplaceValueTable extends WrapperStarTable {

    private final static Replacer unitReplacer_ = new Replacer( true ) {
        public Object replaceValue( Object obj ) {
            return obj;
        }
    };
    private final static double FLOAT_TOL = Float.MIN_VALUE * 2.0;
    private final static double DOUBLE_TOL = Double.MIN_VALUE * 2.0;
    private final static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.filter" );

    private final Replacer[] replacers_;

    /**
     * Constructs a new ReplaceValueTable with the same replacement
     * taking place in zero or more of the base table's columns, 
     * as described by an array of flags.
     *
     * @param  baseTable  base table
     * @param  colFlags  array of flags for each column of the table,
     *         true only for those columns which should be modified
     * @param  oldStr   value to be replaced
     * @param  newStr   replacement value
     */
    public ReplaceValueTable( StarTable baseTable, boolean[] colFlags,
                              String oldStr, String newStr )
            throws IOException {
        super( baseTable );
        int ncol = baseTable.getColumnCount();
        replacers_ = new Replacer[ ncol ];
        for ( int i = 0; i < ncol; i++ ) {
            replacers_[ i ] = colFlags[ i ]
                            ? createReplacer( baseTable.getColumnInfo( i ),
                                              oldStr, newStr )
                            : unitReplacer_;
        }
    }

    /**
     * Constructs a new ReplaceValueTable from parallel arrays describing
     * the columns to change and the old and new values.
     * The additional arguments are a set of parallel arrays, with an
     * element for each of the replacements which will happen.
     * Each of the arrays <code>icols</code>, <code>oldStrs</code> and
     * <code>newStrs</code> must have the same number of elements.
     * Indices in <code>icols</code> ought not to be repeated.
     *
     * @param   baseTable  base table
     * @param   icols   array of column indices in which replacements
     *                  will occur
     * @param   oldStrs  array of strings to be replaced,
     *                   one for each of the columns in <code>icols</code>
     * @param   newStrs  array of strings to furnish replacement values,
     *                   one for each of the columns in <code>icols</code>
     */
    public ReplaceValueTable( StarTable baseTable, int[] icols, 
                              String[] oldStrs, String[] newStrs ) 
            throws IOException {
        super( baseTable );
        int ncol = baseTable.getColumnCount();
        replacers_ = new Replacer[ ncol ];
        Arrays.fill( replacers_, unitReplacer_ );
        for ( int i = 0; i < icols.length; i++ ) {
            int icol = icols[ i ];
            replacers_[ icol ] =
                createReplacer( baseTable.getColumnInfo( icol ),
                                oldStrs[ i ], newStrs[ i ] );
        }
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return replacers_[ icol ]
              .adjustColumnInfo( super.getColumnInfo( icol ) );
    }

    public Object getCell( long irow, int icol ) throws IOException {
        return replacers_[ icol ].replaceValue( super.getCell( irow, icol ) );
    }

    public Object[] getRow( long irow ) throws IOException {
        Object[] row = super.getRow( irow );
        for ( int icol = 0; icol < row.length; icol++ ) {
            row[ icol ] = replacers_[ icol ].replaceValue( row[ icol ] );
        }
        return row;
    }

    public RowSequence getRowSequence() throws IOException {
        return new WrapperRowSequence( super.getRowSequence() ) {
            public Object getCell( int icol ) throws IOException {
                return replacers_[ icol ].replaceValue( super.getCell( icol ) );
            }
            public Object[] getRow() throws IOException {
                Object[] row = super.getRow();
                for ( int icol = 0; icol < row.length; icol++ ) {
                    row[ icol ] = replacers_[ icol ]
                                 .replaceValue( row[ icol ] );
                }
                return row;
            }
        };
    }

    /**
     * Creates a replacer instance which can handle value replacements for
     * a given column.
     *
     * @param  info  metadata object for the column whose values will be
     *         modified
     * @param  oldStr  string representation of the value in the column
     *         to be replaced
     * @param  newStr  string representation of the replacement value
     * @return  replacer instance which can do the work
     */
    private static Replacer createReplacer( ColumnInfo info, String oldStr,
                                            String newStr )
            throws IOException {
        try {
            final Class clazz = info.getContentClass();
            boolean oldBlank = isBlank( oldStr, info );
            boolean newBlank = isBlank( newStr, info );
            final Object newValue = newBlank ? null
                                             : info.unformatString( newStr );
            if ( oldBlank ) {
                return new Replacer( newBlank ) {
                    public Object replaceValue( Object obj ) {
                        return Tables.isBlank( obj ) ? newValue
                                                     : obj;
                    }
                };
            }
            else if ( clazz == Double.class ) {
                final double oldVal = Double.parseDouble( oldStr );
                return new Replacer( true ) {
                    public Object replaceValue( Object obj ) {
                        if ( obj instanceof Double ) {
                            double value = ((Double) obj).doubleValue();
                            return ( value == oldVal ||
                                     Math.abs( value - oldVal ) <= DOUBLE_TOL )
                                 ? newValue
                                 : obj;
                        }
                        else {
                            return obj;
                        }
                    }
                };
            }
            else if ( clazz == Float.class ) {
                final float oldVal = Float.parseFloat( oldStr );
                return new Replacer( true ) {
                    public Object replaceValue( Object obj ) {
                        if ( obj instanceof Float ) {
                            float value = ((Float) obj).floatValue();
                            return ( value == oldVal ||
                                     Math.abs( value - oldVal ) <= FLOAT_TOL )
                                 ? newValue
                                 : obj;
                        }
                        else {
                            return obj;
                        }
                    }
                };
            }
            else {
                final Object oldVal;
                try {
                    oldVal = info.unformatString( oldStr );
                }
                catch ( IllegalArgumentException e ) {
                    logger_.info( "No replacements in column " 
                                + info.getName() + " (" + oldStr + " not "
                                + info.formatClass( info.getContentClass() )
                                + ")" );
                    return unitReplacer_;
                }
                return new Replacer( newBlank ) {
                    public Object replaceValue( Object obj ) {
                        return oldVal.equals( obj ) ? newValue
                                                    : obj;
                    }
                };
            }
        }
        catch ( IllegalArgumentException e ) {
            String msg = "Can't replace \"" + oldStr + "\" with \"" + newStr
                       + "\" in " + info.formatClass( info.getContentClass() )
                       + " column " + info.getName();
            throw (IOException) new IOException( msg ).initCause( e );
        }
    }

    /**
     * Indicates whether a string representation is to be interpreted as
     * a blank value.
     *
     * @param   str  string
     * @param   info   value metadata for which str provides value
     * @return  true iff blank
     */
    private static boolean isBlank( String str, ValueInfo info ) {
        if ( str == null ||
             str.length() == 0 ||
             Tables.isBlank( str ) ||
             "NULL".equals( str ) ) {
            return true;
        }
        else if ( ( Float.class.equals( info.getContentClass() ) ||
                    Double.class.equals( info.getContentClass() ) ) &&
                  "NaN".equals( str ) ) {
            return true;
        }
        else {
            return false;
        }
    }
    
    /**
     * Defines the interface which does the work of replacement.
     * An instance of this class translates an object from its pre-replaced
     * to post-replaced value.
     */
    private static abstract class Replacer {

        private final boolean keepShape_;

        /**
         * Constructor.
         *
         * @param  keepShape true if the output element size/shape is known
         *         to be still valid for the replaced column; otherwise
         *         it will be invalidated (set to -1)
         */
        Replacer( boolean keepShape ) {
            keepShape_ = keepShape;
        }

        /**
         * Replaces the value.
         *
         * @param  value  pre-replacement value
         * @return  post-replacement value
         */
        public abstract Object replaceValue( Object value );

        /**
         * Returns a column info suitable for the replacement column.
         *
         * @param  info  pre-replacement metadata
         * @return  post-replacement metadata
         */
        public ColumnInfo adjustColumnInfo( ColumnInfo info ) {
            if ( ! keepShape_ ) {
                info = new ColumnInfo( info );
                info.setElementSize( -1 );
            }
            return info;
        }
    }
}
