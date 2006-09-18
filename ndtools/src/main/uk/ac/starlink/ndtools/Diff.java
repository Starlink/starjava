package uk.ac.starlink.ndtools;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import uk.ac.starlink.array.ArrayAccess;
import uk.ac.starlink.array.BadHandler;
import uk.ac.starlink.array.ChunkStepper;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.array.OrderedNDShape;
import uk.ac.starlink.array.Type;
import uk.ac.starlink.ndx.Ndx;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Executable;
import uk.ac.starlink.task.IntegerParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.task.TaskException;

/**
 * Task which determines and prints out the discrepancies between 
 * a pair of NDXs.  
 */
class Diff implements Task {

    private ExistingNdxParameter ndx1par;
    private ExistingNdxParameter ndx2par;
    private IntegerParameter ndiffpar;

    public Diff() {
        ndx1par = new ExistingNdxParameter( "ndx1" );
        ndx1par.setPrompt( "First NDX" );
        ndx1par.setPosition( 1 );

        ndx2par = new ExistingNdxParameter( "ndx2" ); 
        ndx2par.setPrompt( "Second NDX" );
        ndx2par.setPosition( 2 );

        ndiffpar = new IntegerParameter( "ndiffs" );
        ndiffpar.setPrompt( "Number of pixel diffs displayed" );
        ndiffpar.setPosition( 3 );
        ndiffpar.setDefault( "4" );
    }

    public String getPurpose() {
        return "Reports the differences between two NDXs";
    }

    public Parameter[] getParameters() {
        return new Parameter[] { ndx1par, ndx2par, ndiffpar };
    }

    public Executable createExecutable( Environment env ) throws TaskException {
        return new Differ( ndx1par.ndxValue( env ), ndx2par.ndxValue( env ),
                           ndiffpar.intValue( env ),
                           ndx1par.stringValue( env ),
                           ndx2par.stringValue( env ),
                           env.getOutputStream() );
    }   


    private class Differ implements Executable {

        final Ndx ndx1;
        final Ndx ndx2;
        final int ndiffs;
        final String name1;
        final String name2;
        final PrintStream pstrm;

        public Differ( Ndx ndx1, Ndx ndx2, int ndiffs,
                       String ndx1name, String ndx2name,
                       PrintStream pstrm ) {
            this.ndx1 = ndx1;
            this.ndx2 = ndx2;
            this.ndiffs = ndiffs;
            this.name1 = ndx1name;
            this.name2 = ndx2name;
            this.pstrm = pstrm;
        }

        public void execute() throws IOException {

            String title1 = ndx1.hasTitle() ? ndx1.getTitle() : null;
            String title2 = ndx2.hasTitle() ? ndx2.getTitle() : null;
            if ( title1 != null && ! title1.equals( title2 ) ||
                 title1 == null && title2 != null ) {
                pstrm.println( "Title: \"" + title1 + "\" != \"" 
                                           + title2  + "\"" );
            }

            int badbits1 = (int) ndx1.getBadBits();
            int badbits2 = (int) ndx2.getBadBits();
            if ( badbits1 != badbits2 ) {
                pstrm.println( "Badbits: " + badbits1 + " != " + badbits2 );
            }

            NDArray im1 = ndx1.getImage();
            NDArray im2 = ndx2.getImage();
            List idiffs = compareArrays( im1, im2, ndiffs );
            if ( idiffs.size() > 0 ) {
                pstrm.println( "Image:" );
                for ( Iterator it = idiffs.iterator(); it.hasNext(); ) {
                    pstrm.println( "    " + (String) it.next() );
                }
            }
            im1.close();
            im2.close();

            if ( ndx1.hasVariance() && ! ndx2.hasVariance() ) {
                pstrm.println( "Variance: present in " + name1 
                             + " but not " + name2 );
            }
            else if ( ! ndx1.hasVariance() && ndx2.hasVariance() ) {
                pstrm.println( "Variance: present in " + name2
                             + " but not " + name1 );
            }
            else if ( ndx1.hasVariance() && ndx2.hasVariance() ) {
                NDArray var1 = ndx1.getVariance();
                NDArray var2 = ndx2.getVariance();
                List vdiffs = compareArrays( var1, var2, ndiffs );
                if ( vdiffs.size() > 0 ) {
                    pstrm.println( "Variance:" );
                    for ( Iterator it = vdiffs.iterator(); it.hasNext(); ) {
                        pstrm.println( "    " + (String) it.next() );
                    }
                }
                var1.close();
                var2.close();
            }
        }
    }

    private List compareArrays( NDArray nda1, NDArray nda2, int ndiffs )
            throws IOException {
        List diffs = new ArrayList();
        boolean done = false;

        Type type1 = nda1.getType();
        Type type2 = nda2.getType();
        OrderedNDShape oshape1 = nda1.getShape();
        OrderedNDShape oshape2 = nda2.getShape();
        BadHandler bh1 = nda1.getBadHandler();
        BadHandler bh2 = nda2.getBadHandler();

        Type type = null;
        if ( ! type1.equals( type2 ) ) {
            diffs.add( "Types: " + type1 + " != " + type2 );
            done = true;
        }
        else {
            type = type1;
        }
        OrderedNDShape oshape = null;
        if ( ! oshape1.equals( oshape2 ) ) {
            diffs.add( "Shapes: " + oshape1 + " != " + oshape2 );
            done = true;
        }
        else { 
            oshape = oshape1;
        }

        if ( ! bh1.equals( bh2 ) ) {
            diffs.add( "BadHandlers: " + bh1 + " != " + bh2 );
        }

        if ( ! done ) {
            long npix = oshape.getNumPixels();
            ChunkStepper cit = new ChunkStepper( npix );
            int size = cit.getSize();
            Object buf1 = type.newArray( size );
            Object buf2 = type.newArray( size );
            ArrayAccess acc1 = nda1.getAccess();
            ArrayAccess acc2 = nda2.getAccess();
            int npixdiff = 0;
            double maxdiff = 0.0;
            for ( ; cit.hasNext(); cit.next() ) {
                long base = cit.getBase();
                size = cit.getSize();
                acc1.read( buf1, 0, size );
                acc2.read( buf2, 0, size );
                boolean same = 
                    ( type == Type.BYTE && 
                      Arrays.equals( (byte[]) buf1, (byte[]) buf2 ) ) ||
                    ( type == Type.SHORT &&
                      Arrays.equals( (short[]) buf1, (short[]) buf2 ) ) ||
                    ( type == Type.INT &&
                      Arrays.equals( (int[]) buf1, (int[]) buf2 ) ) ||
                    ( type == Type.FLOAT &&
                      Arrays.equals( (float[]) buf1, (float[]) buf2 ) ) ||
                    ( type == Type.DOUBLE &&
                      Arrays.equals( (double[]) buf1, (double[]) buf2 ) );
                if ( ! same ) {
                    for ( int i = 0; i < size; i++ ) {
                        Number num1 = bh1.makeNumber( buf1, i );
                        Number num2 = bh2.makeNumber( buf2, i );
                        if ( ( num1 == null && num2 == null ) || 
                             ( num1 != null && num1.equals( num2 ) ) ) {
                            // the same
                        }
                        else {
                            if ( npixdiff++ <= ndiffs ) {
                                long[] pos = oshape
                                            .offsetToPosition( base + i );
                                diffs.add( "    " + NDShape.toString( pos ) +
                                           ": " + num1 + " != " + num2 );
                            }
                            if ( num1 != null && num2 != null ) {
                                double diff = Math.abs( num1.doubleValue() - 
                                                        num2.doubleValue() );
                                if ( diff > maxdiff ) {
                                    maxdiff = diff;
                                }
                            }
                        }
                    }
                }
            }
            acc1.close();
            acc2.close();
            if ( npixdiff > ndiffs ) {
                diffs.add( "      ..." );
            }
            if ( npixdiff > 0 ) {
                diffs.add( "  " + npixdiff + "/" + npix + " pixels differ" );
                diffs.add( "  Maximum discrepancy = " + maxdiff );
            }
        }
        return diffs;
    }
}
