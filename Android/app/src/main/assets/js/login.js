// Funzione per gestire la login
function handleLogin() {
    var username = document.getElementById('username').value;
    var password = document.getElementById('password').value;

    try {
        var result = AndroidInterface.onLogin(username, password);
        if (result === "success") {
            //showMessage('Login riuscito!');
            window.location.href = 'home.html';  // Redirigi alla home
        } else {
            showMessage('Login fallito. Controlla username e password.');
        }
    } catch (e) {
        showMessage("Errore durante la chiamata Kotlin: " + e.message);
    }
}

// Funzione per gestire la registrazione
function handleRegister() {
    var username = document.getElementById('register-username').value;
    var email = document.getElementById('email').value;
    var password = document.getElementById('register-password').value;
    var confirmPassword = document.getElementById('confirm-password').value;

    if (password !== confirmPassword) {
        showMessage('Le password non corrispondono.');
        return;
    }

    // Chiama la funzione Kotlin per gestire la registrazione tramite l'interfaccia AndroidInterface
    try {
        var result = AndroidInterface.onRegister(username, email, password);
        if (result === "success") {
            showMessage('Registrazione riuscita!');
            window.location.href = 'login.html';  // Redirigi alla pagina di login
        } else {
            showMessage('Registrazione fallita. La password deve avere almeno 6 caratteri.');
        }
    } catch (e) {
        showMessage("Errore durante la chiamata Kotlin: " + e.message);
    }
}

