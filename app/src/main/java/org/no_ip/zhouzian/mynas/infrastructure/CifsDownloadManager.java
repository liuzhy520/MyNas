package org.no_ip.zhouzian.mynas.infrastructure;

import android.content.Context;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import jcifs.smb.SmbFile;

public class CifsDownloadManager {
    static private Context appContext;                      //current app appContext
    static private BlockingQueue<DownloadJoblet> queue;     //all joblets stay in a blocking queue
    static private List<DownloadJoblet> history;            //successfully finished jobs will be pushed to history array
    static private DownloadJoblet currentJob;               //current job being executed
    static private Thread jobConsumerThread;                //thread that takes job from queue and execute it

    private CifsDownloadManager () {}

    /* Check if the class has been initialized */
    static public boolean IsInitialized(){
        return appContext != null;
    }

    /* Initialize the download manager by app appContext */
    static public void Init(Context context){
        appContext = context;
        queue = new LinkedBlockingQueue<>();
        history = new ArrayList<>();
        jobConsumerThread = new Thread(new Runnable(){
            @Override
            public void run(){
                while (true) {
                    try {
                        currentJob = queue.take();
                        currentJob.Execute();
                        history.add(0, currentJob);
                        currentJob = null;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

        });
        jobConsumerThread.start();
    }

    /* Get the total number of jobs in the queue */
    static public int GetJobCountInQueue () {
        return queue.size();
    }

    /* Get all jobs and their status */
    static public List<DownloadDC> GetAllJobStatus () {
        List<DownloadDC> ret = new ArrayList<DownloadDC>();
        if (currentJob != null) {
            ret.add(new DownloadDC(currentJob));
        }
        for (DownloadJoblet job : queue) {
            ret.add(new DownloadDC(job));
        }
        for (DownloadJoblet finishedJob : history) {
            ret.add(new DownloadDC(finishedJob));
        }
        return ret;
    }

    /* Add a new job to the job queue */
    static public void AddJob(SmbFile sourceFile, File destFolder) {
        try {
            DownloadJoblet newJob = new DownloadJoblet(appContext, sourceFile, destFolder, sourceFile.getName());
            queue.add(newJob);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /* Remove a job from the job queue */
    static public void RemoveJob(String jobId) {
        DownloadJoblet jobToRemove = null;
        if (currentJob != null && currentJob.getJobId().equals(jobId)){     //check current Job
            currentJob.MarkAsCancel();
        } else {                                                            //check queue
            for (DownloadJoblet job : queue) {
                if (job.getJobId().equals(jobId)) {
                    jobToRemove = job;
                    break;
                }
            }
            if (jobToRemove != null) {
                jobToRemove.setStatus(DownloadJoblet.DownloadStatus.CANCELLED);
                history.add(0, jobToRemove);
                queue.remove(jobToRemove);
            }
        }
    }

    /* Remove the job history from history array */
    static public void RemoveHistory (String jobId) {
        DownloadJoblet jobToRemove = null;
        for (DownloadJoblet job : history) {
            if (job.getJobId().equals(jobId)) {
                jobToRemove = job;
                break;
            }
        }
        if (jobToRemove != null) {
            history.remove(jobToRemove);
        }
    }
}
