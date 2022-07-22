package uk.ac.starlink.ttools.build;

import java.io.IOException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import javax.xml.transform.TransformerException;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.ttools.plot2.data.Input;
import uk.ac.starlink.ttools.plot2.geom.TimeDataGeom;
import uk.ac.starlink.ttools.plot2.task.AbstractPlot2Task;
import uk.ac.starlink.ttools.server.PlotServlet;

/**
 * Write some miscallaneous auto-documentation files for ingestion
 * in the user document.
 *
 * <p>Invoke this from the class's <code>main</code> method.
 *
 * @author   Mark Taylor
 * @since    15 Apr 2020
 */
public class MiscDoc {

    /**
     * Write text to a file in the current directory.
     *
     * @param  filename  relative filename
     * @param  text   text to write
     */
    private static final void writeData( String filename, String text )
            throws IOException {
        File file = new File( new File( "." ), filename );
        System.out.println( "Writing " + filename );
        Writer out = new BufferedWriter(
                         new OutputStreamWriter(
                             new FileOutputStream( file ) ) );
        out.write( text );
        out.close();
    }

    /**
     * Writes miscellaneous documentation files to the current directory.
     */
    public static void main( String[] args )
            throws IOException, TransformerException {
        Input timeInput = TimeDataGeom.T_COORD.getInput();
        String timeSuffix = AbstractPlot2Task.EXAMPLE_LAYER_SUFFIX;
        Parameter<?> ttypeParam = 
            AbstractPlot2Task
           .createDomainMapperParameter( timeInput, timeSuffix );
        writeData( "ttypeN-param.xml",
                   UsageWriter.xmlItem( ttypeParam, null, false ) );
        writeData( "plotserv-syntax.xml",
                   PlotServlet.getXmlSyntaxDocumentation() );
    }
}
