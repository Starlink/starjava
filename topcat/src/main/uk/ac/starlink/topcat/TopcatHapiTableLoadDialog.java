package uk.ac.starlink.topcat;

import uk.ac.starlink.hapi.HapiTableLoadDialog;

/**
 * HapiTableLoadDialog subclass for use with topcat.
 *
 * @author   Mark Taylor
 * @since    30 Jan 2024
 */
public class TopcatHapiTableLoadDialog extends HapiTableLoadDialog {
    public TopcatHapiTableLoadDialog() {
        super( TopcatUtils.getDocUrlHandler() );
    }
}
