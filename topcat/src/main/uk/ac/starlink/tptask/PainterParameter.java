package uk.ac.starlink.tptask;

import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.OutputStreamParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.xdoc.fig.FigureIcon;

/**
 * Parameter which obtains a Painter object.
 *
 * @author   Mark Taylor
 * @since    22 Apr 2008
 */
public class PainterParameter extends Parameter {

    private final OutputStreamParameter outParam_;
    private Painter painterVal_;

    /**
     * Constructor.
     *
     * @param   name   parameter name
     */
    public PainterParameter( String name ) {
        super( name );
        setDefault( "swing" );
        outParam_ = new OutputStreamParameter( "out" );
        outParam_.setPrompt( "Location for the output graphics file" );
    }

    public void setValueFromString( Environment env, String stringVal )
            throws TaskException {
        if ( "swing".equals( stringVal ) ) {
            painterVal_ = new SwingPainter();
        }
        else {
            throw new ParameterValueException( this, "Unknown format" );
        }
        super.setValueFromString( env, stringVal );
    }

    /**
     * Returns the value of this parameter as a Painter object.
     *
     * @param  env  execution environment
     * @return   painter object
     */
    public Painter painterValue( Environment env ) throws TaskException {
        checkGotValue( env );
        return painterVal_;
    }

    /**
     * Returns the output parameter associated with this parameter.
     *
     * @return  output parameter
     */
    public OutputStreamParameter getOutputParameter() {
        return outParam_;
    }

    /**
     * Painter implementation which displays the component to the screen.
     */
    private static class SwingPainter implements Painter {
        public void paintPlot( final JComponent plot ) {
            final JFrame frame = new JFrame();
            frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
            frame.getContentPane().add( plot );
            Object quitKey = "quit";
            plot.getInputMap().put( KeyStroke.getKeyStroke( 'q' ), quitKey );
            plot.getActionMap().put( quitKey, new AbstractAction() {
                public void actionPerformed( ActionEvent evt ) {
                    frame.dispose();
                }
            } );
            frame.pack();
            frame.setVisible( true );
        }
    };
}
