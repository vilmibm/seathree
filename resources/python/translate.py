"""
This helper script is for talking to the Google Translate API
because I did not want to attempt and fight that battle in another
language since this was already working.

Usage: python <script>.py google_api_key srclang targetlang 'text'

Prints translation to STDOUT if successful.
Returns 0 if successful, 1 otherwise.
"""
import HTMLParser
import httplib
import re
import sys

# Google's poorly named thing. It cannot be installed into a virtualenv; be warned.
from apiclient.discovery import build

def mksigil(c):
    """
    Create a sigil used to replace Twitter symbols like
    @replies. Necessary to preserve symbols across trips to
    google. Also cuts down on google character usage. Got to save
    dollars.

    Takes an int, returns a string like `xz0`.
    """
    return 'XZ%d' % c

def mark_sigils(raw_text):
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
        marked_text = marked_text.replace(symbol, mksigil(counter))
        counter += 1

    return marked_text, symbols

def restore_sigils(marked_text, symbols):
    """
    Restore a list of symbols to their placeholders. Accepts the
    output of `mark_sigils`.
    """
    counter = 0
    unmarked_text = marked_text
    for symbol in symbols:
        unmarked_text = unmarked_text.replace(mksigil(counter), symbol)
        counter += 1

    return unmarked_text


def translate(client, text, src, target='en'):
    """
    Send text off to google to be translated. Can take a long time
    and sometimes fails. Returns `text` in language `source`
    translated into language `target`.
    """
    translated = client.translations().list(source=src,
                                            target=target,
                                            q=text).execute()['translations'][0]['translatedText']
    return HTMLParser.HTMLParser().unescape(translated)

get_google_client = lambda key: build('translate', 'v2', developerKey=key)

def main(key, src, target, text):
    client = get_google_client(key)

    marked_text, sigils = mark_sigils(text)

    try:
        print restore_sigils(translate(client, marked_text, src, target), sigils)
    except httplib.BadStatusLine:
        exit(1)

if __name__ == '__main__': main(*sys.argv[1:])
