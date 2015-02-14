package org.no_ip.zhouzian.mynas.infrastructure;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

public class CifsProfileManager {
    static private String PREF_NAME = "cifs_profiles";      //name of preference
    static private String KEY_NAME = "cifs_profiles_key";   //key of preference
    static private ArrayList<CifsProfile> profiles = new ArrayList<CifsProfile>();      //cache
    static private Context appContext;     //current app appContext
    private CifsProfileManager(){}

    /* Check if the class has been initialized */
    static public boolean IsInitialized(){
        return appContext != null;
    }

    /* Initialize the profile manager by app appContext */
    static public void Init(Context context){
        CifsProfileManager.appContext = context;
        Load();
    }

    /* Returns a list of cifs profiles from cache */
    static public List<CifsProfile> GetAllProfiles(){
        return profiles;
    }

    /* Returns a specific profile. Search by profileId.
     * Returns null if the profile cannot be found */
    static public CifsProfile GetProfileById(int id){
        for (CifsProfile profile : profiles){
            if (profile.getProfileId() == id){
                return profile;
            }
        }
        return null;    //didn't found the profile specified by id
    }

    /* Remove a profile by profileId */
    static public void RemoveProfileById (int id) throws CifsProfileException {
        CifsProfile profile = GetProfileById(id);
        if (profile != null){
            profiles.remove(profile);
            Commit();
        }
        else {
            throw new CifsProfileException("Cannot find the profile to remove.");
        }
    }

    /* Modify the profile */
    static public void ModifyProfile(CifsProfile profile) throws CifsProfileException {
        CifsProfile tProfile = GetProfileById(profile.getProfileId());
        if (tProfile != null){
            if (!IsProfileNameDup(profile.getProfileName(), tProfile.getProfileId())) {
                tProfile.setProfileName(profile.getProfileName());
                tProfile.setRootUrl(profile.getRootUrl());
                tProfile.setPortNumber(profile.getPortNumber());
                tProfile.setUsername(profile.getUsername());
                tProfile.setPassword(profile.getPassword());
                tProfile.setSsl(profile.isSsl());
                Commit();
            } else {
                throw new CifsProfileException("Profile name cannot be duplicated");
            }
        }
        else{
            throw new CifsProfileException("Cannot find the profile to modify.");
        }
    }

    /* Add the new profile to the profile list and commit to the shared preferences */
    static public void AddProfile(CifsProfile profile) throws CifsProfileException {
        if (!IsProfileNameDup(profile.getProfileName(), -1)){        //profile name cannot be duplicated
            profile.setProfileId(GetNextId());
            profiles.add(profile);
            Commit();
        }
        else{
            throw new CifsProfileException("Profile name cannot be duplicated");
        }
    }

    /* Load profiles from shared preferences */
    static private void Load(){
        Gson gson = new Gson();
        SharedPreferences sharedPref = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String savedProfiles = sharedPref.getString(KEY_NAME, "[]");
        profiles = gson.fromJson(savedProfiles, new TypeToken < ArrayList < CifsProfile >>() {}.getType());
    }

    /* Persist profiles to shared preferences */
    static private void Commit(){
        Gson gson = new Gson();
        SharedPreferences sharedPref = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.clear();
        editor.putString(KEY_NAME, gson.toJson(profiles));
        editor.commit();
    }

    /* Check if the given new profile name is duplicated with existing ones
     * Once user is modifying a profile, ignoreProfileId specify the profile id
     * If user is adding a new profile, ignoreProfileId should be set to be -1 */
    static private boolean IsProfileNameDup(String pName, int ignoreProfileId){
        boolean ret = false;
        for (CifsProfile profile : profiles){
            if (profile.getProfileId() != ignoreProfileId && profile.getProfileName().equalsIgnoreCase(pName.trim())){
                ret = true;
            }
        }
        return ret;
    }

    /* Get the next available profileId. Profile id cannot be duplicated */
    static private int GetNextId(){
        int ret = -1;
        for(CifsProfile profile : profiles){
            if (profile.getProfileId() > ret){
                ret = profile.getProfileId();
            }
        }
        return ret + 1;
    }
}
