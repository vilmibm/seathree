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
        self.mock_tc.update_status.assert_called_once_with(status='@foo: bar')

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
            mock.call.update_status(status='@foo: Once upon a time long long ago in a galaxy far far away away away some things happend to some people of varying aspects and...'),
            mock.call.update_status(status='intents and gerbils wondered why cosmonauts drank iced kool-aid from the cups of woe, the cups of prison, the cups of prospect, the cups'),
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


class TestSigils(TestCase):
    def setUp(self):
        self.streamer = daemon.TranslationStreamer(*daemon.get_twitter_auth())

    def testMarkSigils(self):
        tweet = "@so_and_so look at this link http://some.link.com #awesome isn't it cool?"
        expected_text = "xz0 look at this link xz1 xz2 isn't it cool?"
        expected_replaced = [
            "@so_and_so",
            "http://some.link.com",
            "#awesome",
        ]
        marked_text, replaced = self.streamer.mark_sigils(tweet)
        self.assertEqual(marked_text, expected_text)
        self.assertEqual(replaced, expected_replaced)

    def test_restore_sigils(self):
        marked_text = "xz0 look at this link xz1 xz2 isn't it cool?"
        replaced = [
            "@so_and_so",
            "http://some.link.com",
            "#awesome",
        ]
        expected_restored = "@so_and_so look at this link http://some.link.com #awesome isn't it cool?"
        restored_text = self.streamer.restore_sigils(marked_text, replaced)
        self.assertEqual(restored_text, expected_restored)

class TestOnSuccess(TestCase):
    def setUp(self):
        self.streamer = daemon.TranslationStreamer(*daemon.get_twitter_auth())
        self.mock_tc = Mock()
        self.mock_gc = Mock()
        self.streamer.set_twitter_client(self.mock_tc)
        self.streamer.set_google_client(self.mock_gc)

    def test_not_a_tweet(self):
        data = {
            'some':'thing'
        }
        self.streamer.on_success(data)
        self.assertEqual(self.mock_tc.update_status.call_count, 0)
        self.assertEqual(self.mock_gc.translations.call_count, 0)

    def test_a_tweet(self):
        data = {
            'user': {
                'screen_name': 'foo',
            },
            'text': 'bar @someone #awesome http://www.foobarbaz.quux'
        }
        self.mock_gc.translations().list().execute.return_value = 'quux xz0 xz1 xz2'
        self.mock_gc.translations.reset_mock()

        self.streamer.on_success(data)

        self.assertEqual(self.mock_gc.translations.call_count, 1)
        self.mock_tc.update_status.assert_called_once_with(status='@foo: quux @someone #awesome http://www.foobarbaz.quux')

    def test_n_tweets(self):
        data = {
            'user': {
                'screen_name': 'foo',
            },
            'text': 'Once upon a time long long ago in a galaxy far far away away away some things happend to some people of varying aspects and intents and gerbils wondered why cosmonauts drank iced kool-aid from the cups of woe, the cups of prison, the cups of prospect, the cups of ultimate eventuality'
        }
        self.mock_gc.translations().list().execute.return_value = 'Thrice upon a time long long ago in a galaxy far far away away away some things happend to some people of varying aspects and intents and gerbils wondered why cosmonauts drank iced kool-aid from the cups of woe, the cups of prison, the cups of prospect, the cups of ultimate eventuality'
        self.mock_gc.translations.reset_mock()

        self.streamer.on_success(data)

        self.assertEqual(self.mock_gc.translations.call_count, 1)
        calls = [
            mock.call.update_status(status='@foo: Thrice upon a time long long ago in a galaxy far far away away away some things happend to some people of varying aspects and...'),
            mock.call.update_status(status='intents and gerbils wondered why cosmonauts drank iced kool-aid from the cups of woe, the cups of prison, the cups of prospect, the cups'),
        ]
        self.mock_tc.assert_has_calls(calls)
