package uk.ac.starlink.treeview;

import java.io.IOException;
import javax.swing.JProgressBar;
import javax.swing.text.Document;
import javax.swing.text.BadLocationException;
import uk.ac.starlink.array.ChunkStepper;
import uk.ac.starlink.array.OrderedNDShape;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.array.Type;
import uk.ac.starlink.datanode.viewers.StyledTextArea;

/**
 * Calculates and displays useful statistics for an array (pixel mean,
 * total, variance etc).
 */
public class StatsViewer extends StyledTextArea implements Runnable {

    private NDArray nda;

    public StatsViewer( NDArray nda ) {
        this.nda = nda;
        addTitle( "Array statistics" );

        /* Calculate and fill in the statistics values in a different thread,
         * since we may be on the event dispatcher thread. */
        new Thread( this ).start();
    }

    public void run() {
        fillInData();
    }

    /**
     * Calculates the actual statisics values and writes them into the
     * text area.
     */
    public void fillInData() {
        try {

            /* Get the shape and type of the array. */
            Type type = nda.getType();
            OrderedNDShape oshape = nda.getShape();
            long npix = oshape.getNumPixels();

            /* Prepare a ChunkStepper which defines the chunks in which the
             * array is stepped through.  By doing this we can arrange for
             * callbacks to update a progress bar as each chunk is processed. */
            int chunksize = ChunkStepper.defaultChunkSize;
            int nchunk = (int) ( npix / chunksize );
            ChunkStepper stepper;
            Document doc = getDocument();
            int prebar = doc.getLength();
            if ( nchunk > 6 ) {
                final JProgressBar pbar = new JProgressBar( 0, nchunk );
                stepper = new ChunkStepper( npix, chunksize ) {
                    int ichunk = 0;
                    public void next() {
                        pbar.setValue( ++ichunk );
                        super.next();
                    }
                };

                prebar = doc.getLength();
                addSubHead( "Working..." );
                addComponent( pbar );
                addSeparator();
            }

            /* If there are only a few chunks it's not worth bothering with
             * a progress bar. */
            else {
                stepper = new ChunkStepper( npix );
            }
            int postbar = doc.getLength();

            /* Actually do the calculations. */
            StatsValues stats = new StatsValues( nda, stepper );

            /* Remove the progress from the document if we posted one. */
            if ( postbar - prebar > 0 ) {
                try {
                    doc.remove( prebar, postbar - prebar );
                }
                catch ( BadLocationException e ) {
                    throw new AssertionError( e );
                }
            }

            /* Insert the results in the document. */
            addKeyedItem( "Sum", (float) stats.total );
            addKeyedItem( "Mean", (float) stats.mean );
            addKeyedItem( "Standard deviation", 
                          (float) Math.sqrt( stats.variance ) );
            addSeparator();
            if ( stats.minValue != null && stats.maxValue != null ) {
                addKeyedItem( "Minimum pixel value", stats.minValue );
                addKeyedItem( "      at pixel",
                              NDShape.toString( stats.minPosition ) );
                addKeyedItem( "Maximum pixel value", stats.maxValue );
                addKeyedItem( "      at pixel",
                              NDShape.toString( stats.maxPosition ) );
                addSeparator();
            }
            long ngood = stats.numGood;
            long nbad = npix - ngood;
            addKeyedItem( "Total number of pixels", npix );
            addKeyedItem( "Number of good pixels", ngood );
            addKeyedItem( "Number of bad pixels", nbad );
            addKeyedItem( "Percentage of good pixels",
                          (float) ( 100.0 * ngood / npix ) + "%" );
        }
        catch ( IOException e ) {
            logError( e );
        }
    }
}
