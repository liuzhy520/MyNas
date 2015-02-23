package org.no_ip.zhouzian.mynas.infrastructure;

public class DownloadDC {
    private String jobId;
    private String destFileName;
    private String fileSize;                //file size in human readable format
    private String progress;                //progress x%
    private String speed;                   //speed in human readable format
    private DownloadJoblet.DownloadStatus status;      //current job status

    /* Constructor */
    public DownloadDC (DownloadJoblet joblet) {
        this.jobId = joblet.getJobId();
        this.destFileName = joblet.getDestFileName();
        this.fileSize = Formatter.FormatFileSize(joblet.getFileSize());
        this.progress = joblet.getProgress() + " %";
        this.speed = Formatter.FormatFileSize(joblet.getSpeed()) + "/s";
        this.status = joblet.getStatus();
    }
}
