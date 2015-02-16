package org.no_ip.zhouzian.mynas.infrastructure;

public class SmbEntry {
    private String name;
    private boolean isDirectory;

    public SmbEntry(String name, boolean isDirectory) {
        this.name = name;
        this.isDirectory = isDirectory;
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
}
