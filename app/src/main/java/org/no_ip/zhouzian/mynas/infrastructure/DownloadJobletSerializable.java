package org.no_ip.zhouzian.mynas.infrastructure;

import java.io.File;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

public class DownloadJobletSerializable {
    private String jobId;               //unique id of the download job
    private String destFolder;          //the destination folder of the download
    private String destFileName;        //the file name in the destination folder
    private long fileSize;              //length of the file in byte
    private int progress;               //current progress in percentage
    private long speed;                 //current speed in byte/second
    private DownloadJoblet.DownloadStatus status;      //current job status

    /* Construct a serializable download history from a joblet */
    public DownloadJobletSerializable(DownloadJoblet joblet) {
        this.jobId = joblet.getJobId();
        this.destFileName = joblet.getDestFileName();
        this.destFolder = joblet.getDestFolder().getAbsolutePath();
        this.fileSize = joblet.getFileSize();
        this.progress = joblet.getProgress();
        this.speed = joblet.getSpeed();
        this.status = joblet.getStatus();
    }

    public DownloadJoblet CreateJobletHistory() {
        DownloadJoblet joblet = new DownloadJoblet();
        joblet.setJobId(this.jobId);
        joblet.setDestFolder(new File(this.destFolder));
        joblet.setDestFileName(this.destFileName);
        joblet.setFileSize(this.fileSize);
        joblet.setProgress(this.progress);
        joblet.setSpeed(this.speed);
        joblet.setStatus(this.status);
        return joblet;
    }
}
