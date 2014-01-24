(function (ng, _) {
  var SEATHREEURL   = 'http://localhost:8888/tweets-for-many',
      FETCHINTERVAL = 5000;

  ng.module('SeaThree', [])
  .controller('TweetsCtrl', function ($scope, $http, $interval) {
    var findUser,
        List,
        fetch;
    List = function (name, usernames) {
      this.name = name;
      this.usernames = usernames;
      this.selected = false;
      this.tweets = [];
    };

    $scope.lists = [
      new List('Mexico', ['GobiernoDF']),
      new List('United States', ['nate_smith']),
      new List('Organizations', []),
      new List('My List', [])
    ];
    $scope.lists.unshift(new List('All', _.chain($scope.lists)
                                         .pluck('usernames')
                                         .flatten()
                                         .unique()
                                         .value()));

    $scope.users = [{
      username: 'GobiernoDF',
      src: 'es',
      tgt: 'en',
      tweets: []
    }, {
      username: 'nate_smith',
      src:'en',
      tgt:'es',
      twwets: []
    }];

    findUser = function (users, username) {
      return _(users).find(function (user) {
               return user.username === username;
             });
    };

    $scope.isSelected = function (list) {return list.selected;};
    $scope.toggleListAt = function (index) {
      $scope.lists[index].selected = !$scope.lists[index].selected;
    };

    fetch = function () {
      var data = _($scope.users).map(function (user) {
                   return _(user).pick('username', 'src', 'tgt');
                 });
      $http.get(SEATHREEURL + '?data=' + JSON.stringify(data)).success(function (users) {
        $scope.users = users;
      });
    };

    $scope.$watch('users', function () {
      // When users is updated, we must update the lists.
      _($scope.lists).each(function (list) {
        var tweets = _(list.usernames).map(function (username) {
          return findUser($scope.users, username).tweets;
        });
        list.tweets = _(_(tweets).flatten()).sortBy('id').reverse();
      });
    });

    // Start polling.
    $interval(fetch, FETCHINTERVAL);
  })
  .controller('LanguageCtrl', function ($scope) {
    // TODO
  });

})(angular, _);
