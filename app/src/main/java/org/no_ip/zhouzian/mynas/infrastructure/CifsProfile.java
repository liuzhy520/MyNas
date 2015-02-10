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
        this.profileName = profileName.trim();
        this.rootUrl = rootUrl.trim();
        this.username = username.trim();
        this.password = password.trim();
        this.isSsl = isSsl;
    }

    /* ProfileId will be reassigned when saved to profile manager
     * unsaved profile has id == -1 */
    public boolean isPersisted(){
        return profileId != -1;
    }

    /* Validate if the profile is correct.
     * Empty profile name or root url is not allowed
     * Duplicated profile name is checked by profile manager */
    public void validate () throws CifsProfileException{
        if (profileName.isEmpty()){
            throw new CifsProfileException("Profile name cannot be empty");
        } else if (rootUrl.isEmpty()){
            throw new CifsProfileException("Root url cannot be empty");
        }
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
        this.profileName = profileName.trim();
    }

    public String getRootUrl() {
        return rootUrl;
    }

    public void setRootUrl(String rootUrl) {
        this.rootUrl = rootUrl.trim();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username.trim();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password.trim();
    }

    public boolean isSsl() {
        return isSsl;
    }

    public void setSsl(boolean isSsl) {
        this.isSsl = isSsl;
    }
}
