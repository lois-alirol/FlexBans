tailwind.config = {
    darkMode: 'class',
};

(function() {
    const theme = localStorage.getItem('darkMode');
    const prefersDarkScheme = window.matchMedia('(prefers-color-scheme: dark)').matches;

    if (theme === 'enabled') {
        document.documentElement.classList.add('dark');
    } else if (theme === 'disabled') {
        document.documentElement.classList.remove('dark');
    } else if (prefersDarkScheme) {
        document.documentElement.classList.add('dark');
    } else {
        document.documentElement.classList.remove('dark');
    }
})();