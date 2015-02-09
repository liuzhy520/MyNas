package org.no_ip.zhouzian.mynas.infrastructure;

public class CifsProfile {
    private int profileId;
    private String profileName;
    private String rootUrl;
    private String username;
    private String password;
    private boolean isSsl;

    public CifsProfile (String profileName, String rootUrl, String username, String password, boolean isSsl){
        this.profileId = -1;
        this.profileName = profileName;
        this.rootUrl = rootUrl;
        this.username = username;
        this.password = password;
        this.isSsl = isSsl;
    }

    /* ProfileId will be reassigned when saved to profile manager
     * unsaved profile has id == -1 */
    public boolean IsPersisted (){
        return profileId != -1;
    }

    /* Validate if the profile is correct.
     * Empty profile name or root url is not allowed
     * Duplicated profile name is checked by profile manager */
    public boolean IsValidated (){
        if (profileName.isEmpty() || rootUrl.isEmpty()) return false;
        else return true;
        // TODO: add code to try to connect to the shared folder for connection validation
    }

    public int getProfileId() {
        return profileId;
    }

    public void setProfileId(int profileId) {
        this.profileId = profileId;
    }

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public String getRootUrl() {
        return rootUrl;
    }

    public void setRootUrl(String rootUrl) {
        this.rootUrl = rootUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isSsl() {
        return isSsl;
    }

    public void setSsl(boolean isSsl) {
        this.isSsl = isSsl;
    }
}
