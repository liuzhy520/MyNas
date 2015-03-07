package org.no_ip.zhouzian.mynas.infrastructure;

import android.app.Activity;
import android.content.Context;
import android.os.StatFs;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.UUID;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

/* Defines a paticular download job */
public class DownloadJoblet {
    private Context appContext;         //current app context
    private String jobId;               //unique id of the download job
    private SmbFile sourceFile;         //the smbFile to be downloaded
    private File destFolder;            //the destination folder of the download
    private String destFileName;        //the file name in the destination folder
    private long fileSize;              //length of the file in byte
    private int progress;               //current progress in percentage
    private long speed;                 //current speed in byte/second
    private DownloadStatus status;      //current job status

    private boolean greenLight;         //indicate if the job should be cancelled in the next loop

    /* Default Constructor */
    public DownloadJoblet() {}

    /* Constructor */
    public DownloadJoblet(Context appContext, SmbFile sourceFile, File destFolder, String destFileName) throws SmbException {
        this.appContext = appContext;
        this.jobId = UUID.randomUUID().toString();
        this.sourceFile = sourceFile;
        this.destFolder = destFolder;
        this.destFileName = destFileName;
        this.fileSize = sourceFile.length();
        this.progress = 0;
        this.speed = 0;
        this.status = DownloadStatus.WAITING;
        this.greenLight = true;     //turn on the green light
    }

    /* Cancel the job */
    public void MarkAsCancel() {
        if (status == DownloadStatus.IN_PROGRESS) {
            greenLight = false;
        }
    }

    /* Execute the job. In each loop, it checks if the green light is turned off. */
    public void Execute() {
        status = DownloadStatus.IN_PROGRESS;
        InputStream in = null;
        OutputStream out = null;
        byte[] buffer = new byte[8192];             //process 8192 bytes in each loop and checks green light for possible cancellation
        long finishedBytes = 0;
        try {
            TryRenameFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
        File destFile = new File(destFolder, destFileName);
        long startTime = new Date().getTime();                //job start time;
        try {
            StatFs fsChecker = new StatFs(destFolder.getAbsolutePath());
            if (fsChecker.getAvailableBytes() < fileSize) {
                throw new Exception();
            }
            in = sourceFile.getInputStream();
            out = new FileOutputStream(destFile);
            int lengthProcessed;
            while((lengthProcessed = in.read(buffer)) > 0) {
                if (greenLight) {
                    finishedBytes += lengthProcessed;
                    out.write(buffer, 0, lengthProcessed);
                    UpdateProgress(finishedBytes, fileSize);
                    UpdateSpeed(finishedBytes, startTime);
                } else {
                    in.close();
                    out.close();
                    break;          //user cancels the job
                }
            }
            in.close();
            out.close();
        } catch (Exception ex) {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (destFile.isFile()) {
                destFile.delete();      //delete the file if download is terminated
            }
            status = DownloadStatus.TERMINATED;     //terminated by exception
            showToast("\"" + destFileName + "\" is terminated due to error or insufficient space.");
            return;
        }
        if (greenLight) {
            status = DownloadStatus.FINISHED;       //successfully finished execution
            showToast("\"" + destFileName + "\" is successfully downloaded.");
        } else {
            status = DownloadStatus.CANCELLED;      //stop execution since user turns off the green light
            if (destFile.isFile()) {
                destFile.delete();      //delete the file if download is cancelled
            }
            showToast("\"" + destFileName + "\" is successfully cancelled.");
        }
    }

    /* Checks the destination folder and rename file if the file name already exists */
    private void TryRenameFile() throws Exception {
        String[] tokens = destFileName.split("\\.(?=[^\\.]+$)");
        String tokenName = tokens[0];
        String tokenExt = tokens.length == 2 ? tokens[1] : "";
        for (int i = 0; i <=100; i ++) {        //at most try 100 times of renaming
            String newDestFileName = i == 0 ? destFileName : tokenName + "_" + i + "." + tokenExt;
            File destFile = new File(destFolder, newDestFileName);
            if (!destFile.isFile()) {
                destFileName = newDestFileName;
                return;
            }
        }
        throw new Exception ("Reached max renaming loop");
    }

    /* Updates the progress of the job */
    private void UpdateProgress(long finishedBytes, long totalBytes) {
        if (totalBytes != 0) {
            progress = (int) (finishedBytes * 100 / totalBytes);
        }
    }

    /* Update the speed. Speed is the average download speed in byte from job started */
    private void UpdateSpeed(long finishedBytes, long startTime) {
        long timeEscaped = (new Date().getTime() - startTime) / 1000;      //time escaped in second
        if (timeEscaped != 0) {
            speed = finishedBytes / timeEscaped;
        }
    }

    /* Display message in the UI */
    private void showToast (final String msg) {
        ((Activity)appContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(appContext, msg, Toast.LENGTH_LONG).show();
            }
        });
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

    public void setStatus(DownloadStatus status) {
        this.status = status;
    }

    public DownloadStatus getStatus() {
        return status;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public void setDestFolder(File destFolder) {
        this.destFolder = destFolder;
    }

    public void setDestFileName(String destFileName) {
        this.destFileName = destFileName;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public void setSpeed(long speed) {
        this.speed = speed;
    }

    public enum DownloadStatus {
        WAITING,        //when the job is created and waiting in the queue
        IN_PROGRESS,    //when the job has been started
        FINISHED,       //when the job has been finished successfully
        TERMINATED,     //when the job is terminated on exception
        CANCELLED       //when the job is cancelled by user
    }
}
