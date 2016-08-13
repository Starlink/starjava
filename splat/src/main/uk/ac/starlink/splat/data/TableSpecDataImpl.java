/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 * Copyright (C) 2007 Particle Physics and Astronomy Research Council
 *
 *  History:
 *     26-FEB-2004 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.data;

import java.util.Arrays;
import java.util.List;
import java.io.IOException;

import org.apache.axis.utils.ArrayUtil;

import nom.tam.fits.Header;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.splat.util.SEDSplatException;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.util.UnitUtilities;
import uk.ac.starlink.table.ArrayColumn;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.UCD;

/**
 * This class provides access to spectral data stored in tables.
 * <p>
 * The tables supported are any that the {@link uk.ac.starlink.table}
 * package supports.
 * <p>
 * As tables can contain many columns it is necessary to provide for
 * the selection of coordinate, value and error columns. These are
 * provided through the standard {@link SpecDataImpl}.
 *
 * @version $Id$
 * @see SpecDataImpl
 * @see SpecData
 */
public class TableSpecDataImpl
    extends MEMSpecDataImpl
{
    /** Index of the column containing the coordinates */
    protected int coordColumn = -1;

    /** Index of the column containing the data values */
    protected int dataColumn = -1;

    /** Index of the column containing the data errors */
    protected int errorColumn = -1;

    /** Names of all the columns in the table */
    protected String[] columnNames = null;

    /** ColumnInfo objects for all columns */
    protected ColumnInfo[] columnInfos = null;

    /** Dimension of a column. */
    protected int[] dims = new int[1];

    /** Counter for generating application unique names */
    static int uniqueCount = 0;

    /** Type to save table as (FITS etc.) */
    protected String saveType = null;

    /**
     * Create an object by opening a resource and reading its
     * content. Note this throws a SEDSplatException, if the table is
     * suspected of being an SED (that is it contain vector cells)
     * with more than one row.
     *
     * @param tablespec the name of the resource (file plus optional fragment).
     */
    public TableSpecDataImpl( String tablespec )
        throws SplatException
    {
        super( tablespec );
        openTable( tablespec );
        setName( starTable );
    }

    /**
     * Create an object by opening a resource and reading a specific row for
     * the contents of this spectrum. Assumes the row contains vector
     * information.
     *
     * @param tablespec the name of the resource (file plus optional fragment).
     * @param row the row of values to read.
     */
    public TableSpecDataImpl( String tablespec, long row )
        throws SplatException
    {
        super( tablespec );
        openTable( tablespec, row );
        setName( starTable );
    }

    /**
     * Create an object by reading values from an existing SpecData
     * object. The target table is associated (so can be a
     * {@link #save} target), but not written to.
     *
     * @param tablespec the name of the table.
     * @param source the SpecData.
     * @param saveType the type that should be used if this is saved to disk.
     */
    public TableSpecDataImpl( String tablespec, SpecData source,
                              String saveType )
        throws SplatException
    {
        super( tablespec, source );
        this.fullName = tablespec;
        this.saveType = saveType;
        makeMemoryTable();
    }

    /**
     * Create an object by reusing an existing instance if StarTable.
     *
     * @param starTable reference to a {@link StarTable}.
     */
    public TableSpecDataImpl( StarTable starTable )
        throws SplatException
    {
        super( starTable.getName() );
        setName( starTable );
        openTable( starTable );
    }

    /**
     * Create an object by reusing an existing instance if StarTable,
     * reading only one row (SDFITS).
     *
     * @param starTable reference to a {@link StarTable}.
     */
    public TableSpecDataImpl( StarTable starTable,  long row )
        throws SplatException
    {
        super( starTable.getName() );
        setName( starTable );
        openTable( starTable , row);
    }

    /**
     * Create an object by reusing an existing instance if StarTable.
     *
     * @param starTable reference to a {@link StarTable}.
     * @param shortName a short name for the table.
     * @param fullName a long name for the table.
     */
    public TableSpecDataImpl( StarTable starTable, String shortName,
                              String fullName )
        throws SplatException
    {
        this(starTable, shortName, fullName, null);
    }
     
     /**
      * Create an object by reusing an existing instance of StarTable.
      *
      * @param starTable reference to a {@link StarTable}.
      * @param shortName a short name for the table.
      * @param fullName a long name for the table.
      * @param fitsHeaders original FITS headers that will be used if no accumulated
      */
     public TableSpecDataImpl( StarTable starTable, String shortName,
                               String fullName, Header fitsHeaders )
         throws SplatException
     {
        super( shortName );
        openTable( starTable );
        this.shortName = shortName;
        this.fullName = fullName;
        this.originalFitsHeaders = fitsHeaders;
    }

    /**
     * Return the data format.
     */
    public String getDataFormat()
    {
        return "TABLE";
    }

    /**
     * Save the spectrum to disk.
     */
    public void save()
        throws SplatException
    {
        saveToTable( fullName );
    }

    public int[] getDims()
    {
        return dims;
    }

    //
    // Column mutability interface.
    //

    public String[] getColumnNames()
    {
        return columnNames;
    }

    public String getCoordinateColumnName()
    {
        
        return columnNames[coordColumn];
    }

    public void setCoordinateColumnName( String name )
        throws SplatException
    {
        for ( int i = 0; i < columnNames.length; i++ ) {
            if ( columnNames[i].equals( name ) ) {
               
                if ( coordColumn != i ) {
                    coordColumn = i;
                    long nrrows = starTable.getRowCount();
                    if (nrrows==1) {
                        coords = readCell( 0, coordColumn );
                       
                    } else {
                         readColumn( coords, coordColumn );
                   }
                    createAst();
                }
                break;
            }
        }
    }

    public String getDataColumnName()
    {
        return columnNames[dataColumn];
    }

    public void setDataColumnName( String name )
        throws SplatException
    {
        for ( int i = 0; i < columnNames.length; i++ ) {
            if ( columnNames[i].equals( name ) ) {
                if ( dataColumn != i ) {
                    dataColumn = i;
                    long nrrows = starTable.getRowCount();
                    if (nrrows==1) {
                        data = readCell( 0, dataColumn );
                        
                    } else {
                        readColumn( data, dataColumn );  
                   }
                    createAst();
                }
                break;
            }
        }
    }

    public String getDataErrorColumnName()
    {
        if ( errorColumn != -1 ) {
            return columnNames[errorColumn];
        }
        return "";
    }

    public void setDataErrorColumnName( String name )
        throws SplatException
    {
        // A null or empty string means no errors.
        if ( name == null || name.equals( "" ) ) {
            errorColumn = -1;
            errors = null;
        }
        else {
            for ( int i = 0; i < columnNames.length; i++ ) {
                if ( columnNames[i].equals( name ) ) {
                    if ( errorColumn != i ) {
                        errorColumn = i;
                        if ( errors == null && ! name.equals( "" ) ) {
                            errors = new double[dims[0]];
                        } 
                        long nrrows = starTable.getRowCount();
                        if (nrrows==1) {                     
                                errors = readCell( 0, errorColumn );
                        } else {
                       
                            readColumn( errors, errorColumn );
                        }
                    }
                    break;
                }
            }
        }
    }

    /**
     * Get the format of this table.
     */
    public String getTableFormat()
        throws SplatException
    {
        throw new SplatException( "Implement this now!" );
    }

    /**
     * Get the list of formats that are supported.
     */
    public static List getKnownFormats()
    {
        return writer.getKnownFormats();
    }

    @Override
    public Header getFitsHeaders() {
        Header accumulatedHeaders = super.getFitsHeaders();
        // if no (or empty) accumulated headers, return the original headers (if any)
        if (accumulatedHeaders != null
                && accumulatedHeaders.getDataSize() == 0
                && originalFitsHeaders != null) {
            return originalFitsHeaders;
        }
        return accumulatedHeaders;
    }
    
    //
    // Implementation specific methods and variables.
    //

    /**
     * Reference to the StarTable.
     */
    protected StarTable starTable = null;
    
   /**
    * Reference to the original FITS headers of the input FITS file.
    */
   protected Header originalFitsHeaders = null;
   
    /**
     * Writer for tables.
     */
    protected final static StarTableOutput writer = new StarTableOutput();

    /**
     * Set the full and short names of this object from the table if
     * possible, if not use a generated name.
     */
    protected void setName( StarTable starTable )
    {
        fullName = null;
        shortName = starTable.getName();
        if ( starTable.getURL() != null ) {
            fullName = starTable.getURL().toString();
        }
        if ( fullName == null || fullName.equals( "" ) ) {
            fullName = "StarTable " + uniqueCount++;
        }
        if ( shortName == null || shortName.equals( "" ) ) {
            shortName = fullName;
        }
    }

    /**
     * Open a table. Throws a SEDSplatException, if the table may be an SED
     */
    protected void openTable( StarTable starTable )
        throws SplatException
    {
        //  Table needs random access so we can size it for making local
        //  copies of the data in the columns. This is a nullop if the table
        //  is already random.
        try {
            this.starTable = Tables.randomTable( starTable );
          //  if (starTable.getName().equals("SINGLE DISH")) { // read SDFITS format
         //       readSDTable(-1);
       //     } else {          
                readTable( -1 );
       //     }
        }
        catch (SEDSplatException e) {
            throw e;
        }
        catch (Exception e) {
            throw new SplatException( "Failed to open table: " +
                                      starTable.getName(), e );
        }
    }
    
    /**
     * Open a table. Throws a SEDSplatException, if the table may be an SED
     */
    protected void openTable( StarTable starTable, long row )
        throws SplatException
    {
        // do nothing right now
    }

    /**
     * Open a table.
     *
     * @param tablespec name of the table.
     */
    protected void openTable( String tablespec )
        throws SplatException
    {
        //  Table needs random access so we can size it for making local
        //  copies of the data in the columns.
        try {
            starTable = new StarTableFactory(true).makeStarTable( tablespec );
            readTable( -1 );
        }
        catch (Exception e) {
                throw new SplatException("Failed to open table: " + tablespec, e);
        }
    }

    /**
     * Open a table and read a row of vector information to create the
     * spectrum (part of an SED stored in a table).
     *
     * @param tablespec name of the table.
     * @param row the row to read.
     */
    protected void openTable( String tablespec, long row )
        throws SplatException
    {
        //  Table needs random access so we can size it for making local
        //  copies of the data in the columns.
        try {
            starTable = new StarTableFactory(true).makeStarTable( tablespec );
            readTable( row );
        }
        catch (Exception e) {
            throw new SplatException("Failed to open table: " + tablespec, e);
        }
    }

    /**
     * Make the current columns (stored in memory) into a table. This
     * table then becomes the current table.
     */
    protected void makeMemoryTable()
    {
        columnInfos = new ColumnInfo[3];

        if ( parentImpl instanceof TableSpecDataImpl ) {
            //  Source implementation is a table. Use the available
            //  ColumnInfos.
            TableSpecDataImpl tsdi = (TableSpecDataImpl) parentImpl;
            columnInfos[0] = 
                new ColumnInfo( tsdi.columnInfos[tsdi.coordColumn] );
            columnInfos[1] = 
                new ColumnInfo( tsdi.columnInfos[tsdi.dataColumn] );
            if ( tsdi.errorColumn != -1 ) {
                columnInfos[2] =
                    new ColumnInfo( tsdi.columnInfos[tsdi.errorColumn] );
            }
            else {
                columnInfos[2] = null;
            }

            columnInfos[0].setContentClass( Double.class );
            columnInfos[1].setContentClass( Double.class );
            if ( columnInfos[2] != null ) {
                columnInfos[2].setContentClass( Double.class );
            }
        }
        else {
            //  Values based on AST FrameSet description.
            FrameSet frameSet = parentImpl.getAst();

            // Coordinates
            String label = frameSet.getLabel( 1 );
            if ( label == null ) {
                label = "Coordinates";
            }
            String units = frameSet.getUnit( 1 );
            columnInfos[0] = new ColumnInfo( label, Double.class,
                                             "Spectral coordinates" );
            if ( units != null ) {
                columnInfos[0].setUnitString(UnitUtilities.fixUpUnits(units));
            }

            int current = frameSet.getCurrent();
            int base = frameSet.getBase();
            frameSet.setCurrent( base );

            //  Data values.
            label = frameSet.getLabel( 1 );
            if ( label == null ) {
                label = "Values";
            }
            dataLabel = label;

            units = frameSet.getUnit( 1 );
            columnInfos[1] = new ColumnInfo( label, Double.class,
                                             "Spectral data values" );
            if ( units != null ) {
                setDataUnits( units );
                columnInfos[1].setUnitString( dataUnits );
            }
            frameSet.setCurrent( current );

            //  Errors.
            if ( errors != null ) {
                label = "Errors";
                columnInfos[2] = new ColumnInfo( label, Double.class,
                                                 "Spectral data errors" );
                if ( units != null ) {
                    columnInfos[2]
                        .setUnitString( UnitUtilities.fixUpUnits( units ) );
                }
            }
            else {
                columnInfos[2] = null;
            }
        }
        int nrows = coords.length;
        ColumnStarTable newTable = ColumnStarTable.makeTableWithRows( nrows );
        newTable.addColumn( ArrayColumn.makeColumn( columnInfos[0], coords ) );
        newTable.addColumn( ArrayColumn.makeColumn( columnInfos[1], data ) );
        if ( errors != null && columnInfos[2] != null ) {
            newTable.addColumn( ArrayColumn.makeColumn( columnInfos[2],
                                                        errors ) );
        }
        starTable = newTable;
    }

    /**
     * Create a new table with the current content.
     *
     * @param tablespec name of the table.
     */
    protected void saveToTable( String tablespec )
        throws SplatException
    {
        try {
            writer.writeStarTable( starTable, tablespec, saveType );
            readTable( -1 );
        }
        catch (Exception e) {
            throw new SplatException( "Error saving table", e );
        }
    }

    /**
     * Read in the data from the current table. If the row is unspecified then
     * try to read the full table, otherwise assume the indicated row contains
     * a series of vector cells describing the spectrum.
     */
    protected void readTable( long row )
        throws SplatException
    {
    	// Detect timeseries
    	for (Object oParam : starTable.getParameters()) {
    		if (oParam instanceof DescribedValue) {
    			DescribedValue param = (DescribedValue) oParam;
    			if (param.getInfo().getName().equals("data_product_type")) {
    				if (param.getValue() != null && param.getValue().equals("timeseries")) {
    					setObjectType(ObjectTypeEnum.TIMESERIES);
    				}
    			}
    		}
    	}
    	
    	//  Access table columns and look for which to assign to the various
        //  data types. The default, if the matching fails, is to use the
        //  first and second (whatever that means) columns that are numeric
        //  types and do not look for any errors. Do not allow spaces in 
        //  column names. These cause trouble with lists of names.
    	
        columnInfos = Tables.getColumnInfos( starTable );
        columnNames = new String[columnInfos.length];
        for ( int i = 0; i < columnNames.length; i++ ) {
            columnNames[i] = columnInfos[i].getName().replaceAll( "\\s", "_" );
        }

        coordColumn =
            TableColumnChooser.getInstance().getCoordMatch( columnInfos, 
                                                            columnNames );
        if ( coordColumn == -1 ) {
            // No match for coordinates, look for "first" numeric column.
            for ( int i = 0; i < columnInfos.length; i++ ) {
                if ( Number.class.isAssignableFrom
                     ( columnInfos[i].getContentClass() ) ) {
                    coordColumn = i;
                    break;
                }
            }
        }

        dataColumn =
            TableColumnChooser.getInstance().getDataMatch( columnInfos,
                                                           columnNames );
        if ( dataColumn == -1 ) {
            // No match for data, look for "second" numeric column.
            int count = 0;
            for ( int i = 0; i < columnInfos.length; i++ ) {
                if ( Number.class.isAssignableFrom
                     ( columnInfos[i].getContentClass() ) ) {
                    if ( count > 0 ) {
                        dataColumn = i;
                        break;
                    }
                    count++;
                }
            }
        }

        if ( coordColumn == -1 || dataColumn == -1 ) {
            throw new SplatException
                ( "Tables must contain at least two numeric columns" );
        }

        if ( coordColumn == dataColumn  ) {
            throw new SplatException
                ( "Tables must contain at least two numeric columns" );
        }

        //  No fallback for errors, just don't have any.
        errorColumn =
            TableColumnChooser.getInstance().getErrorMatch( columnInfos,
                                                            columnNames );

        //  Find the size of the table. Limited to 2G cells.
        dims[0] = (int) starTable.getRowCount();
        
      
        //  If dims is 1, the data could be be vector cells. Allow a test for
        //  that, but we do the next part whenever we have a row.
        if ( dims[0] == 1 && row == -1 ) {
            row = 0;
        }
        if ( row != -1 ) {
            coords = readCell( row, coordColumn );
            data = readCell( row, dataColumn );
            if ( errorColumn != -1 ) {
                errors = readCell( row, errorColumn );
            }
            dims[0] = coords.length;
        }
        else {

            //  Access column data, one value per cell. What about SEDs?
            coords = new double[dims[0]];
            readColumn( coords, coordColumn );

            data = new double[dims[0]];
            readColumn( data, dataColumn );

            if ( errorColumn != -1 ) {
                errors = new double[dims[0]];
                readColumn( errors, errorColumn );
            }
        }

        //  Create the AST frameset that describes the data-coordinate
        //  relationship.
        createAst();
    }

 
    
    /**
     * Read an array of data from a vector cell. Returns the values as a
     * double array.
     */ 
    protected double[] readCell( long row, int column )
        throws SplatException
    {
        double[] data;
        Object cellData = null;
        try {
            cellData = starTable.getCell( row, column );
        }
        catch (IOException e) {
            throw new SplatException( e );
        }
     
        if ( cellData instanceof double[] ) {
            data = (double []) cellData;
            for ( int i = 0; i < data.length; i++ ) {
                if (Double.isNaN(data[i]))
                    data[i]=SpecData.BAD;
            }
            
        }
        else if ( cellData instanceof float[] ) {
            float[] fdata = (float []) cellData;
            data = new double[fdata.length];
            for ( int i = 0; i < fdata.length; i++ ) {
                if ( ! Float.isNaN(fdata[i]))  // NaN values will be BAD. 
                    data[i] = (double) fdata[i];
                else 
                    data[i]=SpecData.BAD;
            }
        }
        else if ( cellData instanceof int[] ) {
            int[] fdata = (int []) cellData;
            data = new double[fdata.length];
            for ( int i = 0; i < fdata.length; i++ ) {
                data[i] = (double) fdata[i];
                if (Double.isNaN(data[i]))
                    data[i]=SpecData.BAD;
            }
        }
        else if ( cellData instanceof long[] ) {
            long[] fdata = (long []) cellData;
            data = new double[fdata.length];
            for ( int i = 0; i < fdata.length; i++ ) {
                data[i] = (double) fdata[i];
                if (Double.isNaN(data[i]))
                    data[i]=SpecData.BAD;
            }
        }
        else if ( cellData instanceof short[] ) {
            short[] fdata = (short []) cellData;
            data = new double[fdata.length];
            for ( int i = 0; i < fdata.length; i++ ) {
                data[i] = (double) fdata[i];
                if (Double.isNaN(data[i]))
                    data[i]=SpecData.BAD;
            }
        }
        else if ( cellData instanceof byte[] ) {
            byte[] fdata = (byte []) cellData;
            data = new double[fdata.length];
            for ( int i = 0; i < fdata.length; i++ ) {
                data[i] = (double) fdata[i];
                if (Double.isNaN(data[i]))
                    data[i]=SpecData.BAD;
            }
        }
        else {
            throw new SplatException
                ( "Vector table cell format not primitive" );
        }
        return data;
    }

    /**
     * Test if a Object is really a primitive array of values.
     */
    protected boolean isPrimitiveArray( Object object )
    {
        boolean result = false;
        if ( object instanceof double[] ) {
            result = true;
        }
        else if ( object instanceof float[] ) {
            result = true;
        }
        else if ( object instanceof int[] ) {
            result = true;
        }
        else if ( object instanceof long[] ) {
            result = true;
        }
        else if ( object instanceof short[] ) {
            result = true;
        }
        else if ( object instanceof byte[] ) {
            result = true;
        }
        return result;
    }

    /**
     * Read a column from the table into the given array. Any NaN values are
     * remapped to BAD.
     */
    protected void readColumn( double[] data, int index )
        throws SplatException
    {
        Object cellData = null;
        try {
            RowSequence rseq = starTable.getRowSequence();
            int i = 0;
            double v = 0.0;
            while( rseq.next() ) {
                cellData = rseq.getCell( index );
                Number value = (Number) cellData;
                v = value.doubleValue();
                if ( v != v ) {  // Double.isNaN
                    v = SpecData.BAD;
                }
                data[i++] = v;
            }
            rseq.close();
        }
        catch (ClassCastException e) {
            //  Isn't a Number, is it a vector?
            if ( isPrimitiveArray( cellData ) ) {
                
                if (starTable.getRowCount()==1) {
                                    
                    //  Read vector data from first row. XXX handle SED.
                    double newdata[] = readCell( 0, index );
                    System.arraycopy( newdata, 0, data, 0, newdata.length );
                } else {
                    throw new SEDSplatException( dims[0], 
                    "Table contains vector cells, assuming it is an SED" );
                }
            }
            else {
                throw new SplatException
                    ( "Table column ("+ index +")contains an unknown data type" );
            }
        }
        catch (Exception e) {
            throw new SplatException( "Failed reading table column" , e );
        }
    }

    /**
     * Read all the data from the table. Efficient, does one pass.
     */
    protected void readAllColumns()
        throws SplatException
    {
        try {
            RowSequence rseq = starTable.getRowSequence();
            int i = 0;
            Number value;
            double v;
            while( rseq.next() ) {

                value = (Number) rseq.getCell( coordColumn );
                v = value.doubleValue();
                if ( v != v ) {  // Double.isNaN
                    v = SpecData.BAD;
                }
                coords[i] = v;


                value = (Number) rseq.getCell( dataColumn );
                v = value.doubleValue();
                if ( v != v ) {  // Double.isNaN
                    v = SpecData.BAD;
                }
                data[i] = v;

                if ( errorColumn != -1 ) {
                    value = (Number) rseq.getCell( errorColumn );
                    v = value.doubleValue();
                    if ( v != v ) {  // Double.isNaN
                        v = SpecData.BAD;
                    }
                    errors[i] = v;
                }
                i++;
            }
            rseq.close();
        }
        catch (Exception e) {
            throw new SplatException( "Failed reading table", e );
        }
    }

    protected void createAst()
    {
        super.createAst();

        // Add what we can find out about the units of the coordinates
        // and data and add these to the current Frame. These may be
        // changed into a SpecFrame by the SpecData wrapper.
        int current = astref.getCurrent();
        int base = astref.getBase();

        //  Base frame. Indices of data values array.
        astref.setCurrent( base );
        if (dataColumn >=0)
              guessUnitsDescription( dataColumn );

        //  Set the units and label for data.
        setDataUnits( astref.getUnit( 1 ) );
        setDataLabel( astref.getLabel( 1 ) );
        
        //  Coordinate units.
        astref.setCurrent( current );
        if (dataColumn >=0)
            guessUnitsDescription( coordColumn );
    }

    /**
     * Guess the unit and description of a given column and assign
     * then to the current frame of the AST frameset.
     */
    protected void guessUnitsDescription( int column )
    {
        // Units exist or not. If so apply the fixup heuristics.
        String unit = columnInfos[column].getUnitString();
        if ( unit != null && ! "".equals( unit ) ) {
            astref.setUnit( 1, UnitUtilities.fixUpUnits( unit ) );
        }

        // If the description doesn't exist, then try for a UCD
        // description, if that doesn't exist just use the column name.
        String desc = columnInfos[column].getDescription();
        if ( desc == null ) {
            desc = columnInfos[column].getUCD();
            if ( desc != null ) {
                UCD ucd = UCD.getUCD( desc );
                if ( ucd == null ) {
                    desc = null;
                }
                else {
                    desc = ucd.getDescription();
                }
            }
            if ( desc == null ) {
                desc = columnInfos[column].getName();
            }
        }

        if ( desc != null && ! "".equals( desc ) ) {
            astref.setLabel( 1, desc );
        }
    }
}
