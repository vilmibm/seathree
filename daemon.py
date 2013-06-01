import copy
import httplib
import json
import re

from twython import Twython, TwythonStreamer
# Google's poorly named thing. It cannot be installed into a virtualenv; be warned.
from apiclient.discovery import build

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
    def set_twitter_client(self, client):
        self.twitter_client = client

    def set_google_client(self, client):
        self.google_client = client

    def translate(self, text, source="es", target="en"):
        """
        Send text off to google to be translated. Can take a long time
        and sometimes fails. Returns `text` in language `source`
        translated into language `target`.
        """
        retries = 3
        _translate = lambda q,s,t: self.google_client.translations().list(source=s, target=t, q=q).execute()['translations'][0]['translatedText']
        while retries > 0:
            try:
                return _translate(text, source, target)
            except httplib.BadStatusLine:
                retries -= 1
        return None

    def is_original_tweet(self, data):
        """
        Given some data from the streaming API, determine if it is an
        original tweet from one of the authed account's
        followings. Returns True or False.
        """
        # Favorites do not come through. Non-tweets lack a text property,
        # it seems (ie new followings).
        if 'text' not in data:
            return False
        if data['user']['screen_name'].lower() == 'translatething':
            # TODO don't hardocde screen_name here, pull it from the secrets.json
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
        """
        Create a sigil used to replace Twitter symbols like
        @replies. Necessary to preserve symbols across trips to
        google. Also cuts down on google character usage. Got to save
        dollars.

        Takes an int, returns a string like `xz0`.
        """
        return 'XZ%d' % c

    def mark_sigils(self, raw_text):
        """
        Replace special forms within a tweet with ordered sigils so
        they can be restored later. For example,

        Este cerveza esta sabrosa #cerveza http://bit.ly/foo @friend

        will be transformed into

        Este cerveza esta sabrosa zx1 zx2 zx3

        (these will be later reconstructed).

        Returns a string and a listing of symbols that were replaced
        with sigils.
        """
        symbol_re = re.compile('(\@\w+|http:\/\/[^ ]+|\#\w+)')
        symbols = re.findall(symbol_re, raw_text)

        counter = 0
        marked_text = raw_text
        for symbol in symbols:
            marked_text = marked_text.replace(symbol, self.mksigil(counter))
            counter += 1

        return marked_text, symbols

    def restore_sigils(self, marked_text, symbols):
        """
        Restore a list of symbols to their placeholders. Accepts the
        output of `mark_sigils`.
        """
        counter = 0
        unmarked_text = marked_text
        for symbol in symbols:
            unmarked_text = unmarked_text.replace(self.mksigil(counter), symbol)
            counter += 1

        return unmarked_text

    def tweet(self, details):
        """
        Given data about a tweet, including its `translated_text`,
        create one or multiple tweets and send them to twitter.

        Returns None.
        """
        text = details['translated_text']
        screen_name = details['user']['screen_name']
        naive_tweet = "@{screen_name}: {text}".format(screen_name=screen_name, text=text)
        if len(naive_tweet) <= 140:
            print "Prepared tweet: %s" % naive_tweet
            return self.twitter_client.update_status(status=naive_tweet)
        # Split by word boundaries and add three ellipses at the end of all but last one

        words = naive_tweet.split(' ')
        tweets = []
        current_tweet = ''
        current_tweet = []
        while len(words) > 0:
            if len(' '.join(current_tweet+[words[0]])) < 137:
                current_tweet.append(words.pop(0))
            else:
                tweets.append(copy.copy(current_tweet))
                current_tweet = []

        if len(current_tweet) > 0:
            tweets.append(copy.copy(current_tweet))

        joined_tweets = []
        counter = 0
        for tweet in tweets:
            tweet_str = ' '.join(tweet)
            if counter != len(tweets) - 1:
                tweet_str += '...'
            counter += 1
            joined_tweets.append(tweet_str)

        print "Prepared tweets: %s" % joined_tweets
        for tweet in joined_tweets:
            self.twitter_client.update_status(status=tweet)

    def listen(self):
        """ Start listening to the Twitter home timeline. """
        self.user(**{'with':'following'})

    def on_success(self, data):
        """
        Data handler for Twitter stream. Handles all our business
        logic for getting tweets translated and sent back out by our
        bot.
        """
        # TODO spawn a thread to do this. Google hangs for so long
        # we'll get a huge backlog of tweets.
        print "Twitter event: "
        print "\t", data
        if not self.is_original_tweet(data):
            print "Event not a tweet, discarding"
            return
        details = self.extract_details(data)
        raw_text = details['text']
        print "Going to mark: %s" % raw_text
        marked_text, symbols = self.mark_sigils(raw_text)
        print "Going to translate: %s" % marked_text
        marked_english_text = self.translate(marked_text)
        if marked_english_text is None:
            print "*** Google was not reachable ***"
            return
        details['translated_text'] = self.restore_sigils(marked_english_text, symbols)
        print "Going to tweet: %s" % details['translated_text']

        self.tweet(details)

    def on_error(self, status_code, data):
        """
        Error handler for Twitter stream. I haven't actually seen any
        errors yet so I'm not sure what the most useful thing to do
        here is. Printing should be sufficient, for now.
        """
        print status_code, data


if __name__ == '__main__':
    home_stream = TranslationStreamer(*get_twitter_auth())

    print "Connecting Twitter client"
    twitter_client = Twython(*get_twitter_auth())
    home_stream.set_twitter_client(twitter_client)

    print "Connecting Google client"
    home_stream.set_google_client(get_google_client())

    def listen():
        try:
            print "Running listener..."
            home_stream.listen()
        except Exception, e:
            print "Saw exception: %s" % e
            print "Restarting listener"
            listen()
    listen()
