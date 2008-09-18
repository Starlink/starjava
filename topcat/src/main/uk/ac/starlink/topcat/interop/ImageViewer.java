package uk.ac.starlink.topcat.interop;

/**
 * Defines the function of viewing an image.
 *
 * @author   Mark Taylor
 * @since    21 Dec 2007
 */
public interface ImageViewer {

    /**
     * Consume an image somehow.
     *
     * @param  label   image label, possible used as window title
     * @param  location  URL giving location of image
     * @return   true if operation apparently completed successfully
     */
    boolean viewImage( String label, String location );
}
