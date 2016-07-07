package uk.ac.starlink.gbin;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides methods for extracting metadata from a GBIN file.
 *
 * @author   Mark Taylor
 * @since    13 Aug 2014
 */
public class GbinMetadataReader {

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.gbin" );
    private static final ClassMap classMap_ = createClassMap();
    private static final MetadataReader mr_ = createMetadataReader();

    /**
     * Private constructor prevents instantiation.
     */
    private GbinMetadataReader() {
    }

    /**
     * Attempts to read a metadata object from a GbinReader object.
     *
     * <p><strong>NOTE:</strong> this method is effectively destructive:
     * if you read metadata from a GbinReader you cannot then go on
     * to read data records from it.
     *
     * <p>A wide range of exceptions and errors may be thrown by this
     * method, since it involves reflection all sorts of things can
     * go wrong.
     *
     * @param  gbinReaderObj   object implementing
     *                         <code>gaia.cu1.tools.dal.gbin.GbinReader</code>
     * @return  metadata if possible
     */
    public static GbinMeta attemptReadMetadata( Object gbinReaderObj )
            throws Throwable {
        Object metaObj = gbinReaderObj.getClass()
                        .getMethod( "getGbinMetaData", new Class[ 0 ] )
                        .invoke( gbinReaderObj, new Object[ 0 ] );
        return Proxies.createReflectionProxy( GbinMeta.class, metaObj );
    }

    /**
     * Returns the "official" table name for a GBIN file containing
     * objects of a given class.
     *
     * @param  objClazz   class of the objects that form the records of
     *                    a GBIN file
     * @return   gaia table name, or null if not known
     * @see   #getTableMetadata
     */
    public static String getGaiaTableName( Class<?> objClazz ) {
        Class<?> dmClazz = classMap_.getDefinition( objClazz );
        if ( dmClazz != null ) {
            String dmClazzName = dmClazz.getSimpleName();
            try {
                return (String)
                       Class
                      .forName( "gaia.cu9.tools.nameconventions.NameChanger" )
                      .getMethod( "convertNameToArchiveFormat",
                                  new Class<?>[] { String.class } )
                      .invoke( null, new Object[] { dmClazzName } );
            }
            catch ( Throwable e ) {
                logger_.log( Level.INFO,
                             "No gaia table name for class " + dmClazz, e );
                return null;
            }
        }
        else {
            return null;
        }
    }

    /**
     * Returns a metadata object containing information about an
     * "official" known Gaia table. 
     * The table name may be obtained by calling {@link #getGaiaTableName}.
     * The returned metadata is extracted from datamodel classes
     * on the classpath, by an instance of
     * <code>gaia.cu9.tools.documentationexport.MetadataReader</code>.
     *
     * @param  gaiaTableName   "official" gaia table name
     * @return   metadata object, or null if table name not known
     * @see   gaia.cu9.tools.documentationexport.MetadataReader
     */
    public static GaiaTableMetadata getTableMetadata( String gaiaTableName ) {
        return mr_.getTableNameList().indexOf( gaiaTableName ) >= 0
             ? new GaiaTableMetadataImpl( mr_, gaiaTableName )
             : null;
    }

    /**
     * Creates a proxy instance of a ClassMap.
     * The result is not null, but if there are reflection problems,
     * all the methods of the returned instance will return null.
     *
     * @return  ClassMap instance
     */
    private static ClassMap createClassMap() {
        try {
            Object cmObj =
                Class.forName( "gaia.cu1.tools.util.GaiaFactory" )
               .getMethod( "getClassMap", new Class<?>[ 0 ] )
               .invoke( null, new Object[ 0 ] );
            return Proxies.createReflectionProxy( ClassMap.class, cmObj );
        }
        catch ( Throwable e ) {
            logger_.log( Level.WARNING, "Failed to get Gaia ClassMap instance",
                         e );
            return Proxies.createNullsProxy( ClassMap.class );
        }
    }

    /**
     * Creates a proxy instance of a MetadataReader.
     * The result is not null, but if there are reflection problems,
     * all the methods of the returned instance will return null.
     *
     * @return  MetadataReader instance
     */
    private static MetadataReader createMetadataReader() {
        try {
            Object mrObj =
                Class
               .forName( "gaia.cu9.tools.documentationexport.MetadataReader" )
               .getMethod( "getInstance", new Class<?>[ 0 ] )
               .invoke( null, new Object[ 0 ] );
            return Proxies.createReflectionProxy( MetadataReader.class, mrObj );
        }
        catch ( Throwable e ) {
            logger_.log( Level.WARNING,
                         "Failed to get Gaia MetadataReader instance" );
            return Proxies.createNullsProxy( MetadataReader.class );
        }
    }

    /**
     * Includes methods from gaia.cu1.tools.dal.ClassMap.
     */
    private interface ClassMap {
        Class<?> getDefinition( Class<?> implementation );
        Class<?> getImplementation( Class<?> definition );
    }

    /**
     * Includes methods from gaia.cu9.tools.documentationexport.MetadataReader.
     */
    private interface MetadataReader {
        List<String> getTableNameList();
        Map<String,String> getParametersWithTypes(String tableName);
        String getTableDescription(String tableName);
        String getParameterDetailedDescription(String tableName,
                                               String parameterName);
        String getParameterDescription(String tableName, String parameterName);
        List<?> getUcds(String tableName, String parameterName);
    }

    /**
     * Implementation of GaiaTableMetadata based on a MetadataReader.
     */
    private static class GaiaTableMetadataImpl implements GaiaTableMetadata {
        private final MetadataReader mr_;
        private final String tableName_;

        /**
         * Constructor.
         *
         * @param  mr   metadata reader
         * @param  tableName  official gaia table name, known to mr
         */
        GaiaTableMetadataImpl( MetadataReader mr, String tableName ) {
            mr_ = mr;
            tableName_ = tableName;
        }
        public Map<String,String> getParametersWithTypes() {
            return mr_.getParametersWithTypes( tableName_ );
        }
        public String getTableDescription() {
            return mr_.getTableDescription( tableName_ );
        }
        public String getParameterDetailedDescription( String paramName ) {
            return mr_.getParameterDetailedDescription( tableName_,
                                                        paramName );
        }
        public String getParameterDescription( String paramName ) {
            return mr_.getParameterDescription( tableName_, paramName );
        }
        public List<?> getUcds( String paramName ) {
            return mr_.getUcds( tableName_, paramName );
        }
    }

    /**
     * Attempts to read metadata from the GBIN file named on the command line
     * and writes the description to stdout.
     */
    public static void main( String[] args ) throws Throwable {
        InputStream in = new FileInputStream( args[ 0 ] );
        GbinObjectReader.initGaiaTools();
        Object gbinRdrObj = GbinObjectReader.createGbinReaderObject( in );
        GbinMeta meta = attemptReadMetadata( gbinRdrObj );
        System.out.println( meta );
    }
}
