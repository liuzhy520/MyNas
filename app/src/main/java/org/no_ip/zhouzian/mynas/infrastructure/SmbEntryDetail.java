package org.no_ip.zhouzian.mynas.infrastructure;

import java.text.DecimalFormat;
import java.util.Date;

public class SmbEntryDetail extends SmbEntry{
    private String created;
    private String lastModified;
    private String size;

    public SmbEntryDetail(String name, boolean isDirectory, long created, long lastModified, long size) {
        super (name, isDirectory);
        this.created = new Date(created).toString();
        this.lastModified = new Date(lastModified).toString();
        this.size = Formatter.FormatFileSize(size);
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = new Date(created).toString();
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = new Date(lastModified).toString();
    }

    public String getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = Formatter.FormatFileSize(size);
    }

}
