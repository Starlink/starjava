/*
 * Copyright (C) 2006-2011 Gaia Data Processing and Analysis Consortium
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */
package gaia.cu1.tools.numeric.algebra;

import gaia.cu1.params.GaiaParam;
import gaia.cu1.tools.astro.coordinate.DirectionCosine;
import gaia.cu1.tools.exception.ErrorMessageFormat;
import gaia.cu1.tools.exception.ErrorMessageKeys;



import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.Random;

/**
 * Class representing a three-dimensional vector in Eucledian space. The space
 * is spanned by a right-handed, orthonormal triad designated [X, Y, Z] in the
 * following.
 * <p>
 * The class suppors all the usual methods to perform conventional vector
 * arithmetics such as vector and dot product.
 * 
 * @version $Id: GVector3d.java 359256 2014-04-07 17:09:18Z hsiddiqu $
 * @author aolias
 */
public class GVector3d implements Serializable, DirectionCosine,
		GVector<GVector3d>, GVectord<GVector3d> {
	/**
     * 
     */
	static final long serialVersionUID = 1L;

	/**
     * 
     */
	static private Random rng = new Random();

	/**
	 * Returns a unit vector extending along the <code>X</code> axis. Repeated
	 * calls to this method will return the same result (as opposed to
	 * {@link #unitX} whose value may be modified from the initial setting).
	 * 
	 * @return
	 */
	public static final GVector3d getUnitX() {
		return new GVector3d(1., 0., 0.);
	}

	/**
	 * Returns a unit vector extending along the <code>Y</code> axis. Repeated
	 * calls to this method will return the same result (as opposed to
	 * {@link #unitY} whose value may be modified from the initial setting).
	 * 
	 * @return
	 */
	public static final GVector3d getUnitY() {
		return new GVector3d(0., 1., 0.);
	}

	/**
	 * Returns a unit vector extending along the <code>Z</code> axis. Repeated
	 * calls to this method will return the same result (as opposed to
	 * {@link #unitZ} whose value may be modified from the initial setting).
	 * 
	 * @return
	 */
	public static final GVector3d getUnitZ() {
		return new GVector3d(0., 0., 1.);
	}

	/**
	 * Returns a unit vector extending along the diagonal in quadrant 1 Repeated
	 * calls to this method will return the same result (as opposed to
	 * {@link #diagonal} whose value may be modified from the initial setting).
	 */
	static final public GVector3d getDiagonal() {
		return new GVector3d(1., 1., 1.).normalize();
	}

	// the vector's basic components
	/**
     * 
     */
	private double x;

	// the vector's basic components
	/**
     * 
     */
	private double y;

	// the vector's basic components
	/**
     * 
     */
	private double z;

	/**
     * 
     */
	private double cache;

	/**
	 * Constructs and initializes a <code>GVector3d</code> to
	 * <code>(0, 0, 0)</code>
	 */
	public GVector3d() {
		this(0.);
	}

	/**
	 * Constructs and initializes a <code>GVector3d</code> to the tuple
	 * <code>(s, s, s)</code>.
	 * 
	 * @param s
	 *            double value to be assigned to all components
	 */
	public GVector3d(final double s) {
		this.x = s;
		this.y = s;
		this.z = s;
	}

	/**
	 * Constructs and initializes a <code>GVector3d</code> from the first 3
	 * elements of a given double array.
	 * 
	 * @param v
	 *            array of length >=3 to initialize this vector from
	 * @throws IllegalArgumentException
	 *             if input array has less than three elements
	 */
	public GVector3d(final double[] v) throws IllegalArgumentException {
		if (v.length < 3) {
			throw new IllegalArgumentException(
					"Cannot construct a GVector3d from a "
							+ "double array of length " + v.length);
		}

		this.x = v[0];
		this.y = v[1];
		this.z = v[2];
	}

	/**
	 * Constructs and initializes a <code>GVector3d</code> from a specified
	 * <code>(x, y, z)</code> tuple.
	 * 
	 * @param x
	 *            <code>x</code> coordinate
	 * @param y
	 *            <code>y</code> coordinate
	 * @param z
	 *            <code>z</code> coordinate
	 */
	public GVector3d(final double x, final double y, final double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/**
	 * Constructs and initializes a <code>GVector3d</code> from the first three
	 * elements of a given <code>GVectorNd</code>.
	 * 
	 * @param v
	 *            Vector to initialize the object from
	 * @throws IllegalArgumentException
	 *             if dimensionality of given vector is less than 3
	 */
	public GVector3d(final GVectorNd v) throws IllegalArgumentException {
		if (v.dimensionality() < 3) {
			throw new IllegalArgumentException(
					"Cannot construct a GVector3d from a "
							+ "GVectorNd with dimensionality "
							+ v.dimensionality());
		}

		this.x = v.get(0);
		this.y = v.get(1);
		this.z = v.get(2);
	}

	/**
	 * The copy constructor - constructs a new <code>GVector3d</code> from a
	 * given one by copying all elements.
	 * 
	 * @param v
	 *            Vector to set this object to
	 */
	public GVector3d(final GVector3d v) {
		this.x = v.x;
		this.y = v.y;
		this.z = v.z;
	}

	/**
	 * Constructs a unit vector from spherical coordinates.
	 * 
	 * @param lon
	 *            longitude [rad]
	 * @param lat
	 *            latitude [rad] in range [-Pi/2, +Pi/2]
	 */
	public GVector3d(final double lon, final double lat) {
		this.fromSphericalCoordinates(lon, lat);
	}

	/**
	 * 
	 * @see gaia.cu1.tools.numeric.algebra.GVector#compare(double, int, boolean)
	 */
	@Override
	public int compare(final double val, final int index,
			final boolean ascending) {
		int cv = this.compare(val, index);

		return ascending ? cv : (-cv);
	}

	/**
	 * 
	 * @see gaia.cu1.tools.numeric.algebra.GVector#compare(double, int)
	 */
	@Override
	public int compare(final double val, final int index) {
		final double elem = this.get(index);

		return (val != elem) ? ((val < elem) ? 1 : (-1)) : 0;
	}

	/**
	 * 
	 * @see gaia.cu1.tools.numeric.algebra.GVector#compare(int, int, boolean)
	 */
	@Override
	public int compare(final int i0, final int i1, final boolean ascending) {
		int cv = this.compare(i0, i1);

		return ascending ? cv : (-cv);
	}

	/**
	 * Comare to vector elements
	 * 
	 * @param i0
	 *            index of first element
	 * @param i1
	 *            index of second element
	 * @return 0/1/-1 if first element is equal/less than/greater than second
	 *         one
	 * @see gaia.cu1.tools.numeric.algebra.GVector#compare(int, int)
	 */
	@Override
	public int compare(final int i0, final int i1) {
		final double a1 = this.get(i0);
		final double a2 = this.get(i1);

		return (a1 != a2) ? ((a1 < a2) ? 1 : (-1)) : 0;
	}

	/**
	 * Re-order the elements of the vector according to a given index vector
	 * 
	 * @param index
	 *            index vector specifying the element order
	 * @return vector sorted in place
	 * @see gaia.cu1.tools.numeric.algebra.GVector#reorder(GVectori)
	 */
	@Override
	public GVector3d reorder(final GVectori<?> index) {
		final double x = this.get(index.get(0));
		final double y = this.get(index.get(1));
		final double z = this.get(index.get(2));

		this.x = x;
		this.y = y;
		this.z = z;

		return this;
	}

	/**
	 * Swaps to elements of a <code>GVector3d</code>
	 * 
	 * @param i0
	 *            index of first element
	 * @param i1
	 *            index of second element
	 * @return updated vector with the two elements swapped
	 * @see gaia.cu1.tools.numeric.algebra.GVector#swap(int, int)
	 */
	@Override
	public GVector3d swap(final int i0, final int i1) {
		if (i0 != i1) {
			final double v = this.get(i0);
			this.set(i0, this.get(i1));
			this.set(i1, v);
		}

		return this;
	}

	/**
	 * Generates a <code>GVector3d</code> with random elements.
	 * 
	 * @return A <code>GVector3d</code> with the three coordinates set to N[0,
	 *         1) uniformly distributed random numbers
	 */
	public static GVector3d random() {
		final double x = GVector3d.rng.nextDouble();
		final double y = GVector3d.rng.nextDouble();
		final double z = GVector3d.rng.nextDouble();

		return new GVector3d(x, y, z);
	}

	/**
	 * Performs a deep copy of the object.
	 * 
	 * @return deep copy of the object, i.e. all coordinates of the new vector
	 *         are set to the ones of this object
	 * @see gaia.cu1.tools.numeric.algebra.GVector#clone()
	 */
	@Override
	public GVector3d clone() {
		return new GVector3d(this);
	}

	/**
	 * Sets the elements of this vector to given 3-tuple.
	 * 
	 * @param x
	 *            new value of the <code>X</code> coordinate
	 * @param y
	 *            new value of the <code>Y</code> coordinate
	 * @param z
	 *            new value of the <code>Z</code> coordinate
	 * @return GVector3d
	 */
	public GVector3d set(final double x, final double y, final double z) {
		this.x = x;
		this.y = y;
		this.z = z;

		return this;
	}

	/**
	 * Set the coordinates of this vectors to the ones of a given vector.
	 * 
	 * @param v
	 *            vector with coordinates to be copied
	 * @return this vector modified in place
	 */
	@Override
	public GVector3d set(final GVector3d v) {
		this.x = v.x;
		this.y = v.y;
		this.z = v.z;

		return this;
	}

	/**
	 * Sets one particular coordinate to a given value.
	 * 
	 * @param i
	 *            index number of the coordinate to set in the range [0, 2] -
	 *            values of
	 *            <code>i<code> outside this range will be ignored, i.e.
	 *            the vector will not be modified
	 * @param val
	 *            new value of coordinate specified by index <code>i</code>
	 * @return this vector modified in place
	 */
	public GVector3d set(final int i, final double val) {
		if (i == 0) {
			this.x = val;
		} else if (i == 1) {
			this.y = val;
		} else if (i == 2) {
			this.z = val;
		}

		return this;
	}

	/**
	 * Identical to {@link #set(int, double)}
	 * 
	 * @see gaia.cu1.tools.numeric.algebra.GVector#setElementFromDouble(int,
	 *      double)
	 */
	@Override
	public GVector3d setElementFromDouble(final int index, final double value) {
		return this.set(index, value);
	}

	/**
	 * Sets the <code>X</code> coordinate of this vector.
	 * 
	 * @param val
	 *            new value of the <code>X</code> coordinate
	 * @return this vector modified in place
	 */
	public GVector3d setX(final double val) {
		this.x = val;

		return this;
	}

	/**
	 * Sets the <code>Y</code> coordinate of this vector.
	 * 
	 * @param val
	 *            new value of the <code>Y</code> coordinate
	 * @return this vector modified in place
	 */
	public GVector3d setY(final double val) {
		this.y = val;

		return this;
	}

	/**
	 * Sets the <code>Z</code> coordinate of this vector.
	 * 
	 * @param val
	 *            new value of the <code>Z</code> coordinate
	 * @return this vector modified in place
	 */
	public GVector3d setZ(final double val) {
		this.z = val;

		return this;
	}

	/**
	 * @return <code>X</code> coordinate value of this vector
	 */
	public double getX() {
		return this.x;
	}

	/**
	 * @return <code>Y</code> coordinate value of this vector
	 */
	public double getY() {
		return this.y;
	}

	/**
	 * @return <code>Z</code> coordinate value of this vector
	 */
	public double getZ() {
		return this.z;
	}

	/**
	 * Identical to {@link #getX()}
	 * 
	 * @return double
	 */
	public double x() {
		return this.x;
	}

	/**
	 * Identical to {@link #getY()}
	 * 
	 * @return double
	 */
	public double y() {
		return this.y;
	}

	/**
	 * Identical to {@link #getZ()}
	 * 
	 * @return double
	 */
	public double z() {
		return this.z;
	}

	/**
	 * Gets a single coordinate value of this vector.
	 * 
	 * @param i
	 *            index number of the coordinate to get in the range [0, 2]
	 * 
	 * @return <code>i</code>th element of the vector
	 * @exception ArrayIndexOutOfBoundsException
	 *                if <code>i</code> is outside the range [0, 2]
	 * @see gaia.cu1.tools.numeric.algebra.GVectord#get(int)
	 */
	@Override
	public double get(final int i) {
		if (i == 0) {
			return this.x;
		} else if (i == 1) {
			return this.y;
		} else if (i == 2) {
			return this.z;
		} else {
			throw new ArrayIndexOutOfBoundsException(ErrorMessageFormat.format(
					ErrorMessageKeys.Data.OUT_OF_RANGE_INDEX, i));
		}
	}

	/**
	 * Identical to {@link #get(int)}
	 * 
	 * @see gaia.cu1.tools.numeric.algebra.GVector#getElementAsDouble(int)
	 */
	@Override
	public double getElementAsDouble(final int index) {
		return this.get(index);
	}

	/**
	 * @return dimentionaliy of <code>GVector3d</code>, i.e. 3
	 * @see gaia.cu1.tools.numeric.algebra.GVector#dimensionality()
	 */
	@Override
	public int dimensionality() {
		return 3;
	}

	/**
	 * Compares this vector to another one.
	 * 
	 * @param v
	 *            vector to compare this one to
	 * @param absTol
	 *            absolute tolerance to within which all coordinates of the
	 *            vector must coincide to consider the vectors as equal
	 * @return <code>true</code> if all coordinates of both vectors agree to
	 *         within <code>absTol<code>
	 */
	@Override
	public boolean equals(final GVector3d v, final double absTol) {
		return (Math.abs(this.x - v.x) < absTol)
				&& (Math.abs(this.y - v.y) < absTol)
				&& (Math.abs(this.z - v.z) < absTol);
	}

	/**
	 * Checks if two vectors are equal with the machine precision.
	 * 
	 * @param v
	 *            Vector to compare with.
	 * @return A boolean answering to the question "Are these vectors equal?"
	 * @see gaia.cu1.tools.numeric.algebra.GVector#equals(Object)
	 */
	@Override
	public boolean equals(final Object v) {
		if (!(v instanceof GVector3d)) {
			return false;
		}

		return (Double.compare(this.x, ((GVector3d) v).x) == 0)
				&& (Double.compare(this.y, ((GVector3d) v).y) == 0)
				&& (Double.compare(this.z, ((GVector3d) v).z) == 0);
	}

	/**
	 * @return coordinates of this vector as a 3-element double array
	 * @see gaia.cu1.tools.numeric.algebra.GVectord#toArray()
	 */
	@Override
	public double[] toArray() {
		return new double[] { this.x, this.y, this.z };
	}

	/**
	 * @return Eucledian norm of the vector, i.e. the square root of the sum of
	 *         the squared vector's elements
	 * @see gaia.cu1.tools.numeric.algebra.GVector#norm()
	 */
	@Override
	public double norm() {
		return Math.sqrt(this.normSquared());
	}

	/**
	 * @return squared Eucledian norm of the vector, i.e. the sum of the squared
	 *         vector's elements
	 * @see gaia.cu1.tools.numeric.algebra.GVector#normSquared()
	 */
	@Override
	public double normSquared() {
		return this.dot(this);
	}

	/**
	 * Compute the vector norm plus the its uncertainty.
	 * 
	 * @param stdDev
	 *            standard deviation of vector
	 * @return vector norm and uncertainty as in a 2-element array
	 */
	public double[] uncertainNorm(GVector3d stdDev) {
		double[] res = new double[] { norm(), 0. };

		double n = res[0];
		if (n > 0. && !Double.isInfinite(n) && !Double.isNaN(n)) {
			double s = 0.;
			for (int i = 0; i < 3; ++i) {
				double r = get(i) * stdDev.get(i);
				s += r * r;
			}

			res[1] = Math.sqrt(s) / n;
		}

		return res;
	}

	/**
	 * Actively rotates this vector around the <code>X</code> axis.
	 * 
	 * @param alpha
	 *            angle of rotation [rad]
	 * @return vector modified in place
	 */
	public GVector3d rotateAroundX(final double alpha) {
		final DirectionCosineMatrix xRot = new DirectionCosineMatrix(
				GVector3d.Axis.X, -alpha);

		return xRot.times(this);
	}

	/**
	 * Actively rotates this vector around the <code>Y</code> axis.
	 * 
	 * @param alpha
	 *            angle of rotation [rad]
	 * @return vector modified in place
	 */
	public GVector3d rotateAroundY(final double alpha) {
		final DirectionCosineMatrix yRot = new DirectionCosineMatrix(
				GVector3d.Axis.Y, -alpha);

		return yRot.times(this);
	}

	/**
	 * Actively rotates this vector around the <code>Z</code> axis.
	 * 
	 * @param alpha
	 *            angle of rotation [rad]
	 * @return vector modified in place
	 */
	public GVector3d rotateAroundZ(final double alpha) {
		final DirectionCosineMatrix zRot = new DirectionCosineMatrix(
				GVector3d.Axis.Z, -alpha);

		return zRot.times(this);
	}

	/**
	 * Rotates this vector around a general axis.
	 * 
	 * @param axis
	 *            <code>GVector3d</code> defining the axis of rotation
	 * @param alpha
	 *            rotation angle [rad]
	 * @return vector modified in place
	 */
	public GVector3d rotateAroundAxis(final GVector3d axis, final double alpha) {
		final DirectionCosineMatrix rot = new DirectionCosineMatrix(axis,
				-alpha);

		return rot.times(this);
	}

	/**
	 * Calculates the unit vector to the barycenter of an input list of
	 * <code>GVector3d</code>s.
	 * 
	 * @param va
	 *            array of <code>GVector3d</code>s
	 * @return unit vector pointing to the barycenter of the input vector system
	 */
	public static GVector3d baryCenter(final GVector3d[] va) {
		final int nvec = va.length;

		double x = 0.;
		double y = 0.;
		double z = 0.;

		for (int i = 0; i < nvec; i++) {
			final GVector3d vn = va[i].clone().normalize();

			x += vn.x;
			y += vn.y;
			z += vn.z;
		}

		return (new GVector3d(x, y, z)).normalize();
	}

	/**
	 * Returns the latitude of this vector in the reference frame with the
	 * <code>Z</code> axis pointing toward the North pole and the <code>X</code>
	 * axis pointing towards zero longitude.
	 * 
	 * @return latitude [rad] of the vector in the range [-Pi/2, +Pi]
	 */
	public double getLatitude() {
		return this.toSphericalCoordinates().get(1);
	}

	/**
	 * Returns the longitude of this vector in the reference frame with the
	 * <code>Z</code> axis pointing towards the North pole and the
	 * <code>X</code> axis pointing towards zero longitude.
	 * 
	 * @return longitude [rad] of the vector in the range [0, 2Pi)
	 */
	public double getLongitude() {
		return this.toSphericalCoordinates().get(0);
	}

	/**
	 * Converts the current direction cosines to spherical coordinates.
	 * 
	 * @return Longitude [0, 2*Pi] and latitude [-Pi/2, Pi/2] corresponding to
	 *         the current direction cosine as a {@link GVector2d}
	 * @see gaia.cu1.tools.astro.coordinate.DirectionCosine#toSphericalCoordinates()
	 */
	@Override
	public GVector2d toSphericalCoordinates() {
		final double xy = Math.sqrt((this.x * this.x) + (this.y * this.y));

		if (xy <= 0.) {
			return new GVector2d(0., .5 * GaiaParam.Nature.PI_CONSTANT
					* Math.signum(this.z));
		}

		double alon = Math.atan2(this.y, this.x);

		// normalise to [0, 2*Pi]
		if (alon < 0.) {
			alon += (2. * GaiaParam.Nature.PI_CONSTANT);
		}

		return new GVector2d(alon, Math.atan2(this.z, xy));
	}

	/**
	 * Converts the input spherical coordinates into direction cosines and sets
	 * the <code>GVector3d</code>'s coordinates accordingly.
	 * 
	 * @param sphCoo
	 *            input spherical coordinates [rad]
	 * @see gaia.cu1.tools.astro.coordinate.DirectionCosine#fromSphericalCoordinates(GVector2d)
	 */
	@Override
	public void fromSphericalCoordinates(final GVector2d sphCoo) {
		this.fromSphericalCoordinates(sphCoo.x(), sphCoo.y());
	}

	/**
	 * Identical to {@link #fromSphericalCoordinates(GVector2d)} except that
	 * longitude and latitude are given as two numbers
	 * 
	 * @see gaia.cu1.tools.astro.coordinate.DirectionCosine#fromSphericalCoordinates(double,
	 *      double)
	 */
	@Override
	public void fromSphericalCoordinates(final double lon, final double lat) {
		final double cosb = Math.cos(lat);

		this.set(Math.cos(lon) * cosb, Math.sin(lon) * cosb, Math.sin(lat));
	}

	/**
	 * Calculates the angle between this vector and a second one.
	 * 
	 * @param v
	 *            other vector
	 * @return calculated angle [rad] between this one and <code>v</code> in the
	 *         range [0, Pi]
	 */
	public double angleTo(final GVector3d v) {
		return Math.atan2(GVector3d.cross(this, v).norm(), this.dot(v));
	}

	/**
	 * Calculates angular separation between this vector and another one passed
	 * as argument. The method is more accurate than {@link #angleTo(GVector3d)}
	 * in the case that the two vectors are almost identical.
	 * 
	 * @param v
	 *            other vector
	 * @return Computed angular separation [rad] in the range [-Pi, Pi]
	 */
	public double smallAngleTo(final GVector3d v) {
		return 2. * Math.atan2(GVector3d.sub(this, v).norm(),
				GVector3d.add(this, v).norm());
	}

	/**
	 * Computes the normal triad [p q r] at spherical coordinates
	 * <code>(alpha, delta)</code>
	 * 
	 * @param alpha
	 *            longitude [rad] (0<=alpha<2 Pi)
	 * @param delta
	 *            latitude [rad] (-Pi/2<=delta<=Pi/2)
	 * @return computed normal trid as three element array 0: unit vector in the
	 *         direction of increasing <code>alpha</code> 1: unit vector in the
	 *         direction of increasing <code>delta</code> 2: unit vector towards
	 *         the point <code>(alpha, delta)</code>
	 */
	static public GVector3d[] localTriad(final double alpha, final double delta) {
		final double ca = Math.cos(alpha);
		double sa = Math.sin(alpha);
		final double cd = Math.cos(delta);
		double sd = Math.sin(delta);

		return new GVector3d[] { new GVector3d(-sa, ca, 0.),
				new GVector3d(-sd * ca, -sd * sa, cd),
				new GVector3d(cd * ca, cd * sa, sd) };
	}

	/**
	 * @return sum of all three coordinates of this vector
	 * @see gaia.cu1.tools.numeric.algebra.GVector#sum()
	 */
	@Override
	public double sum() {
		return this.x + this.y + this.z;
	}

	/**
	 * Adds a scalar <code>r</code> to all coordinates of the vector, i.e.,
	 * <code>this = this + (r, r, r)</code>.
	 * 
	 * @param r
	 *            scalar to add to all coordinates
	 * @return vector modified in place
	 * @see gaia.cu1.tools.numeric.algebra.GVectord#add(double)
	 */
	@Override
	public GVector3d add(final double r) {
		this.x += r;
		this.y += r;
		this.z += r;

		return this;
	}

	/**
	 * Adds another vector <code>v</code> to this one, i.e.
	 * <code>this = this + v</code>.
	 * 
	 * @param v
	 *            vector to add
	 * @return vector modified in place
	 */
	@Override
	public GVector3d add(final GVector3d v) {
		this.x += v.x;
		this.y += v.y;
		this.z += v.z;

		return this;
	}

	/**
	 * Adds two vectors and return the result a new one.
	 * 
	 * @param v
	 *            first operand
	 * @param w
	 *            second operand
	 * @return sum of <code>v</code> and <code>w</code>
	 */
	static public GVector3d add(final GVector3d v, final GVector3d w) {
		final GVector3d res = new GVector3d(v);

		return res.add(w);
	}

	/**
	 * Subtracts a scalar <code>r</code> from all vector coordinates, i.e.,
	 * <code>this = this - (r, r, r)</code>
	 * 
	 * @param r
	 *            scalar to subtract from all coordinate values
	 * @return vector modified in place
	 * @see gaia.cu1.tools.numeric.algebra.GVectord#sub(double)
	 */
	@Override
	public GVector3d sub(final double r) {
		return this.add(-r);
	}

	/**
	 * Subtracts a vector <code>v</code> from this one, i.e.
	 * <code>this = this - v</code>
	 * 
	 * @param v
	 *            vector to subtract from this one
	 * @return vector modified in place
	 */
	@Override
	public GVector3d sub(final GVector3d v) {
		this.x -= v.x;
		this.y -= v.y;
		this.z -= v.z;

		return this;
	}

	/**
	 * Subtracts two vectors <code>v</code> and <code>w</code> from one another.
	 * 
	 * @param v
	 *            first operand of the subtraction
	 * @param w
	 *            second operand of the subtraction
	 * @return difference of <code>v</code> and <code>w</code> and new
	 *         <code>GVector3d</code>
	 */
	static public GVector3d sub(final GVector3d v, final GVector3d w) {
		final GVector3d res = new GVector3d(v);

		return res.sub(w);
	}

	/**
	 * Sets this vector to the outer product of itself and a second vector
	 * <code>v</code>, i.e. <code>this = this x v</code>
	 * 
	 * @param vec
	 *            vector with which to build the outer product
	 * @return vector modified in place
	 */
	@Override
	public GVector3d cross(final GVector3d vec) {
		final double c1 = (this.y * vec.z) - (this.z * vec.y);
		final double c2 = (this.z * vec.x) - (this.x * vec.z);
		final double c3 = (this.x * vec.y) - (this.y * vec.x);

		this.x = c1;
		this.y = c2;
		this.z = c3;

		return this;
	}

	/**
	 * Calculates the outer product of two given vectors <code>v</code> and
	 * <code>w</code> and returns the result as a new <code>GVector3d</code>.
	 * 
	 * @param v
	 *            left operand
	 * @param w
	 *            right operand
	 * @return outer product of <code>v</code> and <code>w</code>
	 */
	static public GVector3d cross(final GVector3d v, final GVector3d w) {
		final GVector3d res = new GVector3d(v);

		return res.cross(w);
	}

	/**
	 * Calculates the dot product of this and another given vector.
	 * 
	 * @param v
	 *            vector to build the dot product with
	 * @return dot product of this vector and <code>v</code>
	 */
	@Override
	public double dot(final GVector3d v) {
		return (this.x * v.x) + (this.y * v.y) + (this.z * v.z);
	}

	/**
	 * Computes the dot product of two given vector <code>v</code> and
	 * <code>w</code>.
	 * 
	 * @param v
	 *            left operand
	 * @param w
	 *            right operand
	 * @return dot product of <code>v</code> and <code>w</code>
	 */
	static double dot(final GVector3d v, final GVector3d w) {
		return v.dot(w);
	}

	/**
	 * Negates all coordinates of this vetor.
	 * 
	 * @return vector modified in place
	 * @see gaia.cu1.tools.numeric.algebra.GVector#negate()
	 */
	@Override
	public GVector3d negate() {
		this.x = -this.x;
		this.y = -this.y;
		this.z = -this.z;

		return this;
	}

	/**
	 * Negates a given vector <code>v</code> and returns the result as a new
	 * <code>GVector3d</code>.
	 * 
	 * @param v
	 *            vector to negate
	 * @return new vector set to the negation of <code>v</code>
	 */
	static public GVector3d negate(final GVector3d v) {
		final GVector3d vec = new GVector3d(v);

		return vec.negate();
	}

	/**
	 * Normalizes the vector to unity. Nothing is done if the vectors magnitude
	 * is zero.
	 * 
	 * @return vector modified in place
	 * @see gaia.cu1.tools.numeric.algebra.GVector#normalize()
	 */
	@Override
	public GVector3d normalize() {
		final double mag = this.norm();

		if (mag > 0.) {
			this.scale(1. / mag);
		}

		return this;
	}

	/**
	 * Scales the vector by a scalar <code>s</code>, i.e.
	 * <code>this = s*this</code>
	 * 
	 * @param s
	 *            scalar to scale the vector with
	 * @return vector modified in place
	 * @see gaia.cu1.tools.numeric.algebra.GVectord#scale(double)
	 */
	@Override
	public GVector3d scale(final double s) {
		this.x *= s;
		this.y *= s;
		this.z *= s;

		return this;
	}

	/**
	 * Constructs new vector from scaling a given one.
	 * 
	 * @param s
	 *            scalar scaling factor
	 * @param v
	 *            vector to scale
	 * @return product of <code>s</code> and <code>v</code> as a new
	 *         <code>GVector3d</code>
	 */
	public static GVector3d scale(final double s, final GVector3d v) {
		final GVector3d vec = new GVector3d(v);

		return vec.scale(s);
	}

	/**
	 * Scales a given vector with a scalar and add the result to this one, i.e.
	 * <code>this = this + s*v</code>.
	 * 
	 * @param s
	 *            scalar scaling factor
	 * @param v
	 *            vector to scale
	 * @return vector modified in place
	 */
	@Override
	public GVector3d scaleAdd(final double s, final GVector3d v) {
		return this.add(GVector3d.scale(s, v));
	}

	/**
	 * Constructs new vector as sum of a given vector <code>v1</code> and a
	 * scaled vector <code>s*v2</code>.
	 * 
	 * @param v1
	 *            first vector
	 * @param s
	 *            scalar scaling factor
	 * @param v2
	 *            second vector
	 * @return new vector set to <code>v1+s*v2</code>
	 */
	public static GVector3d scaleAdd(final GVector3d v1, final double s,
			final GVector3d v2) {
		final GVector3d res = new GVector3d(v2);

		return res.scale(s).add(v1);
	}

	/**
	 * Sets each coordinate of this vector to its inverse (if not 0)
	 * 
	 * @return vector modified in place
	 * @see gaia.cu1.tools.numeric.algebra.GVectord#udiv()
	 */
	@Override
	public GVector3d udiv() {
		if (this.x != 0.) {
			this.x = 1. / this.x;
		}

		if (this.y != 0.) {
			this.y = 1. / this.y;
		}

		if (this.z != 0.) {
			this.z = 1. / this.z;
		}

		return this;
	}

	/**
	 * Returns a string representing the vector elements according to a
	 * specified format.
	 * 
	 * @param df
	 *            <code>DecimalFormat</code> for the output string.
	 * @return string representation of the vector
	 * @see gaia.cu1.tools.numeric.algebra.GVector#toString(DecimalFormat)
	 */
	@Override
	public String toString(final DecimalFormat df) {
		return "(" + df.format(this.x) + ", " + df.format(this.y) + ", "
				+ df.format(this.z) + ")";
	}

	/**
	 * Returns a string representing the vector elements according to a
	 * specified format expressed as a <code>String</code>
	 * 
	 * @param df
	 *            <a href=
	 *            "http://java.sun.com/j2se/1.5.0/docs/api/java/util/Formatter.html#syntax"
	 *            >Format string syntax </a>
	 * @return string representation of the vector
	 */
	public String toString(final String df) {
		return "(" + String.format(df, this.x).trim() + ", "
				+ String.format(df, this.y).trim() + ", "
				+ String.format(df, this.z).trim() + ")";
	}

	/**
	 * @return default string representation of the vector <em>(x, y, z)</em>
	 * @see gaia.cu1.tools.numeric.algebra.GVector#toString()
	 */
	@Override
	public String toString() {
		return "(" + this.x + ", " + this.y + ", " + this.z + ")";
	}

	/**
	 * 
	 * @see gaia.cu1.tools.numeric.sort.Sortable#compareWithCache(int, boolean)
	 */
	@Override
	public int compareWithCache(final int i, final boolean ascending) {
		return this.compare(this.cache, i, ascending);
	}

	/**
	 * 
	 * @see gaia.cu1.tools.numeric.sort.Rearrangeable#cacheElement(int)
	 */
	@Override
	public void cacheElement(final int i) {
		switch (i) {
		case 0:
			this.cache = this.x;

			break;

		case 1:
			this.cache = this.y;

			break;

		case 2:
			this.cache = this.z;

		default:
			throw new IllegalArgumentException(
					"GVector3d can only accept indices in range [0,2]");
		}
	}

	/**
	 * 
	 * @see gaia.cu1.tools.numeric.sort.Rearrangeable#copyElementTo(int, int)
	 */
	@Override
	public void copyElementTo(final int targetElement, final int sourceElement) {
		if ((sourceElement < 0) || (sourceElement > 2)) {
			throw new IllegalArgumentException(
					"GVector3d can only accept indices in range [0,2]");
		}

		if (targetElement != sourceElement) {
			switch (targetElement) {
			case 0:
				this.x = (sourceElement == 1) ? this.y : this.z;

				break;

			case 1:
				this.y = (sourceElement == 0) ? this.x : this.z;

				break;

			case 2:
				this.z = (sourceElement == 0) ? this.x : this.y;

				break;

			default:
				throw new IllegalArgumentException(
						"GVector3d can only accept indices in range [0,2]");
			}
		}
	}

	/**
	 * 
	 * @see gaia.cu1.tools.numeric.sort.Rearrangeable#setElementFromCache(int)
	 */
	@Override
	public void setElementFromCache(final int i) throws IllegalStateException {
		switch (i) {
		case 0:
			this.x = this.cache;

			break;

		case 1:
			this.y = this.cache;

			break;

		case 2:
			this.z = this.cache;

			break;

		default:
			throw new IllegalArgumentException(
					"GVector3d can only accept indices in range [0,2]");
		}
	}

	/**
	 * Sets all components of the vector to their absolute values
	 * 
	 * @return modified vector
	 * @see gaia.cu1.tools.numeric.algebra.GVector#abs()
	 */
	@Override
	public GVector3d abs() {
		this.x = (this.x >= 0) ? this.x : (-this.x);
		this.y = (this.y >= 0) ? this.y : (-this.y);
		this.z = (this.z >= 0) ? this.z : (-this.z);

		return this;
	}

	/**
	 * Rotates this vector by a quaternion, using "vector rotation" according to
	 * (60) in LL-072. Both the original and the returned vectors are expressed
	 * in the same reference frame as the quaternion.
	 * 
	 * @param q
	 *            Quaternion defining the vector rotation.
	 * @return the rotated vector.
	 */
	public GVector3d rotateVectorByQuaternion(final Quaternion q) {
		Quaternion oldVecQ = new Quaternion(this.x, this.y, this.z, 0.0);
		Quaternion newVecQ = q.clone().mult(oldVecQ).multInverse(q);
		this.x = newVecQ.x;
		this.y = newVecQ.y;
		this.z = newVecQ.z;

		return this;
	}

	/**
	 * Rotates the reference frame for this vector by a quaternion, using
	 * "frame rotation" according to (61) in LL-072. If the vector is originally
	 * expressed in the A frame, and the quaternion defines the rotation of A
	 * into B frame, then the result is the same vector expressed in the B
	 * frame.
	 * 
	 * @param q
	 *            Quaternion defining the frame rotation.
	 * @return the vector in the rotated frame.
	 */
	public GVector3d rotateFrameByQuaternion(final Quaternion q) {
		Quaternion oldVecQ = new Quaternion(this.x, this.y, this.z, 0.0);
		Quaternion newVecQ = q.clone().inverse().mult(oldVecQ).mult(q);
		this.x = newVecQ.x;
		this.y = newVecQ.y;
		this.z = newVecQ.z;

		return this;
	}

	// the three principal axes of the reference frame that the vector is
	// defined in
	/**
     */
	public enum Axis {
		X, Y, Z;
	}

	/**
	 * Sets the seed for the internal random number generator
	 * 
	 * @param seed
	 */
	public static void setSeed(long seed) {
		rng.setSeed(seed);
	}
}
