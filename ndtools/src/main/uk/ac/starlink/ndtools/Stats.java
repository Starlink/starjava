package uk.ac.starlink.ndtools;

import java.io.IOException;
import java.io.PrintStream;
import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.array.ArrayAccess;
import uk.ac.starlink.array.BadHandler;
import uk.ac.starlink.array.ChunkStepper;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.NDArrays;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.array.OrderedNDShape;
import uk.ac.starlink.array.Requirements;
import uk.ac.starlink.array.Type;
import uk.ac.starlink.ndx.Ndx;
import uk.ac.starlink.ndx.Ndxs;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Executable;
import uk.ac.starlink.task.ExecutionException;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.task.TaskException;

/**
 * Calculates and outputs statistics for an NDX. 
 */
class Stats implements Task {

    private ExistingNdxParameter inpar;

    public Stats() {
        inpar = new ExistingNdxParameter( "in" );
        inpar.setPrompt( "Input NDX" );
        inpar.setPosition( 1 );
    }

    public String getPurpose() {
        return "Calculates statistics for the pixels of an NDX";
    }

    public Parameter[] getParameters() {
        return new Parameter[] { inpar };
    }

    public Executable createExecutable( Environment env ) throws TaskException {
        return new Statter( inpar.ndxValue( env ), env.getOutputStream() );
    }

    private class Statter implements Executable {

        final Ndx ndx;
        final PrintStream pstrm;

        Statter( Ndx ndx, PrintStream pstrm ) {
            this.ndx = ndx;
            this.pstrm = pstrm;
        }

        public void execute() throws IOException, ExecutionException {
            String title;
            long npix;
            StatsValues answers;
            NDArray nda = null;

            try {
                /* Get the NDArray to work on. */
                title = ndx.hasTitle() ? ndx.getTitle() : null;
                nda = Ndxs.getMaskedImage( ndx );
                npix = nda.getShape().getNumPixels();

                /* Calculate the results. */
                answers = new StatsValues( nda );
            }

            /* Tidy up in any case. */
            finally {
                if ( nda != null ) {
                    nda.close();
                }
            }

            /* Prepare the results for output. */
            float sum = (float) answers.total;
            float mean = (float) answers.mean;
            float stdev = (float) Math.sqrt( answers.variance );
            Number minval = answers.minValue;
            Number maxval = answers.maxValue;
            String minpos = NDShape.toString( answers.minPosition );
            String maxpos = NDShape.toString( answers.maxPosition );
            long ngood = answers.numGood;
            long nbad = npix - ngood;
            float pcgood = (float) ( 100. * ngood / (double) npix );
            float pcbad = (float) ( 100. * nbad / (double) npix );

            /* Output the results. */
            pstrm.println( "Title:   " + title );
            if ( ngood > 0 ) {
                pstrm.println( "   Pixel sum              : " + sum );
                pstrm.println( "   Pixel mean             : " + mean );
                pstrm.println( "   Standard deviation     : " + stdev );
                pstrm.println( "   Minimum pixel value    : " + minval );
                pstrm.println( "      At pixel            : " + minpos );
                pstrm.println( "   Maximum pixel value    : " + maxval );
                pstrm.println( "      At pixel            : " + maxpos );
                pstrm.println( "   Total number of pixels : " + npix );
                pstrm.println( "   Number of pixels used  : " + ngood 
                                                      + " (" + pcgood + "%)" );
                if ( nbad > 0 ) {
                    pstrm.println( "   No. of pixels excluded : " + nbad 
                                                      + " (" + pcbad + "%)" );
                }
            }
            else {
                pstrm.println( "No good pixels" );
            }
            pstrm.println();
        }
    }
}
