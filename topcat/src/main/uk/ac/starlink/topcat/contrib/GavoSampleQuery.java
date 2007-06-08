package uk.ac.starlink.topcat.contrib;

/**
 * Encapsulates example queries used by the GAVO loader.
 *
 * @author   Mark Taylor
 * @author   Gerard Lemson
 * @since    8 Jun 2007
 */
public class GavoSampleQuery {

    private final String name_;
    private final String description_;
    private final String text_;

    /**
     * List of sample queries.
     */
    public static final GavoSampleQuery[] SAMPLES = new GavoSampleQuery[] {
        new GavoSampleQuery( "G1",
            new String[] {
                "find halos at a given redshift (snapnum) within",
                "a certain part of the simulation volume (X,Y,Z)",
            },
            new String[] {
                "select *",
                "  from millimil..DeLucia2006a",
                "  where snapnum=63",
                "  and mag_b between -26 and -18",
                "  and x between 10 and 20",
                "  and y between 10 and 20",
                "  and z between 10 and 20",
            }
        ),

        new GavoSampleQuery( "G2",
            new String[] {
                "Find the whole progenitor tree, in depth-first order,",
                "of a halo identified by its haloId",
            },
            new String[] {
                "select PROG.*",
                "  from millimil..DeLucia2006a PROG,",
                "       millimil..DeLucia2006a DES",
                "  where DES.galaxyId = 1",
                "    and PROG.galaxyId between DES.galaxyId "
                                             + "and DES.lastprogenitorId",
            }
        ),

        new GavoSampleQuery( "G3",
            new String[] {
                "Find the progenitors at a given redshift (snapnum)",
                "of all halos of mass(np)>4000 at a later redshift (snapnum)",
            },
            new String[] {
                "select DES.galaxyId as descendant_id,",
                "       DES.stellarMass as descendant_mass,",
                "       PROG.*",
                "  from millimil..DeLucia2006a DES,",
                "       millimil..DeLucia2006a PROG",
                " where DES.snapnum = 63",
                "   and DES.mag_b < -20",
                "   and PROG.galaxyId between DES.galaxyId and "
                                           + "DES.lastprogenitorId",
                "   and PROG.snapnum = 30",
                "   and PROG.mag_b < -19",
                " order by DES.mag_b asc, PROG.mag_b asc",
            }
        ),

        new GavoSampleQuery( "G4",
            new String[] {
                "Find all the halos of mass >1000 that have just had",
                "a major merger, defined by having at least two progenitors",
                "of mass > 0.2*descendant mass.",
            },
            new String[] {
                "select D.galaxyId,",
                "       D.snapnum,",
                "       D.mag_b as d_mag_b,",
                "       D.sfr as d_sfr,",
                "       P1.mag_b as p1_mag_b,",
                "       P2.mag_b as p2_mag_b,",
                "       D.stellarMass as d_mass,",
                "       P1.stellarMass as p1_mass,",
                "       P2.stellarMass as p2_mass",
                "  from millimil..DeLucia2006a P1,",
                "       millimil..DeLucia2006a P2,",
                "       millimil..DeLucia2006a D",
                " where P1.SNAPNUM=P2.SNAPNUM",
                "   and P1.galaxyId< P2.galaxyId",
                "   and P1.descendantId = D.galaxyId",
                "   and P2.descendantId = D.galaxyId",
                "   and P1.stellarMass >= .2*D.stellarMass",
                "   and P2.stellarMass >= .2*D.stellarMass",
                "   and D.mag_b <-20",
            }
        ),
    };

    /**
     * Constructor.
     *
     * @param   name   query ID
     * @param   descriptionLines  lines for short description (tooltip)
     * @param   textLines   lines for SQL query text
     */
    public GavoSampleQuery( String name, String[] descriptionLines,
                            String[] textLines ) {
        name_ = name;
        description_ = concatLines( descriptionLines ).trim();
        text_ = concatLines( textLines );
    }

    /**
     * Returns query ID.
     *
     * @return  name
     */
    public String getName() {
        return name_;
    }

    /**
     * Returns query description.
     *
     * @return  description
     */
    public String getDescription() {
        return description_;
    }

    /**
     * Returns SQL text for query.
     *
     * @return  text
     */
    public String getText() {
        return text_;
    }

    /**
     * Concatenates lines to form a single string.
     *
     * @param  lines  input lines
     * @return  output string
     */
    private static String concatLines( String[] lines ) {
        StringBuffer sbuf = new StringBuffer();
        for ( int i = 0; i < lines.length; i++ ) {
            sbuf.append( lines[ i ] )
                .append( " \n" );
        }
        return sbuf.toString();
    }
}
