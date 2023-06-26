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

public class Appearance extends Object3D
{

	private CompositingMode compositingMode;
	private Fog fog;
	private Material material;
	private PolygonMode polygonMode;
	private int layer;
	private Texture2D[] texture;


	public Appearance()
	{
		this.layer = 0;
		this.polygonMode = null;
		this.compositingMode = null;
		this.texture = new Texture2D[Graphics3D.NUM_TEXTURE_UNITS];
		this.material = null;
		this.fog = null;
	}


	public CompositingMode getCompositingMode()
	{
		return this.compositingMode;
	}

	public Fog getFog()
	{
		return this.fog;
	}

	public int getLayer()
	{
		return this.layer;
	}

	public Material getMaterial()
	{
		return this.material;
	}

	public PolygonMode getPolygonMode()
	{
		return this.polygonMode;
	}

	public Texture2D getTexture(int index)
	{
		if (index < 0 || Graphics3D.NUM_TEXTURE_UNITS - 1 < index)
			throw new java.lang.IndexOutOfBoundsException("Tried to get texture from invalid index");

		return this.texture[index];
	}

	public void setCompositingMode(CompositingMode compositingMode)
	{
		this.compositingMode = compositingMode;
	}

	public void setFog(Fog fog)
	{
		this.fog = fog;
	}

	public void setLayer(int layer)
	{
		if (layer < -63 || 63 < layer)
			throw new java.lang.IndexOutOfBoundsException("Cannot set invalid layer");

		this.layer = layer;
	}

	public void setMaterial(Material material)
	{
		this.material = material;
	}

	public void setPolygonMode(PolygonMode polygonMode)
	{
		this.polygonMode = polygonMode;
	}

	public void setTexture(int index, Texture2D texture)
	{
		if (index < 0 || Graphics3D.NUM_TEXTURE_UNITS - 1 < index)
			throw new java.lang.IndexOutOfBoundsException("Tried to set texture on an invalid index");

		this.texture[index] = texture;
	}

}
