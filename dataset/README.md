# Clear WikiText

Build: `docker build --pull --rm -t wikiextractor:latest .`

Run: `docker run --rm -it --mount type=bind,source=%cd%\output,target=/wiki/output wikiextractor:latest`
