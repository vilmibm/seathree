import json

from twython import Twython, TwythonStreamer

AUTHFILE = 'auth.json'


def get_auth():
    auth = json.loads(open(AUTHFILE).read())
    return auth['APP_AUTH']['consumer_key'],    \
           auth['APP_AUTH']['consumer_secret'], \
           auth['USER_AUTH']['oauth_token'],    \
           auth['USER_AUTH']['oauth_token_secret']

def get_client():
    return Twython(*get_auth())

def get_stream():
    return TranslationStreamer(*get_auth())

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

def extract_text(data):
    """Returns the text within a given tweet. Returns a string; string
    is empty if text cannot be found."""
    retun data.get('text', '')

def produce_english_tweets(t, raw_english, screen_name):
    """Produce one (or a series of) tweet(s) that represent the translation of some tweet"""
    pass


class TranslationStreamer(TwythonStreamer):
    def on_success(self, data):
        if not original_tweet(data):
            return
        details = extract_details(data)
        # for now, synchronously talk to google
    def on_error(self, status_code, data):
        print status_code, data


if __name__ == '__main__':
    s = get_stream()
    stream.user(**{'with':"followings"})

