FROM python:slim

WORKDIR /wiki

COPY huwiki-latest-pages-articles.xml.bz2 .

RUN pip install wikiextractor

CMD wikiextractor --json --output output huwiki-latest-pages-articles.xml.bz2