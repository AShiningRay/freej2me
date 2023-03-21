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

public class World extends Group
{

	private Background background;
	private Camera camera;

	public World() 
	{  
		this.background = null;
		this.camera = null;
	}


	public Camera getActiveCamera() { return this.camera; }

	public Background getBackground() { return this.background; }

	public void setActiveCamera(Camera cam) 
	{
		/* As per JSR-184, throw NullPointerException if received camera is null. */
		if(cam == null) { throw new NullPointerException("World cannot set a null camera."); }
		
		this.camera = cam; 
	}

	public void setBackground(Background bg) 
	{ 
		/* If received bg is null, the world's background will also be null as per JSR-184. */
		this.background = bg;
	}

	public int getNodeRefs(Object3D[] references)
	{
		int parentCount = super.getReferences(references);

		if (this.camera != null)
		{
			if (references != null) { references[parentCount] = this.camera; }
			parentCount += 1;
		}

		if (this.background != null)
		{
			if (references != null) { references[parentCount] = this.background; }
			parentCount += 1;
		}

		return parentCount;
	}

}
