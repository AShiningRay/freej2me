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
import java.io.File;
import java.io.FilenameFilter;
import java.util.Vector;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;

import javax.microedition.media.Control;
import javax.microedition.media.Controllable;

public class PlatformPlayer implements Player
{

	private String contentType = "";

	private audioplayer player;

	private int state = Player.UNREALIZED;

	private Vector<PlayerListener> listeners;

	private Control[] controls;

	public static boolean customMidi = false;

	public static File soundfontDir = new File("freej2me_system/customMIDI/");

	public PlatformPlayer(InputStream stream, String type)
	{
		listeners = new Vector<PlayerListener>();
		controls = new Control[3];

		contentType = type;

		if(Mobile.sound == false)
		{
			player = new audioplayer();
		}
		else
		{
			if(type.equalsIgnoreCase("audio/mid") || type.equalsIgnoreCase("audio/midi") || type.equalsIgnoreCase("sp-midi") || type.equalsIgnoreCase("audio/spmidi"))
			{
				player = new midiPlayer(stream);
			}
			else
			{
				if(type.equalsIgnoreCase("audio/x-wav") || type.equalsIgnoreCase("audio/wav"))
				{
					player = new wavPlayer(stream);
				}
				else /* TODO: Implement a player for amr and mpeg audio types */
				{
					System.out.println("No Player For: "+contentType);
					player = new audioplayer();
				}
			}
		}
		controls[0] = new volumeControl();
		controls[1] = new tempoControl();
		controls[2] = new midiControl();

		//System.out.println("media type: "+type);
	}

	public PlatformPlayer(String locator)
	{
		player = new audioplayer();
		listeners = new Vector<PlayerListener>();
		controls = new Control[3];
		System.out.println("Player locator: "+locator);
	}


	public void close()
	{
		try
		{
			player.stop();
			state = Player.CLOSED;
			notifyListeners(PlayerListener.CLOSED, null);	
		}
		catch (Exception e) { }
		state = Player.CLOSED;	
	}

	public int getState()
	{
		if(player.isRunning()==false)
		{
			state = Player.PREFETCHED;
		}
		return state;
	}

	public void start()
	{
		//System.out.println("Play "+contentType);
		try
		{
			player.start();
		}
		catch (Exception e) {  }
	}

	public void stop()
	{
		//System.out.println("Stop "+contentType);
		try
		{
			player.stop();
		}
		catch (Exception e) { }
	}

	public void addPlayerListener(PlayerListener playerListener)
	{
		//System.out.println("Add Player Listener");
		listeners.add(playerListener);
	}

	public void removePlayerListener(PlayerListener playerListener)
	{
		//System.out.println("Remove Player Listener");
		listeners.remove(playerListener);
	}

	private void notifyListeners(String event, Object eventData)
	{
		for(int i=0; i<listeners.size(); i++)
		{
			listeners.get(i).playerUpdate(this, event, eventData);
		}
	}

	public void deallocate()
	{
		stop();
		player.deallocate();
		state = Player.CLOSED;
	}

	public String getContentType() { return contentType; }

	public long getDuration() { return Player.TIME_UNKNOWN; }

	public long getMediaTime() { return player.getMediaTime(); }

	public void prefetch() { state = Player.PREFETCHED; }

	public void realize() { state = Player.REALIZED; }

	public void setLoopCount(int count) { player.setLoopCount(count); }

	public long setMediaTime(long now) { return player.setMediaTime(now); }

	// Controllable interface //

	public Control getControl(String controlType)
	{
		if(controlType.equals("VolumeControl")) { return controls[0]; }
		if(controlType.equals("TempoControl")) { return controls[1]; }
		if(controlType.equals("MIDIControl")) { return controls[2]; }
		if(controlType.equals("javax.microedition.media.control.VolumeControl")) { return controls[0]; }
		if(controlType.equals("javax.microedition.media.control.TempoControl")) { return controls[1]; }
		if(controlType.equals("javax.microedition.media.control.MIDIControl")) { return controls[2]; }
		return null;
	}

	public Control[] getControls()
	{
		return controls;
	}

	// Players //

	private class audioplayer
	{
		public void start() {  }
		public void stop() {  }
		public void setLoopCount(int count) {  }
		public long setMediaTime(long now) { return now; }
		public long getMediaTime() { return 0; }
		public boolean isRunning() { return false; }
		public void deallocate() {  }
	}

	private class midiPlayer extends audioplayer
	{
		private Sequencer midi;
		private Soundbank soundfont;
		Synthesizer synth;

		private long tick = 0L;

		public midiPlayer(InputStream stream)
		{
			try
			{
				midi = MidiSystem.getSequencer();

				/* 
				 * Check if the user wants to run a custom MIDI soundfont. Also, there's no harm 
				 * in checking if the directory exists again.
				 */
				if(customMidi && soundfontDir.exists())
				{
					/* Get the first sf2 soundfont in the directory */
					String[] fontfile = soundfontDir.list(new FilenameFilter()
					{
						@Override
						public boolean accept(File f, String soundfont ) 
						{
							return soundfont.endsWith(".sf2");
						}
					});

					/* 
					 * Only really set the player to use a custom midi soundfont if there is one 
					 * inside the directory.
					 */
					if(fontfile.length > 0) 
					{
						soundfont = MidiSystem.getSoundbank(new File(soundfontDir + "/" + fontfile[0]));
						midi = MidiSystem.getSequencer(false);
						synth = MidiSystem.getSynthesizer();
						synth.open();
						synth.loadAllInstruments(soundfont);
						midi.getTransmitter().setReceiver(synth.getReceiver());
					}
				}

				midi.open();
				midi.setSequence(stream);
				state = Player.PREFETCHED;
			}
			catch (Exception e) 
			{ 
				System.out.println("Couldn't load midi file::" + e.getMessage());
				if(customMidi) { synth.close(); }
				midi.close();
			}
		}

		public void start()
		{
			if(isRunning()) { return; }

			if(midi.getTickPosition() >= midi.getTickLength())
			{
				midi.setTickPosition(0);
			}
			tick = midi.getTickPosition();
			midi.start();
			state = Player.STARTED;
			notifyListeners(PlayerListener.STARTED, tick);
		}

		public void stop()
		{
			midi.stop();
			state = Player.PREFETCHED;
			tick = midi.getTickPosition();
			notifyListeners(PlayerListener.STOPPED, tick);
		}
		public void deallocate()
		{
			midi.close();
		}

		public void setLoopCount(int count)
		{
			if(count < 1) {count = 1;} /* Treat cases where an app might set loops as 0 */
			midi.setLoopCount(count-1);
		}
		public long setMediaTime(long now)
		{
			try
			{
				midi.setTickPosition(now);
			}
			catch (Exception e) { }
			return now;
		}
		public long getMediaTime()
		{
			return midi.getTickPosition();
		}
		public boolean isRunning()
		{
			return midi.isRunning();
		}
	}

	private class wavPlayer extends audioplayer
	{
		/* PCM WAV variables */
		private AudioInputStream wavStream;
		private Clip wavClip;
		/* IMA ADPCM WAV variables */
		private WavImaAdpcmDecoder adpcmDec = new WavImaAdpcmDecoder();
		InputStream decodedStream;
		private int[] wavHeaderData = new int[4];
		
		/* Player control variables */
		private long time = 0L;

		public wavPlayer(InputStream stream)
		{
			try
			{
				/*
				 * A wav header is generally 44-bytes long, and it is what we need to read in order to get
				 * the stream's format, frame size, bit rate, number of channels, etc. which gives us information
				 * on the kind of codec needed to play or decode the incoming stream. The stream needs to be reset
				 * or else PCM files will be loaded without a header and it might cause issues with playback.
				 */
				stream.mark(48);
				wavHeaderData = adpcmDec.readHeader(stream);
				stream.reset();

				/* We only check for IMA ADPCM at the moment. */
				if(wavHeaderData[0] != 17) /* If it's not IMA ADPCM we don't need to do anything to the stream. */
				{
					wavStream = AudioSystem.getAudioInputStream(stream);
					wavClip = AudioSystem.getClip();
					wavClip.open(wavStream);
					state = Player.PREFETCHED;
				}
				else /* But if it is IMA ADPCM, we have to decode it manually. */
				{
					decodedStream = adpcmDec.decodeImaAdpcm(stream, wavHeaderData);
					wavStream = AudioSystem.getAudioInputStream(decodedStream);
					wavClip = AudioSystem.getClip();
					wavClip.open(wavStream);
					state = Player.PREFETCHED;
				}
			}
			catch (Exception e) 
			{ 
				System.out.println("Couldn't load wav file: " + e.getMessage());
				wavClip.close();
			}
		}

		public void start()
		{
			if(isRunning()) { return; }
			
			if(wavClip.getFramePosition() >= wavClip.getFrameLength())
			{
				wavClip.setFramePosition(0);
			}
			time = wavClip.getMicrosecondPosition();
			wavClip.start();
			state = Player.STARTED;
			notifyListeners(PlayerListener.STARTED, time);
		}

		public void stop()
		{
			wavClip.stop();
			time = wavClip.getMicrosecondPosition();
			state = Player.PREFETCHED;
			notifyListeners(PlayerListener.STOPPED, time);
		}

		public void setLoopCount(int count)
		{
			if(count < 1) {count = 1;} /* Treat cases where an app might set loops as 0 */
			wavClip.loop(count-1);
		}

		public long setMediaTime(long now)
		{
			wavClip.setMicrosecondPosition(now);
			return now;
		}
		public long getMediaTime()
		{
			return wavClip.getMicrosecondPosition();
		}

		public boolean isRunning()
		{
			return wavClip.isRunning();
		}
	}

	// Controls //

	private class midiControl implements javax.microedition.media.control.MIDIControl
	{
		public int[] getBankList(boolean custom) { return new int[]{}; }

		public int getChannelVolume(int channel) { return 0; }

		public java.lang.String getKeyName(int bank, int prog, int key) { return ""; }

		public int[] getProgram(int channel) { return new int[]{}; }

		public int[] getProgramList(int bank) { return new int[]{}; }

		public java.lang.String getProgramName(int bank, int prog) { return ""; }

		public boolean isBankQuerySupported() { return false; }

		public int longMidiEvent(byte[] data, int offset, int length) { return 0; }

		public void setChannelVolume(int channel, int volume) {  }

		public void setProgram(int channel, int bank, int program) {  }

		public void shortMidiEvent(int type, int data1, int data2) {  }
	}

	private class volumeControl implements javax.microedition.media.control.VolumeControl
	{
		private int level = 100;
		private boolean muted = false;

		public int getLevel() { return level; }

		public boolean isMuted() { return muted; }

		public int setLevel(int value) { level = value; return level; }

		public void setMute(boolean mute) { muted = mute; }
	}

	private class tempoControl implements javax.microedition.media.control.TempoControl
	{
		int tempo = 5000;
		int rate = 5000;

		public int getTempo() { return tempo; }

		public int setTempo(int millitempo) { tempo = millitempo; return tempo; }

		// RateControl interface
		public int getMaxRate() { return rate; }

		public int getMinRate() { return rate; }

		public int getRate() { return rate; }

		public int setRate(int millirate) { rate=millirate; return rate; }
	}
}
