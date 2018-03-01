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
package uk.ac.starlink.dpac.epoch;

// package gaia.cu1.tools.numeric.algebra;

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
public class GVector3d {

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
	 * Adds a scalar <code>r</code> to all coordinates of the vector, i.e.,
	 * <code>this = this + (r, r, r)</code>.
	 * 
	 * @param r
	 *            scalar to add to all coordinates
	 * @return vector modified in place
	 */
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
	 * Sets this vector to the outer product of itself and a second vector
	 * <code>v</code>, i.e. <code>this = this x v</code>
	 * 
	 * @param vec
	 *            vector with which to build the outer product
	 * @return vector modified in place
	 */
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
	 * Scales the vector by a scalar <code>s</code>, i.e.
	 * <code>this = s*this</code>
	 * 
	 * @param s
	 *            scalar to scale the vector with
	 * @return vector modified in place
	 */
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
}
