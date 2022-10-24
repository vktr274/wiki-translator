FROM python:slim

WORKDIR /wiki

COPY cswiki-latest-pages-articles.xml.bz2 .

RUN pip install wikiextractor

CMD wikiextractor --json --output output cswiki-latest-pages-articles.xml.bz2