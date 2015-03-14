/* Global callback functions that can be accessed by Java code */
var Global = function(){
	var backHandler = null;		//function handler called when hardware Back button is clicked
	var menuHandler = null;		//function handler called when hardware menu button is clicked
	
	function returnMainTab() {
		var $firstTab = $('#main_nav_bar ul li').first();
		if ($firstTab.hasClass('active')) {		//if in the first tab, exit the app
			webAppInterface.ExitApp();
		} else {
			$firstTab.trigger('click');			//else return to the first tab
		}
	}
	
	return {
		setBackHandler: function(funcRef){
			backHandler = funcRef;
		},
		resetBackHandler: function() {
			backHandler = returnMainTab;
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
			if (_.isFunction(menuHandler)) {
				menuHandler();
			}
		}
	}
}();

Global.setMenuHandler(function(){
	$('button.navbar-toggle').trigger('click');
});

var app = angular.module('myNasApp', ['ui.checkbox']);		//Bootstrap angular app. Declare app module