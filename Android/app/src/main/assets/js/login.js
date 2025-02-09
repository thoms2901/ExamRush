

function onAuthResult(action, result) {
    if (action === "login") {
        if (result === "success") {
            showMessage("Login riuscito!");
            window.location.href = "home.html"
        } else {
            showMessage("Login fallito. Controlla le credenziali.");
        }
    } else if (action === "register") {
        if (result === "success") {
            showMessage("Registrazione completata!");
            window.location.href = "login.html"
        } else {
            showMessage("Registrazione fallita. Email già in uso?");
        }
    }
}

function handleLogin() {
    var email = document.getElementById("email").value;
    var password = document.getElementById("password").value;
    Android.onLogin(email, password);
}

function handleRegister() {
    var email = document.getElementById("regEmail").value;
    var password = document.getElementById("regPassword").value;
    Android.onRegister(email, password);
}


