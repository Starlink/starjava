package uk.ac.starlink.topcat;

import gnu.jel.CompiledExpression;
import gnu.jel.CompilationException;
import gnu.jel.Evaluator;
import gnu.jel.Library;
import uk.ac.starlink.table.ArrayColumn;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.ttools.RandomJELRowReader;
import uk.ac.starlink.util.TestCase;

public class JELTest extends TestCase {

    public JELTest( String name ) {
        super( name );
    }

    public void testLibrary() throws Throwable {
        ColumnStarTable st = ColumnStarTable.makeTableWithRows( 4 );
        st.addColumn( ArrayColumn
                     .makeColumn( "X", new int[] { 0, 1, 2, 3, } ) );
        st.addColumn( ArrayColumn
                     .makeColumn( "Y", new double[] { 0., 1., 4., 9. } ) );
        RandomJELRowReader jrr = new RandomJELRowReader( st );
        for ( int i = 0; i < 2; i++ ) {
            Library lib = TopcatJELUtils.getLibrary( jrr, i > 0 );
            CompiledExpression compex =
                Evaluator.compile( "X+$2", lib, double.class );
            for ( int j = 0; j < 3; j++ ) {
                double result = j + j * j;
                assertEquals( result, 
                              ( (Double) jrr.evaluateAtRow( compex, j ) )
                             .doubleValue() );
                jrr.setCurrentRow( j );
                assertEquals( result,
                              ( (Double) jrr.evaluate( compex ) )
                             .doubleValue() );
            }
            try {
                Evaluator.compile( "tits", lib, null );
                fail();
            }
            catch ( CompilationException e ) {
            }
        }
    }
}
