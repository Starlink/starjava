package uk.ac.starlink.cdf;

import java.awt.datatransfer.DataFlavor;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import uk.ac.bristol.star.cdf.CdfContent;
import uk.ac.bristol.star.cdf.CdfReader;
import uk.ac.bristol.star.cdf.Variable;
import uk.ac.bristol.star.cdf.VariableAttribute;
import uk.ac.bristol.star.cdf.record.Buf;
import uk.ac.bristol.star.cdf.record.Bufs;
import uk.ac.bristol.star.cdf.record.WrapperBuf;
import uk.ac.starlink.table.ByteStore;
import uk.ac.starlink.table.MultiTableBuilder;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.formats.DocumentedIOHandler;
import uk.ac.starlink.table.formats.DocumentedTableBuilder;
import uk.ac.starlink.util.Compression;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;
import uk.ac.starlink.util.IOUtils;

/**
 * Table input handler for NASA CDF (Common Data Format) files.
 *
 * @author   Mark Taylor
 * @since    24 Jun 2013
 */
public class CdfTableBuilder extends DocumentedTableBuilder
                             implements MultiTableBuilder {

    /**
     * Default CDF-StarTable translation profile.
     * This is based on the ISTP Metadata Guidelines at
     * <a href="https://spdf.gsfc.nasa.gov/sp_use_of_cdf.html"
     *         >https://spdf.gsfc.nasa.gov/sp_use_of_cdf.html</a>
     */
    public static final CdfTableProfile DEFAULT_PROFILE = createProfile(
        true,
        new String[] { "CATDESC", "FIELDNAM", "DESCRIP", "DESCRIPTION", },
        new String[] { "UNITS", "UNIT", "UNITSTRING", },
        new String[] { "FILLVAL", },
        new String[] { "DEPEND_0", }
    );

    private static final int IPOS_BASE = 0;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.cdf" );

    private final CdfTableProfile profile_;

    /**
     * Constructs a default Cdf table builder.
     */
    public CdfTableBuilder() {
        this( DEFAULT_PROFILE );
    }

    /**
     * Constructs a Cdf table builder with a custom translation profile.
     *
     * @param   profile  CDF-Startable translation profile
     */
    public CdfTableBuilder( CdfTableProfile profile ) {
        super( new String[] { "cdf" } );
        profile_ = profile;
    }

    /**
     * Returns "CDF".
     */
    public String getFormatName() {
        return "CDF";
    }

    public StarTable makeStarTable( DataSource datsrc, boolean wantRandom,
                                    StoragePolicy storagePolicy )
            throws IOException {
        TablesContent tcontent =
            new TablesContent( datsrc, storagePolicy, profile_ );
        String pos = datsrc.getPosition();
        final int ipos;
        if ( pos == null || pos.trim().length() == 0 ) {
            ipos = IPOS_BASE;
        }
        else if ( pos.trim().matches( "[0-9]+" ) ) {
            ipos = Integer.parseInt( pos.trim() );
        }
        else if ( tcontent.getTableIndex( pos ) >= 0 ) {
            ipos = tcontent.getTableIndex( pos ) + IPOS_BASE;
        }
        else {
            String msg = "Unknown pos \"" + pos + "\", should be numeric "
                       + "(" + IPOS_BASE + "-"
                             + ( IPOS_BASE + tcontent.getTableCount() ) + ") "
                       + "or one of "
                       + Arrays.stream( tcontent.dependVars_ )
                               .map( Variable::getName )
                               .collect( Collectors.toList() );
            throw new TableFormatException( msg );
        }
        int jpos = ipos - IPOS_BASE;
        if ( jpos < 0 || jpos >= tcontent.getTableCount() ) {
            throw new TableFormatException( "No table at pos #" + ipos );
        }
        return tcontent.createTable( jpos );
    }

    public TableSequence makeStarTables( DataSource datsrc,
                                         StoragePolicy storagePolicy )
            throws IOException {
         TablesContent tcontent =
             new TablesContent( datsrc, storagePolicy, profile_ );
         int nt = tcontent.getTableCount();
         CdfStarTable[] tables = new CdfStarTable[ nt ];
         for ( int it = 0; it < nt; it++ ) {
             tables[ it ] = tcontent.createTable( it );
         }
         return Tables.arrayTableSequence( tables );
    }

    /**
     * Returns false.  I don't think there is a MIME type associated with
     * the CDF format.  References to application/x-cdf and application/cdf
     * appear on the web, but neither is IANA registered, and I <em>think</em>
     * they refer to some other format.
     */
    public boolean canImport( DataFlavor flavor ) {
        return false;
    }

    /**
     * Throws a TableFormatException.
     * CDF is not suitable for streaming.
     */
    public void streamStarTable( InputStream in, TableSink sink, String pos )
            throws IOException {
        throw new TableFormatException( "Can't stream from CDF format" );
    }

    public String getXmlDescription() {
        return String.join( "\n",
            "<p>NASA's Common Data Format, described at",
            DocumentedIOHandler.toLink( "https://cdf.gsfc.nasa.gov/" ) + ",",
            "is a binary format for storing self-described data.",
            "It is typically used to store tabular data for subject areas",
            "like space and solar physics.",
            "</p>",
            "<p>CDF does not store tables as such, but sets of variables",
            "(columns) which are typically linked to a time quantity;",
            "there may be multiple such disjoint sets in a single CDF file.",
            "This reader attempts to extract these sets into separate tables",
            "using, where present, the <code>DEPEND_0</code> attribute",
            "defined by the",
            "<webref url='https://spdf.gsfc.nasa.gov/sp_use_of_cdf.html'",
            ">ISTP Metadata Guidelines</webref>.",
            "Where there are multiple tables they can be identified",
            "using a \"<code>#</code>\" symbol at the end of the filename",
            "by index (\"<code>&lt;file&gt;.cdf#0</code>\" is the first table)",
            "or by the name of the independent variable",
            "(\"<code>&lt;file&gt;.cdf#EPOCH</code>\" is the table relating to",
            "the <code>EPOCH</code> column).",
            "</p>",
        "" );
    }

    public boolean docIncludesExample() {
        return false;
    }

    public boolean canStream() {
        return false;
    }

    /**
     * Utility method to find a Variable from its name.
     *
     * @param  content  CdfContent object
     * @param  name    variable name
     * @return  Variable object matching name, or null
     */
    private static Variable getVariable( CdfContent content, String name ) {
        for ( Variable var : content.getVariables() ) {
            if ( var.getName().equals( name ) ) {
                return var;
            }
        }
        return null;
    }

    /**
     * Utility method to find a VariableAttribute from its name.
     *
     * @param  content  CdfContent object
     * @param  name   attribute name
     * @return   VariableAttribute object matching name, or null
     */
    private static VariableAttribute getVariableAttribute( CdfContent content,
                                                           String name ) {
        for ( VariableAttribute att : content.getVariableAttributes() ) {
            if ( att.getName().equals( name ) ) {
                return att;
            }
        }
        return null;
    }

    /**
     * Buf implementation that delegates most methods to a base instance,
     * but when creating new Buf objects it does so in accordance with
     * a give STIL StoragePolicy.
     */
    private static class StoragePolicyBuf extends WrapperBuf {
        private final Buf baseBuf_;
        private final StoragePolicy storagePolicy_;

        /**
         * Constructor.
         *
         * @param   baseBuf  buf supplying base behaviour
         * @param  storagePolicy  policy for managing new buffer creation
         */
        StoragePolicyBuf( Buf baseBuf, StoragePolicy storagePolicy ) {
            super( baseBuf );
            baseBuf_ = baseBuf;
            storagePolicy_ = storagePolicy;
        }

        @Override
        public Buf fillNewBuf( long count, InputStream in ) throws IOException {

            /* Copy the input stream data to a byte store obtained from
             * the storage policy. */
            ByteStore byteStore = storagePolicy_.makeByteStore();
            OutputStream out = byteStore.getOutputStream();
            int bufsiz = 16384;
            byte[] a = new byte[ bufsiz ];
            for ( int ntot = 0; ntot < count; ) {
                int n = in.read( a, 0,
                                 Math.min( bufsiz, (int) ( count - ntot ) ) );
                if ( n < 0 ) {
                    throw new IOException( "Stream ended after " + ntot + "/"
                                         + count + " bytes" );
                }
                else {
                    out.write( a, 0, n );
                    ntot += n;
                }
            }
            out.flush();

            /* Turn the byte store into NIO buffers. */
            ByteBuffer[] bbufs = byteStore.toByteBuffers();
            byteStore.close();

            /* Turn the NIO buffers into a Buf. */
            return Bufs.createBuf( bbufs,
                                   super.isBit64(), super.isBigendian() );
        }
    }

    /**
     * Constructs an instance of CdfTableProfile with some suggestions
     * for attribute names with known semantics.
     *
     * @param  invarParams  true for turning non-row-varying variables into
     *                      table parameters, false for turning them into
     *                      variables
     * @param  descripAttNames  ordered list of names of attributes
     *                          that might supply description metadata
     * @param  unitAttNames     ordered list of names of attributes
     *                          that might supply units metadata
     * @param  blankvalAttNames ordered list of names of attributes
     *                          that might supply magic blank values
     * @param  depend0AttNames  ordered list of names of attributes
     *                          that might supply a dependent variable name
     * @return  new profile instance
     */
    public static CdfTableProfile createProfile( boolean invarParams,
                                                 String[] descripAttNames,
                                                 String[] unitAttNames,
                                                 String[] blankvalAttNames,
                                                 String[] depend0AttNames ) {
        return new ListCdfTableProfile( invarParams, descripAttNames,
                                        unitAttNames, blankvalAttNames,
                                        depend0AttNames );
    }

    /**
     * CdfTableProfile implementation based on lists of names.
     */
    private static class ListCdfTableProfile implements CdfTableProfile {
        private final boolean invarParams_;
        private final String[] descAttNames_;
        private final String[] unitAttNames_;
        private final String[] blankvalAttNames_;
        private final String[] depend0AttNames_;

        /**
         * Constructor.
         *
         * @param  invarParams  true for turning non-row-varying variables into
         *                      table parameters, false for turning them into
         *                      variables
         * @param  descripAttNames  ordered list of names of attributes
         *                          that might supply description metadata
         * @param  unitAttNames     ordered list of names of attributes
         *                          that might supply units metadata
         * @param  blankvalAttNames ordered list of names of attributes
         *                          that might supply magic blank values
         * @param  depend0AttNames  ordered list of names of attributes
         *                          that might supply a dependent variable name
         */
        ListCdfTableProfile( boolean invarParams, String[] descripAttNames,
                             String[] unitAttNames, String[] blankvalAttNames,
                             String[] depend0AttNames ) {
            invarParams_ = invarParams;
            descAttNames_ = descripAttNames;
            unitAttNames_ = unitAttNames;
            blankvalAttNames_ = blankvalAttNames;
            depend0AttNames_ = depend0AttNames;
        }

        public boolean invariantVariablesToParameters() {
            return invarParams_;
        }

        public String getDescriptionAttribute( String[] attNames ) {
            return match( descAttNames_, attNames );
        }

        public String getUnitAttribute( String[] attNames ) {
            return match( unitAttNames_, attNames );
        }

        public String getBlankValueAttribute( String[] attNames ) {
            return match( blankvalAttNames_, attNames );
        }

        public String getDepend0Attribute( String[] attNames ) {
            return match( depend0AttNames_, attNames );
        }

        /**
         * Returns the first element of a list of options that matches
         * any element of a list of targets.  Matching is normalised.
         *
         * @param  opts  options
         * @param  targets  targets
         * @return  first matching option, or null if none match
         */
        private String match( String[] opts, String[] targets ) {
            Set<String> targetSet = new HashSet<String>();
            for ( int i = 0; i < targets.length; i++ ) {
                targetSet.add( normalise( targets[ i ] ) );
            }
            for ( int i = 0; i < opts.length; i++ ) {
                String opt = opts[ i ];
                if ( targetSet.contains( normalise( opt ) ) ) {
                    return opt;
                }
            }
            return null;
        }

        /**
         * Normalises a token by folding case.
         *
         * @param  txt  token
         * @return  normalised token
         */
        private String normalise( String txt ) {
            return txt.trim().toLowerCase();
        }
    }

    /**
     * Packages a CdfContent ready to yield one or more StarTables.
     */
    private static class TablesContent {

        private final CdfContent content_;
        private final CdfTableProfile profile_;
        private final Variable[] dependVars_;

        /**
         * Constructor.
         *
         * @param  datsrc  data source
         * @param  storagePolicy  storage policy
         * @param  profile  defines details of CDF-to-table mapping
         */
        TablesContent( DataSource datsrc, StoragePolicy storagePolicy,
                       CdfTableProfile profile ) throws IOException {
            profile_ = profile;

            /* Check the magic number. */
            if ( ! CdfReader.isMagic( datsrc.getIntro() ) ) {
                throw new TableFormatException( "Not a CDF file" );
            }
    
            /* Get a buf containing the byte data from the input source. */
            final Buf nbuf;
            if ( datsrc instanceof FileDataSource &&
                 datsrc.getCompression() == Compression.NONE ) {
                File file = ((FileDataSource) datsrc).getFile();
                nbuf = Bufs.createBuf( file, true, true );
            }
            else {
                ByteStore byteStore = storagePolicy.makeByteStore();
                BufferedOutputStream storeOut =
                    new BufferedOutputStream( byteStore.getOutputStream() );
                InputStream dataIn = datsrc.getInputStream();
                IOUtils.copy( dataIn, storeOut );
                dataIn.close();
                storeOut.flush();
                ByteBuffer[] bbufs = byteStore.toByteBuffers();
                storeOut.close();
                byteStore.close();
                nbuf = Bufs.createBuf( bbufs, true, true );
            }

            /* Fix the Buf implementation so that it uses the supplied
             * storage policy for allocating any more required storage. */
            Buf buf = new StoragePolicyBuf( nbuf, storagePolicy );

            /* Turn the buf into a CdfContent. */
            content_ = new CdfContent( new CdfReader( buf ) );

            /* Identify independent variables in the CDF, by noting all the
             * distinct values of the DEPEND_0 attribute (if present).
             * These will correspond to different tables. */
            String[] attNames =
                Arrays.stream( content_.getVariableAttributes() )
                              .map( VariableAttribute::getName )
                              .toArray( n -> new String[ n ] );
            String dependAttName = profile.getDepend0Attribute( attNames );
            final String[] dependVarNames;
            if ( dependAttName != null ) {
                VariableAttribute dependAtt =
                    getVariableAttribute( content_, dependAttName );
                if ( dependAtt != null ) {
                    dependVarNames =
                        Arrays.stream( content_.getVariables() )
                              .map( v -> CdfStarTable
                                        .getStringEntry( dependAtt, v ) )
                              .filter( s -> s != null && s.trim().length() > 0 )
                              .distinct()
                              .toArray( n -> new String[ n ] );
                }
                else {
                    dependVarNames = null;
                }
            }
            else {
                dependVarNames = null;
            }

            /* Turn the independent variable names into Variable instances,
             * and store them for later reference. */
            final List<Variable> dependVars = new ArrayList<>();
            if ( dependVarNames != null && dependVarNames.length > 0 ) {
                for ( String varName : dependVarNames ) {
                    Variable var = getVariable( content_, varName );
                    if ( var == null ) {
                        logger_.warning( "Unknown independent variable "
                                       + varName );
                    }
                    else {
                        dependVars.add( var );
                    }
                }
            }
            dependVars_ = dependVars.toArray( new Variable[ 0 ] );
        }

        /**
         * Returns the number of distinct tables that this content will yeild.
         *
         * @return  table count
         */
        public int getTableCount() {
            return dependVars_.length == 0 ? 1 : dependVars_.length;
        }

        /**
         * Returns the table index given a symbolic name.
         *
         * @param  pos  name of independent variable (case-insensitive)
         * @return  index of table, or -1 if not present
         */
        public int getTableIndex( String pos ) {
            for ( int it = 0; it < dependVars_.length; it++ ) {
                if ( dependVars_[ it ].getName().equalsIgnoreCase( pos ) ) {
                    return it;
                }
            }
            return -1;
        }

        /**
         * Returns a table based on this content.
         *
         * @param  itable  table index
         * @return  new table
         */
        public CdfStarTable createTable( int itable ) throws IOException {
            final Variable dependVar;
            if ( dependVars_.length == 0 ) {
                if ( itable == 0 ) {
                    dependVar = null;
                }
                else {
                    throw new TableFormatException( "No table at " + itable );
                }
            }
            else {
                dependVar = dependVars_[ itable ];
            }
            return new CdfStarTable( content_, profile_, dependVar );
        }
    }
}
