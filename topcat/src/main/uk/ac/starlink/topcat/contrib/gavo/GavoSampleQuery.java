package uk.ac.starlink.topcat.contrib.gavo;

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
     * List of sample queries, mainly halos.
     */
    public static final GavoSampleQuery[] HALO_SAMPLES = new GavoSampleQuery[] {

        new GavoSampleQuery( "H1",
            new String[] {
                "Find halos at a given redshift (snapnum) within a certain",
                "part of the simulation (X,Y,Z)",
            },
            new String[] {
                "select *",
                "  from millimil..MPAHalo",
                " where snapnum=50 ",
                "   and np between 100 and 1000 ",
                "   and x between 10 and 20",
                "   and y between 10 and 20",
                "   and z between 10 and 20",
            }
        ),

        new GavoSampleQuery( "H2",
            new String[] {
                "Find the whole progenitor tree, in depth-first order",
                "of a halo identified by its haloId",
            },
            new String[] {
                "select PROG.*",
                "  from millimil..MPAHalo PROG,",
                "       millimil..MPAHalo DES",
                " where DES.haloId = 1",
                "   and PROG.haloId between DES.haloId and DES.lastprogenitorId"
            }
        ),

        new GavoSampleQuery( "H3",
            new String[] {
                "Find the progenitors at a given redshift (snapnum)",
                "of all halos of mass(np)>=4000",
                "at a later redshift (snapnum).",
                "The progenitors are limited in mass as well.",
            },
            new String[] {
                "select DES.haloId as descendant_id,",
                "       DES.np as descendant_mass,",
                "       PROG.*",
                "  from millimil..MPAHalo DES,",
                "       millimil..MPAHalo PROG",
                " where DES.snapnum = 63",
                "   and DES.np > 4000",
                "   and PROG.haloId between DES.haloId"
                + " and DES.lastprogenitorId",
                "   and PROG.snapnum = 30",
                "   and PROG.np > 100",
                " order by DES.np desc, PROG.np desc",
            }
        ),

        new GavoSampleQuery( "H4",
            new String[] {
                "Find all the halos of mass >= 1000 that have just had",
                "a major merger, defined by having at least two progenitors",
                "of mass >= 0.2*descendant mass",
            },
            new String[] {
                "select D.haloId,",
                "       D.snapnum,",
                "       D.np as d_np,",
                "       P1.np as p1_np,",
                "       P2.np as p2_np",
                "  from millimil..MPAHalo P1,",
                "       millimil..MPAHalo P2,",
                "       millimil..MPAHalo D",
                " where P1.SNAPNUM=P2.SNAPNUM",
                "   and P1.haloId < P2.haloId",
                "   and P1.descendantId = D.haloId",
                "   and P2.descendantId = D.haloId",
                "   and P1.np >= .2*D.np",
                "   and P2.np >= .2*D.np",
                "   and D.np > 1000",
            }
        ),

        new GavoSampleQuery( "H5",
            new String[] {
                "Find the mass function of halos at z=0 using logarithmic "
                + "intervals",
            },
            new String[] {
                "select power(10, .1*(.5+floor(log10(np)/.1))) as mass,",
                "        count(*) as num",
                "   from millimil..MPAHalo",
                "  where snapnum=63",
                " group by power(10, .1*(.5+floor(log10(np)/.1)))",
                " order by mass",
            }
        ),

        new GavoSampleQuery( "HF1",
            new String[] {
                "Find all the halos residing in background overdensities",
                "between 2 and 3, at Gaussian smoothing radius 5 Mpc/h",
            },
            new String[] {
                "select h.*",
                "  from millimil..MPAHalo h,",
                "       millimil..MMField f",
                " where f.g5 between 2 and 3",
                "   and f.snapnum=63",
                "   and f.snapnum = h.snapnum",
                "   and f.phkey = h.phkey",
            }
        ),
 
        new GavoSampleQuery( "HF2",
            new String[] {
                "Find halo mass functions at overdensities "
                + "at two different values",
            },
            new String[] {
                "select 0*haloId as lim,",
                "       power(10, .1*(.5+floor(log10(h.np)/.1))) as mass,",
                "       count(*) as num",
                "  from millimil..MPAHalo h, ",
                "       millimil..MMField f",
                " where f.g5 between 3 and 5",
                "   and f.snapnum=63",
                "   and f.snapnum = h.snapnum",
                "   and f.phkey = h.phkey    ",
                " group by 0*haloId, power(10, .1*(.5+floor(log10(h.np)/.1)))",
                "union",
                " select 0*haloId+1 as lim,",
                "        power(10, .1*(.5+floor(log10(h.np)/.1))) as mass,",
                "        count(*) as num",
                "  from millimil..MPAHalo h, ",
                "       millimil..MMField f",
                " where f.g5 between .2 and .4",
                "   and f.snapnum=63",
                "   and f.snapnum = h.snapnum",
                "   and f.phkey = h.phkey    ",
                " group by 0*haloId+1, "
                + "power(10, .1*(.5+floor(log10(h.np)/.1)))",
                "order by lim,mass",
            }
        ),

        new GavoSampleQuery( "HF3",
            new String[] {
                "Find formation time dependency on background overdensities",
                "for halos in particular mass bin",
            },
            new String[] {
                "select zmax, avg(g5) as g5, stdev(g5) as g5err,",
                "       avg(g10) as g10, stdev(g10) as g10err,",
                "       count(*) as num",
                "  from millimil..mmfield f, ",
                "       ( select des.haloId, des.np,"
                + " des.phkey,max(PROG.redshift) as zmax",
                "           from millimil..MPAHalo PROG,",
                "                millimil..MPAHalo DES",
                "          where DES.snapnum = 63",
                "            and PROG.haloId between DES.haloId"
                + " and DES.lastprogenitorId",
                "            and prog.np >= des.np/2    ",
                "            and des.np between 100 and 200",
                "            and des.haloId = des.firsthaloinfofgroupid",
                "          group by des.haloId, des.np ,des.phkey    ",
                "       ) t",
                " where t.phkey = f.phkey ",
                "   and f.snapnum=63",
                "group by zmax ",
            }
        ),
    };


    /**
     * List of sample queries, mainly galaxies.
     */
    public static final GavoSampleQuery[] GAL_SAMPLES = new GavoSampleQuery[] {
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

        new GavoSampleQuery( "G5",
            new String[] {
                "Find the luminosity function of galaxies at z=0",
            },
            new String[] {
                "select .2*(.5+floor(mag_b/.2)) as mag,",
                "       count(*) as num",
                "  from millimil..DeLucia2006a",
                " where mag_b < -10",
                "   and snapnum=63",
                " group by .2*(.5+floor(mag_b/.2))",
                " order by mag",
            }
        ),

        new GavoSampleQuery( "G6",
            new String[] {
                "Find the Tully-Fisher relation, Mag vs Vvir for galaxies",
                "with bulge/total mass ratio < 0.1",
            },
            new String[] {
                "select vVir, mag_b, mag_v, mag_i, mag_r, mag_k",
                "  from millimil..DeLucia2006a",
                " where (bulgeMass < 0.1*stellarMass or bulgeMass is null)",
                "   and snapnum = 41",
            }
        ),

        new GavoSampleQuery( "HG1",
            new String[] {
                "Find the conditional luminosity functions",
                "for galaxies in two ranges of halo mass",
            },
            new String[] {
                "select 0*galaxyId as lim, ",
                "       .2*(.5+floor(mag_b/.2)) as mag,",
                "       count(*) as num",
                "  from millimil..DeLucia2006a g, ",
                "       millimil..MPAHalo h",
                " where h.np between 2000 and 3000",
                "   and h.snapnum=63",
                "   and g.haloId = h.haloId",
                "   and g.mag_b < 0",
                " group by 0*galaxyId, .2*(.5+floor(mag_b/.2))",
                "union",
                "select 0*galaxyId+1 as lim, ",
                "       .2*(.5+floor(mag_b/.2)) as mag,",
                "       count(*) as num",
                "  from millimil..DeLucia2006a g, ",
                "       millimil..MPAHalo h",
                " where h.np between 200 and 300",
                "   and h.snapnum= 63",
                "   and g.haloId = h.haloId",
                "   and g.mag_b < 0",
                " group by 0*galaxyId+1, .2*(.5+floor(mag_b/.2))",
                "order by lim,  mag  ",
            }
        ),

        new GavoSampleQuery( "HG2",
            new String[] {
                "Find average galaxy properties as function of halo mass",
            },
            new String[] {
                "select power(10, .1*(.5+floor(log(g.np)/.1))) as halo_np,",
                "       avg(g.stellarMass) as stars_avg,",
                "       max(g.stellarMass) as stars_max,",
                "       avg(g.bulgeMass) as bulge_avg,",
                "       max(g.bulgeMass) as bulge_max,",
                "       avg(g.mag_b-g.mag_v) as color_avg",
                "  from millimil..DeLucia2006a g",
                " where g.snapnum= 63 ",
                "   and g.mag_b < 0",
                " group by halo_np",
                " order by halo_np",
            }
        ),

        new GavoSampleQuery( "GF2",
            new String[] {
                "Find galaxy luminosity functions in overdensities",
                "at two differnt values",
            },
            new String[] {
                "select 0*galaxyId as lim,",
                "       .2*(.5+floor(mag_b/.2)) as mag,",
                "       count(*) as num",
                "  from millimil..DeLucia2006a g,",
                "       millimil..MMField f",
                " where f.g5 between 3 and 5",
                "   and f.snapnum=63",
                "   and f.snapnum = g.snapnum",
                "   and f.phkey = g.phkey",
                "   and g.mag_b < 0 ",
                " group by 0*galaxyId, .2*(.5+floor(mag_b/.2))",
                "union",
                " select 0*galaxyId+1 as lim,",
                "        .2*(.5+floor(mag_b/.2)) as mag,",
                "        count(*) as num",
                "  from millimil..DeLucia2006a g,",
                "       millimil..MMField f",
                " where f.g5 between .2 and .4",
                "   and f.snapnum=63",
                "   and f.snapnum = g.snapnum",
                "   and f.phkey = g.phkey",
                "   and g.mag_b < 0 ",
                " group by 0*galaxyId+1, .2*(.5+floor(mag_b/.2))",
                "order by lim,  mag",
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
