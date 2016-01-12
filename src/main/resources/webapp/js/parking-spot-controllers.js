var parkingSpotControllers = angular.module('parkingSpotControllers', []);

parkingSpotControllers.controller('ParkingSpotListCtrl', [
    '$scope',
    '$http',
    '$location',
    'lwResources',
    'parkingSpotServices',
    function ParkingSpotListCtrl($scope, $http,$location, lwResources, parkingSpotServices) {
        // update navbar
        angular.element("#navbar").children().removeClass('active');
        angular.element("#parking-spot-navlink").addClass('active');
        
        // free resource when controller is destroyed
        $scope.$on('$destroy', function(){
            if ($scope.eventsource){
                $scope.eventsource.close()
            }
        });
        
        // add function to show parking-spot
        $scope.showParkingSpot = function(parkingSpot) {
            $location.path('/parking-spots/' + parkingSpot.endpoint);
        };
        
        $scope.numParkingSpots = function(state) {
            var ret = 0;
            $scope.parkingSpots.forEach(function(ps){
                if (ps !== undefined && ps.state == state) {
                    ret++;
                }
            });
            return(ret);
        }
        
        // get the list of connected clients
        $http.get('http://localhost:8080/api/clients'). error(function(data, status, headers, config){
            $scope.error = "Unable get client list: " + status + " " + data  
            console.error($scope.error)
        }).success(function(data, status, headers, config) {
            $scope.parkingSpots = data.map(function(ps){
                parkingSpotServices.startObserving(ps);
                return(parkingSpotServices.assignAttr(ps));
            });
            
            // HACK : we can not use ng-if="clients"
            // because of https://github.com/angular/angular.js/issues/3969
            $scope.parkingspotslist = true;
        
            // listen for clients registration/deregistration
            $scope.eventsource = new EventSource('http://localhost:8080/event');
        
            var registerCallback = function(msg) {
                $scope.$apply(function() {
                    var parkingSpot = JSON.parse(msg.data);
                    parkingSpot = parkingSpotServices.assignAttr(parkingSpot);
                    parkingSpotServices.startObserving(parkingSpot);
                    $scope.parkingSpots.push(parkingSpot);
                });
            }
            $scope.eventsource.addEventListener('REGISTRATION', registerCallback, false);
        
            var getParkingSpotIdx = function(parkingSpot) {
                for (var i = 0; i < $scope.parkingSpots.length; i++) {
                    if ($scope.parkingSpots[i].registrationId == parkingSpot.registrationId) {
                        return i;
                    }
                }
                return -1;
            }
            var deregisterCallback = function(msg) {
                $scope.$apply(function() {
                    var parkingSpotIdx = getParkingSpotIdx(JSON.parse(msg.data));
                    if(parkingSpotIdx >= 0) {
                        $scope.parkingSpots.splice(parkingSpotIdx, 1);
                    }
                });
            }
            $scope.eventsource.addEventListener('DEREGISTRATION', deregisterCallback, false);
            
            
            var notificationCallback = function(msg) {
                parkingSpotServices.notificationCallback(msg, $scope);
            }
            $scope.eventsource.addEventListener('NOTIFICATION', notificationCallback, false);

        });
}]);

parkingSpotControllers.controller('ParkingSpotDetailCtrl', [
    '$scope',
    '$location',
    '$routeParams',
    '$http',
    'lwResources',
    '$filter',
    'parkingSpotServices',
    function($scope, $location, $routeParams, $http, lwResources, $filter, parkingSpotServices) {
        // update navbar
        angular.element("#navbar").children().removeClass('active');
        angular.element("#parking-spot-navlink").addClass('active');
        $( "#datepicker" ).datepicker({
            onSelect: function( selectedDate ) {
                console.log(selectedDate);
            }
        });
    
        // free resource when controller is destroyed
        $scope.$on('$destroy', function(){
            if ($scope.eventsource){
                $scope.eventsource.close()
            }
        });
    
        $scope.parkingSpotId = $routeParams.parkingSpotId;
    
        // get parkingSpot details
        $http.get('http://localhost:8080/api/clients/' + $routeParams.parkingSpotId)
        .error(function(data, status, headers, config) {
            $scope.error = "Unable get client " + $routeParams.parkingSpotId+" : "+ status + " " + data;  
            console.error($scope.error);
        })
        .success(function(data, status, headers, config) {
            $scope.parkingSpot = data;
            $scope.parkingspot = true;
            
            // update resource tree with parkingSpot details
            lwResources.buildResourceTree($scope.parkingSpot.rootPath, $scope.parkingSpot.objectLinks, function (objects){
                $scope.objects = objects;
                parkingSpotServices.assignAttr($scope.parkingSpot);
                parkingSpotServices.startObserving($scope.parkingSpot);
            });
        });
}]);
