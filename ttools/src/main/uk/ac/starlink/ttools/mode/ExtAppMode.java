package uk.ac.starlink.ttools.mode;

import cds.tools.ExtApp;
import gnu.jel.CompilationException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.ExecutionException;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.DocUtils;
import uk.ac.starlink.ttools.TableConsumer;
import uk.ac.starlink.ttools.filter.AddJELColumnTable;
import uk.ac.starlink.votable.VOTableWriter;

/**
 * Debugging mode for testing TOPCAT's ExtApp functionality.
 * This is the channel for control from Aladin.
 * This mode is not intended for use except in a debugging context.
 *
 * @author   Mark Taylor
 * @since    18 Oct 2005
 */
public class ExtAppMode implements ProcessingMode {

    private final Parameter selParam_;
    private final Parameter showParam_;
    private final Parameter visParam_;

    public ExtAppMode() {
        selParam_ = new Parameter( "select" );
        selParam_.setUsage( "<expr>" );
        selParam_.setNullPermitted( true );

        showParam_ = new Parameter( "show" );
        showParam_.setUsage( "<expr>" );
        showParam_.setNullPermitted( true );

        visParam_ = new ChoiceParameter( "visible",
                                         new String[] { "true", "false" } );
        visParam_.setNullPermitted( true );
    }

    public Parameter[] getAssociatedParameters() {
        return new Parameter[] {
            selParam_,
            showParam_,
            visParam_,
        };
    }

    public String getDescription() {
        return DocUtils.join( new String[] {
            "<p>Talks to TOPCAT as a CDS-style <code>ExtApp</code>,",
            "used for TOPCAT ExtApp implementation testing purposes.",
            "</p>",
            "<p>ExtApp functionality is moribund, having more or less",
            "been superceded by PLASTIC.",
            "</p>",
       } );
    }

    public TableConsumer createConsumer( Environment env )
            throws TaskException {
        final String selExpr = selParam_.stringValue( env );
        final String showExpr = showParam_.stringValue( env );
        String visStr = visParam_.stringValue( env );
        final Boolean vis = visStr == null 
                          ? null
                          : Boolean.valueOf( visParam_.stringValue( env ) );
        return new TableConsumer() {
            public void consume( StarTable table ) throws IOException {
                try {
                    table = doctor( table );
                }
                catch ( CompilationException e ) {
                    throw new RuntimeException( "Unexpected", e );
                }
                ByteArrayOutputStream ostrm = new ByteArrayOutputStream();
                new StarTableOutput()
                   .writeStarTable( table, ostrm, new VOTableWriter() );
                InputStream istrm = 
                    new ByteArrayInputStream( ostrm.toByteArray() );
                getExtApp().loadVOTable( new DummyExtApp(), istrm );
    
                if ( selExpr != null ) {
                    String[] selIds = getSelectedIds( table, selExpr );
                    getExtApp().selectVOTableObject( selIds );
                }

                if ( showExpr != null ) {
                    String[] showIds = getSelectedIds( table, showExpr );
                    getExtApp().showVOTableObject( showIds );
                }

                if ( vis != null ) {
                    getExtApp().setVisible( vis.booleanValue() );
                }
            }
        };
    }

    private StarTable doctor( StarTable table ) throws CompilationException {
        return new AddJELColumnTable( table, new ColumnInfo( "_OID" ),
                                       "\"id_\"+$0", -1 );
    }

    private String[] getSelectedIds( StarTable table, String selexpr )
            throws IOException {
        int idcol = table.getColumnCount() - 1;
        ColumnInfo flagInfo = new ColumnInfo( "flag", Boolean.class, null );
        try {
            table = new AddJELColumnTable( table, flagInfo, selexpr, -1 );
        }
        catch ( CompilationException e ) {
            throw (IOException) new IOException( "Bad expression " + selexpr )
                               .initCause( e );
        }
        int flagcol = table.getColumnCount() - 1;
        List idList = new ArrayList();
        RowSequence rseq = table.getRowSequence();
        while ( rseq.next() ) {
            if ( ((Boolean) rseq.getCell( flagcol )).booleanValue() ) {
                idList.add( rseq.getCell( idcol ) );
            }
        }
        rseq.close();
        return (String[]) idList.toArray( new String[ 0 ] );
    }

    private ExtApp getExtApp() throws IOException {
        try {
            Object controlWindow = 
                Class.forName( "uk.ac.starlink.topcat.ControlWindow" )
                     .getMethod( "getInstance", new Class[ 0 ] )
                     .invoke( null, new Object[ 0 ] );
            return (ExtApp) controlWindow.getClass()
                           .getMethod( "getExtApp", new Class[ 0 ] )
                           .invoke( controlWindow, new Object[ 0 ] );
        }
        catch ( Throwable e ) {
            throw (IOException) new IOException( "Can't get ExtApp" )
                               .initCause( e );
        }
    }

    private static class DummyExtApp implements ExtApp {
        public void loadVOTable( ExtApp app, InputStream istrm ) {
        }
        public void setVisible( boolean flag ) {
        }
        public String execCommand( String cmd ) {
            return "No action";
        }
        public void showVOTableObject( String[] oids ) {
        }
        public void selectVOTableObject( String[] oids ) {
        }
        public String toString() {
            return "STILTS";
        }
    }
}
