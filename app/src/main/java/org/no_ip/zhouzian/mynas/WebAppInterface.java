package org.no_ip.zhouzian.mynas;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.widget.Toast;
import com.google.gson.Gson;

import org.no_ip.zhouzian.mynas.infrastructure.CifsDownloadManager;
import org.no_ip.zhouzian.mynas.infrastructure.CifsProfile;
import org.no_ip.zhouzian.mynas.infrastructure.CifsProfileManager;
import org.no_ip.zhouzian.mynas.infrastructure.MimeType;
import org.no_ip.zhouzian.mynas.infrastructure.StreamServer;

import java.io.File;

public class WebAppInterface {
    private Context appContext;
    private View webView;
    private ProgressDialog loadingDlg;
    private StreamServer sServer;

    /* Constructor. Initialize other manager classes */
    public WebAppInterface(Context context, View webView){
        this.appContext = context;
        this.webView = webView;
        loadingDlg = new ProgressDialog(context);
        loadingDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        loadingDlg.setCancelable(false);
        if (!CifsProfileManager.IsInitialized()){
            CifsProfileManager.Init(context);
        }
        if (!CifsDownloadManager.IsInitialized()){
            CifsDownloadManager.Init(context);
        }
    }
    @JavascriptInterface
    public String GetAllProfiles(){
        WebAppResponseStatus status = WebAppResponseStatus.SUCCESS;
        Object data = null;
        String errorMsg = null;
        try {
            data = CifsProfileManager.GetAllProfiles();
        } catch (Exception ex){
            status = WebAppResponseStatus.ERROR;
            errorMsg = ex.getMessage();
            Toast.makeText(appContext, errorMsg, Toast.LENGTH_LONG).show();
        }
        Gson gson = new Gson();
        WebAppResponse response = new WebAppResponse(status, data, errorMsg);
        return gson.toJson(response);
    }

    @JavascriptInterface
    public String AddProfile(String profileName, String rootUrl, int portNumber, String username, String password){
        WebAppResponseStatus status = WebAppResponseStatus.SUCCESS;
        Object data = null;
        String errorMsg = null;
        try {
            CifsProfile newProfile = new CifsProfile(profileName, rootUrl, portNumber, username, password);
            ShowLoading("Validating profile...");
            newProfile.validate();
            HideLoading();
            CifsProfileManager.AddProfile(newProfile);
        }catch(Exception ex){
            HideLoading();
            status = WebAppResponseStatus.ERROR;
            errorMsg = ex.getMessage();
            Toast.makeText(appContext, errorMsg, Toast.LENGTH_LONG).show();
        }
        Gson gson = new Gson();
        WebAppResponse response = new WebAppResponse(status, data, errorMsg);
        return gson.toJson(response);
    }

    @JavascriptInterface
    public String ModifyProfile(int profileId, String profileName, String rootUrl, int portNumber, String username, String password){
        WebAppResponseStatus status = WebAppResponseStatus.SUCCESS;
        Object data = null;
        String errorMsg = null;
        try{
            CifsProfile profile = new CifsProfile(profileName, rootUrl, portNumber, username, password);
            ShowLoading("Validating profile...");
            profile.validate();
            HideLoading();
            profile.setProfileId(profileId);
            CifsProfileManager.ModifyProfile(profile);
        }catch(Exception ex){
            HideLoading();
            status = WebAppResponseStatus.ERROR;
            errorMsg = ex.getMessage();
            Toast.makeText(appContext, errorMsg, Toast.LENGTH_LONG).show();
        }
        Gson gson = new Gson();
        WebAppResponse response = new WebAppResponse(status, data, errorMsg);
        return gson.toJson(response);
    }

    @JavascriptInterface
    public String RemoveProfileById(int profileId){
        WebAppResponseStatus status = WebAppResponseStatus.SUCCESS;
        Object data = null;
        String errorMsg = null;
        try{
            CifsProfileManager.RemoveProfileById(profileId);
        }catch(Exception ex){
            status = WebAppResponseStatus.ERROR;
            errorMsg = ex.getMessage();
            Toast.makeText(appContext, errorMsg, Toast.LENGTH_LONG).show();
        }
        Gson gson = new Gson();
        WebAppResponse response = new WebAppResponse(status, data, errorMsg);
        return gson.toJson(response);
    }

    @JavascriptInterface
    public String Browse(int profileId, String relativePath, String orderBy) {
        WebAppResponseStatus status = WebAppResponseStatus.SUCCESS;
        Object data = null;
        String errorMsg = null;
        try{
            CifsProfile profile = CifsProfileManager.GetProfileById(profileId);
            data = profile.browse(relativePath, orderBy);
        } catch (Exception ex) {
            status = WebAppResponseStatus.ERROR;
            errorMsg = ex.getMessage();
            Toast.makeText(appContext, errorMsg, Toast.LENGTH_LONG).show();
        }
        Gson gson = new Gson();
        WebAppResponse response = new WebAppResponse(status, data, errorMsg);
        return gson.toJson(response);
    }

    @JavascriptInterface
    public String Detail(int profileId, String relativePath) {
        WebAppResponseStatus status = WebAppResponseStatus.SUCCESS;
        Object data = null;
        String errorMsg = null;
        try{
            CifsProfile profile = CifsProfileManager.GetProfileById(profileId);
            data = profile.getDetail(relativePath);
        } catch (Exception ex) {
            status = WebAppResponseStatus.ERROR;
            errorMsg = ex.getMessage();
            Toast.makeText(appContext, errorMsg, Toast.LENGTH_LONG).show();
        }
        Gson gson = new Gson();
        WebAppResponse response = new WebAppResponse(status, data, errorMsg);
        return gson.toJson(response);
    }

    @JavascriptInterface
    public void CreateProfileShortcut (int profileId, String relativePath) {
        try{
            String newProfileName = CifsProfileManager.CreateProfileShortCut(profileId, relativePath);
            Toast.makeText(appContext, "New profile: \"" + newProfileName + "\" is created.", Toast.LENGTH_LONG).show();
        } catch (Exception ex) {
            Toast.makeText(appContext, ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @JavascriptInterface
    public void StreamFile (int profileId, String relativePath) {
        try{
            CifsProfile profile = CifsProfileManager.GetProfileById(profileId);
            if (sServer != null) {      //stop previous thread.
                sServer.stop();
            }
            sServer = new StreamServer();
            sServer.init("127.0.0.1");
            Uri uri = profile.streamFile(sServer, relativePath);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, MimeType.GetMimeType(relativePath));
            Intent chooser = Intent.createChooser(intent, "Play with ...");
            appContext.startActivity(chooser);
        } catch (Exception ex) {
            Toast.makeText(appContext, ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @JavascriptInterface
    public void OpenFile (int profileId, String relativePath) {
        try {
            CifsProfile profile = CifsProfileManager.GetProfileById(profileId);
            ShowLoading("Loading file ...");
            File downloadedFile = profile.downloadFileSync(appContext, relativePath);
            HideLoading();
            if (downloadedFile.exists()) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(downloadedFile), MimeType.GetMimeType(relativePath));
                Intent chooser = Intent.createChooser(intent, "Play with ...");
                appContext.startActivity(chooser);
            } else {
                throw new Exception("No file is downloaded.");
            }
        } catch (Exception ex) {
            Toast.makeText(appContext, ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @JavascriptInterface
    public void DownloadFile (int profileId, String relativePath) {
        try{
            CifsProfile profile = CifsProfileManager.GetProfileById(profileId);
            profile.downloadFileAsync(relativePath);
            Toast.makeText(appContext, "Download will be scheduled shortly.", Toast.LENGTH_LONG).show();
        } catch (Exception ex) {
            Toast.makeText(appContext, ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @JavascriptInterface
    public String GetAllDownloads () {
        WebAppResponseStatus status = WebAppResponseStatus.SUCCESS;
        Object data = null;
        String errorMsg = null;
        try{
            data = CifsDownloadManager.GetAllJobStatus();
        } catch (Exception ex) {
            status = WebAppResponseStatus.ERROR;
            errorMsg = ex.getMessage();
            Toast.makeText(appContext, errorMsg, Toast.LENGTH_LONG).show();
        }
        Gson gson = new Gson();
        WebAppResponse response = new WebAppResponse(status, data, errorMsg);
        return gson.toJson(response);
    }

    @JavascriptInterface
    public String GetDownloadCount () {
        WebAppResponseStatus status = WebAppResponseStatus.SUCCESS;
        Object data = null;
        String errorMsg = null;
        try{
            data = CifsDownloadManager.GetJobCount();
        } catch (Exception ex) {
            status = WebAppResponseStatus.ERROR;
            errorMsg = ex.getMessage();
            Toast.makeText(appContext, errorMsg, Toast.LENGTH_LONG).show();
        }
        Gson gson = new Gson();
        WebAppResponse response = new WebAppResponse(status, data, errorMsg);
        return gson.toJson(response);
    }

    @JavascriptInterface
    public void OpenDownloadedFile (String jobId) {
        try {
            File file = CifsDownloadManager.GetFileFromHistory(jobId);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(file), MimeType.GetMimeType(file.getName()));
            Intent chooser = Intent.createChooser(intent, "Play with ...");
            appContext.startActivity(chooser);
        } catch (Exception ex) {
            Toast.makeText(appContext, ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @JavascriptInterface
    public void StopJob (String jobId) {
        CifsDownloadManager.RemoveJob(jobId);
    }

    @JavascriptInterface
    public void RemoveHistory (String jobId) {
        CifsDownloadManager.RemoveHistory(jobId);
    }

    @JavascriptInterface
    public void HapticFeedback () {
        webView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
    }

    @JavascriptInterface
    public void ExitApp () {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        appContext.startActivity(intent);
    }

    private void ShowLoading(final String msg){
        ((Activity)appContext).runOnUiThread(new Runnable(){
            @Override
            public void run() {
                loadingDlg.setMessage(msg);
                loadingDlg.show();
            }
        });
    }

    private void HideLoading(){
        ((Activity)appContext).runOnUiThread(new Runnable(){
            @Override
            public void run() {
                loadingDlg.dismiss();
            }
        });
    }

    private class WebAppResponse {
        private WebAppResponseStatus status;        //SUCCESS or ERROR
        private Object data;                        //serialized return object
        private String errorMsg;                    //error message

        /* Constructor
         * If response is success, errorMsg should be null
         * If response is error, data should be null */
        public WebAppResponse(WebAppResponseStatus status, Object data, String errorMsg){
            this.status = status;
            this.data = data;
            this.errorMsg = errorMsg;
        }
    }

    private enum WebAppResponseStatus{
        SUCCESS,
        ERROR
    }
}
