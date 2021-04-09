package nl.tue.visualcomputingproject.group9a.project.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.util.Properties;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SettingsFile {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	
	private final Properties props = new Properties();
	private final File path;

	private final Lock lock = new ReentrantLock();
	private final Condition waitForMod = lock.newCondition();
	private boolean modified = false;
	
	
	public SettingsFile(File path) {
		this.path = path;
		readFile();
		Thread t = new Thread(() -> {
			while (true) {
				try {
					try {
						lock.lock();
						if (!modified) {
							waitForMod.await();
						}
						
					} finally {
						lock.unlock();
					}

					//noinspection BusyWait
					Thread.sleep(1000);
					try {
						lock.lock();
						modified = false;
					} finally {
						lock.unlock();
					}
					
					writeFile();
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}, "Settings thread");
		t.setDaemon(false);
		t.start();
	}
	
	public void updateValue(String key, boolean value) {
		updateValue(key, Boolean.toString(value));
	}

	public void updateValue(String key, float value) {
		updateValue(key, Float.toString(value));
	}

	public void updateValue(String key, double value) {
		updateValue(key, Double.toString(value));
	}
	
	public void updateValue(String key, String value) {
		lock.lock();
		try {
			props.setProperty(key, value);
			modified = true;
			waitForMod.signal();
		} finally {
			lock.unlock();
		}
	}

	public boolean getValue(String key, boolean def) {
		String v = getValue(key);
		if (v == null) {
			updateValue(key, def);
			return def;
		}
		return Boolean.parseBoolean(v);
	}

	public float getValue(String key, float def) {
		String v = getValue(key);
		if (v == null) {
			updateValue(key, def);
			return def;
		}
		return Float.parseFloat(v);
	}

	public double getValue(String key, double def) {
		String v = getValue(key);
		if (v == null) {
			updateValue(key, def);
			return def;
		}
		return Double.parseDouble(v);
	}
	
	public String getValue(String key) {
		lock.lock();
		try {
			return props.getProperty(key);
		} finally {
			lock.unlock();
		}
	}
	
	public void readFile() {
		try {
			synchronized(props) {
				try (FileInputStream inputStream = new FileInputStream(path)) {
					props.load(inputStream);
				}
			}
		} catch(FileNotFoundException e) {
			LOGGER.info("No settings file found. Nothing is changing.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeFile() {
		try {
			synchronized(props) {
				try (FileOutputStream outputStream = new FileOutputStream(path)) {
					props.store(outputStream, "Settings");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public boolean isModified() {
		lock.lock();
		try {
			return modified;
		} finally {
			lock.unlock();
		}
	}
	
}
