package uk.ac.starlink.treeview;

import java.io.IOException;
import uk.ac.starlink.array.OrderedNDShape;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.array.Type;

public class StatsViewer extends StyledTextArea {

    public StatsViewer( NDArray nda ) throws IOException {
        StatsValues stats = new StatsValues( nda );

        Type type = nda.getType();
        OrderedNDShape oshape = nda.getShape();

        addTitle( "Array statistics" );

        addKeyedItem( "Sum", (float) stats.total );
        addKeyedItem( "Mean", (float) stats.mean );
        addKeyedItem( "Standard deviation", 
                      (float) Math.sqrt( stats.variance ) );
        addSeparator();
        addKeyedItem( "Minimum pixel value", stats.minValue );
        addKeyedItem( "      at pixel", NDShape.toString( stats.minPosition ) );
        addKeyedItem( "Maximum pixel value", stats.maxValue );
        addKeyedItem( "      at pixel", NDShape.toString( stats.maxPosition ) );
        addSeparator();
        long npix = oshape.getNumPixels();
        long ngood = stats.numGood;
        long nbad = npix - ngood;
        addKeyedItem( "Total number of pixels", npix );
        addKeyedItem( "Number of good pixels", ngood );
        addKeyedItem( "Number of bad pixels", nbad );
        addKeyedItem( "Percentage of good pixels",
                      (float) ( 100.0 * ngood / npix ) + "%" );
    }
}
