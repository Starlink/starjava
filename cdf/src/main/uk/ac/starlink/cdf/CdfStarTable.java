package uk.ac.starlink.cdf;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.bristol.star.cdf.AttributeEntry;
import uk.ac.bristol.star.cdf.CdfContent;
import uk.ac.bristol.star.cdf.DataType;
import uk.ac.bristol.star.cdf.GlobalAttribute;
import uk.ac.bristol.star.cdf.Shaper;
import uk.ac.bristol.star.cdf.Variable;
import uk.ac.bristol.star.cdf.VariableAttribute;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.DomainMapper;
import uk.ac.starlink.table.RowAccess;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;

/**
 * StarTable implementation for CDF files.
 *
 * @author   Mark Taylor
 * @since    24 Jun 2013
 */
public class CdfStarTable extends AbstractStarTable {

    private final Variable[] vars_;
    private final VariableReader[] randomVarReaders_;
    private final int ncol_;
    private final long nrow_;
    private final ColumnInfo[] colInfos_;
    private final VariableAttribute blankvalAtt_;
    private static final Logger logger_ =
        Logger.getLogger( CdfStarTable.class.getName() );

    /**
     * STIL, following FITS and VOTable (and FORTRAN), uses column-major order
     * for array storage.
     */
    private static final boolean STIL_ROW_MAJOR = false;

    /**
     * Constructor.
     *
     * <p>The optional <code>dependVar</code> parameter provides an
     * effective grouping of columns, and corresponds to the DEPEND_0
     * variable attribute defined by the ISTP Metadata Guidelines.
     * CDF files contain multiple variables that are not necessarily
     * all columns of the same table; at least within the ISTP guidelines
     * a table is effectively defined by an independent epoch variable
     * and a number of other variables depending on that (and thus
     * presumably having the same number of values/rows).
     * This interpretation is not exactly explicit in ISTP, but it looks
     * like our best bet for turning CDF content into a thing or things
     * that look like StarTables.
     * If <code>dependVar</code> is null, we just have to assume that
     * all the variables are effectively columns in the same table.
     * In case that they don't all have the same multiplicity,
     * just guess something and issue a warning.
     *
     * @param   content  CDF data content object
     * @param   profile  parameterisation of how CDFs should get turned
     *                   into StarTables
     * @param   dependVar  independent variable on which other columns depend,
     *                     or null
     * @throws  IOException  in case of error
     */
    @SuppressWarnings("this-escape")
    public CdfStarTable( CdfContent content, CdfTableProfile profile,
                         Variable dependVar )
            throws IOException {

        /* Identify useful attributes. */
        VariableAttribute[] vatts = content.getVariableAttributes();
        String[] attNames = Arrays.stream( vatts )
                                  .map( VariableAttribute::getName )
                                  .toArray( n -> new String[ n ] );
        String descAttName = profile.getDescriptionAttribute( attNames );
        String unitAttName = profile.getUnitAttribute( attNames );
        String blankvalAttName = profile.getBlankValueAttribute( attNames );
        String dependAttName = profile.getDepend0Attribute( attNames );
        VariableAttribute descAtt = null;
        VariableAttribute unitAtt = null;
        VariableAttribute blankvalAtt = null;
        VariableAttribute dependAtt = null;
        for ( VariableAttribute vatt : vatts ) {
            String vattName = vatt.getName();
            if ( vattName != null ) {
                if ( vattName.equals( descAttName ) ) {
                    descAtt = vatt;
                }
                else if ( vattName.equals( unitAttName ) ) {
                    unitAtt = vatt;
                }
                else if ( vattName.equals( blankvalAttName ) ) {
                    blankvalAtt = vatt;
                }
                else if ( vattName.equals( dependAttName ) ) {
                    dependAtt = vatt;
                }
            }
        }
        blankvalAtt_ = blankvalAtt;

        /* Remove the attributes we've used for a specific purpose above
         * from the variable attribute list to give a list of miscellaneous
         * attributes. */
        List<VariableAttribute> miscAttList =
            new ArrayList<>( Arrays.asList( vatts ) );
        miscAttList.remove( descAtt );
        miscAttList.remove( unitAtt );
        if ( dependVar != null ) {
            miscAttList.remove( dependAtt );
        }

        /* Identify variables to turn into columns. */
        final List<Variable> varList;
        if ( dependVar == null || dependAtt == null ) {

            /* No declared independent variable; just use all of them. */
            varList = new ArrayList<Variable>( Arrays.asList( content
                                                             .getVariables() ));
        }
        else {

            /* We have an independent variable: assemble a column list
             * including dependVar itself, other variables declared to
             * depend on it, and any variables that do not vary per row. */
            String dependVarName = dependVar.getName();
            varList = new ArrayList<Variable>();
            varList.add( dependVar );
            for ( Variable var : content.getVariables() ) {
                if ( dependVarName.equals( getStringEntry( dependAtt, var ) ) ||
                     ! var.getRecordVariance() ) {
                    varList.add( var );
                }
            }
        }

        /* Identify variables to turn into parameters.
         * This list will only have entries if there are non-varying variables
         * (recordVariance = false) and the profile says these are to be
         * treated as parameters. */
        List<Variable> paramVarList = new ArrayList<>();
        if ( profile.invariantVariablesToParameters() ) {
            for ( Iterator<Variable> it = varList.iterator(); it.hasNext(); ) {
                Variable var = it.next();
                if ( ! var.getRecordVariance() ) {
                    it.remove();
                    paramVarList.add( var );
                }
            }
        }
        Variable[] paramVars = paramVarList.toArray( new Variable[ 0 ] );
        vars_ = varList.toArray( new Variable[ 0 ] );
        ncol_ = vars_.length;

        /* Calculate the row count.  This should be the same for all the
         * variables we are interpreting as columns here.
         * Take the first column as authoritative; if a dependency variable
         * has been declared, that will be in the first position.
         * If the row counts don't line up there's something wrong,
         * possibly our dependency assumptions, so issue a warning. */
        long nrow = -1;
        for ( Variable var : vars_ ) {
            if ( var.getRecordVariance() ) {
                long nr = var.getRecordCount();
                if ( nrow < 0 ) {
                    nrow = nr;
                }
                else {
                    if ( nr != nrow ) {
                        logger_.warning( "Inconsistent row counts; "
                                       + "probably trouble"
                                       + ": " + nr + " != " + nrow
                                       + " at column " + var.getName() );
                    }
                }
            }
        }
        nrow_ = nrow;

        /* Set up random data access. */
        randomVarReaders_ = new VariableReader[ ncol_ ];
        for ( int iv = 0; iv < ncol_; iv++ ) {
            randomVarReaders_[ iv ] = createVariableReader( vars_[ iv ],
                                                            blankvalAtt_ );
        }

        /* Get column metadata for each variable column. */
        colInfos_ = new ColumnInfo[ ncol_ ];
        for ( int icol = 0; icol < ncol_; icol++ ) {
            Variable var = vars_[ icol ];
            Map<String,Object> miscAttMap = new LinkedHashMap<String,Object>();
            for ( VariableAttribute vatt : miscAttList ) {
                if ( ! ( vatt == blankvalAtt_ &&
                         randomVarReaders_[ icol ].usesBlankValue() ) ) {
                    AttributeEntry entry = vatt.getEntry( var );
                    if ( entry != null ) {
                        miscAttMap.put( vatt.getName(),
                                        entry.getShapedValue() );
                    }
                }
            }
            colInfos_[ icol ] =
                createColumnInfo( var, getStringEntry( descAtt, var ),
                                  getStringEntry( unitAtt, var ), miscAttMap );
        }

        /* Generate table parameters from non-variant variables
         * (if applicable). */
        for ( int ipv = 0; ipv < paramVars.length; ipv++ ) {
            Variable pvar = paramVars[ ipv ];
            ValueInfo info =
                createValueInfo( pvar, getStringEntry( descAtt, pvar ),
                                 getStringEntry( unitAtt, pvar ) );
            Object value = createVariableReader( pvar, blankvalAtt_ )
                          .readShapedRecord( 0 );
            setParameter( new DescribedValue( info, value ) );
        }

        /* Generate table parameters from global attributes. */
        GlobalAttribute[] gatts = content.getGlobalAttributes();
        for ( int iga = 0; iga < gatts.length; iga++ ) {
            DescribedValue dval = createParameter( gatts[ iga ] );
            if ( dval != null ) {
                setParameter( dval );
            }
        }
    }

    public int getColumnCount() {
        return ncol_;
    }

    public long getRowCount() {
        return nrow_;
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return colInfos_[ icol ];
    }

    public boolean isRandom() {
        return true;
    }

    public Object getCell( long irow, int icol ) throws IOException {
        return randomVarReaders_[ icol ]
              .readShapedRecord( toRecordIndex( irow ) );
    }

    public RowSequence getRowSequence() throws IOException {
        final VariableReader[] vrdrs = new VariableReader[ ncol_ ];
        for ( int icol = 0; icol < ncol_; icol++ ) {
            vrdrs[ icol ] = createVariableReader( vars_[ icol ], blankvalAtt_ );
        }
        return new RowSequence() {
            private long irow = -1;
            public boolean next() {
                return ++irow < nrow_;
            }
            public Object getCell( int icol ) throws IOException {
                return vrdrs[ icol ].readShapedRecord( toRecordIndex( irow ) );
            }
            public Object[] getRow() throws IOException {
                Object[] row = new Object[ ncol_ ];
                for ( int icol = 0; icol < ncol_; icol++ ) {
                    row[ icol ] = getCell( icol );
                }
                return row;
            }
            public void close() {
            }
        };
    }

    public RowAccess getRowAccess() throws IOException {
        final VariableReader[] vrdrs = new VariableReader[ ncol_ ];
        for ( int icol = 0; icol < ncol_; icol++ ) {
            vrdrs[ icol ] = createVariableReader( vars_[ icol ], blankvalAtt_ );
        }
        final Object[] row = new Object[ ncol_ ];
        return new RowAccess() {
            private long irow = -1;
            public void setRowIndex( long ir ) {
                irow = ir;
            }
            public Object getCell( int icol ) throws IOException {
                return vrdrs[ icol ].readShapedRecord( toRecordIndex( irow ) );
            }
            public Object[] getRow() throws IOException {
                for ( int icol = 0; icol < ncol_; icol++ ) {
                    row[ icol ] = getCell( icol );
                }
                return row;
            }
            public void close() {
            }
        };
    }

    /**
     * Turns a CDF global attribute into a STIL table parameter.
     *
     * @param  gatt  global attribute
     * @return   described value for use as a table parameter
     */
    private static DescribedValue createParameter( GlobalAttribute gatt ) {
        String name = gatt.getName();

        /* Construct the list of non-blank entries for this atttribute. */
        List<AttributeEntry> entryList = new ArrayList<AttributeEntry>();
        for ( AttributeEntry ent : gatt.getEntries() ) {
            if ( ent != null ) {
                entryList.add( ent );
            }
        }
        AttributeEntry[] entries = entryList.toArray( new AttributeEntry[ 0 ] );
        int nent = entries.length;

        /* No entries, treat as a blank value. */
        if ( nent == 0 ) {
            return null;
        }

        /* One entry, treat as a scalar value. */
        else if ( nent == 1 ) {
            Object value = entries[ 0 ].getShapedValue();
            if ( value == null ) {
                return null;
            }
            else {
                ValueInfo info =
                    new DefaultValueInfo( name, value.getClass(), null );
                return new DescribedValue( info, value );
            }
        }

        /* Multiple entries.  Typically they will have the same type
         * so can be treated as an array.  But they might have different
         * types, some might be arrays ... I doubt if such usages are
         * common, but don't know.  Do what we can. */
        else {
            Object array;
            try {
                array = getValueArray( entries );
            }
            catch ( RuntimeException e ) {
                logger_.log( Level.WARNING,
                             "Omitting complicated global attribute "
                           + gatt.getName(), e );
                return null;
            }
            DefaultValueInfo info =
                new DefaultValueInfo( name, array.getClass(), null );
            info.setShape( new int[] { Array.getLength( array ) } );
            return new DescribedValue( info, array );
        }
    }

    /**
     * Attempt to turn an array of attribute entries into an array of
     * values, preferably of some type more specific than Object.
     * If the input array is too weird to make sense of, an unchecked
     * exception of some sort may be thrown.
     *
     * @param  entries  array of entries, hopefully similar to each other
     * @return  array of values
     * @throws  RuntimeException  if something fails
     */
    private static Object getValueArray( AttributeEntry[] entries ) {
        int nent = entries.length;
        assert nent > 1;
        DataType dtype = entries[ 0 ].getDataType();
        boolean allScalar = true;
        for ( int i = 1; i < nent; i++ ) {
            AttributeEntry entry = entries[ i ];
            if ( entry.getDataType() != dtype ) {
                dtype = null;
            }
            allScalar = allScalar && entry.getItemCount() == 1;
        }
        if ( dtype == null || ! allScalar ) {
            Object[] array = new Object[ nent ];
            for ( int i = 0; i < nent; i++ ) {
                array[ i ] = entries[ i ].getShapedValue();
            }
            return array;
        }
        else {
            Class<?> elClass = dtype.getArrayElementClass();
            Object array =Array.newInstance( elClass, nent );
            for ( int i = 0; i < nent; i++ ) {
                Array.set( array, i, entries[ i ].getShapedValue() );
            }
            return array;
        }
    }

    /**
     * Gets a basic value header from a CDF variable and extra information.
     *
     * @param  var  CDF variable
     * @param  descrip   variable description text, or null
     * @param  units    variable units text, or null
     * @return   value metadata
     */
    private static ValueInfo createValueInfo( Variable var, String descrip,
                                              String units ) {
        String name = var.getName();
        Class<?> clazz = var.getShaper().getShapeClass();
        DataType dtype = var.getDataType();
        int grpSize = dtype.getGroupSize();
        final int[] shape; 
        if ( grpSize == 1 ) {
            shape = clazz.getComponentType() == null
                  ? null
                  : var.getShaper().getDimSizes();
        }
        else {
            assert clazz.getComponentType() != null;
            int[] shDims = var.getShaper().getDimSizes();
            shape = new int[ shDims.length + 1 ];
            shape[ 0 ] = grpSize;
            System.arraycopy( shDims, 0, shape, 1, shDims.length );
        }
        DefaultValueInfo info = new DefaultValueInfo( name, clazz, descrip );
        info.setUnitString( units );
        info.setShape( shape );
        DomainMapper mapper = CdfDomains.getMapper( dtype );
        info.setDomainMappers( mapper == null ? new DomainMapper[ 0 ]
                                              : new DomainMapper[] { mapper } );
        return info;
    }

    /**
     * Gets a column header, including auxiliary metadata, from a CDF variable
     * and extra information.
     *
     * @param  var  CDF variable
     * @param  descrip   variable description text, or null
     * @param  units    variable units text, or null
     * @return   column metadata
     */
    private static ColumnInfo createColumnInfo( Variable var, String descrip,
                                                String units,
                                                Map<String,Object> attMap ) {

        /* Create basic column metadata. */
        ColumnInfo info =
            new ColumnInfo( createValueInfo( var, descrip, units ) );

        /* Augment it with auxiliary metadata for the column by examining
         * the attribute values for the variable. */
        List<DescribedValue> auxData = new ArrayList<DescribedValue>();
        for ( Map.Entry<String,Object> attEntry : attMap.entrySet() ) {
            String auxName = attEntry.getKey();
            Object auxValue = attEntry.getValue();
            if ( auxValue != null ) {
                ValueInfo auxInfo =
                    new DefaultValueInfo( auxName, auxValue.getClass() );
                auxData.add( new DescribedValue( auxInfo, auxValue ) );
            }
        }

        /* Flag unsigned byte values according to STIL convention. */
        DataType dtype = var.getDataType();
        if ( dtype == DataType.UINT1 ) {
            assert dtype.getByteCount() == 1;
            assert dtype.getScalarClass() == Short.class;
            auxData.add( new DescribedValue( Tables.UBYTE_FLAG_INFO,
                                             Boolean.TRUE ) );
        }

        /* Return metadata. */
        info.setAuxData( auxData );
        return info;
    }

    /**
     * Gets a variable's attribute value expected to be of string type.
     *
     * @param   att  attribute
     * @param   var  variable
     * @return   string value of att for var, or null if it doesn't exist
     *           or has the wrong type
     */
    public static String getStringEntry( VariableAttribute att, Variable var ) {
        AttributeEntry entry = att == null ? null : att.getEntry( var );
        Object item = entry == null ? null : entry.getShapedValue();
        return item instanceof String ? (String) item : null;
    }

    /**
     * Converts a long to an int when the value is a record/row index.
     *
     * @param   irow   StarTable row index
     * @retrun   CDF record index
     */
    private static int toRecordIndex( long irow ) {
        int irec = (int) irow;
        if ( irec != irow ) {
            /* Long record counts not supported in CDF
             * so this must be a call error. */
            throw new IllegalArgumentException( "Out of range: " + irow );
        }
        else if ( irec < 0 ) {
            throw new IllegalStateException( "No row" );
        }
        return irec;
    }

    /**
     * Constructs a reader for a given variable.
     *
     * @param    var   variable whose values will be read
     * @param   blankValAtt  attribute providing per-variable blank values
     *                       (probably FILLVAL)
     * @return   new variable reader
     */
    private static VariableReader
            createVariableReader( Variable var,
                                  VariableAttribute blankvalAtt ) {

        /* Check if we have a fixed blank value (FILLVAL) for this variable. */
        AttributeEntry blankvalEntry = blankvalAtt == null
                                     ? null
                                     : blankvalAtt.getEntry( var );
                             
        final Object blankval = blankvalEntry == null
                              ? null
                              : blankvalEntry.getShapedValue();
        Shaper shaper = var.getShaper();

        /* No declared blank value, no matching. */
        if ( blankval == null ) {
            return new VariableReader( var, false );
        }

        /* If the variable is a scalar, just match java objects for equality
         * and return null if matched. */
        else if ( shaper.getRawItemCount() == 1 ) {
            return new VariableReader( var, true ) {
                public synchronized Object readShapedRecord( int irec )
                        throws IOException {
                    Object obj = super.readShapedRecord( irec );
                    return blankval.equals( obj ) ? null : obj;
                }
            };
        }

        /* If the value is an array of floating point values, and the 
         * blank value is a scalar number, match each element with the
         * blank value, and set it to NaN in case of match. */
        else if ( double[].class.equals( shaper.getShapeClass() ) &&
                  blankval instanceof Number && 
                  ! Double.isNaN( ((Number) blankval).doubleValue() ) ) {
            final double dBlank = ((Number) blankval).doubleValue();
            return new VariableReader( var, true ) {
                public synchronized Object readShapedRecord( int irec )
                        throws IOException {
                    Object obj = super.readShapedRecord( irec );
                    if ( obj instanceof double[] ) {
                        double[] darr = (double[]) obj;
                        for ( int i = 0; i < darr.length; i++ ) {
                            if ( darr[ i ] == dBlank ) {
                                darr[ i ] = Double.NaN;
                            }
                        }
                    }
                    else {
                        assert false;
                    }
                    return obj;
                }
            };
        }
        else if ( float[].class.equals( shaper.getShapeClass() ) &&
                  blankval instanceof Number &&
                  ! Float.isNaN( ((Number) blankval).floatValue() ) ) {
            final float fBlank = ((Number) blankval).floatValue();
            return new VariableReader( var, true ) {
                public synchronized Object readShapedRecord( int irec )
                        throws IOException {
                    Object obj = super.readShapedRecord( irec );
                    if ( obj instanceof float[] ) {
                        float[] farr = (float[]) obj;
                        for ( int i = 0; i < farr.length; i++ ) {
                            if ( farr[ i ] == fBlank ) {
                                farr[ i ] = Float.NaN;
                            }
                        }
                    }
                    else {
                        assert false;
                    }
                    return obj;
                }
            };
        }

        /* Otherwise (non-floating point array) we have no mechanism to
         * make use of the blank value (can't set integer array elements to
         * null/NaN), so ignore the blank value. */
        else {
            logger_.info( "Magic value " + blankvalAtt.getName()
                        + "=" + String.valueOf( blankval )
                        + " ignored for non-float array CDF variable "
                        + var.getName() );
            return new VariableReader( var, false );
        }
    }

    /**
     * Reads the values for a variable.
     * This class does two things beyond making the basic call to the
     * variable to read the shaped data.
     * First, it provides a workspace array required for the read.
     * Second, it manages matching values against the declared blank value
     * (probably FILLVAL).
     */
    private static class VariableReader {
        private final Variable var_;
        private final boolean usesBlankValue_;
        private final Object work_;

        /**
         * Constructor.
         *
         * @param  var  variable
         * @param  usesBlankValue  true iff this reader will attempt to
         *                         use the blank value to blank out
         *                         matching values (in some cases this can't
         *                         be done in STIL)
         */
        VariableReader( Variable var, boolean usesBlankValue ) {
            var_ = var;
            usesBlankValue_ = usesBlankValue;
            work_ = var.createRawValueArray();
        }

        /* Synchronize so the work array doesn't get trampled on.
         * Subclasses should synchronize too (synchronization is not
         * inherited). */
        synchronized Object readShapedRecord( int irec ) throws IOException {
            return var_.readShapedRecord( irec, STIL_ROW_MAJOR, work_ );
        }

        /**
         * Returns true iff this reader attempts to match values against
         * a magic blank value.
         *
         * @param  true  iff reader tries to use magic blanks
         */
        boolean usesBlankValue() {
            return usesBlankValue_;
        }
    }
}
