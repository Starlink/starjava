package uk.ac.starlink.votable;

/**
 * Class of objects representing the different serialization formats
 * into which VOTable cell data can be written.
 * This class exemplifies the typesafe enum pattern.
 *
 * @author   Mark Taylor (Starlink)
 */
public class DataFormat {

    private final String name;

    /**
     * Private constructor which creates an instance of this class.
     *
     * @param   name  the XML tag name for the element this data lives in
     * @param   extension  a suitable file extension for streamable formats
     */
    private DataFormat( String name ) {
        this.name = name;
    }

    public String toString() {
        return name;
    }

    //
    // All legal instances of this class are created here.
    //

    /** TABLEDATA format (pure XML). */
    public static final DataFormat TABLEDATA = new DataFormat( "TABLEDATA" );

    /** FITS format. */
    public static final DataFormat FITS = new DataFormat( "FITS" );

    /** Raw binary format. */
    public static final DataFormat BINARY = new DataFormat( "BINARY" );

}
