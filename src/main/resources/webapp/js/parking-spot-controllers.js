var lwParkingSpotControllers = angular.module('parkingSpotControllers', []);

lwParkingSpotControllers.controller('ParkingSpotListCtrl', [
    '$scope',
    '$http',
    '$location',
    function ParkingSpotListCtrl($scope, $http,$location) {
        console.log("ps");
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
        
        // get the list of connected clients
        $http.get('http://localhost:8080/api/clients'). error(function(data, status, headers, config){
            $scope.error = "Unable get client list: " + status + " " + data  
            console.error($scope.error)
        }).success(function(data, status, headers, config) {
            $scope.parkingSpots = data;
        
            // HACK : we can not use ng-if="clients"
            // because of https://github.com/angular/angular.js/issues/3969
            $scope.parkingspotslist = true;
        
            // listen for clients registration/deregistration
            $scope.eventsource = new EventSource('event');
        
            var registerCallback = function(msg) {
                $scope.$apply(function() {
                    var parkingSpot = JSON.parse(msg.data);
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
        });
}]);

lwParkingSpotControllers.controller('ParkingSpotDetailCtrl', [
    '$scope',
    '$location',
    '$routeParams',
    '$http',
    'lwResources',
    '$filter',
    function($scope, $location, $routeParams, $http, lwResources,$filter) {
        // update navbar
        angular.element("#navbar").children().removeClass('active');
        angular.element("#parking-spot-navlink").addClass('active');
    
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
    
            // update resource tree with parkingSpot details
            lwResources.buildResourceTree($scope.parkingSpot.rootPath, $scope.parkingSpot.objectLinks, function (objects){
                $scope.objects = objects;
            });
    
            // listen for parkingSpots registration/deregistration/observe
            $scope.eventsource = new EventSource('event?ep=' + $routeParams.parkingSpotId);
    
            var registerCallback = function(msg) {
                $scope.$apply(function() {
                    $scope.deregistered = false;
                    $scope.parkingSpot = JSON.parse(msg.data);
                    lwResources.buildResourceTree($scope.parkingSpot.rootPath, $scope.parkingSpot.objectLinks, function (objects){
                        $scope.objects = objects;
                    });
                });
            }
            $scope.eventsource.addEventListener('REGISTRATION', registerCallback, false);
    
            var deregisterCallback = function(msg) {
                $scope.$apply(function() {
                    $scope.deregistered = true;
                    $scope.parkingSpot = null;
                });
            }
            $scope.eventsource.addEventListener('DEREGISTRATION', deregisterCallback, false);
    
            var notificationCallback = function(msg) {
                $scope.$apply(function() {
                    var content = JSON.parse(msg.data);
                    var resource = lwResources.findResource($scope.objects, content.res);
                    if (resource) {
                        if("value" in content.val) {
                            // single value
                            resource.value = content.val.value
                        }
                        else if("values" in content.val) {
                            // multiple instances
                            var tab = new Array();
                            for (var i in content.val.values) {
                                tab.push(i+"="+content.val.values[i])
                            }
                            resource.value = tab.join(", ");
                        }
                        resource.valuesupposed = false;
                        resource.observed = true;
    
                        var formattedDate = $filter('date')(new Date(), 'HH:mm:ss.sss');
                        resource.tooltip = formattedDate;
                    } else {
                        // instance?
                        var instance = lwResources.findInstance($scope.objects, content.res);
                        if (instance) {
                            instance.observed = true;
                            for(var i in content.val.resources) {
                                var tlvresource = content.val.resources[i];
                                resource = lwResources.addResource(instance.parent, instance, tlvresource.id, null)
                                if("value" in tlvresource) {
                                    // single value
                                    resource.value = tlvresource.value
                                } else if("values" in tlvresource) {
                                    // multiple instances
                                    var tab = new Array();
                                    for (var j in tlvresource.values) {
                                        tab.push(j+"="+tlvresource.values[j])
                                    }
                                    resource.value = tab.join(", ");
                                }
                                resource.valuesupposed = false;
                                resource.tooltip = formattedDate;
                            }
                        }
                    } // TODO object level
                });
            }
            $scope.eventsource.addEventListener('NOTIFICATION', notificationCallback, false);
    
            $scope.coaplogs = [];
            var coapLogCallback = function(msg) {
                $scope.$apply(function() {
                    var log = JSON.parse(msg.data);
                    log.date = $filter('date')(new Date(log.timestamp), 'HH:mm:ss.sss');
                    console.log(log);
                    $scope.coaplogs.push(log);
                });
            }
            $scope.eventsource.addEventListener('COAPLOG', coapLogCallback, false);
    
            // coap logs hidden by default
            $scope.coapLogsCollapsed = true;
            $scope.toggleCoapLogs = function() {
                $scope.coapLogsCollapsed = !$scope.coapLogsCollapsed;
            }
        });
}]);
