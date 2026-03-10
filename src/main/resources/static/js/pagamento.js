document.addEventListener('DOMContentLoaded', function () {
    var spanValorAgora = document.getElementById('valor-pagar-agora');
    var spanEnvio = document.getElementById('valor-total-envio');
    var spanSacolinha = document.getElementById('valor-total-sacolinha');
    var radioEnvio = document.getElementById('tipoEnvio');
    var radioSacolinha = document.getElementById('tipoSacola');

    if (!spanValorAgora || !spanEnvio || !spanSacolinha || !radioEnvio || !radioSacolinha) {
        return;
    }

    function formatar(valorTexto) {
        // valorTexto vem como "123.45"; convertemos para "123,45"
        return valorTexto.replace('.', ',');
    }

    function atualizarValor() {
        var valorBase;
        if (radioSacolinha.checked) {
            valorBase = spanSacolinha.textContent.trim();
        } else {
            valorBase = spanEnvio.textContent.trim();
        }

        spanValorAgora.textContent = 'R$ ' + formatar(valorBase);
    }

    radioEnvio.addEventListener('change', atualizarValor);
    radioSacolinha.addEventListener('change', atualizarValor);

    // Ajusta logo na carga da página
    atualizarValor();
});
