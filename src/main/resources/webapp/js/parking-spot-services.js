var parkingSpotServices = angular.module('parkingSpotServices', []);
parkingSpotServices.factory('parkingSpotServices',["$http", function($http) {
    var serviceInstance = {};
    var mappings = {
        "id": '/32700/0/32800',
        "state": '/32700/0/32801',
        "vehicleId": '/32700/0/32802',
        "billingRate": '/32700/0/32803'
    }
    
    var assignAttr = function(parkingSpot) {
        for (var k in mappings) {
            (function(key){ // anonymous function needed since otherwise key will always be last element of mappings
                var objectPath = mappings[key];
                var uri = "http://localhost:8080/api/clients/" + parkingSpot.endpoint + objectPath;
                $http.get(uri)
                .success(function(data, status, headers, config) {
                    if (data.status == "CONTENT" && data.content) {
                        console.log(key + " - " + data.content.value);
                        parkingSpot[key] = data.content.value;
                    }
                }).error(function(data, status, headers, config) {
                    console.error("error")
                });
            })(k);
        }
        return(parkingSpot);
    }
    
    var startObserving = function(parkingSpot) {
        for (var k in mappings) {
            (function(key){ // anonymous function needed since otherwise key will always be last element of mappings
                var objectPath = mappings[key];
                var uri = "http://localhost:8080/api/clients/" + parkingSpot.endpoint + objectPath + "/observe";
                $http.post(uri)
                .success(function(data, status, headers, config) {
                    if (data.status == "CONTENT" && data.content) {
                        console.log("started observing " + parkingSpot.endpoint + objectPath);
                    }
                }).error(function(data, status, headers, config) {
                    console.error("error")
                });
            })(k);
        }
    }
    
    var notificationCallback = function(msg,$scope) {
        $scope.$apply(function() {
            var content = JSON.parse(msg.data);
            for (var k in mappings) {
                var objectPath = mappings[k];
                if (objectPath == content.res) {
                    $scope.parkingSpots.forEach(function(ps){
                        if (ps.endpoint == content.ep) {
                            console.log(ps.endpoint + objectPath + " = " + content.val.value)
                            ps[k] = content.val.value;
                        }
                    });
                }
            }
        });
    }
    
    serviceInstance.assignAttr = assignAttr;
    serviceInstance.startObserving = startObserving;
    serviceInstance.notificationCallback = notificationCallback;
    return serviceInstance;
}]);
