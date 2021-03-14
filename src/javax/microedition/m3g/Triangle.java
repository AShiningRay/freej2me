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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;

// package-private
class Triangle
{
	static final float EPSILON = Float.MIN_VALUE * 16f;

	static float[][] xp = new float[][] {{ 0, 0, 0, 0 }, {-1, 0, 0, 1 }};
	static float[][] xn = new float[][] {{ 0, 0, 0, 0 }, { 1, 0, 0, 1 }};
	static float[][] yp = new float[][] {{ 0, 0, 0, 0 }, { 0,-1, 0, 1 }};
	static float[][] yn = new float[][] {{ 0, 0, 0, 0 }, { 0, 1, 0, 1 }};
	static float[][] zp = new float[][] {{ 0, 0, 0, 0 }, { 0, 0,-1, 1 }};
	static float[][] zn = new float[][] {{ 0, 0, 0, 0 }, { 0, 0, 1, 1 }};

	float[] v;
		// xA, yA, zA, wA,
		// xB, yB, zB, wB,
		// xC, yC, zC, wC;
		// 0   1   2   3

	float[] t;
		// sA, tA, rA, qA,
		// sB, tB, rB, qB,
		// sC, tC, rC, qC;
		// 0   1   2   3

	Triangle(float[] vertices, float[] texcoords)
	{
		this.v = vertices;
		this.t = texcoords;
	}

	static Triangle[] fromVertAndTris(float[] vert, float[] texc, int[] tris)
	{
		Triangle[] result = new Triangle[tris.length / 3];

		for (int tri_id = 0; tri_id < tris.length / 3; tri_id++)
			result[tri_id] = new Triangle(new float[] {
				vert[4 * tris[3 * tri_id + 0] + 0],
				vert[4 * tris[3 * tri_id + 0] + 1],
				vert[4 * tris[3 * tri_id + 0] + 2],
				vert[4 * tris[3 * tri_id + 0] + 3],
				vert[4 * tris[3 * tri_id + 1] + 0],
				vert[4 * tris[3 * tri_id + 1] + 1],
				vert[4 * tris[3 * tri_id + 1] + 2],
				vert[4 * tris[3 * tri_id + 1] + 3],
				vert[4 * tris[3 * tri_id + 2] + 0],
				vert[4 * tris[3 * tri_id + 2] + 1],
				vert[4 * tris[3 * tri_id + 2] + 2],
				vert[4 * tris[3 * tri_id + 2] + 3]
			}, texc == null ? new float[12] : new float[] {
				texc[4 * tris[3 * tri_id + 0] + 0],
				texc[4 * tris[3 * tri_id + 0] + 1],
				texc[4 * tris[3 * tri_id + 0] + 2],
				texc[4 * tris[3 * tri_id + 0] + 3],
				texc[4 * tris[3 * tri_id + 1] + 0],
				texc[4 * tris[3 * tri_id + 1] + 1],
				texc[4 * tris[3 * tri_id + 1] + 2],
				texc[4 * tris[3 * tri_id + 1] + 3],
				texc[4 * tris[3 * tri_id + 2] + 0],
				texc[4 * tris[3 * tri_id + 2] + 1],
				texc[4 * tris[3 * tri_id + 2] + 2],
				texc[4 * tris[3 * tri_id + 2] + 3]
			});

		return result;
	}

	float xA() { return this.v[4*0 + 0]; }
	float yA() { return this.v[4*0 + 1]; }
	float zA() { return this.v[4*0 + 2]; }
	float wA() { return this.v[4*0 + 3]; }
	float xB() { return this.v[4*1 + 0]; }
	float yB() { return this.v[4*1 + 1]; }
	float zB() { return this.v[4*1 + 2]; }
	float wB() { return this.v[4*1 + 3]; }
	float xC() { return this.v[4*2 + 0]; }
	float yC() { return this.v[4*2 + 1]; }
	float zC() { return this.v[4*2 + 2]; }
	float wC() { return this.v[4*2 + 3]; }

	float sA() { return this.t[4*0 + 0]; }
	float tA() { return this.t[4*0 + 1]; }
	float rA() { return this.t[4*0 + 2]; }
	float qA() { return this.t[4*0 + 3]; }
	float sB() { return this.t[4*1 + 0]; }
	float tB() { return this.t[4*1 + 1]; }
	float rB() { return this.t[4*1 + 2]; }
	float qB() { return this.t[4*1 + 3]; }
	float sC() { return this.t[4*2 + 0]; }
	float tC() { return this.t[4*2 + 1]; }
	float rC() { return this.t[4*2 + 2]; }
	float qC() { return this.t[4*2 + 3]; }

	Stream<Triangle> clip()
	{
		Triangle[] orig = new Triangle[] { this };
		return Arrays.stream(orig)
			.filter(t -> t.isValid())
			.flatMap(t -> Arrays.stream(t.clipPlane(xp[0], xp[1])))
			.flatMap(t -> Arrays.stream(t.clipPlane(xn[0], xn[1])))
			.flatMap(t -> Arrays.stream(t.clipPlane(yp[0], yp[1])))
			.flatMap(t -> Arrays.stream(t.clipPlane(yn[0], yn[1])))
			.flatMap(t -> Arrays.stream(t.clipPlane(zp[0], zp[1])))
			.flatMap(t -> Arrays.stream(t.clipPlane(zn[0], zn[1])))
			.map(t -> t.project());
	}

	static void transform(Triangle[] triangles, Transform trVert, Transform trTex)
	{
		for (int i = 0; i < triangles.length; i++)
		{
			trVert.transform(triangles[i].v);
			if (triangles[i].t != null && trTex != null)
				trTex.transform(triangles[i].t);
		}
	}

	private boolean isValid()
	{
		return
			this.wA() >= EPSILON ||
			this.wB() >= EPSILON ||
			this.wC() >= EPSILON;
	}

	private Triangle project()
	{
		this.v[4*0 + 0] = this.v[4*0 + 0] / this.v[4*0 + 3];
		this.v[4*0 + 1] = this.v[4*0 + 1] / this.v[4*0 + 3];
		this.v[4*0 + 2] = this.v[4*0 + 2] / this.v[4*0 + 3];
		this.v[4*0 + 3] = 1f;
		this.v[4*1 + 0] = this.v[4*1 + 0] / this.v[4*1 + 3];
		this.v[4*1 + 1] = this.v[4*1 + 1] / this.v[4*1 + 3];
		this.v[4*1 + 2] = this.v[4*1 + 2] / this.v[4*1 + 3];
		this.v[4*1 + 3] = 1f;
		this.v[4*2 + 0] = this.v[4*2 + 0] / this.v[4*2 + 3];
		this.v[4*2 + 1] = this.v[4*2 + 1] / this.v[4*2 + 3];
		this.v[4*2 + 2] = this.v[4*2 + 2] / this.v[4*2 + 3];
		this.v[4*2 + 3] = 1f;

		this.t[4*0 + 0] = this.t[4*0 + 0] / this.t[4*0 + 3];
		this.t[4*0 + 1] = this.t[4*0 + 1] / this.t[4*0 + 3];
		this.t[4*0 + 2] = this.t[4*0 + 2] / this.t[4*0 + 3];
		this.t[4*0 + 3] = 1f;
		this.t[4*1 + 0] = this.t[4*1 + 0] / this.t[4*1 + 3];
		this.t[4*1 + 1] = this.t[4*1 + 1] / this.t[4*1 + 3];
		this.t[4*1 + 2] = this.t[4*1 + 2] / this.t[4*1 + 3];
		this.t[4*1 + 3] = 1f;
		this.t[4*2 + 0] = this.t[4*2 + 0] / this.t[4*2 + 3];
		this.t[4*2 + 1] = this.t[4*2 + 1] / this.t[4*2 + 3];
		this.t[4*2 + 2] = this.t[4*2 + 2] / this.t[4*2 + 3];
		this.t[4*2 + 3] = 1f;

		return this;
	}

	private Triangle[] clipPlane(float[] p, float[] pn)
	{
		pn = div(pn, (float) Math.sqrt(dot(pn, pn)));
		ArrayList<Integer> vin = new ArrayList<Integer>();
		ArrayList<Integer> vout = new ArrayList<Integer>();
		float[][] vert = new float[][] {
			Arrays.copyOfRange(this.v, 4*0, 4*0+4),
			Arrays.copyOfRange(this.v, 4*1, 4*1+4),
			Arrays.copyOfRange(this.v, 4*2, 4*2+4)
		};
		float[][] tex = new float[][] {
			Arrays.copyOfRange(this.t, 4*0, 4*0+4),
			Arrays.copyOfRange(this.t, 4*1, 4*1+4),
			Arrays.copyOfRange(this.t, 4*2, 4*2+4)
		};

		for (int i = 0; i < 3; i++)
			if (dot(pn, vert[i]) - dot(pn, p) >= 0)
				vin.add(i);
			else
				vout.add(i);

		float[] v1, v2, t1, t2;
		float[][] n1, n2;
		v1 = new float[12];
		v2 = new float[12];
		t1 = new float[12];
		t2 = new float[12];
		switch (vin.size())
		{
			case 0:
				return new Triangle[0];
			case 1:
				n1 = intersect(p, pn, vert[vin.get(0)], vert[vout.get(0)],
										tex[vin.get(0)], tex[vout.get(0)]);
				n2 = intersect(p, pn, vert[vin.get(0)], vert[vout.get(1)],
										tex[vin.get(0)], tex[vout.get(1)]);
				v1[4*0 + 0] = vert[vin.get(0)][0];
				v1[4*0 + 1] = vert[vin.get(0)][1];
				v1[4*0 + 2] = vert[vin.get(0)][2];
				v1[4*0 + 3] = vert[vin.get(0)][3];
				v1[4*1 + 0] = n1[0][0];
				v1[4*1 + 1] = n1[0][1];
				v1[4*1 + 2] = n1[0][2];
				v1[4*1 + 3] = n1[0][3];
				v1[4*2 + 0] = n2[0][0];
				v1[4*2 + 1] = n2[0][1];
				v1[4*2 + 2] = n2[0][2];
				v1[4*2 + 3] = n2[0][3];

				t1[4*0 + 0] = tex[vin.get(0)][0];
				t1[4*0 + 1] = tex[vin.get(0)][1];
				t1[4*0 + 2] = tex[vin.get(0)][2];
				t1[4*0 + 3] = tex[vin.get(0)][3];
				t1[4*1 + 0] = n1[1][0];
				t1[4*1 + 1] = n1[1][1];
				t1[4*1 + 2] = n1[1][2];
				t1[4*1 + 3] = n1[1][3];
				t1[4*2 + 0] = n2[1][0];
				t1[4*2 + 1] = n2[1][1];
				t1[4*2 + 2] = n2[1][2];
				t1[4*2 + 3] = n2[1][3];
				return new Triangle[] { new Triangle(v1, t1) };
			case 2:
				n1 = intersect(p, pn, vert[vin.get(0)], vert[vout.get(0)],
										tex[vin.get(0)], tex[vout.get(0)]);
				n2 = intersect(p, pn, vert[vin.get(1)], vert[vout.get(0)],
										tex[vin.get(1)], tex[vout.get(0)]);
				v1[4*0 + 0] = vert[vin.get(0)][0];
				v1[4*0 + 1] = vert[vin.get(0)][1];
				v1[4*0 + 2] = vert[vin.get(0)][2];
				v1[4*0 + 3] = vert[vin.get(0)][3];
				v1[4*1 + 0] = vert[vin.get(1)][0];
				v1[4*1 + 1] = vert[vin.get(1)][1];
				v1[4*1 + 2] = vert[vin.get(1)][2];
				v1[4*1 + 3] = vert[vin.get(1)][3];
				v1[4*2 + 0] = n1[0][0];
				v1[4*2 + 1] = n1[0][1];
				v1[4*2 + 2] = n1[0][2];
				v1[4*2 + 3] = n1[0][3];

				t1[4*0 + 0] = tex[vin.get(0)][0];
				t1[4*0 + 1] = tex[vin.get(0)][1];
				t1[4*0 + 2] = tex[vin.get(0)][2];
				t1[4*0 + 3] = tex[vin.get(0)][3];
				t1[4*1 + 0] = tex[vin.get(1)][0];
				t1[4*1 + 1] = tex[vin.get(1)][1];
				t1[4*1 + 2] = tex[vin.get(1)][2];
				t1[4*1 + 3] = tex[vin.get(1)][3];
				t1[4*2 + 0] = n1[1][0];
				t1[4*2 + 1] = n1[1][1];
				t1[4*2 + 2] = n1[1][2];
				t1[4*2 + 3] = n1[1][3];

				v2[4*0 + 0] = vert[vin.get(1)][0];
				v2[4*0 + 1] = vert[vin.get(1)][1];
				v2[4*0 + 2] = vert[vin.get(1)][2];
				v2[4*0 + 3] = vert[vin.get(1)][3];
				v2[4*1 + 0] = n1[0][0];
				v2[4*1 + 1] = n1[0][1];
				v2[4*1 + 2] = n1[0][2];
				v2[4*1 + 3] = n1[0][3];
				v2[4*2 + 0] = n2[0][0];
				v2[4*2 + 1] = n2[0][1];
				v2[4*2 + 2] = n2[0][2];
				v2[4*2 + 3] = n2[0][3];

				t2[4*0 + 0] = tex[vin.get(1)][0];
				t2[4*0 + 1] = tex[vin.get(1)][1];
				t2[4*0 + 2] = tex[vin.get(1)][2];
				t2[4*0 + 3] = tex[vin.get(1)][3];
				t2[4*1 + 0] = n1[1][0];
				t2[4*1 + 1] = n1[1][1];
				t2[4*1 + 2] = n1[1][2];
				t2[4*1 + 3] = n1[1][3];
				t2[4*2 + 0] = n2[1][0];
				t2[4*2 + 1] = n2[1][1];
				t2[4*2 + 2] = n2[1][2];
				t2[4*2 + 3] = n2[1][3];
				return new Triangle[] {
					new Triangle(v1, t1),
					new Triangle(v2, t2)
				};
			case 3:
				return new Triangle[] { this };
		}
		throw new java.lang.IllegalStateException();
	}

	private static float[] add(float[] a, float[] b)
	{
		if (a.length != b.length)
			throw new java.lang.IllegalArgumentException();
		float[] out = new float[a.length];
		for (int i = 0; i < a.length; i++)
			out[i] = a[i] + b[i];
		return out;
	}

	private static float[] sub(float[] a, float[] b)
	{
		return add(a, neg(b));
	}

	private static float[] mul(float[] a, float b)
	{
		float[] out = new float[a.length];
		for (int i = 0; i < a.length; i++)
			out[i] = a[i] * b;
		return out;
	}

	private static float[] div(float[] a, float b)
	{
		return mul(a, 1f / b);
	}

	private static float[] neg(float[] a)
	{
		float[] out = new float[a.length];
		for (int i = 0; i < a.length; i++)
			out[i] = -1f * a[i];
		return out;
	}

	private static float dot(float[] a, float[] b)
	{
		if (a.length != b.length)
			throw new java.lang.IllegalArgumentException();
		float sum = 0;
		for (int i = 0; i < a.length; i++)
			sum += a[i] * b[i];
		return sum;
	}

	private static float[][] intersect(
		float[] p,
		float[] pn,
		float[] a,
		float[] b,
		float[] ta,
		float[] tb
	) {
		float pd, ad, bd, ratio;
		pd = dot(p, pn);
		ad = dot(a, pn);
		bd = dot(b, pn);
		ratio = (pd - ad) / (bd - ad);
		return new float[][] {
			add(a, mul(sub(b, a), ratio)),
			add(ta, mul(sub(tb, ta), ratio))
		};
	}
}