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

public class Group extends Node
{

	private Vector<Node> childrenNodes;
	int numCullableNodes;
	int numRenderableNodes;

	public Group() 
	{ 
		this.childrenNodes = new Vector<Node>();
		this.numCullableNodes = 0;
		this.numRenderableNodes = 0;
	}


	public void addChild(Node child)
	{
		/* As per JSR-184, throw NullPointerException if child is null. */
		if (child == null)
			{ throw new NullPointerException("Received a null child node."); };

		/* Also per JSR-184, throw IllegalArgumentException if child is this group, already has a parent and is a world node. */
		if (child == this) 
			{ throw new IllegalArgumentException("Child node is this group."); };
		if (child.getParent() != null)
			{ throw new IllegalArgumentException("Child node already has a parent node."); };
		if(child instanceof World)
			{ throw new IllegalArgumentException("Child node is a World node."); };
		
		/* Also per JSR-184, throw IllegalArgumentException if the child node is actually an ancestor of this Group. */
		/* if(child instanceof Group && child.getChild(TODO))
			{ throw new IllegalArgumentException("Received child node is an ancestor of this group."); }; */

		childrenNodes.add(child);
		child.setParent(this);
	}

	public Node getChild(int index) { return (Node) childrenNodes.elementAt(index); }

	public int getChildCount() { return childrenNodes.size(); }

	public boolean pick(int scope, float x, float y, Camera camera, RayIntersection ri) { return false; }

	public boolean pick(int scope, float ox, float oy, float oz, float dx, float dy, float dz, RayIntersection ri) { return false; }

	public void removeChild(Node child) 
	{ 
		childrenNodes.remove(child);
		child.setParent(null);
	}

	public int getReferences(Object3D[] references) 
	{
		int parentCount = super.getReferences(references);
		if (references != null)
		{
			for (int index = 0; index < childrenNodes.size(); ++index)
				{ references[parentCount + index] = (Object3D) childrenNodes.get(index); }
		}
		return parentCount + childrenNodes.size();
	}

}
