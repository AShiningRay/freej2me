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
package org.recompile.freej2me;

import org.recompile.mobile.*;

import java.awt.Image;
import java.awt.Canvas;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.midlet.MIDlet;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.microedition.media.Manager;

public class Libretro
{
	private int lcdWidth;
	private int lcdHeight;

	private BufferedImage surface;
	private Graphics2D gc;

	private Config config;
	private boolean useNokiaControls = false;
	private boolean useSiemensControls = false;
	private boolean useMotorolaControls = false;
	private boolean rotateDisplay = false;
	private boolean soundEnabled = true;
	private int limitFPS = 0;
	private boolean directionalsAsEntireKeypad = false;
	private int maxmidiplayers = 32;
	private static boolean HWAccelEnabled = false;

	private boolean[] pressedKeys = new boolean[128];

	private byte[] frameBuffer = new byte[800*800*3];
	private byte[] frameHeader = new byte[]{(byte)0xFE, 0, 0, 0, 0, 0};

	private int mousex;
	private int mousey;

	private Set<Integer> incomingPressedKeys = new HashSet<>();

	/* 
	 * StringBuilder used to get the updated configs from the libretro core
	 * String[] used to tokenize each setting as its own string.
	 */
	private StringBuilder cfgs;
	String[] cfgtokens;

	LibretroIO lio;

	public static void main(String args[])
	{
		/* 
		 * Like for FreeJ2ME's standalone jar, the sooner we set those, the better.
		 * 2D Hardware acceleration requires OpenGL 1.2 / GLES 1.0, 
		 * so it should be safe to enable in the vast majority of cases.
		 */
		if(Integer.parseInt(args[8]) == 1) 
		{ 
			System.setProperty("sun.java2d.opengl", "true");
			System.setProperty("sun.java2d.opengles", "true");
			System.setProperty("sun.java2d.accthreshold", "0");
			HWAccelEnabled = true;
		}
		else 
		{ 
			System.setProperty("sun.java2d.opengl", "false");
			System.setProperty("sun.java2d.opengles", "false");
			HWAccelEnabled = false;
		}

		Libretro app = new Libretro(args);
	}

	public Libretro(String args[])
	{
		lcdWidth  = 240;
		lcdHeight = 320;

		/* 
		 * Notify the MIDlet class that this version of FreeJ2ME is for Libretro, which disables 
		 * the ability to close the jar when a J2ME app requests an exit as this can cause segmentation
		 * faults on frontends and also close the unexpectedly.
		*/
		MIDlet.isLibretro = true;

		/* 
		 * If the directory for custom soundfonts doesn't exist, create it, no matter if the user
		 * is going to use it or not.
		 */
		try 
		{
			if(!PlatformPlayer.soundfontDir.exists()) 
			{ 
				PlatformPlayer.soundfontDir.mkdirs();
				File dummyFile = new File(PlatformPlayer.soundfontDir + "/Put your sf2 bank here");
				dummyFile.createNewFile();
			}
		}
		catch(IOException e) { System.out.println("Failed to create custom midi info file:" + e.getMessage()); }

		/* 
		 * Checks if the arguments were received from the commandline -> width, height, rotate, phonetype, fps, sound, ...
		 * 
		 * NOTE:
		 * Due to differences in how linux and win32 pass their cmd arguments, we can't explictly check for a given size
		 * on the argv array. Linux includes the "java", "-jar" and "path/to/freej2me" into the array while WIN32 doesn't.
		 */
		lcdWidth =  Integer.parseInt(args[0]);
		lcdHeight = Integer.parseInt(args[1]);

		if(Integer.parseInt(args[2]) == 1) { rotateDisplay = true; }

		if(Integer.parseInt(args[3]) == 1)      { useNokiaControls = true;    }
		else if(Integer.parseInt(args[3]) == 2) { useSiemensControls = true;  }
		else if(Integer.parseInt(args[3]) == 3) { useMotorolaControls = true; }

		limitFPS = Integer.parseInt(args[4]);

		if(Integer.parseInt(args[5]) == 0) { soundEnabled = false; }

		if(Integer.parseInt(args[6]) == 1) { directionalsAsEntireKeypad = true; }

		if(Integer.parseInt(args[7]) == 1) { PlatformPlayer.customMidi = true; }
		
		maxmidiplayers = Integer.parseInt(args[9]);
		Manager.updatePlayerNum((byte) maxmidiplayers);

		/* Once it finishes parsing all arguments, it's time to set up freej2me-lr */

		surface = new BufferedImage(lcdWidth, lcdHeight, BufferedImage.TYPE_INT_ARGB); // libretro display
		gc = (Graphics2D)surface.getGraphics();

		Mobile.setPlatform(new MobilePlatform(lcdWidth, lcdHeight));

		config = new Config();
		config.onChange = new Runnable() { public void run() { settingsChanged(); } };

		lio = new LibretroIO();

		lio.start();

		Mobile.getPlatform().setPainter(new Runnable()
		{
			public void run()
			{
				try
				{
					gc.drawImage(Mobile.getPlatform().getLCD(), 0, 0, lcdWidth, lcdHeight, null);
					if(limitFPS>0) { Thread.sleep(limitFPS); }
				}
				catch (Exception e) { }
			}
		});

		System.out.println("+READY");
		System.out.flush();
	}

	private class LibretroIO
	{
		private Timer keytimer;
		private TimerTask keytask;

		public void start()
		{
			keytimer = new Timer();
			keytask = new LibretroTimerTask();
			keytimer.schedule(keytask, 0, 1);
		}

		public void stop()
		{
			keytimer.cancel();
		}

		private class LibretroTimerTask extends TimerTask
		{
			private int bin;
			private int[] din = new int[5];
			private int count = 0;
			private int code;
			private int mobikey;
			private StringBuilder path;
			private URL url;

			public void run()
			{
				try // to read keys
				{
					while(true)
					{
						bin = System.in.read();
						if(bin==-1) { return; }
						//System.out.print(" "+bin);
						din[count] = (int)(bin & 0xFF);
						count++;
						if (count==5)
						{
							count = 0;
							code = (din[1]<<24) | (din[2]<<16) | (din[3]<<8) | din[4];
							switch(din[0])
							{
								case 0: // keyboard key up (unused)
								break;

								case 1:	// keyboard key down (unused)
								break;

								case 2:	// joypad key up
									mobikey = getMobileKeyJoy(code, false);
									if (mobikey != 0)
									{
										keyUp(mobikey);
									}
								break;

								case 3: // joypad key down
									mobikey = getMobileKeyJoy(code, true);
									if (mobikey != 0)
									{
										keyDown(mobikey);
									}
								break;

								case 4: // mouse up
									mousex = (din[1]<<8) | din[2];
									mousey = (din[3]<<8) | din[4];
									if(!rotateDisplay)
									{
										Mobile.getPlatform().pointerReleased(mousex, mousey);
									}
									else
									{
										Mobile.getPlatform().pointerReleased(lcdWidth-mousey, mousex);
									}
								break;

								case 5: // mouse down
									mousex = (din[1]<<8) | din[2];
									mousey = (din[3]<<8) | din[4];
									if(!rotateDisplay)
									{
										Mobile.getPlatform().pointerPressed(mousex, mousey);
									}
									else
									{
										Mobile.getPlatform().pointerPressed(lcdWidth-mousey, mousex);
									}
								break;

								case 6: // mouse drag
									mousex = (din[1]<<8) | din[2];
									mousey = (din[3]<<8) | din[4];
									if(!rotateDisplay)
									{
										Mobile.getPlatform().pointerDragged(mousex, mousey);
									}
									else
									{
										Mobile.getPlatform().pointerDragged(lcdWidth-mousey, mousex);
									}
								break;

								case 10: // load jar
									path = new StringBuilder();
									for(int i=0; i<code; i++)
									{
										bin = System.in.read();
										path.append((char)bin);
									}
									url = (new File(path.toString())).toURI().toURL();
									if(Mobile.getPlatform().loadJar(url.toString()))
									{
										// Check config
										config.init();

										/* Override configs with the ones passed through commandline */
										config.settings.put("width",  ""+lcdWidth);
										config.settings.put("height", ""+lcdHeight);

										if(rotateDisplay)   { config.settings.put("rotate", "on");  }
										if(!rotateDisplay)  { config.settings.put("rotate", "off"); }

										if(useNokiaControls)         { config.settings.put("phone", "Nokia");    }
										else if(useSiemensControls)  { config.settings.put("phone", "Siemens");  }
										else if(useMotorolaControls) { config.settings.put("phone", "Motorola"); }
										else                         { config.settings.put("phone", "Standard"); }

										if(soundEnabled)   { config.settings.put("sound", "on");  }
										if(!soundEnabled)  { config.settings.put("sound", "off"); }

										config.settings.put("fps", ""+limitFPS);

										if(directionalsAsEntireKeypad)   { config.settings.put("maptofullkeypad", "on");  }
										if(!directionalsAsEntireKeypad)  { config.settings.put("maptofullkeypad", "off"); }

										config.settings.put("maxmidiplayers", ""+maxmidiplayers);

										if(!PlatformPlayer.customMidi) { config.settings.put("soundfont", "Default"); }
										else                           { config.settings.put("soundfont", "Custom");  }
										
										if(HWAccelEnabled) { config.sysSettings.put("2DHWAcceleration", "on");  }
										else               { config.sysSettings.put("2DHWAcceleration", "off"); }

										config.saveConfigs();
										settingsChanged();

										// Run jar
										Mobile.getPlatform().runJar();
									}
									else
									{
										System.out.println("Couldn't load jar...");
										System.exit(0);
									}
								break;

								case 11: // set save path //
									path = new StringBuilder();
									for(int i=0; i<code; i++)
									{
										bin = System.in.read();
										path.append((char)bin);
									}
									Mobile.getPlatform().dataPath = path.toString();
								break;

								case 13:
									/* Received updated settings from libretro core */
									cfgs = new StringBuilder();
									for(int i=0; i<code; i++)
									{
										bin = System.in.read();
										cfgs.append((char)bin);
									}
									String cfgvars = cfgs.toString();
									/* Tokens: [0]="FJ2ME_LR_OPTS:", [1]=width, [2]=height, [3]=rotate, [4]=phone, [5]=fps, ... */
									cfgtokens = cfgvars.split("[| x]", 0);
									/* 
									 * cfgtokens[0] is the string used to indicate that the 
									 * received string is a config update. Only useful for debugging, 
									 * but better leave it in there as we might make adjustments later.
									 */
									config.settings.put("width",  ""+Integer.parseInt(cfgtokens[1]));
									config.settings.put("height", ""+Integer.parseInt(cfgtokens[2]));

									if(Integer.parseInt(cfgtokens[3])==1)      { config.settings.put("rotate", "on");  }
									else if(Integer.parseInt(cfgtokens[3])==0) { config.settings.put("rotate", "off"); }

									if(Integer.parseInt(cfgtokens[4])==0)      { config.settings.put("phone", "Standard"); }
									else if(Integer.parseInt(cfgtokens[4])==1) { config.settings.put("phone", "Nokia");    }
									else if(Integer.parseInt(cfgtokens[4])==2) { config.settings.put("phone", "Siemens");  }
									else if(Integer.parseInt(cfgtokens[4])==3) { config.settings.put("phone", "Motorola"); }

									config.settings.put("fps", ""+cfgtokens[5]);

									if(Integer.parseInt(cfgtokens[6])==1)      { config.settings.put("sound", "on");  }
									else if(Integer.parseInt(cfgtokens[6])==0) { config.settings.put("sound", "off"); }

									if(Integer.parseInt(cfgtokens[7])==0)      { config.settings.put("maptofullkeypad", "off"); }
									else if(Integer.parseInt(cfgtokens[7])==1) { config.settings.put("maptofullkeypad", "on");  }

									if(Integer.parseInt(cfgtokens[8])==0)      { config.settings.put("soundfont", "Default"); }
									else if(Integer.parseInt(cfgtokens[8])==1) { config.settings.put("soundfont", "Custom");  }

									/* 
									 * Although hardware acceleration can be toggled at runtime, it still requires FreeJ2ME to be
									 * restarted in order to fully apply.
									 */
									if(Integer.parseInt(cfgtokens[9]) == 1)      { config.sysSettings.put("2DHWAcceleration", "on");  HWAccelEnabled = true;  }
									else if(Integer.parseInt(cfgtokens[9]) == 0) { config.sysSettings.put("2DHWAcceleration", "off"); HWAccelEnabled = false; }

									if(Integer.parseInt(cfgtokens[10])==0) { config.settings.put("maxmidiplayers", "1");}
									if(Integer.parseInt(cfgtokens[10])==1) { config.settings.put("maxmidiplayers", "2");}
									if(Integer.parseInt(cfgtokens[10])==2) { config.settings.put("maxmidiplayers", "4");}
									if(Integer.parseInt(cfgtokens[10])==3) { config.settings.put("maxmidiplayers", "8");}
									if(Integer.parseInt(cfgtokens[10])==4) { config.settings.put("maxmidiplayers", "16");}
									if(Integer.parseInt(cfgtokens[10])==5) { config.settings.put("maxmidiplayers", "32");}
									if(Integer.parseInt(cfgtokens[10])==6) { config.settings.put("maxmidiplayers", "48");}
									if(Integer.parseInt(cfgtokens[10])==7) { config.settings.put("maxmidiplayers", "64");}
									if(Integer.parseInt(cfgtokens[10])==8) { config.settings.put("maxmidiplayers", "96");}

									config.saveConfigs();
									settingsChanged();
								break;
								
								case 15:
									// Send Frame Libretro //
									try
									{
										int[] data;
	
										data = surface.getRGB(0, 0, lcdWidth, lcdHeight, null, 0, lcdWidth);

										int bufferLength = data.length*3;
										int cb = 0;
										for(int i=0; i<data.length; i++)
										{
											frameBuffer[cb]   = (byte)((data[i]>>16)&0xFF);
											frameBuffer[cb+1] = (byte)((data[i]>>8)&0xFF);
											frameBuffer[cb+2] = (byte)((data[i])&0xFF);
											cb+=3;
										}
										//frameHeader[0] = (byte)0xFE;
										frameHeader[1] = (byte)((lcdWidth>>8)&0xFF);
										frameHeader[2] = (byte)((lcdWidth)&0xFF);
										frameHeader[3] = (byte)((lcdHeight>>8)&0xFF);
										frameHeader[4] = (byte)((lcdHeight)&0xFF);
										//frameHeader[5] = rotate - set from config
										System.out.write(frameHeader, 0, 6);
										System.out.write(frameBuffer, 0, bufferLength);
										System.out.flush();
									}
									catch (Exception e)
									{
										System.out.print("Error sending frame: "+e.getMessage());
										System.exit(0);
									}
								break;
							}
							//System.out.println(" ("+code+") <- Key");
							//System.out.flush();
						}
					}
				}
				catch (Exception e) { System.exit(0); }
			}
		} // timer
	} // LibretroIO

	private void settingsChanged()
	{
		int w = Integer.parseInt(config.settings.get("width"));
		int h = Integer.parseInt(config.settings.get("height"));

		limitFPS = Integer.parseInt(config.settings.get("fps"));
		if(limitFPS>0) { limitFPS = 1000 / limitFPS; }

		String sound = config.settings.get("sound");
		Mobile.sound = false;
		if(sound.equals("on")) { Mobile.sound = true; }

		String phone = config.settings.get("phone");
		useNokiaControls = false;
		useSiemensControls = false;
		useMotorolaControls = false;
		Mobile.nokia = false;
		Mobile.siemens = false;
		Mobile.motorola = false;
		if(phone.equals("Nokia")) { Mobile.nokia = true; useNokiaControls = true; }
		if(phone.equals("Siemens")) { Mobile.siemens = true; useSiemensControls = true; }
		if(phone.equals("Motorola")) { Mobile.motorola = true; useMotorolaControls = true; }

		String rotate = config.settings.get("rotate");
		if(rotate.equals("on")) { rotateDisplay = true; frameHeader[5] = (byte)1; }
		if(rotate.equals("off")) { rotateDisplay = false; frameHeader[5] = (byte)0; }

		String midiSoundfont = config.settings.get("soundfont");
		if(midiSoundfont.equals("Custom"))  { PlatformPlayer.customMidi = true; }
		if(midiSoundfont.equals("Default")) { PlatformPlayer.customMidi = false; }

		String mapToEntireKeypad = config.settings.get("maptofullkeypad");
		if(mapToEntireKeypad.equals("on"))  { directionalsAsEntireKeypad = true; }
		if(mapToEntireKeypad.equals("off")) { directionalsAsEntireKeypad = false; }

		String G2DHardwareAcceleration = config.sysSettings.get("2DHWAcceleration");
		if(G2DHardwareAcceleration.equals("on")) 
		{ 
			System.setProperty("sun.java2d.opengl", "true");
			System.setProperty("sun.java2d.opengles", "true");
			System.setProperty("sun.java2d.accthreshold", "0");
			HWAccelEnabled = true;
		}
		else
		{
			System.setProperty("sun.java2d.opengl", "false");
			System.setProperty("sun.java2d.opengles", "false");
			HWAccelEnabled = false;
		}

		if(lcdWidth != w || lcdHeight != h)
		{
			lcdWidth = w;
			lcdHeight = h;
			Mobile.getPlatform().resizeLCD(w, h);
			surface = new BufferedImage(lcdWidth, lcdHeight, BufferedImage.TYPE_INT_ARGB); // libretro display
			gc = (Graphics2D)surface.getGraphics();
		}

		Manager.updatePlayerNum((byte) Integer.parseInt(config.settings.get("maxmidiplayers")));
	}

	private void keyDown(int key)
	{
		int mobikeyN = (key + 64) & 0x7F; //Normalized value for indexing the pressedKeys array
		if(config.isRunning)
		{
			config.keyPressed(key);
		}
		else
		{
			if (pressedKeys[mobikeyN] == false)
			{
				Mobile.getPlatform().keyPressed(key);
			}
			else
			{
				Mobile.getPlatform().keyRepeated(key);
			}
		}
		pressedKeys[mobikeyN] = true;
	}

	private void keyUp(int key)
	{
		int mobikeyN = (key + 64) & 0x7F; //Normalized value for indexing the pressedKeys array
		if(!config.isRunning)
		{
			Mobile.getPlatform().keyReleased(key);
		}
		pressedKeys[mobikeyN] = false;
	}

	private int getMobileKeyJoy(int keycode, boolean pressed)
	{
		// Input mappings that are expected to be the same on all control modes
		switch(keycode)
		{
			case 4: return Mobile.KEY_NUM9; // A
			case 5: return Mobile.KEY_NUM7; // B
			case 6: return Mobile.KEY_NUM0; // X
			case 10: return Mobile.KEY_NUM1; // L
			case 11: return Mobile.KEY_NUM3; // R
			case 12: return Mobile.KEY_STAR; // L2
			case 13: return Mobile.KEY_POUND; // R2
		}

		// These keys are overridden by the "useXControls" variables
		if(useNokiaControls)
		{
			switch(keycode)
			{
				case 0: 
					if(pressed) 
					{ 
						incomingPressedKeys.add(Mobile.KEY_NUM2);
						if(incomingPressedKeys.contains(Mobile.KEY_NUM4) && directionalsAsEntireKeypad) { keyUp(Mobile.KEY_NUM4); return Mobile.KEY_NUM1; }
						else if (incomingPressedKeys.contains(Mobile.KEY_NUM6) && directionalsAsEntireKeypad) { keyUp(Mobile.KEY_NUM6); return Mobile.KEY_NUM3; }
						else { keyDown(Mobile.NOKIA_UP); return Mobile.KEY_NUM2; } // Up
					}
					else 
					{ 
						incomingPressedKeys.remove(Mobile.KEY_NUM2);
						if(incomingPressedKeys.contains(Mobile.KEY_NUM4) && directionalsAsEntireKeypad) { keyDown(Mobile.KEY_NUM4); return Mobile.KEY_NUM1; }
						else if (incomingPressedKeys.contains(Mobile.KEY_NUM6) && directionalsAsEntireKeypad) { keyDown(Mobile.KEY_NUM6); return Mobile.KEY_NUM3; }
						else { keyUp(Mobile.NOKIA_UP); return Mobile.KEY_NUM2; } // Up
					}

				case 1: 
					if(pressed) 
					{ 
						incomingPressedKeys.add(Mobile.KEY_NUM8);
						if(incomingPressedKeys.contains(Mobile.KEY_NUM4) && directionalsAsEntireKeypad) { keyUp(Mobile.KEY_NUM4); return Mobile.KEY_NUM7; }
						else if (incomingPressedKeys.contains(Mobile.KEY_NUM6) && directionalsAsEntireKeypad) { keyUp(Mobile.KEY_NUM6); return Mobile.KEY_NUM9; }
						else { keyDown(Mobile.NOKIA_DOWN); return Mobile.KEY_NUM8; }  // Down
					}
					else 
					{ 
						incomingPressedKeys.remove(Mobile.KEY_NUM8);
						if(incomingPressedKeys.contains(Mobile.KEY_NUM4) && directionalsAsEntireKeypad) { keyDown(Mobile.KEY_NUM4); return Mobile.KEY_NUM7; }
						else if (incomingPressedKeys.contains(Mobile.KEY_NUM6) && directionalsAsEntireKeypad) { keyDown(Mobile.KEY_NUM6); return Mobile.KEY_NUM9; }
						else { keyUp(Mobile.NOKIA_DOWN); return Mobile.KEY_NUM8; }  // Down
					}

				case 2: 
					if(pressed)
					{ 
						incomingPressedKeys.add(Mobile.KEY_NUM4);
						if(incomingPressedKeys.contains(Mobile.KEY_NUM2) && directionalsAsEntireKeypad) { keyUp(Mobile.KEY_NUM2); return Mobile.KEY_NUM1; }
						else if (incomingPressedKeys.contains(Mobile.KEY_NUM8) && directionalsAsEntireKeypad) { keyUp(Mobile.KEY_NUM8); return Mobile.KEY_NUM7; }
						else { keyDown(Mobile.NOKIA_LEFT); return Mobile.KEY_NUM4; }  // Left
					}
					else 
					{
						incomingPressedKeys.remove(Mobile.KEY_NUM4); 
						if(incomingPressedKeys.contains(Mobile.KEY_NUM2) && directionalsAsEntireKeypad) { keyDown(Mobile.KEY_NUM2); return Mobile.KEY_NUM1; }
						else if (incomingPressedKeys.contains(Mobile.KEY_NUM8) && directionalsAsEntireKeypad) { keyDown(Mobile.KEY_NUM8); return Mobile.KEY_NUM7; }
						else { keyUp(Mobile.NOKIA_LEFT); return Mobile.KEY_NUM4; }  // Left
					}

				case 3:
					if(pressed) 
					{
						incomingPressedKeys.add(Mobile.KEY_NUM6); 
						if(incomingPressedKeys.contains(Mobile.KEY_NUM2) && directionalsAsEntireKeypad) { keyUp(Mobile.KEY_NUM2); return Mobile.KEY_NUM3; }
						else if (incomingPressedKeys.contains(Mobile.KEY_NUM8) && directionalsAsEntireKeypad) { keyUp(Mobile.KEY_NUM8); return Mobile.KEY_NUM9; }
						else { keyDown(Mobile.NOKIA_RIGHT); return Mobile.KEY_NUM6; }  // Right
					}
					else 
					{
						incomingPressedKeys.remove(Mobile.KEY_NUM6); 
						if(incomingPressedKeys.contains(Mobile.KEY_NUM2) && directionalsAsEntireKeypad) { keyDown(Mobile.KEY_NUM2); return Mobile.KEY_NUM3; }
						else if (incomingPressedKeys.contains(Mobile.KEY_NUM8) && directionalsAsEntireKeypad) { keyDown(Mobile.KEY_NUM8); return Mobile.KEY_NUM9; }
						else { keyUp(Mobile.NOKIA_RIGHT); return Mobile.KEY_NUM6; }  // Right
					}

				case 7: 
					if(pressed) { keyDown(Mobile.NOKIA_SOFT3); }
					else { keyUp(Mobile.NOKIA_SOFT3); }
					return Mobile.KEY_NUM5; // Y

				case 8: return Mobile.NOKIA_SOFT2; // Start
				case 9: return Mobile.NOKIA_SOFT1; // Select
			}
		}
		if(useSiemensControls)
		{
			switch(keycode)
			{
				case 0: 
					if(pressed) 
					{ 
						incomingPressedKeys.add(Mobile.KEY_NUM2);
						if(incomingPressedKeys.contains(Mobile.KEY_NUM4) && directionalsAsEntireKeypad) { keyUp(Mobile.KEY_NUM4); return Mobile.KEY_NUM1; }
						else if (incomingPressedKeys.contains(Mobile.KEY_NUM6) && directionalsAsEntireKeypad) { keyUp(Mobile.KEY_NUM6); return Mobile.KEY_NUM3; }
						else { keyDown(Mobile.SIEMENS_UP); return Mobile.KEY_NUM2; } // Up
					}
					else 
					{ 
						incomingPressedKeys.remove(Mobile.KEY_NUM2);
						if(incomingPressedKeys.contains(Mobile.KEY_NUM4) && directionalsAsEntireKeypad) { keyDown(Mobile.KEY_NUM4); return Mobile.KEY_NUM1; }
						else if (incomingPressedKeys.contains(Mobile.KEY_NUM6) && directionalsAsEntireKeypad) { keyDown(Mobile.KEY_NUM6); return Mobile.KEY_NUM3; }
						else { keyUp(Mobile.SIEMENS_UP); return Mobile.KEY_NUM2; } // Up
					}

				case 1: 
					if(pressed) 
					{ 
						incomingPressedKeys.add(Mobile.KEY_NUM8);
						if(incomingPressedKeys.contains(Mobile.KEY_NUM4) && directionalsAsEntireKeypad) { keyUp(Mobile.KEY_NUM4); return Mobile.KEY_NUM7; }
						else if (incomingPressedKeys.contains(Mobile.KEY_NUM6) && directionalsAsEntireKeypad) { keyUp(Mobile.KEY_NUM6); return Mobile.KEY_NUM9; }
						else { keyDown(Mobile.SIEMENS_DOWN); return Mobile.KEY_NUM8; }  // Down
					}
					else 
					{ 
						incomingPressedKeys.remove(Mobile.KEY_NUM8);
						if(incomingPressedKeys.contains(Mobile.KEY_NUM4) && directionalsAsEntireKeypad) { keyDown(Mobile.KEY_NUM4); return Mobile.KEY_NUM7; }
						else if (incomingPressedKeys.contains(Mobile.KEY_NUM6) && directionalsAsEntireKeypad) { keyDown(Mobile.KEY_NUM6); return Mobile.KEY_NUM9; }
						else { keyUp(Mobile.SIEMENS_DOWN); return Mobile.KEY_NUM8; }  // Down
					}

				case 2: 
					if(pressed)
					{ 
						incomingPressedKeys.add(Mobile.KEY_NUM4);
						if(incomingPressedKeys.contains(Mobile.KEY_NUM2) && directionalsAsEntireKeypad) { keyUp(Mobile.KEY_NUM2); return Mobile.KEY_NUM1; }
						else if (incomingPressedKeys.contains(Mobile.KEY_NUM8) && directionalsAsEntireKeypad) { keyUp(Mobile.KEY_NUM8); return Mobile.KEY_NUM7; }
						else { keyDown(Mobile.SIEMENS_LEFT); return Mobile.KEY_NUM4; }  // Left
					}
					else 
					{
						incomingPressedKeys.remove(Mobile.KEY_NUM4); 
						if(incomingPressedKeys.contains(Mobile.KEY_NUM2) && directionalsAsEntireKeypad) { keyDown(Mobile.KEY_NUM2); return Mobile.KEY_NUM1; }
						else if (incomingPressedKeys.contains(Mobile.KEY_NUM8) && directionalsAsEntireKeypad) { keyDown(Mobile.KEY_NUM8); return Mobile.KEY_NUM7; }
						else { keyUp(Mobile.SIEMENS_LEFT); return Mobile.KEY_NUM4; }  // Left
					}

				case 3:
					if(pressed) 
					{
						incomingPressedKeys.add(Mobile.KEY_NUM6); 
						if(incomingPressedKeys.contains(Mobile.KEY_NUM2) && directionalsAsEntireKeypad) { keyUp(Mobile.KEY_NUM2); return Mobile.KEY_NUM3; }
						else if (incomingPressedKeys.contains(Mobile.KEY_NUM8) && directionalsAsEntireKeypad) { keyUp(Mobile.KEY_NUM8); return Mobile.KEY_NUM9; }
						else { keyDown(Mobile.SIEMENS_RIGHT); return Mobile.KEY_NUM6; }  // Right
					}
					else 
					{
						incomingPressedKeys.remove(Mobile.KEY_NUM6); 
						if(incomingPressedKeys.contains(Mobile.KEY_NUM2) && directionalsAsEntireKeypad) { keyDown(Mobile.KEY_NUM2); return Mobile.KEY_NUM3; }
						else if (incomingPressedKeys.contains(Mobile.KEY_NUM8) && directionalsAsEntireKeypad) { keyDown(Mobile.KEY_NUM8); return Mobile.KEY_NUM9; }
						else { keyUp(Mobile.SIEMENS_RIGHT); return Mobile.KEY_NUM6; }  // Right
					}

				
				case 7:
					if(pressed) { keyDown(Mobile.SIEMENS_FIRE); }
					else { keyUp(Mobile.SIEMENS_FIRE); }
					return Mobile.KEY_NUM5; // Y

				case 8: return Mobile.SIEMENS_SOFT2; // Start
				case 9: return Mobile.SIEMENS_SOFT1; // Select
			}
		}
		if(useMotorolaControls)
		{
			switch(keycode)
			{
				case 0: 
					if(pressed) 
					{ 
						incomingPressedKeys.add(Mobile.KEY_NUM2);
						if(incomingPressedKeys.contains(Mobile.KEY_NUM4) && directionalsAsEntireKeypad) { keyUp(Mobile.KEY_NUM4); return Mobile.KEY_NUM1; }
						else if (incomingPressedKeys.contains(Mobile.KEY_NUM6) && directionalsAsEntireKeypad) { keyUp(Mobile.KEY_NUM6); return Mobile.KEY_NUM3; }
						else { keyDown(Mobile.MOTOROLA_UP); return Mobile.KEY_NUM2; } // Up
					}
					else 
					{ 
						incomingPressedKeys.remove(Mobile.KEY_NUM2);
						if(incomingPressedKeys.contains(Mobile.KEY_NUM4) && directionalsAsEntireKeypad) { keyDown(Mobile.KEY_NUM4); return Mobile.KEY_NUM1; }
						else if (incomingPressedKeys.contains(Mobile.KEY_NUM6) && directionalsAsEntireKeypad) { keyDown(Mobile.KEY_NUM6); return Mobile.KEY_NUM3; }
						else { keyUp(Mobile.MOTOROLA_UP); return Mobile.KEY_NUM2; } // Up
					}

				case 1: 
					if(pressed) 
					{ 
						incomingPressedKeys.add(Mobile.KEY_NUM8);
						if(incomingPressedKeys.contains(Mobile.KEY_NUM4) && directionalsAsEntireKeypad) { keyUp(Mobile.KEY_NUM4); return Mobile.KEY_NUM7; }
						else if (incomingPressedKeys.contains(Mobile.KEY_NUM6) && directionalsAsEntireKeypad) { keyUp(Mobile.KEY_NUM6); return Mobile.KEY_NUM9; }
						else { keyDown(Mobile.MOTOROLA_DOWN); return Mobile.KEY_NUM8; }  // Down
					}
					else 
					{ 
						incomingPressedKeys.remove(Mobile.KEY_NUM8);
						if(incomingPressedKeys.contains(Mobile.KEY_NUM4) && directionalsAsEntireKeypad) { keyDown(Mobile.KEY_NUM4); return Mobile.KEY_NUM7; }
						else if (incomingPressedKeys.contains(Mobile.KEY_NUM6) && directionalsAsEntireKeypad) { keyDown(Mobile.KEY_NUM6); return Mobile.KEY_NUM9; }
						else { keyUp(Mobile.MOTOROLA_DOWN); return Mobile.KEY_NUM8; }  // Down
					}

				case 2: 
					if(pressed)
					{ 
						incomingPressedKeys.add(Mobile.KEY_NUM4);
						if(incomingPressedKeys.contains(Mobile.KEY_NUM2) && directionalsAsEntireKeypad) { keyUp(Mobile.KEY_NUM2); return Mobile.KEY_NUM1; }
						else if (incomingPressedKeys.contains(Mobile.KEY_NUM8) && directionalsAsEntireKeypad) { keyUp(Mobile.KEY_NUM8); return Mobile.KEY_NUM7; }
						else { keyDown(Mobile.MOTOROLA_LEFT); return Mobile.KEY_NUM4; }  // Left
					}
					else 
					{
						incomingPressedKeys.remove(Mobile.KEY_NUM4); 
						if(incomingPressedKeys.contains(Mobile.KEY_NUM2) && directionalsAsEntireKeypad) { keyDown(Mobile.KEY_NUM2); return Mobile.KEY_NUM1; }
						else if (incomingPressedKeys.contains(Mobile.KEY_NUM8) && directionalsAsEntireKeypad) { keyDown(Mobile.KEY_NUM8); return Mobile.KEY_NUM7; }
						else { keyUp(Mobile.MOTOROLA_LEFT); return Mobile.KEY_NUM4; }  // Left
					}

				case 3:
					if(pressed) 
					{
						incomingPressedKeys.add(Mobile.KEY_NUM6); 
						if(incomingPressedKeys.contains(Mobile.KEY_NUM2) && directionalsAsEntireKeypad) { keyUp(Mobile.KEY_NUM2); return Mobile.KEY_NUM3; }
						else if (incomingPressedKeys.contains(Mobile.KEY_NUM8) && directionalsAsEntireKeypad) { keyUp(Mobile.KEY_NUM8); return Mobile.KEY_NUM9; }
						else { keyDown(Mobile.MOTOROLA_RIGHT); return Mobile.KEY_NUM6; }  // Right
					}
					else 
					{
						incomingPressedKeys.remove(Mobile.KEY_NUM6); 
						if(incomingPressedKeys.contains(Mobile.KEY_NUM2) && directionalsAsEntireKeypad) { keyDown(Mobile.KEY_NUM2); return Mobile.KEY_NUM3; }
						else if (incomingPressedKeys.contains(Mobile.KEY_NUM8) && directionalsAsEntireKeypad) { keyDown(Mobile.KEY_NUM8); return Mobile.KEY_NUM9; }
						else { keyUp(Mobile.MOTOROLA_RIGHT); return Mobile.KEY_NUM6; }  // Right
					}

				case 7:
					if(pressed) { keyDown(Mobile.MOTOROLA_FIRE); }
					else { keyUp(Mobile.MOTOROLA_FIRE); }
					return Mobile.KEY_NUM5; // Y

				case 8: return Mobile.MOTOROLA_SOFT2; // Start
				case 9: return Mobile.MOTOROLA_SOFT1; // Select
			}
		}
		else // Standard keycodes
		{
			switch(keycode)
			{
				case 0:
					if(pressed) 
					{ 
						incomingPressedKeys.add(Mobile.KEY_NUM2);
						if(incomingPressedKeys.contains(Mobile.KEY_NUM4) && directionalsAsEntireKeypad) { keyUp(Mobile.KEY_NUM4); return Mobile.KEY_NUM1; }
						else if (incomingPressedKeys.contains(Mobile.KEY_NUM6) && directionalsAsEntireKeypad) { keyUp(Mobile.KEY_NUM6); return Mobile.KEY_NUM3; }
						else { return Mobile.KEY_NUM2; } // Up
					}
					else 
					{ 
						incomingPressedKeys.remove(Mobile.KEY_NUM2);
						if(incomingPressedKeys.contains(Mobile.KEY_NUM4) && directionalsAsEntireKeypad) { keyDown(Mobile.KEY_NUM4); return Mobile.KEY_NUM1; }
						else if (incomingPressedKeys.contains(Mobile.KEY_NUM6) && directionalsAsEntireKeypad) { keyDown(Mobile.KEY_NUM6); return Mobile.KEY_NUM3; }
						else { return Mobile.KEY_NUM2; } // Up
					}

				case 1: 
					if(pressed) 
					{ 
						incomingPressedKeys.add(Mobile.KEY_NUM8);
						if(incomingPressedKeys.contains(Mobile.KEY_NUM4) && directionalsAsEntireKeypad) { keyUp(Mobile.KEY_NUM4); return Mobile.KEY_NUM7; }
						else if (incomingPressedKeys.contains(Mobile.KEY_NUM6) && directionalsAsEntireKeypad) { keyUp(Mobile.KEY_NUM6); return Mobile.KEY_NUM9; }
						else { return Mobile.KEY_NUM8; }  // Down
					}
					else 
					{ 
						incomingPressedKeys.remove(Mobile.KEY_NUM8);
						if(incomingPressedKeys.contains(Mobile.KEY_NUM4) && directionalsAsEntireKeypad) { keyDown(Mobile.KEY_NUM4); return Mobile.KEY_NUM7; }
						else if (incomingPressedKeys.contains(Mobile.KEY_NUM6) && directionalsAsEntireKeypad) { keyDown(Mobile.KEY_NUM6); return Mobile.KEY_NUM9; }
						else { return Mobile.KEY_NUM8; }  // Down
					}

				case 2: 
					if(pressed)
					{ 
						incomingPressedKeys.add(Mobile.KEY_NUM4);
						if(incomingPressedKeys.contains(Mobile.KEY_NUM2) && directionalsAsEntireKeypad) { keyUp(Mobile.KEY_NUM2); return Mobile.KEY_NUM1; }
						else if (incomingPressedKeys.contains(Mobile.KEY_NUM8) && directionalsAsEntireKeypad) { keyUp(Mobile.KEY_NUM8); return Mobile.KEY_NUM7; }
						else { return Mobile.KEY_NUM4; }  // Left
					}
					else 
					{
						incomingPressedKeys.remove(Mobile.KEY_NUM4); 
						if(incomingPressedKeys.contains(Mobile.KEY_NUM2) && directionalsAsEntireKeypad) { keyDown(Mobile.KEY_NUM2); return Mobile.KEY_NUM1; }
						else if (incomingPressedKeys.contains(Mobile.KEY_NUM8) && directionalsAsEntireKeypad) { keyDown(Mobile.KEY_NUM8); return Mobile.KEY_NUM7; }
						else { return Mobile.KEY_NUM4; }  // Left
					}

				case 3:
					if(pressed) 
					{
						incomingPressedKeys.add(Mobile.KEY_NUM6); 
						if(incomingPressedKeys.contains(Mobile.KEY_NUM2) && directionalsAsEntireKeypad) { keyUp(Mobile.KEY_NUM2); return Mobile.KEY_NUM3; }
						else if (incomingPressedKeys.contains(Mobile.KEY_NUM8) && directionalsAsEntireKeypad) { keyUp(Mobile.KEY_NUM8); return Mobile.KEY_NUM9; }
						else { return Mobile.KEY_NUM6; }  // Right
					}
					else 
					{
						incomingPressedKeys.remove(Mobile.KEY_NUM6); 
						if(incomingPressedKeys.contains(Mobile.KEY_NUM2) && directionalsAsEntireKeypad) { keyDown(Mobile.KEY_NUM2); return Mobile.KEY_NUM3; }
						else if (incomingPressedKeys.contains(Mobile.KEY_NUM8) && directionalsAsEntireKeypad) { keyDown(Mobile.KEY_NUM8); return Mobile.KEY_NUM9; }
						else { return Mobile.KEY_NUM6; }  // Right
					}

				case 7: return Mobile.KEY_NUM5; // Y
				case 8: return Mobile.NOKIA_SOFT2; // Start
				case 9: return Mobile.NOKIA_SOFT1; // Select
			}
		}

		return Mobile.KEY_NUM5;
	}
}
