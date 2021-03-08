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

		VertexArray posRaw = vertices.getPositions(scaleBias);
		int posRawCount = posRaw.getVertexCount();

		float scale = scaleBias[0];
		float biasX = scaleBias[1];
		float biasY = scaleBias[2];
		float biasZ = scaleBias[3];

		Transform projection = new Transform();
		this.currCam.getProjection(projection);

		Transform tr = new Transform();

		// Scale and translate mesh
		tr.preScale(scale, scale, scale);
		tr.preTranslate(biasX, biasY, biasZ);

		// Transform mesh from local coords to world coords
		tr.preMultiplyTry(transform);

		// "Set up" camera in the world
		tr.preMultiplyTry(this.currCamTransInv);

		// Project to 2D
		tr.preMultiply(projection);

		// Fit to viewport
		tr.preScale(1, -1, 1);
		tr.preTranslate(1, 1, 0);
		tr.preScale((float) vieww / 2f, (float) viewh / 2f, 1f);

		// Do the transformation
		float[] pos = new float[4 * posRawCount];
		tr.transform(posRaw, pos, true);

		// Get list of triangles
		int[] tris = new int[triangles.getIndexCount()];
		triangles.getIndices(tris);

		// `pos` contains 4 coordinates (XYZW) for each vertex:
		//   x0 y0 z0 w0   x1 y1 z1 w1   ...
		//
		// `tris` contains 3 indices (ABC) for each triangle:
		//   a0 b0 c0   a1 b1 c1   ...
		//
		// Each entry in `tris` is an index,
		// which refers to an entry in `pos`,
		// which contains the coordinates for one corner of one triangle.
		//
		// Coordinate R of corner L of triangle T:
		//     = pos[4*tris[3*T + L] + R],
		//   where R, L, T are 0-based indices
		//
		// For example, the 6th triangle's 2nd corner's Z (3rd) coordinate:
		//     = pos[4 * tris[3 * (6-1) + (2-1)] + (3-1)]
		//     = pos[4 * tris[3 *   5   +   1  ] +   2  ]
		//     = pos[4 * tris[16] + 2]

		if (this.target instanceof Image2D)
		{
			Image2D i2d = (Image2D) this.target;
			// TODO support rendering to Image2D
		}
		else if (this.target instanceof PlatformGraphics)
		{
			Graphics2D grp = ((PlatformGraphics) this.target).getGraphics2D();

			Color colorOrig = grp.getColor();
			Color colorFill = new Color(0, 32, 224, 16);
			Color colorDraw = new Color(224, 0, 0, 255);

			for (int tri_id = 0; tri_id < tris.length; tri_id += 3)
			{
				int cornerA_id, cornerB_id, cornerC_id;
				cornerA_id = 4 * tris[tri_id + 0];
				cornerB_id = 4 * tris[tri_id + 1];
				cornerC_id = 4 * tris[tri_id + 2];

				float wA, wB, wC;
				wA = pos[cornerA_id + 3];
				wB = pos[cornerB_id + 3];
				wC = pos[cornerC_id + 3];

				if (Math.abs(wA) < 1e-5 ||
					Math.abs(wB) < 1e-5 ||
					Math.abs(wC) < 1e-5) continue;

				int[] coordsX = new int[] {
					Math.round(pos[cornerA_id + 0] / wA),
					Math.round(pos[cornerB_id + 0] / wB),
					Math.round(pos[cornerC_id + 0] / wC)
				};
				int[] coordsY = new int[] {
					Math.round(pos[cornerA_id + 1] / wA),
					Math.round(pos[cornerB_id + 1] / wB),
					Math.round(pos[cornerC_id + 1] / wC)
				};
				float cornerA_z = pos[cornerA_id + 2] / wA;
				float cornerB_z = pos[cornerB_id + 2] / wB;
				float cornerC_z = pos[cornerC_id + 2] / wC;

				grp.setColor(colorFill);
				grp.fillPolygon(coordsX, coordsY, 3);
				grp.setColor(colorDraw);
				grp.drawPolygon(coordsX, coordsY, 3);
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
