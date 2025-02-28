document.addEventListener('DOMContentLoaded', function() {
    const url = new URL(window.location);
    const searchInput = document.getElementById('searchInput');
    const clearButton = document.createElement('button');
    clearButton.innerHTML = '×';
    clearButton.className = 'clear-button text-xl p-2 text-gray-500';
    clearButton.style.display = 'none';
    searchInput.parentElement.appendChild(clearButton);

    const savedQuery = localStorage.getItem('searchQuery');
    if (savedQuery) {
        searchInput.value = savedQuery;
        clearButton.style.display = 'inline';
    }

    searchInput.addEventListener('input', function() {
        localStorage.setItem('searchQuery', searchInput.value);
        if (searchInput.value) {
            clearButton.style.display = 'inline';
        } else {
            clearButton.style.display = 'none';
        }
    });

    clearButton.addEventListener('click', function() {
        searchInput.value = '';
        localStorage.removeItem('searchQuery');
        clearButton.style.display = 'none';
        url.searchParams.delete('player')
        window.location.href = url.toString();
    });
});