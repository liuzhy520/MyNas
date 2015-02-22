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
    static private List<DownloadJoblet> history;            //finished jobs will be pushed to history array
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
                        if (currentJob != null) {
                            history.add(currentJob);
                        }
                        currentJob = queue.take();
                        currentJob.Execute();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

        });
        jobConsumerThread.start();
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
        if (currentJob != null && currentJob.getJobId().equals(jobId)){     //check current Job
            currentJob.Cancel();
            //TODO: continue taking from queue
        } else {                                                            //check queue
            for (DownloadJoblet job : queue) {
                if (job.getJobId().equals(jobId)) {
                    break;
                }
            }
            if (jobToRemove != null) {
                jobToRemove.Cancel();
                history.add(jobToRemove);   //need to manually put it into history list.
                queue.remove(jobToRemove);
            }
        }
    }
}
