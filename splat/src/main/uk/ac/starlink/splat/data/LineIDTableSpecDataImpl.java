/*
 * Copyright (C) 2007 Science and Technology Facilities Council
 *
 *  History:
 *     26-JUN-2007 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.data;

import java.util.Arrays;
import java.util.List;
import java.io.IOException;

import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.util.UnitUtilities;
import uk.ac.starlink.table.ArrayColumn;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.UCD;

/**
 * A {@link LineIDSpecDataImpl} that provides facilities for reading tables
 * of various kinds that have a coordinate, value and an associated String,
 * typically a spectral line identification.
 *
 * @version $Id$
 * @see SpecDataImpl
 * @see SpecData
 */
public class LineIDTableSpecDataImpl
    extends LineIDMEMSpecDataImpl
{
    /** Index of the column containing the coordinates */
    private int coordColumn = -1;

    /** Index of the column containing the optional data values */
    private int dataColumn = -1;

    /** Index of the column containing the line identifier labels */
    private int labelColumn = -1;

    /** Names of all the columns in the table */
    private String[] columnNames = null;

    /** ColumnInfo objects for all columns */
    private ColumnInfo[] columnInfos = null;

    /** Dimension of a column. */
    private int[] dims = new int[1];

    /** Counter for generating application unique names */
    static int uniqueCount = 0;

    /** Type to save table as (i.e. disk file format) */
    protected String saveType = null;

    /**
     * Create an object by opening a resource and reading its
     * content.
     *
     * @param tablespec the name of the resource (file plus optional fragment).
     */
    public LineIDTableSpecDataImpl( String tablespec )
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
    public LineIDTableSpecDataImpl( String tablespec, SpecData source,
                                    String saveType )
        throws SplatException
    {
        super( tablespec, source );
        this.fullName = tablespec;
        this.saveType = saveType;
        makeMemoryTable();
    }

    /**
     * Create an object by reusing an existing instance if a StarTable.
     *
     * @param starTable reference to a {@link StarTable}.
     */
    public LineIDTableSpecDataImpl( StarTable starTable )
        throws SplatException
    {
        super( starTable.getName() );
        setName( starTable );
        openTable( starTable );
    }

    /**
     * Create an object by reusing an existing instance if a StarTable.
     *
     * @param starTable reference to a {@link StarTable}.
     * @param shortName a short name for the table.
     * @param fullName a long name for the table.
     */
    public LineIDTableSpecDataImpl( StarTable starTable, String shortName,
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
        return "Line Identifiers (Table)";
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
        if (dataColumn != -1)
            return columnNames[dataColumn];
        return "";
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
        return null;
    }

    public void setDataErrorColumnName( String name )
        throws SplatException
    {
        //  Do nothing.
    }

    // XXX Label column mutability.

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

        if ( parentImpl instanceof LineIDTableSpecDataImpl ) {
            //  Source implementation is a table. Use the available
            //  ColumnInfos.
            LineIDTableSpecDataImpl tsdi =
                (LineIDTableSpecDataImpl) parentImpl;
            columnInfos[0] = new ColumnInfo( tsdi.columnInfos[0] );
            columnInfos[1] = new ColumnInfo( tsdi.columnInfos[1] );
            if ( tsdi.columnInfos.length > 2 && tsdi.columnInfos[2] != null ) {
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
                columnInfos[0].setUnitString(UnitUtilities.fixUpUnits(units));
            }

            int current = frameSet.getCurrent();
            int base = frameSet.getBase();
            frameSet.setCurrent( base );

            //  Data values.
            if ( data != null ) {
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
            }
            else {
                columnInfos[1] = null;
            }

            //  Labels.
            label = "Labels";
            columnInfos[2] = new ColumnInfo( label, Double.class,
                                             "Spectral line identifiers" );
        }

        int nrows = coords.length;
        ColumnStarTable newTable = ColumnStarTable.makeTableWithRows( nrows );
        newTable.addColumn( ArrayColumn.makeColumn( columnInfos[0], coords ) );
        if ( data != null ) {
            newTable.addColumn(ArrayColumn.makeColumn( columnInfos[1], data ));
        }
        newTable.addColumn( ArrayColumn.makeColumn( columnInfos[2], labels ) );
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
        //  data. The default, if the matching fails, is to use the
        //  first and second (whatever that means) columns and use those.
        columnInfos = Tables.getColumnInfos( starTable );
        columnNames = new String[columnInfos.length];
        for ( int i = 0; i < columnNames.length; i++ ) {
            columnNames[i] = columnInfos[i].getName();
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
     /*   if ( dataColumn == -1 ) {
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
        }*/

        if ( coordColumn == -1 ) {
            throw new SplatException( "Line identifier tables must "+
                                      "contain at least one numeric column" );
        }

        //  Look for the labels.
        labelColumn =
            TableColumnChooser.getInstance().getLabelMatch( columnInfos,
                                                            columnNames );

        //  Find the size of the table. Limited to 2G cells.
        dims[0] = (int) starTable.getRowCount();

        //  Access column data, one value per cell.
        coords = new double[dims[0]];
        readColumn( coords, coordColumn );

        if ( dataColumn != -1 ) {
            data = new double[dims[0]];
            readColumn( data, dataColumn );
        } else {
            data = new double[dims[0]];
            Arrays.fill(data, SpecData.BAD);
        }

        labels = new String[dims[0]];
        readColumn( labels, labelColumn );

        //  Create the AST frameset that describes the data-coordinate
        //  relationship.
        createAst();
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
        catch (Exception e) {
            throw new SplatException( "Failed reading table column" , e );
        }
    }

    /**
     * Read a column of strings from the table into the given array.
     */
    protected void readColumn( String[] labels, int index )
        throws SplatException
    {
        Object cellData = null;
        try {
            RowSequence rseq = starTable.getRowSequence();
            int i = 0;
            while( rseq.next() ) {
                labels[i++] = (String) rseq.getCell( index );
            }
            rseq.close();
        }
        catch (Exception e) {
            throw new SplatException( "Failed reading table column" , e );
        }
    }

    // XXX need to set attributes like in LineIDTXTSpecDataImpl.
    // if want to match coordinate systems.

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
        
        if (dataColumn != -1) {
            guessUnitsDescription( dataColumn );

            //  Set the units and label for data.
            setDataUnits( astref.getUnit( 1 ) );
            setDataLabel( astref.getLabel( 1 ) );
        }
        
        //  Coordinate units.
        astref.setCurrent( current );
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
                if ( ucd != null ) {
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
