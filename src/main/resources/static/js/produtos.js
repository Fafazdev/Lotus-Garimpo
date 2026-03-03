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
