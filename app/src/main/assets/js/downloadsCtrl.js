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