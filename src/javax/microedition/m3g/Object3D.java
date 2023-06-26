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

import java.util.Vector;
import java.lang.Object;

public abstract class Object3D
{

	private int userID;
	Object userObject;
	Vector animTracks = new Vector();


	public void addAnimationTrack(AnimationTrack animationTrack) 
	{  
		if (animationTrack == null) 
		{
			throw new NullPointerException("Object3D received null animationTrack");
		}
		if (/*(!isCompatible(animationTrack)) ||*/ animTracks.contains(animationTrack)) 
		{
			throw new IllegalArgumentException("AnimationTrack already exists");
		}
			
		int newTrackTarget = animationTrack.getTargetProperty();
		int components = animationTrack.getKeyframeSequence().getComponentCount();
		int i;
		for (i = 0; i < animTracks.size(); i++) {
			AnimationTrack track = (AnimationTrack) animTracks.elementAt(i);

			if (track.getTargetProperty() > newTrackTarget)
				break;

			if (track.getTargetProperty() == newTrackTarget && (track.getKeyframeSequence().getComponentCount() != components)) {
				throw new IllegalArgumentException();
			}
		}
		
		animTracks.add(i, animationTrack);
	}

	public int animate(int time) 
	{ 
		int validity = 0x7FFFFFFF;

		if (animTracks == null) { return validity; }

		int numTracks = animTracks.size();

		for (int trackIndex = 0; trackIndex < numTracks; ) 
		{
			AnimationTrack track = (AnimationTrack) animTracks.elementAt(trackIndex);
			KeyframeSequence sequence = track.getKeyframeSequence();

			int components = sequence.getComponentCount();
			int property = track.getTargetProperty();
			int nextProperty;

			int sumWeights = 0;
			float[] sumValues = new float[components];

			for (int i = 0; i < components; i++) sumValues[i] = 0;

			do 
			{
				float[] weight = new float[1];
				int[] Validity = new int[1];

				track.getContribution(time, sumValues, weight, Validity);
				if (Validity[0] <= 0) { return 0; }

				sumWeights += weight[0];
				validity = (validity <= Validity[0]) ? validity : Validity[0];

				if (++trackIndex == numTracks) { break; }
				track = (AnimationTrack) animTracks.elementAt(trackIndex);
				nextProperty = track.getTargetProperty();
			} while (nextProperty == property);

			if (sumWeights > 0) { /* TODO: Update properties when sum of weights are positive */ }
		}
		return validity;
	}

	public Object3D duplicate() { return this; }

	public Object3D find(int userID) 
	{ 
		if (this.userID == userID) { return this; }
		else if (animTracks != null) 
		{

			for (int i = 0; i < animTracks.size(); i++) {
				AnimationTrack track = (AnimationTrack) animTracks.elementAt(i);
				Object3D found = track.find(userID);
				if (found != null)
					return found;
			}
		}
		return null;
	}

	public AnimationTrack getAnimationTrack(int index) { return (AnimationTrack) animTracks.elementAt(index); }

	public int getAnimationTrackCount() { return 0; }

	public int getReferences(Object3D[] references) 
	{ 
		if (!animTracks.isEmpty()) 
		{
			if (references != null) 
			{
				for (int i = 0; i < animTracks.size(); ++i) { references[i] = (Object3D) animTracks.elementAt(i); }
			}
			return animTracks.size();
		}
		else { return 0; }
	}

	void updateProperty(int property, float[] value) { }

	public int getUserID() { return this.userID; }

	public Object getUserObject() { return this.userObject; }

	public void removeAnimationTrack(AnimationTrack animationTrack) { animTracks.removeElement(animationTrack);  }

	public void setUserID(int userID) { this.userID = userID; }

	public void setUserObject(Object userObject) { this.userObject = userObject; }

}
