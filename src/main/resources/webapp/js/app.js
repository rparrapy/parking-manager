/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/

'use strict';

/* App Module */

var leshanApp = angular.module('leshanApp',[ 
        'ngRoute',
        'clientControllers',
        'parkingSpotControllers',
        'parkingSpotServices',
        'objectDirectives',
        'instanceDirectives',
        'resourceDirectives',
        'resourceFormDirectives',
        'lwResourcesServices',
        'securityControllers',
        'uiDialogServices',
        'modalInstanceControllers',
        'ui.bootstrap',
]);

leshanApp.config(['$routeProvider', '$locationProvider', function($routeProvider, $locationProvider) {
    $routeProvider.
        when('/clients',           { templateUrl : 'partials/client-list.html',   controller : 'ClientListCtrl' }).
        when('/clients/:clientId', { templateUrl : 'partials/client-detail.html', controller : 'ClientDetailCtrl' }).
        when('/parking-spots',     { templateUrl : 'partials/parking-spot-list.html', controller : 'ParkingSpotListCtrl' }).
        when('/parking-spots/:parkingSpotEndpoint/:parkingSpotId', { templateUrl : 'partials/parking-spot-detail.html', controller : 'ParkingSpotDetailCtrl' }).
        when('/bills',             { templateUrl : 'partials/bills.html', controller : 'BillsCtrl' }).
        when('/security',          { templateUrl : 'partials/security-list.html', controller : 'SecurityCtrl' }).
        otherwise({ redirectTo : '/clients' });
}]);
