FROM python:slim

WORKDIR /wiki

COPY skwiki-latest-pages-articles.xml.bz2 .

RUN pip install wikiextractor

CMD wikiextractor --json --output output skwiki-latest-pages-articles.xml.bz2