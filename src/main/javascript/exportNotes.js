function currentSlideNum() {
    return Math.round(Reveal.getProgress() * (Reveal.getTotalSlides() - 1) + 1);
}

function nextNote() {
    for (;Reveal.getProgress() < 1;  Reveal.next()) {
        var slide = Reveal.getCurrentSlide();
        var notes = slide.querySelector("aside.notes");
        var titleElem = slide.querySelector("h2");
        var title = "(No title)"
        if (titleElem) {
            title = titleElem.innerHTML;
        }
        if (notes) {
            var retVal = {
                number: currentSlideNum(),
                notes: notes.innerHTML,
                title: title
            }
            Reveal.next();
            return retVal;
        }
    }
    return null;
};
JSON.stringify(nextNote())