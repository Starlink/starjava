/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     26-FEB-2004 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.data;

import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.ast.FrameSet;

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
    private int coordColumn = -1;

    /** Index of the column containing the data values */
    private int dataColumn = -1;

    /** Index of the column containing the data errors */
    private int errorColumn = -1;

    /** Names of all the columns in the table */
    private String[] columnNames = null;

    /** ColumnInfo objects for all columns */
    private ColumnInfo[] columnInfos = null;

    /** Dimension of a column. */
    private int[] dims = new int[1];

    /**
     * Create an object by opening a resource and reading its
     * content.
     *
     * @param tablespec the name of the resource (file plus optional fragment).
     */
    public TableSpecDataImpl( String tablespec )
        throws SplatException
    {
        super( tablespec );
        this.fullName = tablespec;
        openTable( tablespec );
    }

    /**
     * Create an object by reading values from an existing SpecData
     * object. The target table is associated (so can be a save target),
     * but not opened.
     *
     * @param tablespec the name of the table.
     */
    public TableSpecDataImpl( String tablespec, SpecData source )
        throws SplatException
    {
        super( tablespec, source );
        this.fullName = tablespec;
    }

    /**
     * Return the data format.
     */
    public String getDataFormat()
    {
        return "TABLE"; // Can we be more specific?
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
                    readColumn( coords, coordColumn );
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
                    readColumn( data, dataColumn );
                    createAst();
                }
                break;
            }
        }
    }

    public String getDataErrorColumnName()
    {
        return columnNames[errorColumn];
    }

    public void setDataErrorColumnName( String name )
        throws SplatException
    {
        for ( int i = 0; i < columnNames.length; i++ ) {
            if ( columnNames[i].equals( name ) ) {
                if ( errorColumn != i ) {
                    errorColumn = i;
                    readColumn( errors, errorColumn );
                }
            }
        }
    }

    //
    // Implementation specific methods and variables.
    //

    /**
     * Reference to the StarTable.
     */
    protected StarTable starTable = null;

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
            readTable();
        }
        catch (Exception e) {
            throw new SplatException( "Failed to open table: "+tablespec, e );
        }
    }

    /**
     * Create a new table with the current content.
     *
     * @param tablespec name of the table.
     */
    protected void saveToTable( String tablespec )
        throws SplatException
    {
        // TODO write this part.
    }

    /**
     * Read in the data from the current table.
     */
    protected void readTable()
        throws SplatException
    {
        //  Access table columns and look for which to assign to the various
        //  data types. The default, if the matching fails, is to use the
        //  first and second (whatever that means) columns that are numeric
        //  types and do not look for any errors.
        columnInfos = Tables.getColumnInfos( starTable );
        columnNames = new String[columnInfos.length];
        for ( int i = 0; i < columnNames.length; i++ ) {
            columnNames[i] = columnInfos[i].getName();
        }

        coordColumn = 
            TableColumnChooser.getInstance().getCoordMatch( columnNames );
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
            TableColumnChooser.getInstance().getDataMatch( columnNames );
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

        //  No fallback for errors, just don't have any.
        errorColumn = 
            TableColumnChooser.getInstance().getErrorMatch( columnNames );
        
        //  Find the size of the table. Limited to 2G cells.
        dims[0] = (int) starTable.getRowCount();

        //  Access column data.
        coords = new double[dims[0]];
        readColumn( coords, coordColumn );

        data = new double[dims[0]];
        readColumn( data, dataColumn );

        if ( errorColumn != -1 ) {
            errors = new double[dims[0]];
            readColumn( errors, errorColumn );
        }

        //  Create the AST frameset that describes the data-coordinate
        //  relationship.
        createAst();
    }

    /**
     * Write spectral data to the file.
     *
     * @param file File object.
     */
    protected void writeData()
        throws SplatException
    {
        // TODO: anything.
    }

    /**
     * Read a column from the table into the given array.
     */
    protected void readColumn( double[] data, int index )
        throws SplatException
    {
        try {
            RowSequence rseq = starTable.getRowSequence();
            int i = 0;
            while( rseq.hasNext() ) {
                rseq.next();
                Number value = (Number) rseq.getCell( index );
                data[i++] = value.doubleValue();
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
            while( rseq.hasNext() ) {
                rseq.next();
                
                value = (Number) rseq.getCell( coordColumn );
                coords[i] = value.doubleValue();
                
                value = (Number) rseq.getCell( dataColumn );
                data[i] = value.doubleValue();
                
                if ( errorColumn != -1 ) {
                    value = (Number) rseq.getCell( errorColumn );
                    errors[i] = value.doubleValue();
                }
                i++;
            }
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

        astref.setCurrent( base );
        String unit = columnInfos[dataColumn].getUnitString();
        String desc = columnInfos[dataColumn].getDescription();
        if ( desc != null ) {
            astref.set( "label(1)=" + desc );

        }
        if ( unit != null ) {
            astref.set( "unit(1)=" + unit );
        }        

        astref.setCurrent( current );
        unit = columnInfos[coordColumn].getUnitString();
        desc = columnInfos[coordColumn].getDescription();
        if ( desc != null ) {
            astref.set( "label(1)=" + desc );

        }
        if ( unit != null ) {
            astref.set( "unit(1)=" + unit );
        }
    }
}
