/* Global callback functions that can be accessed by Java code */
var Global = function(){
	var backHandler = null;		//function handler called when hardware Back button is clicked
	var menuHandler = null;		//function handler called when hardware menu button is clicked
	return {
		setBackHandler: function(funcRef){
			backHandler = funcRef;
		},
		resetBackHandler: function() {
			backHandler = null;
		},
		onBackClicked: function(){
			if ($('.navbar-collapse').hasClass('in')) {		//always first try to close the opened nav menu
				menuHandler();
			} else {
				if (typeof backHandler == 'function') {
					backHandler();
				}
			}
		},
		setMenuHandler: function(funcRef){
			menuHandler = funcRef;
		},
		resetMenuHandler: function() {
			menuHandler = null;
		},
		onMenuClicked: function(){
			if (typeof menuHandler == 'function') {
				menuHandler();
			}
		}
	}
}();

Global.setMenuHandler(function(){
	$('button.navbar-toggle').trigger('click');
});

var app = angular.module('myNasApp', ['ui.checkbox']);		//Bootstrap angular app. Declare app module

/* ng-long-press directive */
app.directive('onLongPress', function($timeout) {
	return {
		restrict: 'A',
		link: function($scope, $elm, $attrs) {
			$elm.bind('touchstart', function(evt) {
				// Locally scoped variable that will keep track of the long press
				$scope.longPress = true;
 
				// We'll set a timeout for 600 ms for a long press
				$timeout(function() {
					if ($scope.longPress) {
						// If the touchend event hasn't fired,
						// apply the function given in on the element's on-long-press attribute
						$scope.$apply(function() {
							$scope.$eval($attrs.onLongPress)
						});
					}
				}, 600);
			});
 
			$elm.bind('touchend', function(evt) {
				// Prevent the onLongPress event from firing
				$scope.longPress = false;
				// If there is an on-touch-end function attached to this element, apply it
				if ($attrs.onTouchEnd) {
					$scope.$apply(function() {
						$scope.$eval($attrs.onTouchEnd)
					});
				}
			});
			
			$elm.bind('touchmove', function(e) { $scope.longPress = false; });
		}
	};
});

/* Navigation bar controller */
app.controller('navCtrl', ['$scope', function($scope){		//Navigation controller
	var APP_TITLE = 'My Home Cloud';
	/* List of available menus */
    $scope.tabs = [{display_name: APP_TITLE,
					idx: 0,
					selected: true},
					{display_name: 'Downloads',
					idx: 1,
					activeCnt: 0,		//number of active joblets including pending joblets
					totalCnt: 0,		//number of total joblets including terminated and cancelled joblets
					selected: false},
					{display_name: 'Settings',
                    idx: 2,
                    selected: false},
                    {display_name: 'About',
                    idx: 3,
                    selected: false},
					{display_name: 'Exit',
					idx: 4,
					selected: false}];
	/* Select a tab by its idx */
    $scope.selectTab = function(idx){
		if (idx == 4) {		//4 is for exit
			webAppInterface.ExitApp();
			return;
		}	
        _.forEach($scope.tabs, function(tab){
            tab.selected = false;
        });
        $scope.tabs[idx].selected = true;
        $('#main_nav_bar').collapse('hide');		
    }
	
	/* update downloading count when the menu is opened */
	$('.navbar-collapse').on('shown.bs.collapse', function() {
		var response = JSON.parse(webAppInterface.GetDownloadCount());
		if (response.status == 'SUCCESS'){
			$scope.tabs[1].activeCnt = response.data.activeCnt;
			$scope.tabs[1].totalCnt = response.data.totalCnt;
			$scope.$apply();
		} else {
			// do nothing
		}
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
}]);

/* Main page controller */
app.controller('nasMainCtrl', ['$scope', function($scope){
	$scope.connected = false;		//true means browse view; false means profile list view
	$scope.profiles = [];			//list of pre-configured profiles
	$scope.docTree = [];			//list of entries in the current browse view.
	$scope.curEntryDetail = {}		//currently selected entry detail model
	var relativePath = [];			//list of folder paths leading to current browse view
	var selectedEntry = null;		//current selected entry name
	$scope.selectedEntryIsFile = true;	//true is file; false is directory
	$scope.selectedEntryCanPlay = false;	//true if the file can be streamed
	$scope.selectedEntryCanOpen = false;	//tru if the file can be downloaded and opened
	var selectedProfileId = null;	//current profile id in the browse view;
	var backEntry = {name: 'Up ...', command: 'up'}		//the first element in the docTree. Used for goes back
	$scope.orderBy = 'typeAsc';		//default sort
	
	/* Order by options */
	$scope.sortOpts = [{label: 'Name Asc', value: 'nameAsc'},
						{label: 'Name Desc', value: 'nameDesc'},
						{label: 'Date Modified Asc', value: 'dateAsc'},
						{label: 'Date Modified Desc', value: 'dateDesc'},
						{label: 'Folder First', value: 'typeAsc'},
						{label: 'File First', value: 'typeDesc'}];
	
	function reloadProfiles (){
		 var response = JSON.parse(webAppInterface.GetAllProfiles());
		 if (response.status == 'SUCCESS'){
			$scope.profiles = response.data;
		 } else {
			$scope.profiles = [];
		 }
	}
	function isModalOpened (){
		return $('.modal:visible').length > 0;
	}
	function closeModal() {
		$('.modal').modal('hide');
	}
	function removeTailingSlash(entryName) {
		if (_.isString(entryName) && _.endsWith(entryName, '/')) {
			return entryName.substring(0, entryName.length - 1);
		} else {
			return entryName;
		}
	}
	var streamTypes = ['mp3', 'aac', 'ogg', 'flac', 'mp4', 'avi'];		//list of streamable file types
	var openTypes = ['pdf', 'jpg', 'jpeg'];											//list of openable file types
	function canStream(entryName) {
		var ext = entryName.split('.').pop().toLowerCase();
		return _.includes(streamTypes, ext);
	}
	function canOpen(entryName) {
		var ext = entryName.split('.').pop().toLowerCase();
		return _.includes(openTypes, ext);
	}
	reloadProfiles();
	
	$scope.connectProfile = function(profileId){
		relativePath = [];	//need to reset the relative path array.
		selectedProfileId = profileId;
		if ($scope.loadDocTree()) {
			$scope.connected = true;
			Global.setBackHandler($scope.goTo);
		} else {
			selectedProfileId = null;
		}
	}
	
	/* Only used in browse view. Open the selected entry under the current profile, or go back to the parent folder */
	$scope.goTo = function(entry) {
		if (entry == null || entry.command == 'up') {		//go back. When called from hardware back button, entry is null; When called from up soft button, entry is an smbEntry
			if (isModalOpened()) {	//close popup menu dialog when Back hardware is clicked
				closeModal();
			}
			else if (relativePath.length == 0) {
				$scope.disconnect();
				if (entry == null) {
					$scope.$apply();
				}
			}
			else if (relativePath.length != 0) {
				var last = relativePath.pop();
				if (!$scope.loadDocTree()) {
					relativePath.push(last);		//if operation fails, push the entry back
				}
				if (entry == null) {	//called from Java code. Need to manually call $apply().
					$scope.$apply();
				}
			}
		}
		else if (!entry.isDirectory) {		//open file using Android default viewer
			//TODO: not implemented yet.
		}
		else if (entry.isDirectory) {		//open folder
			relativePath.push(removeTailingSlash(entry.name));
			if (!$scope.loadDocTree()) {
				relativePath.pop();			//if operation fails, remove the entry
			}
		}
	}
	
	/* Load cifs doc tree using the current orderBy option and relative path
	   Return boolean value indicating success or fail */
	$scope.loadDocTree = function () {
		var path = relativePath.length == 0 ? '' : relativePath.join('/') + '/';
		var response = JSON.parse(webAppInterface.Browse(selectedProfileId, path, $scope.orderBy));
		if (response.status == 'SUCCESS') {
			$scope.docTree = response.data;
			$scope.docTree.unshift(backEntry);
			return true;
		} else {
			return false;
		}
	}
	
	/* Return to the profile list view */
	$scope.disconnect = function(){
		reloadProfiles();
		$scope.connected = false;
		selectedProfileId = null;
		Global.resetBackHandler();
	}
	
	/* Show context menu for entry when long-pressed */
	$scope.showContextMenu = function(entry) {
		if (!entry.command) {
			if (entry.isDirectory) {
				$scope.selectedEntryIsFile = false;
			} else {
				$scope.selectedEntryIsFile = true;
			}
			webAppInterface.HapticFeedback();
			selectedEntry = removeTailingSlash(entry.name);
			if (!entry.isDirectory) {
				$scope.selectedEntryCanPlay = canStream(selectedEntry);
				$scope.selectedEntryCanOpen = canOpen(selectedEntry);
			} else {
				$scope.selectedEntryCanPlay = false;
				$scope.selectedEntryCanOpen = false;
			}
			$('#entry-menu').modal('show');
		}
	}
	
	/* Show detail information about an entry */
	$scope.showEntryDetails = function () {
		var path = relativePath.length == 0 ? selectedEntry : relativePath.join('/') + '/' + selectedEntry;
		var response = JSON.parse(webAppInterface.Detail(selectedProfileId, path));
			if (response.status == 'SUCCESS') {
				$scope.curEntryDetail = response.data;
				$('#entry-menu').modal('hide');
				$('#entry-details').modal('show');
			}
	}
	
	/* Stream the selected file. Open it using the default application */
	$scope.playFile = function() {
		var path = relativePath.length == 0 ? selectedEntry : relativePath.join('/') + '/' + selectedEntry;
		webAppInterface.StreamFile(selectedProfileId, path);
		$('#entry-menu').modal('hide');
	}
	
	/* Open the selected file. The file will be loaded into cache directory and open later */
	$scope.openFile = function() {
		var path = relativePath.length == 0 ? selectedEntry : relativePath.join('/') + '/' + selectedEntry;
		webAppInterface.OpenFile(selectedProfileId, path);
		$('#entry-menu').modal('hide');
	}
	
	/* Download the selected file */
	$scope.downloadFile = function () {
		var path = relativePath.length == 0 ? selectedEntry : relativePath.join('/') + '/' + selectedEntry;
		webAppInterface.DownloadFile(selectedProfileId, path);
		$('#entry-menu').modal('hide');
	}
	
	/* Create a profile linking to the folder */
	$scope.createProfileShortCut = function () {
		var path = relativePath.length == 0 ? selectedEntry : relativePath.join('/') + '/' + selectedEntry;
		webAppInterface.CreateProfileShortcut(selectedProfileId, path);
		$('#entry-menu').modal('hide');
	}
	
}]);

/* Downloads controller */
app.controller('downloadsCtrl', ['$scope', '$interval', function($scope, $interval){
	$scope.downloads = [];
	//get all downloads including finished downloads, and download in the queue
	$scope.getDownloads = function(){
		var response = JSON.parse(webAppInterface.GetAllDownloads());
		if (response.status == 'SUCCESS'){
			$scope.downloads = response.data;
		} else {
			$scope.downloads = [];
		}
	}
	
	//refresh downloads every 3 seconds
	$scope.getDownloads();		//get the download immediately when the controller is initialized
	var downloadsHandler = $interval($scope.getDownloads, 3000);
	
	//destroy the downloads handler to stop refreshing when the page is not visible
	$scope.$on('$destroy', function() {
		$interval.cancel(downloadsHandler);
    });
	
	//get the background color class a job
	$scope.getBgColor = function(job) {
		switch(job.status) {
			case 'WAITING' : return 'alert-warning';
			case 'IN_PROGRESS' : return 'alert-success';
			case 'FINISHED' : return 'alert-info';
			case 'TERMINATED' : return 'alert-danger';
			case 'CANCELLED' : return 'alert-danger';
			default: return '';
		}
	}
	
	//use different style of progress bar for different status
	$scope.getProgressStyle = function(job) {
		switch(job.status) {
			case 'WAITING' : return 'progress-bar-striped';
			case 'IN_PROGRESS' : return 'progress-bar-striped active';
			case 'FINISHED' : return '';
			case 'TERMINATED' : return '';
			case 'CANCELLED' : return '';
			default: return '';
		}
	}
	
	//stop or cancel a job
	$scope.stopJob = function(job) {
		webAppInterface.StopJob(job.jobId);
		$scope.getDownloads();
	}
	
	//remove the download history
	$scope.removeHistory = function(job) {
		webAppInterface.RemoveHistory(job.jobId);
		$scope.getDownloads();
	}
	
	//open the downloaded file using the default application. If more than 2 application is available, an application chooser appears
	$scope.open = function (job) {
		webAppInterface.OpenDownloadedFile(job.jobId);
	}
}]);

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
                            username: '',
                            password: '',
                            rootUrl: '',
							portNumber: 445};
		Global.setBackHandler($scope.goToListView);
    }
	
	/* Calls Java code to create a new profile */
    $scope.createProfile = function(){
		document.activeElement.blur()	//hide keyboard
		var response = JSON.parse(webAppInterface.AddProfile($scope.curProfile.profileName, $scope.curProfile.rootUrl, $scope.curProfile.portNumber, $scope.curProfile.username, $scope.curProfile.password));
		$scope.wait = false;
		if (response.status == 'SUCCESS'){
			$scope.goToListView(false);
		}
    }
	
	/* Calls Java code to modify the current selected profile */
    $scope.saveProfile = function(){
		document.activeElement.blur()	//hide keyboard
		var response = JSON.parse(webAppInterface.ModifyProfile($scope.curProfile.profileId, $scope.curProfile.profileName, $scope.curProfile.rootUrl, $scope.curProfile.portNumber, $scope.curProfile.username, $scope.curProfile.password));
		$scope.wait = false;
		if (response.status == 'SUCCESS'){
			$scope.goToListView(false);
		}
    }
	
	/* Calls Java code to delete the current selected profile */
    $scope.deleteProfile = function(){
		document.activeElement.blur()	//hide keboard
		var response = JSON.parse(webAppInterface.RemoveProfileById($scope.curProfile.profileId));
		if (response.status == 'SUCCESS'){
			$scope.goToListView(false);
		}
    }
}]);