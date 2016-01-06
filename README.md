## Reveal notes exporter

This program was specifically written for the typesafe training slidesets.
It can be used to export notes of a reveal slideset to markdown and html files.
These can then be printed for a handy reference in case no second screen is available during presentation.


### How to use
 1. Make sure you have JRE 1.8.40 or greater installed
 1. Run the com.github.stivo.revealnotesexporter.NoteExporter.scala with the reveal html as argument
    (must be a valid filename, e. g. /opt/deck/index.html or c:/index.html)
    1. The program will open the slide set
    2. It will start exporting the notes one by one (the window is not refreshed in that time)
    3. The program closes itself.
 1. The program wrote a markdown file and an html file.
    The html file will be automatically opened in the default browser.
 1. Print either of those files.

### Command line
You can currently set these arguments:

  * --fileName=testnotes => Sets the filename of the generated html and markdown file
  * /opt/deck/index.html (must be last argument) => Sets the deck with the notes to be exported

Easiest way to run it is with sbt:

    sbt run --fileName=testnotes /opt/deck/index.html

### Thanks
This program was heavily inspired by [deck2pdf](https://github.com/melix/deck2pdf).
Which I probably could have adapted, but this was more fun.

### License
[MIT](./LICENSE)
