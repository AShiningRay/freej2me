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

public class Material extends Object3D
{

	public static final int AMBIENT = 1024;
	public static final int DIFFUSE = 2048;
	public static final int EMISSIVE = 4096;
	public static final int SPECULAR = 8192;

	private int ambientColor;
	private int diffuseColor;
	private int emissiveColor;
	private int specularColor;
	private float shininess;
	private boolean tracking;

	public Material() 
	{  
		this.tracking = false;
		this.ambientColor = 0x00333333;
		this.diffuseColor = 0xFFCCCCCC;
		this.emissiveColor = 0x00000000;
		this.specularColor = 0x00000000;
		this.shininess = 0f;
	}

	public int getColor(int target) 
	{ 
		/* As per JSR-184, throw IllegalArgumentException if target has a value other than AMBIENT, DIFFUSSE, EMISSIVE or SPECULAR. */
		if(target != AMBIENT || target != DIFFUSE || target != EMISSIVE || target != SPECULAR) 
			{ throw new IllegalArgumentException("Tried to get invalid color component from material."); }
		
		switch(target)
		{
			case AMBIENT:
				return this.ambientColor; 
			case DIFFUSE:
				return this.diffuseColor;
			case EMISSIVE:
				return this.emissiveColor;
			case SPECULAR:
				return this.specularColor;
		}

		return this.ambientColor; 
	}

	public float getShininess() { return this.shininess; }

	public boolean isVertexColorTrackingEnabled() { return this.tracking; }

	public void setColor(int target, int ARGB) 
	{ 
		/* As per JSR-184, throw IllegalArgumentException if target has a value other than an inclusive OR of one or more of AMBIENT, DIFFUSE, EMISSIVE, SPECULAR. */
		if((target & ~(AMBIENT | DIFFUSE | EMISSIVE | SPECULAR)) != 0) 
			{throw new IllegalArgumentException("Trying to set material color on invalid material component."); }
		
		switch(target)
		{
			case AMBIENT:
				this.ambientColor = ARGB;
				break;
			case DIFFUSE:
				this.diffuseColor = ARGB;
				break;
			case EMISSIVE:
				this.emissiveColor = ARGB;
				break;
			case SPECULAR:
				this.specularColor = ARGB;
		}
	}

	public void setShininess(float shininess) 
	{ 
		/* As per JSR-184, throw IllegalArgumentException if shininess > 128(1f) or < 0(0f). */
		if(shininess < 0f || shininess > 1f) { throw new IllegalArgumentException("Material received invalid shininess value."); }
		
		this.shininess = shininess; 
	}

	public void setVertexColorTrackingEnable(boolean enable) { this.tracking = enable; }

}
