/* Main page controller */
app.controller('nasMainCtrl', ['$scope', function($scope){
	Global.resetBackHandler();
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
	$scope.sortOpts = [{label: 'Folder First', value: 'typeAsc'},
						{label: 'File First', value: 'typeDesc'},
						{label: 'Name Asc', value: 'nameAsc'},
						{label: 'Name Desc', value: 'nameDesc'},
						{label: 'Date Modified Asc', value: 'dateAsc'},
						{label: 'Date Modified Desc', value: 'dateDesc'}];

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