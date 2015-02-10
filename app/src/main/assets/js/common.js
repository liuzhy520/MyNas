$(document).ready(function(){
	$.material.init();      //Initialize material.js
	//$.material.ripples();	//Add ripple effects
});

var app = angular.module('myNasApp', []);		//Bootstrap angular app. Declare app module

app.controller('navCtrl', ['$scope', function($scope){		//Navigation controller
    $scope.tabs = [{display_name: 'Settings',
                    idx: 0,
                    selected: false},
                    {display_name: 'About',
                    idx: 1,
                    selected: false}];
    $scope.selectTab = function(idx){
        _.forEach($scope.tabs, function(tab){
            tab.selected = false;
        });
        $scope.tabs[idx].selected = true;
        $('#main_nav_bar').collapse('hide');
    }
    $scope.getCurTitle = function(){
        var curTitle = 'My Nas';
        _.forEach($scope.tabs, function(tab){
            if (tab.selected) {
                curTitle = tab.display_name;
            }
        });
        return curTitle;
    }
}]);

app.controller('profileSettingsCtrl', ['$scope', function($scope){		//Profile controller
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
                            isSsl: false};
    }
    $scope.createProfile = function(){
		document.activeElement.blur()	//hide keyboard
		var response = JSON.parse(webAppInterface.AddProfile($scope.curProfile.profileName, $scope.curProfile.rootUrl, $scope.curProfile.username, $scope.curProfile.password, $scope.curProfile.isSsl));
		if (response.status == 'SUCCESS'){
			$scope.goToListView();
		}
    }
    $scope.saveProfile = function(){
		document.activeElement.blur()	//hide keyboard
		var response = JSON.parse(webAppInterface.ModifyProfile($scope.curProfile.profileId, $scope.curProfile.profileName, $scope.curProfile.rootUrl, $scope.curProfile.username, $scope.curProfile.password, $scope.curProfile.isSsl));
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