/*
	This file is part of FreeJ2ME.

	FreeJ2ME is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	FreeJ2ME is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with FreeJ2ME.  If not, see http://www.gnu.org/licenses/
*/
package javax.microedition.m3g;

import java.lang.Math;
import java.util.Arrays;

public class Transform
{

	// This is a 4x4 matrix represented as a 16 item long array.
	// The items are in row major order:
	//   [  0,  1,  2,  3 ]
	//   [  4,  5,  6,  7 ]   Addressing in 2D vs in 1D:
	//   [  8,  9, 10, 11 ]     mat_2D[row][col] == mat_1D[4*row + col]
	//   [ 12, 13, 14, 15 ]
	private float[] matrix;

	/* ------------------------- public methods ------------------------- */
	public Transform()
	{
		this.setIdentity();
	}

	public Transform(Transform transform)
	{
		if (transform == null) throw new java.lang.NullPointerException();
		this.matrix = transform.matrix.clone();
	}

	public void get(float[] matrix)
	{
		if (matrix == null) throw new java.lang.NullPointerException();
		if (matrix.length < 16) throw new java.lang.IllegalArgumentException();
		for (int i = 0; i < 16; i++)
			matrix[i] = this.matrix[i];
	}

	public void invert()
	{
		// TODO Check if the current matrix is invertible in a fast way,
		//      and only use `slowInvert` if there is no other way.
		this.slowInvert();
	}

	public void postMultiply(Transform transform)
	{
		if (transform == null) throw new java.lang.NullPointerException();;
		this.matrix = Transform.multiply(
			this.matrix,
			transform.matrix
		);
	}

	public void postRotate(float angle, float ax, float ay, float az)
	{
		this.postMultiplyTry(Transform.rotate(angle, ax, ay, az));
	}

	public void postRotateQuat(float qx, float qy, float qz, float qw)
	{
		this.postMultiplyTry(Transform.rotateQuat(qx, qy, qz, qw));
	}

	public void postScale(float sx, float sy, float sz)
	{
		this.postMultiplyTry(Transform.scale(sx, sy, sz));
	}

	public void postTranslate(float tx, float ty, float tz)
	{
		this.postMultiplyTry(Transform.translate(tx, ty, tz));
	}

	public void set(float[] matrix)
	{
		if (matrix == null) throw new java.lang.NullPointerException();
		if (matrix.length < 16) throw new java.lang.IllegalArgumentException();
		for (int i = 0; i < 16; i++)
			this.matrix[i] = matrix[i];
	}

	public void set(Transform transform)
	{
		if (transform == null) throw new java.lang.NullPointerException();
		this.matrix = transform.matrix.clone();
	}

	public void setIdentity()
	{
		this.matrix = new float[] {
			1, 0, 0, 0,
			0, 1, 0, 0,
			0, 0, 1, 0,
			0, 0, 0, 1
		};
	}

	public void transform(float[] vectors)
	{
		if (vectors == null) throw new java.lang.NullPointerException();
		if (vectors.length % 4 != 0)
			throw new java.lang.IllegalArgumentException();

		float[] m = this.matrix;
		for (int offset = 0; offset < vectors.length; offset += 4)
		{
			float[] result = new float[4];
			for (int row = 0; row < 4; row++)
				result[row] =
					+ m[4*row + 0] * vectors[offset + 0]
					+ m[4*row + 1] * vectors[offset + 1]
					+ m[4*row + 2] * vectors[offset + 2]
					+ m[4*row + 3] * vectors[offset + 3];

			vectors[offset + 0] = result[0];
			vectors[offset + 1] = result[1];
			vectors[offset + 2] = result[2];
			vectors[offset + 3] = result[3];
		}
	}

	public void transform(VertexArray in, float[] out, boolean W)
	{
		if (in == null) throw new java.lang.NullPointerException();
		if (out == null) throw new java.lang.NullPointerException();

		int vertexCount = in.getVertexCount();
		int vertexDims = in.getComponentCount();
		boolean pass_z = vertexDims == 3;
		float w = W ? 1f : 0f;

		if (vertexDims == 4) throw new java.lang.IllegalArgumentException();
		if (out.length < 4 * vertexCount)
			throw new java.lang.IllegalArgumentException();

		short[] vertices = new short[vertexCount * vertexDims];
		in.get(0, vertexCount, vertices);

		// Fill the `out` array with raw data
		for (int vertexIndex = 0; vertexIndex < vertexCount; vertexIndex++)
		{
			int  in_offset = vertexIndex * vertexDims;
			int out_offset = vertexIndex * 4;

			float x =          vertices[in_offset + 0]     ;
			float y =          vertices[in_offset + 1]     ;
			float z = pass_z ? vertices[in_offset + 2] : 0f;

			out[out_offset + 0] = x;
			out[out_offset + 1] = y;
			out[out_offset + 2] = z;
			out[out_offset + 3] = w;
		}

		// Do the transformation on the raw data that is currently in `out`
		this.transform(out);
	}

	public void transpose()
	{
		float[] old = this.matrix.clone();
		for (int i = 0; i < 16; i++)
			this.matrix[4*(i/4) + (i%4)] = old[4*(i%4) + (i/4)];
	}

	/* ------------------------- package methods ------------------------- */

	// The pre* methods exist to facilitate chaining transformations.
	// They are mainly used in rendering.

	// package-private
	void preMultiply(Transform transform)
	{
		if (transform == null) throw new java.lang.NullPointerException();;
		this.matrix = Transform.multiply(
			transform.matrix,
			this.matrix
		);
	}

	// The two *MultiplyTry methods will silently ignore a null transform.
	// This is used to concisely ignore nulls, mostly in rendering.

	// package-private
	void preMultiplyTry(Transform transform)
	{
		if (transform != null) this.preMultiply(transform);
	}

	// package-private
	void postMultiplyTry(Transform transform)
	{
		if (transform != null) this.postMultiply(transform);
	}

	// package-private
	void preRotate(float angle, float ax, float ay, float az)
	{
		this.preMultiplyTry(Transform.rotate(angle, ax, ay, az));
	}

	// package-private
	void preRotateQuat(float qx, float qy, float qz, float qw)
	{
		this.preMultiplyTry(Transform.rotateQuat(qx, qy, qz, qw));
	}

	// package-private
	void preScale(float sx, float sy, float sz)
	{
		this.preMultiplyTry(Transform.scale(sx, sy, sz));
	}

	// package-private
	void preTranslate(float tx, float ty, float tz)
	{
		this.preMultiplyTry(Transform.translate(tx, ty, tz));
	}

	// package-private
	static Transform rotate(float angle, float ax, float ay, float az)
	{
		if (angle == 0) return null;
		if (ax == 0 && ay == 0 && az == 0)
			throw new java.lang.IllegalArgumentException();

		// Compute sine and cosine of the angle
		double rad = Math.toRadians(angle);
		double s = Math.sin(rad);
		double c = Math.cos(rad);
		double d = 1 - c;

		// Normalize the axis
		double l = Math.sqrt(Math.pow(ax,2) + Math.pow(ay,2) + Math.pow(az,2));
		double x = ax / l;
		double y = ay / l;
		double z = az / l;

		double[] rotationMatrix = new double[] {
			x*x*d +  c ,  y*x*d - z*s,  z*x*d + y*s,  0,
			x*y*d + z*s,  y*y*d +  c ,  z*y*d - x*s,  0,
			x*z*d - y*s,  y*z*d + x*s,  z*z*d +  c ,  0,
			     0     ,       0     ,       0     ,  1
		};

		return new Transform(rotationMatrix);
	}

	// package-private
	static Transform rotateQuat(float qx, float qy, float qz, float qw)
	{
		if (qx == 0 && qy == 0 && qz == 0 && qw == 0)
			throw new java.lang.IllegalArgumentException();

		// Normalize the quaternion
		double l = Math.sqrt(
			Math.pow(qx,2) + Math.pow(qy,2) + Math.pow(qz,2) + Math.pow(qw,2)
		);
		double x = qx / l;
		double y = qy / l;
		double z = qz / l;
		double w = qw / l;

		double[] rotationMatrix = new double[] {
			1-2*y*y-2*z*z,    2*x*y-2*z*w,    2*x*z+2*y*w,  0,
			  2*x*y+2*z*w,  1-2*x*x-2*z*z,    2*y*z-2*x*w,  0,
			  2*x*z-2*y*w,    2*y*z+2*x*w,  1-2*x*x-2*y*y,  0,
			      0      ,        0      ,        0      ,  1
		};

		return new Transform(rotationMatrix);
	}

	// package-private
	static Transform scale(float sx, float sy, float sz)
	{
		if (sx == 1 && sy == 1 && sz == 1) return null;

		float[] scaleMatrix = new float[] {
			sx,  0,  0, 0,
			 0, sy,  0, 0,
			 0,  0, sz, 0,
			 0,  0,  0, 1
		};

		return new Transform(scaleMatrix);
	}

	// package-private
	static Transform translate(float tx, float ty, float tz)
	{
		if (tx == 0 && ty == 0 && tz == 0) return null;

		float[] translationMatrix = new float[] {
			1, 0, 0, tx,
			0, 1, 0, ty,
			0, 0, 1, tz,
			0, 0, 0,  1
		};

		return new Transform(translationMatrix);
	}

	// package-private
	void debug()
	{
		System.out.println();
		for (int i = 0; i < 16; i += 4)
		{
			System.out.println(String.format(
				"dbg-mat %5.2f %5.2f %5.2f %5.2f",
				this.matrix[i + 0],
				this.matrix[i + 1],
				this.matrix[i + 2],
				this.matrix[i + 3]
			));
		}
	}

	// package-private
	void debug(VertexArray in)
	{
		float[] buf = new float[4 * in.getVertexCount()];
		this.transform(in, buf, true);
		this.debug();
		for (int i = 0; i < buf.length; i += 4)
		{
			System.out.println(String.format(
				"dbg-out %5.2f %5.2f %5.2f %5.2f",
				buf[i + 0],
				buf[i + 1],
				buf[i + 2],
				buf[i + 3]
			));
		}
		System.out.println();
	}

	/* ------------------------- private methods ------------------------- */

	private Transform(float[] matrix)
	{
		this.matrix = matrix;
	}

	private Transform(double[] matrix)
	{
		this.matrix = new float[16];
		for (int i = 0; i < 16; i++)
			this.matrix[i] = (float) matrix[i];
	}

	private static float[] multiply(float[] left, float[] right)
	{
		float[] result = new float[16];

		for (int row = 0; row < 4; row++)
			for (int col = 0; col < 4; col++)
				result[4*row + col] =
					left[4*row + 0] * right[4*0 + col] +
					left[4*row + 1] * right[4*1 + col] +
					left[4*row + 2] * right[4*2 + col] +
					left[4*row + 3] * right[4*3 + col];

		return result;
	}

	private void slowInvert()
	{
		float[] m = this.matrix;

		float det =
			+ m[4*0+3] * m[4*1+2] * m[4*2+1] * m[4*3+0]
			- m[4*0+2] * m[4*1+3] * m[4*2+1] * m[4*3+0]
			- m[4*0+3] * m[4*1+1] * m[4*2+2] * m[4*3+0]
			+ m[4*0+1] * m[4*1+3] * m[4*2+2] * m[4*3+0]
			+ m[4*0+2] * m[4*1+1] * m[4*2+3] * m[4*3+0]
			- m[4*0+1] * m[4*1+2] * m[4*2+3] * m[4*3+0]
			- m[4*0+3] * m[4*1+2] * m[4*2+0] * m[4*3+1]
			+ m[4*0+2] * m[4*1+3] * m[4*2+0] * m[4*3+1]
			+ m[4*0+3] * m[4*1+0] * m[4*2+2] * m[4*3+1]
			- m[4*0+0] * m[4*1+3] * m[4*2+2] * m[4*3+1]
			- m[4*0+2] * m[4*1+0] * m[4*2+3] * m[4*3+1]
			+ m[4*0+0] * m[4*1+2] * m[4*2+3] * m[4*3+1]
			+ m[4*0+3] * m[4*1+1] * m[4*2+0] * m[4*3+2]
			- m[4*0+1] * m[4*1+3] * m[4*2+0] * m[4*3+2]
			- m[4*0+3] * m[4*1+0] * m[4*2+1] * m[4*3+2]
			+ m[4*0+0] * m[4*1+3] * m[4*2+1] * m[4*3+2]
			+ m[4*0+1] * m[4*1+0] * m[4*2+3] * m[4*3+2]
			- m[4*0+0] * m[4*1+1] * m[4*2+3] * m[4*3+2]
			- m[4*0+2] * m[4*1+1] * m[4*2+0] * m[4*3+3]
			+ m[4*0+1] * m[4*1+2] * m[4*2+0] * m[4*3+3]
			+ m[4*0+2] * m[4*1+0] * m[4*2+1] * m[4*3+3]
			- m[4*0+0] * m[4*1+2] * m[4*2+1] * m[4*3+3]
			- m[4*0+1] * m[4*1+0] * m[4*2+2] * m[4*3+3]
			+ m[4*0+0] * m[4*1+1] * m[4*2+2] * m[4*3+3];

		// Check if the matrix is invertible
		if (det == 0) throw new java.lang.ArithmeticException();

		float[] inverted = new float[] {
			( m[4*1+2]*m[4*2+3]*m[4*3+1] - m[4*1+3]*m[4*2+2]*m[4*3+1]
			+ m[4*1+3]*m[4*2+1]*m[4*3+2] - m[4*1+1]*m[4*2+3]*m[4*3+2]
			- m[4*1+2]*m[4*2+1]*m[4*3+3] + m[4*1+1]*m[4*2+2]*m[4*3+3]) / det,
			( m[4*0+3]*m[4*2+2]*m[4*3+1] - m[4*0+2]*m[4*2+3]*m[4*3+1]
			- m[4*0+3]*m[4*2+1]*m[4*3+2] + m[4*0+1]*m[4*2+3]*m[4*3+2]
			+ m[4*0+2]*m[4*2+1]*m[4*3+3] - m[4*0+1]*m[4*2+2]*m[4*3+3]) / det,
			( m[4*0+2]*m[4*1+3]*m[4*3+1] - m[4*0+3]*m[4*1+2]*m[4*3+1]
			+ m[4*0+3]*m[4*1+1]*m[4*3+2] - m[4*0+1]*m[4*1+3]*m[4*3+2]
			- m[4*0+2]*m[4*1+1]*m[4*3+3] + m[4*0+1]*m[4*1+2]*m[4*3+3]) / det,
			( m[4*0+3]*m[4*1+2]*m[4*2+1] - m[4*0+2]*m[4*1+3]*m[4*2+1]
			- m[4*0+3]*m[4*1+1]*m[4*2+2] + m[4*0+1]*m[4*1+3]*m[4*2+2]
			+ m[4*0+2]*m[4*1+1]*m[4*2+3] - m[4*0+1]*m[4*1+2]*m[4*2+3]) / det,
			( m[4*1+3]*m[4*2+2]*m[4*3+0] - m[4*1+2]*m[4*2+3]*m[4*3+0]
			- m[4*1+3]*m[4*2+0]*m[4*3+2] + m[4*1+0]*m[4*2+3]*m[4*3+2]
			+ m[4*1+2]*m[4*2+0]*m[4*3+3] - m[4*1+0]*m[4*2+2]*m[4*3+3]) / det,
			( m[4*0+2]*m[4*2+3]*m[4*3+0] - m[4*0+3]*m[4*2+2]*m[4*3+0]
			+ m[4*0+3]*m[4*2+0]*m[4*3+2] - m[4*0+0]*m[4*2+3]*m[4*3+2]
			- m[4*0+2]*m[4*2+0]*m[4*3+3] + m[4*0+0]*m[4*2+2]*m[4*3+3]) / det,
			( m[4*0+3]*m[4*1+2]*m[4*3+0] - m[4*0+2]*m[4*1+3]*m[4*3+0]
			- m[4*0+3]*m[4*1+0]*m[4*3+2] + m[4*0+0]*m[4*1+3]*m[4*3+2]
			+ m[4*0+2]*m[4*1+0]*m[4*3+3] - m[4*0+0]*m[4*1+2]*m[4*3+3]) / det,
			( m[4*0+2]*m[4*1+3]*m[4*2+0] - m[4*0+3]*m[4*1+2]*m[4*2+0]
			+ m[4*0+3]*m[4*1+0]*m[4*2+2] - m[4*0+0]*m[4*1+3]*m[4*2+2]
			- m[4*0+2]*m[4*1+0]*m[4*2+3] + m[4*0+0]*m[4*1+2]*m[4*2+3]) / det,
			( m[4*1+1]*m[4*2+3]*m[4*3+0] - m[4*1+3]*m[4*2+1]*m[4*3+0]
			+ m[4*1+3]*m[4*2+0]*m[4*3+1] - m[4*1+0]*m[4*2+3]*m[4*3+1]
			- m[4*1+1]*m[4*2+0]*m[4*3+3] + m[4*1+0]*m[4*2+1]*m[4*3+3]) / det,
			( m[4*0+3]*m[4*2+1]*m[4*3+0] - m[4*0+1]*m[4*2+3]*m[4*3+0]
			- m[4*0+3]*m[4*2+0]*m[4*3+1] + m[4*0+0]*m[4*2+3]*m[4*3+1]
			+ m[4*0+1]*m[4*2+0]*m[4*3+3] - m[4*0+0]*m[4*2+1]*m[4*3+3]) / det,
			( m[4*0+1]*m[4*1+3]*m[4*3+0] - m[4*0+3]*m[4*1+1]*m[4*3+0]
			+ m[4*0+3]*m[4*1+0]*m[4*3+1] - m[4*0+0]*m[4*1+3]*m[4*3+1]
			- m[4*0+1]*m[4*1+0]*m[4*3+3] + m[4*0+0]*m[4*1+1]*m[4*3+3]) / det,
			( m[4*0+3]*m[4*1+1]*m[4*2+0] - m[4*0+1]*m[4*1+3]*m[4*2+0]
			- m[4*0+3]*m[4*1+0]*m[4*2+1] + m[4*0+0]*m[4*1+3]*m[4*2+1]
			+ m[4*0+1]*m[4*1+0]*m[4*2+3] - m[4*0+0]*m[4*1+1]*m[4*2+3]) / det,
			( m[4*1+2]*m[4*2+1]*m[4*3+0] - m[4*1+1]*m[4*2+2]*m[4*3+0]
			- m[4*1+2]*m[4*2+0]*m[4*3+1] + m[4*1+0]*m[4*2+2]*m[4*3+1]
			+ m[4*1+1]*m[4*2+0]*m[4*3+2] - m[4*1+0]*m[4*2+1]*m[4*3+2]) / det,
			( m[4*0+1]*m[4*2+2]*m[4*3+0] - m[4*0+2]*m[4*2+1]*m[4*3+0]
			+ m[4*0+2]*m[4*2+0]*m[4*3+1] - m[4*0+0]*m[4*2+2]*m[4*3+1]
			- m[4*0+1]*m[4*2+0]*m[4*3+2] + m[4*0+0]*m[4*2+1]*m[4*3+2]) / det,
			( m[4*0+2]*m[4*1+1]*m[4*3+0] - m[4*0+1]*m[4*1+2]*m[4*3+0]
			- m[4*0+2]*m[4*1+0]*m[4*3+1] + m[4*0+0]*m[4*1+2]*m[4*3+1]
			+ m[4*0+1]*m[4*1+0]*m[4*3+2] - m[4*0+0]*m[4*1+1]*m[4*3+2]) / det,
			( m[4*0+1]*m[4*1+2]*m[4*2+0] - m[4*0+2]*m[4*1+1]*m[4*2+0]
			+ m[4*0+2]*m[4*1+0]*m[4*2+1] - m[4*0+0]*m[4*1+2]*m[4*2+1]
			- m[4*0+1]*m[4*1+0]*m[4*2+2] + m[4*0+0]*m[4*1+1]*m[4*2+2]) / det
		};

		this.matrix = inverted;
	}

}
