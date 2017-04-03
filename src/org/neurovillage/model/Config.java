package org.neurovillage.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.ini4j.Ini;
import org.ini4j.IniPreferences;
import org.neurovillage.tools.ResourceManager;

public class Config {
	
	public static enum audiofeedback_params { sample, volume, x, y }
	public static enum server_settings_params { serial_address , pro_mode, pp, load_from_disk, audiofeedback, simulation, bit24, fir, avg, tf }
	public static enum osc_settings_params { mode, address, ip, port }
	public static enum serial_settings_params { address, baudrate, message, mode}
	
	public static String serial_settings = "serial_settings";
	public static String osc_settings = "osc_settings";
	public static String audiofeedback = "default_audiofeedback";
	public static String server_settings = "server_settings";

	private File iniFile;
	public Preferences prefs;
	private Ini ini;

	public Config(String filename) {
		iniFile = new File(filename);
		System.out.println("opening config from " + filename);
		try {
			loadConfig(iniFile);
		} catch (BackingStoreException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean loadConfig(File iniFile) throws BackingStoreException, IOException {
		// iniFile = new File();
		System.out.println(iniFile);
		if (!iniFile.exists()) {
			ini = new Ini(ResourceManager.getInstance().getResource(iniFile.getName()));
			ini.store(iniFile);
//			Files.copy(new BufferedReader(new FileReader(ResourceManager.getInstance().getResource(iniFile.getName()))),iniFile, StandardCopyOption.REPLACE_EXISTING);
//			if (!iniFile.createNewFile())
//				return false;
			
		}

		ini = new Ini(iniFile);
		prefs = new IniPreferences(ini);


		System.out.println("loaded config");
		return true;
	}

	public boolean setPref(String section, String key, String value) {
		try {
			ini.put(section, key, value);
			prefs.sync();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public String getPref(String section, String key) {
		try {
			if (prefs.nodeExists(section))
				return prefs.node(section).get(key, null);
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
		return null;
	}

	public boolean store() {
		try {
			ini.store();
			System.out.println("stored config");
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

}
