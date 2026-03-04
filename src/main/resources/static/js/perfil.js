// Funções específicas da página Perfil

function editField(element, fieldName) {
    const form = element.querySelector('.editable-form');
    const input = form.querySelector('.editable-input');
    form.classList.add('active');
    input.focus();
}

let unsavedWarnings = {};

function cancelEdit(button) {
    const form = button.closest('.editable-form');

    // Remove o aviso
    const warning = form.querySelector('.unsaved-warning');
    if (warning) warning.remove();

    form.classList.remove('active');
}

function saveField(button, fieldName) {
    // Se for CEP, salvar CEP + endereço auto-preenchido
    if (fieldName === 'cep') {
        saveCepAndAddress(button);
        return;
    }

    const form = button.closest('.editable-form');
    const input = form.querySelector('.editable-input');
    const item = form.closest('.editable-item');
    const textDisplay = item.querySelector('.editable-text-display');
    const value = input.value.trim();

    if (!value) {
        showAlert('error', 'Erro ao atualizar', 'Campo não pode estar vazio.');
        return;
    }

    // Validação simples de CPF no perfil: 11 dígitos
    if (fieldName === 'cpf') {
        const digits = value.replace(/\D/g, '');
        if (digits.length !== 11) {
            showAlert('error', 'CPF inválido', 'O CPF deve ter 11 dígitos numéricos.');
            return;
        }
    }

    // Envia para o servidor
    fetch('/api/usuario/atualizar', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            campo: fieldName,
            valor: value
        })
    })
        .then(response => response.json())
        .then(data => {
            if (data.sucesso) {
                textDisplay.textContent = value;
                form.classList.remove('active');

                // Remove o aviso
                const warning = form.querySelector('.unsaved-warning');
                if (warning) warning.remove();

                console.log('Campo ' + fieldName + ' atualizado com sucesso!');
                const successTitle = fieldName === 'cpf' ? 'CPF atualizado' : 'Dados salvos';
                const successMsg = fieldName === 'cpf'
                    ? 'Seu CPF foi atualizado com sucesso.'
                    : 'O campo "' + fieldName + '" foi atualizado com sucesso.';
                showAlert('success', successTitle, successMsg);
            } else {
                console.error('Erro ao atualizar campo', fieldName, 'detalhes:', data);
                const errorTitle = fieldName === 'cpf' ? 'Erro com CPF' : 'Erro ao atualizar';
                const errorMsg = data.mensagem
                    ? data.mensagem
                    : (fieldName === 'cpf'
                        ? 'Não foi possível validar seu CPF. Tente novamente.'
                        : 'Erro desconhecido (campo: ' + fieldName + ')');
                showAlert('error', errorTitle, errorMsg);
            }
        })
        .catch(error => {
            console.error('Erro ao salvar dados do campo', fieldName, error);
            showAlert('error', 'Erro de comunicação', 'Não foi possível salvar os dados. Tente novamente.');
        });
}

// Função para salvar CEP e campos de endereço auto-preenchidos
function saveCepAndAddress(button) {
    const form = button.closest('.editable-form');
    const cepInput = form.querySelector('.editable-input');
    const cepValue = cepInput.value.trim();

    if (!cepValue) {
        showAlert('error', 'Erro ao atualizar', 'Campo CEP não pode estar vazio.');
        return;
    }

    // Coletar valores dos campos de endereço
    const endereco = document.getElementById('ruaInput').value.trim();
    const cidade = document.getElementById('cidadeInput').value.trim();
    const estado = document.getElementById('estadoInput').value.trim();
    const numero = document.getElementById('numeroInput').value.trim();
    const complemento = document.getElementById('complementoInput').value.trim();

    // Array de campos para salvar
    const fieldsToSave = [
        { campo: 'cep', valor: cepValue }
    ];

    if (endereco) fieldsToSave.push({ campo: 'endereco', valor: endereco });
    if (cidade) fieldsToSave.push({ campo: 'cidade', valor: cidade });
    if (estado) fieldsToSave.push({ campo: 'estado', valor: estado });
    if (numero) fieldsToSave.push({ campo: 'numero', valor: numero });
    if (complemento) fieldsToSave.push({ campo: 'complemento', valor: complemento });

    // Salvar cada campo
    let successCount = 0;
    let totalFields = fieldsToSave.length;

    fieldsToSave.forEach((fieldData, index) => {
        fetch('/api/usuario/atualizar', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(fieldData)
        })
            .then(response => response.json())
            .then(data => {
                if (data.sucesso) {
                    successCount++;

                    // Atualizar display e remover avisos para cada campo
                    const fieldElement = document.getElementById(fieldData.campo + 'Input');
                    if (fieldElement) {
                        const item = fieldElement.closest('.editable-item');
                        if (item) {
                            const textDisplay = item.querySelector('.editable-text-display');
                            if (textDisplay) textDisplay.textContent = fieldData.valor;

                            const editForm = item.querySelector('.editable-form');
                            if (editForm) {
                                editForm.classList.remove('active');
                                const warning = editForm.querySelector('.unsaved-warning');
                                if (warning) warning.remove();
                            }
                        }
                    }

                    console.log('Campo ' + fieldData.campo + ' atualizado com sucesso!');

                    // Se todos foram salvos, fechar o formulário do CEP
                    if (successCount === totalFields) {
                        form.classList.remove('active');
                        console.log('Todos os campos foram salvos com sucesso!');
                        showAlert('success', 'Endereço salvo', 'CEP e endereço foram atualizados com sucesso.');
                    }
                } else {
                    console.error('Erro ao atualizar ' + fieldData.campo + ': ' + (data.mensagem || 'Erro desconhecido'));
                    showAlert('error', 'Erro ao atualizar', (data.mensagem || 'Erro desconhecido') + ' (campo: ' + fieldData.campo + ')');
                }
            })
            .catch(error => {
                console.error('Erro ao salvar ' + fieldData.campo + ':', error);
                showAlert('error', 'Erro de comunicação', 'Não foi possível salvar o campo "' + fieldData.campo + '".');
            });
    });
}

// Detectar mudanças nos inputs
Document.addEventListener('DOMContentLoaded', function () {
    const editableInputs = document.querySelectorAll('.editable-input');
    editableInputs.forEach(input => {
        const initialValue = input.value;
        const form = input.closest('.editable-form');

        input.addEventListener('input', function () {
            const buttonGroup = form.querySelector('.button-group-editable');
            const existingWarning = form.querySelector('.unsaved-warning');

            if (this.value !== initialValue && !existingWarning) {
                const warning = document.createElement('span');
                warning.className = 'unsaved-warning';
                warning.textContent = '⚠ não salvo';
                buttonGroup.appendChild(warning);
            } else if (this.value === initialValue && existingWarning) {
                existingWarning.remove();
            }
        });
    });

    // CPF: apenas números e no máximo 11 dígitos
    const cpfInput = document.querySelector('.editable-item[onclick*="cpf"] .editable-input');
    if (cpfInput) {
        cpfInput.addEventListener('input', function (e) {
            let digits = e.target.value.replace(/\D/g, '');
            if (digits.length > 11) {
                digits = digits.substring(0, 11);
            }
            e.target.value = digits;
        });
    }

    // CEP: apenas números e no máximo 8 dígitos
    const cepInput = document.getElementById('cepInput');
    if (cepInput) {
        cepInput.addEventListener('input', function (e) {
            let digits = e.target.value.replace(/\D/g, '');
            if (digits.length > 8) {
                digits = digits.substring(0, 8);
            }
            e.target.value = digits;
        });
    }

    // Máscara de telefone
    const telefoneInputs = document.querySelectorAll('input[type="tel"]');
    telefoneInputs.forEach(input => {
        input.addEventListener('input', function (e) {
            let telefone = e.target.value.replace(/\D/g, '');
            if (telefone.length > 0) {
                telefone = '(' + telefone.substring(0, 2) + ') ' + telefone.substring(2, 7) + '-' + telefone.substring(7, 11);
            }
            e.target.value = telefone;
        });
    });
});

// Buscar endereço por CEP
function buscarEnderecoPorCep() {
    const cepInput = document.getElementById('cepInput');
    if (!cepInput) return;

    const cep = cepInput.value.replace(/\D/g, '');

    // Se não tiver 8 dígitos ainda, não faz nada (nem mostra erro)
    if (cep.length === 0) {
        return;
    }

    if (cep.length !== 8) {
        showAlert('error', 'CEP inválido', 'CEP deve conter 8 dígitos.');
        return;
    }

    // Formatar CEP para display (XXXXX-XXX)
    cepInput.value = cep.substring(0, 5) + '-' + cep.substring(5);

    // Tenta BrasilAPI primeiro; se falhar, cai para ViaCEP
    fetch('https://brasilapi.com.br/api/cep/v1/' + cep)
        .then(response => {
            if (!response.ok) {
                throw new Error('BrasilAPI respondeu com status ' + response.status);
            }
            return response.json();
        })
        .then(data => {
            // Campos: street, city, state
            const ruaInput = document.getElementById('ruaInput');
            if (ruaInput) {
                const ruaItem = ruaInput.closest('.editable-item');
                if (ruaItem) {
                    const ruaDisplay = ruaItem.querySelector('.editable-text-display');
                    if (ruaDisplay) ruaDisplay.textContent = data.street || '';
                }
                ruaInput.value = data.street || '';
            }

            const cidadeInput = document.getElementById('cidadeInput');
            if (cidadeInput) {
                const cidadeItem = cidadeInput.closest('.editable-item');
                if (cidadeItem) {
                    const cidadeDisplay = cidadeItem.querySelector('.editable-text-display');
                    if (cidadeDisplay) cidadeDisplay.textContent = data.city || '';
                }
                cidadeInput.value = data.city || '';
            }

            const estadoInput = document.getElementById('estadoInput');
            if (estadoInput) {
                const estadoItem = estadoInput.closest('.editable-item');
                if (estadoItem) {
                    const estadoDisplay = estadoItem.querySelector('.editable-text-display');
                    if (estadoDisplay) estadoDisplay.textContent = data.state || '';
                }
                estadoInput.value = data.state || '';
            }

            console.log('Endereço encontrado (BrasilAPI):', data);
        })
        .catch(error => {
            console.warn('Erro com BrasilAPI, tentando ViaCEP...', error);

            // Fallback: ViaCEP
            fetch('https://viacep.com.br/ws/' + cep + '/json/')
                .then(response => response.json())
                .then(data => {
                    if (data.erro) {
                        showAlert('error', 'CEP não encontrado', 'Verifique o CEP informado e tente novamente.');
                        return;
                    }

                    const ruaInput = document.getElementById('ruaInput');
                    if (ruaInput) {
                        const ruaItem = ruaInput.closest('.editable-item');
                        if (ruaItem) {
                            const ruaDisplay = ruaItem.querySelector('.editable-text-display');
                            if (ruaDisplay) ruaDisplay.textContent = data.logradouro || '';
                        }
                        ruaInput.value = data.logradouro || '';
                    }

                    const cidadeInput = document.getElementById('cidadeInput');
                    if (cidadeInput) {
                        const cidadeItem = cidadeInput.closest('.editable-item');
                        if (cidadeItem) {
                            const cidadeDisplay = cidadeItem.querySelector('.editable-text-display');
                            if (cidadeDisplay) cidadeDisplay.textContent = data.localidade || '';
                        }
                        cidadeInput.value = data.localidade || '';
                    }

                    const estadoInput = document.getElementById('estadoInput');
                    if (estadoInput) {
                        const estadoItem = estadoInput.closest('.editable-item');
                        if (estadoItem) {
                            const estadoDisplay = estadoItem.querySelector('.editable-text-display');
                            if (estadoDisplay) estadoDisplay.textContent = data.uf || '';
                        }
                        estadoInput.value = data.uf || '';
                    }

                    console.log('Endereço encontrado (ViaCEP fallback):', data);
                })
                .catch(err2 => {
                    console.error('Erro ao buscar CEP (BrasilAPI e ViaCEP falharam):', err2);
                    showAlert('error', 'Erro de comunicação', 'Não foi possível buscar o CEP.');
                });
        });
}
