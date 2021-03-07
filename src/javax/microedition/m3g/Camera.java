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

public class Camera extends Node
{

	public static final int GENERIC = 48;
	public static final int PARALLEL = 49;
	public static final int PERSPECTIVE = 50;

	private int projMode;
	private float[] projMatrix; // Same kind of matrix as in Transform.
	private float[] params;     // params: { fovy, aspectRatio, near, far }

	public Camera()
	{
		this.projMode = GENERIC;
		this.projMatrix = new float[] {
			1, 0, 0, 0,
			0, 1, 0, 0,
			0, 0, 1, 0,
			0, 0, 0, 1
		};
		this.params = new float[4];
	}


	public int getProjection(float[] params)
	{
		if (params == null) return this.projMode;
		if (params.length < 4) throw new IllegalArgumentException();

		if (this.projMode != GENERIC)
			for (int i = 0; i < 4; i++)
				params[i] = this.params[i];

		return this.projMode;
	}

	public int getProjection(Transform transform)
	{
		if (transform == null) return this.projMode;

		// Since the projection matrix is only computed in this stage,
		// checking the correctness of the projection parameters is done here.
		//                                        near == far
		if (this.projMode != GENERIC && this.params[2] == this.params[3])
			throw new java.lang.ArithmeticException();

		// The computation of the projection matrix is only done if requested.
		// To prevent repeating the same computation, the matrix is cached.
		// This cache is re-set to null by `setParallel` and `setPerspective`.
		if (this.projMatrix == null) this.computeMatrix();

		// The method name says `GET`, but `transform.SET` is called.
		//
		// Here we *get* the projection from `this.projMatrix`
		//     and *set* the inner matrix of `transform` to that
		//
		//  .------------------.
		//  v                  |
		transform.set(this.projMatrix);
		return this.projMode;
	}

	public void setGeneric(Transform transform)
	{
		if (transform == null) throw new java.lang.NullPointerException();

		// The method name says `SET`, but `transform.GET` is called.
		//
		// Here we *get* the inner matrix of `transform`
		//     and *set* `this.projMatrix` to that
		//
		//  .------------------.
		//  |                  v
		transform.get(this.projMatrix);
		this.projMode = GENERIC;
	}

	public void setParallel(
		float fovy,
		float aspectRatio,
		float near,
		float far
	) {
		if (fovy <= 0 || aspectRatio <= 0)
			throw new java.lang.IllegalArgumentException();

		// The projection matrix is computed
		// only when `getProjection(Transform)` is called.
		this.projMatrix = null;
		this.projMode = PARALLEL;
		this.params = new float[] {
			fovy, aspectRatio, near, far
		};
	}

	public void setPerspective(
		float fovy,
		float aspectRatio,
		float near,
		float far
	) {
		if (fovy <= 0 || 180 <= fovy ||
			aspectRatio <= 0 || near <= 0 || far <= 0)
			throw new java.lang.IllegalArgumentException();

		// The projection matrix is computed
		// only when `getProjection(Transform)` is called.
		this.projMatrix = null;
		this.projMode = PERSPECTIVE;
		this.params = new float[] {
			fovy, aspectRatio, near, far
		};
	}

	private void computeMatrix()
	{
		float fovy = this.params[0];
		float aspectRatio = this.params[1];
		float near = this.params[2];
		float far = this.params[3];

		float h, w, d, b;

		switch (this.projMode)
		{
			case PARALLEL:

				h = fovy;
				w = aspectRatio * h;
				d = Math.abs(far - near);
				b = near + far;

				this.projMatrix = new float[] {
					2/w,  0 ,   0 ,   0 ,
					 0 , 2/h,   0 ,   0 ,
					 0 ,  0 , -2/d, -b/d,
					 0 ,  0 ,   0 ,   1
				};

				break;
			case PERSPECTIVE:

				h = (float) Math.tan(Math.toRadians(fovy)/2f);
				w = aspectRatio * h;
				d = Math.abs(far - near);
				b = near + far;

				this.projMatrix = new float[] {
					1/w,  0 ,   0 ,       0      ,
					 0 , 1/h,   0 ,       0      ,
					 0 ,  0 , -b/d, -2*near*far/d,
					 0 ,  0 ,  -1 ,       0
				};

				break;
		}
	}

}
