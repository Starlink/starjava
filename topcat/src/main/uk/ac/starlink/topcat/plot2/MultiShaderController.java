package uk.ac.starlink.topcat.plot2;

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
     */
    public MultiShaderController( ZoneFactory zfact,
                                  MultiConfigger configger ) {
        super( createShaderControllerFactory( configger ), zfact, configger );
    }

    /**
     * Creates a controller factory for shader controls.
     *
     * @param  mConfigger  manages shared config items
     * @return  factory
     */
    private static ControllerFactory<ShaderControl>
            createShaderControllerFactory( final MultiConfigger mConfigger ) {
        return new ControllerFactory<ShaderControl>() {
            public int getControlCount() {
                return 1;
            }
            public ShaderControl createController() {
                return new ShaderControl( mConfigger );
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
