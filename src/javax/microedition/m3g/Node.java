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

public abstract class Node extends Transformable
{

	public static final int NONE  = 144;
	public static final int ORIGIN  = 145;
	public static final int X_AXIS  = 146;
	public static final int Y_AXIS  = 147;
	public static final int Z_AXIS  = 148;

	private Node parentNode = null;
	private Node alignRef = null;
	private Node yRef = null;
	private Node zRef = null;
	private int yTarget = NONE;
	private int zTarget = NONE;
	private float alphaFactor = 1f;
	private boolean picking = true;
	private boolean rendering = true;
	private int scope = -1;

	boolean hasRenderables = false;
	boolean hasBones = false;
	boolean[] dirty = new boolean[2];


	public void align(Node reference) 
	{  
		/* 
		 * TODO: Throw...
		 * java.lang.IllegalArgumentException - if reference is not in the same scene graph as this node 
    	 * java.lang.IllegalStateException - if the zRef or yRef node of any aligned node is not in the same scene graph as the aligned node 
    	 * java.lang.IllegalStateException - if any node is aligned to itself or its descendant (note: this applies to null alignment references, as well) 
    	 * java.lang.ArithmeticException - if a transformation required in the alignment computations cannot be computed
		*/

		Node root = this.getRootNode();
		Node zRef = this.zRef;
		Node yRef = this.yRef;
		int zTarget = this.zTarget;
		int yTarget = this.yTarget;

		System.out.println("align(Node)");
		/* TODO: Flesh out the alignment itself. */
		if (zTarget != NONE || yTarget != NONE)
		{
			if(reference == null) /* Align based on this node. */
			{
				System.out.println("align(Node) based on this node");
			} else /* Align based on the reference node. */
			{
				System.out.println("align(Node) based on reference node");
			}
		}
		
	}

	static boolean isChildOf(Node parent, Node child) {
		Node n;
		for (n = child; n != null; n = n.parentNode)
			if (n.parentNode == parent)
				return true;

		return false;
	}

	@Override
	void updateProperty(int property, float[] value) {
		switch (property) {
			case AnimationTrack.ALPHA:
				alphaFactor = (int) (Math.max(0.f, Math.min(1.f, value[0]) * 0xFFFF));
				break;
			case AnimationTrack.PICKABILITY:
				picking = (value[0] >= 0.5f);
				break;
			case AnimationTrack.VISIBILITY:
				rendering = (value[0] >= 0.5f);
				break;
			default:
				super.updateProperty(property, value);
		}
	}

	public Node getAlignmentReference(int axis) 
	{ 
		if(axis == Y_AXIS) { return this.yRef; }
		else if(axis == Z_AXIS) {return this.zRef; }

		/* If it's not Y_AXIS or Z_AXIS, throw IllegalArgumentException as per JSR-184. */
		throw new IllegalArgumentException("Tried requesting alignment reference on invalid axis.");
	}

	public int getAlignmentTarget(int axis) 
	{ 
		if(axis == Y_AXIS) { return this.yTarget; }
		else if(axis == Z_AXIS) {return this.zTarget; }

		/* If it's not Y_AXIS or Z_AXIS, throw IllegalArgumentException as per JSR-184. */
		throw new IllegalArgumentException("Tried requesting alignment target on invalid axis.");
	}

	public float getAlphaFactor() { return this.alphaFactor; }

	public Node getParent() { return this.parentNode; }

	public int getScope() { return this.scope; }

	public boolean getTransformTo(Node target, Transform transform) { return false; }

	public boolean isPickingEnabled() { return this.picking; }

	public boolean isRenderingEnabled() { return this.rendering; }

	public void setAlignment(Node zRef, int zTarget, Node yRef, int yTarget) 
	{
		/* 
		 * As per JSR-184, throw IllegalArgumentException if:
		 * yTarget or zTarget is not one of the symbolic constants listed above
		 * (zRef == yRef) && (zTarget == yTarget != NONE)
		 * zRef or yRef is this Node.
		 */
		if ( ((zTarget != this.NONE) && (zTarget != this.X_AXIS) && (zTarget != this.Y_AXIS) && (zTarget != this.Z_AXIS) && (zTarget != this.ORIGIN)) 
			|| ((yTarget != this.NONE) && (yTarget != this.X_AXIS) && (yTarget != this.Y_AXIS) && (yTarget != this.Z_AXIS) && (yTarget != this.ORIGIN)) )
			{ throw new IllegalArgumentException("Node target axis is invalid."); }
		if ((zRef == yRef) && (zTarget != NONE || yTarget != NONE))
			{ throw new IllegalArgumentException("Tried to align with two references having the same axis."); }
		if (zRef == this || yRef == this)
			{ throw new IllegalArgumentException("Tried to use this node as one of the reference nodes."); }
		
		this.zRef = (zTarget != NONE) ? zRef : null;
		this.yRef = (yTarget != NONE) ? yRef : null;
		this.zTarget = zTarget;
		this.yTarget = yTarget;
	}

	public void setAlphaFactor(float alphaFactor) 
	{ 
		/* As per JSR-184, throw IllegalArgumentException if factor < 0 or factor > 1.0.*/
		if (alphaFactor < 0 || alphaFactor > 1)
			{ throw new IllegalArgumentException("Tried to set AlphaFactor with out of range value."); }
		
		this.alphaFactor = alphaFactor; 
	}

	public void setPickingEnable(boolean enable) { this.picking = enable; }

	public void setRenderingEnable(boolean enable) { this.rendering = enable; }

	public void setScope(int scope) { this.scope = scope; }

	public void setParent(Node parentNode) 
	{
		/* TODO: Flesh out this method. */
		this.parentNode = parentNode;
	}

	/* Mostly used so we can find whether a child node*/
	public Node getRootNode()
	{
		Node root = this;
		
		while (root.getParent() != null) { root = root.getParent(); }
		
		return root;
	}

}
