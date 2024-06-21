package uk.ac.starlink.table.join;

import javax.swing.DefaultBoundedRangeModel;
import javax.swing.SwingUtilities;

/**
 * ProgressIndicator implementation that can serve as the model for
 * a {@link javax.swing.JProgressBar}.
 * Calls to <code>startStage</code>, <code>setLevel</code>
 * and <code>endStage</code>
 * cause the state of the {@link javax.swing.BoundedRangeModel} that
 * this implements to be updated accordingly (asynchronously of course).
 * The string messages passed to <code>startStage</code> and
 * <code>logMessage</code> are ignored -
 * subclasses should override these methods (calling the superclass
 * implementations as well) to do something with these strings.
 *
 * @author   Mark Taylor (Starlink)
 * @since    24 Mar 2004
 */
public class RangeModelProgressIndicator extends DefaultBoundedRangeModel
                                         implements ProgressIndicator, 
                                                    Runnable {

    double level;
    double lastUpdatedLevel;
    final Profiler profiler;
    final static int MAX = 1000;
    final static double DMAX = (double) MAX;
  
    public RangeModelProgressIndicator( boolean profile ) {
        super( 0, MAX, 0, MAX );
        profiler = profile ? new Profiler() : null;
    }

    public void startStage( String stage ) {
        level = 0.0;
        lastUpdatedLevel = 0.0;
        updateNow();
        if ( profiler != null ) {
            profiler.reset();
        }
    }

    public void setLevel( double lev ) throws InterruptedException {
        this.level = lev;
        if ( level - lastUpdatedLevel > 0.01 ) {
            lastUpdatedLevel = level;
            updateNow();
        }
    }

    public void endStage() {
        level = 0.0;
        lastUpdatedLevel = 0.0;
        updateNow();
        if ( profiler != null ) {
            logMessage( profiler.report() );
        }
    }

    public void logMessage( String msg ) {
    }

    private void updateNow() {
        SwingUtilities.invokeLater( this );
    }

    /**
     * Updates the state of the <code>BoundedRangeModel</code> - should only
     * be called from the event dispatch thread.
     */
    public void run() {
        setValue( (int) ( level * DMAX ) );
        fireStateChanged();
    }
}
