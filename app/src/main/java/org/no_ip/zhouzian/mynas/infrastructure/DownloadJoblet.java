package org.no_ip.zhouzian.mynas.infrastructure;

import java.io.File;
import java.util.UUID;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

/* Defines a paticular download job */
public class DownloadJoblet {
    private String jobId;                  //unique id of the download job
    private SmbFile sourceFile;         //the smbFile to be downloaded
    private File destFolder;            //the destination folder of the download
    private String destFileName;        //the file name in the destination folder
    private long fileSize;              //length of the file in byte
    private int progress;               //current progress in percentage
    private long speed;                 //current speed in byte/second
    private DownloadStatus status;      //current job status

    /* Constructor */
    public DownloadJoblet(SmbFile sourceFile, File destFolder, String destFileName) throws SmbException {
        this.jobId = UUID.randomUUID().toString();
        this.sourceFile = sourceFile;
        this.destFolder = destFolder;
        this.destFileName = destFileName;
        this.fileSize = sourceFile.length();
        this.progress = 0;
        this.speed = 0;
        this.status = DownloadStatus.WAITING;
    }

    /* Cancel the job */
    public void Cancel() {
        if (status == DownloadStatus.IN_PROGRESS) {
            //TODO: stop the downloading
        }
        status = DownloadStatus.CANCELLED;
    }

    public void Execute() {
        status = DownloadStatus.IN_PROGRESS;
        //TODO: start the download job
        status = DownloadStatus.FINISHED;
    }

    public String getJobId() {
        return jobId;
    }

    public SmbFile getSourceFile() {
        return sourceFile;
    }

    public File getDestFolder() {
        return destFolder;
    }

    public String getDestFileName() {
        return destFileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public int getProgress() {
        return progress;
    }

    public long getSpeed() {
        return speed;
    }

    public DownloadStatus getStatus() {
        return status;
    }

    public enum DownloadStatus {
        WAITING,        //when the job is created and waiting in the queue
        IN_PROGRESS,    //when the job has been started
        FINISHED,       //when the job has been finished successfully
        TERMINATED,     //when the job is terminated on exception
        CANCELLED       //when the job is cancelled by user
    }
}
