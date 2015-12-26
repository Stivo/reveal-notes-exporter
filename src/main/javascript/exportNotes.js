function currentSlideNum() {
    return parseInt(Reveal.getProgress() * (Reveal.getTotalSlides() - 1) + 1);
}

function nextNote() {
    for (;Reveal.getProgress() < 0.3;  Reveal.next()) {
        var slide = Reveal.getCurrentSlide();
        var notes = slide.querySelector("aside.notes");
        if (notes) {
            var retVal = {
                number: currentSlideNum(),
                notes: notes.innerHTML,
                title: slide.querySelector("h2").innerHTML
            }
            Reveal.next();
            return retVal;
        }
    }
    return null;
};
JSON.stringify(nextNote())