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

public class MorphingMesh extends Mesh
{

	private VertexBuffer[] morphtargets;
	private float[] weights;


	public MorphingMesh(VertexBuffer base, VertexBuffer[] targets, IndexBuffer[] submeshes, Appearance[] appearances) 
	{  
		super(base, submeshes, appearances);
		checkTargets(targets);
		this.morphtargets = targets;
	}

	public MorphingMesh(VertexBuffer base, VertexBuffer[] targets, IndexBuffer submesh, Appearance appearance) 
	{  
		super(base, submesh, appearance);
		checkTargets(targets);
		this.morphtargets = targets;
	}


	public VertexBuffer getMorphTarget(int index) { return morphtargets[index]; }

	public int getMorphTargetCount() { return morphtargets.length; }

	public void getWeights(float[] store)
	{
		if (weights == null) 
		{
			throw new NullPointerException("Weights must not be null");
		}
		if (weights.length < getMorphTargetCount()) 
		{
			throw new IllegalArgumentException("Number of weights must be greater or equal to getMorphTargetCount()");
		}
		System.arraycopy(this.weights, 0, weights, 0, this.weights.length);
	}

	public void setWeights(float[] values) 
	{ 
		if (values == null) {
			throw new NullPointerException("Weights must not be null");
		}
		this.weights = values; 
	}

	private void checkTargets(VertexBuffer[] targets) 
	{

		if (targets == null) 
		{
			throw new NullPointerException("MorphingMesh targets are null");
		}
		if (targets.length == 0) 
		{
			throw new IllegalArgumentException("Skeleton already has a parent");
		}

		boolean hasArrayNullElement = false;
		for (int i = 0; i < targets.length; i++) 
		{
			if (targets[i] == null) { hasArrayNullElement = true; }
		}
		if (hasArrayNullElement) 
		{
			throw new IllegalArgumentException("Target array contains null elements");
		}

	}

}
