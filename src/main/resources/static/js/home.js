// Funções específicas da página Home

// Clique na imagem do produto para ver em tela cheia e controles de edição/exclusão
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

    // Modal de edição de peça (home)
    const editModalElement = document.getElementById('editPieceModal');
    if (editModalElement) {
        const editModal = new bootstrap.Modal(editModalElement);

        // Função auxiliar para sincronizar os dropdowns customizados
        function syncCustomSelect(selectId, value) {
            const hiddenSelect = document.getElementById(selectId);
            const custom = document.querySelector('.custom-select[data-select-id="' + selectId + '"]');
            if (!hiddenSelect || !custom) return;

            if (value != null) {
                hiddenSelect.value = value;
            }

            const labelEl = custom.querySelector('.custom-select-label');
            const placeholder = custom.querySelector('.custom-select-toggle')?.getAttribute('data-placeholder') || '';
            const option = hiddenSelect.options[hiddenSelect.selectedIndex];
            labelEl.textContent = option && option.value ? option.textContent : placeholder;

            custom.querySelectorAll('.custom-select-option').forEach(btn => {
                btn.classList.toggle('active', btn.getAttribute('data-value') === hiddenSelect.value);
            });
        }

        // Inicializa comportamento dos dropdowns customizados (clique e seleção)
        document.querySelectorAll('#editPieceModal .custom-select').forEach(custom => {
            const selectId = custom.getAttribute('data-select-id');
            const toggle = custom.querySelector('.custom-select-toggle');
            const options = custom.querySelectorAll('.custom-select-option');

            if (!selectId || !toggle) return;

            toggle.addEventListener('click', function (e) {
                e.stopPropagation();

                document.querySelectorAll('#editPieceModal .custom-select.open').forEach(other => {
                    if (other !== custom) {
                        other.classList.remove('open');
                    }
                });

                custom.classList.toggle('open');
            });

            options.forEach(btn => {
                btn.addEventListener('click', function (e) {
                    e.stopPropagation();
                    const value = btn.getAttribute('data-value') || '';
                    syncCustomSelect(selectId, value);
                    custom.classList.remove('open');
                });
            });
        });

        // Fecha dropdowns customizados ao clicar fora do modal
        document.addEventListener('click', function () {
            document.querySelectorAll('#editPieceModal .custom-select.open').forEach(custom => {
                custom.classList.remove('open');
            });
        });

        document.querySelectorAll('.edit-card-button').forEach(btn => {
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

                // Atualiza selects ocultos e dropdowns customizados
                document.getElementById('editPieceTamanho').value = tamanho;
                document.getElementById('editPieceCategoria').value = categoria;
                syncCustomSelect('editPieceTamanho');
                syncCustomSelect('editPieceCategoria');

                const preview = document.getElementById('editPiecePreview');
                if (preview) {
                    preview.src = imagem || '/imagens/lotus.webp';
                }

                editModal.show();
            });
        });
    }

    // Modal de confirmação de exclusão (home)
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

// Tratamento de parâmetros de erro/sucesso na URL (cadastro/login/edição de peça)
(function () {
    document.addEventListener('DOMContentLoaded', function () {
        const params = new URLSearchParams(window.location.search);
        const erro = params.get('erro');
        const sucesso = params.get('sucesso');

        if (typeof showAlert !== 'function') {
            return;
        }

        if (erro === 'email') {
            showAlert('error', 'Erro no cadastro', 'Este e-mail já está cadastrado. Tente fazer login ou use outro e-mail.');
        } else if (erro === 'senha') {
            showAlert('error', 'Erro no cadastro', 'As senhas não conferem. Verifique e tente novamente.');
        } else if (erro === 'cpf') {
            showAlert('error', 'Problema com CPF', 'CPF inválido ou já cadastrado. Verifique o dado informado.');
        } else if (erro === 'login') {
            // Erro ao tentar entrar na conta (e-mail ou senha incorretos)
            showAlert('error', 'Erro no login', 'E-mail ou senha incorretos. Verifique e tente novamente.');

            // Abre automaticamente o modal de login para o usuário corrigir
            const loginModalEl = document.getElementById('loginModal');
            if (loginModalEl && typeof bootstrap !== 'undefined') {
                const loginModal = new bootstrap.Modal(loginModalEl);
                loginModal.show();
            }
        } else if (sucesso === 'produtoAtualizado') {
            // Sucesso na atualização de um card de produto
            showAlert('success', 'Peça atualizada', 'A peça foi atualizada com sucesso.');
        }
    });
})();
