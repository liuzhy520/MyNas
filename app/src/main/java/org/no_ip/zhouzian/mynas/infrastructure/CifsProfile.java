package org.no_ip.zhouzian.mynas.infrastructure;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;

public class CifsProfile {
	private int profileId;
	private String profileName;
	private String rootUrl;
	private int portNumber;     //default should be 445, but some old NAS devices use 139
	private String domain;
	private String username;
	private String password;

	public CifsProfile(String profileName, String rootUrl, int portNumber, String domain, String username, String password) {
		this.profileId = -1;
		this.profileName = profileName.trim();
		this.rootUrl = rootUrl.trim();
		this.portNumber = portNumber;
		this.domain = domain.trim();
		this.username = username.trim();
		this.password = password.trim();
	}

	/* Return a list of entries by the relative path */
	public List<SmbEntry> browse(final String relativePath, final String orderBy) throws Exception {
		List<SmbEntry> ret = new ArrayList<SmbEntry>();
		SmbFile root = new SmbFile(getSmbInstance(), relativePath);
		for (SmbFile entry : root.listFiles()) {
			if (!entry.isHidden()) {
				ret.add(new SmbEntry(entry.getName(), entry.isDirectory(), entry.getLastModified()));
			}
		}
		Collections.sort(ret, new Comparator<SmbEntry>() {
			@Override
			public int compare(SmbEntry lhs, SmbEntry rhs) {
				if (orderBy.equals("typeAsc")) {
					int sComp = -BooleanCompare(lhs.isDirectory(), rhs.isDirectory());     //always shows directories first
					if (sComp != 0) {
						return sComp;
					} else {
						return lhs.getName().compareTo(rhs.getName());
					}
				} else if (orderBy.equals("typeDesc")) {
					int sComp = BooleanCompare(lhs.isDirectory(), rhs.isDirectory());     //always shows directories first
					if (sComp != 0) {
						return sComp;
					} else {
						return lhs.getName().compareTo(rhs.getName());
					}
				} else if (orderBy.equals("nameAsc")) {
					return lhs.getName().compareTo(rhs.getName());
				} else if (orderBy.equals("nameDesc")) {
					return -lhs.getName().compareTo(rhs.getName());
				} else if (orderBy.equals("dateAsc")) {
					return lhs.getLastModifiedTime() - rhs.getLastModifiedTime() > 0 ? 1 : -1;
				} else if (orderBy.equals("dateDesc")) {
					return lhs.getLastModifiedTime() - rhs.getLastModifiedTime() <= 0 ? 1 : -1;
				} else {
					return 0;       //Unsupported sort option. Do not sort.
				}
			}
		});
		return ret;
	}

	/* Returns detail infomation of a smbFile specified by relative path under the root url */
	public SmbEntryDetail getDetail(String relativePath) throws Exception {
		SmbFile entry = new SmbFile(getSmbInstance(), relativePath);
		return new SmbEntryDetail(entry.getName(), entry.isDirectory(), entry.createTime(), entry.lastModified(), entry.length());
	}

	/* Start a streaming server to serve the file and return the uri */
	public Uri streamFile(StreamServer sServer, String relativePath) throws Exception {
		SmbFile file = new SmbFile(getSmbInstance(), relativePath);
		sServer.stop();
		sServer.setFile(file);
		sServer.start();
		return Uri.parse(sServer.getFileUrl());
	}

	/* Download the file async using CifsDownloadManager class */
	public void downloadFileAsync(String relativePath) throws Exception {
		SmbFile file = new SmbFile(getSmbInstance(), relativePath);
		if (file.isFile()) {
			File destFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
			CifsDownloadManager.AddJob(file, destFolder);
		} else {
			throw new CifsProfileException("Cannot download folder.");
		}
	}

	/* Download a file sync and return the File object. Return value does not guarantee exist */
	public File downloadFileSync(Context context, String relativePath) throws Exception {
		String SYNC_DL_FOLDER = "cached_doc";
		File syncDownloadFolder = new File(context.getExternalCacheDir(), SYNC_DL_FOLDER);
		if (syncDownloadFolder.exists()) {
			for (File f : syncDownloadFolder.listFiles()) {
				if (f.isFile()) {
					f.delete();
				}
			}
		} else {
			syncDownloadFolder.mkdir();
		}
		SmbFile file = new SmbFile(getSmbInstance(), relativePath);
		if (file.isFile()) {
			InputStream in = null;
			OutputStream out = null;
			byte[] buffer = new byte[8192];
			try {
				in = file.getInputStream();
				out = new FileOutputStream(new File(syncDownloadFolder, file.getName()));
				int lengthProcessed;
				while ((lengthProcessed = in.read(buffer)) > 0) {
					out.write(buffer, 0, lengthProcessed);
				}
			} catch (Exception ex) {

			} finally {
				if (in != null) {
					in.close();
				}
				if (out != null) {
					out.close();
				}
			}
			return new File(syncDownloadFolder, file.getName());
		} else {
			throw new CifsProfileException("Cannot download folder.");
		}
	}

	/* ProfileId will be reassigned when saved to profile manager
	 * unsaved profile has id == -1 */
	public boolean isPersisted() {
		return profileId != -1;
	}

	/* Check if connection type is anonymous.
	 * Only check username. */
	public boolean isAnonymous() {
		return username.isEmpty();
	}

	/* Validate if the profile is correct.
	 * Empty profile name or root url is not allowed
	 * Duplicated profile name is checked by profile manager
	 * Test connection may takes long time. */
	public void validate() throws CifsProfileException {
		if (profileName.isEmpty()) {
			throw new CifsProfileException("Profile name cannot be empty");
		} else if (rootUrl.isEmpty()) {
			throw new CifsProfileException("Root url cannot be empty");
		}
		try {
			SmbFile smbInstance = getSmbInstance();
			smbInstance.connect();
			smbInstance.list();
		} catch (Throwable th) {
			throw new CifsProfileException("Connection failed. Please verify your profile.");
		}
	}

	/* Create a SmbFile instance the can be used to query folders */
	public SmbFile getSmbInstance() throws MalformedURLException {
		NtlmPasswordAuthentication auth = isAnonymous() ? new NtlmPasswordAuthentication(domain, "Guest", "") : new NtlmPasswordAuthentication(domain, username, password);
		return new SmbFile(formatUrl(), auth);
	}

	/* Create a shortcut profile based on the relativePath
	 * The new profile name is based on best guess, so still needs duplication check before persisting */
	public CifsProfile createShortCut(String relativePath) {
		String newRootUrl = rootUrl, newProfileName;
		if (!newRootUrl.endsWith("/")) {
			newRootUrl += "/";
		}
		newRootUrl += relativePath;
		String[] paths = relativePath.split("/");
		return new CifsProfile(paths[paths.length - 1], newRootUrl, this.portNumber, this.domain, this.username, this.password);
	}

	/* Insert port number to url if the port number is not 445.
	 * Added trailing slash if it's not there. */
	private String formatUrl() {
		StringBuilder sb = new StringBuilder();
		String[] paths = rootUrl.split("/");
		if (portNumber != 445) {
			paths[0] = paths[0] + ":" + portNumber;
		}
		sb.append("smb://");
		for (String path : paths) {
			sb.append(path);
			sb.append("/");
		}
		return sb.toString();
	}

	/* Support boolean compare for api level < 19 */
	private int BooleanCompare(boolean lhs, boolean rhs) {
		if ((lhs && rhs) || (!lhs && !rhs)) return 0;       //equal
		else if (lhs && !rhs) return 1;                     //greater
		else return -1;                                     //less
	}

	public int getProfileId() {
		return profileId;
	}

	public void setProfileId(int profileId) {
		this.profileId = profileId;
	}

	public String getProfileName() {
		return profileName;
	}

	public void setProfileName(String profileName) {
		this.profileName = profileName.trim();
	}

	public String getRootUrl() {
		return rootUrl;
	}

	public void setRootUrl(String rootUrl) {
		this.rootUrl = rootUrl.trim();
	}

	public int getPortNumber() {
		return portNumber;
	}

	public void setPortNumber(int portNumber) {
		this.portNumber = portNumber;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username.trim();
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password.trim();
	}

}
