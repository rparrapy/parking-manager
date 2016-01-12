var parkingSpotServices = angular.module('parkingSpotServices', []);
parkingSpotServices.factory('parkingSpotServices',["$http", function($http) {
    var serviceInstance = {};

    var getResourceValue = function(parkingSpot, objectPath) {
        var uri = "http://localhost:8080/api/clients/" + parkingSpot.endpoint + objectPath;
        var ret = "";
        $http.get(uri)
        .success(function(data, status, headers, config) {
            if (data.status == "CONTENT" && data.content) {
                console.log(data.content.value);
                ret = data.content.value;
            }
        }).error(function(data, status, headers, config) {
            console.error("error")
        });
        return(ret);
    }
    
    var assignAttr = function(parkingSpot) {
        var mappings = {
            "parkingSpotId": '/32700/0/32800',
            "state": '/32700/0/32801',
            "licensePlate": '/32700/0/32802'
        }
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
    }
    
    serviceInstance.getResourceValue = getResourceValue;
    serviceInstance.assignAttr = assignAttr;
    return serviceInstance;
}]);