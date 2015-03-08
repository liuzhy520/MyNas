/* Navigation bar controller */
app.controller('navCtrl', ['$scope', function($scope){		//Navigation controller
	var APP_TITLE = 'My Home Cloud';
	/* List of available menus */
    $scope.tabs = [{display_name: APP_TITLE,
					name: 'MAIN',
					idx: 0,
					profileCnt: 0,
					selected: true},
					{display_name: 'Downloads',
					name: 'DOWNLOAD',
					idx: 1,
					activeCnt: 0,		//number of active joblets including pending joblets
					totalCnt: 0,		//number of total joblets including terminated and cancelled joblets
					selected: false},
					{display_name: 'Settings',
					name: 'SETTINGS',
                    idx: 2,
                    selected: false},
                    {display_name: 'About',
					name: 'ABOUT',
                    idx: 3,
                    selected: false}];
	/* utility function returns a reference to the tab specified by unique name */
	function findTabByName (name) {
		var idx = 0;
		_.forEach($scope.tabs, function(tab){
            if (tab.name == name) {
                idx = tab.idx;
            }
        });
		return $scope.tabs[idx];
	}
	/* Select a tab by its idx */
    $scope.selectTab = function(name){
        _.forEach($scope.tabs, function(tab){
            tab.selected = false;
        });
        var selectedTab = findTabByName(name);
		selectedTab.selected = true;
		Global.resetBackHandler();		//when back is clicked, return to the main tab.
        $('#main_nav_bar').collapse('hide');
    }

	/* update downloading count when the menu is opened */
	$('.navbar-collapse').on('shown.bs.collapse', function() {
		var response = JSON.parse(webAppInterface.GetDownloadCount());
		if (response.status == 'SUCCESS'){
			var downloadTab = findTabByName('DOWNLOAD');
			downloadTab.activeCnt = response.data.activeCnt;
			downloadTab.totalCnt = response.data.totalCnt;
		} else {
			// do nothing
		}

		response = JSON.parse(webAppInterface.GetProfileCount());
		if (response.status == 'SUCCESS'){
			var mainTab = findTabByName('MAIN');
			mainTab.profileCnt = response.data;
		} else {
			// do nothing
		}

		$scope.$apply();
	});

	/* Returns the current selected tab title */
    $scope.getCurTitle = function(){
        var curTitle = APP_TITLE;
        _.forEach($scope.tabs, function(tab){
            if (tab.selected) {
                curTitle = tab.display_name;
            }
        });
        return curTitle;
    }
	
	/* Returns the current selected tab unique name */
	$scope.getCurName = function(){
		var curName = 'MAIN';
        _.forEach($scope.tabs, function(tab){
            if (tab.selected) {
                curName = tab.name;
            }
        });
        return curName;
	}
}]);