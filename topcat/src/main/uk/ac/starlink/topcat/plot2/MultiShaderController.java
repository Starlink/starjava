package uk.ac.starlink.topcat.plot2;

import uk.ac.starlink.topcat.ToggleButtonModel;

/**
 * MultiController that works with ShaderControl instances.
 *
 * @author   Mark Taylor
 * @since    27 May 2016
 */
public class MultiShaderController extends MultiController<ShaderControl> {

    /**
     * Constructor.
     *
     * @param  zfact  zone id factory
     * @param  configger   manages per-zone shader config items
     * @param  auxLockModel   single toggle model to control whether aux
     *                        axes are locked; could be done on a per-zone
     *                        basis but currently is not
     */
    public MultiShaderController( ZoneFactory zfact,
                                  MultiConfigger configger,
                                  ToggleButtonModel auxLockModel ) {
        super( createShaderControllerFactory( configger, auxLockModel ),
               zfact, configger );
    }

    /**
     * Creates a controller factory for shader controls.
     *
     * @param  mConfigger  manages shared config items
     * @param  auxLockModel   toggle model to control whether
     *                        aux axes are locked
     * @return  factory
     */
    private static ControllerFactory<ShaderControl>
            createShaderControllerFactory( final MultiConfigger mConfigger,
                                           final ToggleButtonModel lockModel ) {
        return new ControllerFactory<ShaderControl>() {
            public int getControlCount() {
                return 1;
            }
            public ShaderControl createController() {
                return new ShaderControl( mConfigger, lockModel );
            }
            public Control[] getControls( ShaderControl control ) {
                return new Control[] { control };
            }
            public Configger getConfigger( ShaderControl control ) {
                return control;
            }
        };
    }
}
