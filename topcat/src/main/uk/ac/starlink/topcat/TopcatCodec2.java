package uk.ac.starlink.topcat;

import gnu.jel.CompilationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.ValueInfo;

/**
 * Second version of TopcatCodec implementation.
 * This defines a different serialization format to the older
 * {@link TopcatCodec1},
 * and unlike that, this one stores the algebraic expressions
 * etc for algebraically-defined columns and subsets,
 * as well as the definition of some other RowSubset variants
 * like ones based on other subsets or columns.
 * It can thus preserve state better and also requires less storage.
 * 
 * @author   Mark Taylor
 * @since    6 Sep 2017
 */
public class TopcatCodec2 implements TopcatCodec {

    private static final String CODEC_UTYPE_PREFIX = "topcat_session:";
    private static final String CODEC_NAME_PREFIX = "TC_";
    private static final ValueInfo TCVERSION_INFO =
        createCodecInfo( "topcatVersion", String.class );
    private static final ValueInfo CODEC_VERSION_INFO =
        createCodecInfo( "codecVersion", String.class );
    private static final String CODEC_VERSION_VALUE = "2.0";

    private static final ValueInfo LABEL_INFO =
        createCodecInfo( "label", String.class );
    private static final ValueInfo COLS_INDEX_INFO =
        createCodecInfo( "columnIndices", int[].class );
    private static final ValueInfo COLS_VISIBLE_INFO =
        createCodecInfo( "columnVisibilities", boolean[].class );
    private static final ValueInfo SORT_COLUMN_INFO =
        createCodecInfo( "sortColumn", Integer.class );
    private static final ValueInfo SORT_SENSE_INFO =
        createCodecInfo( "sortSense", Boolean.class );
    private static final ValueInfo CURRENT_SUBSET_INFO =
        createCodecInfo( "currentSubset", Integer.class );
    private static final ValueInfo ACTIVATION_INFO =
        createCodecInfo( "activationActions", String.class );

    private static final ValueInfo COL_SPECS_INFO =
        createCodecInfo( "colSpecs", String[].class );
    private static final ValueInfo SUBSET_SPECS_INFO =
        createCodecInfo( "subsetSpecs", String[].class );
    private static final ValueInfo SUBSET_NAMES_INFO =
        createCodecInfo( "subsetNames", String[].class );
    private static final ValueInfo SYNTH_NAMES_INFO =
        createCodecInfo( "synthNames", String[].class );
    private static final ValueInfo SYNTH_EXPRS_INFO =
        createCodecInfo( "synthExprs", String[].class );
    private static final ValueInfo SYNTH_UTYPS_INFO =
        createCodecInfo( "synthUtypes", String[].class );

    private static final String FLAGS_PREFIX = "flags_";
    private static final String SYNTHMETA_PREFIX = "synthmeta_";

    private static final SynthColSpec SYNTH_COLSPEC = new SynthColSpec();
    private static final DataColSpec DATA_COLSPEC = new DataColSpec();

    private static final AllSetSpec ALL_SETSPEC = new AllSetSpec();
    private static final DeletedSetSpec DEL_SETSPEC = new DeletedSetSpec();
    private static final ExprSetSpec EXPR_SETSPEC = new ExprSetSpec();
    private static final InverseSetSpec INV_SETSPEC = new InverseSetSpec();
    private static final ColumnSetSpec COL_SETSPEC = new ColumnSetSpec();
    private static final BitSetSpec BIT_SETSPEC = new BitSetSpec();

    private final static int MAX_NBIT = Integer.SIZE;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat" );

    public StarTable encode( TopcatModel tcModel ) {

        /* Prepare storage for columns and parameters in the output table. */
        List<DescribedValue> paramList = new ArrayList<DescribedValue>();
        List<ColumnData> cdataList = new ArrayList<ColumnData>();

        /* Get table information. */
        PlasticStarTable dataModel = tcModel.getDataModel();
        long nrow = dataModel.getRowCount();
        int ncol = dataModel.getColumnCount();

        /* Mark as a serialized TopcatModel with enough information to
         * identify it as such. */
        paramList.add( new DescribedValue( CODEC_VERSION_INFO,
                                           CODEC_VERSION_VALUE ) );
        paramList.add( new DescribedValue( TCVERSION_INFO,
                                           TopcatUtils.getVersion() ) );

        /* Record label. */
        paramList.add( new DescribedValue( LABEL_INFO, tcModel.getLabel() ) );

        /* Record all synthetic and data columns.  Assemble a list of
         * text specifiers for each column to indicate how to recreate it;
         * either look in one of the data columns of the attached table,
         * or look in a parameter for most of the metadata and other arrays
         * to get the expression and name. */
        String[] colSpecs = new String[ ncol ];
        int nDataCol = 0;
        List<SyntheticColumn> synthCols = new ArrayList<SyntheticColumn>();
        for ( int ic = 0; ic < ncol; ic++ ) {
            ColumnData cdata = dataModel.getColumnData( ic );
            final String cspec;
            if ( cdata instanceof SyntheticColumn ) {
                int isynth = synthCols.size();
                cspec = SYNTH_COLSPEC.createStringSpec( isynth );
                paramList.add( SYNTH_COLSPEC
                              .createMetaParam( cdata.getColumnInfo(),
                                                isynth ) );
                synthCols.add( (SyntheticColumn) cdata );
            }
            else {
                cdataList.add( cdata );
                cspec = DATA_COLSPEC.createStringSpec( nDataCol++ );
                assert nDataCol == cdataList.size();
            }
            colSpecs[ ic ] = cspec;
        }
        int nSynthCol = synthCols.size();
        assert nSynthCol + nDataCol == ncol;
        if ( colSpecs.length > 0 ) {
            paramList.add( new DescribedValue( COL_SPECS_INFO, colSpecs ) );
        }

        /* If we have synthetic columns, record the additional metadata
         * that is not captured in the corresponding parameter. */
        if ( nSynthCol > 0 ) {
            String[] synthNames = new String[ nSynthCol ];
            String[] synthExprs = new String[ nSynthCol ];
            String[] synthUtyps = new String[ nSynthCol ];
            for ( int i = 0; i < nSynthCol; i++ ) {
                ColumnInfo cinfo = synthCols.get( i ).getColumnInfo();
                synthNames[ i ] = cinfo.getName();
                synthExprs[ i ] = cinfo.getAuxDatum( TopcatUtils.EXPR_INFO )
                                       .getValue().toString();
                synthUtyps[ i ] = cinfo.getUtype();
            }
            addArrayParam( paramList, SYNTH_NAMES_INFO, synthNames );
            addArrayParam( paramList, SYNTH_EXPRS_INFO, synthExprs );
            addArrayParam( paramList, SYNTH_UTYPS_INFO, synthUtyps );
        }

        /* Get the list of RowSubsets to serialize. */
        RowSubset[] subsets = getSubsetArray( tcModel );
        int nset = subsets.length;

        /* Record synthetic and data RowSubsets. */
        String[] subsetSpecs = new String[ nset ];
        String[] subsetNames = new String[ nset ];
        List<RowSubset> dataSubsetList = new ArrayList<RowSubset>();
        int iFlagCol = 0;
        boolean hadAll = false;
        for ( int is = 0; is < nset; is++ ) {
            RowSubset rset = subsets[ is ];
            final String sspec;
            if ( rset == RowSubset.ALL ) {
                hadAll = true;
                sspec = ALL_SETSPEC.createStringSpec();
            }
            else if ( rset instanceof DeletedSubset ) {
                sspec = DEL_SETSPEC.createStringSpec();
            }
            else if ( rset instanceof SyntheticRowSubset ) {
                String expr = ((SyntheticRowSubset) rset).getExpression();
                sspec = EXPR_SETSPEC.createStringSpec( expr );
            }
            else if ( rset instanceof InverseRowSubset &&
                      Arrays.asList( subsets )
                            .contains( ((InverseRowSubset) rset)
                                      .getInvertedSubset() ) ) {
                RowSubset complement =
                    ((InverseRowSubset) rset).getInvertedSubset();
                int invId = Arrays.asList( subsets ).indexOf( complement );
                sspec = INV_SETSPEC.createStringSpec( invId );
            }
            else if ( rset instanceof BooleanColumnRowSubset &&
                      ((BooleanColumnRowSubset) rset).getTable()
                       == tcModel.getDataModel() ) {
                int ic = ((BooleanColumnRowSubset) rset).getColumnIndex();
                sspec = COL_SETSPEC.createStringSpec( ic );
            }
            else {
                if ( dataSubsetList.size() >= MAX_NBIT ) {
                    cdataList.add( createFlagsColumn( dataSubsetList,
                                                      iFlagCol++ ) );
                    dataSubsetList = new ArrayList<RowSubset>();
                }
                int icol = cdataList.size();
                int ibit = dataSubsetList.size();
                sspec = BIT_SETSPEC.createStringSpec( icol, ibit );
                dataSubsetList.add( rset );
            }
            subsetSpecs[ is ] = sspec;
            subsetNames[ is ] = rset.getName();
        }
        assert hadAll;
        if ( dataSubsetList.size() > 0 ) {
            cdataList.add( createFlagsColumn( dataSubsetList, iFlagCol++ ) );
        }
        if ( nset > 0 ) {
            paramList.add( new DescribedValue( SUBSET_NAMES_INFO,
                                               subsetNames ) );
            paramList.add( new DescribedValue( SUBSET_SPECS_INFO, 
                                               subsetSpecs ) );
        }

        /* Record column sequences. */
        ColumnList colList = tcModel.getColumnList();
        int nCol = colList.size();
        int[] icols = new int[ nCol ];
        boolean[] activs = new boolean[ nCol ];
        for ( int jc = 0; jc < nCol; jc++ ) {
            icols[ jc ] = colList.getColumn( jc ).getModelIndex();
            activs[ jc ] = colList.isActive( jc );
        }
        paramList.add( new DescribedValue( COLS_INDEX_INFO, icols ) );
        paramList.add( new DescribedValue( COLS_VISIBLE_INFO, activs ) );

        /* Record sort order. */
        SortOrder sortOrder = tcModel.getSelectedSort();
        TableColumn sortCol = sortOrder == null ? null : sortOrder.getColumn();
        if ( sortCol != null ) {
            int icolSort = tcModel.getColumnList().indexOf( sortCol );
            if ( icolSort >= 0 ) {
                boolean sense = tcModel.getSortSenseModel().isSelected();
                paramList.add( new DescribedValue( SORT_COLUMN_INFO,
                                                   new Integer( icolSort ) ) );
                paramList.add( new DescribedValue( SORT_SENSE_INFO,
                                                   Boolean.valueOf( sense ) ) );
            }
        }

        /* Record current subset. */
        int iset = Arrays.asList( subsets )
                  .indexOf( tcModel.getSelectedSubset() );
        if ( iset >= 0 ) {
            paramList.add( new DescribedValue( CURRENT_SUBSET_INFO,
                                               new Integer( iset ) ) );
        }

        /* Record activation actions. */
        if ( tcModel.hasActivationWindow() ) {
            List<Map<String,String>> activState =
                tcModel.getActivationWindow().getActivationState();
            String activTxt = serializeMapList( activState );
            if ( activTxt != null ) {
                paramList.add( new DescribedValue( ACTIVATION_INFO,
                                                   activTxt ) );
            }
        }

        /* Copy parameters from the input table.
         * Be paranoid about possible name clashes. */
        for ( DescribedValue dval : dataModel.getParameters() ) {
            String utype = dval.getInfo().getUtype();
            if ( ! isCodecUtype( utype ) ) {
                paramList.add( dval );
            }
        }

        /* Package the column and parameter data into a table for export. */
        ColumnStarTable outTable = ColumnStarTable.makeTableWithRows( nrow );
        outTable.setName( dataModel.getName() );
        outTable.getParameters().addAll( paramList );
        for ( ColumnData cdata : cdataList ) {
            outTable.addColumn( cdata );
        }
        return outTable;
    }

    public boolean isEncoded( StarTable table ) {
        return isEncoded( new CodecParamSet( table.getParameters() ) );
    }

    public TopcatModel decode( StarTable table, String location,
                               ControlWindow controlWindow ) {
        try {
            return doDecode( table, location, controlWindow );
        }
        catch ( RuntimeException e ) {
            logger_.log( Level.WARNING,
                         "Error parsing TOPCAT session file: " + e, e );
            return null;
        }
    }

    /**
     * Tests whether the parameters in a given parameter set
     * look like they are part of a table that has been encoded by
     * this codec.
     *
     * @param  pset  parameter set
     * @return   true iff correspondingn table looks like one of ours
     */
    private boolean isEncoded( CodecParamSet pset ) {
        if ( ! CODEC_VERSION_VALUE
              .equals( pset.getCodecValue( CODEC_VERSION_INFO ) ) ) {
            return false;
        }
        return true;
    }

    /**
     * Does the work for the decoding.  May throw an unchecked exception,
     * for instance a ClassCastException if certain metadata items are
     * present but have the wrong type (not likely excepting deliberate
     * sabotage, but conceivable).
     *
     * @param  table  encoded table
     * @param  location  table location string
     * @param  controlWindow  control window
     * @return   topcat model, or null
     */
    private TopcatModel doDecode( final StarTable inTable, String location,
                                  ControlWindow controlWindow ) {

        /* Sort out parameters. */
        CodecParamSet pset = new CodecParamSet( inTable.getParameters() );

        /* Check the table looks like it has been serialized by this codec. */
        if ( ! isEncoded( pset ) ) {
            return null;
        }

        /* Get extra information required to reconstruct synthetic columns. */
        String[] synthNames = (String[]) pset.getCodecValue( SYNTH_NAMES_INFO );
        String[] synthExprs = (String[]) pset.getCodecValue( SYNTH_EXPRS_INFO );
        String[] synthUtyps = (String[]) pset.getCodecValue( SYNTH_UTYPS_INFO );

        /* Assemble the list of columns. */
        String[] colSpecs = (String[]) pset.getCodecValue( COL_SPECS_INFO );
        int ncol = colSpecs.length;
        ColumnData[] cdatas = new ColumnData[ ncol ];
        String[] colExprs = new String[ ncol ];
        for ( int ic = 0; ic < ncol; ic++ ) {
            String colSpec = colSpecs[ ic ];
            final ColumnData cdata;
            if ( SYNTH_COLSPEC.isSpec( colSpec ) ) {
                int isynth = SYNTH_COLSPEC.getSynthIndex( colSpec );
                ValueInfo pinfo =
                    pset.getCodecParam( SYNTH_COLSPEC.getMetaInfo( isynth ) )
                        .getInfo();
                ColumnInfo cinfo = new ColumnInfo( pinfo );
                cinfo.setName( synthNames[ isynth ] );
                cinfo.setUtype( synthUtyps != null && synthUtyps.length > 0
                                ? synthUtyps[ isynth ]
                                : null );
                colExprs[ ic ] = synthExprs[ isynth ];

                /* At this stage it is not safe to construct a ColumnData
                 * based on a JEL expression, since it may depend on other
                 * tokens (column or subset names) which are not yet
                 * present in the table.  However, we need to add the
                 * column with the correct data type so that other JEL
                 * expressions can be written based on these.
                 * So for now create a column with the right name and
                 * data type (and other metadata).  This is good enough
                 * to compile other JEL expressions.  We will replace the
                 * column content later when all the tokens are in place. */
                cdata = new ColumnData( cinfo ) {
                    public Object readValue( long irow ) {
                        return null;
                    }
                };
            }
            else if ( DATA_COLSPEC.isSpec( colSpec ) ) {
                final int icol = DATA_COLSPEC.getColumnIndex( colSpec );
                cdata = new ColumnData( inTable.getColumnInfo( icol ) ) {
                    public Object readValue( long irow ) throws IOException {
                        return inTable.getCell( irow, icol );
                    }
                };
            }
            else {
                throw new IllegalArgumentException( "Unknown column spec \""
                                                  + colSpec + "\"" );
            }
            cdatas[ ic ] = cdata;
        }

        /* Construct a table. */
        ColumnStarTable dataTable =
            ColumnStarTable.makeTableWithRows( inTable.getRowCount() );
        dataTable.setName( inTable.getName() );
        for ( ColumnData cdata : cdatas ) {
            dataTable.addColumn( cdata );
        }

        /* Prepare a basic TopcatModel. */
        TopcatModel tcModel =
            TopcatModel.createRawTopcatModel( dataTable, location,
                                              controlWindow );

        /* Assemble the list of RowSubsets. */
        String[] rsetNames = (String[]) pset.getCodecValue( SUBSET_NAMES_INFO );
        String[] rsetSpecs = (String[]) pset.getCodecValue( SUBSET_SPECS_INFO );
        int nset = rsetSpecs.length;
        RowSubset[] rsets = new RowSubset[ nset ];
        Map<RowSubset,String> rsetExprMap = new HashMap<>();
        Map<RowSubset,Integer> rsetInvMap = new HashMap<>();
        for ( int is = 0; is < nset; is++ ) {
            String rsetName = rsetNames[ is ];
            String rsetSpec = rsetSpecs[ is ];
            RowSubset rset;
            if ( ALL_SETSPEC.isSpec( rsetSpec ) ) {
                rset = RowSubset.ALL;
            }
            else if ( DEL_SETSPEC.isSpec( rsetSpec ) ) {
                rset = new DeletedSubset();

                /* The name picked up from the session file is "DELETED"
                 * for all deleted subsets, which was a bad decision when
                 * writing the serialization format.  Having multiple subsets
                 * with the same name will confuse the subset indexing,
                 * so do something to make sure the names are unique in case
                 * there is more than one deleted subset. */
                rsetName = "***DELETED***" + is;
            }
            else if ( EXPR_SETSPEC.isSpec( rsetSpec ) ) {
                String expr = EXPR_SETSPEC.getExpression( rsetSpec );
                rset = new RowSubset( rsetName ) {
                    public boolean isIncluded( long lrow ) {
                        return false;
                    }
                };
                rsetExprMap.put( rset, expr );
            }
            else if ( INV_SETSPEC.isSpec( rsetSpec ) ) {
                int subsetId = INV_SETSPEC.getSubsetId( rsetSpec );
                rset = new InverseRowSubset( rsets[ subsetId ] );
                rsetInvMap.put( rset, Integer.valueOf( subsetId ) );
            }
            else if ( COL_SETSPEC.isSpec( rsetSpec ) ) {
                int ic = COL_SETSPEC.getColumnIndex( rsetSpec );
                rset = new BooleanColumnRowSubset( tcModel.getDataModel(), ic );
            }
            else if ( BIT_SETSPEC.isSpec( rsetSpec ) ) {
                int icol = BIT_SETSPEC.getColumnIndex( rsetSpec );
                int ibit = BIT_SETSPEC.getBitIndex( rsetSpec );
                rset = createRowSubset( rsetName, inTable, icol, ibit );
            }
            else {
                throw new IllegalArgumentException( "Unknown subset spec \""
                                                  + rsetSpec + "\"" );
            }
            if ( rsetName != null ) {
                rset.setName( rsetName );
            }
            rsets[ is ] = rset;
        }

        /* Reorder the columns to match their saved state. */
        int[] icols = (int[]) pset.getCodecValue( COLS_INDEX_INFO );
        TableColumnModel colModel = tcModel.getColumnModel();
        ColumnList colList = tcModel.getColumnList();
        assert ncol == colModel.getColumnCount();
        assert ncol == colList.size();
        TableColumn[] tcols = new TableColumn[ ncol ];
        for ( int ic = 0; ic < ncol; ic++ ) {
            tcols[ ic ] = colList.getColumn( icols[ ic ] );
        }

        /* Reorder the columns in the TableColumnModel to match the saved
         * order.  This has the effect of updating the ColumnList as well,
         * since it is a listener. */
        for ( int ic = 0; ic < ncol; ic++ ) {
            TableColumn tcol = tcols[ ic ];
            if ( colModel.getColumn( ic ) != tcol ) {
                int kc = -1;
                for ( int jc = ic; jc < ncol && kc < 0; jc++ ) {
                    if ( colModel.getColumn( jc ) == tcol ) {
                        kc = jc;
                    }
                }
                assert kc >= 0;
                colModel.moveColumn( kc, ic );
            }
        }
        for ( int ic = 0; ic < ncol; ic++ ) {
            assert colModel.getColumn( ic ) == tcols[ ic ];
            assert colList.getColumn( ic ) == tcols[ ic ];
        }

        /* Flag each column as visible or not, according to the saved state. */
        boolean[] activs = (boolean[]) pset.getCodecValue( COLS_VISIBLE_INFO );
        for ( int ic = 0; ic < ncol; ic++ ) {
            colList.setActive( ic, activs[ ic ] );
        }

        /* Get index of current subset. */
        Integer jndexCurrentSubset =
            (Integer) pset.getCodecValue( CURRENT_SUBSET_INFO );
        int jCurrentSubset = jndexCurrentSubset != null
                           ? jndexCurrentSubset.intValue()
                           : -1;

        /* Add row subsets to the TopcatModel, except for ALL, which is
         * added as part of TopcatModel construction. */
        for ( RowSubset rset : rsets ) {
            if ( rset != RowSubset.ALL ) {
                tcModel.addSubset( rset );
            }
        }

        /* Add original table parameters. */
        for ( DescribedValue param : pset.getDataParameters() ) {
            tcModel.addParameter( param );
        }

        /* Now all the symbols (columns, subsets and parameters) are in
         * place with the correct type.  That means we can set the
         * synthetic columns and subsets up with their JEL expressions. */

        /* First do the synthetic columns.
         * Note we have to work on the TopcatModel's dataModel here,
         * not the dataTable that we fed it earlier; the TopcatModel
         * has taken a somewhat deep copy. */
        PlasticStarTable dataModel = tcModel.getDataModel();
        for ( int ic = 0; ic < ncol; ic++ ) {
            String expr = colExprs[ ic ];
            if ( expr != null ) {
                ColumnInfo info = dataModel.getColumnInfo( ic );
                Class<?> clazz = info.getContentClass();
                try {
                    ColumnData cdata =
                        new SyntheticColumn( tcModel, info, expr, clazz );
                    dataModel.setColumn( ic, cdata );
                }
                catch ( CompilationException e ) {
                    logger_.log( Level.WARNING,
                                 "Can't evaluate column " + info.getName()
                               + " (" + expr + ")", e );
                }
            }
        }

        /* Next do the algebraic and inverse subsets. */
        OptionsListModel<RowSubset> subsets = tcModel.getSubsets();
        for ( int is = 0; is < subsets.size(); is++ ) {
            RowSubset rset0 = subsets.get( is );
            String name = rset0.getName();

            /* If it was an algebraic subset, recompile it using the symbols
             * that are now in place. */
            if ( rsetExprMap.containsKey( rset0 ) ) {
                String expr = rsetExprMap.get( rset0 );
                try {
                    RowSubset rset1 =
                        new SyntheticRowSubset( name, tcModel, expr );
                    subsets.set( is, rset1 );
                }
                catch ( CompilationException e ) {
                    logger_.log( Level.WARNING,
                                 "Can't evaluate subset " + name
                               + "(" + expr + ")", e );
                }
            }

            /* If it was an inverse subset, reconstruct it pointing at the
             * actual rather than placeholder complement it references. */
            else if ( rsetInvMap.containsKey( rset0 ) ) {
                int subsetId = rsetInvMap.get( rset0 ).intValue();
                RowSubset rset1 =
                    new InverseRowSubset( subsets.get( subsetId ) );
                assert rset0.getName().equals( rset1.getName() );
                subsets.set( is, rset1 );
            }
        }

        /* I don't think this is required, but it can't hurt. */
        tcModel.recompileSubsets();

        /* Remove any subsets marked as deleted.  We have to add them first
         * and then delete them here, so the unique identifiers (_ID values)
         * and other setup uses the correct indices. */
        RowSubset currentSubset = tcModel.getSubsets().get( jCurrentSubset );
        for ( Iterator<RowSubset> rsIt = tcModel.getSubsets().iterator();
              rsIt.hasNext(); ) {
            if ( rsIt.next() instanceof DeletedSubset ) {
                rsIt.remove();
            }
        }
        int iCurrentSubset = tcModel.getSubsets().indexOf( currentSubset );

        /* Restore activation actions. */
        String activTxt = (String) pset.getCodecValue( ACTIVATION_INFO );
        if ( activTxt != null ) {
            List<Map<String,String>> activState =
                deserializeMapList( activTxt );
            if ( activState != null ) {
                tcModel.getActivationWindow().setActivationState( activState );
            }
        }

        /* Set label. */
        tcModel.setLabel( (String) pset.getCodecValue( LABEL_INFO ) );

        /* Set current subset. */
        if ( iCurrentSubset >= 0 ) {
            tcModel.applySubset( tcModel.getSubsets().get( iCurrentSubset ) );
        }

        /* Set current sort order. */
        Integer icolSort = (Integer) pset.getCodecValue( SORT_COLUMN_INFO );
        if ( icolSort != null ) {
            int icSort = icolSort.intValue();
            boolean sortSense =
                Boolean.TRUE.equals( pset.getCodecValue( SORT_SENSE_INFO ) );
            TableColumn tcolSort = colList.getColumn( icSort );
            tcModel.getSortSenseModel().setSelected( sortSense );
            tcModel.sortBy( new SortOrder( tcolSort ), sortSense );
        }

        /* Return the fully populated TopcatModel. */
        return tcModel;
    }

    /**
     * Returns a ColumnData object storing an array of RowSubsets,
     * packed as bits into integer values of a suitable size.
     *
     * @param   rsetList  list of no more than MAX_NBIT row subsets
     * @param   iseq   identifying sequence number of the output column
     * @return  column data
     */
    private ColumnData createFlagsColumn( List<RowSubset> rsetList, int iseq ) {
        final RowSubset[] rsets = rsetList.toArray( new RowSubset[ 0 ] );
        final int nset = rsets.length;
        String cname = FLAGS_PREFIX + iseq;
        final ColumnData coldata;
        if ( nset <= Short.SIZE ) {
            coldata = new ColumnData( createCodecInfo( cname, Short.class ) ) {
                public Object readValue( long irow ) {
                    int flag = 0;
                    for ( int iset = nset - 1; iset >= 0; iset-- ) {
                        flag <<= 1;
                        if ( rsets[ iset ].isIncluded( irow ) ) {
                            flag = flag | 1;
                        }
                    }
                    return new Short( (short) flag );
                }
            };
        }
        else if ( nset <= Integer.SIZE ) {
            coldata = new ColumnData( createCodecInfo( cname,
                                                       Integer.class ) ) {
                public Object readValue( long irow ) {
                    int flag = 0;
                    for ( int iset = nset - 1; iset >= 0; iset-- ) {
                        flag <<= 1;
                        if ( rsets[ iset ].isIncluded( irow ) ) {
                            flag = flag | 1;
                        }
                    }
                    return new Integer( flag );
                }
            };
        }
        else {
            assert nset > MAX_NBIT;
            throw new IllegalArgumentException( "Too many subsets in group" );
        }
        coldata.getColumnInfo().setNullable( false );
        return coldata;
    }

    /**
     * Generates a RowSubset from a column like one generated by a call
     * to {@link #createFlagsColumn}.
     *
     * @param   name  subset name
     * @param   table   input table
     * @param   icol   index of column containing flag data
     * @param   iflag   index of flag within column
     * @return   iflag'th subset derived from column icol in table
     */
    private RowSubset createRowSubset( String name, final StarTable table,
                                       final int icol, int iflag ) {
        ColumnInfo info = table.getColumnInfo( icol );
        Class<?> clazz = info.getContentClass();
        if ( clazz == Short.class ) {
            final short mask = (short) ( 1 << iflag );
            return new RowSubset( name ) {
                public boolean isIncluded( long lrow ) {
                    try {
                        return ( ((Number) table.getCell( lrow, icol ))
                                          .shortValue() & mask ) != 0;
                    }
                    catch ( IOException e ) {
                        return false;
                    }
                }
            };
        }
        else if ( clazz == Integer.class ) {
            final int mask = 1 << iflag;
            return new RowSubset( name ) {
                public boolean isIncluded( long lrow ) {
                    try {
                        return ( ((Number) table.getCell( lrow, icol ))
                                          .intValue() & mask ) != 0;
                    }
                    catch ( IOException e ) {
                        return false;
                    }
                }
            };
        }
        else {
            throw new IllegalArgumentException( "Can't decode subsets column" );
        }
    }

    /**
     * Returns an array of the RowSubsets associated with a given
     * TopcatModel, indexed by their unique identifier (original
     * creation sequence), not by their current index in the subsets list.
     * Entries in the list corresponding to subsets that have been
     * deleted are null.
     *
     * @param  tcModel  topcat model
     * @return   subset array indexed by identifier
     */
    private RowSubset[] getSubsetArray( TopcatModel tcModel ) {
        OptionsListModel<RowSubset> subsetModel = tcModel.getSubsets();
        int maxId = -1;
        for ( int ix = 0; ix < subsetModel.size(); ix++ ) {
            maxId = Math.max( maxId, subsetModel.indexToId( ix ) );
        }
        RowSubset[] subsetArray = new RowSubset[ maxId + 1 ];
        for ( int id = 0; id < maxId + 1; id++ ) {
            int ix = subsetModel.idToIndex( id );
            subsetArray[ id ] = ix >= 0 ? subsetModel.get( ix )
                                        : new DeletedSubset();
        }
        return subsetArray;
    }

    /**
     * Adds a String-array-valued parameter to a supplied list.
     * This is pretty straightforward, except that if the supplied
     * array is empty no action is taken.  This is necessary, since
     * there are problems with serializing empty string arrays in VOTable.
     *
     * @param  paramList   list of DescribedValues to append to
     * @param  info     key for new DescribedValue
     * @param  arrayValue  value for new DescribedValue
     */
    private void addArrayParam( List<DescribedValue> paramList,
                                ValueInfo info, String[] arrayValue ) {
        boolean hasValues = false;
        if ( arrayValue != null ) {
            for ( String a : arrayValue ) {
                if ( a != null && a.trim().length() > 0 ) {
                    hasValues = true;
                }
            }
        }
        if ( hasValues ) {
            paramList.add( new DescribedValue( info, arrayValue ) );
        }
    }

    /**
     * Returns a ValueInfo which describes a particular metadata item
     * suitable for use with this codec.
     *
     * @param   unique, but not namespaced, name for the metadata item
     * @param   clazz  class of value which will be stored under this item
     * @return   new metadata description object
     */
    private static ValueInfo createCodecInfo( String name, Class<?> clazz ) {
        DefaultValueInfo info =
            new DefaultValueInfo( CODEC_NAME_PREFIX + name, clazz );
        info.setUtype( CODEC_UTYPE_PREFIX + name ); 
        return info;
    }

    /** 
     * Indicates whether a given utype is a marker for metadata private
     * to the serialization scheme used by this class.
     *      
     * @param  utype  info utype
     * @return  true iff utype is for private codec purposes
     */         
    private static boolean isCodecUtype( String utype ) {
        return utype != null && utype.startsWith( CODEC_UTYPE_PREFIX );
    }

    /**
     * Converts a list of maps to a string using JSON.
     *
     * @param  list  list of maps
     * @return  JSON string encoding list
     */
    private static String serializeMapList( List<Map<String,String>> list ) {
        StringBuffer buf = new StringBuffer()
            .append( "[" );
        for ( Map<String,String> map : list ) {
            buf.append( "\n  " )
               .append( JSONObject.valueToString( map ) )
               .append( "," );
        }
        buf.setLength( buf.length() - 1 );
        buf.append( "\n]" );
        return buf.toString();
    }

    /**
     * Decodes a JSON string representing a list of string-&gt;string maps.
     * Elements of the wrong type are ignored, if the decode fails completely
     * null is returned and a message is written through the logging system.
     *
     * @param  txt  encoded text
     * @return  list of string-&gt;string maps
     */
    private static List<Map<String,String>> deserializeMapList( String txt ) {
        try {
            JSONArray jsonArray = new JSONArray( txt );
            int nel = jsonArray.length();
            List<Map<String,String>> list =
                new ArrayList<Map<String,String>>( nel );
            for ( int i = 0; i < nel; i++ ) {
                Object el = jsonArray.get( i );
                if ( el instanceof JSONObject ) {
                    JSONObject jsonObj = (JSONObject) el;
                    Map<String,String> map = new LinkedHashMap<String,String>();
                    for ( String key : jsonObj.keySet() ) {
                        Object val = jsonObj.get( key );
                        if ( val instanceof String ) {
                            map.put( key, (String) val );
                        }
                    }
                    list.add( map );
                }
            }
            return list;
        }
        catch ( JSONException e ) {
            logger_.log( Level.WARNING, "JSON deserialization error: " + e, e );
            return null;
        }
    }

    /**
     * RowSubset implementation to represent a subset that has been deleted.
     */
    private static class DeletedSubset extends RowSubset {
        DeletedSubset() {
            super( "DELETED" );
        }
        public boolean isIncluded( long lrow ) {
            return false;
        }
    };

    /**
     * Utility class to package a list of table parameters and
     * interrogate them, especially for information that has been
     * inserted specially as part of the encoding format.
     */
    private static class CodecParamSet {

        private final List<DescribedValue> dataParamList_;
        private final Map<String,DescribedValue> codecParamMap_;

        /**
         * Constructor.
         *
         * @param  params  list of table parameters
         */
        CodecParamSet( List<DescribedValue> params ) {
            dataParamList_ = new ArrayList<DescribedValue>();

            /* For the codec parameters, store them keyed by utype
             * for later retrieval.  These parameters should all have
             * custom and easily-identifiable topcat-specific utypes. */
            codecParamMap_ = new LinkedHashMap<String,DescribedValue>();
            for ( DescribedValue dval : params ) {
                String utype = dval.getInfo().getUtype();
                if ( isCodecUtype( utype ) ) {
                    codecParamMap_.put( utype, dval );
                }
                else {
                    dataParamList_.add( dval );
                }
            }
        }

        /**
         * Returns a list of all the parameters which were not added as
         * part of the encoding process, that is those which are
         * intrinsic to the original saved table.
         *
         * @return  list of table parameters
         */
        public List<DescribedValue> getDataParameters() {
            return dataParamList_;
        }

        /**
         * Returns a specific parameter added as part of the encoding process.
         *
         * @param  info  key for required codec parameter
         * @return   typed value for the given codec key,
         *           or null if none is present
         */
        public DescribedValue getCodecParam( ValueInfo info ) {
            return codecParamMap_.get( info.getUtype() );
        }

        /**
         * Returns a codec-specific parameter value from the input list.
         *
         * @param  info  metadata description
         * @return   value stored under the given info, or null if absent
         */
        public Object getCodecValue( ValueInfo info ) {
            DescribedValue dval = getCodecParam( info );
            Object value = dval == null ? null : dval.getValue();
            
            /* This is mostly a case of getting the DescribedValue
             * keyed by utype and returning its value.
             * However, there is a complication: because of the way 
             * VOTable values are written, a single-element array is not
             * distinguished when written or read from a scalar,
             * but we need to return the value in the form requested
             * by the supplied info argument, since it will get cast 
             * to that class. */
            Class<?> infoClazz = info.getContentClass();
            if ( value == null || infoClazz.isInstance( value ) ) {
                return value;
            }
            else if ( boolean[].class.equals( infoClazz ) 
                      && value instanceof Boolean ) {
                return new boolean[] { ((Boolean) value).booleanValue() };
            }
            else if ( int[].class.equals( infoClazz )
                      && value instanceof Integer ) {
                return new int[] { ((Integer) value).intValue() };
            }
            else if ( String[].class.equals( infoClazz )
                      && value instanceof String ) {
                return new String[] { (String) value };
            }
            else {
                logger_.warning( "Session metadata value "
                               + info.getName() + " has type "
                               + value.getClass().getName() + " not "
                               + infoClazz );
                return value; 
            }
        }
    }

    /**
     * Abstract superclass for objects that encapsulate how column or
     * subset specifications are encoded in a StarTable.
     * This is not much more than a marker interface, with the useful
     * methods defined as well as implemented in the concrete subclasses,
     * but the purpose of doing it this way is to make sure that the
     * coding and decoding prescriptions are kept together.
     *
     * <p>Each concrete subclass has a createStringSpec method, that can
     * generate a short text string packing the required information
     * to work out how to reconstruct the column or subset in question.
     * The signature of this method differs per concrete subclass,
     * since the information that must be packed differs.
     */
    private static abstract class Spec {
        final String prefix_;

        /**
         * Constructor.
         *
         * @param  prefix  short string that identifies specific instances
         *                 as distinct from other instances that might
         *                 appear in the same context
         */
        Spec( String prefix ) {
            prefix_ = prefix;
        }

        /**
         * Indicates whether a text string corresponds to the specifier for
         * this instance.
         *
         * @param  txt   text specifier
         * @return   true  iff txt starts with prefix
         */
        boolean isSpec( String txt ) {
            return txt != null && txt.startsWith( prefix_ );
        }

        /**
         * Returns a part of the given text specifier stripped of this
         * object's prefix.
         *
         * @param  txt   text specifier
         * @return  txt stripped of prefix, or null if not suitably prefixed
         */
        String getSuffix( String txt ) {
            return isSpec( txt )
                 ? txt.substring( prefix_.length() )
                 : null;
        }
    }

    /**
     * Specifier for a synthetic column.  The column metadata and the
     * algebraic expression defining the column contents are stored in
     * table parameters.
     */
    private static class SynthColSpec extends Spec {

        /**
         * Constructor.
         */
        SynthColSpec() {
            super( "synth:" );
        }

        /**
         * Returns a specifier string for a synthetic column with a given
         * index.
         *
         * @param  isynth  index into list of known synthetic columns
         * @return specifier string
         */
        public String createStringSpec( int isynth ) {
            return prefix_ + Integer.toString( isynth );
        }

        /**
         * Retrieves the synthetic column index from a string specifier.
         *
         * @param  txt  synthetic column specifier string
         * @return   synthetic column index
         */
        public int getSynthIndex( String txt ) {
            return Integer.parseInt( getSuffix( txt ) );
        }

        /**
         * Returns a DescribedValue (that can be stored as a table parameter)
         * containing most of the metadata for a given column.
         * This can be used as the primary storage for this metadata,
         * but the name and utype are not stored here, since they
         * must be used to identify the table parameter.
         *
         * @param  info  metadata for column to be stored
         * @param  isynth  index into list of synthetic columns
         * @param  metadata item representing column info
         */
        DescribedValue createMetaParam( ColumnInfo info, int isynth ) {
            ValueInfo codecInfo = getMetaInfo( isynth );
            DefaultValueInfo metaInfo = new DefaultValueInfo( info );
            metaInfo.setName( codecInfo.getName() );
            metaInfo.setUtype( codecInfo.getUtype() );
            return new DescribedValue( metaInfo, null );
        }

        /**
         * Returns the parameter key for a given synthetic column.
         *
         * @param  isynth  index into list of synthetic columns
         * @return   metadata key
         */
        ValueInfo getMetaInfo( int isynth ) {
            return createCodecInfo( SYNTHMETA_PREFIX + isynth, String.class );
        }
    }

    /**
     * Specifier for a data column.  The column data and metadata are
     * taken directly from a column in the encoded table.
     */
    private static class DataColSpec extends Spec {

        /**
         * Constructor.
         */
        DataColSpec() {
            super( "col:" );
        }

        /**
         * Returns a specifier string for a data column with a given index.
         *
         * @param  icol  index of the column in the encoded table
         *               to which this column specifier corresponds
         * @return  specifier string
         */
        public String createStringSpec( int icol ) {
            return prefix_ + Integer.toString( icol );
        }

        /**
         * Retrieves the data column index from a data column string specifier.
         *
         * @param  txt  data column specifier string
         * @return  data column index
         */
        public int getColumnIndex( String txt ) {
            return Integer.parseInt( getSuffix( txt ) );
        }
    }

    /**
     * Specifier for the special RowSubset.ALL subset.
     * This is automatically added to the subset list
     * when a TopcatModel is created.
     */
    private static class AllSetSpec extends Spec {

        /**
         * Constructor.
         */
        AllSetSpec() {
            super( "all" );
        }

        /**
         * Returns the unparameterised specifier string.
         *
         * @return  prefix
         */
        public String createStringSpec() {
            return prefix_;
        }
    }

    /**
     * Specifier for a RowSubset that has been deleted.
     * All such specifiers look the same, but it doesn't matter
     * because they never get turned into working subsets.
     */
    private static class DeletedSetSpec extends Spec {

        /**
         * Constructor.
         */
        DeletedSetSpec() {
            super( "deleted" );
        }

        /**
         * Returns the unparameterised specifier string.
         *
         * @return  prefix
         */
        public String createStringSpec() {
            return prefix_;
        }
    }

    /**
     * Specifier for a RowSubset that is defined by an algebraic expression.
     */
    private static class ExprSetSpec extends Spec {

        /**
         * Constructor.
         */
        ExprSetSpec() {
            super( "expr:" );
        }

        /**
         * Returns a specifier string that encodes the given
         * algebraic expression.
         *
         * @param  expr   algebraic expression
         * @return  specifier string
         */
        public String createStringSpec( String expr ) {
            return prefix_ + expr;
        }

        /**
         * Retrieves the algebraic expression from an expression-type
         * string specifier.
         *
         * @param  txt  expression subset specifier string
         * @return  expression text
         */
        public String getExpression( String txt ) {
            return getSuffix( txt );
        }
    }

    /**
     * Specifier for a RowSubset that is defined as the inverse of
     * another subset.
     */
    private static class InverseSetSpec extends Spec {

        /**
         * Constructor.
         */
        InverseSetSpec() {
            super( "inv:" );
        }

        /**
         * Returns a specifier string that encodes the given subset identifier.
         *
         * @param  subsetId  subset identifier
         * @return  specifier string
         */
        public String createStringSpec( int subsetId ) {
            return prefix_ + subsetId;
        }

        /**
         * Retrieves the subset identifier from an inverse-type
         * subset specifier string.
         *
         * @param  txt  inverse subset specifier string
         * @return  subset identifier
         */
        public int getSubsetId( String txt ) {
            return Integer.parseInt( getSuffix( txt ) );
        }
    }

    /**
     * Specifier for a RowSubset that corresponds to a boolean table column.
     */
    private static class ColumnSetSpec extends Spec {

        /**
         * Constructor.
         */
        ColumnSetSpec() {
            super( "bcol:" );
        }

        /**
         * Returns a specifier string for a row subset defined by a given
         * table data column.
         *
         * @param  icol  index of the boolean column in the encoded table
         *               to which this subset specifier corresponds
         * @return  specifier string
         */
        public String createStringSpec( int icol ) {
            return prefix_ + icol;
        }

        /**
         * Retrieves the data column index from a column subset string
         * specifier.
         *
         * @param  txt  column subset specifier string
         * @return  data column index
         */
        public int getColumnIndex( String txt ) {
            return Integer.parseInt( getSuffix( txt ) );
        }
    }

    /**
     * Specifier for a RowSubset defined by one bit in a special bitmask
     * column of the data table.
     */
    private static class BitSetSpec extends Spec {
        private final Pattern ixRegex_;

        /**
         * Constructor.
         */
        BitSetSpec() {
            super( "flagcol:" );
            ixRegex_ = Pattern.compile( prefix_ + "([0-9]+)[.]([0-9]+)" );
        }

        /**
         * Returns a speicifier for a row subset based on a given column bit.
         *
         * @param  icol  column index in data table at which bitmask
         *               subset data appears
         * @param  ibit  index of the bit in the given column
         * @return  string specifier
         */
        public String createStringSpec( int icol, int ibit ) {
            return prefix_ + icol + "." + ibit;
        }

        /**
         * Retrieves the column index from a BitSet specifier string.
         *
         * @param   txt   bit subset specifier string
         * @return  index of column in data table
         */
        public int getColumnIndex( String txt ) {
            Matcher matcher = ixRegex_.matcher( txt ); 
            return matcher.matches() ? Integer.parseInt( matcher.group( 1 ) )
                                     : -1;
        }

        /**
         * Retrieves the bit index from a BitSet specifier string.
         *
         * @param   txt   bit subset specifier string
         * @return  index of the bit in the data table column
         */
        public int getBitIndex( String txt ) {
            Matcher matcher = ixRegex_.matcher( txt );
            return matcher.matches() ? Integer.parseInt( matcher.group( 2 ) )
                                     : -1;
        }
    }
}
