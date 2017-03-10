package uk.ac.starlink.ttools.plot2.config;

import com.jidesoft.plaf.LookAndFeelFactory;
import com.jidesoft.plaf.UIDefaultsLookup;
import java.lang.reflect.Method;
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

    /**
     * Private constructor prevents instantiation.
     */
    private RangeSliderUtil() {
    }

    /**
     * Constructs a range-capable slider instance.
     * The output of this factory method depends on the current Look&amp;Feel.
     * If possible, an instance of <code>com.jidesoft.swing.RangeSlider</code>
     * is returned.  But not all LAFs are supported, so in case of failure
     * it falls back to the (uglier) local variant.
     *
     * @param  imin  minimum value
     * @param  imax  maximum value
     * @return   range slider
     */
    public static JSlider createRangeSlider( int imin, int imax ) {
        try {
            return new JideRangeSlider( imin, imax );
        }
        catch ( Exception e ) {
            return new uk.ac.starlink.ttools.plot2.config
                      .RangeSlider( imin, imax );
        }
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
                Class acClass = JComponent.class;
                Method m = uiClass.getMethod( "createUI",
                                              new Class[] { acClass } );
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
