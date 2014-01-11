/*
 SeaThree, Realtime Twitter Translations
 Copyright (C) 2014 Nathaniel Smith and Benjamin Valentine

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
(function (ng) {

  var List = function (name, users) {
    this.name = name;
    this.users = users;
  };

  var User = function (username, src, tgt) {
    this.username = username;
    this.src = src || 'es';
    this.tgt = tgt || 'tgt';
    this.tweets = [];
  };

  var lists = [
    new List('Mexico', [
      new User('GobiernoDF')
    ]),
    new List('United States', [
      new User('nate_smith', 'en', 'es')
    ]),
    new List('Organizations', [
    ]),
    new List('My List', [
    ])
  ];

  lists.push(new List('All', lists.
      map(function (list) {return list.users;}).
      reduce(function (userList0, userList1) {
             return userList0.concat(userList1);
           }).
      sort(function (user0, user1) {
             if (user0.username < user1.username)
               return -1;
             if (user0.username > user1.username)
               return 1;
             return 0;
           }).
      reduce(function (list, user) {
             if (list.length == 0)
               list.push(user);
             if (list[list.length-1].username != user.username)
               list.push(user);
             return list;
           }, [])
  ));

  ng.module('SeaThree', []).

  controller('TweetsCtrl', function ($scope, $http, $interval) {
    $scope.lists = lists;
    $scope.selectedLists = [];
    // TODO setup polling interval
  }).

  controller('LanguageCtrl', function ($scope) {
    // TODO. Putting this off until I learn more about
    // internationalization.
  });


})(angular);
