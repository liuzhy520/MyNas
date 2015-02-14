package org.no_ip.zhouzian.mynas;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import com.google.gson.Gson;
import org.no_ip.zhouzian.mynas.infrastructure.CifsProfile;
import org.no_ip.zhouzian.mynas.infrastructure.CifsProfileManager;
import java.util.List;

public class WebAppInterface {
    private Context appContext;
    private ProgressDialog loadingDlg;

    /* Constructor. Initialize other manager classes */
    public WebAppInterface(Context context){
        appContext = context;
        loadingDlg = new ProgressDialog(context);
        loadingDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        loadingDlg.setCancelable(false);
        if (!CifsProfileManager.IsInitialized()){
            CifsProfileManager.Init(context);
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
    public String AddProfile(String profileName, String rootUrl, int portNumber, String username, String password, boolean isSsl){
        WebAppResponseStatus status = WebAppResponseStatus.SUCCESS;
        Object data = null;
        String errorMsg = null;
        try {
            CifsProfile newProfile = new CifsProfile(profileName, rootUrl, portNumber, username, password, isSsl);
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
    public String ModifyProfile(int profileId, String profileName, String rootUrl, int portNumber, String username, String password, boolean isSsl){
        WebAppResponseStatus status = WebAppResponseStatus.SUCCESS;
        Object data = null;
        String errorMsg = null;
        try{
            CifsProfile profile = new CifsProfile(profileName, rootUrl, portNumber, username, password, isSsl);
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
