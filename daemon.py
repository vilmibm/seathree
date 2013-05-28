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

def translate(client, text, from="es", to="en"):
    return client.translations().list(source=from, target=to, q=text).execute()

def get_twitter_auth():
    secrets = get_secrets()
    return secrets['APP_AUTH']['consumer_key'],    \
           secrets['APP_AUTH']['consumer_secret'], \
           secrets['USER_AUTH']['oauth_token'],    \
           secrets['USER_AUTH']['oauth_token_secret']

def get_client():
    return Twython(*get_twitter_auth())

def get_stream():
    return TranslationStreamer(*get_twitter_auth())

def original_tweet(data):
    """Given some data from the streaming API, determine if it is an
    original tweet from one of the authed account's
    followings. Returns True or False"""
    # Favorites do not come through. Non-tweets lack a text property,
    # it seems (ie new followings).
    if 'text' not in data:
        return False
    if 'retweeted_status' in data:
        return False
    return True

def extract_details(data):
    """Returns a dictionary with user and text keys"""
    return {
        'text': data.get('text', ''),
        'user': {
            # the only key we need for now
            'screen_name': data['user']['screen_name'],
        }
    }

mksigil = lambda c: 'z%d' % c

def mark_sigils(raw_text):
    """Replace special forms within a tweet with ordered sigils so
    they can be restored later. For example,

    Este cerveza esta sabrosa #cerveza http://bit.ly/foo @friend

    will be transformed into

    Este cerveza esta sabrosa zx1 zx2 zx3

    (these will be later reconstructed).

    Returns a string and a mapping of sigil to yes
    """
    symbol_re = re.compile('(\@\w+|http:\/\/\w+|\#\w+)')
    symbols = re.findall(raw_text)

    counter = 0
    marked_text = raw_text
    for symbol in symbols:
        marked_text = raw_text.replace(symbol, mksigil(counter))
        counter += 1

    return marked_text, symbols

def restore_sigils(marked_text, symbols):
    counter = 0
    unmarked_text = marked_text
    for symbol in symbols:
        sigil = mksigil(counter)
        unmarked_text.replace(sigil, symbol)

    return unmarked_text

class TranslationStreamer(TwythonStreamer):
    def __init__(self, client):
        super(TwythonStreamer, self).__init__()
        self.client = client

    def on_success(self, data):
        if not original_tweet(data):
            return
        details = extract_details(data)
        raw_text = details['text']
        marked_text, symbols = mark_sigils(raw_text)
        english_text = translate(marked_text)
        details['translated_text'] = restore_sigils(marked_text, symbols)

        tweet(self.client, details)

    def on_error(self, status_code, data):
        print status_code, data


if __name__ == '__main__':
    t = get_client()
    s = get_stream(t)
    stream.user(**{'with':"followings"})

