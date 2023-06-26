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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.PushbackInputStream;

import org.recompile.mobile.Mobile;

import java.util.zip.Checksum;
import java.util.zip.Inflater;

import javax.microedition.lcdui.Image;

public class Loader
{

	private static final byte[] IDENTIFIER = {(byte) 0xAB, 0x4A, 0x53, 0x52, 0x31, 0x38, 0x34, (byte) 0xBB, 0x0D,
		0x0A, 0x1A, 0x0A};

	private static DataInputStream dis;
	private static java.util.Vector objs;

	public static Object3D[] load(byte[] data, int offset) {
		DataInputStream old = dis;
		dis = new DataInputStream(new ByteArrayInputStream(data));

		try {
			while (dis.available() > 0) {
				int objectType = readByte();
				int length = readInt();

				//System.out.println("objectType: " + objectType);
				//System.out.println("length: " + length);

				dis.mark(Integer.MAX_VALUE);

				if (objectType == 0) {
					int versionHigh = readByte();
					int versionLow = readByte();
					boolean hasExternalReferences = readBoolean();
					int totolFileSize = readInt();
					int approximateContentSize = readInt();
					String authoringField = readString();

					objs.addElement(new Group()); // dummy
				} else if (objectType == 255) {
					// TODO: load external resource
					System.out.println("Loader: Loading external resources not implemented.");
					String uri = readString();
				} else if (objectType == 1) {
					AnimationController cont = new AnimationController();
					loadObject3D(cont);
					float speed = readFloat();
					float weight = readFloat();
					int start = readInt();
					int end = readInt();
					cont.setActiveInterval(start, end);
					float referenceSeqTime = readFloat();
					int referenceWorldTime = readInt();
					cont.setPosition(referenceSeqTime, referenceWorldTime);
					cont.setSpeed(speed, referenceWorldTime);
					cont.setWeight(weight);
					objs.addElement(cont);
				} else if (objectType == 2) {
					loadObject3D(new Group());
					KeyframeSequence ks = (KeyframeSequence) getObject(readInt());
					AnimationController cont = (AnimationController) getObject(readInt());
					int property = readInt();
					AnimationTrack track = new AnimationTrack(ks, property);
					track.setController(cont);
					dis.reset();
					loadObject3D(track);
					objs.addElement(track);
				} else if (objectType == 3) {
					Appearance appearance = new Appearance();
					loadObject3D(appearance);
					appearance.setLayer(readByte());
					appearance.setCompositingMode((CompositingMode) getObject(readInt()));
					appearance.setFog((Fog) getObject(readInt()));
					appearance.setPolygonMode((PolygonMode) getObject(readInt()));
					appearance.setMaterial((Material) getObject(readInt()));
					int numTextures = readInt();
					for (int i = 0; i < numTextures; ++i)
						appearance.setTexture(i, (Texture2D) getObject(readInt()));
					objs.addElement(appearance);
				} else if (objectType == 4) {
					Background background = new Background();
					loadObject3D(background);
					background.setColor(readRGBA());
					background.setImage((Image2D) getObject(readInt()));
					int modeX = readByte();
					int modeY = readByte();
					background.setImageMode(modeX, modeY);
					int cropX = readInt();
					int cropY = readInt();
					int cropWidth = readInt();
					int cropHeight = readInt();
					background.setCrop(cropX, cropY, cropWidth, cropHeight);
					background.setDepthClearEnable(readBoolean());
					background.setColorClearEnable(readBoolean());
					objs.addElement(background); // dummy
				} else if (objectType == 5) {
					Camera camera = new Camera();
					loadNode(camera);

					int projectionType = readByte();
					if (projectionType == Camera.GENERIC) {
						Transform t = new Transform();
						t.set(readMatrix());
						camera.setGeneric(t);
					} else {
						float fovy = readFloat();
						float aspect = readFloat();
						float near = readFloat();
						float far = readFloat();
						if (projectionType == Camera.PARALLEL)
							camera.setParallel(fovy, aspect, near, far);
						else
							camera.setPerspective(fovy, aspect, near, far);
					}
					objs.addElement(camera);
				} else if (objectType == 6) {
					CompositingMode compositingMode = new CompositingMode();
					loadObject3D(compositingMode);
					compositingMode.setDepthTestEnable(readBoolean());
					compositingMode.setDepthWriteEnable(readBoolean());
					compositingMode.setColorWriteEnable(readBoolean());
					compositingMode.setAlphaWriteEnable(readBoolean());
					compositingMode.setBlending(readByte());
					compositingMode.setAlphaThreshold((float) readByte() / 255.0f);

					float factor = readFloat();
					float units = readFloat();
					compositingMode.setDepthOffset(factor, units);
					objs.addElement(compositingMode);
				} else if (objectType == 7) {
					Fog fog = new Fog();
					loadObject3D(fog);
					fog.setColor(readRGB());
					fog.setMode(readByte());
					if (fog.getMode() == Fog.EXPONENTIAL)
						fog.setDensity(readFloat());
					else {
						float near = readFloat();
						float far = readFloat();
						fog.setLinear(near, far);
					}
					objs.addElement(fog);
				} else if (objectType == 9) {
					Group group = new Group();
					loadGroup(group);
					objs.addElement(group);
				} else if (objectType == 10) {
					Image2D image = null;
					loadObject3D(new Group()); // dummy
					int format = readByte();
					boolean isMutable = readBoolean();
					int width = readInt();
					int height = readInt();
					if (!isMutable) {
						int paletteSize = readInt();
						byte[] palette = null;
						if (paletteSize > 0) {
							palette = new byte[paletteSize];
							dis.readFully(palette);
						}

						int pixelSize = readInt();
						byte[] pixel = new byte[pixelSize];
						dis.readFully(pixel);
						if (palette != null)
							image = new Image2D(format, width, height, pixel, palette);
						else
							image = new Image2D(format, width, height, pixel);
					} else
						image = new Image2D(format, width, height);

					dis.reset();
					loadObject3D(image);

					objs.addElement(image);
				} else if (objectType == 19) {
					loadObject3D(new Group());
					int interpolation = readByte();
					int repeatMode = readByte();
					int encoding = readByte();
					int duration = readInt();
					int rangeFirst = readInt();
					int rangeLast = readInt();
					int components = readInt();
					int keyFrames = readInt();
					int size = (encoding == 0) ? (keyFrames * (4 + components * 4)) : (components * 8 + keyFrames * (4 + components * (encoding == 1 ? 1 : 2)));

					KeyframeSequence seq = new KeyframeSequence(keyFrames, components, interpolation);
					seq.setRepeatMode(repeatMode);
					seq.setDuration(duration);
					seq.setValidRange(rangeFirst, rangeLast);
					float[] values = new float[components];
					if (encoding == 0) {
						for (int i = 0; i < keyFrames; i++) {
							int time = readInt();

							for (int j = 0; j < components; j++) {
								values[j] = readFloat();
							}

							seq.setKeyframe(i, time, values);
						}
					} else {
						float[] vectorBiasScale = new float[components * 2];
						for (int i = 0; i < components; i++) {
							vectorBiasScale[i] = readFloat();
						}

						for (int i = 0; i < components; i++) {
							vectorBiasScale[i + components] = readFloat();
						}

						for (int i = 0; i < keyFrames; i++) {
							int time = readInt();
							if (encoding == 1) {
								for (int j = 0; j < components; j++) {
									int v = readByte();
									values[j] = vectorBiasScale[j] + ((vectorBiasScale[j + components] * v) / 255.0f);
								}
							} else {
								for (int j = 0; j < components; j++) {
									int v = readShort();
									values[j] = vectorBiasScale[j] + ((vectorBiasScale[j + components] * v) / 65535.0f);
								}
							}
							seq.setKeyframe(i, time, values);
						}
					}
					objs.addElement(seq);
				} else if (objectType == 12) {
					Light light = new Light();
					loadNode(light);
					float constant = readFloat();
					float linear = readFloat();
					float quadratic = readFloat();
					light.setAttenuation(constant, linear, quadratic);
					light.setColor(readRGB());
					light.setMode(readByte());
					light.setIntensity(readFloat());
					light.setSpotAngle(readFloat());
					light.setSpotExponent(readFloat());
					objs.addElement(light);
				} else if (objectType == 13) {
					Material material = new Material();
					loadObject3D(material);
					material.setColor(Material.AMBIENT, readRGB());
					material.setColor(Material.DIFFUSE, readRGBA());
					material.setColor(Material.EMISSIVE, readRGB());
					material.setColor(Material.SPECULAR, readRGB());
					material.setShininess(readFloat());
					material.setVertexColorTrackingEnable(readBoolean());
					objs.addElement(material);
				} else if (objectType == 14) {
					loadNode(new Group()); // dummy

					VertexBuffer vertices = (VertexBuffer) getObject(readInt());
					int submeshCount = readInt();

					IndexBuffer[] submeshes = new IndexBuffer[submeshCount];
					Appearance[] appearances = new Appearance[submeshCount];
					for (int i = 0; i < submeshCount; ++i) {
						submeshes[i] = (IndexBuffer) getObject(readInt());
						appearances[i] = (Appearance) getObject(readInt());
					}
					Mesh mesh = new Mesh(vertices, submeshes, appearances);

					dis.reset();
					loadNode(mesh);

					objs.addElement(mesh);
				} else if (objectType == 15) {
					loadNode(new Group());
					VertexBuffer vb = (VertexBuffer) getObject(readInt());
					int subMeshCount = readInt();
					IndexBuffer[] ib = new IndexBuffer[subMeshCount];
					Appearance[] ap = new Appearance[subMeshCount];

					for (int i = 0; i < subMeshCount; i++) {
						ib[i] = (IndexBuffer) getObject(readInt());
						ap[i] = (Appearance) getObject(readInt());
					}

					int targetCount = readInt();
					float[] weights = new float[targetCount];
					VertexBuffer[] targets = new VertexBuffer[targetCount];

					for (int i = 0; i < targetCount; i++) {
						targets[i] = (VertexBuffer) getObject(readInt());
						weights[i] = readFloat();
					}

					MorphingMesh mesh = new MorphingMesh(vb, targets, ib, ap);
					dis.reset();
					loadNode(mesh);

					objs.addElement(mesh);
				} else if (objectType == 8) {
					PolygonMode polygonMode = new PolygonMode();
					loadObject3D(polygonMode);
					polygonMode.setCulling(readByte());
					polygonMode.setShading(readByte());
					polygonMode.setWinding(readByte());
					polygonMode.setTwoSidedLightingEnable(readBoolean());
					polygonMode.setLocalCameraLightingEnable(readBoolean());
					polygonMode.setPerspectiveCorrectionEnable(readBoolean());
					objs.addElement(polygonMode);
				} else if (objectType == 16) {
					loadNode(new Group());
					VertexBuffer vb = (VertexBuffer) getObject(readInt());
					int subMeshCount = readInt();
					IndexBuffer[] ib = new IndexBuffer[subMeshCount];
					Appearance[] ap = new Appearance[subMeshCount];

					for (int i = 0; i < subMeshCount; i++) {
						ib[i] = (IndexBuffer) getObject(readInt());
						ap[i] = (Appearance) getObject(readInt());
					}

					Group skeleton = (Group) getObject(readInt());

					SkinnedMesh mesh = new SkinnedMesh(vb, ib, ap, skeleton);
					int transformReferenceCount = readInt();

					for (int i = 0; i < transformReferenceCount; i++) {
						Node bone = (Node) getObject(readInt());
						int firstVertex = readInt();
						int vertexCount = readInt();
						int weight = readInt();
						mesh.addTransform(bone, weight, firstVertex, vertexCount);
					}

					dis.reset();
					loadNode(mesh);
					objs.addElement(mesh);
				} else if (objectType == 18) {
					loadNode(new Group());
					Image2D image = (Image2D) getObject(readInt());
					Appearance ap = (Appearance) getObject(readInt());
					Sprite3D sprite = new Sprite3D(readBoolean(), image, ap);
					int x = readInt();
					int y = readInt();
					int width = readInt();
					int height = readInt();
					sprite.setCrop(x, y, width, height);
					dis.reset();
					loadNode(sprite);
					objs.addElement(sprite);
				} else if (objectType == 17) {
					loadTransformable(new Group()); // dummy
					Texture2D texture = new Texture2D((Image2D) getObject(readInt()));
					texture.setBlendColor(readRGB());
					texture.setBlending(readByte());
					int wrapS = readByte();
					int wrapT = readByte();
					texture.setWrapping(wrapS, wrapT);
					int levelFilter = readByte();
					int imageFilter = readByte();
					texture.setFiltering(levelFilter, imageFilter);

					dis.reset();
					loadTransformable(texture);

					objs.addElement(texture);
				} else if (objectType == 11) {
					loadObject3D(new Group()); // dummy

					int encoding = readByte();
					int firstIndex = 0;
					int[] indices = null;
					if (encoding == 0)
						firstIndex = readInt();
					else if (encoding == 1)
						firstIndex = readByte();
					else if (encoding == 2)
						firstIndex = readShort();
					else if (encoding == 128) {
						int numIndices = readInt();
						indices = new int[numIndices];
						for (int i = 0; i < numIndices; ++i)
							indices[i] = readInt();
					} else if (encoding == 129) {
						int numIndices = readInt();
						indices = new int[numIndices];
						for (int i = 0; i < numIndices; ++i)
							indices[i] = readByte();
					} else if (encoding == 130) {
						int numIndices = readInt();
						indices = new int[numIndices];
						for (int i = 0; i < numIndices; ++i)
							indices[i] = readShort();
					}

					int numStripLengths = readInt();
					int[] stripLengths = new int[numStripLengths];
					for (int i = 0; i < numStripLengths; i++)
						stripLengths[i] = readInt();

					dis.reset();

					TriangleStripArray triStrip = null;
					if (indices == null)
						triStrip = new TriangleStripArray(firstIndex, stripLengths);
					else
						triStrip = new TriangleStripArray(indices, stripLengths);

					loadObject3D(triStrip);

					objs.addElement(triStrip);
				} else if (objectType == 20) {
					loadObject3D(new Group()); // dummy

					int componentSize = readByte();
					int componentCount = readByte();
					int encoding = readByte();
					int vertexCount = readShort();

					VertexArray vertices = new VertexArray(vertexCount, componentCount, componentSize);

					if (componentSize == 1) {
						byte[] values = new byte[componentCount * vertexCount];
						if (encoding == 0)
							dis.readFully(values);
						else {
							byte last = 0;
							for (int i = 0; i < vertexCount * componentCount; ++i) {
								last += readByte();
								values[i] = last;
							}
						}
						vertices.set(0, vertexCount, values);
					} else {
						short last = 0;
						short[] values = new short[componentCount * vertexCount];
						for (int i = 0; i < componentCount * vertexCount; ++i) {
							if (encoding == 0)
								values[i] = (short) readShort();
							else {
								last += (short) readShort();
								values[i] = last;
							}
						}
						vertices.set(0, vertexCount, values);
					}

					dis.reset();
					loadObject3D(vertices);

					objs.addElement(vertices);
				} else if (objectType == 21) {
					VertexBuffer vertices = new VertexBuffer();
					loadObject3D(vertices);

					vertices.setDefaultColor(readRGBA());

					VertexArray positions = (VertexArray) getObject(readInt());
					float[] bias = new float[3];
					bias[0] = readFloat();
					bias[1] = readFloat();
					bias[2] = readFloat();
					float scale = readFloat();
					vertices.setPositions(positions, scale, bias);

					vertices.setNormals((VertexArray) getObject(readInt()));
					vertices.setColors((VertexArray) getObject(readInt()));

					int texCoordArrayCount = readInt();
					for (int i = 0; i < texCoordArrayCount; ++i) {
						VertexArray texcoords = (VertexArray) getObject(readInt());
						bias[0] = readFloat();
						bias[1] = readFloat();
						bias[2] = readFloat();
						scale = readFloat();
						vertices.setTexCoords(i, texcoords, scale, bias);
					}

					objs.addElement(vertices);
				} else if (objectType == 22) {
					World world = new World();
					loadGroup(world);

					world.setActiveCamera((Camera) getObject(readInt()));
					world.setBackground((Background) getObject(readInt()));
					objs.addElement(world);
				} else if (objectType == 171) {
					for (int sk = 0; sk < 7; sk++)
						readByte();
					Object3D[] ret = loadM3G(dis);
					dis = old;
					return ret;
				} else {
					System.out.println("Loader: unsupported objectType " + objectType + ".");
				}

				dis.reset();
				dis.skipBytes(length);
			}
		} catch (Exception e) {
			System.out.println("Exception: " + e.getMessage());
			e.printStackTrace();
		}

		dis = old;
		Object3D[] obj = new Object3D[objs.size()];
		for (int i = 0; i < objs.size(); i++) {
			obj[i] = (Object3D) objs.elementAt(i);
		}
		return obj;
	}

	public static Object3D[] load(java.lang.String name) 
	{ 
		InputStream is;
		if (name.startsWith("/")) {
			is = Mobile.getResourceAsStream(Loader.class, name);
			
			if(is == null) 
			{
				System.out.println("Can't load M3G resource " + name);
				return null;
			}
		} else {
			// TODO
			is = null; /* This is here just to silence an error about PushbackInputStream below */
			return null;
		}

		PushbackInputStream pis = new PushbackInputStream(is, 12);

		byte[] identifier = new byte[12];
		int read;
		try { read = pis.read(identifier, 0, 12); }
		catch (IOException e) {System.out.println(e.getMessage());}
		boolean isM3GFile = true;
		for (int i = 0; i < 12; i++) {
			if (identifier[i] != IDENTIFIER[i]) {
				isM3GFile = false;
			}
		}

		if (isM3GFile) 
		{ 
			try { return loadM3G(is); }
			catch (IOException e) {System.out.println(e.getMessage()); }
		} 
		else 
		{
			try 
			{
				pis.unread(identifier);
				Object image = Image.createImage(pis);
				Image2D image2D = new Image2D(Image2D.RGB, image);
				return new Object3D[]{image2D};
			}
			catch (IOException e) {System.out.println(e.getMessage());}
		}

		return null;
	}

	private static Object3D[] loadM3G(InputStream is) throws IOException
	{

		objs = new java.util.Vector();
		dis = new DataInputStream(is);

		while (dis.available() > 0) 
		{
			int compressionScheme = readByte();
			int totalSectionLength = readInt();
			int uncompressedLength = readInt();

			byte[] uncompressedData = new byte[uncompressedLength];

			if (compressionScheme == 0) { dis.readFully(uncompressedData); } 
			else if (compressionScheme == 1) 
			{
				int compressedLength = totalSectionLength - 13;
				byte[] compressedData = new byte[compressedLength];
				dis.readFully(compressedData);

				Inflater decompresser = new Inflater();
				decompresser.setInput(compressedData, 0, compressedLength);
				int resultLength = 0;
				try { resultLength = decompresser.inflate(uncompressedData); } 
				catch (Exception e) { e.printStackTrace(); }
				decompresser.end();

				if (resultLength != uncompressedLength) { throw new IOException("Unable to decompress data."); }
			} 
			else { throw new IOException("Unknown compression scheme."); }

			int checkSum = dis.readInt();

			load(uncompressedData, 0);
		}

		Object3D[] obj = new Object3D[objs.size()];
		for (int i = 0; i < objs.size(); i++)
			obj[i] = (Object3D) objs.elementAt(i);
		return obj;
	}

	private static int readByte() throws IOException {
		return dis.readUnsignedByte();
	}

	private static int readShort() throws IOException {
		int a = readByte();
		int b = readByte();
		return (b << 8) + a;
	}

	private static int readRGB() throws IOException {
		byte r = dis.readByte();
		byte g = dis.readByte();
		byte b = dis.readByte();
		return (r << 16) + (g << 8) + b;
	}

	private static int readRGBA() throws IOException {
		byte r = dis.readByte();
		byte g = dis.readByte();
		byte b = dis.readByte();
		byte a = dis.readByte();
		return (a << 24) + (r << 16) + (g << 8) + b;
	}

	private static float readFloat() throws IOException {
		return Float.intBitsToFloat(readInt());
	}

	private static int readInt() throws IOException {
		int a = dis.readUnsignedByte();
		int b = dis.readUnsignedByte();
		int c = dis.readUnsignedByte();
		int d = dis.readUnsignedByte();
		int i = (d << 24) | (c << 16) | (b << 8) | a;
		return i;
	}

	private static boolean readBoolean() throws IOException {
		return readByte() == 1;
	}

	private static String readString() throws IOException {
		// TODO
		return "";
	}

	private static float[] readMatrix() throws IOException {
		float[] m = new float[16];
		for (int i = 0; i < 16; ++i)
			m[i] = readFloat();
		return m;
	}

	private static Object getObject(int index) {
		if (index == 0) { return null; }
		return objs.elementAt(index - 1);
	}

	private static void loadObject3D(Object3D object) throws IOException {
		object.setUserID(readInt());

		int animationTracks = readInt();
		for (int i = 0; i < animationTracks; ++i)
			readInt();//object.addAnimationTrack((AnimationTrack)getObject(readInt()));

		int userParameterCount = readInt();
		for (int i = 0; i < userParameterCount; ++i) {
			int parameterID = readInt();
			int numBytes = readInt();
			System.out.println("Object3D parameterID:" + parameterID);
			byte[] parameterBytes = new byte[numBytes];
			dis.readFully(parameterBytes);
		}
	}

	private static void loadTransformable(Transformable transformable) throws IOException 
	{
		loadObject3D(transformable);
		if (readBoolean()) // hasComponentTransform
		{
			float tx = readFloat();
			float ty = readFloat();
			float tz = readFloat();
			transformable.setTranslation(tx, ty, tz);
			float sx = readFloat();
			float sy = readFloat();
			float sz = readFloat();
			transformable.setScale(sx, sy, sz);
			float angle = readFloat();
			float ax = readFloat();
			float ay = readFloat();
			float az = readFloat();
			transformable.setOrientation(angle, ax, ay, az);
		}
		if (readBoolean()) // hasGeneralTransform
		{
			Transform t = new Transform();
			t.set(readMatrix());
			transformable.setTransform(t);
		}
	}

	private static void loadNode(Node node) throws IOException 
	{
		loadTransformable(node);
		node.setRenderingEnable(readBoolean());
		node.setPickingEnable(readBoolean());
		int alpha = readByte();
		node.setAlphaFactor((float) alpha / 255.0f);
		node.setScope(readInt());
		if (readBoolean()) // hasAlignment
		{
			int zTarget = readByte();
			int yTarget = readByte();
			Node zReference = (Node) getObject(readInt());
			Node yReference = (Node) getObject(readInt());
			node.setAlignment(zReference, zTarget, yReference, yTarget);
		}
	}

	private static void loadGroup(Group group) throws IOException 
	{
		loadNode(group);
		int count = readInt();
		for (int i = 0; i < count; ++i) { group.addChild((Node) getObject(readInt())); }
	}

}
