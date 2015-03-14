package org.no_ip.zhouzian.mynas.infrastructure;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

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
	static private String PREF_NAME = "download_history";      //name of preference
	static private String KEY_NAME = "download_history_key";   //key of preference

	private CifsDownloadManager() {
	}

	/* Check if the class has been initialized */
	static public boolean IsInitialized() {
		return appContext != null;
	}

	/* Initialize the download manager by app appContext */
	static public void Init(Context context) {
		appContext = context;
		queue = new LinkedBlockingQueue<>();
		history = new ArrayList<>();
		LoadHistory();      //load history array from shared preference
		jobConsumerThread = new Thread(new Runnable() {
			@Override
			public void run() {
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

	/* Get the number of jobs */
	static public DownloadJobCountDC GetJobCount() {
		DownloadJobCountDC ret = new DownloadJobCountDC();
		int downloadingCnt = currentJob == null ? 0 : 1;
		int pendingCnt = queue.size();
		int historyCnt = history.size();
		ret.setActiveCnt(downloadingCnt + pendingCnt);
		ret.setTotalCnt(downloadingCnt + pendingCnt + historyCnt);
		return ret;
	}

	/* Get all jobs and their status */
	static public List<DownloadDC> GetAllJobStatus() {
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
		if (currentJob != null && currentJob.getJobId().equals(jobId)) {     //check current Job
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
	static public void RemoveHistory(String jobId) {
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

	/* Try to open the downloaded file in history */
	static public File GetFileFromHistory(String jobId) throws Exception {
		DownloadJoblet jobToOpen = null;
		for (DownloadJoblet job : history) {
			if (job.getJobId().equals(jobId)) {
				jobToOpen = job;
				break;
			}
		}
		if (jobToOpen != null) {
			File file = new File(jobToOpen.getDestFolder(), jobToOpen.getDestFileName());
			if (file.exists()) {
				return file;
			} else {
				throw new Exception("Cannot find the downloaded file.");
			}
		} else {
			throw new Exception("Job Id does not exist.");
		}
	}

	/* Load download history from shared preference */
	static private void LoadHistory() {
		Gson gson = new Gson();
		SharedPreferences sharedPref = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
		String savedProfiles = sharedPref.getString(KEY_NAME, "[]");
		List<DownloadJobletSerializable> serializableHistory = gson.fromJson(savedProfiles, new TypeToken<ArrayList<DownloadJobletSerializable>>() {
		}.getType());
		for (DownloadJobletSerializable serializableJoblet : serializableHistory) {
			history.add(serializableJoblet.CreateJobletHistory());
		}
	}

	/* Write download history to shared preference. It should be called when the application exits */
	static public void Sync() {
		List<DownloadJobletSerializable> serializableHistory = new ArrayList<DownloadJobletSerializable>();
		for (DownloadJoblet joblet : history) {
			serializableHistory.add(new DownloadJobletSerializable(joblet));
		}
		Gson gson = new Gson();
		SharedPreferences sharedPref = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.clear();
		editor.putString(KEY_NAME, gson.toJson(serializableHistory));
		editor.commit();
	}
}
