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
package org.recompile.mobile;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AmrDecoder
{

	/* Information about this audio format: 
     * https://wiki.multimedia.cx/index.php/AMR-NB
     * https://web.archive.org/web/20110625112053/http://www.developer.nokia.com/Community/Wiki/AMR_format 
     */

	/* Enums for supported AMR-NB Bitrates. There's only a few so they can be declared as byte types */
    private static final byte AMRNB_ENCODE_BITRATE_4_75    =  0;  /* 4.75 kbps -> 13 Byte FrameSize */
    private static final byte AMRNB_ENCODE_BITRATE_5_15    =  1;  /* 5.15 kbps -> 14 Byte FrameSize */
    private static final byte AMRNB_ENCODE_BITRATE_5_9     =  2;  /* 5.90 kbps -> 16 Byte FrameSize */
    private static final byte AMRNB_ENCODE_BITRATE_6_7     =  3;  /* 6.70 kbps -> 18 Byte FrameSize */
    private static final byte AMRNB_ENCODE_BITRATE_7_4     =  4;  /* 7.40 kbps -> 20 Byte FrameSize */
    private static final byte AMRNB_ENCODE_BITRATE_7_95    =  5;  /* 7.95 kbps -> 21 Byte FrameSize */
    private static final byte AMRNB_ENCODE_BITRATE_10_2    =  6;  /* 10.2 kbps -> 27 Byte FrameSize */
    private static final byte AMRNB_ENCODE_BITRATE_12_2    =  7;  /* 12.2 kbps -> 32 Byte FrameSize */
    private static final byte AMRNB_ENCODE_DTX             =  8;  /* Data comprised only of silence */
    private static final byte AMRNB_ENCODE_N_MODES         =  9;  /* Amount of supported data modes */
    private static final byte AMRNB_ENCODE_NO_DATA         =  15; /* No data transmitted / received */


    /*
	 * According to https://en.wikipedia.org/wiki/Adaptive_Multi-Rate_audio_codec
     * the format is encoded at 8000Hz.
	 */
	public final static short AMRNB_ENCODE_FREQUENCY = 8000;

    /* Currently assuming all incoming AMR files will be mono */
    public final static byte AMRNB_ENCODE_CHANNELS = 1;


    /* NOTE: These are sourced from FFMPEG's "amrnbdec.c" */
    private static final short  AMR_BLOCK_SIZE   =  160;   ///< samples per frame
    private static final int AMR_SAMPLE_BOUND =  32768;   ///< threshold for synthesis overflow

    /**
     * Scale from constructed speech to [-1,1]
     *
     * AMR is designed to produce 16-bit PCM samples (3GPP TS 26.090 4.2) but
     * upscales by two (section 6.2.2).
     *
     * Fundamentally, this scale is determined by energy_mean through
     * the fixed vector contribution to the excitation vector.
     */
    private static final float AMR_SAMPLE_SCALE = (2.0f / 32768.0f);

    /** Prediction factor for 12.2kbit/s mode */
    private static final float PRED_FAC_MODE_12k2 = 0.65f;

    private static final float LSF_R_FAC               = (8000 / 32768); ///< LSF residual tables to Hertz
    private static final float MIN_LSF_SPACING         = (50.0488f / 8000.0f); ///< Ensures stability of LPC filter
    private static final byte PITCH_LAG_MIN_MODE_12k2  = 18;   ///< Lower bound on decoded lag search in 12.2kbit/s mode

    /** Initial energy in dB. Also used for bad frames (unimplemented). */
    private static final float MIN_ENERGY = -14.0f;

    /** Maximum sharpening factor
     *
     * The specification says 0.8, which should be 13107, but the reference C code
     * uses 13017 instead. (Amusingly the same applies to SHARP_MAX in g729dec.c.)
     */
    private static final double SHARP_MAX = 0.79449462890625;

    /** Number of impulse response coefficients used for tilt factor */
    private static final byte AMR_TILT_RESPONSE =  22;
    /** Tilt factor = 1st reflection coefficient * gamma_t */
    private static final float AMR_TILT_GAMMA_T = 0.8f;
    /** Adaptive gain control factor used in post-filter */
    private static final float AMR_AGC_ALPHA    = 0.9f;




	/* This method will decode AMR-NB into linear WAV PCM_S16LE. */
	public static byte[] decodeNarrowBand(final byte[] input, int inputSize, final int numChannels, final int frameSize)
	{
		/* TODO: Figure out a way of decoding AMR-NB streams */

        return null;
	}

	/* This method will decode a single IMA ADPCM sample to linear PCM_S16LE sample. */
	static short decodeSample(final int channel, byte adpcmSample)
	{
		/* TODO: Figure out a way of decoding AMR-NB samples. */

        return 0;
	}
	
	/*
	 * Since the header is always expected to be positioned right at the start
	 * of a byte array, read it to determine the AMR type.
	 * 
	 * Optionally it also returns some information about the audio format to help build a 
	 * new header for the decoded stream.
	 */
	public int[] readHeader(InputStream input) throws IOException 
	{
		/*
		 * The header of an AMR-NB  file is X bytes long and has the following format:
         *
         *  TODO: Study the header for both AMR-NB and AMR-WB, then document it here
         *  and rework this function accordingly.
		 */

		String riff = readInputStreamASCII(input, 4);
		int dataSize = readInputStreamInt32(input);
		String format = readInputStreamASCII(input, 4);
		String fmt = readInputStreamASCII(input, 4);
		int chunkSize = readInputStreamInt32(input);
		short audioFormat = (short) readInputStreamInt16(input);
		short audioChannels = (short) readInputStreamInt16(input);
		int sampleRate = readInputStreamInt32(input);
		int bytesPerSec = readInputStreamInt32(input);
		short frameSize = (short) readInputStreamInt16(input);
		short bitsPerSample = (short) readInputStreamInt16(input);
		String data = readInputStreamASCII(input, 4);
		int sampleDataLength = readInputStreamInt32(input);

		/* Those are only meant for debugging. */
		/*
		System.out.println("WAV HEADER_START");

		System.out.println(riff);
		System.out.println("FileSize:" + dataSize);
		System.out.println("Format: " + format);

		System.out.println("---'fmt' header---\n");
		System.out.println("Header ChunkSize:" + Integer.toString(chunkSize));
		System.out.println("AudioFormat: " + Integer.toString(audioFormat));
		System.out.println("AudioChannels:" + Integer.toString(audioChannels));
		System.out.println("SampleRate:" + Integer.toString(sampleRate));
		System.out.println("BytesPerSec:" + Integer.toString(bytesPerSec));
		System.out.println("FrameSize:" + Integer.toString(frameSize));
		System.out.println("BitsPerSample:" + Integer.toHexString(bitsPerSample));

		System.out.println("\n---'data' header---\n");
		System.out.println("SampleData Length:" + Integer.toString(sampleDataLength));

		System.out.println("WAV HEADER_END\n\n\n");
		*/
		
		/* 
		 * We need the audio format to check if it's ADPCM or PCM, and the file's 
		 * dataSize, SampleRate and audioChannels to decode ADPCM and build a new header. 
		 */
		return new int[] {audioFormat, sampleRate, (int) AMRNB_ENCODE_CHANNELS, frameSize};
	}

	/* Read a 16-bit little-endian unsigned integer from input.*/
	public static int readInputStreamInt16(InputStream input) throws IOException 
	{ return ( input.read() & 0xFF ) | ( ( input.read() & 0xFF ) << 8 ); }

	/* Read a 32-bit little-endian signed integer from input.*/
	public static int readInputStreamInt32(InputStream input) throws IOException 
	{
		return ( input.read() & 0xFF ) | ( ( input.read() & 0xFF ) << 8 )
			| ( ( input.read() & 0xFF ) << 16 ) | ( ( input.read() & 0xFF ) << 24 );
	}

	/* Return a String containing 'n' Characters of ASCII/ISO-8859-1 text from input. */
	public static String readInputStreamASCII(InputStream input, int nChars) throws IOException 
	{
		byte[] chars = new byte[nChars];
		readInputStreamData(input, chars, 0, nChars);
		return new String(chars, "ISO-8859-1");
	}

	/* Read 'n' Bytes from the InputStream starting from the specified offset into the output array. */
	public static void readInputStreamData(InputStream input, byte[] output, int offset, int nBytes) throws IOException 
	{
		int end = offset + nBytes;
		while(offset < end) 
		{
			int read = input.read(output, offset, end - offset);
			if(read < 0) throw new java.io.EOFException();
			offset += read;
		}
	}

	/* 
	 * Builds a WAV header that describes the decoded AMR file on the first 44 bytes. 
	 * Data: little-endian, 16-bit, signed, same sample rate and channels as source AMR format.
	 */
	private void buildHeader(final byte[] buffer, final short numChannels, final int sampleRate) 
	{ 
		final short bitsPerSample = 16;   /* 16-bit PCM */
		final short audioFormat = 1;      /* WAV linear PCM */
		final int subChunkSize = 16;      /* Fixed size for Wav Linear PCM */
		final int chunk = 0x52494646;     /* 'RIFF' */ 
		final int format = 0x57415645;    /* 'WAVE' */ 
		final int subChunk1 = 0x666d7420; /* 'fmt ' */ 
		final int subChunk2 = 0x64617461; /* 'data' */ 

		/* 
		 * We'll have 16 bits per sample, so each sample has 2 bytes, with that we just divide
		 * the size of the byte buffer (minus the header) by (bitsPerSample/8), then multiply by the amount 
		 * of channels... assuming i didn't mess anything up on the calculus.
		*/
		final int samplesPerChannel = buffer.length-44 / ((bitsPerSample/8) * numChannels);

		/* 
		 * Frame size is fairly standard, and PCM's fixed sample size makes it so the frameSize is either 2 bytes 
		 * for mono, or 4 bytes for stereo.
		 */
		final short frameSize = (short) (numChannels * (bitsPerSample / 8));

		/* 
		 * Previously only took into account mono streams. And since we know the framesize and
		 * the amount of samples per channel, in a format that has a fixed amount of bits per sample,
		 * we can account for multiple audio channels on sampleDataLength with a simpler calculus:
		 */
		final int sampleDataLength = (samplesPerChannel * numChannels) * frameSize;

		/* 
		 * Represents how many bytes are streamed per second. With all of the data above, it's trivial to
		 * calculate by getting the sample rate, the amount of channels and bytes per sample (bitsPerSample / 8)
		 */
		final int bytesPerSec = sampleRate * numChannels * (bitsPerSample / 8);
		
		/* NOTE: ChunkSize includes the header, so sampleDataLength + 44, which is the byte size of our header */

		/* ChunkID */
		ByteBuffer.wrap(buffer, 0, 4).order(ByteOrder.BIG_ENDIAN).putInt(chunk);
		/* ChunkSize (or File size) */
		ByteBuffer.wrap(buffer, 4, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(sampleDataLength + 44);
		/* Format (WAVE) */
		ByteBuffer.wrap(buffer, 8, 4).order(ByteOrder.BIG_ENDIAN).putInt(format);
		/* SubchunkID (fmt) */
		ByteBuffer.wrap(buffer, 12, 4).order(ByteOrder.BIG_ENDIAN).putInt(subChunk1);
		/* SubchunkSize (or format chunk size) */
		ByteBuffer.wrap(buffer, 16, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(subChunkSize);
		/* Audioformat */
		ByteBuffer.wrap(buffer, 20, 2).order(ByteOrder.LITTLE_ENDIAN).putShort(audioFormat);
		/* NumChannels (will be the same as source ADPCM) */
		ByteBuffer.wrap(buffer, 22, 2).order(ByteOrder.LITTLE_ENDIAN).putShort((short) numChannels);
		/* SampleRate (will be the same as source ADPCM) */
		ByteBuffer.wrap(buffer, 24, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(sampleRate);
		/* ByteRate (BytesPerSec) */
		ByteBuffer.wrap(buffer, 28, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(bytesPerSec);
		/* BlockAlign (Frame Size) */
		ByteBuffer.wrap(buffer, 32, 2).order(ByteOrder.LITTLE_ENDIAN).putShort(frameSize);
		/* BitsPerSample */
		ByteBuffer.wrap(buffer, 34, 2).order(ByteOrder.LITTLE_ENDIAN).putShort(bitsPerSample);
		/* Subchunk2ID (data) */
		ByteBuffer.wrap(buffer, 36, 4).order(ByteOrder.BIG_ENDIAN).putInt(subChunk2);
		/* Subchunk2 Size (sampledata length) */
		ByteBuffer.wrap(buffer, 40, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(sampleDataLength);
	}

	/* Decode the received AMR-NB stream into a signed WAV PCM16LE byte array, then return it to PlatformPlayer. */
	public ByteArrayInputStream decodeAmrNB(InputStream stream, int[] wavHeaderData) throws IOException
	{
		/* Remove the header from the stream, we shouldn't "decode" it as if it was a sample */
		readHeader(stream);

		final byte[] input = new byte[stream.available()];
		readInputStreamData(stream, input, 0, stream.available());

		byte[] output = decodeNarrowBand(input, input.length, wavHeaderData[2], wavHeaderData[3]);
		buildHeader(output, (short) wavHeaderData[2], wavHeaderData[1]); /* Builds a new header for the decoded stream. */

		return new ByteArrayInputStream(output);
	}

    /* Same as above but for AMR-WB. */
	public ByteArrayInputStream decodeAmrWB(InputStream stream, int[] wavHeaderData) throws IOException
	{
		/* TODO: This whole function after AMR-NB works and AMR-WB decoding is figured out. */
        return null;
	}

}

