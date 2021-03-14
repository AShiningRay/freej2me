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
import java.util.ArrayList;
import java.util.Arrays;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

import org.recompile.mobile.Mobile;
import org.recompile.mobile.PlatformGraphics;

public class Graphics3D
{

	public static final int ANTIALIAS = 2;
	public static final int DITHER = 4;
	public static final int OVERWRITE = 16;
	public static final int TRUE_COLOR = 8;


	public static final boolean SUPPORT_ANTIALIASING = false;
	public static final boolean SUPPORT_TRUE_COLOR = false;
	public static final boolean SUPPORT_DITHERING = false;
	public static final boolean SUPPORT_MIPMAPPING = false;
	public static final boolean SUPPORT_PERSPECTIVE_CORRECTION = false;
	public static final boolean SUPPORT_LOCAL_CAMERA_LIGHTING = false;
	public static final int MAX_LIGHTS = 8;
	public static final int MAX_VIEWPORT_WIDTH = 1024;
	public static final int MAX_VIEWPORT_HEIGHT = 1024;
	public static final int MAX_VIEWPORT_DIMENSION = 1024;
	public static final int MAX_TEXTURE_DIMENSION = 256;
	public static final int MAX_SPRITE_CROP_DIMENSION = 256;
	public static final int MAX_TRANSFORMS_PER_VERTEX = 2;
	public static final int NUM_TEXTURE_UNITS = 1;
	private static Hashtable properties;

	// Render target
	private Object target;

	// Viewport
	private int viewx;
	private int viewy;
	private int vieww;
	private int viewh;

	// Depth buffer (not used currently)
	private boolean depthEnabled;
	private float[] depthBuffer;
	private float near;
	private float far;

	private int hints;

	private Camera currCam;
	private Transform currCamTrans;
	private Transform currCamTransInv;
	private ArrayList<Light> currLights;
	private ArrayList<Transform> currLightTrans;


	public Graphics3D()
	{
		this.near = 0;
		this.far = 1;
		this.currCam = null;
		this.currCamTrans = null;
		this.currCamTransInv = null;
		this.currLights = new ArrayList<Light>();
		this.currLightTrans = new ArrayList<Transform>();
	}

	public int addLight(Light light, Transform transform)
	{
		if (light == null)
			throw new java.lang.NullPointerException();

		if (transform == null)
			transform = new Transform();

		this.currLights.add(light);
		this.currLightTrans.add(transform);
		return this.currLights.size() - 1;
	}

	public void bindTarget(Object target)
	{
		this.bindTarget(target, true, 0);
	}

	public void bindTarget(Object target, boolean depthBuffer, int hints)
	{
		if (target == null)
			throw new java.lang.NullPointerException();
		if (this.target != null)
			throw new java.lang.IllegalStateException();

		if (target instanceof Image2D)
		{
			Image2D i2d = (Image2D) target;

			if (i2d.getFormat() != Image2D.RGB &&
				i2d.getFormat() != Image2D.RGBA)
				throw new java.lang.IllegalArgumentException();

			this.viewx = 0;
			this.viewy = 0;
			this.vieww = i2d.getWidth();
			this.viewh = i2d.getHeight();
		}
		else if (target instanceof PlatformGraphics)
		{
			// This is supposed to be either of the following:
			//   - java.awt.Graphics
			//   - javax.microedition.lcdui.Graphics
			// but we're getting org.recompile.mobile.PlatformGraphics.
			//
			// I assume it serves the same purpose and will work as expected.

			PlatformGraphics grp = (PlatformGraphics) target;
			this.viewx = grp.getClipX();
			this.viewy = grp.getClipY();
			this.vieww = grp.getClipWidth();
			this.viewh = grp.getClipHeight();
		} else
			throw new java.lang.IllegalArgumentException();

		if (this.vieww > MAX_VIEWPORT_WIDTH ||
			this.viewh > MAX_VIEWPORT_HEIGHT ||
			(hints & ~(ANTIALIAS | DITHER | TRUE_COLOR | OVERWRITE)) != 0)
			throw new java.lang.IllegalArgumentException();

		this.target = target;
		this.depthBuffer = new float[this.vieww * this.viewh];
		this.depthEnabled = depthBuffer;
		this.hints = hints;
	}

	public void clear(Background background)
	{
		if (this.target == null)
			throw new java.lang.IllegalStateException();

		int color = 0;
		int x = viewx;
		int y = viewy;
		int w = vieww;
		int h = viewh;
		boolean clearColor = true;
		boolean clearDepth = true;

		if (background != null)
		{
			color = background.getColor();
			x = background.getCropX();
			y = background.getCropY();
			w = background.getCropWidth();
			h = background.getCropHeight();
			clearColor = background.isColorClearEnabled();
			clearDepth = background.isDepthClearEnabled();
		}

		if (clearColor)
		{
			if (this.target instanceof Image2D)
			{
				Image2D i2d = (Image2D) this.target;

				// CHECK is the bg image used only if clearColor is true?

				// TODO do this check in the PlatformGraphics branch too
				if (background.getImage() == null ||
					background.getImage().getFormat() != i2d.getFormat())
					throw new java.lang.IllegalArgumentException();

				// TODO support clearing Image2D
			}
			else if (this.target instanceof PlatformGraphics)
			{
				PlatformGraphics grp = (PlatformGraphics) this.target;
				grp.setColor(color);
				grp.fillRect(x, y, w, h);
			}
		}

		if (clearDepth)
			Arrays.fill(this.depthBuffer, this.far);
	}

	public Camera getCamera(Transform transform)
	{
		if (transform != null)
			transform.set(this.currCamTrans);
		return this.currCam;
	}

	public float getDepthRangeFar()
	{
		return far;
	}

	public float getDepthRangeNear()
	{
		return near;
	}

	public int getHints()
	{
		return hints;
	}

	public static Graphics3D getInstance()
	{
		return Mobile.getGraphics3D();
	}

	public Light getLight(int index, Transform transform)
	{
		if (index < 0 ||
			index > this.currLights.size())
			throw new java.lang.IndexOutOfBoundsException();

		if (transform != null)
			transform.set(this.currLightTrans.get(index));

		return this.currLights.get(index);
	}

	public int getLightCount()
	{
		// This is supposed to include nulls, so just return the size
		return this.currLights.size();
	}

	public static Hashtable getProperties()
	{
		if (Graphics3D.properties != null)
			return Graphics3D.properties;

		Hashtable<String, Object> p = new Hashtable<String, Object>();
		p.put("supportAntialiasing", SUPPORT_ANTIALIASING);
		p.put("supportTrueColor", SUPPORT_TRUE_COLOR);
		p.put("supportDithering", SUPPORT_DITHERING);
		p.put("supportMipmapping", SUPPORT_MIPMAPPING);
		p.put("supportPerspectiveCorrection", SUPPORT_PERSPECTIVE_CORRECTION);
		p.put("supportLocalCameraLighting", SUPPORT_LOCAL_CAMERA_LIGHTING);
		p.put("maxLights", MAX_LIGHTS);
		p.put("maxViewportWidth", MAX_VIEWPORT_WIDTH);
		p.put("maxViewportHeight", MAX_VIEWPORT_HEIGHT);
		p.put("maxViewportDimension", MAX_VIEWPORT_DIMENSION);
		p.put("maxTextureDimension", MAX_TEXTURE_DIMENSION);
		p.put("maxSpriteCropDimension", MAX_SPRITE_CROP_DIMENSION);
		p.put("maxTransformsPerVertex", MAX_TRANSFORMS_PER_VERTEX);
		p.put("numTextureUnits", NUM_TEXTURE_UNITS);
		Graphics3D.properties = p;

		return Graphics3D.properties;
	}

	public Object getTarget()
	{
		return this.target;
	}

	public int getViewportHeight()
	{
		return viewh;
	}

	public int getViewportWidth()
	{
		return vieww;
	}

	public int getViewportX()
	{
		return viewx;
	}

	public int getViewportY()
	{
		return viewy;
	}

	public boolean isDepthBufferEnabled()
	{
		return this.depthEnabled;
	}

	public void releaseTarget()
	{
		this.target = null;
	}

	public void render(Node node, Transform transform)
	{
		if (node == null)
			throw new java.lang.NullPointerException();
		if (!(
			node instanceof Sprite3D ||
			node instanceof Mesh ||
			node instanceof Group))
			throw new java.lang.IllegalArgumentException();
		if (this.target == null ||
			this.currCam == null)
			throw new java.lang.IllegalStateException();

		// if any Mesh that is rendered violates the constraints defined in
		//    Mesh, MorphingMesh, SkinnedMesh, VertexBuffer, or IndexBuffer
		//    throw new java.lang.IllegalStateException();

		System.out.println("Graphics3D.render NT");
		// TODO implement Graphics3D.render(Node, Transform)
	}

	public void render(
		VertexBuffer vertices,
		IndexBuffer triangles,
		Appearance appearance,
		Transform transform
	) {
		this.render(vertices, triangles, appearance, transform, -1);
	}

	public void render(
		VertexBuffer vertices,
		IndexBuffer triangles,
		Appearance appearance,
		Transform transform,
		int scope
	) {
		if (vertices == null ||
			triangles == null ||
			appearance == null)
			throw new java.lang.NullPointerException();
		if (this.target == null ||
			this.currCam == null)
			throw new java.lang.IllegalStateException();

		// if `vertices` or `triangles` violates the constraints
		//    defined in VertexBuffer or IndexBuffer
		//    throw new java.lang.IllegalStateException();

		float[] scaleBias = new float[4];

		VertexArray vertRaw = vertices.getPositions(scaleBias);
		int vertCount = vertRaw.getVertexCount();

		float scale = scaleBias[0];
		float biasX = scaleBias[1];
		float biasY = scaleBias[2];
		float biasZ = scaleBias[3];

		int[] triIndices = new int[triangles.getIndexCount()];
		triangles.getIndices(triIndices);

		VertexArray col = vertices.getColors();
		Texture2D tex = appearance.getTexture(0);
		Image2D teximg = tex == null ? null : tex.getImage();
		VertexArray texVertRaw = vertices.getTexCoords(0, scaleBias);

		float texScale = scaleBias[0];
		float texBiasS = scaleBias[1];
		float texBiasT = scaleBias[2];
		float texBiasR = scaleBias[3];

		Transform texcomptr = new Transform();
		if (tex != null)
			tex.getCompositeTransform(texcomptr);

		Transform projection = new Transform();
		this.currCam.getProjection(projection);

		Transform tr = new Transform();
		Transform textr = new Transform();

		// Scale and translate mesh
		tr.preScale(scale, scale, scale);
		tr.preTranslate(biasX, biasY, biasZ);
		// -> Local space

		// Transform mesh from local coords to world coords
		tr.preMultiplyTry(transform);
		// -> World space

		// Apply the inverse of the camera's transform to the mesh
		tr.preMultiplyTry(this.currCamTransInv);
		// -> View space

		// Apply projection matrix
		tr.preMultiply(projection);
		// -> Clip space

		// Scale and translate texture coordinates
		textr.preScale(texScale, texScale, texScale);
		textr.preTranslate(texBiasS, texBiasT, texBiasR);
		textr.preMultiply(texcomptr);

		// Do the transformation
		float[] vertClip = new float[4 * vertCount];
		tr.transform(vertRaw, vertClip, true);

		float[] texVert = new float[4 * vertCount];
		if (texVertRaw != null)
			textr.transform(texVertRaw, texVert, true);

		// Create Triangle objects for clipping
		Triangle[] trisClip = Triangle.fromVertAndTris(vertClip, texVert, triIndices);

		// Clip triangles
		Triangle[] trisScreen = Arrays.stream(trisClip)
				.flatMap(t -> t.clip())
				.toArray(Triangle[]::new);
		// At this point the triangles in `trisScreen` are actually
		// in Normalized Device Coordinates, but they will be tranformed
		// to Screen space in-place, hence the name.


		// Reset transform
		tr.setIdentity();
		textr.setIdentity();

		// Fit to viewport
		tr.preScale(1, -1, 1);
		tr.preTranslate(1, 1, 0);
		tr.preScale((float) vieww / 2f, (float) viewh / 2f, 1f);
		if (teximg != null)
			textr.preScale(teximg.getWidth(), teximg.getHeight(), 1);
		// -> Screen space

		// Perform viewport transform
		Triangle.transform(trisScreen, tr, textr);

		if (this.target instanceof Image2D)
		{
			Image2D i2d = (Image2D) this.target;
			// TODO support rendering to Image2D
		}
		else if (this.target instanceof PlatformGraphics)
		{
			PlatformGraphics pgrp = (PlatformGraphics) this.target;
			BufferedImage img = pgrp.getCanvas();
			Graphics2D grp = pgrp.getGraphics2D();
			WritableRaster ras = img.getRaster();

			Color colorOrig = grp.getColor();
			Color colorFill = new Color(0, 0, 224, 32);
			Color colorDraw = new Color(0, 0, 0, 128);

			for (int tri_id = 0; tri_id < trisScreen.length; tri_id++)
			{
				Triangle tri = trisScreen[tri_id];

				if (tex == null || texVertRaw == null)
				{
					int[] coXr = new int[] {
						Math.round(tri.xA()),
						Math.round(tri.xB()),
						Math.round(tri.xC())
					};
					int[] coYr = new int[] {
						Math.round(tri.yA()),
						Math.round(tri.yB()),
						Math.round(tri.yC())
					};
					// grp.setColor(colorFill);
					// grp.fillPolygon(coX, coY, 3);
					grp.setColor(colorDraw);
					grp.drawPolygon(coXr, coYr, 3);

					continue;
				}

				Integer[] ordX = new Integer[] { 0, 1, 2 };
				Integer[] ordY = new Integer[] { 0, 1, 2 };

				Arrays.sort(ordX, (Integer a, Integer b) ->
					((int) Math.signum(tri.v[4*a + 0] - tri.v[4*b + 0])));

				Arrays.sort(ordY, (Integer a, Integer b) ->
					((int) Math.signum(tri.v[4*a + 1] - tri.v[4*b + 1])));

				float[] coX = new float[] { tri.xA(), tri.xB(), tri.xC() };
				float[] coY = new float[] { tri.yA(), tri.yB(), tri.yC() };
				float[] coZ = new float[] { tri.zA(), tri.zB(), tri.zC() };
				float[] coS = new float[] { tri.sA(), tri.sB(), tri.sC() };
				float[] coT = new float[] { tri.tA(), tri.tB(), tri.tC() };

				// beginning of texture unit loop
				// for (int tex_id = 0; tex_id < NUM_TEXTURE_UNITS; tex_id++)
				// {

				float drawX, drawY, rHorizon,
					yTop, yMid,         yBot,
					xTop, xMidL, xMidR, xBot,
					zTop, zMidL, zMidR, zBot,
					sTop, sMidL, sMidR, sBot,
					tTop, tMidL, tMidR, tBot,
					xL, xR, zL, zR, sL, sR, tL, tR, z, s, t;

				xTop  = coX[ordY[0]];
				xMidL = coX[ordY[1]];
				xBot  = coX[ordY[2]];
				yTop  = coY[ordY[0]];
				yMid  = coY[ordY[1]];
				yBot  = coY[ordY[2]];
				zTop  = coZ[ordY[0]];
				zMidL = coZ[ordY[1]];
				zBot  = coZ[ordY[2]];

				sTop  = coS[ordY[0]];
				sMidL = coS[ordY[1]];
				sBot  = coS[ordY[2]];
				tTop  = coT[ordY[0]];
				tMidL = coT[ordY[1]];
				tBot  = coT[ordY[2]];

				rHorizon = (yMid - yTop) / (yBot - yTop);

				xMidR = xTop + rHorizon * (xBot - xTop);
				zMidR = zTop + rHorizon * (zBot - zTop);
				sMidR = sTop + rHorizon * (sBot - sTop);
				tMidR = tTop + rHorizon * (tBot - tTop);

				if (xMidL > xMidR)
				{
					float temp;
					temp = xMidL; xMidL = xMidR; xMidR = temp;
					temp = zMidL; zMidL = zMidR; zMidR = temp;
					temp = sMidL; sMidL = sMidR; sMidR = temp;
					temp = tMidL; tMidL = tMidR; tMidR = temp;
				}

				// Draw upper "half" of the triangle
				for (int y = Math.round(yTop); y < Math.round(yMid); y++)
				{
					drawY = (y - yTop) / (yMid - yTop);
					drawY = Math.max(0f, Math.min(drawY, 1f));
					xL = xTop + drawY * (xMidL - xTop);
					xR = xTop + drawY * (xMidR - xTop);
					zL = zTop + drawY * (zMidL - zTop);
					zR = zTop + drawY * (zMidR - zTop);
					sL = sTop + drawY * (sMidL - sTop);
					sR = sTop + drawY * (sMidR - sTop);
					tL = tTop + drawY * (tMidL - tTop);
					tR = tTop + drawY * (tMidR - tTop);
					for (int x = Math.round(xL); x < Math.round(xR); x++)
					{
						try {
							drawX = (x - xL) / (xR - xL);
							drawX = Math.max(0f, Math.min(drawX, 1f));
							z = zL + drawX * (zR - zL);
							if (this.depthBuffer[this.vieww * y + x] < z)
								continue;
							else
								this.depthBuffer[this.vieww * y + x] = z;

							s = sL + drawX * (sR - sL);
							t = tL + drawX * (tR - tL);
							ras.setPixel(x, y, teximg.getPixelArr(
								Math.round(s), Math.round(t)
							));
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
				}

				// Draw lower "half" of the triangle
				for (int y = Math.round(yMid); y < Math.round(yBot); y++)
				{
					drawY = 1f - (y - yMid) / (yBot - yMid);
					drawY = Math.max(0f, Math.min(drawY, 1f));
					xL = xBot + drawY * (xMidL - xBot);
					xR = xBot + drawY * (xMidR - xBot);
					zL = zBot + drawY * (zMidL - zBot);
					zR = zBot + drawY * (zMidR - zBot);
					sL = sBot + drawY * (sMidL - sBot);
					sR = sBot + drawY * (sMidR - sBot);
					tL = tBot + drawY * (tMidL - tBot);
					tR = tBot + drawY * (tMidR - tBot);
					for (int x = Math.round(xL); x < Math.round(xR); x++)
					{
						try {
							drawX = (x - xL) / (xR - xL);
							drawX = Math.max(0f, Math.min(drawX, 1f));
							z = zL + drawX * (zR - zL);
							if (this.depthBuffer[this.vieww * y + x] < z)
								continue;
							else
								this.depthBuffer[this.vieww * y + x] = z;

							s = sL + drawX * (sR - sL);
							t = tL + drawX * (tR - tL);
							ras.setPixel(x, y, teximg.getPixelArr(
								Math.round(s), Math.round(t)
							));
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
				}

				// }
				// end of texture unit loop

			}

			grp.setColor(colorOrig);
		}
	}

	public void render(World world)
	{
		if (world == null)
			throw new java.lang.NullPointerException();
		if (this.target == null)
			throw new java.lang.IllegalStateException();

		// if `world` has no active camera, or
		//    the active camera is not in that `world`
		//    throw new java.lang.IllegalStateException();

		// if the bg-img of `world` is not the same format as `this.target`:
		//    throw new java.lang.IllegalStateException();

		// if any Mesh that is rendered violates the constraints defined in
		//    Mesh, MorphingMesh, SkinnedMesh, VertexBuffer, or IndexBuffer
		//    throw new java.lang.IllegalStateException();

		// if the Transform from the active camera of `world`
		//    to the world space is uninvertible
		//    throw new java.lang.ArithmeticException();
		// Note: this will be thrown by Transform.invert() if appropriate

		System.out.println("Graphics3D.render W");
		// TODO implement Graphics3D.render(World)
	}

	public void resetLights()
	{
		this.currLights.clear();
		this.currLightTrans.clear();
	}

	public void setCamera(Camera camera, Transform transform)
	{
		this.currCam = camera;
		if (transform == null)
			this.currCamTrans = null;
		else
			this.currCamTrans = new Transform(transform);
			this.currCamTransInv = new Transform(transform);
			this.currCamTransInv.invert();
	}

	public void setDepthRange(float near, float far)
	{
		if (near < 0 || 1 < near || far < 0 || 1 < far)
			throw new java.lang.IllegalArgumentException();

		this.near = near;
		this.far = far;
	}

	public void setLight(int index, Light light, Transform transform)
	{
		if (index < 0 ||
			index > this.currLights.size())
			throw new java.lang.IndexOutOfBoundsException();

		if (transform == null)
			transform = new Transform();

		// Indices are NOT supposed to change here,
		// so we're simply updating the arrays at the index,
		// even if any new value is null.
		this.currLights.set(index, light);
		this.currLightTrans.set(index, transform);
	}

	public void setViewport(int x, int y, int width, int height)
	{
		if (width <= 0 || height <= 0 ||
			width > MAX_VIEWPORT_WIDTH ||
			height > MAX_VIEWPORT_HEIGHT)
			throw new java.lang.IllegalArgumentException();

		this.viewx = x;
		this.viewy = y;
		this.vieww = width;
		this.viewh = height;
	}

}
