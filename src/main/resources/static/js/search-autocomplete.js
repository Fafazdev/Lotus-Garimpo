// Shared autocomplete for the search bar across pages.
(function () {
    document.addEventListener('DOMContentLoaded', function () {
        const searchContainer = document.querySelector('.search-container');
        const searchInput = document.querySelector('.search-container .search-input');
        if (!searchContainer || !searchInput) {
            return;
        }

        const suggestionsPanel = document.createElement('div');
        suggestionsPanel.className = 'search-suggestions';
        suggestionsPanel.setAttribute('role', 'listbox');
        searchContainer.appendChild(suggestionsPanel);

        let debounceTimer = null;
        let pendingController = null;
        let currentSuggestions = [];
        let activeIndex = -1;

        const redirectToCategoryResults = function (term) {
            const value = (term || '').trim();
            if (!value) {
                return;
            }

            const fallback = function () {
                window.location.assign('/produtos?busca=' + encodeURIComponent(value));
            };

            fetch('/produto/categoria-por-termo?q=' + encodeURIComponent(value), {
                headers: {
                    Accept: 'application/json'
                }
            })
                .then(function (response) {
                    if (!response.ok) {
                        return null;
                    }
                    return response.json();
                })
                .then(function (payload) {
                    const categoria = (payload && payload.categoria ? payload.categoria : '').toString().trim();

                    if (categoria) {
                        window.location.assign('/produtos?categoriaBusca=' + encodeURIComponent(categoria));
                        return;
                    }

                    fallback();
                })
                .catch(function () {
                    fallback();
                });
        };

        const hideSuggestions = function () {
            suggestionsPanel.classList.remove('is-open');
            suggestionsPanel.innerHTML = '';
            currentSuggestions = [];
            activeIndex = -1;
        };

        const setActiveSuggestion = function (index) {
            const items = suggestionsPanel.querySelectorAll('.search-suggestion-item');
            items.forEach(function (item, itemIndex) {
                item.classList.toggle('is-active', itemIndex === index);
            });
            activeIndex = index;
        };

        const selectSuggestion = function (value) {
            searchInput.value = value;
            hideSuggestions();

            // Trigger existing search listeners (product filters) after selecting a suggestion.
            searchInput.dispatchEvent(new Event('input', { bubbles: true }));
        };

        const renderSuggestions = function (items) {
            suggestionsPanel.innerHTML = '';
            currentSuggestions = items.slice();
            activeIndex = -1;

            if (!currentSuggestions.length) {
                hideSuggestions();
                return;
            }

            currentSuggestions.forEach(function (item) {
                const optionButton = document.createElement('button');
                optionButton.type = 'button';
                optionButton.className = 'search-suggestion-item';
                optionButton.setAttribute('role', 'option');
                optionButton.textContent = item;

                optionButton.addEventListener('mousedown', function (event) {
                    event.preventDefault();
                    selectSuggestion(item);
                });

                suggestionsPanel.appendChild(optionButton);
            });

            suggestionsPanel.classList.add('is-open');
        };

        const fetchSuggestions = function (term) {
            if (!term) {
                hideSuggestions();
                return;
            }

            if (pendingController) {
                pendingController.abort();
            }

            pendingController = new AbortController();

            fetch('/produto/sugestoes?q=' + encodeURIComponent(term), {
                headers: {
                    Accept: 'application/json'
                },
                signal: pendingController.signal
            })
                .then(function (response) {
                    if (!response.ok) {
                        return [];
                    }

                    return response.json();
                })
                .then(function (payload) {
                    if (!Array.isArray(payload)) {
                        hideSuggestions();
                        return;
                    }

                    const suggestions = payload
                        .map(function (value) {
                            return (value || '').toString().trim();
                        })
                        .filter(function (value) {
                            return value.length > 0;
                        })
                        .slice(0, 10);

                    renderSuggestions(suggestions);
                })
                .catch(function (error) {
                    if (error && error.name !== 'AbortError') {
                        hideSuggestions();
                    }
                });
        };

        searchInput.addEventListener('input', function () {
            const term = searchInput.value.trim();
            window.clearTimeout(debounceTimer);

            debounceTimer = window.setTimeout(function () {
                fetchSuggestions(term);
            }, 180);
        });

        searchInput.addEventListener('focus', function () {
            const term = searchInput.value.trim();
            if (term) {
                fetchSuggestions(term);
            }
        });

        searchInput.addEventListener('keydown', function (event) {
            if (event.key === 'ArrowDown') {
                if (!currentSuggestions.length) {
                    return;
                }
                event.preventDefault();
                const nextIndex = activeIndex < currentSuggestions.length - 1 ? activeIndex + 1 : 0;
                setActiveSuggestion(nextIndex);
            } else if (event.key === 'ArrowUp') {
                if (!currentSuggestions.length) {
                    return;
                }
                event.preventDefault();
                const nextIndex = activeIndex > 0 ? activeIndex - 1 : currentSuggestions.length - 1;
                setActiveSuggestion(nextIndex);
            } else if (event.key === 'Enter') {
                event.preventDefault();

                const selectedValue = activeIndex >= 0
                    ? currentSuggestions[activeIndex]
                    : searchInput.value.trim();

                if (!selectedValue) {
                    return;
                }

                if (activeIndex >= 0) {
                    selectSuggestion(selectedValue);
                } else {
                    hideSuggestions();
                }

                redirectToCategoryResults(selectedValue);
            } else if (event.key === 'Escape') {
                hideSuggestions();
            }
        });

        document.addEventListener('click', function (event) {
            if (!searchContainer.contains(event.target)) {
                hideSuggestions();
            }
        });
    });
})();
