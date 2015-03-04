package org.no_ip.zhouzian.mynas.infrastructure;

public class MimeType {

    /* Get the mime type from url or file name */
    static public String GetMimeType(String fileName) {
        String extension = "";
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            extension = fileName.substring(i+1).toLowerCase();
        }
        switch (extension) {
            case "mp3": return "audio/mpeg";
            case "mp4": return "video/mp4";
            default: return "";
        }
    }
}