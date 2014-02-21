(function (ng, _) {
  var SEATHREEURL   = 'http://localhost:8888/tweets-for-many',
      FETCHINTERVAL = 30000,
      List,
      defaultLists,
      defaultUsers,
      findUser;

  List = function (name, usernames) {
    this.name = name;
    this.usernames = usernames;
    this.selected = false;
    this.tweets = [];
  };

  findUser = function (users, username) {
    return _(users).find(function (user) {
      return user.username === username;
    });
  };


  defaultUsers = [{
    username: 'GobiernoDF',
    src: 'es',
    tgt: 'en',
    tweets: []
  }, {
    username: 'VertigoPolitico',
    src: 'es',
    tgt: 'en',
    tweets: []
  }, {
    username: 'bennnyv',
    src: 'en',
    tgt: 'es',
    tweets: []
  }, {
    username: 'nate_smith',
    src:'en',
    tgt:'es',
    tweets: []
  }];

  defaultLists = [
    new List('Mexico', ['GobiernoDF', 'VertigoPolitico']),
    new List('United States', ['nate_smith', 'bennnyv']),
    new List('Organizations', ['VertigoPolitico']),
    new List('My List', [])
  ];

  defaultLists.unshift(new List('All',
                                _.chain(defaultLists)
                                .pluck('usernames')
                                .flatten()
                                .unique()
                                .value()));

  ng.module('SeaThree', ['ngSanitize'])
  .controller('TweetsCtrl', function ($scope, $http, $interval) {
    var fetch,
        addUserAt;

    $scope.lists = defaultLists;

    $scope.users = defaultUsers;

    $scope.addUserModalShown = false;

    $scope.isSelected = function (list) {return list.selected;};
    $scope.toggleList = function (list) {
      list.selected = !list.selected;
    };
    $scope.addUserAt = function (list) {
      // TODO nasty hack; since there is now only one custom list we
      // can just save a reference in scope. When there are multiple
      // custom lists, the list being added to will have to be exposed
      // to the modal. It might be fine to just have a "currentlist"
      // model but that gives me the willies
      $scope.mylist = list;
      $scope.addUserModalShown = true;
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
    fetch();
    $interval(fetch, FETCHINTERVAL);
  })
  .controller('AddUserCtrl', function ($scope) {
    $scope.langs = [{code:'en', display:'English'},
                    {code:'es', display:'Spanish'}];
    $scope.addUser = function () {
      $scope.users.push({
        username: $scope.username,
        src: $scope.src,
        tgt: $scope.tgt
      });
      $scope.mylist.usernames.push($scope.username);
      $scope.lists[0].usernames.push($scope.username)
      // TweetsCtrl >> Modal >> AddUserCtrl
      $scope.username = '';
      $scope.$parent.$parent.addUserModalShown = false;
    };
  })
  .directive('modal', function() {
    return {
      restrict: 'E',
      scope: {
        show: '='
      },
      replace: true,
      transclude: true,
      link: function (scope, element, attrs) {
        scope.hideModal = function () {
          scope.show = false;
        };
      },
      template:
      '<div class="ng-modal" ng-show="show">'
        + '<div class="ng-modal-overlay" ng-click="hideModal()"></div>'
        + '<div class="ng-modal-dialog">'
        + '<div class="ng-modal-close" ng-click="hideModal()">×</div>'
          + '<div class="ng-modal-dialog-content" ng-transclude></div>'
        + '</div></div>'
    };
  })
  .controller('LanguageCtrl', function ($scope) {
    // TODO
  });

})(angular, _);
