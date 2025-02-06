package uk.ac.starlink.table.formats;

import java.awt.datatransfer.DataFlavor;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.RowStore;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.table.MultiTableBuilder;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.URLUtils;

/**
 * Table builder for reading tables in the loosely-defined "ver" format.
 * This is a family of more or less ad-hoc(?) ASCII-based formats
 * for specifying the shapes of 3d bodies.
 *
 * <p>There are usually two parts, which are read by this handler as
 * two tables: a list of 3d vertices, and a list of polygons
 * referencing these vertices.
 * The first line contains two integers, for the vertex and face counts.
 *
 * <p>At present, the vertices are represented as 3 columns X,Y,Z,
 * and the plates must be triangular and are represented as an intial 
 * 3-element position followed by a 6-element array of "other" positions.
 * This is just because those are in the form that's easy to plot
 * using the plot2 * <code>polygon</code> layer.
 * But there might be better ways to do this.
 *
 * <p>This implementation is hacky, mainly because I don't have a robust
 * definition of the format(s).  It should and maybe could be improved.
 * See MKDSK documentation referenced below.
 *
 * <p>Some examples are:
 * <ul>
 * <li>DAMIT variant:
 *     <a href="https://astro.troja.mff.cuni.cz/projects/damit/stored_files/open/60970/shape.txt"
 *        >example file</a>
 * <li>SBN (Gaskell?) format:
 *     <a href="https://sbnarchive.psi.edu/pds4/non_mission/gaskell.phobos.shape-model/data/phobos_ver64q.tab"
 *        >example file</a>
 * <li>Comsim(?) variant:
 *     <a href="http://comsim.esac.esa.int/rossim/SHAPE_MODEL_DRAFTS/OTHER_LEGACY/SHAP7_v1.6/cg-spc-shap7-v1.6-cheops.ver"
 *        >example file</a>
 * </ul>
 *
 * <p>Resources:
 * <ul>
 * <li>Stephane Erard's confluence page (see section "Small body shape models"):
 *    <a href="https://voparis-wiki.atlassian.net/wiki/spaces/VES/pages/156565506/Aladin+TOPCAT+use+cases+in+3D">here</a>
 * <li>MKDSK user guide
 *    <a href="https://naif.jpl.nasa.gov/pub/naif/utilities/PC_Linux_64bit/mkdsk.ug"
 *       >here</a>.
 *    This has a promising description of some other similar formats
 *    which a future evolution of this reader could be made to read.
 * </ul>
 *
 * @author   Mark Taylor
 * @since    6 Feb 2025
 */
public class VerTableBuilder implements MultiTableBuilder {

    private static final int POS_VERTEX_DFLT = 1;
    private static final int POS_PLATE = 2;

    public VerTableBuilder() {
    }

    public String getFormatName() {
        return "ver";
    }

    public boolean looksLikeFile( String location ) {
        return location.endsWith( ".ver" );
    }

    public boolean canImport( DataFlavor flavor ) {
        return false;
    }

    /**
     * Currently returns false.
     * This wouldn't be so hard to do, but it's not that useful since
     * it can only return one table.
     * Maybe do it at some point,
     * but work out first what format variants we will support.
     */
    public boolean canStream() {
        return false;
    }

    public void streamStarTable( InputStream in, TableSink sink, String pos )
            throws IOException {
        throw new UnsupportedOperationException();
    }

    public TableSequence makeStarTables( DataSource datsrc,
                                         StoragePolicy policy )
            throws IOException {
        try ( InputStream in =
                 new BufferedInputStream( datsrc.getInputStream() ) ) {
            NumReader rdr = new NumReader( in );
            int[] counts = rdr.readInts( true, 2 );
            int nVertex = counts[ 0 ];
            int nPlate = counts[ 1 ];
            RowStore vertexStore = policy.makeRowStore();
            streamVertexTable( rdr, nVertex, vertexStore );
            StarTable vertexTable = vertexStore.getStarTable();
            setVertexMeta( vertexTable, datsrc );
            assert vertexTable.isRandom();
            VertexStore vertices = new VertexStore() {
                public double getCoord( int iv, int ic ) throws IOException {
                    return ((Number) vertexTable.getCell( iv, ic ))
                          .doubleValue();
                }
            };
            RowStore plateStore = policy.makeRowStore();
            streamPlateTable( rdr, nPlate, vertices, plateStore );
            StarTable plateTable = plateStore.getStarTable();
            setPlateMeta( plateTable, datsrc );
            return Tables.arrayTableSequence( new StarTable[] {
                vertexTable, plateTable,
            } );
        }
    }

    public StarTable makeStarTable( DataSource datsrc, boolean wantRandom,
                                    StoragePolicy storagePolicy )
            throws IOException {
        String pos = datsrc.getPosition();
        final int ipos;
        if ( pos == null || pos.trim().length() == 0 ) {
            ipos = POS_VERTEX_DFLT;
        }
        else if ( pos.trim().matches( "[0-9]+" ) ) {
            ipos = Integer.parseInt( pos.trim() );
        }
        else {
            throw new TableFormatException( "Unknown pos: " + pos + "; "
                                          + "should be " + POS_VERTEX_DFLT
                                          + " or " + POS_PLATE );
        }
        if ( ipos == POS_VERTEX_DFLT ) {
            try ( InputStream in =
                      new BufferedInputStream( datsrc.getInputStream() ) ) {
                NumReader rdr = new NumReader( in );
                int[] counts = rdr.readInts( true, 2 );
                int nVertex = counts[ 0 ];
                int nPlate = counts[ 1 ];
                // could stream this
                RowStore vertexStore = storagePolicy.makeRowStore();
                streamVertexTable( rdr, nVertex, vertexStore );
                StarTable vertexTable = vertexStore.getStarTable();
                setVertexMeta( vertexTable, datsrc );
                return vertexTable;
            }
        }
        else if ( ipos == POS_PLATE ) {
            TableSequence tseq = makeStarTables( datsrc, storagePolicy );
            tseq.nextTable();
            return tseq.nextTable();
        }
        else {
            throw new TableFormatException( "Unknown pos: " + ipos + "; "
                                          + "should be " + POS_VERTEX_DFLT
                                          + " or " + POS_PLATE );
        }
    }

    /**
     * Returns a 3-element array of ColumnInfos for describing
     * X, Y, Z positions.
     *
     * @return  column metadata for 3d positions
     */
    private ColumnInfo[] createVertexTableInfos() {
        ColumnInfo[] infos = new ColumnInfo[ 3 ];
        for ( int ic = 0; ic < 3; ic++ ) {
            String letter = new String[] { "X", "Y", "Z" }[ ic ];
            ColumnInfo info =
                new ColumnInfo( letter, Double.class, letter + " coordinate" );
            info.setUCD( "pos.cartesian." + letter.toLowerCase() );
            infos[ ic ] = info;
        }
        return infos;
    }

    /**
     * Feeding a fixed number of vertex lines as rows to a TableSink.
     *
     * @param  rdr  row reader
     * @param  nVertex   number of rows to read
     * @param  sink   table sink
     */
    private void streamVertexTable( NumReader rdr, int nVertex, TableSink sink )
            throws IOException {
        sink.acceptMetadata( createDummyTable( createVertexTableInfos(),
                                               nVertex ) );
        for ( int i = 0; i < nVertex; i++ ) {
            double[] values = rdr.readDoubles( true, -1 );
            int nval = values.length;
            int ic;

            // Sometimes the rows are index, x, y, z, sometimes just x, y, z.
            switch ( values.length ) {
                case 3:
                    ic = 0;
                    break;
                case 4: // first value is index
                    ic = 1;
                    break;
                default:
                    throw new TableFormatException( "Expecting 3 or 4 values, "
                                                  + "found " + values.length
                                                  + rdr.atRow() );
            }
            double x = values[ ic + 0 ];
            double y = values[ ic + 1 ];
            double z = values[ ic + 2 ];
            sink.acceptRow( new Object[] { Double.valueOf( x ),
                                           Double.valueOf( y ),
                                           Double.valueOf( z ) } );
        }
        sink.endRows();
    }

    /**
     * Returns column metadata suitable for a plate table.
     *
     * @return  column infos
     */
    private ColumnInfo[] createPlateTableInfos() {
        List<ColumnInfo> infos = new ArrayList<>();
        infos.addAll( Arrays.asList( createVertexTableInfos() ) );
        ColumnInfo othersInfo =
            new ColumnInfo( "others", double[].class, "other coords in plate" );
        othersInfo.setShape( new int[] { 6 } );
        infos.add( othersInfo );
        return infos.toArray( new ColumnInfo[ 0 ] );
    }

    /**
     * Feeds a fixed number of plate lines to a TableSink.
     *
     * @param  rdr  row reader
     * @param  nPlate  number of plates to read
     * @param  vertexStore   has a list of vertices that will be referenced
     * @param  sink   table sink
     */
    private void streamPlateTable( NumReader rdr, int nPlate,
                                   VertexStore vertices, TableSink sink )
            throws IOException {
        sink.acceptMetadata( createDummyTable( createPlateTableInfos(),
                                               nPlate ) );
        for ( int i = 0; i < nPlate; i++ ) {
            int[] values = rdr.readInts( true, -1 );
            int[] iv1s;
            switch ( values.length ) {
                case 1:
                    int nv = values[ 0 ];
                    iv1s = rdr.readInts( true, nv );
                    break;
                case 3:
                    iv1s = new int[] { values[ 0 ], values[ 1 ], values[ 2 ] };
                    break;
                case 4:
                    int index = values[ 0 ];
                    iv1s = new int[] { values[ 1 ], values[ 2 ], values[ 3 ] };
                    break;
                default:
                    throw new TableFormatException( "Found " + values.length
                                                  + " values"
                                                  + ", expecting 1, 3 or 4"
                                                  + rdr.atRow() );
            }
            int iv0 = iv1s[ 0 ] - 1;
            double x0 = vertices.getCoord( iv0, 0 );
            double y0 = vertices.getCoord( iv0, 1 );
            double z0 = vertices.getCoord( iv0, 2 );
            int nv = iv1s.length;
            double[] otherCoords = new double[ ( nv - 1 ) * 3 ];
            for ( int iv = 1; iv < nv; iv++ ) {
                int ic3 = ( iv - 1 ) * 3;
                iv0 = iv1s[ iv ] - 1;
                otherCoords[ ic3 + 0 ] = vertices.getCoord( iv0, 0 );
                otherCoords[ ic3 + 1 ] = vertices.getCoord( iv0, 1 );
                otherCoords[ ic3 + 2 ] = vertices.getCoord( iv0, 2 );
            }
            sink.acceptRow( new Object[] {
                Double.valueOf( x0 ),
                Double.valueOf( y0 ),
                Double.valueOf( z0 ),
                otherCoords,
            } );
        }
        sink.endRows();
    }

    /**
     * Configures a table with metadata suitable for vertex rows.
     *
     * @param  table  table to modify
     * @param  datsrc   data source
     */
    private void setVertexMeta( StarTable table, DataSource datsrc ) {
        setTableMeta( table, datsrc, "vertex", POS_VERTEX_DFLT );
    }

    /**
     * Configures a table with metadata suitable for plate rows.
     *
     * @param  table  table to modify
     * @param  datsrc  data source
     */
    private void setPlateMeta( StarTable table, DataSource datsrc ) {
        setTableMeta( table, datsrc, "plate", POS_PLATE );
    }

    /**
     * Configures a table with metadata.
     *
     * @param  table  table to modify
     * @param  datsrc  data source
     * @param  type   type name
     * @param  ipos   index of table in file
     */
    private void setTableMeta( StarTable table, DataSource datsrc,
                               String type, int ipos ) {
        String name = datsrc.getName();
        String tail = new File( name ).getName();
        if ( tail.length() < name.length() ) {
            name = tail;
        }
        int idot = name.lastIndexOf( '.' );
        if ( idot >= 0 ) {
            name = name.substring( 0, idot );
        }
        table.setName( name + "-" + type );

        URL srcUrl = datsrc.getURL();
        if ( srcUrl != null ) {
            String outTxt = srcUrl.toString().replaceAll( "#.*", "" );
            if ( ipos != POS_VERTEX_DFLT ) {
                outTxt = outTxt + "#" + ipos;
            }
            table.setURL( URLUtils.makeURL( outTxt ) );
        }
    }

    /**
     * Creates a dummy table with a row count and column metadata
     * but no way to access the data.
     *
     * @param  infos  column metadata
     * @param  nrow  row count
     * @return   data-less table
     */
    private static StarTable createDummyTable( ColumnInfo[] infos, long nrow ) {
        return new AbstractStarTable() {
            public int getColumnCount() {
                return infos.length;
            }
            public ColumnInfo getColumnInfo( int ic ) {
                return infos[ ic ];
            }
            public long getRowCount() {
                return nrow;
            }
            public RowSequence getRowSequence() {
                throw new UnsupportedOperationException( "Metadata only" );
            }
        };
    }

    /**
     * Abstraction for accessing homogeneous vertex data.
     */
    private static interface VertexStore {

        /**
         * Reads a stored coordinate.
         *
         * @param  iv  vertex index
         * @param  ic  coordinate index
         * @return   coordinate value
         */
        double getCoord( int iv, int ic ) throws IOException;
    }

    /**
     * Utility class for reading numeric values from an ASCII file.
     */
    private static class NumReader {
        final LineSequence lseq_;
        long iline_;

        /**
         * Constructor.
         *
         * @param  in  input stream to read from
         */
        NumReader( InputStream in ) {
            lseq_ = new LineSequence( in );
        }

        /**
         * Reads a line, returning integer values.
         * If there is no line to read (EOF), behaviour depends on isReq:
         * either null return or an IOException
         *
         * @param  isReq  if true and there is no line to read,
         *                an IOException will result
         * @param  nValue  number of ints that must be read;
         *                 if -1 any number is permitted
         * @return   array of ints read from line,
         *           or null for end of file
         */
        int[] readInts( boolean isReq, int nValue ) throws IOException {
            String[] tokens = readTokens( isReq );
            if ( tokens == null ) {
                return null;
            }
            else {
                int ntok = tokens.length;
                int[] values = new int[ ntok ];
                for ( int i = 0; i < ntok; i++ ) {
                    try {
                        values[ i ] = Integer.parseInt( tokens[ i ] );
                    }
                    catch ( NumberFormatException e ) {
                        throw new TableFormatException( "Not integer" + atRow()
                                                      + ": " + tokens[ i ] );
                    }
                }
                if ( nValue >= 0 && nValue != ntok ) {
                    throw new TableFormatException( "Expecting " + nValue
                                                  + " values, found " + ntok
                                                  + atRow() );
                }
                return values;
            }
        }

        /**
         * Reads a line, returning double values.
         * If there is no line to read (EOF), behaviour depends on isReq:
         * either null return or an IOException
         *
         * @param  isReq  if true and there is no line to read,
         *                an IOException will result
         * @param  nValue  number of doubles that must be read;
         *                 if -1 any number is permitted
         * @return   array of doubles read from line,
         *           or null for end of file
         */
        double[] readDoubles( boolean isReq, int nValue ) throws IOException {
            String[] tokens = readTokens( isReq );
            if ( tokens == null ) {
                return null;
            }
            else {
                int ntok = tokens.length;
                double[] values = new double[ ntok ];
                for ( int i = 0; i < ntok; i++ ) {
                    try {
                        values[ i ] = Double.parseDouble( tokens[ i ] );
                    }
                    catch ( NumberFormatException e ) {
                        throw new TableFormatException( "Not numeric" + atRow()
                                                      + ": " + tokens[ i ] );
                    }
                }
                if ( nValue >= 0 && nValue != ntok ) {
                    throw new TableFormatException( "Expecting " + nValue
                                                  + " values, found " + ntok
                                                  + atRow() );
                }
                return values;
            }
        }

        /**
         * Reads a line, returning space-separated tokens found.
         * If there is no line to read (EOF), behaviour depends on isRequired:
         * either null return or an IOException
         *
         * @param  isRequired  if true and there is no line to be read,
         *                     an IOException will result
         * @return  array of tokens read from line, or null for end of file
         */
        String[] readTokens( boolean isRequired ) throws IOException {
            String line = lseq_.nextLine();
            if ( line == null ) {
                if ( isRequired ) {
                    throw new TableFormatException( "Premature file end"
                                                  + atRow() );
                }
                else {
                    return null;
                }
            }
            else {
                iline_++;
                return line.trim().split( "\\s+", 0 );
            }
        }

        /**
         * Returns a user-directed row identifier suitable for error messages.
         *
         * @return  "at row <nnn>"
         */
        private String atRow() {
            return " at row " + iline_;
        }
    }
}
