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

import java.util.Arrays;

public class TriangleStripArray extends IndexBuffer
{

	public TriangleStripArray(int[] indices, int[] stripLengths)
	{
		if (indices == null || stripLengths == null)
			throw new java.lang.NullPointerException();
		if (stripLengths.length == 0)
			throw new java.lang.IllegalArgumentException();
		if (Arrays.stream(stripLengths).anyMatch(e -> e < 3))
			throw new java.lang.IllegalArgumentException();
		if (indices.length < Arrays.stream(stripLengths).sum())
			throw new java.lang.IllegalArgumentException();
		if (Arrays.stream(indices).anyMatch(e -> e < 0 || 65535 < e))
			throw new java.lang.IndexOutOfBoundsException();

		this.updateFields(true, indices, stripLengths);
	}

	public TriangleStripArray(int firstIndex, int[] stripLengths)
	{
		if (stripLengths == null)
			throw new java.lang.NullPointerException();
		if (stripLengths.length == 0)
			throw new java.lang.IllegalArgumentException();
		if (Arrays.stream(stripLengths).anyMatch(e -> e < 3))
			throw new java.lang.IllegalArgumentException();
		if (firstIndex < 0)
			throw new java.lang.IndexOutOfBoundsException();
		if (firstIndex + Arrays.stream(stripLengths).sum() > 65535)
			throw new java.lang.IndexOutOfBoundsException();

		this.updateFields(false, new int[] { firstIndex }, stripLengths);
	}

	private void updateFields(
		boolean isExplicit,
		int[] indices,
		int[] stripLengths
	) {
		super.indexCount = Arrays.stream(stripLengths)
			.map(e -> (e - 2) * 3)
			.sum();

		super.indices = new int[this.indexCount];

		int  in_offset = 0;
		int out_offset = 0;

		for (int strip_id = 0; strip_id < stripLengths.length; strip_id++)
		{
			for (int i = 0; i < (stripLengths[strip_id] - 2); i++)
			{
				int x,y,z;
				int abs_index = in_offset + i;
				boolean swap = i % 2 == 1;

				if (isExplicit)
				{
					x = indices[abs_index + 0];
					y = indices[abs_index + 1];
					z = indices[abs_index + 2];
				}
				else
				{
					x = indices[0] + abs_index + 0;
					y = indices[0] + abs_index + 1;
					z = indices[0] + abs_index + 2;
				}

				// TODO determine correct way to swap vertices
				super.indices[out_offset + 0] = swap ? x : x;
				super.indices[out_offset + 1] = swap ? y : y;
				super.indices[out_offset + 2] = swap ? z : z;

				out_offset += 3;
			}
			in_offset += stripLengths[strip_id];
		}
	}

}
