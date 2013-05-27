import json

from twython import Twython 

AUTHFILE = 'auth.json'

def get_auth():
    return json.loads(open(AUTHFILE).read())

def get_client():
    auth = get_auth()
    return Twython(
        auth['APP_AUTH']['consumer_key'],
        auth['APP_AUTH']['consumer_secret'],
        auth['USER_AUTH']['oauth_token'],
        auth['USER_AUTH']['oauth_token_secret']
    )

if __name__ == '__main__':
    t = get_client()
    print t.get_home_timeline()

