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

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ByteOrder;

public class Background extends Object3D
{

	public static final int BORDER = 32;
	public static final int REPEAT = 33;

	private int color = 0x00000000;
	private int modex = BORDER;
	private int modey = BORDER;
	private int cropw;
	private int croph;
	private int cropx;
	private int cropy;

	private Image2D image;
	private boolean depthclear = true;
	private boolean colorclear = true;
	private Texture2D texture = null;

	private FloatBuffer vertexBuffer;
	private FloatBuffer textureBuffer;
	// top right, top left, bottom right, bottom left coordinates
	private float[] vertexArray = { 1.0f, 1.0f, 0.0f, -1.0f, 1.0f, 0.0f, 1.0f, -1.0f, 0.0f, -1.0f, -1.0f, 0.0f };
	private float[] textureArray;


	public Background() 
	{  
		vertexBuffer = ByteBuffer.allocateDirect(4 * 3 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		vertexBuffer.put(vertexArray);
		vertexBuffer.flip();
		//	4 elements, 2 coordinates per element, float type
		textureBuffer = ByteBuffer.allocateDirect(4 * 2 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		textureArray = new float[4 * 2];
	}


	public int getColor() { return color; }

	public int getCropHeight() { return croph; }

	public int getCropWidth() { return cropw; }

	public int getCropX() { return cropx; }

	public int getCropY() { return cropy; }

	public Image2D getImage() { return image; }

	public int getImageModeX() { return modex; }

	public int getImageModeY() { return modey; }

	public boolean isColorClearEnabled() { return colorclear; }

	public boolean isDepthClearEnabled() { return depthclear; }

	public void setColor(int ARGB) { color = ARGB; }

	public void setColorClearEnable(boolean enable) {  colorclear = enable; }

	public void setCrop(int cropX, int cropY, int width, int height)
	{
		cropx=cropX;
		cropy=cropY;
		cropw=width;
		croph=height;
	}

	public void setDepthClearEnable(boolean enable) { depthclear = enable; }

	public void setImage(Image2D img) 
	{ 
		if ((image != null) && (image.getFormat() != Image2D.RGB) && (image.getFormat() != Image2D.RGBA)) 
		{
			throw new IllegalArgumentException("Image format must be RGB or RGBA");
		}
		this.image = image;

		if (image != null) 
		{
			texture = new Texture2D(image);
			texture.setFiltering(Texture2D.FILTER_LINEAR, Texture2D.FILTER_LINEAR);
			texture.setWrapping(Texture2D.WRAP_CLAMP, Texture2D.WRAP_CLAMP);
			texture.setBlending(Texture2D.FUNC_REPLACE);
		} 
		else { texture = null; }
	}

	public void setImageMode(int modeX, int modeY) 
	{ 
		if (((modeX != BORDER) && (modeX != REPEAT)) || ((modeY != BORDER) && (modeY != REPEAT))) 
		{
			throw new IllegalArgumentException("Invalid image mode for background");
		}
		modex=modeX; 
		modey=modeY; 
	}

}
