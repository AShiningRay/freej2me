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

public class VertexArray extends Object3D
{

	private byte[][] inner1;
	private short[][] inner2;
	private int numVertices;
	private int numComponents;
	private int componentSize;

	public VertexArray(int numVertices, int numComponents, int componentSize)
	{
		if (numVertices < 1 || 65535 < numVertices ||
			numComponents < 2 || 4 < numComponents ||
			componentSize < 1 || 2 < componentSize)
			throw new java.lang.IllegalArgumentException();

		this.numVertices = numVertices;
		this.numComponents = numComponents;
		this.componentSize = componentSize;
		switch (componentSize)
		{
			case 1:
				this.inner1 = new byte[numVertices][numComponents];
				this.inner2 = null;
				break;
			case 2:
				this.inner1 = null;
				this.inner2 = new short[numVertices][numComponents];
				break;
		}
	}


	public void get(int firstVertex, int numVertices, byte[] values)
	{
		if (values == null)
			throw new java.lang.NullPointerException();
		if (this.componentSize != 1)
			throw new java.lang.IllegalStateException();
		if (numVertices < 0 ||
			values.length < numVertices * this.numComponents)
			throw new java.lang.IllegalArgumentException();
		if (firstVertex < 0 || this.numVertices < firstVertex + numVertices)
			throw new java.lang.IndexOutOfBoundsException();

		for (int vid = 0; vid < numVertices; vid++)
			for (int cid = 0; cid < this.numComponents; cid++)
			{
				int abs_vid = vid + firstVertex;
				int flat_id = vid * this.numComponents + cid;
				values[flat_id] = this.inner1[abs_vid][cid];
			}
	}

	public void get(int firstVertex, int numVertices, short[] values)
	{
		if (values == null)
			throw new java.lang.NullPointerException();
		if (this.componentSize != 2)
			throw new java.lang.IllegalStateException();
		if (numVertices < 0 ||
			values.length < numVertices * this.numComponents)
			throw new java.lang.IllegalArgumentException();
		if (firstVertex < 0 || this.numVertices < firstVertex + numVertices)
			throw new java.lang.IndexOutOfBoundsException();

		for (int vid = 0; vid < numVertices; vid++)
			for (int cid = 0; cid < this.numComponents; cid++)
			{
				int abs_vid = vid + firstVertex;
				int flat_id = vid * this.numComponents + cid;
				values[flat_id] = this.inner2[abs_vid][cid];
			}
	}

	public int getComponentCount()
	{
		return this.numComponents;
	}

	public int getComponentType()
	{
		return this.componentSize;
	}

	public int getVertexCount()
	{
		return this.numVertices;
	}

	public void set(int firstVertex, int numVertices, byte[] values)
	{
		if (values == null)
			throw new java.lang.NullPointerException();
		if (this.componentSize != 1)
			throw new java.lang.IllegalStateException();
		if (numVertices < 0 ||
			values.length < numVertices * this.numComponents)
			throw new java.lang.IllegalArgumentException();
		if (firstVertex < 0 || this.numVertices < firstVertex + numVertices)
			throw new java.lang.IndexOutOfBoundsException();

		for (int vid = 0; vid < numVertices; vid++)
			for (int cid = 0; cid < this.numComponents; cid++)
			{
				int abs_vid = vid + firstVertex;
				int flat_id = vid * this.numComponents + cid;
				this.inner1[abs_vid][cid] = values[flat_id];
			}
	}

	public void set(int firstVertex, int numVertices, short[] values)
	{
		if (values == null)
			throw new java.lang.NullPointerException();
		if (this.componentSize != 2)
			throw new java.lang.IllegalStateException();
		if (numVertices < 0 ||
			values.length < numVertices * this.numComponents)
			throw new java.lang.IllegalArgumentException();
		if (firstVertex < 0 || this.numVertices < firstVertex + numVertices)
			throw new java.lang.IndexOutOfBoundsException();

		for (int vid = 0; vid < numVertices; vid++)
			for (int cid = 0; cid < this.numComponents; cid++)
			{
				int abs_vid = vid + firstVertex;
				int flat_id = vid * this.numComponents + cid;
				this.inner2[abs_vid][cid] = values[flat_id];
			}
	}

}
