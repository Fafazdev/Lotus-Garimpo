// Funções específicas da página Produtos

// Clique na imagem do produto para ver em tela cheia
document.addEventListener('DOMContentLoaded', function () {
    const modalElement = document.getElementById('imageViewModal');
    if (!modalElement || typeof bootstrap === 'undefined') return;

    const imageModal = new bootstrap.Modal(modalElement);
    const modalImg = document.getElementById('imageViewModalImg');

    document.querySelectorAll('.product-image-fullscreen').forEach(function (img) {
        img.addEventListener('click', function () {
            const fullSrc = img.getAttribute('data-full-src') || img.getAttribute('src');
            modalImg.src = fullSrc;
            imageModal.show();
        });
    });
});

// Abrir modal de edição preenchendo com dados do card + confirmação de exclusão
document.addEventListener('DOMContentLoaded', function () {
    if (typeof bootstrap === 'undefined') return;

    // Edição
    const editButtons = document.querySelectorAll('.edit-card-button');
    const editModalElement = document.getElementById('editPieceModal');
    if (editModalElement) {
        const editModal = new bootstrap.Modal(editModalElement);

        editButtons.forEach(btn => {
            btn.addEventListener('click', function () {
                const id = btn.getAttribute('data-id');
                const nome = btn.getAttribute('data-nome') || '';
                const descricao = btn.getAttribute('data-descricao') || '';
                const preco = btn.getAttribute('data-preco') || '';
                const tamanho = btn.getAttribute('data-tamanho') || '';
                const categoria = btn.getAttribute('data-categoria') || '';
                const imagem = btn.getAttribute('data-imagem') || '';

                document.getElementById('editPieceId').value = id;
                document.getElementById('editPieceNome').value = nome;
                document.getElementById('editPieceDescricao').value = descricao;
                document.getElementById('editPiecePreco').value = preco;
                document.getElementById('editPieceTamanho').value = tamanho;
                document.getElementById('editPieceCategoria').value = categoria;

                const preview = document.getElementById('editPiecePreview');
                if (preview) {
                    preview.src = imagem || '/imagens/lotus.webp';
                }

                editModal.show();
            });
        });
    }

    // Confirmação de exclusão
    const deleteModalElement = document.getElementById('deleteConfirmModal');
    const confirmDeleteButton = document.getElementById('confirmDeleteButton');
    let deleteFormToSubmit = null;

    if (deleteModalElement && confirmDeleteButton) {
        const deleteModal = new bootstrap.Modal(deleteModalElement);

        document.querySelectorAll('.delete-card-button').forEach(btn => {
            btn.addEventListener('click', function (event) {
                event.preventDefault();
                deleteFormToSubmit = btn.closest('form');
                deleteModal.show();
            });
        });

        confirmDeleteButton.addEventListener('click', function () {
            if (deleteFormToSubmit) {
                deleteFormToSubmit.submit();
            }
        });
    }
});

// Mostrar alerta de sucesso quando um card for atualizado
(function () {
    document.addEventListener('DOMContentLoaded', function () {
        const params = new URLSearchParams(window.location.search);
        const sucesso = params.get('sucesso');

        if (sucesso === 'produtoAtualizado') {
            if (typeof showAlert === 'function') {
                showAlert('success', 'Peça atualizada', 'A peça foi atualizada com sucesso.');
            } else {
                // Fallback simples caso o componente de alerta não esteja disponível
                alert('Peça atualizada com sucesso.');
            }
        }
    });
})();

// Filtros da sidebar em dropdown: categorias, tamanhos e preços
(function () {
    document.addEventListener('DOMContentLoaded', function () {
        const shopSection = document.querySelector('.shop-section');
        if (!shopSection) return;

        const cards = Array.from(shopSection.querySelectorAll('.card-produto'));
        if (!cards.length) return;

        const categoryList = shopSection.querySelector('.sidebar-list');
        const sizeContainer = shopSection.querySelector('.filter-options');
        if (!categoryList || !sizeContainer) return;

        const searchInput = document.querySelector('.search-container .search-input');

        const normalize = function (value) {
            return (value || '')
                .toString()
                .normalize('NFD')
                .replace(/[\u0300-\u036f]/g, '')
                .toLowerCase()
                .trim();
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

                if (aIdx !== -1 && bIdx !== -1) {
                    return aIdx - bIdx;
                }
                if (aIdx !== -1) return -1;
                if (bIdx !== -1) return 1;

                return aLabel.localeCompare(bLabel, 'pt-BR');
            });
        };

        const normalizeSize = function (value) {
            return normalize(value).replace(/^tamanho\s+/, '').trim();
        };

        const faixasPreco = [
            { value: '', label: 'Todos os preços' },
            { value: '0-30', label: 'Até R$ 30' },
            { value: '30-50', label: 'R$ 30 a R$ 50' },
            { value: '50-80', label: 'R$ 50 a R$ 80' },
            { value: '80-120', label: 'R$ 80 a R$ 120' },
            { value: '120-160', label: 'R$ 120 a R$ 160' },
            { value: '160-200', label: 'R$ 160 a R$ 200' },
            { value: '200-300', label: 'R$ 200 a R$ 300' },
            { value: '300-500', label: 'R$ 300 a R$ 500' },
            { value: '500+', label: 'Acima de R$ 500' }
        ];

        const pertenceFaixaPreco = function (preco, faixa) {
            if (!faixa) return true;
            if (faixa === '500+') return preco >= 500;

            const parts = faixa.split('-');
            if (parts.length !== 2) return true;

            const min = parseFloat(parts[0]);
            const max = parseFloat(parts[1]);
            if (!Number.isFinite(min) || !Number.isFinite(max)) return true;

            return preco >= min && preco <= max;
        };

        const produtos = cards.map(function (card) {
            const nome = card.querySelector('.title span')?.textContent || '';
            const descricao = card.querySelector('.description span')?.textContent || '';
            const categoria = (card.querySelector('.category span')?.textContent || '').trim();
            const tamanho = (card.querySelector('.item-list-button')?.textContent || '').trim();
            const precoTexto = card.querySelector('.price span')?.textContent || '';
            const tamanhoValue = normalizeSize(tamanho);

            return {
                coluna: card.closest('.col-12') || card.parentElement,
                categoriaLabel: categoria,
                categoriaValue: normalize(categoria),
                tamanhoLabel: tamanho,
                tamanhoValue: tamanhoValue,
                preco: parsePriceValue(precoTexto),
                textoBusca: normalize([nome, descricao, categoria, tamanho].join(' '))
            };
        });

        const categorias = new Map();
        const tamanhos = new Map();

        produtos.forEach(function (produto) {
            if (produto.categoriaValue && produto.categoriaValue !== 'categoria') {
                const itemCategoria = categorias.get(produto.categoriaValue) || {
                    label: produto.categoriaLabel,
                    count: 0
                };
                itemCategoria.count += 1;
                categorias.set(produto.categoriaValue, itemCategoria);
            }

            if (produto.tamanhoValue && produto.tamanhoValue !== 'tamanho') {
                const itemTamanho = tamanhos.get(produto.tamanhoValue) || {
                    label: produto.tamanhoLabel.replace(/^\s*Tamanho\s+/i, '').trim(),
                    count: 0
                };
                itemTamanho.count += 1;
                tamanhos.set(produto.tamanhoValue, itemTamanho);
            }
        });

        categoryList.innerHTML = '';
        const categoryItem = document.createElement('li');
        const categorySelect = document.createElement('select');
        categorySelect.className = 'filter-select';
        categorySelect.setAttribute('aria-label', 'Filtrar por categoria');

        const allCategoriesOption = document.createElement('option');
        allCategoriesOption.value = '';
        allCategoriesOption.textContent = 'Todas as categorias';
        categorySelect.appendChild(allCategoriesOption);

        Array.from(categorias.entries())
            .sort(function (a, b) { return a[1].label.localeCompare(b[1].label, 'pt-BR'); })
            .forEach(function ([value, data]) {
                const option = document.createElement('option');
                option.value = value;
                option.textContent = data.label + ' (' + data.count + ')';
                categorySelect.appendChild(option);
            });

        categoryItem.appendChild(categorySelect);
        categoryList.appendChild(categoryItem);

        sizeContainer.innerHTML = '';
        const sizeSelect = document.createElement('select');
        sizeSelect.className = 'filter-select';
        sizeSelect.setAttribute('aria-label', 'Filtrar por tamanho');

        const allSizesOption = document.createElement('option');
        allSizesOption.value = '';
        allSizesOption.textContent = 'Todos os tamanhos';
        sizeSelect.appendChild(allSizesOption);

        sortSizes(Array.from(tamanhos.entries())).forEach(function ([value, data]) {
            const option = document.createElement('option');
            option.value = value;
            option.textContent = data.label + ' (' + data.count + ')';
            sizeSelect.appendChild(option);
        });

        sizeContainer.appendChild(sizeSelect);

        let priceSubtitle = shopSection.querySelector('.filter-price-subtitle');
        if (!priceSubtitle) {
            priceSubtitle = document.createElement('h3');
            priceSubtitle.className = 'sidebar-subtitle mt-4 filter-price-subtitle';
            priceSubtitle.textContent = 'Preço';
            sizeContainer.insertAdjacentElement('afterend', priceSubtitle);
        }

        let priceContainer = shopSection.querySelector('.filter-price-options');
        if (!priceContainer) {
            priceContainer = document.createElement('div');
            priceContainer.className = 'filter-price-options';
            priceSubtitle.insertAdjacentElement('afterend', priceContainer);
        }

        priceContainer.innerHTML = '';
        const priceSelect = document.createElement('select');
        priceSelect.className = 'filter-select';
        priceSelect.setAttribute('aria-label', 'Filtrar por preço');

        faixasPreco.forEach(function (faixa) {
            const option = document.createElement('option');
            option.value = faixa.value;
            option.textContent = faixa.label;
            priceSelect.appendChild(option);
        });

        priceContainer.appendChild(priceSelect);

        const aplicarFiltros = function () {
            const categoriaSelecionada = categorySelect.value || '';
            const tamanhoSelecionado = sizeSelect.value || '';
            const faixaSelecionada = priceSelect.value || '';
            const termoBusca = normalize(searchInput?.value || '');

            produtos.forEach(function (produto) {
                const passouCategoria = !categoriaSelecionada || produto.categoriaValue === categoriaSelecionada;
                const passouTamanho = !tamanhoSelecionado || produto.tamanhoValue === tamanhoSelecionado;
                const passouPreco = pertenceFaixaPreco(produto.preco, faixaSelecionada);
                const passouBusca = !termoBusca || produto.textoBusca.includes(termoBusca);

                produto.coluna.style.display = (passouCategoria && passouTamanho && passouPreco && passouBusca) ? '' : 'none';
            });
        };

        categorySelect.addEventListener('change', aplicarFiltros);
        sizeSelect.addEventListener('change', aplicarFiltros);
        priceSelect.addEventListener('change', aplicarFiltros);

        if (searchInput) {
            searchInput.addEventListener('input', aplicarFiltros);
        }

        aplicarFiltros();
    });
})();
