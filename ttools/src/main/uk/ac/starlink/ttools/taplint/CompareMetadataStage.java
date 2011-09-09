package uk.ac.starlink.ttools.taplint;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import uk.ac.starlink.vo.ColumnMeta;
import uk.ac.starlink.vo.ForeignMeta;
import uk.ac.starlink.vo.TableMeta;

/**
 * Validation stage for checking that two sets of table metadata
 * (presumably obtained via different routes) are equivalent to each other.
 *
 * @author   Mark Taylor
 * @since    7 Jun 2011
 */
public class CompareMetadataStage implements Stage {

    private final String srcDesc1_;
    private final String srcDesc2_;
    private final MetadataHolder metaHolder1_;
    private final MetadataHolder metaHolder2_;
    private static Pattern ADQLTYPE_REGEX =
        Pattern.compile( "^(adql:)?([^(]*)(\\(.*\\))?$" );

    /**
     * Constructor.
     *
     * @param  srcDesc1  short description of source of first metadata set
     * @param  srcDesc2  short description of source of second metadata set
     * @param  metaHolder1  supplies first metadata set at comparison time
     * @param  metaHolder2  supplies second metadata set at comparison time
     */
    public CompareMetadataStage( String srcDesc1, String srcDesc2,
                                 MetadataHolder metaHolder1,
                                 MetadataHolder metaHolder2 ) {
        srcDesc1_ = srcDesc1;
        srcDesc2_ = srcDesc2;
        metaHolder1_ = metaHolder1;
        metaHolder2_ = metaHolder2;
    }

    public String getDescription() {
        return "Compare table metadata from " + srcDesc1_ + " and " + srcDesc2_;
    }

    public void run( Reporter reporter, URL serviceUrl ) {
        TableMeta[] tmetas1 = metaHolder1_.getTableMetadata();
        TableMeta[] tmetas2 = metaHolder2_.getTableMetadata();
        if ( tmetas1 == null || tmetas2 == null ) {
            reporter.report( ReportType.FAILURE, "NOTM",
                             "Don't have two metadata sets to compare"
                           + " (earlier stages failed/skipped?)" );
            return;
        }
        compareTables( reporter, tmetas1, tmetas2 );
    }

    /**
     * Compares the content of two arrays of TableMeta objects.
     *
     * @param  reporter  destination for validation messages
     * @param  tmetas1  first table metadata set
     * @param  tmetas2  second table metadata set
     */
    private void compareTables( Reporter reporter,
                                TableMeta[] tmetas1, TableMeta[] tmetas2 ) {
        Map<String,TableMeta> tmMap1 = createNameMap( tmetas1 );
        Map<String,TableMeta> tmMap2 = createNameMap( tmetas2 );
        Collection<String> tNames =
            getIntersection( reporter, 'T', "Table", null, tmetas1, tmetas2 );
        for ( String tname : tNames ) {
            TableMeta tm1 = tmMap1.get( tname );
            TableMeta tm2 = tmMap2.get( tname );
            assert tm1 != null && tm2 != null : tm1 + " " + tm2;
            compareColumns( reporter, tname,
                            tm1.getColumns(), tm2.getColumns() );
            compareForeignKeys( reporter, tname,
                                tm1.getForeignKeys(), tm2.getForeignKeys() );
        }
    }

    /**
     * Compares the content of two arrays of ColumnMeta objects.
     *
     * @param  reporter  destination for validation messages
     * @param  cmetas1  first column metadata set
     * @param  cmetas2  second column metadata set
     */
    private void compareColumns( Reporter reporter, String tableName,
                                 ColumnMeta[] cmetas1, ColumnMeta[] cmetas2 ) {
        Map<String,ColumnMeta> cmMap1 = createNameMap( cmetas1 );
        Map<String,ColumnMeta> cmMap2 = createNameMap( cmetas2 );
        Collection<String> cNames =
            getIntersection( reporter, 'C', "Column", "table " + tableName,
                             cmetas1, cmetas2 );
        for ( String cname : cNames ) {
            ColumnMeta cm1 = cmMap1.get( cname );
            ColumnMeta cm2 = cmMap2.get( cname );
            Checker checker =
                new Checker( reporter, "Column", tableName + ":" + cname );
            checker.checkDataTypes( "CTYP",
                                    cm1.getDataType(), cm2.getDataType() );
            checker.check( "UCD", "CUCD", cm1.getUcd(), cm2.getUcd() );
            checker.check( "Utype", "CUTP", cm1.getUtype(), cm2.getUtype() );
            checker.check( "Unit", "CUNI", cm1.getUnit(), cm2.getUnit() );
            checker.check( "IsIndexed", "CIDX",
                           cm1.isIndexed(), cm2.isIndexed() );
        }
    }

    /**
     * Compares the content of two arrays of ForeignMeta objects.
     *
     * @param  reporter  destination for validation messages
     * @param  fmetas1  first foreign key metadata set
     * @param  fmetas2  second foreign key metadata set
     */
    private void compareForeignKeys( Reporter reporter, String tableName,
                                     ForeignMeta[] fmetas1,
                                     ForeignMeta[] fmetas2 ) {

        /* ForeignMeta's toString method encodes most of its useful
         * content, so the checking done in getIntersection does pretty
         * much everything required.  Also, there is no identifier field
         * for foreign keys, so apart from that content, it's not clear
         * how you would work out which pairs are supposed to correspond. */
        getIntersection( reporter, 'F', "Foreign key", "table " + tableName,
                         fmetas1, fmetas2 );
    }

    /**
     * Returns the list of names (toString results) which is common to
     * two arrays of objects.  If the objects differ, messages are
     * written to the reporter.
     *
     * @param  reporter  destination for validation messages
     * @param  lchr  labelling character for object type
     * @param  ltype  labelling name for object type
     * @param  context  location of items for reporting
     * @param  items1  array of items with toString methods indicating identity
     * @param  items2  array of items with toString methods indicating identity
     * @return  collection of common toString values between input item sets
     */
    private Collection<String> getIntersection( Reporter reporter, char lchr,
                                                String ltype, String context,
                                                Object[] items1,
                                                Object[] items2 ) {

        /* Prepare list of names. */
        List<String> names1 = new ArrayList<String>();
        for ( int i1 = 0; i1 < items1.length; i1++ ) {
            names1.add( items1[ i1 ].toString() );
        }
        List<String> names2 = new ArrayList<String>();
        for ( int i2 = 0; i2 < items2.length; i2++ ) {
            names2.add( items2[ i2 ].toString() );
        }

        /* Report on discrepancies. */
        List<String> extras1 = new ArrayList( names1 );
        List<String> extras2 = new ArrayList( names2 );
        extras1.removeAll( names2 );
        extras2.removeAll( names1 );
        String contextString = context == null ? "" : " from " + context;
        for ( String ex1 : extras1 ) {
            String msg = new StringBuffer()
               .append( ltype )
               .append( " " )
               .append( ex1 )
               .append( contextString )
               .append( " exists in " )
               .append( srcDesc1_ )
               .append( " but not in " )
               .append( srcDesc2_ )
               .toString();
            reporter.report( ReportType.ERROR, "" + lchr + "M12", msg );
        }
        for ( String ex2 : extras2 ) {
            String msg = new StringBuffer()
               .append( ltype )
               .append( " " )
               .append( ex2 )
               .append( contextString )
               .append( " exists in " )
               .append( srcDesc2_ )
               .append( " but not " )
               .append( srcDesc1_ )
               .toString();
            reporter.report( ReportType.ERROR, "" + lchr + "M21", msg );
        }

        /* Calculate and return intersection set. */
        List<String> intersect = new ArrayList( names1 );
        intersect.retainAll( names2 );
        return intersect;
    }

    /**
     * Indicates whether two datatypes are compatible with each other.
     * Datatypes may be either VOTable or TAP/adql type.
     * See VODataService v1.1 section 3.5.3 and TAP v1.0 section 2.5.
     * The logic is somewhat sloppy.
     *
     * @param  dt1  first data type
     * @param  dt2  second data type
     * @return  true iff it looks like the submitted types are compatible
     */
    public static boolean compatibleDataTypes( String dt1, String dt2 ) {
        return compatibleDataTypesOneWay( dt1, dt2 ) ||
               compatibleDataTypesOneWay( dt2, dt1 );
    }

    /**
     * Service method used by {@link #compatibleDataTypes}.
     *
     * @param  dt1  first data type
     * @param  dt2  second data type
     * @return  true iff it looks like the submitted types are compatible
     */
    private static boolean compatibleDataTypesOneWay( String dt1, String dt2 ) {
        boolean isBlank1 = dt1 == null || dt1.trim().length() == 0;
        boolean isBlank2 = dt2 == null || dt2.trim().length() == 0;
        if ( isBlank1 && isBlank2 ) {
            return true;
        }
        if ( isBlank1 || isBlank2 ) {
            return false;
        }
        Matcher atm1 = ADQLTYPE_REGEX.matcher( dt1 );
        if ( atm1.matches() ) {
            dt1 = atm1.group( 2 );
        }
        if ( dt1.equals( dt2 ) ) {
            return true;
        }
        if ( dt1.equals( "SMALLINT" ) && dt2.equals( "short" ) ||
             dt1.equals( "INTEGER" ) && dt2.equals( "int" ) ||
             dt1.equals( "BIGINT" ) && dt2.equals( "long" ) ||
             dt1.equals( "REAL" ) && dt2.equals( "float" ) ||
             dt1.equals( "DOUBLE" ) && dt2.equals( "double" ) ||
             dt1.equals( "VARBINARY" ) && ( dt2.equals( "short" ) ||
                                            dt2.equals( "int" ) ||
                                            dt2.equals( "long" ) ||
                                            dt2.equals( "float" ) ||
                                            dt2.equals( "double" ) ||
                                            dt2.equals( "unsignedByte" ) ) ||
             dt1.equals( "BLOB" ) && dt2.equals( "unsignedByte" ) ||
             ( dt1.equals( "CHAR" ) ||
               dt1.equals( "VARCHAR" ) ||
               dt1.equals( "CLOB" ) ||
               dt1.equals( "TIMESTAMP" ) ||
               dt1.equals( "POINT" ) ||
               dt1.equals( "REGION" ) ) && dt2.equals( "char" ) ||
             ( dt1.equals( "SMALLINT" ) ||
               dt1.equals( "INTEGER" ) ) && dt2.equals( "boolean" ) ) {
            return true;
        }
        return false;
    }

    /**
     * Returns a name->object map for a given object array, where the
     * name is obtained using the toString method.
     * No reporting or checking is done.
     *
     * @param   items   item array
     * @return  name->item map
     */
    private static <T> Map<String,T> createNameMap( T[] items ) {
        Map<String,T> map = new LinkedHashMap<String,T>();
        for ( int i = 0; i < items.length; i++ ) {
            T item = items[ i ];
            map.put( item.toString(), item );
        }
        return map;
    }

    /**
     * Utility method to construct a CompareMetadataStage instance given
     * two TableMetadataStage objects.
     *
     * @param  stage1  first metadata producing stage
     * @param  stage2  second metadata producing stage
     * @return   comparison stage
     */
    public static CompareMetadataStage
            createStage( final TableMetadataStage stage1,
                         final TableMetadataStage stage2 ) {
        return new CompareMetadataStage( stage1.getSourceDescription(),
                                         stage2.getSourceDescription(),
                                         stage1, stage2 ) {
            protected TableMeta[] getMetas1() {
                return stage1.getTableMetadata();
            }
            protected TableMeta[] getMetas2() {
                return stage2.getTableMetadata();
            }
        };
    }

    /**
     * Class which checks and reports on equality of objects.
     */
    private static class Checker {
        private final Reporter reporter_;
        private final String objectType_;
        private final String objectName_;

        /**
         * Constructor.
         *
         * @param  reporter  validation message destination
         * @param  objectType  category of object being checked
         * @param  objectName  identifier of object being checked
         */
        Checker( Reporter reporter, String objectType, String objectName ) {
            reporter_ = reporter;
            objectType_ = objectType;
            objectName_ = objectName;
        }

        /**
         * Check equality of two items with the same name.
         *
         * @param    itemName  name of item
         * @param    code  reporter code for validation messages
         * @param    item1  first item
         * @param    item2  second item
         */
        void check( String itemName, String code, Object item1, Object item2 ) {
            if ( item1 == null && item2 == null ||
                 item1 != null && item1.equals( item2 ) ) {
                // ok
            }
            else {
                String q1 = item1 instanceof String ? "\"" : "";
                String q2 = item2 instanceof String ? "\"" : "";
                String msg = new StringBuffer()
                    .append( itemName )
                    .append( " mismatch for " )
                    .append( objectType_ )
                    .append( " " )
                    .append( objectName_ )
                    .append( "; " )
                    .append( q1 )
                    .append( item1 )
                    .append( q1 )
                    .append( " != " )
                    .append( q2 )
                    .append( item2 )
                    .append( q2 )
                    .toString();
                reporter_.report( ReportType.WARNING, code, msg );
            }
        }

        /**
         * Check compatibility of two datatypes.
         *
         * @param  code  error code
         * @param  dt1  first data type
         * @param  dt2  second data type
         */
        void checkDataTypes( String code, String dt1, String dt2 ) {
            if ( ! compatibleDataTypes( dt1, dt2 ) ) {
                String msg = new StringBuffer()
                    .append( "Incompatible datatypes for " )
                    .append( objectType_ )
                    .append( " " )
                    .append( objectName_ )
                    .append( "; " )
                    .append( dt1 )
                    .append( " vs. " )
                    .append( dt2 )
                    .toString();
                reporter_.report( ReportType.WARNING, code, msg );
            }
        }
    }
}
