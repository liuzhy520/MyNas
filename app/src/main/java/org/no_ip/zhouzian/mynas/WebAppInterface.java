package org.no_ip.zhouzian.mynas;

import android.content.Context;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import com.google.gson.Gson;
import org.no_ip.zhouzian.mynas.infrastructure.CifsProfile;
import org.no_ip.zhouzian.mynas.infrastructure.CifsProfileManager;
import java.util.List;

public class WebAppInterface {
    private Context appContext;

    /* Constructor. Initialize other manager classes */
    public WebAppInterface(Context context){
        appContext = context;
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
    public String AddProfile(String profileName, String rootUrl, String username, String password, boolean isSsl){
        WebAppResponseStatus status = WebAppResponseStatus.SUCCESS;
        Object data = null;
        String errorMsg = null;
        try {
            CifsProfile newProfile = new CifsProfile(profileName, rootUrl, username, password, isSsl);
            newProfile.validate();
            CifsProfileManager.AddProfile(newProfile);
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
    public String ModifyProfile(int profileId, String profileName, String rootUrl, String username, String password, boolean isSsl){
        WebAppResponseStatus status = WebAppResponseStatus.SUCCESS;
        Object data = null;
        String errorMsg = null;
        try{
            CifsProfile profile = new CifsProfile(profileName, rootUrl, username, password, isSsl);
            profile.validate();
            profile.setProfileId(profileId);
            CifsProfileManager.ModifyProfile(profile);
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
