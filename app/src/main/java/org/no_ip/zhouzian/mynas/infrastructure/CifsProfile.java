package org.no_ip.zhouzian.mynas.infrastructure;

import android.app.DownloadManager;
import android.content.Intent;
import android.net.Uri;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbRandomAccessFile;

public class CifsProfile {
    private int profileId;
    private String profileName;
    private String rootUrl;
    private int portNumber;     //default should be 445, but some old NAS devices use 139
    private String username;
    private String password;

    public CifsProfile (String profileName, String rootUrl, int portNumber, String username, String password){
        this.profileId = -1;
        this.profileName = profileName.trim();
        this.rootUrl = rootUrl.trim();
        this.portNumber = portNumber;
        this.username = username.trim();
        this.password = password.trim();
    }

    /* Return a list of entries by the relative path */
    public List<SmbEntry> browse (String relativePath) throws Exception{
        List<SmbEntry> ret = new ArrayList<SmbEntry>();
        SmbFile root = new SmbFile(getSmbInstance(), relativePath);
        for(SmbFile entry : root.listFiles()) {
            if (!entry.isHidden()) {
                ret.add(new SmbEntry(entry.getName(), entry.isDirectory()));
            }
        }
        Collections.sort(ret, new Comparator<SmbEntry>() {
            @Override
            public int compare(SmbEntry lhs, SmbEntry rhs) {
                int sComp = -Boolean.compare(lhs.isDirectory(), rhs.isDirectory());
                if (sComp != 0) {
                    return sComp;
                } else {
                    return lhs.getName().compareTo(rhs.getName());
                }
            }
        });
        return ret;
    }

    /* Returns detail infomation of a smbFile specified by relative path under the root url */
    public SmbEntryDetail getDetail (String relativePath) throws Exception {
        SmbFile entry = new SmbFile(getSmbInstance(), relativePath);
        return new SmbEntryDetail(entry.getName(), entry.isDirectory(), entry.createTime(), entry.lastModified(), entry.length());
    }

    /* Download the file using the given download manager service and return the reference id */
    public long downloadFile (String relativePath, DownloadManager downloadManager) throws Exception {
        SmbFile file = new SmbFile(getSmbInstance(), relativePath);
        if (file.isFile()) {
            StreamOverHttp httpServer = new StreamOverHttp(file, null);
            Uri uri = httpServer.getUri(file.getName());
            DownloadManager.Request request = new DownloadManager.Request(uri);
            request.setTitle(file.getName());
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            return downloadManager.enqueue(request);
        } else {
            throw new CifsProfileException("Cannot download folder.");
        }
    }

    /* ProfileId will be reassigned when saved to profile manager
     * unsaved profile has id == -1 */
    public boolean isPersisted(){
        return profileId != -1;
    }

    /* Check if connection type is anonymous.
     * Only check username. */
    public boolean isAnonymous(){
        return username.isEmpty();
    }

    /* Validate if the profile is correct.
     * Empty profile name or root url is not allowed
     * Duplicated profile name is checked by profile manager
     * Test connection may takes long time. */
    public void validate () throws CifsProfileException{
        if (profileName.isEmpty()){
            throw new CifsProfileException("Profile name cannot be empty");
        } else if (rootUrl.isEmpty()){
            throw new CifsProfileException("Root url cannot be empty");
        }
        try{
            SmbFile smbInstance = getSmbInstance();
            smbInstance.connect();
            smbInstance.list();
        } catch (Throwable th) {
            throw new CifsProfileException("Connection failed. Please verify your profile.");
        }
    }

    /* Create a SmbFile instance the can be used to query folders */
    public SmbFile getSmbInstance () throws MalformedURLException {
        NtlmPasswordAuthentication auth = isAnonymous() ? new NtlmPasswordAuthentication("", "Guest", "") : new NtlmPasswordAuthentication("", username, password);    // we don't support domain, so the first parameter is always empty string
        return new SmbFile(formatUrl(), auth);
    }

    /* Insert port number to url if the port number is not 445.
     * Added trailing slash is it's not there. */
    private String formatUrl (){
        StringBuilder sb = new StringBuilder();
        String[] paths = rootUrl.split("/");
        if (portNumber != 445){
            paths[0] = paths[0] + ":" + portNumber;
        }
        sb.append("smb://");
        for(String path : paths){
            sb.append(path);
            sb.append("/");
        }
        return sb.toString();
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
