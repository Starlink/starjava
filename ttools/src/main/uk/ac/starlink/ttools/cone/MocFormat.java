package uk.ac.starlink.ttools.cone;

import cds.moc.HealpixMoc;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Defines a MOC serialization format.
 *
 * @author   Mark Taylor
 * @since    8 Dec 2016
 */
public interface MocFormat {

    /**
     * Outputs a given MOC to a given stream.
     *
     * @param  moc  MOC
     * @param  out  destination stream
     */
    public void writeMoc( HealpixMoc moc, OutputStream out ) throws IOException;
}
