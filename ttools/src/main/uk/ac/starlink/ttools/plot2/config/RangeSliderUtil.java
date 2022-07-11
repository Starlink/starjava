package uk.ac.starlink.ttools.plot2.config;

import com.jidesoft.plaf.LookAndFeelFactory;
import com.jidesoft.plaf.UIDefaultsLookup;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoundedRangeModel;
import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;

/**
 * Utility classes for JSlider components that can define a range.
 * The JSlider API (especially BoundedRangeModel) is capable of
 * working with (2-element) ranges rather than single values.
 * The Swing JSlider component only has a single value, but there are
 * external implementations that provide the range functionality.
 * Since the JSlider API is up to the job, we don't define a
 * new interface here, but the methods here let you construct
 * and use range-capable JSlider instances.
 *
 * @author   Mark Taylor
 * @since    10 Mar 2017
 */
public class RangeSliderUtil {

    private static boolean jideBroken_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.plot2.config" );

    /**
     * Private constructor prevents instantiation.
     */
    private RangeSliderUtil() {
    }

    /**
     * Constructs a range-capable slider instance.
     * If possible, an instance of <code>com.jidesoft.swing.RangeSlider</code>
     * is returned, but if that fails it falls back to the
     * (uglier, less capable) local variant.
     *
     * <p>JIDE failure can happen in one of (at least?) two ways.
     * First, if the current Look&amp;Feel is not supported by the
     * JIDE component, which doesn't support for example Nimbus(?).
     * Second, if the JIDE class refers to a JRE class that is not present;
     * this appears to happen on OSX Java 10, where a
     * java.lang.ClassNotFoundException is reported for
     * com.sun.java.swing.plaf.windows.WindowsLookAndFeel,
     * which has apparently been withdrawn in that environment.
     * Tweaking the JIDE source to avoid looking for that class,
     * or to fail more gracefully, or upgrading to a later JIDE-OSS version,
     * could probably work to avoid the latter problem.
     * However, there seems to be a lot of UI magic in the JIDE component
     * (probably unavoidable to get full L&amp;F integration), so it's
     * wise to have a fallback.
     *
     * @param  imin  minimum value
     * @param  imax  maximum value
     * @return   range slider
     */
    public static JSlider createRangeSlider( int imin, int imax ) {
        JSlider slider = createUnconfiguredRangeSlider( imin, imax );

        /* Some implementations set the initial values, some do not,
         * so make sure it's configured correctly. */
        setSliderRange( slider, imin, imax );
        return slider;
    }

    /**
     * Constructs a range-capable slider instance, without necessarily
     * configuring it fully.
     *
     * @param  imin  minimum value
     * @param  imax  maximum value
     * @return   range slider
     */
    private static JSlider createUnconfiguredRangeSlider( int imin, int imax ) {
        if ( ! jideBroken_ ) {
            try {
                return new JideRangeSlider( imin, imax );
            }
            catch ( Throwable e ) {
                jideBroken_ = true;
                logger_.log( Level.INFO,
                             "JIDE-OSS RangeSlider unavailable, using fallback"
                           + " (" + e + ")", e );
            }
        }
        return new uk.ac.starlink.ttools.plot2.config.RangeSlider( imin, imax );
    }

    /**
     * Returns the range represented by a range-capable slider.
     *
     * @param  slider  range-capable slider (presumably created by this class)
     * @return  2-element (lo,hi) array
     */
    public static int[] getSliderRange( JSlider slider ) {
        BoundedRangeModel model = slider.getModel();
        int ilo = model.getValue();
        int ihi = ilo + model.getExtent();
        return new int[] { ilo, ihi };
    }

    /**
     * Sets the range represented by a range-capable slider.
     *
     * @param  slider  range-capable slider (presumably created by this class)
     * @param  ilo   range lower value
     * @param  ihi   range upper value
     */
    public static void setSliderRange( JSlider slider, int ilo, int ihi ) {
        BoundedRangeModel model = slider.getModel();

        /* Careful when setting these, they "correct" each other,
         * which can lead to surprising results.
         * Set the extent to zero first, which means the setValue won't
         * do anything surprising, then set the extent as desired. */
        model.setExtent( 0 );
        model.setValue( ilo );
        model.setExtent( ihi - ilo );
    }

    /**
     * JIDE-OSS-based range-capable slider implementation.
     * This is a thin wrapper around com.jidesoft.swing.RangeSlider
     * that overrides some undesirable behaviour.
     */
    private static class JideRangeSlider
                   extends com.jidesoft.swing.RangeSlider {

        /**
         * Constructs a range slider.
         * May throw a runtime exception if PLAF-dependent issues
         * are encountered.
         *
         * @param  imin  minimum value
         * @param  imax  maximum value
         * @throws RuntimeException  if UI configuration fails
         */ 
        JideRangeSlider( int imin, int imax ) {
            super( imin, imax );
        }
     
        // This method is invoked by the constructor.
        @Override
        public void updateUI() {
     
            /* This code is copied directly from the JIDE-OSS source. */
            if ( UIDefaultsLookup.get( getActualUIClassID() ) == null) {
                LookAndFeelFactory.installJideExtension();
            }
            try {
                Class<?> uiClass =
                    Class.forName( UIManager.getString( getActualUIClassID() ));
                Class<JComponent> acClass = JComponent.class;
                Method m = uiClass.getMethod( "createUI",
                                              new Class<?>[] { acClass } );
                if ( m != null ) {
                    Object uiObject = m.invoke( null, new Object[]{this} );
                    setUI( (ComponentUI) uiObject );
                } 
            }

            /* But here, the JIDE source does an (evil) e.printStackTrace().
             * Instead we want to throw the exception so it can be caught
             * higher up and recovered from. */
            catch ( RuntimeException e ) {
                throw e;
            }
            catch ( Exception e ) {
                throw new RuntimeException( e.toString(), e );
            }
        }
    }
}
