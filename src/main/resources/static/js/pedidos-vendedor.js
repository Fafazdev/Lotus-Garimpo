(function () {
    const ctx = document.getElementById('vendasChart');
    if (ctx && window.vendasLabels && window.vendasValores) {
        try {
            const labels = Array.isArray(window.vendasLabels) ? window.vendasLabels : [];
            const valores = Array.isArray(window.vendasValores) ? window.vendasValores : [];

            if (labels.length && valores.length) {
                // eslint-disable-next-line no-undef
                new Chart(ctx, {
                    type: 'line',
                    data: {
                        labels: labels,
                        datasets: [{
                            label: 'Valor de Vendas (R$)',
                            data: valores,
                            borderColor: '#b54976',
                            backgroundColor: 'rgba(181, 73, 118, 0.2)',
                            borderWidth: 2,
                            fill: true,
                            tension: 0.4
                        }]
                    },
                    options: {
                        responsive: true,
                        plugins: { legend: { display: false } },
                        scales: {
                            y: { beginAtZero: true }
                        }
                    }
                });
            }
        } catch (e) {
            console.error('Erro ao montar gráfico de vendas:', e);
        }
    }

    document.addEventListener('DOMContentLoaded', function () {
        const pedidoCards = document.querySelectorAll('[data-pedido-status]');

        function normalizaStatus(valor) {
            return (valor || '').toString().trim().toUpperCase();
        }

        function filtrarPorStatus(statusEsperados) {
            const lista = (statusEsperados || []).map(s => normalizaStatus(s));

            pedidoCards.forEach(card => {
                const status = normalizaStatus(card.getAttribute('data-pedido-status'));
                if (!lista.length || lista.includes(status)) {
                    card.classList.remove('d-none');
                } else {
                    card.classList.add('d-none');
                }
            });
        }

        const btnSacolinha = document.getElementById('btnGerenciarSacolinha');
        if (btnSacolinha) {
            btnSacolinha.addEventListener('click', function () {
                filtrarPorStatus(['SACOLINHA_ABERTA']);
            });
        }

        const btnConcluidos = document.getElementById('btnVerConcluidos');
        if (btnConcluidos) {
            btnConcluidos.addEventListener('click', function () {
                filtrarPorStatus(['ENVIADO']);
            });
        }

        const btnEtiquetas = document.getElementById('btnImprimirEtiquetas');
        if (btnEtiquetas) {
            btnEtiquetas.addEventListener('click', function () {
                const url = btnEtiquetas.getAttribute('data-etiquetas-url');
                if (url) {
                    window.open(url, '_blank');
                }
            });
        }

        const btnExportar = document.getElementById('btnExportarMensal');
        if (btnExportar) {
            btnExportar.addEventListener('click', function () {
                const url = btnExportar.getAttribute('data-export-url');
                if (url) {
                    window.location.href = url;
                }
            });
        }
    });
})();
