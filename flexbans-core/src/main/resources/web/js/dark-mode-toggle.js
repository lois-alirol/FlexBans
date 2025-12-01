document.addEventListener('DOMContentLoaded', () => {
    const darkModeToggle = document.getElementById('dark-mode-toggle');
    const darkModeIcon = document.getElementById('dark-mode-icon');

    const toggleDarkMode = () => {
        document.documentElement.classList.toggle('dark');

        darkModeIcon.classList.toggle('fa-moon');
        darkModeIcon.classList.toggle('fa-sun');

        if (document.documentElement.classList.contains('dark')) {
            localStorage.setItem('darkMode', 'enabled');
        } else {
            localStorage.setItem('darkMode', 'disabled');
        }
    };

    darkModeToggle.addEventListener('click', toggleDarkMode);

    if (document.documentElement.classList.contains('dark')) {
        darkModeIcon.classList.add('fa-sun');
        darkModeIcon.classList.remove('fa-moon');
    } else {
        darkModeIcon.classList.add('fa-moon');
        darkModeIcon.classList.remove('fa-sun');
    }
});