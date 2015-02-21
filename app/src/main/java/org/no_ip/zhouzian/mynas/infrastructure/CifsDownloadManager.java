package org.no_ip.zhouzian.mynas.infrastructure;

import android.content.Context;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import jcifs.smb.SmbFile;

public class CifsDownloadManager {
    static private Context appContext;              //current app appContext
    static private List<DownloadJoblet> queue;     //all joblets stay in queue
    private CifsDownloadManager () {}

    /* Check if the class has been initialized */
    static public boolean IsInitialized(){
        return appContext != null;
    }

    /* Initialize the download manager by app appContext */
    static public void Init(Context context){
        appContext = context;
        queue = new ArrayList<DownloadJoblet>();
    }

    /* Get the totall number of jobs in the queue */
    static public int getTotalJobCount () {
        return queue.size();
    }

    /* Add a new job to the job queue */
    static public void AddJob(SmbFile sourceFile, File destFolder) {
        try {
            DownloadJoblet newJob = new DownloadJoblet(sourceFile, destFolder, sourceFile.getName());
            queue.add(newJob);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /* Remove a job from the job queue */
    static public void RemoveJob(String jobId) {
        DownloadJoblet jobToRemove = null;
        for (DownloadJoblet job : queue) {
            if (job.getJobId().equals(jobId)){
                jobToRemove = job;
                break;
            }
        }
        if (jobToRemove != null) {
            jobToRemove.Cancel();
            queue.remove(jobToRemove);
        }
    }
}
