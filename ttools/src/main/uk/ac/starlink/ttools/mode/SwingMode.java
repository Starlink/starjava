package uk.ac.starlink.ttools.mode;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.gui.StarJTable;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.ttools.DocUtils;
import uk.ac.starlink.ttools.TableConsumer;
import uk.ac.starlink.util.gui.SizingScrollPane;

/**
 * Processing mode for displaying the table in a scrollable Swing window.
 *
 * @author   Mark Taylor
 * @since    6 Jul 2014
 */
public class SwingMode implements ProcessingMode, TableConsumer {

    public TableConsumer createConsumer( Environment env ) {
        return this;
    }

    public Parameter[] getAssociatedParameters() {
        return new Parameter[ 0 ];
    }

    public String getDescription() {
        return DocUtils.join( new String[] {
            "<p>Displays the table in a scrollable window.",
            "</p>",
        } );
    }

    public void consume( StarTable table ) {
        StarJTable jt = new StarJTable( table, true );
        jt.configureColumnWidths( jt, 600, 500 );
        final JFrame frame = new JFrame();
        frame.getContentPane().add( new SizingScrollPane( jt ) );
        frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
        Object quitKey = "quit";
        jt.getInputMap().put( KeyStroke.getKeyStroke( 'q' ), quitKey );
        jt.getActionMap().put( quitKey, new AbstractAction() {
            public void actionPerformed( ActionEvent evt ) {
                frame.dispose();
            }
        } );
        frame.pack();
        frame.setVisible( true );
    }
}
