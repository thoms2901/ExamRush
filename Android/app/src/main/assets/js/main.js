function navigateTo(page) {
    window.location.href = page;
}

function showMessage(message) {
    // Crea un nuovo div per il messaggio
    var messageDiv = document.createElement('div');
    messageDiv.textContent = message;
    messageDiv.style.position = 'fixed';
    messageDiv.style.top = '20px';
    messageDiv.style.left = '50%';
    messageDiv.style.transform = 'translateX(-50%)';
    messageDiv.style.backgroundColor = 'rgba(0, 0, 0, 0.7)';
    messageDiv.style.color = 'white';
    messageDiv.style.padding = '10px 20px';
    messageDiv.style.borderRadius = '5px';
    messageDiv.style.zIndex = '1000';
    messageDiv.style.fontSize = '16px';
    messageDiv.style.boxShadow = '0 4px 8px rgba(0, 0, 0, 0.5)';

    // Aggiungi il div al body del documento
    document.body.appendChild(messageDiv);

    // Rimuovi il messaggio dopo 3 secondi
    setTimeout(function() {
        messageDiv.remove();
    }, 3000);
}

