/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     26-FEB-2004 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.data;

import java.util.List;

import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.table.ArrayColumn;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.StarTableFactory;
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

    /** Counter for generating application unique names */
    protected static int uniqueCount = 0;

    /** Type to save table as (FITS etc.) */
    protected String saveType = null;

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
        openTable( tablespec );
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
        super( shortName );
        openTable( starTable );
        this.shortName = shortName;
        this.fullName = fullName;
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
                        readColumn( errors, errorColumn );
                    }
                    break;
                }
            }
        }
    }

    /**
     * Get the list of formats that are supported.
     */
    public static List getKnownFormats()
    {
        return writer.getKnownFormats();
    }

    //
    // Implementation specific methods and variables.
    //

    /**
     * Reference to the StarTable.
     */
    protected StarTable starTable = null;

    /**
     * Writer for tables.
     */
    protected static StarTableOutput writer = new StarTableOutput();

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
     * Open a table.
     */
    protected void openTable( StarTable starTable )
        throws SplatException
    {
        //  Table needs random access so we can size it for making local
        //  copies of the data in the columns. This is a nullop if the table
        //  is already random.
        try {
            this.starTable = Tables.randomTable( starTable );
            readTable();
        }
        catch (Exception e) {
            throw new SplatException( "Failed to open table: " +
                                      starTable.getName(), e );
        }
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
            readTable();
        }
        catch (Exception e) {
            throw new SplatException( "Failed to open table: "+tablespec, e );
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
            columnInfos[0] = new ColumnInfo( tsdi.columnInfos[0] );
            columnInfos[1] = new ColumnInfo( tsdi.columnInfos[1] );
            if ( tsdi.columnInfos[2] != null ) {
                columnInfos[2] = new ColumnInfo( tsdi.columnInfos[2] );
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
                columnInfos[0].setUnitString( units );
            }

            int current = frameSet.getCurrent();
            int base = frameSet.getBase();
            frameSet.setCurrent( base );

            //  Data values.
            label = frameSet.getLabel( 1 );
            if ( label == null ) {
                label = "Values";
            }
            units = frameSet.getUnit( 1 );
            columnInfos[1] = new ColumnInfo( label, Double.class,
                                             "Spectral data values" );
            if ( units != null ) {
                columnInfos[1].setUnitString( units );
            }
            frameSet.setCurrent( current );

            //  Errors.
            if ( errors != null ) {
                label = "Errors";
                columnInfos[2] = new ColumnInfo( label, Double.class,
                                                 "Spectral data errors" );
                if ( units != null ) {
                    columnInfos[2].setUnitString( units );
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
        if ( errors != null || columnInfos[2] != null ) {
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
        }
        catch (Exception e) {
            throw new SplatException( "Error saving table", e );
        }
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

        //  Base frame. Indices of data values array.
        astref.setCurrent( base );
        guessUnitsDescription( dataColumn );
        astref.setCurrent( current );
        guessUnitsDescription( coordColumn );
    }

    /**
     * Guess the unit and description of a given column and assign
     * then to the current frame of the AST frameset.
     */
    protected void guessUnitsDescription( int column )
    {
        // Units exist or not.
        String unit = columnInfos[column].getUnitString();
        if ( unit != null && ! "".equals( unit ) ) {
            astref.setUnit( 1, unit );
        }

        // If the description doesn't exist, then try for a UCD
        // description, if that doesn't exist just use the column name.
        String desc = columnInfos[column].getDescription();
        if ( desc == null ) {
            desc = columnInfos[column].getUCD();
            if ( desc != null ) {
                desc = UCD.getUCD( desc ).getDescription();
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
