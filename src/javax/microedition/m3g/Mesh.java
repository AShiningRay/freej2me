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

import java.util.Vector;

public class Mesh extends Node
{

	private Vector appearances = new Vector();
	private Vector indexbuffer = new Vector();
	private VertexBuffer vertexbuffer;


	public Mesh() { /* DELETE THIS */ }

	public Mesh(VertexBuffer vertices, IndexBuffer[] submeshes, Appearance[] appearances) 
	{  
		if ((vertices == null) || (submeshes == null) || hasArrayNullElement(submeshes)) {
			throw new NullPointerException("Provided mesh is invalid");
		}
		if ((submeshes.length == 0) || ((appearances != null) && (appearances.length < submeshes.length))) {
			throw new IllegalArgumentException();
		}
		this.vertexbuffer = vertices;
		for (int i = 0; i < submeshes.length; ++i)
			this.indexbuffer.addElement(submeshes[i]);
		for (int i = 0; i < appearances.length; ++i)
			this.appearances.addElement(appearances[i]);
	}

	public Mesh(VertexBuffer vertices, IndexBuffer submesh, Appearance appearance) 
	{  
		if ((vertices == null) || (submesh == null)) {
			throw new NullPointerException("Provided mesh has invalid submesh or number of vertices.");
		}
		this.vertexbuffer = vertices;
		this.indexbuffer.addElement(submesh);
		this.appearances.addElement(appearance);
	}


	public Appearance getAppearance(int index) { return (Appearance) appearances.elementAt(index); }

	@Override
	public int getReferences(Object3D[] references) throws IllegalArgumentException {
		int parentCount = super.getReferences(references);

		if (vertexbuffer != null) {
			if (references != null)
				references[parentCount] = vertexbuffer;
			++parentCount;
		}

		for (int i = 0; i < indexbuffer.size(); ++i) {
			if (references != null)
				references[parentCount] = (Object3D) indexbuffer.elementAt(i);
			++parentCount;
		}

		for (int i = 0; i < appearances.size(); ++i) {
			if (references != null)
				references[parentCount] = (Object3D) appearances.elementAt(i);
			++parentCount;
		}

		return parentCount;
	}

	private boolean hasArrayNullElement(IndexBuffer[] buffer) {
		for (int i = 0; i < buffer.length; i++) {
			if (buffer[i] == null) {
				return true;
			}
		}
		return false;
	}

	public IndexBuffer getIndexBuffer(int index) { return (IndexBuffer) indexbuffer.elementAt(index); }

	public int getSubmeshCount() { return indexbuffer.size(); }

	public VertexBuffer getVertexBuffer() { return vertexbuffer; }

	public void setAppearance(int index, Appearance a) { appearances.setElementAt(a, index); }

}
