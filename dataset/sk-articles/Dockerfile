FROM python:slim

WORKDIR /wiki

COPY skwiki-latest-pages-articles.xml.bz2 /wiki/

RUN pip install wikiextractor

CMD wikiextractor --json --no-templates --output /wiki/output /wiki/skwiki-latest-pages-articles.xml.bz2