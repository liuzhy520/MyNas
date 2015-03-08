/* Profile controller */
app.controller('profileSettingsCtrl', ['$scope', function($scope){
    $scope.showDetail = false;		//true means displaying detail info of a profile; false means displaying a list of profiles
	function reloadProfiles (){
		 var response = JSON.parse(webAppInterface.GetAllProfiles());
		 if (response.status == 'SUCCESS'){
			$scope.profiles = response.data;
		 } else {
			$scope.profiles = [];
		 }
	}
	reloadProfiles();
    $scope.curProfile = null;		//In detail view, caches a deep copy of selected profile instance

	/* Changes the view to detail view */
    $scope.viewDetail = function(profile){
        $scope.showDetail = true;
        $scope.curProfile = angular.copy(profile);
		Global.setBackHandler($scope.goToListView);
    }

	/* Returns to the profile list view */
    $scope.goToListView = function(noApply){
		reloadProfiles();
        $scope.showDetail = false;
		Global.resetBackHandler();
		if (!noApply) {
			$scope.$apply();
		}
    }

	/* Goes to detail view. No profile is selected. New empty profile template is used. */
    $scope.goToNewProfile = function(){
        $scope.showDetail = true;
        $scope.curProfile = {profileId: -1,
                            profileName: '',
							domain: '',
                            username: '',
                            password: '',
                            rootUrl: '',
							portNumber: 445};
		Global.setBackHandler($scope.goToListView);
    }

	/* Calls Java code to create a new profile */
    $scope.createProfile = function(){
		document.activeElement.blur()	//hide keyboard
		var response = JSON.parse(webAppInterface.AddProfile($scope.curProfile.profileName, $scope.curProfile.rootUrl, $scope.curProfile.portNumber, $scope.curProfile.domain, $scope.curProfile.username, $scope.curProfile.password));
		$scope.wait = false;
		if (response.status == 'SUCCESS'){
			$scope.goToListView(true);
		}
    }

	/* Calls Java code to modify the current selected profile */
    $scope.saveProfile = function(){
		document.activeElement.blur()	//hide keyboard
		var response = JSON.parse(webAppInterface.ModifyProfile($scope.curProfile.profileId, $scope.curProfile.profileName, $scope.curProfile.rootUrl, $scope.curProfile.portNumber, $scope.curProfile.domain, $scope.curProfile.username, $scope.curProfile.password));
		$scope.wait = false;
		if (response.status == 'SUCCESS'){
			$scope.goToListView(true);
		}
    }

	/* Calls Java code to delete the current selected profile */
    $scope.deleteProfile = function(){
		document.activeElement.blur()	//hide keboard
		var response = JSON.parse(webAppInterface.RemoveProfileById($scope.curProfile.profileId));
		if (response.status == 'SUCCESS'){
			$scope.goToListView(true);
		}
    }
}]);