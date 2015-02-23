package org.no_ip.zhouzian.mynas.infrastructure;

import java.text.DecimalFormat;

public class Formatter {

    /* Convert bytes to human readable format: byte, KB, MB, GB */
    static public String FormatFileSize (long byteSize) {
        DecimalFormat df = new DecimalFormat("#.##");
        if (byteSize / 1024 < 1) {
            return byteSize + " bytes";
        } else if ((double)byteSize / 1024 / 1024 < 1) {
            return df.format((double)byteSize / 1024) + " KB";
        } else if ((double)byteSize / 1024 / 1024 / 1024 < 1) {
            return df.format((double)byteSize / 1024 / 1024) + " MB";
        } else {
            return df.format((double)byteSize / 1024 / 1024 / 1024) + " GB";
        }
    }
}
