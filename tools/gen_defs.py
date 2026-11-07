#!/usr/bin/env python3
"""Build app/src/main/assets/definitions.txt from Princeton WordNet.

Usage: tools/gen_defs.py <wordnet dict folder>   (the "dict" folder from wn3.1.dict.tar.gz)

Only common words get a definition, because only common words appear in the grid. Each line is
    word <tab> part of speech <tab> base word <tab> meaning
Inflected words such as "cats" point at their base word. The commonest sense wins, unless it's marked
as offensive or uses a word from the bad-word list, in which case the next clean sense is used. The
game is for everyone, so a word with no clean sense gets no definition at all.
The WordNet licence is copied to assets/WORDNET-LICENSE.txt, which its terms require.
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "app", "src", "main", "assets")
MAX_MEANING = 110

# Everyday words WordNet leaves out, because it only covers nouns, verbs, adjectives and adverbs,
# and a few whose only senses were dropped by the filter below.
OWN_WORDS = {
    "against": ("prep", "in opposition to"), "although": ("conj", "even though"),
    "among": ("prep", "in the middle of a group"), "amongst": ("prep", "in the middle of a group"),
    "and": ("conj", "as well as"), "anybody": ("pron", "any person at all"), "anyone": ("pron", "any person at all"),
    "anything": ("pron", "a thing of any kind"), "anytime": ("adv", "at whatever time you like"),
    "aye": ("adv", "yes"), "because": ("conj", "for the reason that"), "beside": ("prep", "next to"),
    "cattle": ("noun", "cows and bulls kept on a farm"), "could": ("verb", "was able to"),
    "during": ("prep", "all through a stretch of time"), "else": ("adv", "other or different"),
    "everyone": ("pron", "every person"), "for": ("prep", "meant to be given to or used by"),
    "from": ("prep", "starting at a place or time"), "gender": ("noun", "being male, female or another identity"),
    "her": ("pron", "that woman or girl"), "herself": ("pron", "that same woman or girl"),
    "hey": ("interj", "a word to get attention"), "him": ("pron", "that man or boy"),
    "himself": ("pron", "that same man or boy"), "how": ("adv", "in what way"), "into": ("prep", "to the inside of"),
    "itself": ("pron", "that same thing"), "keyword": ("noun", "an important word used to search for something"),
    "keywords": ("noun", "important words used to search for something"), "kidney": ("noun", "an organ that cleans the blood"),
    "myself": ("pron", "this same person speaking"), "nor": ("conj", "and also not"), "onto": ("prep", "to a place on top of"),
    "oops": ("interj", "said after a small mistake"), "others": ("pron", "different people or things"),
    "ought": ("verb", "should"), "our": ("pron", "belonging to us"), "ours": ("pron", "the one belonging to us"),
    "per": ("prep", "for each"), "rehab": ("noun", "treatment that helps someone recover"),
    "remix": ("noun", "a new version of a recording"), "shall": ("verb", "will"), "she": ("pron", "that woman or girl"),
    "should": ("verb", "ought to"), "since": ("prep", "from a past time until now"), "than": ("conj", "compared with"),
    "that": ("pron", "the one over there"), "the": ("article", "a particular one"), "thee": ("pron", "an old word for you"),
    "their": ("pron", "belonging to them"), "them": ("pron", "those people or things"), "these": ("pron", "the ones here"),
    "they": ("pron", "those people or things"), "this": ("pron", "the one here"), "those": ("pron", "the ones over there"),
    "thru": ("prep", "a short spelling of through"), "thy": ("pron", "an old word for your"),
    "til": ("prep", "a short spelling of until"), "toward": ("prep", "in the direction of"),
    "towards": ("prep", "in the direction of"), "turbo": ("noun", "a device that gives an engine more power"),
    "unless": ("conj", "except if"), "until": ("prep", "up to a certain time"), "unto": ("prep", "an old word for to"),
    "upon": ("prep", "on top of"), "versus": ("prep", "against, in a contest"), "via": ("prep", "by way of"),
    "what": ("pron", "which thing"), "whatever": ("pron", "anything at all"), "when": ("adv", "at what time"),
    "whenever": ("adv", "at any time that"), "where": ("adv", "at what place"), "whereas": ("conj", "but on the other hand"),
    "whether": ("conj", "if it is so or not"), "which": ("pron", "what one"), "whilst": ("conj", "at the same time as"),
    "whom": ("pron", "what person"), "whose": ("pron", "belonging to which person"), "with": ("prep", "in the company of"),
    "without": ("prep", "not having"), "would": ("verb", "used to talk about something imagined"),
    "you": ("pron", "the person being spoken to"), "your": ("pron", "belonging to you"),
    "yours": ("pron", "the one belonging to you"), "yourself": ("pron", "that same person you are"),
    "ave": ("noun", "short for avenue"), "bio": ("noun", "a short life story"), "bios": ("noun", "short life stories"),
    "carb": ("noun", "a food like bread or pasta"), "celebs": ("noun", "famous people"), "dee": ("noun", "the letter D"),
    "del": ("noun", "a sign used in advanced maths"), "dom": ("noun", "a title for a monk"), "dow": ("noun", "an Arab sailing boat"),
    "eau": ("noun", "the French word for water"), "electro": ("noun", "dance music made with electronics"),
    "hon": ("noun", "short for honey, a pet name"), "inkjet": ("noun", "a printer that sprays tiny drops of ink"),
    "kay": ("noun", "the letter K"), "lib": ("noun", "short for liberation"), "libs": ("noun", "short for liberals"),
    "mel": ("noun", "a unit used to measure the pitch of a sound"), "meta": ("adj", "about itself"),
    "poly": ("noun", "short for polyester"), "rec": ("noun", "short for recreation"), "reg": ("noun", "short for regulation"),
    "reseller": ("noun", "someone who sells goods again"), "sans": ("prep", "without"), "ser": ("noun", "an old unit of weight in India"),
    "sim": ("noun", "a computer game that copies real life"), "sims": ("noun", "computer games that copy real life"),
    "sri": ("noun", "a title of respect in India"), "stat": ("noun", "short for statistic"), "stats": ("noun", "short for statistics"),
    "tel": ("noun", "an ancient mound in the Middle East"), "whats": ("pron", "short for what is"),
}

POS = {"noun": "n", "verb": "v", "adj": "a", "adv": "r"}
# WordNet's rules for undoing an ending, from its morphy tool.
DETACH = {
    "noun": [("s", ""), ("ses", "s"), ("xes", "x"), ("zes", "z"), ("ches", "ch"), ("shes", "sh"), ("men", "man"), ("ies", "y")],
    "verb": [("s", ""), ("ies", "y"), ("es", "e"), ("es", ""), ("ed", "e"), ("ed", ""), ("ing", "e"), ("ing", "")],
    "adj": [("er", ""), ("est", ""), ("er", "e"), ("est", "e")],
    "adv": [],
}
SENSE_TYPES = {"1": "noun", "2": "verb", "3": "adj", "4": "adv", "5": "adj"}
MARKED = re.compile(r"\b(offensive|obscene|vulgar|derogatory|disparaging|slur|slang|sexual|genitals?|penis|vagina|"
                    r"prostitute|intercourse|copulate|feces|excrement|urine)\b", re.I)


def read_index(folder, name):
    """Lemma to its synset offsets, commonest first."""
    out = {}
    with open(os.path.join(folder, "index." + name), encoding="utf-8") as f:
        for line in f:
            if line.startswith(" "):
                continue
            parts = line.split()
            lemma, pointers = parts[0], int(parts[3])
            senses = int(parts[2])
            out[lemma] = parts[6 + pointers:6 + pointers + senses]
    return out


def read_glosses(folder, name):
    out = {}
    with open(os.path.join(folder, "data." + name), encoding="utf-8") as f:
        for line in f:
            if line.startswith(" ") or "|" not in line:
                continue
            out[line.split(" ", 1)[0]] = line.split("|", 1)[1].strip()
    return out


def read_exceptions(folder, name):
    out = {}
    with open(os.path.join(folder, name + ".exc"), encoding="utf-8") as f:
        for line in f:
            parts = line.split()
            if len(parts) >= 2:
                out[parts[0]] = parts[1]
    return out


def read_counts(folder):
    """How often each lemma's first sense turned up in WordNet's tagged texts, by part of speech."""
    counts = {}
    with open(os.path.join(folder, "index.sense"), encoding="utf-8") as f:
        for line in f:
            parts = line.split()
            lemma, rest = parts[0].split("%", 1)
            kind = SENSE_TYPES[rest[0]]
            key = (lemma, kind)
            counts[key] = counts.get(key, 0) + int(parts[3])
    return counts


def licence(folder):
    lines = []
    with open(os.path.join(folder, "data.noun"), encoding="utf-8") as f:
        for line in f:
            if not line.startswith(" "):
                break
            lines.append(re.sub(r"^\s+\d+\s?", "", line.rstrip()))
    return "\n".join(lines).strip() + "\n"


def base_forms(word, kind, indexes, exceptions):
    """The lemmas this word could be an inflection of, nearest first."""
    forms = []
    if word in indexes[kind]:
        forms.append(word)
    if word in exceptions[kind] and exceptions[kind][word] in indexes[kind]:
        forms.append(exceptions[kind][word])
    for ending, replacement in DETACH[kind]:
        if word.endswith(ending):
            lemma = word[: -len(ending)] + replacement
            if lemma in indexes[kind] and lemma not in forms:
                forms.append(lemma)
    return forms


def clean(gloss):
    """The first plain definition, with examples dropped and any cut made at a word."""
    text = gloss.split(';')[0].split(' "')[0].strip().rstrip(":;,")
    text = re.sub(r"\([^)]*\)", "", text)
    text = re.sub(r"\s+", " ", text).strip()
    if len(text) > MAX_MEANING:
        text = text[:MAX_MEANING].rsplit(" ", 1)[0].rstrip(",;")
    return text


def main():
    folder = sys.argv[1]
    with open(os.path.join(ROOT, "core", "data", "badwords.txt"), encoding="utf-8") as f:
        bad = {line.strip().lower() for line in f if line.strip() and " " not in line.strip()}
    bad_word = re.compile(r"\b(" + "|".join(sorted(map(re.escape, bad))) + r")\b", re.I)

    indexes = {k: read_index(folder, k) for k in POS}
    glosses = {k: read_glosses(folder, k) for k in POS}
    exceptions = {k: read_exceptions(folder, k) for k in POS}
    counts = read_counts(folder)

    with open(os.path.join(ASSETS, "common.txt"), encoding="utf-8") as f:
        words = sorted({line.strip().lower() for line in f if line.strip()})

    lines, missing = [], []
    for word in words:
        options = []
        for kind in POS:
            for rank, lemma in enumerate(base_forms(word, kind, indexes, exceptions)):
                # An exact match beats an inflection, then the commoner part of speech wins.
                options.append((rank, -counts.get((lemma, kind), 0), kind, lemma))
        chosen = (OWN_WORDS[word][0], word, OWN_WORDS[word][1]) if word in OWN_WORDS else None
        for rank, _, kind, lemma in sorted(options) if not chosen else []:
            for offset in indexes[kind][lemma]:
                gloss = glosses[kind][offset]
                if not MARKED.search(gloss) and not bad_word.search(gloss) and clean(gloss):
                    chosen = (kind, lemma, clean(gloss))
                    break
            if chosen:
                break
        if chosen:
            lines.append("\t".join((word, chosen[0], chosen[1], chosen[2])))
        else:
            missing.append(word)

    with open(os.path.join(ASSETS, "definitions.txt"), "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(lines) + "\n")
    with open(os.path.join(ASSETS, "WORDNET-LICENSE.txt"), "w", encoding="utf-8", newline="\n") as f:
        f.write(licence(folder))
    print(len(lines), "definitions,", len(missing), "words without one")
    print("for example:", ", ".join(missing[:40]))


if __name__ == "__main__":
    main()
