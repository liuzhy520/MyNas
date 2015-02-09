package org.no_ip.zhouzian.mynas;

import android.content.Context;
import android.support.annotation.NonNull;
import android.webkit.JavascriptInterface;

import com.google.gson.Gson;

import org.no_ip.zhouzian.mynas.infrastructure.CifsProfile;
import org.no_ip.zhouzian.mynas.infrastructure.CifsProfileManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

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
        List<CifsProfile> profiles = CifsProfileManager.GetAllProfiles();
        /* dummy data for testing */
//        List<CifsProfile> profiles = new ArrayList<>();
//        CifsProfile profile1 = new CifsProfile("Zurich", "//192.168.0.1/nas_1", "zhouzian", "1982", false);
//        CifsProfile profile2 = new CifsProfile("Release92", "//192.168.0.1/nas_2", "tbd", "1983", false);
//        CifsProfile profile3 = new CifsProfile("v-tor-dev-lfs", "//192.168.0.1/nas_3", "jy", "1984", false);
//        profiles.add(profile1);
//        profiles.add(profile2);
//        profiles.add(profile3);
        /* end of dummy data */
        Gson gson = new Gson();
        return gson.toJson(profiles);
    }

    @JavascriptInterface
    public void AddProfile(String profileName, String rootUrl, String username, String password, boolean isSsl){
        CifsProfile newProfile = new CifsProfile(profileName, rootUrl, username, password, isSsl);
        try {
            CifsProfileManager.AddProfile(newProfile);
        }catch(Exception ex){
            ex.printStackTrace();
            //TODO: error handling here
        }
    }

    @JavascriptInterface
    public void ModifyProfile(int profileId, String profileName, String rootUrl, String username, String password, boolean isSsl){
        CifsProfile profile = new CifsProfile(profileName, rootUrl, username, password, isSsl);
        profile.setProfileId(profileId);
        try{
            CifsProfileManager.ModifyProfile(profile);
        }catch(Exception ex){
            ex.printStackTrace();
            //TODO: error handling here
        }
    }

    @JavascriptInterface
    public void RemoveProfileById(int profileId){
        try{
            CifsProfileManager.RemoveProfileById(profileId);
        }catch(Exception ex){
            ex.printStackTrace();
            //TODO: error handling here
        }
    }
}
