FROM python:slim

WORKDIR /wiki

COPY huwiki-latest-pages-articles.xml.bz2 /wiki/

RUN pip install wikiextractor

CMD wikiextractor --json --no-templates --output /wiki/output /wiki/huwiki-latest-pages-articles.xml.bz2