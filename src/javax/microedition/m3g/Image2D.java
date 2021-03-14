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

import org.recompile.mobile.PlatformImage;
import java.awt.image.Raster;

public class Image2D extends Object3D
{

	public static final int ALPHA = 96;
	public static final int LUMINANCE = 97;
	public static final int LUMINANCE_ALPHA = 98;
	public static final int RGB = 99;
	public static final int RGBA = 100;


	private byte[] image;
	private int width;
	private int height;
	private int format;
	private boolean mutable;

	public Image2D(int format, int w, int h)
	{
		this.mutable = true;
		this.width = w;
		this.height = h;
		this.format = format;
	}

	public Image2D(int format, int w, int h, byte[] image)
	{
		this.mutable = false;
		this.width = w;
		this.height = h;
		this.format = format;
		this.image = image;
	}

	public Image2D(int format, int w, int h, byte[] image, byte[] Palette)
	{
		this.mutable = false;
		this.width = w;
		this.height = h;
		this.format = format;
		this.image = image;
	}

	public Image2D(int format, Object image)
	{
		if (image == null)
			throw new java.lang.NullPointerException();
		if (format != ALPHA &&
			format != LUMINANCE &&
			format != LUMINANCE_ALPHA &&
			format != RGB &&
			format != RGBA)
			throw new java.lang.IllegalArgumentException();
		if (!(image instanceof PlatformImage))
			throw new java.lang.IllegalArgumentException();

		Raster img = ((PlatformImage) image).getCanvas().getData();
		int bppSrc = img.getNumBands();
		int[] buf = new int[bppSrc];

		this.mutable = false;
		this.width = img.getWidth();
		this.height = img.getHeight();
		this.format = format;
		int bpp = this.bpp();
		this.image = new byte[this.width * this.height * bpp];
		for (int row = 0; row < this.height; row++)
			for (int col = 0; col < this.width; col++)
			{
				img.getPixel(col, row, buf);
				for (int ch = 0; ch < bpp; ch++)
				{
					this.image[bpp * (this.width * row + col) + ch] =
						(byte) buf[ch % bppSrc];
				}
			}
	}


	public int getFormat()
	{
		return this.format;
	}

	public int getHeight()
	{
		return this.height;
	}

	public int getWidth()
	{
		return this.width;
	}

	public boolean isMutable()
	{
		return this.mutable;
	}

	public void set(int x, int y, int w, int h, byte[] image)
	{
		if (image == null)
			throw new java.lang.NullPointerException();
		if (!this.mutable)
			throw new java.lang.IllegalStateException();
		if (x < 0 || y < 0 || w <= 0 || h <= 0 ||
			x + w > this.width || y + h > this.height ||
			image.length < w * h * this.bpp())
			throw new java.lang.IllegalArgumentException();

		for (int i = 0; i < w; i++)
			for (int j = 0; j < h; j++)
				this.image[this.width * (y + j) + (x + i)] = image[j * w + i];
	}

	int getPixel(int x, int y)
	{
		x = ((x % this.width) + this.width) % this.width;
		y = ((y % this.height) + this.height) % this.height;
		int offset = this.bpp() * (this.width * y + x);
		int result = 0;
		for (int ch = 0; ch < this.bpp(); ch++)
			result |= this.image[offset + ch] << (8 * (this.bpp() - ch - 1));
		return result;
	}

	int[] getPixelArr(int x, int y)
	{
		x = ((x % this.width) + this.width) % this.width;
		y = ((y % this.height) + this.height) % this.height;
		int offset = this.bpp() * (this.width * y + x);
		int[] result = new int[] { 0, 0, 0, 255 };
		for (int ch = 0; ch < this.bpp(); ch++)
			result[ch] = this.image[offset + ch];
		return result;
	}

	private int bpp()
	{
		switch (this.format)
		{
			case ALPHA:
				return 1;
			case LUMINANCE:
				return 1;
			case LUMINANCE_ALPHA:
				return 2;
			case RGB:
				return 3;
			case RGBA:
				return 4;
			default:
				return 0;
		}
	}
}
