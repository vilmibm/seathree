import mock
from mock import Mock
from unittest2 import TestCase

import daemon

class TestTweetAuthoring(TestCase):
    def setUp(self):
        self.streamer = daemon.TranslationStreamer(*daemon.get_twitter_auth())
        self.mock_tc = Mock()
        self.streamer.set_twitter_client(self.mock_tc)
        
    def test_one_tweet(self):
        details = {
            'user': {
                'screen_name': 'foo',
            },
            'translated_text': 'bar'
        }
        self.streamer.tweet(details)
        self.mock_tc.update_status.assert_called_once_with('@foo: bar')

    def test_n_tweets(self):
        details = {
            'user': {
                'screen_name': 'foo',
            },
            'translated_text': 'Once upon a time long long ago in a galaxy far far away away away some things happend to some people of varying aspects and intents and gerbils wondered why cosmonauts drank iced kool-aid from the cups of woe, the cups of prison, the cups of prospect, the cups of ultimate eventuality'
        }
        self.streamer.tweet(details)
        self.assertEqual(self.mock_tc.update_status.call_count, 2)
        calls = [
            mock.call.update_status('@foo: Once upon a time long long ago in a galaxy far far away away away some things happend to some people of varying aspects and...'),
            mock.call.update_status('intents and gerbils wondered why cosmonauts drank iced kool-aid from the cups of woe, the cups of prison, the cups of prospect, the cups'),
        ]
        self.mock_tc.assert_has_calls(calls)


class TestDetectOriginalTweet(TestCase):
    def setUp(self):
        self.streamer = daemon.TranslationStreamer(*daemon.get_twitter_auth())
    def test_retweet(self):
        data = {
            'retweeted_status':{'stuff':'things'},
            'text': 'some retweet'
        }
        self.assertFalse(self.streamer.is_original_tweet(data))

    def test_not_tweet(self):
        data = {
            'not': 'a tweet'
        }
        self.assertFalse(self.streamer.is_original_tweet(data))

    def test_original_tweet(self):
        data = {
            'user':{
                'screen_name': 'foobar'
            },
            'text': 'a tweet'

        }
        self.assertTrue(self.streamer.is_original_tweet(data))

