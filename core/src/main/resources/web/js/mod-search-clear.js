document.addEventListener('DOMContentLoaded', function() {
    const url = new URL(window.location);
    const searchInput = document.getElementById('moderatorSearchInput');
    const clearButton = document.createElement('button');
    clearButton.innerHTML = '×';
    clearButton.className = 'clear-button text-xl text-gray-500 absolute right-4 top-1/2 transform -translate-y-1/2';
    clearButton.style.display = 'none';

    searchInput.parentElement.appendChild(clearButton);

    const savedQuery = localStorage.getItem('moderatorSearchQuery');
    if (savedQuery) {
        searchInput.value = savedQuery;
        clearButton.style.display = 'inline';
    }

    searchInput.addEventListener('input', function() {
        localStorage.setItem('moderatorSearchQuery', searchInput.value);
        if (searchInput.value) {
            clearButton.style.display = 'inline';
        } else {
            clearButton.style.display = 'none';
        }
    });

    clearButton.addEventListener('click', function() {
        searchInput.value = '';
        localStorage.removeItem('moderatorSearchQuery');
        clearButton.style.display = 'none';
        url.searchParams.delete('executor');
        window.location.href = url.toString();
    });
});
