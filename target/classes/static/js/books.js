document.addEventListener('DOMContentLoaded', function () {
    const cards = Array.from(document.querySelectorAll('.catalog-card'));
    const search = document.getElementById('bookSearch');
    const category = document.getElementById('categoryFilter');
    const author = document.getElementById('authorFilter');
    const language = document.getElementById('languageFilter');
    const rating = document.getElementById('ratingFilter');
    const sort = document.getElementById('sortBooks');
    const price = document.getElementById('priceRange');
    const priceValue = document.getElementById('priceValue');
    const count = document.getElementById('resultsCount');
    const empty = document.getElementById('emptyState');
    const prevButton = document.querySelector('[data-page="previous"]');
    const nextButton = document.querySelector('[data-page="next"]');
    const pageNumbers = document.getElementById('pageNumbers');
    let currentPage = 1;
    const pageSize = 12;

    function matchesQuery(card, query) {
        if (!query) return true;
        const haystack = [card.dataset.title, card.dataset.author, card.dataset.category, card.dataset.language]
            .join(' ')
            .toLowerCase();
        return haystack.includes(query);
    }

    function renderPageNumbers(current, totalPages) {
        pageNumbers.innerHTML = '';
        if (totalPages <= 1) return;

        const pagesToShow = new Set([1, totalPages]);
        for (let p = current - 1; p <= current + 1; p++) {
            if (p >= 1 && p <= totalPages) pagesToShow.add(p);
        }
        const sortedPages = Array.from(pagesToShow).sort(function (a, b) { return a - b; });

        let previousPage = 0;
        sortedPages.forEach(function (pageNumber) {
            if (previousPage && pageNumber - previousPage > 1) {
                const ellipsis = document.createElement('span');
                ellipsis.textContent = '…';
                ellipsis.setAttribute('aria-hidden', 'true');
                ellipsis.style.cssText = 'padding:0 .3rem;color:#a59387;';
                pageNumbers.appendChild(ellipsis);
            }
            const button = document.createElement('button');
            button.type = 'button';
            button.textContent = String(pageNumber);
            button.dataset.page = String(pageNumber);
            if (pageNumber === current) button.classList.add('active');
            button.addEventListener('click', function () {
                currentPage = pageNumber;
                applyFilters();
            });
            pageNumbers.appendChild(button);
            previousPage = pageNumber;
        });
    }

    function applyFilters() {
        const query = search.value.trim().toLowerCase();
        const maxPrice = Number(price.value);
        const minimumRating = Number(rating.value);
        let visible = cards.filter(function (card) {
            return matchesQuery(card, query) &&
                (!category.value || card.dataset.category === category.value) &&
                (!author.value || card.dataset.author === author.value) &&
                (!language.value || card.dataset.language === language.value) &&
                Number(card.dataset.rating) >= minimumRating &&
                Number(card.dataset.price) <= maxPrice;
        });
        visible.sort(function (first, second) {
            if (sort.value === 'price-low') return Number(first.dataset.price) - Number(second.dataset.price);
            if (sort.value === 'price-high') return Number(second.dataset.price) - Number(first.dataset.price);
            if (sort.value === 'rating') return Number(second.dataset.rating) - Number(first.dataset.rating);
            if (sort.value === 'popular') return Number(second.dataset.popular) - Number(first.dataset.popular);
            if (sort.value === 'newest') return new Date(second.dataset.published) - new Date(first.dataset.published);
            return cards.indexOf(first) - cards.indexOf(second);
        });

        const totalPages = Math.max(1, Math.ceil(visible.length / pageSize));
        currentPage = Math.min(Math.max(1, currentPage), totalPages);

        cards.forEach(function (card) { card.classList.add('d-none'); });
        const start = (currentPage - 1) * pageSize;
        visible.slice(start, start + pageSize).forEach(function (card) { card.classList.remove('d-none'); });

        count.textContent = visible.length;
        empty.classList.toggle('d-none', visible.length > 0);

        prevButton.disabled = currentPage === 1;
        nextButton.disabled = currentPage >= totalPages;
        renderPageNumbers(currentPage, totalPages);
    }

    [search, category, author, language, rating, sort, price].forEach(function (control) {
        control.addEventListener('input', function () { currentPage = 1; applyFilters(); });
    });
    document.querySelectorAll('.sidebar-link').forEach(function (button) {
        button.addEventListener('click', function () {
            category.value = button.dataset.category;
            document.querySelectorAll('.sidebar-link').forEach(function (item) { item.classList.remove('active'); });
            button.classList.add('active');
            currentPage = 1;
            applyFilters();
        });
    });
    price.addEventListener('input', function () { priceValue.textContent = '₹' + Number(price.value).toLocaleString('en-IN'); });
    prevButton.addEventListener('click', function () { currentPage = Math.max(1, currentPage - 1); applyFilters(); });
    nextButton.addEventListener('click', function () { currentPage = currentPage + 1; applyFilters(); });
    document.querySelectorAll('.wishlist-btn').forEach(function (button) {
        button.addEventListener('click', function () {
            button.classList.toggle('is-saved');
            button.querySelector('i').classList.toggle('far');
            button.querySelector('i').classList.toggle('fas');
        });
    });
    if (window.AOS) AOS.refresh();
    applyFilters();
});
