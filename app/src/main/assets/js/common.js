var app = angular.module('myNasApp', ['ui.checkbox']);		//Bootstrap angular app. Declare app module

/* Navigation bar controller */
app.controller('navCtrl', ['$scope', function($scope){		//Navigation controller
	var APP_TITLE = 'My Nas';
    $scope.tabs = [{display_name: APP_TITLE,
					idx: 0,
					selected: true},
					{display_name: 'Settings',
                    idx: 1,
                    selected: false},
                    {display_name: 'About',
                    idx: 2,
                    selected: false}];
    $scope.selectTab = function(idx){
        _.forEach($scope.tabs, function(tab){
            tab.selected = false;
        });
        $scope.tabs[idx].selected = true;
        $('#main_nav_bar').collapse('hide');
    }
    $scope.getCurTitle = function(){
        var curTitle = APP_TITLE;
        _.forEach($scope.tabs, function(tab){
            if (tab.selected) {
                curTitle = tab.display_name;
            }
        });
        return curTitle;
    }
}]);

/* Main page controller */
app.controller('nasMainCtrl', ['$scope', function($scope){
	$scope.browseProfiles = true;
	function reloadProfiles (){
		 var response = JSON.parse(webAppInterface.GetAllProfiles());
		 if (response.status == 'SUCCESS'){
			$scope.profiles = response.data;
		 } else {
			$scope.profiles = [];
		 }
	}
	reloadProfiles();
	
}]);

/* Profile controller */
app.controller('profileSettingsCtrl', ['$scope', function($scope){
    $scope.showDetail = false;
	function reloadProfiles (){
		 var response = JSON.parse(webAppInterface.GetAllProfiles());
		 if (response.status == 'SUCCESS'){
			$scope.profiles = response.data;
		 } else {
			$scope.profiles = [];
		 }
	}
	reloadProfiles();
    $scope.curProfile = null;
    $scope.viewDetail = function(profile){
        $scope.showDetail = true;
        $scope.curProfile = angular.copy(profile);
    }
    $scope.goToListView = function(){
		reloadProfiles();
        $scope.showDetail = false;
    }
    $scope.goToNewProfile = function(){
        $scope.showDetail = true;
        $scope.curProfile = {profileId: -1,
                            profileName: '',
                            username: '',
                            password: '',
                            rootUrl: '',
							portNumber: 445,
                            isSsl: false};
    }
    $scope.createProfile = function(){
		document.activeElement.blur()	//hide keyboard
		var response = JSON.parse(webAppInterface.AddProfile($scope.curProfile.profileName, $scope.curProfile.rootUrl, $scope.curProfile.portNumber, $scope.curProfile.username, $scope.curProfile.password, $scope.curProfile.isSsl));
		$scope.wait = false;
		if (response.status == 'SUCCESS'){
			$scope.goToListView();
		}
    }
    $scope.saveProfile = function(){
		document.activeElement.blur()	//hide keyboard
		var response = JSON.parse(webAppInterface.ModifyProfile($scope.curProfile.profileId, $scope.curProfile.profileName, $scope.curProfile.rootUrl, $scope.curProfile.portNumber, $scope.curProfile.username, $scope.curProfile.password, $scope.curProfile.isSsl));
		$scope.wait = false;
		if (response.status == 'SUCCESS'){
			$scope.goToListView();
		}
    }
    $scope.deleteProfile = function(){
		document.activeElement.blur()	//hide keboard
		var response = JSON.parse(webAppInterface.RemoveProfileById($scope.curProfile.profileId));
		if (response.status == 'SUCCESS'){
			$scope.goToListView();
		}
    }
}]);