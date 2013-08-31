import mock
from mock import Mock

from unittest2 import TestCase

import translate 

class TestTranslate(TestCase):
    def test_success(self):
        execute_result = {'translations': [{'translatedText':'hello how are&amp;you'}]}

        mock_client = Mock()
        mock1       = Mock()
        mock0       = Mock()

        mock1.execute.return_value = execute_result
        mock0.list.return_value    = mock1
        mock_client.translations.return_value = mock0

        translated = translate.translate(mock_client, "", "", "")
        self.assertEqual(translated, "hello how are&you")

class TestSigils(TestCase):
    def test_mark_sigils(self):
        tweet = "@so_and_so look at this link http://some.link.com #awesome isn't it cool?"
        expected_text = "XZ0 look at this link XZ1 XZ2 isn't it cool?"
        expected_replaced = [
            "@so_and_so",
            "http://some.link.com",
            "#awesome",
        ]
        marked_text, replaced = translate.mark_sigils(tweet)
        self.assertEqual(marked_text, expected_text)
        self.assertEqual(replaced, expected_replaced)

    def test_restore_sigils(self):
        marked_text = "XZ0 look at this link XZ1 XZ2 isn't it cool?"
        replaced = [
            "@so_and_so",
            "http://some.link.com",
            "#awesome",
        ]
        expected_restored = "@so_and_so look at this link http://some.link.com #awesome isn't it cool?"
        restored_text = translate.restore_sigils(marked_text, replaced)
        self.assertEqual(restored_text, expected_restored)
