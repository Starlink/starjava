package uk.ac.starlink.ttools.plot2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import junit.framework.TestCase;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.plot.GraphicExporter;
import uk.ac.starlink.ttools.plot.PdfGraphicExporter;
import uk.ac.starlink.ttools.plot.Picture;
import uk.ac.starlink.ttools.plot2.task.PlanePlot2Task;
import uk.ac.starlink.ttools.scheme.AttractorScheme;
import uk.ac.starlink.ttools.task.MapEnvironment;
import uk.ac.starlink.util.LogUtils;

public class OutputTest extends TestCase {

    public OutputTest() {
        LogUtils.getLogger( "uk.ac.starlink.util" ).setLevel( Level.WARNING );
        LogUtils.getLogger( "uk.ac.starlink.ttools.plot2" )
                .setLevel( Level.WARNING );
    }

    public void testExporters()
            throws IOException, TaskException, InterruptedException {
        MapEnvironment env = new MapEnvironment();
        env.setValue( "in",
                      new AttractorScheme().createTable( "100,clifford" ) );
        env.setValue( "layer", "Mark" );
        env.setValue( "x", "x" );
        env.setValue( "y", "y" );
        PlanePlot2Task task = new PlanePlot2Task();
        Picture picture = PlotUtil.toPicture( task.createPlotIcon( env ) );
        for ( GraphicExporter ex :
              GraphicExporter.getKnownExporters( PdfGraphicExporter.BASIC ) ) {
            try ( OutputStream out = new ByteArrayOutputStream() ) {
                ex.exportGraphic( picture, out );
            }
        }
    }
}
