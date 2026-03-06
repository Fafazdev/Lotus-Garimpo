function abrirCarrinho() {
    document.getElementById('cartModal').classList.add('active');
    document.getElementById('cartOverlay').classList.add('active');
    // Trava o scroll da página de fundo
    document.body.style.overflow = 'hidden'; 
}

function fecharCarrinho() {
    document.getElementById('cartModal').classList.remove('active');
    document.getElementById('cartOverlay').classList.remove('active');
    // Destrava o scroll da página de fundo
    document.body.style.overflow = 'auto'; 
}

// Remover item do carrinho via AJAX, sem recarregar a página
document.addEventListener('DOMContentLoaded', function () {
    var cartItemsContainer = document.querySelector('.cart-items');

    // Remoção de item do carrinho (sem recarregar)
    if (cartItemsContainer) {
        cartItemsContainer.addEventListener('click', function (event) {
        var button = event.target.closest('.btn-remove');
        if (!button) return;

        event.preventDefault();

        var itemId = button.getAttribute('data-item-id');
        if (!itemId) return;

        fetch('/carrinho/remover-ajax', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
                'X-Requested-With': 'XMLHttpRequest'
            },
            body: 'itemId=' + encodeURIComponent(itemId)
        })
        .then(function (response) {
            if (!response.ok) {
                throw new Error('Falha na requisição de remoção do carrinho');
            }
            return response.json();
        })
            .then(function (data) {
                if (!data.success) {
                    console.error(data.message || 'Não foi possível remover o item do carrinho');
                    return;
                }

                // Atualiza quantidade ou remove o item visualmente, conforme resposta
                atualizarResumoCarrinhoNoDom(data, button, cartItemsContainer);
            })
        .catch(function (error) {
            console.error(error);
        });
    });
    }

    // Intercepta botões "Comprar" para adicionar ao carrinho via AJAX
    var addForms = document.querySelectorAll('form[action="/carrinho/adicionar"]');
    addForms.forEach(function (form) {
        form.addEventListener('submit', function (event) {
            event.preventDefault();

            var produtoInput = form.querySelector('input[name="produtoId"]');
            if (!produtoInput) return;

            var produtoId = produtoInput.value;

            fetch('/carrinho/adicionar-ajax', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
                    'X-Requested-With': 'XMLHttpRequest'
                },
                body: 'produtoId=' + encodeURIComponent(produtoId)
            })
            .then(function (response) {
                if (!response.ok) {
                    throw new Error('Falha na requisição de adicionar ao carrinho');
                }
                return response.json();
            })
            .then(function (data) {
                if (!data.success) {
                    if (data.requiresLogin) {
                        var loginModalEl = document.getElementById('loginModal');
                        if (loginModalEl && typeof bootstrap !== 'undefined') {
                            var loginModal = new bootstrap.Modal(loginModalEl);
                            loginModal.show();
                        } else {
                            window.location.href = '/';
                        }
                    } else if (data.message) {
                        console.error(data.message);
                    }
                    return;
                }

                atualizarResumoCarrinhoNoDom(data, null, cartItemsContainer);

                // Garante que o item apareça imediatamente no modal
                sincronizarItemCarrinhoNoDom(data.novoItem, cartItemsContainer);

                // Mensagem de feedback ao usuário
                if (typeof showAlert === 'function') {
                    showAlert('success', 'Produto adicionado', 'O produto foi adicionado ao carrinho.');
                }
            })
            .catch(function (error) {
                console.error(error);
            });
        });
    });
});

// Atualiza subtotal/total, badge e estado "vazio" do carrinho no DOM
function atualizarResumoCarrinhoNoDom(data, removeButton, cartItemsContainer) {
    // Se veio de uma remoção, atualiza a quantidade do item ou remove o card
    if (removeButton && data) {
        var cartItem = removeButton.closest('.cart-item');
        if (cartItem) {
            var quantidadeItem = data.quantidadeItem;
            var itemRemovido = data.itemRemovido;

            if (itemRemovido === true || quantidadeItem == null || quantidadeItem <= 0) {
                cartItem.remove();
            } else {
                var qtySpan = cartItem.querySelector('.item-qty');
                if (qtySpan) {
                    qtySpan.textContent = 'Qtd: ' + quantidadeItem;
                }
            }
        }
    }

    // Atualiza subtotal e total
    var subtotalSpan = document.getElementById('cartSubtotal');
    var totalSpan = document.getElementById('cartTotal');
    if (subtotalSpan && data.subtotalFormatado != null) {
        subtotalSpan.textContent = 'R$ ' + data.subtotalFormatado;
    }
    if (totalSpan && data.subtotalFormatado != null) {
        totalSpan.textContent = 'R$ ' + data.subtotalFormatado;
    }

    // Atualiza o badge de quantidade em todos os headers
    var badges = document.querySelectorAll('.cart-badge');
    badges.forEach(function (badge) {
        badge.textContent = data.quantidadeCarrinho != null ? data.quantidadeCarrinho : 0;
    });

    if (!cartItemsContainer) {
        cartItemsContainer = document.querySelector('.cart-items');
    }

    if (!cartItemsContainer) return;

    // Se não houver mais itens, mostra a mensagem de carrinho vazio
    var remainingItems = cartItemsContainer.querySelectorAll('.cart-item').length;
    var emptyDiv = cartItemsContainer.querySelector('.cart-empty');
    if (remainingItems === 0) {
        if (!emptyDiv) {
            emptyDiv = document.createElement('div');
            emptyDiv.className = 'cart-empty';
            emptyDiv.textContent = 'Seu carrinho está vazio.';
            cartItemsContainer.appendChild(emptyDiv);
        }
    } else if (emptyDiv) {
        emptyDiv.remove();
    }
}

// Cria ou atualiza visualmente um item do carrinho no modal
function sincronizarItemCarrinhoNoDom(novoItem, cartItemsContainer) {
    if (!novoItem) return;

    if (!cartItemsContainer) {
        cartItemsContainer = document.querySelector('.cart-items');
    }
    if (!cartItemsContainer) return;

    // Remove mensagem de carrinho vazio, se existir
    var emptyDiv = cartItemsContainer.querySelector('.cart-empty');
    if (emptyDiv) {
        emptyDiv.remove();
    }

    var itemId = novoItem.id;
    if (!itemId) return;

    // Procura se já existe um card para esse item
    var existingRemoveBtn = cartItemsContainer.querySelector('.btn-remove[data-item-id="' + itemId + '"]');
    if (existingRemoveBtn) {
        var existingItem = existingRemoveBtn.closest('.cart-item');
        if (existingItem) {
            var qtySpan = existingItem.querySelector('.item-qty');
            if (qtySpan && novoItem.quantidade != null) {
                qtySpan.textContent = 'Qtd: ' + novoItem.quantidade;
            }
        }
        return;
    }

    // Cria um novo card de item
    var cartItem = document.createElement('div');
    cartItem.className = 'cart-item';

    var imagem = novoItem.imagem || '/imagens/lotus.webp';
    var nome = novoItem.nome || 'Produto';
    var tamanho = novoItem.tamanho || '';
    var preco = novoItem.precoFormatado || '0,00';
    var quantidade = novoItem.quantidade != null ? novoItem.quantidade : 1;

    cartItem.innerHTML =
        '<div class="item-image" style="background-color: #fbe5f0;">' +
            '<img src="' + imagem + '" alt="Produto" class="image" style="width: 40px; height: 40px; object-fit: cover; border-radius: 6px;">' +
        '</div>' +
        '<div class="item-details">' +
            '<span class="item-name">' + nome + '</span>' +
            '<span class="item-size">' + (tamanho ? ('Tamanho: ' + tamanho) : '') + '</span>' +
            '<span class="item-price">R$ ' + preco + '</span>' +
            '<div class="item-actions">' +
                '<span class="item-qty">Qtd: ' + quantidade + '</span>' +
                '<button class="btn-remove" type="button" data-item-id="' + itemId + '">Remover</button>' +
            '</div>' +
        '</div>';

    cartItemsContainer.appendChild(cartItem);
}

// Limpa todos os itens do carrinho via AJAX
function limparCarrinho() {
    var cartItemsContainer = document.querySelector('.cart-items');

    fetch('/carrinho/limpar-ajax', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
            'X-Requested-With': 'XMLHttpRequest'
        }
    })
        .then(function (response) {
            if (!response.ok) {
                throw new Error('Falha na requisição de limpar carrinho');
            }
            return response.json();
        })
        .then(function (data) {
            if (!data.success) {
                console.error(data.message || 'Não foi possível limpar o carrinho');
                return;
            }

            // Remove todos os cards de itens
            if (cartItemsContainer) {
                var items = cartItemsContainer.querySelectorAll('.cart-item');
                items.forEach(function (el) { el.remove(); });
            }

            // Atualiza resumo, badges e mensagem de carrinho vazio
            atualizarResumoCarrinhoNoDom(data, null, cartItemsContainer);

            if (typeof showAlert === 'function') {
                showAlert('success', 'Carrinho limpo', 'Todos os itens foram removidos do carrinho.');
            }
        })
        .catch(function (error) {
            console.error(error);
        });
}