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
    const pageNumbers = document.querySelector('.page-numbers');
    let currentPage = 1;
    const pageSize = 12;

    function pageWindow(totalPages, activePage) {
        // Builds a compact, dynamic list of page numbers (with '...' gaps)
        // instead of hardcoding a fixed set of page buttons.
        const pages = [];
        if (totalPages <= 7) {
            for (let i = 1; i <= totalPages; i++) pages.push(i);
            return pages;
        }
        pages.push(1);
        const start = Math.max(2, activePage - 1);
        const end = Math.min(totalPages - 1, activePage + 1);
        if (start > 2) pages.push('...');
        for (let i = start; i <= end; i++) pages.push(i);
        if (end < totalPages - 1) pages.push('...');
        pages.push(totalPages);
        return pages;
    }

    function renderPageButtons(totalPages, activePage) {
        if (!pageNumbers) return;
        pageNumbers.innerHTML = '';
        pageWindow(totalPages, activePage).forEach(function (page) {
            const button = document.createElement('button');
            button.type = 'button';
            if (page === '...') {
                button.textContent = '…';
                button.disabled = true;
                button.setAttribute('aria-hidden', 'true');
            } else {
                button.textContent = String(page);
                button.dataset.page = String(page);
                if (page === activePage) button.classList.add('active');
                button.addEventListener('click', function () {
                    currentPage = page;
                    applyFilters();
                });
            }
            pageNumbers.appendChild(button);
        });
    }

    function applyFilters() {
        const query = search.value.trim().toLowerCase();
        const maxPrice = Number(price.value);
        const minimumRating = Number(rating.value);
        let visible = cards.filter(function (card) {
            const matchesQuery = !query || (card.dataset.title + ' ' + card.dataset.author).toLowerCase().includes(query);
            return matchesQuery && (!category.value || card.dataset.category === category.value) && (!author.value || card.dataset.author === author.value) && (!language.value || card.dataset.language === language.value) && Number(card.dataset.rating) >= minimumRating && Number(card.dataset.price) <= maxPrice;
        });
        visible.sort(function (first, second) {
            if (sort.value === 'price-low') return Number(first.dataset.price) - Number(second.dataset.price);
            if (sort.value === 'price-high') return Number(second.dataset.price) - Number(first.dataset.price);
            if (sort.value === 'rating') return Number(second.dataset.rating) - Number(first.dataset.rating);
            if (sort.value === 'popular') return Number(second.dataset.popular) - Number(first.dataset.popular);
            return cards.indexOf(first) - cards.indexOf(second);
        });
        const totalPages = Math.max(1, Math.ceil(visible.length / pageSize));
        currentPage = Math.min(currentPage, totalPages);
        cards.forEach(function (card) { card.classList.add('d-none'); });
        const start = (currentPage - 1) * pageSize;
        visible.slice(start, start + pageSize).forEach(function (card) { card.classList.remove('d-none'); });
        count.textContent = visible.length;
        empty.classList.toggle('d-none', visible.length > 0);
        if (prevButton) prevButton.disabled = currentPage === 1;
        if (nextButton) nextButton.disabled = currentPage >= totalPages;
        renderPageButtons(totalPages, currentPage);
        // Cards revealed by filtering/pagination were hidden (display:none) when AOS
        // last scanned the page, so AOS never marked them animated - they'd otherwise
        // stay stuck at their pre-animation opacity/transform and visually overlap the
        // content below them. Re-scanning after every visibility change fixes that
        // without altering the animation itself.
        if (window.AOS) AOS.refresh();
    }

    [search, category, author, language, rating, sort, price].forEach(function (control) { control.addEventListener('input', function () { currentPage = 1; applyFilters(); }); });
    document.querySelectorAll('.sidebar-link').forEach(function (button) { button.addEventListener('click', function () { category.value = button.dataset.category; document.querySelectorAll('.sidebar-link').forEach(function (item) { item.classList.remove('active'); }); button.classList.add('active'); currentPage = 1; applyFilters(); }); });
    price.addEventListener('input', function () { priceValue.textContent = '₹' + Number(price.value).toLocaleString('en-IN'); });
    if (prevButton) prevButton.addEventListener('click', function () { currentPage = Math.max(1, currentPage - 1); applyFilters(); });
    if (nextButton) nextButton.addEventListener('click', function () { currentPage = currentPage + 1; applyFilters(); });
    document.querySelectorAll('.wishlist-btn').forEach(function (button) { button.addEventListener('click', function () { button.classList.toggle('is-saved'); button.querySelector('i').classList.toggle('far'); button.querySelector('i').classList.toggle('fas'); }); });
    if (window.AOS) AOS.refresh();
    applyFilters();
});
