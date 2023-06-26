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

import java.util.Hashtable;

public class Sprite3D extends Node
{

	private static Hashtable textures = new Hashtable();

	private Image2D image;
	private Appearance appearance;
	private boolean scaled;
	private Texture2D texture;

	private int cropw;
	private int croph;
	private int cropx;
	private int cropy;


	public Sprite3D(boolean isScaled, Image2D img, Appearance a)
	{
		scaled = isScaled;
		image = img;
		appearance = a;
	}


	public Appearance getAppearance() { return appearance; }

	public int getCropHeight() { return croph; }

	public int getCropWidth() { return cropw; }

	public int getCropX() { return cropx; }

	public int getCropY() { return cropy; }

	public Image2D getImage() { return image; }

	public boolean isScaled() { return scaled; }

	public void setAppearance(Appearance a) { appearance = a; }

	public void setCrop(int cropX, int cropY, int width, int height)
	{
		cropx=cropX;
		cropy=cropY;
		cropw=width;
		croph=height;
	}

	public void setImage(Image2D img) 
	{ 
		this.image = image;
		texture = (Texture2D) textures.get(image);

		if (texture == null) {
			texture = new Texture2D(image);
			texture.setFiltering(Texture2D.FILTER_LINEAR, Texture2D.FILTER_LINEAR);
			texture.setWrapping(Texture2D.WRAP_CLAMP, Texture2D.WRAP_CLAMP);
			texture.setBlending(Texture2D.FUNC_REPLACE);

			// cache texture
			textures.put(image, texture);
		}
	}

}
