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

public class SkinnedMesh extends Mesh
{

	private Group skeleton;


	public SkinnedMesh(VertexBuffer vertices, IndexBuffer[] submeshes, Appearance[] appearances, Group skeleton) 
	{  
		super(vertices, submeshes, appearances);
		checkSkeleton(skeleton);
		this.skeleton = skeleton;
	}

	public SkinnedMesh(VertexBuffer vertices, IndexBuffer submesh, Appearance appearance, Group skeleton) 
	{  
		super(vertices, submesh, appearance);
		checkSkeleton(skeleton);
		this.skeleton = skeleton;
	}


	public void addTransform(Node bone, int weight, int firstVertex, int numVertices) 
	{  
		if (bone == null) { throw new NullPointerException("Tried to apply transform to null bone"); }
	    if ((weight <= 0) || (numVertices <= 0)) { throw new IllegalArgumentException(); }
	    if ((firstVertex < 0) || (firstVertex + numVertices > 65535)) { throw new IndexOutOfBoundsException(); }
	}

	public void getBoneTransform(Node bone, Transform transform) 
	{  
		if ((bone == null) || (transform == null)) { throw new NullPointerException("Cannot get bone transform because transform or bone are null"); }
	}

	public int getBoneVertices(Node bone, int[] indices, float[] weights) 
	{ 
		if (bone == null) { throw new NullPointerException("Cannot get vertices from a null bone"); }
		return 0;
	}

	public Group getSkeleton() { return this.skeleton; }

	private void checkSkeleton(Group skeleton) 
	{
		if (skeleton == null) { throw new NullPointerException(); }
		if (skeleton.getParent() != null) { throw new IllegalArgumentException("Skeleton already has a parent"); }
	}

}
