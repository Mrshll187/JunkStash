app.controller('homeController', ['$scope', 'homeFactory', '$timeout', '$rootScope', '$cookieStore', 'socketService',
      function($scope, homeFactory, $timeout, $rootScope, $cookieStore, socketService) {
	
	$scope.files = [];
	$scope.users = [];
	
	$scope.admin = false;
	
	$scope.fileSearch = undefined;
	$scope.userSearch = undefined;
	
	$scope.result = undefined;
	$scope.uploadFile = undefined;
	$scope.loading = false;
    
    $scope.user = undefined;
    $scope.userKey = undefined;
    $scope.selectedFile = undefined;
    $scope.hasAccess = false;
    
    $scope.totalSpace = undefined;
    $scope.totalSpaceNormalized = undefined;
    $scope.maxSpaceNormalized = undefined;
    $scope.maxSpace = undefined;
    $scope.percentUsed = undefined;
    
    $scope.message = false;
    
    $scope.showLoginModal = false;
    $scope.showShareModal = false;
    $scope.showMessageModal = false;
    $scope.showMailModal = false;
    $scope.showNotificationModal = false;

    $scope.notification = false;
    $scope.notificationType = undefined;
	$scope.notificationCount = undefined;
	
    $scope.refreshFiles = function(){
    	
		$scope.listAllFiles();
    };
    
    $scope.toggleNotificationModal = function(user){
    	
    	$scope.userKey = user;
    	$scope.showNotificationModal = !$scope.showNotificationModal;
    };
    
    $scope.toggleLoginModal = function(){
    	
        $scope.showLoginModal = !$scope.showLoginModal;
    };
	
    $scope.toggleMailModal = function(mailRecipient){
    	
    	$scope.mailRecipient = mailRecipient;
    	$scope.showMailModal = !$scope.showMailModal;
    };
    
    $scope.toggleMessageModal = function(messageRecipient){
    	
    	$scope.messageRecipient = messageRecipient;
        $scope.showMessageModal = !$scope.showMessageModal;
    };
    
    $scope.toggleShareModal = function(file){
    	
    	$scope.selectedFile = file.id;
        $scope.showShareModal = !$scope.showShareModal;
    };
    
    var getAccess = function(user){
    	
		homeFactory.getUserAccess(user).success(function (data) {
			
			$scope.hasAccess = data.payload;
		});
    };
    
	$scope.upload = function(data) { 		
		
		$scope.loading = true;
		
		homeFactory.upload($scope.uploadFile, $scope.userKey)
			.success(function (data) {
			
				$scope.result = data;
				$scope.loading = false;
				$scope.uploadFile = undefined;
			})
			.error(function (data) {
			
				$scope.result = data;
				$scope.loading = false;
				$scope.uploadFile = undefined;
			});
	};
	
	$scope.listAllFiles = function(data) { 		
		
		homeFactory.getFiles($scope.userKey).success(function (data) {
			
			$scope.files = [];
			
		    angular.forEach(data.payload, function(value, key) {
		        
		    	$scope.files.push({ 
    				
		    		message : value.message,
    				time : value.time,
    				id : value.id,
    				name : value.name,
    				type : value.type,
    				size : value.size,
    				shared : value.shared.shared,
    				sharer : value.shared.sharer,
    				shareDate : value.shared.shareDate,
    				owner : value.owner
				});
		    });
		});
	};
	
	$scope.listAllUsers = function(data) { 		
		
		homeFactory.getUsers($scope.userKey).success(function (data) {
			
			$scope.users = [];
			
		    angular.forEach(data.payload, function(value, key) {
		        
		    	$scope.users.push({ 
    				
		    		user : value.user,
    				status : value.status,
    				added : value.added,
    				space : value.space
				});
		    });
		});
	};
	
	$scope.approve = function(date){
		
		homeFactory.approve(data, $scope.userKey).success(function (data) {
			$scope.result = data;
			$scope.listAllUsers();
		});
	};
	
	$scope.getTotalDiskSpace = function(data) { 		
		
		homeFactory.getTotalDiskSpace($scope.userKey).success(function (data) {
			
			$scope.totalSpace = data.payload.size;
			$scope.totalSpaceNormalized = data.payload.normalized;
			$scope.maxSpace = data.payload.maxSpace;
			$scope.maxSpaceNormalized = data.payload.maxSpaceNormalized;
			$scope.percentUsed = (($scope.totalSpace/$scope.maxSpace)*100);
			
		});
	};
	
	$scope.logout = function(){
		
		if($scope.userKey===undefined)
			return;
		
		var payload = {
			
			user : $scope.user,
			userKey : $scope.userKey
		}
		
		homeFactory.logout(payload).success(function (data) {
			
			$scope.result = data;
			
			$scope.user = undefined;
			$scope.userKey = undefined;
			$scope.files = undefined;
			$scope.admin = undefined;
			$scope.selectedFile = undefined;
			$scope.hasAccess = false;
			
			$rootScope.$broadcast('user-logout', function (event, args) {});
			
			// Remove User Info Cookies
			$cookieStore.remove('userKey');
			$cookieStore.remove('user');
			$cookieStore.remove('admin');
			
			$scope.updatePage();
			autoCloseAlert();
			
		});
	};
	
	$scope.updatePage = function(){
		
		if($scope.userKey===undefined)
			return;
		
		$scope.listAllFiles();
		$scope.listAllUsers();
		$scope.getTotalDiskSpace();
		
		autoCloseAlert();
	};
	
	$scope.clear = function(){
		
		$scope.fileSearch = undefined;
		$scope.userSearch = undefined;
		$scope.result = undefined;
		$scope.selectedFile = undefined;
	};
	
	$scope.removeFile = function(data){
		
		homeFactory.removeFile(data, $scope.userKey).success(function (data) {
			$scope.result = data;
		});
	};
		
	$scope.removeUser = function(data){
		
		homeFactory.removeUser(data, $scope.userKey).success(function (data) {
			$scope.result = data;
			$scope.listAllUsers();
		});
	};
	
	$scope.approveUser = function(data){
		
		homeFactory.approveUser(data, $scope.userKey).success(function (data) {
			$scope.result = data;
			$scope.listAllUsers();
		});
	};
	
	$scope.denyUser = function(data){
		
		homeFactory.denyUser(data, $scope.userKey).success(function (data) {
			$scope.result = data;
			$scope.listAllUsers();
		});
	};
	
	$rootScope.$on('user-login', function (event, args) {
		
		$scope.user = args.user;
		$scope.userKey = args.userKey;
		$scope.admin = args.admin;
		
		// Put User Info cookie
		$cookieStore.put('userKey', args.userKey);
		$cookieStore.put('user', args.user);
		$cookieStore.put('admin', args.admin);
		
		getAccess($scope.userKey);
		
		$scope.updatePage();
	});
	
	$rootScope.$on('user-notification', function (event, args) {
		
		$scope.notificationType = args.type;
		$scope.notificationCount = args.count;
		
		if(args.count>0)
			$scope.notification = true;
		else
			$scope.notification = false;
		
		$scope.$apply();
	});
	
	$rootScope.$on('message', function (event, args) {
		
		$scope.message = true;
		$scope.$apply();
	});
	
	$rootScope.$on('seen-message', function (event, args) {
		
		$scope.message = false;
		$scope.$apply();
	});
	
	$rootScope.$on('file-update', function (event, args) {
		
		$scope.refreshFiles();
		$scope.getTotalDiskSpace();
	});
	
	$rootScope.$on('access-update', function (event, args) {
		
		$scope.hasAccess = args.hasAccess;
		$scope.$apply();
	});
	
	var autoCloseAlert = function(){
        
    	$timeout(function(){
    		
    		$scope.result = undefined;
    	
    	}, 1000);
    };
    
	// Get cookies
	if($cookieStore.get('userKey') !== undefined){
		
		$scope.userKey = $cookieStore.get('userKey');
		$scope.user = $cookieStore.get('user');
		$scope.admin = $cookieStore.get('admin');
		
		console.log("Using Previous User Session : "+$scope.user);
		
		getAccess($scope.userKey);
		
		$scope.listAllFiles();
		$scope.getTotalDiskSpace();
		
		if($scope.admin)
			$scope.listAllUsers();
		
		socketService.openSocketConnection($scope.userKey);
	}
	
}]);