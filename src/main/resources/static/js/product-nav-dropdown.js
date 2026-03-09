// Dropdown de caracteristicas no menu "Produtos" com links para filtros da pagina de produtos.
(function () {
    document.addEventListener('DOMContentLoaded', function () {
        const dropdown = document.querySelector('.nav-dropdown-produtos');
        if (!dropdown) return;

        const nameContainer = dropdown.querySelector('[data-caracteristica="nome"]');
        const categoryContainer = dropdown.querySelector('[data-caracteristica="categoria"]');
        const sizeContainer = dropdown.querySelector('[data-caracteristica="tamanho"]');
        const priceContainer = dropdown.querySelector('[data-caracteristica="preco"]');

        if (!nameContainer || !categoryContainer || !sizeContainer || !priceContainer) {
            return;
        }

        const cards = Array.from(document.querySelectorAll('.card-produto'));

        const normalize = function (value) {
            return (value || '')
                .toString()
                .normalize('NFD')
                .replace(/[\u0300-\u036f]/g, '')
                .toLowerCase()
                .trim();
        };

        const normalizeSize = function (value) {
            return normalize(value).replace(/^tamanho\s+/, '').trim();
        };

        const parsePriceValue = function (value) {
            const raw = (value || '').toString().replace(/[^\d,.-]/g, '');
            if (!raw) return 0;

            let normalized = raw;
            const hasComma = normalized.includes(',');
            const hasDot = normalized.includes('.');

            if (hasComma && hasDot) {
                const commaIndex = normalized.lastIndexOf(',');
                const dotIndex = normalized.lastIndexOf('.');
                if (commaIndex > dotIndex) {
                    normalized = normalized.replace(/\./g, '').replace(',', '.');
                } else {
                    normalized = normalized.replace(/,/g, '');
                }
            } else if (hasComma) {
                normalized = normalized.replace(',', '.');
            }

            const parsed = parseFloat(normalized);
            return Number.isFinite(parsed) ? parsed : 0;
        };

        const sortSizes = function (entries) {
            const order = ['pp', 'p', 'm', 'g', 'gg', 'xg', 'xgg', 'u'];

            return entries.sort(function (a, b) {
                const aLabel = a[1].label;
                const bLabel = b[1].label;
                const aNum = Number.parseInt(aLabel, 10);
                const bNum = Number.parseInt(bLabel, 10);

                if (!Number.isNaN(aNum) && !Number.isNaN(bNum)) {
                    return aNum - bNum;
                }

                const aIdx = order.indexOf(normalize(aLabel));
                const bIdx = order.indexOf(normalize(bLabel));

                if (aIdx !== -1 && bIdx !== -1) return aIdx - bIdx;
                if (aIdx !== -1) return -1;
                if (bIdx !== -1) return 1;

                return aLabel.localeCompare(bLabel, 'pt-BR');
            });
        };

        const faixasPreco = [
            { value: '0-30', label: 'Ate R$ 30', min: 0, max: 30 },
            { value: '30-50', label: 'R$ 30 a R$ 50', min: 30, max: 50 },
            { value: '50-80', label: 'R$ 50 a R$ 80', min: 50, max: 80 },
            { value: '80-120', label: 'R$ 80 a R$ 120', min: 80, max: 120 },
            { value: '120-160', label: 'R$ 120 a R$ 160', min: 120, max: 160 },
            { value: '160-200', label: 'R$ 160 a R$ 200', min: 160, max: 200 },
            { value: '200-300', label: 'R$ 200 a R$ 300', min: 200, max: 300 },
            { value: '300-500', label: 'R$ 300 a R$ 500', min: 300, max: 500 },
            { value: '500+', label: 'Acima de R$ 500', min: 500, max: null }
        ];

        const categoriaMap = new Map();
        const nomeMap = new Map();
        const tamanhoMap = new Map();
        const precoMap = new Map();

        cards.forEach(function (card) {
            const nomeLabel = (card.querySelector('.title span')?.textContent || '').trim();
            const nomeValue = normalize(nomeLabel);
            if (nomeValue && nomeValue !== 'produto' && nomeValue !== 'nome do produto') {
                const nomeAtual = nomeMap.get(nomeValue) || { label: nomeLabel, count: 0 };
                nomeAtual.count += 1;
                nomeMap.set(nomeValue, nomeAtual);
            }

            const categoriaLabel = (card.querySelector('.category span')?.textContent || '').trim();
            const categoriaValue = normalize(categoriaLabel);
            if (categoriaValue && categoriaValue !== 'categoria') {
                const categoriaAtual = categoriaMap.get(categoriaValue) || { label: categoriaLabel, count: 0 };
                categoriaAtual.count += 1;
                categoriaMap.set(categoriaValue, categoriaAtual);
            }

            const tamanhoOriginal = (card.querySelector('.item-list-button')?.textContent || '').trim();
            const tamanhoLabel = tamanhoOriginal.replace(/^\s*Tamanho\s+/i, '').trim();
            const tamanhoValue = normalizeSize(tamanhoOriginal);
            if (tamanhoValue && tamanhoValue !== 'tamanho') {
                const tamanhoAtual = tamanhoMap.get(tamanhoValue) || { label: tamanhoLabel, count: 0 };
                tamanhoAtual.count += 1;
                tamanhoMap.set(tamanhoValue, tamanhoAtual);
            }

            const preco = parsePriceValue(card.querySelector('.price span')?.textContent || '');
            if (preco > 0) {
                faixasPreco.forEach(function (faixa) {
                    const dentroDaFaixa = faixa.max == null
                        ? preco >= faixa.min
                        : preco >= faixa.min && preco <= faixa.max;

                    if (dentroDaFaixa) {
                        const countAtual = precoMap.get(faixa.value) || 0;
                        precoMap.set(faixa.value, countAtual + 1);
                    }
                });
            }
        });

        const createLink = function (text, href) {
            const link = document.createElement('a');
            link.className = 'nav-dropdown-link';
            link.href = href;
            link.textContent = text;
            return link;
        };

        const buildCategoryLinks = function () {
            const links = [createLink('Todos', '/produtos')];
            const entries = Array.from(categoriaMap.entries())
                .sort(function (a, b) { return a[1].label.localeCompare(b[1].label, 'pt-BR'); });

            if (!entries.length) {
                ['Partes de cima', 'Partes de baixo', 'Acessorios', 'Outros'].forEach(function (categoria) {
                    links.push(createLink(categoria, '/produtos?categoriaBusca=' + encodeURIComponent(categoria)));
                });
                return links;
            }

            entries.forEach(function (entry) {
                const data = entry[1];
                links.push(createLink(data.label + ' (' + data.count + ')', '/produtos?categoriaBusca=' + encodeURIComponent(data.label)));
            });

            return links;
        };

        const buildNameLinks = function () {
            const links = [createLink('Todos', '/produtos')];
            const entries = Array.from(nomeMap.entries())
                .sort(function (a, b) { return a[1].label.localeCompare(b[1].label, 'pt-BR'); })
                .slice(0, 10);

            if (!entries.length) {
                return links;
            }

            entries.forEach(function (entry) {
                const data = entry[1];
                links.push(createLink(data.label + ' (' + data.count + ')', '/produtos?busca=' + encodeURIComponent(data.label)));
            });

            return links;
        };

        const buildSizeLinks = function () {
            const links = [createLink('Todos', '/produtos')];
            const entries = sortSizes(Array.from(tamanhoMap.entries()));

            if (!entries.length) {
                ['PP', 'P', 'M', 'G', 'GG', 'U'].forEach(function (tamanho) {
                    links.push(createLink(tamanho, '/produtos?tamanhoBusca=' + encodeURIComponent(tamanho)));
                });
                return links;
            }

            entries.forEach(function (entry) {
                const value = entry[0];
                const data = entry[1];
                links.push(createLink(data.label + ' (' + data.count + ')', '/produtos?tamanhoBusca=' + encodeURIComponent(value)));
            });

            return links;
        };

        const buildPriceLinks = function () {
            const links = [createLink('Todos', '/produtos')];

            faixasPreco.forEach(function (faixa) {
                const count = precoMap.get(faixa.value) || 0;
                if (cards.length && count === 0) {
                    return;
                }

                links.push(createLink(faixa.label + (count > 0 ? ' (' + count + ')' : ''), '/produtos?faixaPreco=' + encodeURIComponent(faixa.value)));
            });

            return links;
        };

        const renderLinks = function (container, links) {
            container.innerHTML = '';
            links.forEach(function (link) {
                container.appendChild(link);
            });
        };

        renderLinks(nameContainer, buildNameLinks());
        renderLinks(categoryContainer, buildCategoryLinks());
        renderLinks(sizeContainer, buildSizeLinks());
        renderLinks(priceContainer, buildPriceLinks());
    });
})();
