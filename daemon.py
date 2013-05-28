import json
import re

from twython import Twython, TwythonStreamer
from apiclient.discovery import build # Google's poorly named thing

SECRETS_FILE = 'secrets.json'

def get_secrets():
    return json.loads(open(SECRETS_FILE).read())

def get_google_client():
    secrets = get_secrets()
    google_api_key = secrets['GOOGLE_KEY']
    return build('translate', 'v2', developerKey=google_api_key)

def get_twitter_auth():
    secrets = get_secrets()
    return secrets['APP_AUTH']['consumer_key'],    \
           secrets['APP_AUTH']['consumer_secret'], \
           secrets['USER_AUTH']['oauth_token'],    \
           secrets['USER_AUTH']['oauth_token_secret']

class TranslationStreamer(TwythonStreamer):
    def __init__(**kwargs, *args):
        self.dry_run = kwargs.get('dry_run', False)
        super(TwythonStreamer, self).__init__(*args)

    def set_twitter_client(self, client):
        self.twitter_client = client

    def set_google_client(self, client):
        self.google_client = client

    def translate(text, from="es", to="en"):
        if self.dry_run:
            return "<translated tweet>"
        return self.google_client.translations().list(source=from, target=to, q=text).execute()

    def is_original_tweet(self, data):
        """
        Given some data from the streaming API, determine if it is an
        original tweet from one of the authed account's
        followings. Returns True or False
        """
        # Favorites do not come through. Non-tweets lack a text property,
        # it seems (ie new followings).
        if 'text' not in data:
            return False
        if 'retweeted_status' in data:
            return False
            return True

    def extract_details(self, data):
        """Returns a dictionary with user and text keys"""
        return {
            'text': data.get('text', ''),
            'user': {
                # the only key we need for now
                'screen_name': data['user']['screen_name'],
            }
        }

    def mksigil(self, c):
        return 'x%d' % c

    def mark_sigils(self, raw_text):
        """Replace special forms within a tweet with ordered sigils so
        they can be restored later. For example,

        Este cerveza esta sabrosa #cerveza http://bit.ly/foo @friend

        will be transformed into

        Este cerveza esta sabrosa zx1 zx2 zx3

        (these will be later reconstructed).

        Returns a string and a listing of symbols that were replaced
        with sigils.
        """
        symbol_re = re.compile('(\@\w+|http:\/\/\w+|\#\w+)')
        symbols = re.findall(raw_text)

        counter = 0
        marked_text = raw_text
        for symbol in symbols:
            marked_text = raw_text.replace(symbol, mksigil(counter))
            counter += 1

        return marked_text, symbols

    def restore_sigils(self, marked_text, symbols):
        counter = 0
        unmarked_text = marked_text
        for symbol in symbols:
            sigil = mksigil(counter)
            unmarked_text.replace(sigil, symbol)

        return unmarked_text

    def tweet(self, details):
        text = details['translated_text']
        screen_name = details['user']['screen_name']
        tweet_tmpl = "%s: %s"
        if self.dry_run:
            print tweet_tmpl % (screen_name, text)
            return
        # TODO actually tweet

    def listen():
        self.user(**{'with':'following'})

    def on_success(self, data):
        if not is_original_tweet(data):
            return
        details = self.extract_details(data)
        raw_text = details['text']
        marked_text, symbols = self.mark_sigils(raw_text)
        english_text = self.translate(marked_text)
        details['translated_text'] = self.restore_sigils(marked_text, symbols)

        self.tweet(details)

    def on_error(self, status_code, data):
        print status_code, data


if __name__ == '__main__':
    twitter_client = Twython(*get_twitter_auth())
    home_stream = TranslationStreamer(**{'dry_run':True}, *get_twitter_auth())
    home_stream.set_client(twitter_client)
    home_stream.set_google_client(get_google_client())

    home_stream.listen()

