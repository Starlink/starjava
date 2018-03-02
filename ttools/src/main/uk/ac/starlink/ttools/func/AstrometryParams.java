package uk.ac.starlink.ttools.func;

class AstrometryParams {

    /** Right ascension in degrees. */
    public final double ra;

    /** Declination in degrees. */
    public final double dec;

    /** Parallax in mas. */
    public final double plx;

    /** Proper motion in right ascension multiplied by cos(dec) in mas/year. */
    public final double pmra;

    /** Proper motion in declination in mas/year. */
    public final double pmdec;

    /** Radial velocity in barycentric km/s. */
    public final double rv;

    /** Array giving the six parameters: ra, dec, plx, pmra, pmdec, rv. */
    public final double[] params;

    /**
     * Constructs an AstrometryParams object from the six numeric values.
     *
     * @param   ra     right ascension in degrees
     * @param   dec    declination in degrees
     * @param   plx    parallax in mas
     * @param   pmra   proper motion in right ascension multiplied by cos(dec)
     *                 in mas/year
     * @param   pmdec  proper motion in declination in mas/year
     * @param   rv     radial velocity in barycentric km/s;
     *                 if NaN, is treated as zero
     */
    public AstrometryParams( double ra, double dec, double plx,
                             double pmra, double pmdec, double rv ) {
        params = new double[] {
            this.ra = ra,
            this.dec = dec,
            this.plx = plx,
            this.pmra = pmra,
            this.pmdec = pmdec,
            this.rv = Double.isNaN( rv ) ? 0 : rv,
        };
    }

    /**
     * Constructs an AstrometryParams objecct from the six-parameter array.
     *
     * @param  params 6-element array: ra, dec, plx, pmra, pmdec, rv
     */
    public AstrometryParams( double[] params ) {
        this( params[ 0 ], params[ 1 ], params[ 2 ], params[ 3 ], params[ 4 ],
              params.length < 6 ? 0 : params[ 5 ] );
    }
}
