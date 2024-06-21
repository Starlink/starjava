package uk.ac.starlink.table;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper table which takes any column whose value is N-element arrays
 * and turns it into N scalar-valued columns.
 *
 * @author   Mark Taylor (Starlink)
 * @since    2 Mar 2005
 */
public class ExplodedStarTable extends WrapperStarTable {

    private final StarTable baseTable_;
    private final ColPointer[] pointers_;

    /**
     * Constructs a table in which specified columns are exploded. 
     * All of the specified columns must
     * have values which are fixed-length arrays.
     *
     * @param  baseTable  base table
     * @param  colFlags   array of flags the same length as the number of
     *         columns in <code>baseTable</code>; true elements indicate
     *         columns in the base table which should be exploded
     * @throws  IllegalArgumentException  if any column specified by 
     *          <code>colFlags</code> has a type which is not
     *          a fixed-length array
     */
    public ExplodedStarTable( StarTable baseTable, boolean[] colFlags ) {
        super( baseTable );
        baseTable_ = baseTable;
        List<ColPointer> colList = new ArrayList<ColPointer>();
        for ( int icol = 0; icol < baseTable.getColumnCount(); icol++ ) {
            if ( colFlags[ icol ] ) {
                ColumnInfo baseInfo = baseTable.getColumnInfo( icol );
                String[] labels = baseInfo.isArray()
                                ? Tables.getElementLabels( baseInfo.getShape() )
                                : null;
                if ( labels != null ) {
                    for ( int j = 0; j < labels.length; j++ ) {
                        colList.add( new ColPointer( icol, j, labels[ j ] ) );
                    }
                }
                else {
                    throw new IllegalArgumentException( 
                        "Column cannot be exploded, not fixed-length array: " +
                        baseInfo );
                }
            }
            else {
                colList.add( new ColPointer( icol ) );
            }
        }
        pointers_ = colList.toArray( new ColPointer[ 0 ] );
    }

    /**
     * Constructs a table in which all fixed-length array-valued columns
     * are exploded.
     *
     * @param  baseTable  base table
     */
    public ExplodedStarTable( StarTable baseTable ) {
        this( baseTable, findExplodableColumns( baseTable ) );
    }

    public int getColumnCount() {
        return pointers_.length;
    }

    public ColumnInfo getColumnInfo( int icol ) {
        ColPointer pointer = pointers_[ icol ];
        ColumnInfo baseInfo = 
            baseTable_.getColumnInfo( pointer.getBaseIndex() );
        if ( pointer.getSubIndex() < 0 ) {
            return baseInfo;
        }
        else {
            int subIndex = pointer.getSubIndex();
            int subIndex1 = subIndex + 1;
            ColumnInfo info = new ColumnInfo( baseInfo );
            info.setContentClass( getComponentType( info.getContentClass() ) );
            info.setName( info.getName() + pointer.getLabel() );
            String desc = info.getDescription();
            if ( desc == null || desc.length() == 0 ) {
                desc = baseInfo.getName() + " element #" + subIndex1;
            }
            else {
                desc = desc + " (element #" + subIndex1 + ")";
            }
            info.setDescription( desc );
            return info;
        }
    }

    public Object getCell( long irow, int icol ) throws IOException {
        ColPointer pointer = pointers_[ icol ];
        Object baseCell = baseTable.getCell( irow, pointer.getBaseIndex() );
        return translateCell( pointer, baseCell );
    }

    public Object[] getRow( long irow ) throws IOException {
        return translateRow( baseTable.getRow( irow ) );
    }

    public RowSequence getRowSequence() throws IOException {
        RowSequence baseSeq = baseTable.getRowSequence();
        return new WrapperRowSequence( baseSeq, explodeMapper( baseSeq ) );
    }

    public RowAccess getRowAccess() throws IOException {
        RowAccess baseAcc = baseTable.getRowAccess();
        return new WrapperRowAccess( baseAcc, explodeMapper( baseAcc ) );
    }

    public RowSplittable getRowSplittable() throws IOException {
        RowSplittable baseSplit = baseTable.getRowSplittable();
        return new MappingRowSplittable( baseSplit, this::explodeMapper );
    }

    /**
     * Maps a RowData from the base table to a RowData for output from
     * this table.
     *
     * @param  baseData  base row data
     * @return  output row data
     */
    private RowData explodeMapper( final RowData baseData ) {
        return new RowData() {
            public Object getCell( int icol ) throws IOException {
                ColPointer pointer = pointers_[ icol ];
                Object baseCell = baseData.getCell( pointer.getBaseIndex() );
                return translateCell( pointer, baseCell );
            }
            public Object[] getRow() throws IOException {
                return translateRow( baseData.getRow() );
            }
        };
    }

    /**
     * Translates a base table cell into a cell in this table;
     * may involve dereferencing an array in the base table.
     *
     * @param  pointer for the column in question
     * @param  baseCell  value of the baseIndex cell in the base table
     * @return  value of the cell in this table
     */
    private Object translateCell( ColPointer pointer, Object baseCell ) {
        if ( pointer.getSubIndex() < 0 ||
             baseCell == null ||
             ! baseCell.getClass().isArray() ) {
            return baseCell;
        }
        else {
            return Array.get( baseCell, pointer.getSubIndex() );
        }
    }

    /**
     * Translates a base table row into a row in this table;
     * may involve dereferencing arrays in the base table.
     *
     * @param  baseRow  row in the base table
     * @return   row in this table
     */
    private Object[] translateRow( Object[] baseRow ) {
        int ncol = pointers_.length;
        Object[] row = new Object[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            ColPointer pointer = pointers_[ icol ];
            Object baseCell = baseRow[ pointer.getBaseIndex() ];
            row[ icol ] = translateCell( pointer, baseCell );
        }
        return row;
    }

    /**
     * Returns the element type of an array class, but providing wrapper
     * classes for the primitive types.
     *
     * @param  aclazz  array class
     * @return  element type
     */
    private static Class<?> getComponentType( Class<?> aclazz ) {
        Class<?> clazz = aclazz.getComponentType();
        if ( clazz == boolean.class ) {
            return Boolean.class;
        }
        else if ( clazz == byte.class ) {
            return Byte.class;
        }
        else if ( clazz == short.class ) {
            return Short.class;
        }
        else if ( clazz == int.class ) {
            return Integer.class;
        }
        else if ( clazz == long.class ) {
            return Long.class;
        }
        else if ( clazz == char.class ) {
            return Character.class;
        }
        else if ( clazz == float.class ) {
            return Float.class;
        }
        else if ( clazz == double.class ) {
            return Double.class;
        }
        else {
            return clazz;
        }
    }

    /**
     * Locates columns in a table which are suitable for explosion.
     *
     * @param  table  table to investigate
     * @return   array of flags, one for each column in <code>table</code>,
     *           each element true only if the type of the corresponding 
     *           column is a fixed-length array
     */
    private static boolean[] findExplodableColumns( StarTable table ) {
        int ncol = table.getColumnCount();
        boolean[] colFlags = new boolean[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            ColumnInfo info = table.getColumnInfo( icol );
            colFlags[ icol ] = 
                info.isArray() &&
                Tables.getElementLabels( info.getShape() ) != null;
        }
        return colFlags;
    }

    /**
     * Helper class which describes a column in this table by indexing 
     * into a possibly array-valued column in the base table.
     */
    private static class ColPointer {
        final int baseIndex_;
        final int subIndex_;
        final String label_;

        /**
         * Constructor for array-valued base column.
         *
         * @param  baseIndex  index of the column in the base table 
         *         corresponding to this column
         * @param  subIndex  index of the element in the array value of the
         *         <code>baseIndex</code> column in the base table; -1 if
         *         it's scalar
         * @param  label  suffix describing the position of the subindex
         */
        public ColPointer( int baseIndex, int subIndex, String label ) {
            baseIndex_ = baseIndex;
            subIndex_ = subIndex;
            label_ = label;
        }

        /**
         * Constructor for scalar-valued base column.
         *
         * @param  baseIndex index of the scalar column in the base table
         */
        public ColPointer( int baseIndex ) {
            this( baseIndex, -1, null );
        }

        /**
         * Returns the index of the base table column this column points to.
         *
         * @return   base index
         */
        public int getBaseIndex() {
            return baseIndex_;
        }

        /**
         * Returns the index into the array value of the base column.
         *
         * @return  subindex
         */
        public int getSubIndex() {
            return subIndex_;
        }

        public String getLabel() {
            return label_;
        }
    }
    
}
