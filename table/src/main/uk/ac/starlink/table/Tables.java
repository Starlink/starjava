package uk.ac.starlink.table;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for miscellaneous table-related functionality.
 */
public class Tables {

    /**
     * Returns a table based on a given table and guaranteed to have 
     * random access.  If the original table <tt>stab</tt> has random
     * access then it is returned, otherwise a new random access table
     * is built using its data.
     *
     * @param  stab  original table
     * @return  a table with the same data as <tt>startab</tt> and with 
     *          <tt>isRandom()==true</tt>
     */
    public static StarTable randomTable( StarTable startab )
            throws IOException {

        /* If it has random access already, we don't need to do any work. */
        if ( startab.isRandom() ) {
            return startab;
        }

        /* Otherwise, we need to construct a table based on the sequential
         * table that acts random. */
        return new ScratchStarTable( startab );
    }

    private static class ScratchStarTable extends RandomStarTable {

        private StarTable basetab;
        private int ncol;
        private long nrow;
        private List rows = new ArrayList();

        public ScratchStarTable( StarTable basetab ) throws IOException {
            this.basetab = basetab;
            ncol = basetab.getColumnCount();

            /* Check we don't have an unfeasible number of rows. */
            nrow = basetab.getRowCount();
            if ( nrow > Integer.MAX_VALUE ) {
                throw new IllegalArgumentException( 
                    "Table " + basetab + " has too many rows (" + 
                    nrow + " > Integer.MAX_VALUE)" );
            }

            /* If we don't know the number of rows, we have to load all the
             * data in now so we do. */
            else if ( nrow < 0 ) {
                long irow;
                for ( irow = 0; basetab.hasNext(); irow++ ) {
                    basetab.next();
                    rows.add( basetab.getRow() );
                    if ( irow > Integer.MAX_VALUE ) {
                        throw new IllegalArgumentException( 
                            "Table " + basetab + " has too many rows (" + 
                            " > Integer.MAX_VALUE)" );
                    }
                }
                nrow = irow;
            }
        }

        public int getColumnCount() {
            return ncol;
        }

        public ColumnHeader getHeader( int icol ) {
            return basetab.getHeader( icol );
        }

        public long getRowCount() {
            return nrow;
        }

        protected Object[] doGetRow( long lrow ) throws IOException {
            assert (int) nrow == nrow;
            int irow = (int) lrow;

            /* If we haven't got this far in the base table yet, read rows
             * from it until we have. */
            for ( long toRead = irow - rows.size(); toRead >= 0; toRead-- ) {
                basetab.next();
                rows.add( basetab.getRow() );
            }

            /* Return the row that we have now definitely read from our
             * interal row store. */
            assert irow < rows.size();
            return (Object[]) rows.get( irow );
        }
    }
}
