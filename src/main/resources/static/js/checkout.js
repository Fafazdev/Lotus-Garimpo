document.addEventListener('DOMContentLoaded', function () {
    const form = document.querySelector('.endereco-form');
    if (!form) return;

    const radios = form.querySelectorAll('input[name="enderecoId"]');
    radios.forEach(function (radio) {
        radio.addEventListener('change', function () {
            if (form) {
                form.submit();
            }
        });
    });
});
