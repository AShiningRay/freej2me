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

public class KeyframeSequence extends Object3D
{

	public static final int CONSTANT = 192;
	public static final int LINEAR = 176;
	public static final int LOOP = 193;
	public static final int SLERP = 177;
	public static final int SPLINE = 178;
	public static final int SQUAD = 179;
	public static final int STEP = 180;


	private int duration;
	private int intType;
	private int keyframe;
	private int keyframes;
	private int repeat;
	private int rangeFirst;
	private int rangeLast;
	private int componentCount;
	private int nextKeyframe;
	private boolean dirty;

	private float[][] keyFrames;
	private float[][] inTangents;
	private float[][] outTangents;
	private int[] keyFrameTimes;
	private QVec4[] a;
	private QVec4[] b;


	public KeyframeSequence(int numKeyframes, int numComponents, int interpolation) 
	{  
		if ((numKeyframes < 1) || (numComponents < 1)) 
		{
			throw new IllegalArgumentException("Number of keyframes/components must be >= 1");
		}

		// Check given interpolation mode
		switch (interpolation) 
		{
			case SLERP:
				if (numComponents != 4)
					throw new IllegalArgumentException("SLERP and SQUAD mode requires 4 components in each keyframe");
				break;
			case SQUAD:
				if (numComponents != 4)
					throw new IllegalArgumentException("SLERP and SQUAD mode requires 4 components in each keyframe");
				a = new QVec4[numKeyframes];
				b = new QVec4[numKeyframes];
				break;
			case STEP:
				break;
			case LINEAR:
				break;
			case SPLINE:
				inTangents = new float[numKeyframes][numComponents];
				outTangents = new float[numKeyframes][numComponents];
				break;
			default:
				throw new IllegalArgumentException("Unknown interpolation mode");
		}

		this.keyframes = numKeyframes;
		this.componentCount = numComponents;
		this.intType = interpolation;

		// Initialize the sequence with default values
		keyFrames = new float[numKeyframes][numComponents];
		keyFrameTimes = new int[numKeyframes];
		rangeFirst = 0;
		rangeLast = numKeyframes - 1;
		dirty = true;
	}


	public int getComponentCount() { return this.componentCount; }

	public float[] keyframeAt(int idx) { return this.keyFrames[idx]; }

	public int previousKeyframeIndex(int ind) 
	{
		if (ind == rangeFirst)
			return rangeLast;
		else if (ind == 0)
			return keyframes - 1;
		else
			return ind - 1;
	}

	public int nextKeyframeIndex(int ind) 
	{
		if (ind == rangeLast)
			return rangeFirst;
		else if (ind == (keyframes - 1))
			return 0;
		else
			return (ind + 1);
	}

	public float[] keyframeBefore(int idx) { return keyframeAt(previousKeyframeIndex(idx)); }

	public float[] keyframeAfter(int idx) { return keyframeAt(nextKeyframeIndex(idx)); }

	public int timeDelta(int ind) 
	{
		if (ind == rangeLast)
			return (duration - keyFrameTimes[rangeLast]) + keyFrameTimes[rangeFirst];

		return keyFrameTimes[nextKeyframeIndex(ind)] - keyFrameTimes[ind];
	}

	public float incomingTangentScale(int ind) 
	{
		if (repeat != LOOP && (ind == rangeFirst || ind == rangeLast))
			return 0;
		else 
		{
			int prevind = previousKeyframeIndex(ind);
			return (((float) timeDelta(prevind) * 2.0f) / ((float) (timeDelta(ind) + timeDelta(prevind))));
		}
	}

	public float outgoingTangentScale(int ind) 
	{
		if (repeat != LOOP && (ind == rangeFirst || ind == rangeLast))
			return 0;
		else 
		{
			int prevind = previousKeyframeIndex(ind);
			return (((float) timeDelta(ind) * 2.0f) / ((float) (timeDelta(ind) + timeDelta(prevind))));
		}
	}

	public float[] tangentTo(int idx) 
	{
		if (inTangents == null)
			throw new NullPointerException("Cannot find tangent to keyframe if tangents are null");
		return inTangents[idx];
	}

	public float[] tangentFrom(int idx) 
	{
		if (outTangents == null)
			throw new NullPointerException("Cannot find tangent from keyframe if tangents are null");
		return outTangents[idx];
	}

	public int getSample(int time, float[] sample) 
	{
		if (dirty) 
		{
			if (intType == SPLINE) 
			{
				int kf = rangeFirst;
				do {
					float[] prev = keyframeBefore(kf);
					float[] next = keyframeAfter(kf);
					float sIn = incomingTangentScale(kf);
					float sOut = outgoingTangentScale(kf);
					float[] in = tangentTo(kf);
					float[] out = tangentFrom(kf);

					for (int i = 0; i < componentCount; i++) {
						in[i] = ((0.5f * ((next[i] - prev[i]))) * sIn);
						out[i] = ((0.5f * ((next[i] - prev[i]))) * sOut);
					}

					kf = nextKeyframeIndex(kf);
				} while (kf != rangeFirst);
			} 
			else if (intType == SQUAD) 
			{
				int kf = rangeFirst;
				QVec4 start = new QVec4();
				QVec4 end = new QVec4();
				QVec4 prev = new QVec4();
				QVec4 next = new QVec4();
				QVec4 tempq = new QVec4();
				Vector3 tempv = new Vector3();
				Vector3 cfd = new Vector3();
				Vector3 tangent = new Vector3();
				do {
					prev.setQuat(keyframeBefore(kf));
					start.setQuat(keyframeAt(kf));
					end.setQuat(keyframeAfter(kf));
					next.setQuat(keyframeAfter(nextKeyframeIndex(kf)));

					cfd.logDiffQuat(start, end);
					tempv.logDiffQuat(prev, start);
					cfd.addVec3(tempv);
					cfd.scaleVec3(0.5f);

					tangent.assign(cfd);
					tangent.scaleVec3(outgoingTangentScale(kf));

					tempv.logDiffQuat(start, end);
					tangent.subVec3(tempv);
					tangent.scaleVec3(0.5f);
					tempq.x = tempv.x;
					tempq.y = tempv.y;
					tempq.z = tempv.z;
					tempq.expQuat(tangent);
					a[kf].assign(start);
					a[kf].mulQuat(tempq);

					tangent.assign(cfd);
					tangent.scaleVec3(incomingTangentScale(kf));

					tempv.x = tempq.x;
					tempv.y = tempq.y;
					tempv.z = tempq.z;
					tempv.logDiffQuat(prev, start);
					tempv.subVec3(tangent);
					tempv.scaleVec3(0.5f);
					tempq.x = tempv.x;
					tempq.y = tempv.y;
					tempq.z = tempv.z;
					tempq.expQuat(tempv);
					b[kf].assign(start);
					b[kf].mulQuat(tempq);

					kf = nextKeyframeIndex(kf);
				} while (kf != rangeFirst);
			}
			dirty = false;
			nextKeyframe = rangeFirst;
		}

		if (repeat == LOOP) 
		{
			if (time < 0)
				time = (time % duration) + duration;
			else
				time = time % duration;

			if (time < keyFrameTimes[rangeFirst])
				time += duration;
		} 
		else 
		{
			if (time < keyFrameTimes[rangeFirst]) 
			{
				float[] value = keyframeAt(rangeFirst);
				for (int i = 0; i < componentCount; i++)
					sample[i] = value[i];
				return (keyFrameTimes[rangeFirst] - time);
			}
			else if (time >= keyFrameTimes[rangeLast]) 
			{
				float[] value = keyframeAt(rangeLast);
				for (int i = 0; i < componentCount; i++)
					sample[i] = value[i];
				return 0x7FFFFFFF;
			}
		}

		int start = nextKeyframe;
		if (keyFrameTimes[start] > time)
			start = rangeFirst;
		while (start != rangeLast && keyFrameTimes[nextKeyframeIndex(start)] <= time)
			start = nextKeyframeIndex(start);
		nextKeyframe = start;

		if (time == keyFrameTimes[start] || intType == STEP) 
		{
			float[] value = keyframeAt(start);
			for (int i = 0; i < componentCount; i++)
				sample[i] = value[i];
			return (intType == STEP) ? (timeDelta(start) - (time - keyFrameTimes[start])) : 1;
		}

		float s = ((time - keyFrameTimes[start]) / (float) timeDelta(start));

		int end = nextKeyframeIndex(start);
		float[] Start;
		float[] End;
		float[] temp;
		float[] tStart;
		float[] tEnd;
		float s2;
		float s3;
		QVec4 q0;
		QVec4 q1;
		QVec4 sampl;
		QVec4 temp0;
		QVec4 temp1;
		QVec4 A;
		QVec4 B;
		switch (intType) 
		{
			case LINEAR:
				Start = keyframeAt(start);
				End = keyframeAt(end);
				Vector3.lerp(componentCount, sample, s, Start, End);
				break;
			case SLERP:
				if (componentCount != 4)
					throw new IllegalStateException();
				q0 = new QVec4();
				q1 = new QVec4();
				sampl = new QVec4();

				q0.setQuat(keyframeAt(start));
				q1.setQuat(keyframeAt(end));
				sampl.setQuat(sample);

				sampl.slerpQuat(s, q0, q1);
				sample[0] = sampl.x;
				sample[1] = sampl.y;
				sample[2] = sampl.z;
				sample[3] = sampl.w;
				// may be not necessary
				temp = keyframeAt(start);
				temp[0] = q0.x;
				temp[1] = q0.y;
				temp[2] = q0.z;
				temp[3] = q0.w;
				temp = keyframeAt(end);
				temp[0] = q1.x;
				temp[1] = q1.y;
				temp[2] = q1.z;
				temp[3] = q1.w;
				break;
			case SPLINE:
				Start = keyframeAt(start);
				End = keyframeAt(end);
				tStart = tangentFrom(start);
				tEnd = tangentTo(end);

				s2 = s * s;
				s3 = s2 * s;

				for (int i = 0; i < componentCount; i++)
					sample[i] = (Start[i] * (((s3 * 2) - (3.f * s2)) + 1.f) + (End[i] * ((3.f * s2) - (s3 * 2)) + (tStart[i] * ((s3 - (s2 * 2)) + s) + (tEnd[i] * (s3 - s2)))));
				break;
			case SQUAD:
				if (componentCount != 4)
					throw new IllegalStateException();
				temp0 = new QVec4();
				temp1 = new QVec4();
				q0 = new QVec4();
				q1 = new QVec4();
				//A = new QVec4();
				//B = new QVec4();
				sampl = new QVec4();

				q0.setQuat(keyframeAt(start));
				q1.setQuat(keyframeAt(end));
				//A.setQuat(a[start]);
				//B.setQuat(b[end]);
				sampl.setQuat(sample);
				temp0.slerpQuat(s, q0, q1);
				temp1.slerpQuat(s, a[start], b[end]);
				sampl.slerpQuat(((s * (1.0f - s)) * 2), temp0, temp1);
				sample[0] = sampl.x;
				sample[1] = sampl.y;
				sample[2] = sampl.z;
				sample[3] = sampl.w;
				// may be not necessary
				temp = keyframeAt(start);
				temp[0] = q0.x;
				temp[1] = q0.y;
				temp[2] = q0.z;
				temp[3] = q0.w;
				temp = keyframeAt(end);
				temp[0] = q1.x;
				temp[1] = q1.y;
				temp[2] = q1.z;
				temp[3] = q1.w;
				/*temp = a[start];
                temp[0] = A.x;
				temp[1] = A.y;
				temp[2] = A.z;
				temp[3] = A.w;
				temp = b[end];
				temp[0] = B.x;
				temp[1] = B.y;
				temp[2] = B.z;
				temp[3] = B.w;*/
				break;
			default:
				throw new IllegalStateException();
		}
		return 1;
	}

	public int getDuration() { return duration; }

	public int getInterpolationType() { return intType; }

	public int getKeyframe(int index, float[] value) 
	{ 
		if ((index < 0) || (index >= keyframes)) {
			throw new IndexOutOfBoundsException();
		}

		if ((value != null) && (value.length < componentCount)) {
			throw new IllegalArgumentException();
		}

		if (value != null) {
			System.arraycopy(keyFrames[index], 0, value, 0, componentCount);
		}

		return keyFrameTimes[index];
	}

	public int getKeyframeCount() { return keyframes; }

	public int getRepeatMode() { return repeat; }

	public int getValidRangeFirst() { return rangeFirst; }

	public int getValidRangeLast() { return rangeLast; }

	public void setDuration(int value) 
	{ 
		duration = value;
		dirty = true;
	}

	public void setKeyframe(int index, int time, float[] value) 
	{ 
		if (value == null) 
		{
			throw new NullPointerException("Keyframe value vector must not be null");
		}
		if ((index < 0) || (index >= keyframes)) 
		{
			throw new IndexOutOfBoundsException();
		}
		if ((value.length < componentCount) || (time < 0)) 
		{
			throw new IllegalArgumentException();
		}

		System.arraycopy(value, 0, keyFrames[index], 0, componentCount);
		keyFrameTimes[index] = time;
		if (intType == SLERP || intType == SQUAD) {
			QVec4 q = new QVec4();
			float[] kf = keyframeAt(index);
			q.setQuat(kf);
			q.normalizeQuat();
			kf[0] = q.x;
			kf[1] = q.y;
			kf[2] = q.z;
			kf[3] = q.w;
		}
		dirty = true;
	}

	public void setRepeatMode(int mode) { repeat=mode; }

	public void setValidRange(int first, int last) 
	{
		if ((first < 0) || (first >= keyframes) || (last < 0) || (last >= keyframes)) 
		{
			throw new IndexOutOfBoundsException("Invalid range");
		}
		rangeFirst=first; 
		rangeLast=last;
		dirty=true;
	}

}
