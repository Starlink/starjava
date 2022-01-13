package uk.ac.starlink.ttools.jel;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import uk.ac.starlink.table.Tables;

/**
 * JELRowReader for accessing JDBC {@link java.sql.ResultSet} objects.
 * Column indices are 1-based, as for other JDBC methods.
 *
 * @author   Mark Taylor
 * @since    10 Dec 2007
 */
public class ResultSetJELRowReader extends JELRowReader {

    private final ResultSet rset_;
    private String[] colNames_;
    private String[] colLabels_;
    private Class<?>[] colClazzes_;

    /**
     * Constructor.
     *
     * @param   rset  result set
     */
    public ResultSetJELRowReader( ResultSet rset ) throws SQLException {
        rset_ = rset;
        ResultSetMetaData rsetMeta = rset.getMetaData();
        int ncol = rsetMeta.getColumnCount();
        colNames_ = new String[ ncol ];
        colLabels_ = new String[ ncol ];
        colClazzes_ = new Class<?>[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            int icol1 = icol + 1;
            colNames_[ icol ] = rsetMeta.getColumnName( icol1 );
            colLabels_[ icol ] = rsetMeta.getColumnLabel( icol1 );
            String clazzName = rsetMeta.getColumnClassName( icol1 );
            try {
                colClazzes_[ icol ] = Class.forName( clazzName );
            }
            catch ( ClassNotFoundException e ) {
                throw (SQLException)
                      new SQLException( "No such class " + clazzName )
                     .initCause( e );
            }
        }
    }

    protected int getColumnIndexByName( String name ) {
        int ncol = colNames_.length;
        for ( int icol = 0; icol < ncol; icol++ ) {
            if ( colNames_[ icol ].equalsIgnoreCase( name ) ||
                 colLabels_[ icol ].equalsIgnoreCase( name ) ) {
                return icol;
            }
        }
        return -1;
    }

    protected Constant<?> getConstantByName( String name ) {
        return null;
    }

    protected boolean isBlank( int icol ) {
        try {
            return Tables.isBlank( rset_.getObject( icol + 1 ) )
                || rset_.wasNull();
        }
        catch ( SQLException e ) {
            return true;
        }
    }

    protected Class<?> getColumnClass( int icol ) {
        return icol < colClazzes_.length ? colClazzes_[ icol ] : null;
    }

    protected boolean getBooleanColumnValue( int icol ) {
        try {
            boolean result = rset_.getBoolean( icol + 1 );
            if ( rset_.wasNull() ) {
                foundNull();
            }
            return result;
        }
        catch ( SQLException e ) {
            throw new RuntimeException( e );
        }
    }
    protected byte getByteColumnValue( int icol ) {
        try {
            byte result = rset_.getByte( icol + 1 );
            if ( rset_.wasNull() ) {
                foundNull();
            }
            return result;
        }
        catch ( SQLException e ) {
            throw new RuntimeException( e );
        }
    }
    protected char getCharColumnValue( int icol ) {
        throw new UnsupportedOperationException();
    }
    protected short getShortColumnValue( int icol ) {
        try {
            short result = rset_.getShort( icol + 1 );
            if ( rset_.wasNull() ) {
                foundNull();
            }
            return result;
        }
        catch( SQLException e ) {
            throw new RuntimeException( e );
        }
    }
    protected int getIntColumnValue( int icol ) {
        try {
            int result = rset_.getInt( icol + 1 );
            if ( rset_.wasNull() ) {
                foundNull();
            }
            return result;
        }
        catch ( SQLException e ) {
            throw new RuntimeException( e );
        }
    }
    protected long getLongColumnValue( int icol ) {
        try {
            long result = rset_.getLong( icol + 1 );
            if ( rset_.wasNull() ) {
                foundNull();
            }
            return result;
        }
        catch ( SQLException e ) {
            throw new RuntimeException( e );
        }
    }
    protected float getFloatColumnValue( int icol ) {
        try {
            float result = rset_.getFloat( icol + 1 );
            if ( rset_.wasNull() ) {
                foundNull();
            }
            return result;
        }
        catch ( SQLException e ) {
            throw new RuntimeException( e );
        }
    }
    protected double getDoubleColumnValue( int icol ) {
        try {
            double result = rset_.getDouble( icol + 1 );
            if ( rset_.wasNull() ) {
                foundNull();
            }
            return result;
        }
        catch ( SQLException e ) {
            throw new RuntimeException( e );
        }
    }
    protected Object getObjectColumnValue( int icol ) {
        try {
            return rset_.getObject( icol + 1 );
        }
        catch ( SQLException e ) {
            throw new RuntimeException( e );
        }
    }

    protected Constant<?> getSpecialByName( String name ) {
        return super.getSpecialByName( name );
    }
}
