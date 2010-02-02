package uk.ac.starlink.table.jdbc;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ValueInfo;

/**
 * TypeMapper implementation classes.
 *
 * @author   Mark Taylor
 * @since    2 Feb 2010
 */
public class TypeMappers {

    /**
     * TypeMapper implementation which performs generally useful conversions.
     * In particular, {@link java.util.Date} subclasses (including
     * <code>java.sql.Date</code>, <code>java.sql.Time</code> and
     * <code>java.sql.Timestamp</code> are turned into Strings.
     * The intention is that by using this implementation you will get
     * an output table which can be written using non-specialist output
     * formats such as FITS and VOTable.
     */
    public static final TypeMapper STANDARD = new StandardTypeMapper();

    /**
     * TypeMapper implementation which performs no conversions.
     * The output types are just as JDBC provides them.
     */
    public static final TypeMapper IDENTITY = new IdentityTypeMapper();

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.table.jdbc" );
    private static final ValueInfo JDBC_CLAZZ_INFO =
        new DefaultValueInfo( "JDBC_class", String.class,
                              "Class returned by JDBC database driver" );
    private static final ValueInfo JDBC_TYPE_INFO =
        new DefaultValueInfo( "JDBC_type", String.class,
                              "SQL type of column in database" );
    private static final ValueInfo JDBC_LABEL_INFO =
        new DefaultValueInfo( "JDBC_label", String.class,
                              "Label of column in JDBC database" );

    /**
     * Private constructor prevents instantiation.
     */
    private TypeMappers() {
    }

    /**
     * Constructs a new ValueHandler which performs no conversions.
     *
     * @param   meta   JDBC metadata object
     * @param   jcol1  JDBC column index (first column is 1)
     */
    public static ValueHandler createIdentityValueHandler(
            ResultSetMetaData meta, int jcol1 ) throws SQLException {
        return new IdentityValueHandler( meta, jcol1 );
    }

    /**
     * Constructs a new ValueHandler which converts values to Strings.
     *
     * @param   meta   JDBC metadata object
     * @param   jcol1  JDBC column index (first column is 1)
     */
    public static ValueHandler createStringValueHandler(
            ResultSetMetaData meta, int jcol1 ) throws SQLException {
        return new StringValueHandler( meta, jcol1 );
    }

    /**
     * ValueHandler implementation which performs no conversions.
     */
    private static class IdentityValueHandler implements ValueHandler {
        final ColumnInfo colInfo_;

        /**
         * Constructor.
         *
         * @param   meta   JDBC metadata object
         * @param   jcol1  JDBC column index (first column is 1)
         */
        IdentityValueHandler( ResultSetMetaData meta, int jcol1 )
                throws SQLException {

            /* Set up the name and metadata for this column. */
            String name = meta.getColumnName( jcol1 );
            colInfo_ = new ColumnInfo( name );

            /* Find out what class objects will have.  If the class hasn't
             * been loaded yet, just call it an Object (could try obtaining
             * an object from that column and using its class, but then it
             * might be null...). */
            String className = meta.getColumnClassName( jcol1 );
            if ( className != null ) {
                try {
                    Class clazz = getClass().forName( className );
                    colInfo_.setContentClass( clazz );
                }
                catch ( ClassNotFoundException e ) {
                    logger_.warning( "Cannot determine class " + className
                                   + " for column " + name );
                    colInfo_.setContentClass( Object.class );
                }
            }
            else {
                logger_.warning( "No column class given for column " + name );
                colInfo_.setContentClass( Object.class );
            }

            /* Assign nullability. */
            if ( meta.isNullable( jcol1 ) == ResultSetMetaData.columnNoNulls ) {
                colInfo_.setNullable( false );
            }
            String label = meta.getColumnLabel( jcol1 );
            if ( label != null &&
                 label.trim().length() > 0 &&
                 ! label.equalsIgnoreCase( name ) ) {
                colInfo_.setAuxDatum( new DescribedValue( JDBC_LABEL_INFO,
                                                          label.trim() ) );
            }
        }

        public ColumnInfo getColumnInfo() {
            return colInfo_;
        }

        public Object getValue( Object baseValue ) {
            return baseValue;
        }
    }

    /**
     * ValueHandler implementation which converts JDBC values to Strings.
     */
    private static class StringValueHandler extends IdentityValueHandler {

        /**
         * Constructor.
         *
         * @param   meta   JDBC metadata object
         * @param   jcol1  JDBC column index (first column is 1)
         */
        StringValueHandler( ResultSetMetaData meta, int jcol1 )
                throws SQLException {
            super( meta, jcol1 );
            String origClazz = meta.getColumnClassName( jcol1 );
            String origType = meta.getColumnTypeName( jcol1 );
            if ( origClazz != null && origClazz.trim().length() > 0 ) {
                colInfo_.setAuxDatum( new DescribedValue( JDBC_CLAZZ_INFO,
                                                          origClazz ) );
            }
            if ( origType != null && origType.trim().length() > 0 ) {
                colInfo_.setAuxDatum( new DescribedValue( JDBC_TYPE_INFO,
                                                          origType ) );
            }
            colInfo_.setContentClass( String.class );
        }

        public Object getValue( Object baseValue ) {
            return baseValue == null ? null
                                     : baseValue.toString();
        }
    }

    /**
     * TypeMapper which performs no conversions.
     */
    private static class IdentityTypeMapper implements TypeMapper {
        public ValueHandler createValueHandler( ResultSetMetaData meta,
                                                int jcol1 )
                throws SQLException {
            return createIdentityValueHandler( meta, jcol1 );
        }
        public List getColumnAuxDataInfos() {
            return Collections.unmodifiableList( Arrays
                                                .asList( new ValueInfo[] {
                JDBC_LABEL_INFO,
            } ) );
        }
    }

    /**
     * TypeMapper which performs generally useful conversions.
     * The intention is that by using this implementation you will get
     * an output table which can be written using non-specialist output
     * formats such as FITS and VOTable.
     */
    private static class StandardTypeMapper implements TypeMapper {
        public ValueHandler createValueHandler( ResultSetMetaData meta,
                                                int jcol1 )
                throws SQLException {
            ValueHandler handler = createIdentityValueHandler( meta, jcol1 );
            Class clazz = handler.getColumnInfo().getContentClass();

            /* java.sql.{Date,Time,Timestamp} are all java.util.Date
             * subclasses. */
            if ( java.util.Date.class.isAssignableFrom( clazz ) ) {
                return createStringValueHandler( meta, jcol1 );
            }

            /* Is this a likely type?  Earlier versions of the code 
             * treated it specially, so it may occur in some DBs. */
            else if ( clazz.equals( char[].class ) ) {
                return new StringValueHandler( meta, jcol1 ) {
                    public Object getValue( Object baseValue ) {
                        return baseValue instanceof char[]
                             ? new String( (char[]) baseValue )
                             : null;
                    }
                };
            }

            /* If no known special case, use the identity handler. */
            else {
                return handler;
            }
        }

        public List getColumnAuxDataInfos() {
            return Collections.unmodifiableList( Arrays
                                                .asList( new ValueInfo[] {
                JDBC_LABEL_INFO,
                JDBC_TYPE_INFO,
                JDBC_CLAZZ_INFO,
            } ) );
        }
    }
}
