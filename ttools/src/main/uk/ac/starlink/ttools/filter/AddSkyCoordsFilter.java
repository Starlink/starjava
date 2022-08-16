package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import java.util.Iterator;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnPermutedStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.UsageException;
import uk.ac.starlink.ttools.convert.SkyUnits;
import uk.ac.starlink.ttools.convert.SkySystem;
import uk.ac.starlink.ttools.jel.ColumnIdentifier;

/**
 * Filter used for converting sky coordinates from one system to another.
 *
 * @author  Mark Taylor
 * @since   30 Aug 2005
 */
public class AddSkyCoordsFilter extends BasicFilter {

    public AddSkyCoordsFilter() {
        super( "addskycoords",
               "[-epoch <expr>] [-inunit deg|rad|sex] [-outunit deg|rad|sex]\n"
             + "<insys> <outsys> "
             + "<col-id1> <col-id2> <col-name1> <col-name2>" );
    }

    public String[] getDescriptionLines() {
        return new String[] {
            "<p>Add new columns to the table representing position on the sky.",
            "The values are determined by converting a sky position",
            "whose coordinates are contained in existing columns.",
            "The <code>&lt;col-id&gt;</code> arguments give identifiers for",
            "the two input coordinate columns",
            "in the coordinate system named by",
            "<code>&lt;insys&gt;</code>, and",
            "the <code>&lt;col-name&gt;</code> arguments name",
            "the two new columns,",
            "which will be in the coordinate system named by",
            "<code>&lt;outsys&gt;</code>.",
            "The <code>&lt;insys&gt;</code> and <code>&lt;outsys&gt;</code>",
            "coordinate system specifiers are one of",
            SkySystem.getSystemUsage(),
            "</p>",
            "<p>The <code>-inunit</code> and <code>-outunit</code> flags",
            "may be used to indicate the units of the existing coordinates",
            "and the units for the new coordinates respectively;",
            "use one of",
            "<code>degrees</code>, <code>radians</code> or",
            "<code>sexagesimal</code> (may be abbreviated),",
            "otherwise degrees will be assumed.",
            "For sexagesimal, the two corresponding columns must be",
            "string-valued in forms like hh:mm:ss.s and dd:mm:ss.s",
            "respectively.",
            "</p>",
            "<p>For certain conversions, the value specified by the ",
            "<code>-epoch</code> flag is of significance.",
            "Where significant its value defaults to 2000.0.",
            "</p>",
            explainSyntax( new String[] { "expr", "col-id1", "col-id2", } ),
        };
    }

    public ProcessingStep createStep( Iterator<String> argIt )
            throws ArgException {
        String sEpoch = null;
        String sInUnit = null;
        String sOutUnit = null;
        String sInSys = null;
        String sOutSys = null;
        String sInCol1 = null;
        String sInCol2 = null;
        String sOutCol1 = null;
        String sOutCol2 = null;
        while ( argIt.hasNext() && 
                ( sInSys == null || sOutSys == null || 
                  sInCol1 == null || sInCol2 == null ||
                  sOutCol1 == null || sOutCol2 == null ) ) {
            String arg = argIt.next();
            if ( arg.startsWith( "-" ) && arg.length() > 1 ) {
                if ( arg.equals( "-epoch" ) && sEpoch == null &&
                     argIt.hasNext() ) {
                    argIt.remove();
                    sEpoch = argIt.next();
                    argIt.remove();
                }
                else if ( arg.equals( "-inunit" ) && sInUnit == null &&
                          argIt.hasNext() ) {
                    argIt.remove();
                    sInUnit = argIt.next();
                    argIt.remove();
                }
                else if ( arg.equals( "-outunit" ) && sOutUnit == null &&
                          argIt.hasNext() ) {
                    argIt.remove();
                    sOutUnit = argIt.next();
                    argIt.remove();
                }
                else {
                    throw new ArgException( "Unknown flag " + arg );
                }
            }
            else if ( sInSys == null ) {
                argIt.remove();
                sInSys = arg;
            }
            else if ( sOutSys == null ) {
                argIt.remove();
                sOutSys = arg;
            }
            else if ( sInCol1 == null ) {
                argIt.remove();
                sInCol1 = arg;
            }
            else if ( sInCol2 == null ) {
                argIt.remove();
                sInCol2 = arg;
            }
            else if ( sOutCol1 == null ) {
                argIt.remove();
                sOutCol1 = arg;
            }
            else if ( sOutCol2 == null ) {
                argIt.remove();
                sOutCol2 = arg;
            }
        }
        if ( sInSys == null || sOutSys == null ||
             sInCol1 == null || sInCol2 == null ||
             sOutCol1 == null || sOutCol2 == null ) {
            throw new ArgException( "Not enough arguments supplied" );
        }

        final SkyUnits inUnits;
        final SkyUnits outUnits;
        final SkySystem inSys;
        final SkySystem outSys;
        final double epoch;
        final String[] inCols;
        final String[] outCols;
        try {
            inUnits = SkyUnits.getUnitsFor( sInUnit );
            outUnits = SkyUnits.getUnitsFor( sOutUnit );
            inSys = SkySystem.getSystemFor( sInSys );
            outSys = SkySystem.getSystemFor( sOutSys );
            epoch = ( sEpoch == null || sEpoch.length() == 0 )
                               ? 2000.0
                               : Double.parseDouble( sEpoch );
            inCols = new String[] { sInCol1, sInCol2 };
            outCols = new String[] { sOutCol1, sOutCol2 };
        }
        catch ( IllegalArgumentException e ) {
            throw new ArgException( e.getMessage(), e );
        }

        return new ProcessingStep() {
            public StarTable wrap( StarTable base ) throws IOException {
                ColumnIdentifier identifier = new ColumnIdentifier( base );
                int[] inColIndices = new int[ 2 ];
                for ( int i = 0; i < 2; i++ ) {
                    int inColIndex = identifier.getColumnIndex( inCols[ i ] );
                    inColIndices[ i ] = inColIndex;
                    ColumnInfo baseInfo = base.getColumnInfo( inColIndex );
                    if ( ! isCompatible( inUnits.getUnitTypes()[ i ],
                                         baseInfo.getContentClass() ) ) {
                        throw new IOException(
                            "Column " + baseInfo + " not suitable for units " +
                            inUnits );
                    }
                }

                ColumnInfo[] outColInfos = new ColumnInfo[ 2 ];
                for ( int i = 0; i < 2; i++ ) {
                    ColumnInfo cinfo = new ColumnInfo( outCols[ i ] );
                    cinfo.setUnitString( outUnits.getUnitStrings()[ i ] );
                    cinfo.setContentClass( outUnits.getUnitTypes()[ i ] );
                    cinfo.setDescription( outSys
                                         .getCoordinateDescriptions()[ i ] );
                    cinfo.setUCD( outSys.getCoordinateUcds()[ i ] );
                    outColInfos[ i ] = cinfo;
                }

                ColumnSupplement inCoordsSup =
                    new PermutedColumnSupplement( base, inColIndices );
                ColumnSupplement outCoordsSup =
                        new CalculatorColumnSupplement( inCoordsSup,
                                                        outColInfos ) {
                    protected Object[] calculate( Object[] inputs ) {
                        double[] inRads =
                            inUnits.decode( inputs[ 0 ], inputs[ 1 ] );
                        double[] fk5 =
                            inSys.toFK5( inRads[ 0 ], inRads[ 1 ], epoch );
                        double[] outRads =
                            outSys.fromFK5( fk5[ 0 ], fk5[ 1 ], epoch );
                        Object[] outVals =
                            outUnits.encode( outRads[ 0 ], outRads[ 1 ] );
                        return outVals;
                    }
                };
                StarTable out = new AddColumnsTable( base, outCoordsSup );
                int ncol = out.getColumnCount();
                AddColumnFilter.checkDuplicatedName( out, ncol - 2 );
                AddColumnFilter.checkDuplicatedName( out, ncol - 1 );
                return out;
            }
        };
    }

    /**
     * Checks whether the class required by a SkyUnit specification is
     * compatible with one provided by a data column.
     *
     * @param  unitClass  class required for units
     * @param  dataClass  class provided by data
     * @return  true if the two are compatible
     */
    private static boolean isCompatible( Class<?> unitClass,
                                         Class<?> dataClass ) {
        if ( unitClass.equals( dataClass ) ) {
            return true;
        }
        else if ( Number.class.isAssignableFrom( unitClass ) &&
                  Number.class.isAssignableFrom( dataClass ) ) {
            return true;
        }
        else {
            return false;
        }
    }
}
