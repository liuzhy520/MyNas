package org.no_ip.zhouzian.mynas.infrastructure;

public class SmbEntry {
	private String name;
	private boolean isDirectory;
	private long lastModifiedTime;

	public SmbEntry(String name, boolean isDirectory, long lastModified) {
		this.name = name;
		this.isDirectory = isDirectory;
		this.lastModifiedTime = lastModified;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isDirectory() {
		return isDirectory;
	}

	public void setDirectory(boolean isDirectory) {
		this.isDirectory = isDirectory;
	}

	public long getLastModifiedTime() {
		return lastModifiedTime;
	}

	public void setLastModifiedTime(long lastModified) {
		this.lastModifiedTime = lastModified;
	}
}
