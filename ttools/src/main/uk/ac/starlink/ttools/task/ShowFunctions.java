package uk.ac.starlink.ttools.task;

import javax.swing.JFrame;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Executable;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.ttools.gui.MethodBrowser;
import uk.ac.starlink.ttools.jel.JELUtils;

/**
 * Task for browsing available algebraic functions.
 *
 * @author   Mark Taylor
 * @since    9 Feb 2007
 */
public class ShowFunctions implements Task, Executable {

    public String getPurpose() {
        return "Browses functions used by algebraic expression langauage";
    }

    public Parameter[] getParameters() {
        return new Parameter[ 0 ];
    }

    public Executable createExecutable( Environment env ) {
        return this;
    }

    public void execute() {
        MethodBrowser browser = new MethodBrowser();
        browser.addStaticClasses( (Class[]) JELUtils.getStaticClasses()
                                           .toArray( new Class[ 0 ] ) );
        JFrame frame = new JFrame( "Functions" );
        frame.getContentPane().add( browser );
        frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
        frame.pack();
        frame.setVisible( true );
    }
}
