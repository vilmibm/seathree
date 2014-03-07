(function (ng, _) {
  var FETCHINTERVAL = 10000,
      List,
      User,
      Users,
      defaultLists,
      defaultUsers,
      findUser,
      findList;

  List = function (name, users) {
    this.name = name;
    this.users = users;
    this.selected = false;
    this.visibleTweets = [];
    this.newTweets = [];
  };

  List.prototype.topTweetId = function () {
    if (this.visibleTweets.length === 0) {
      return -1;
    }
    return this.visibleTweets[0].id;
  };

  // MOST RECENT TWEETS FIRST
  User = function (username, src, tgt) {
    this.username = username;
    this.src = src;
    this.tgt = tgt;
    this.tweets = [];
    this._fetchUrl = '/tweets/' + username + '/' + src + '/' + tgt;
  };

  User.prototype.fetchUrl = function () {
    if (this.tweets.length === 0) {
      return this._fetchUrl;
    }
    else {
      return this._fetchUrl + '/' + this.tweets[0].id_str;
    }
  };

  User.prototype.fetch = function ($http, $q) {
    var url = this.fetchUrl(),
        self = this,
        deferred = $q.defer();

    $http.get(url).success(function (tweets) {
      self.tweets = tweets.concat(self.tweets);
      deferred.resolve(self);
    });

    return deferred;
  };

  findThingByProperty = function (searchProperty, things, value) {
    return _(things).find(function (thing) {
      return thing[searchProperty] === value;
    });
  };

  findUser = findThingByProperty.bind(null, 'username');
  findList = findThingByProperty.bind(null, 'name');

  defaultUsers = [
    new User('GobiernoDF',      'es', 'en'),
    new User('VertigoPolitico', 'es', 'en'),
    new User('bennnyv',         'en', 'es'),
    new User('nate_smith',      'en', 'es')
  ];

  defaultLists = [
    // TODO I can still use usernames here. In terms of memory object
    // references are fine but strings are a little more predictable.
    new List('Mexico',        [findUser(defaultUsers, 'GobiernoDF'), findUser(defaultUsers, 'VertigoPolitico')]),
    new List('United States', [findUser(defaultUsers, 'nate_smith'), findUser(defaultUsers, 'bennnyv')]),
    new List('My List', [])
  ];

  defaultLists.unshift(new List('All',
                                _.chain(defaultLists)
                                .pluck('users')
                                .flatten()
                                .unique(false, function(user) { return user.username; })
                                .value()));

  ng.module('SeaThree', ['ngSanitize'])
  .controller('TweetsCtrl', function ($scope, $http, $q, $interval, $window) {
    var fetch,
        addUserAt,
        lists,
        users;

    // persist
    //lists = JSON.parse($window.localStorage.getItem('lists'));
    $scope.lists = lists || defaultLists;

    // persist
    //users = JSON.parse($window.localStorage.getItem('users'));
    $scope.users = users || defaultUsers;

    $scope.addUserModalShown = false;

    $scope.isSelected = function (list) {return list.selected;};
    $scope.toggleList = function (list) {
      list.selected = !list.selected;
      //$window.localStorage.setItem('lists', JSON.stringify($scope.lists));
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

    $scope.loadNewTweetsAt = function (list) {
      console.log("loading new tweets...");
      var newTweets = list.newTweets;
      list.newTweets = [];
      if (list.visibleTweets.length === 0) {
        list.visibleTweets = newTweets;
      }
      else {
        list.visibleTweets = newTweets.concat(list.visibleTweets);
      }
    };

    fetch = function () {
      console.log('fetching');
      var fetchPromises;
      // We have a list of lists.
      // For each shown list, we want to have a unique list of all the users.
      // They will each fetch their tweets since their most recent tweet,
      //  updating themselves.

      fetchPromises = _($scope.lists).chain()
        .filter(function (list) {return !!list.selected;})
        .pluck('users')
        .flatten()
        .uniq(false, function (user) { return user.username; })
        .map(function (user) {return user.fetch($http, $q);})
        .value();

      $q.all(fetchPromises).then(function () {
        $scope.lists.forEach(function (list) {
          var topListTweetId = list.topTweetId();
          var newTweets = _(list.users).chain()
            .pluck('tweets')
            .flatten()
            .filter(function (tweet) { return tweet.id > topListTweetId; })
            .sortBy('id')
            .reverse()
            .value();
          list.newTweets = _(newTweets.concat(list.newTweets)).uniq(false, function(t) { return t.id_str; });
        });

        //$window.localStorage.setItem('lists', JSON.stringify($scope.lists));
        //$window.localStorage.setItem('users', JSON.stringify($scope.users));
      });
    };

    // Now, to handle the "new tweets" button. I feel like it can
    // mostly be managed in the views.

    // Start polling.
    fetch();
    $interval(fetch, FETCHINTERVAL);
  })
  .controller('AddUserCtrl', function ($scope) {
    $scope.langs = [{code:'en', display:'English'},
                    {code:'es', display:'Spanish'},
                    {code:'uk', display:'Ukranian'},
                    {code:'ru', display:'Russian'},
                    {code:'de', display:'German'},
                    {code:'fr', display:'French'},
                    {code:'pl', display:'Polish'},
                    {code:'it', display:'Italian'},
                    {code:'nl', display:'Dutch'},
                    {code:'pt', display:'Portuguese'},
                    {code:'ro', display:'Romanian'}
                   ];
    $scope.addUser = function () {
      var newUser = new User($scope.username, $scope.src, $scope.tgt);
      $scope.users.push(newUser);
      $scope.mylist.users.push(newUser);
      $scope.lists[0].users.push(newUser);
      // TweetsCtrl >> Modal >> AddUserCtrl
      $scope.username = '';
      $scope.$parent.$parent.addUserModalShown = false;
      //$window.localStorage.setItem('lists', JSON.stringify($scope.lists));
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
