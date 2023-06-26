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


public class AnimationController extends Object3D
{

	private float weight;
	private float speed;
	private int world;
	private float sequence;
	private int intStart;
	private int intEnd;


	public AnimationController() {  }


	public int timeToActivation(int worldTime) 
	{
		if (worldTime < intStart) { return intStart - worldTime; }
		else if (worldTime < intEnd) { return 0; }

		return 0x7FFFFFFF;
	}

	public int timeToDeactivation(int worldTime) 
	{
		if (worldTime < intEnd) { return intEnd - worldTime; }
		return 0x7FFFFFFF;
	}

	public boolean isActive(int worldTime) 
	{
		if (intStart == intEnd) { return true; }
		return (worldTime >= intStart && worldTime < intEnd);
	}

	public int getActiveIntervalEnd() { return intEnd; }

	public int getActiveIntervalStart()  { return intStart; }

	public float getPosition(int worldTime)  { return (this.sequence + (this.speed * (float) (worldTime - this.world))); }

	public int getRefWorldTime()  { return world; }

	public float getSpeed()  { return speed; }

	public float getWeight()  { return weight; }

	public void setActiveInterval(int start, int end)
	{
		if (start > end) { throw new IllegalArgumentException("Invalid Active interval (> than end interval)"); }

		this.intStart = start;
		this.intEnd = end;
	}

	public void setPosition(float sequenceTime, int worldTime)
	{
		this.sequence = sequenceTime;
		this.world = worldTime;
	}

	public void setSpeed(float value, int worldTime)
	{
		this.sequence = getPosition(worldTime);
		this.speed = value;
		this.world = worldTime;
	}

	public void setWeight(float value)  
	{
		if (weight < 0) { throw new IllegalArgumentException("Weight must be >= 0"); }
		this.weight = value; 
	}

}
