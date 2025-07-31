const LINE_HEIGHT = 24; // approx line height in px
const LINE_LIMIT = 7;
const COLLAPSED_HEIGHT = LINE_HEIGHT * LINE_LIMIT;

document.querySelectorAll('.ipsQuote').forEach(quote => {
    const content = quote.querySelector(':scope > .ipsQuote_contents');
    const label = quote.querySelector(':scope > .expand-label');
    const citation = quote.querySelector(':scope > .ipsQuote_citation');

    if (!content || !label) return; // Prevents crashes if structure isn't matched

    content.style.maxHeight = 'none';
    const fullHeight = content.scrollHeight;

    if (fullHeight <= COLLAPSED_HEIGHT) {
        label.classList.add('hidden');
        quote.setAttribute('data-expandable', 'false');
        return;
    }

    quote.setAttribute('data-expandable', 'true');
    content.style.maxHeight = COLLAPSED_HEIGHT + 'px';

    function toggleQuote() {
        const isExpanded = quote.getAttribute('data-expanded') === 'true';

        if (isExpanded) {
        content.style.maxHeight = COLLAPSED_HEIGHT + 'px';
        quote.setAttribute('data-expanded', 'false');
        label.textContent = 'Expand ▼';
        } else {
        // Expand all children temporarily
        const nestedQuotes = content.querySelectorAll('.ipsQuote_contents');
        const originalHeights = [];

        nestedQuotes.forEach(nested => {
            originalHeights.push(nested.style.maxHeight);
            nested.style.maxHeight = 'none';
        });

        // Now measure with all children fully open
        content.style.maxHeight = content.scrollHeight + 'px';

        // Restore collapsed children
        nestedQuotes.forEach((nested, i) => {
            nested.style.maxHeight = originalHeights[i];
        });

        quote.setAttribute('data-expanded', 'true');
        label.textContent = 'Collapse ▲';
        }
    }

    label.addEventListener('click', toggleQuote);
    if (citation) {
        citation.style.cursor = 'pointer';
        citation.addEventListener('click', (e) => {
          if (e.target.closest('.fa')) return; // prevent toggle if click is inside the menu button
          toggleQuote();
      });
    }
});