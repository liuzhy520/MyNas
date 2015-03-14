package org.no_ip.zhouzian.mynas.infrastructure;

import android.webkit.MimeTypeMap;

public class MimeType {

	/* Get the mime type from url or file name */
	static public String GetMimeType(String fileName) {
		String extension = "";
		int i = fileName.lastIndexOf('.');
		if (i > 0) {
			extension = fileName.substring(i + 1).toLowerCase();
		}
		switch (extension) {
			case "mp3":
				return "audio/mpeg";
			case "aac":
				return "audio/aac";
			case "ogg":
				return "audio/ogg";
			case "flac":
				return "audio/flac";
			case "mp4":
				return "video/mp4";
			case "avi":
				return "video/x-msvideo";
			case "pdf":
				return "application/pdf";
			case "jpg":
				return "image/jpeg";
			case "jpeg":
				return "image/jpeg";
			default:
				return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
		}
	}
}
