#! /usr/bin/env python
# -*- coding: utf-8 -*-

# cat links_rewrites.csv | python3 make-links-rewrites.py > links_rewrites.md

import sys
import base64
from urllib.request import urlopen

DOCS_PREFIX = 'https://docs.google.com/spreadsheets/d/'

def make_links(types, tag, doc):
    return ', '.join(make_link(tag, x, doc) for x in types)

def make_link(tag, out, doc):
    if out == 'tsv':
        return '[TSV]({0}{1}/export?format=tsv)'.format(DOCS_PREFIX, tag)
    if out == 'sheets':
        return '[Sheets]({0}{1}/edit?usp=sharing)'.format(DOCS_PREFIX, tag)
    if out == 'install':
        save_file(
            make_install_page(doc, '[PAIGALDA]({0})'.format(enc_url_content(tag))),
            tag + '.md'
        )
        return '[Paigalda]({0}.html)'.format(tag)
    if out == 'base64':
        return '[k6-URI]({0})'.format(enc_url_content(tag))
    return 'UNKNOWN_TYPE'

def enc_url_content(tag):
    f = urlopen('{0}{1}/export?format=tsv'.format(DOCS_PREFIX, tag))
    return 'k6://' + base64.urlsafe_b64encode(f.read()).decode('utf-8')

def save_file(content, name):
    with open(name, 'w') as text_file:
        text_file.write(content)

def make_md_line(tag, doc, level):
    return '{0} [{1}] {2}'.format(
        (' ' * 2 * level) + '-',
        make_links(['tsv', 'sheets', 'install'], tag, doc),
        doc
    )

def get_fields(line):
    fields = line.split('\t')
    if len(fields) == 1:
        return fields[0], '', 0
    if len(fields) == 2:
        return fields[0], fields[1], 0
    return fields[0], fields[1], int(fields[2])

def get_header(title):
    return '''---
layout: page
comments: true
title: {title}
---'''.format(title=title)

def make_install_page(lead, content):
    return '''---
layout: page
---

{lead}

{content}
'''.format(lead=lead, content=content)

def main():
    print(get_header('Ümberkirjutusreeglid'))
    for idx, line in enumerate(sys.stdin):
        line = line.strip()
        tag, doc, level = get_fields(line)
        print('Processing: {0}: {1}'.format(idx, line), file=sys.stderr)
        print(make_md_line(tag, doc, level))

if __name__ == "__main__":
    main()
