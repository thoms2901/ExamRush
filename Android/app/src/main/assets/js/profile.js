document.addEventListener("DOMContentLoaded", function() {
    try {
        var userInfo = JSON.parse(Android.getUserInfo());

        // Aggiorna i dati nel profilo
        document.getElementById("profile-img").src = userInfo.urltarget + "/api/users/" + userInfo.user_id + "/profile-image";
        document.getElementById("user-name").textContent = userInfo.name;
        document.getElementById("user-surname").textContent = userInfo.surname;
        document.getElementById("user-email").textContent = userInfo.email;
        document.getElementById("user-role").textContent = userInfo.role;

        // Aggiorna le statistiche
        document.getElementById("played-games").textContent = userInfo.played_games;
        document.getElementById("score").textContent = userInfo.score;
        document.getElementById("average-score").textContent = userInfo.average_score;

        // Calcola la percentuale di vittorie
        var winPercentage = (userInfo.score / (userInfo.played_games || 1));
        document.getElementById("win-percentage").textContent = winPercentage.toFixed(2) * 100 + "%";

        document.getElementById("delete-account-btn").addEventListener("click", function() {
            deleteAccount(userInfo.user_id);
        });
        document.getElementById("logout-btn").addEventListener("click", function () {
            Android.logoutUser(); // Chiamata alla funzione Kotlin
            window.location.href = "login.html"; // Reindirizza alla pagina di login
        });


        //showMessage("Dati aggiornati con successo");

    } catch (e) {
        console.error("Errore durante il recupero delle informazioni utente: " + e.message);
    }
});


function changeProfilePic() {
    Android.pickImageFromGallery(); // Call Kotlin function
}

// Callback to update profile image
function onImagePicked(imagePath) {
    document.getElementById("profile-img").src = imagePath; // Aggiorna immagine visivamente
    uploadProfileImage(); // Avvia l'upload automatico dopo la selezione
}


function deleteAccount(userId) {
    Android.onDeleteUser(userId);
    window.location.href = "login.html";
}

function uploadProfileImage() {
    const imgElement = document.getElementById("profile-img");
    if (!imgElement) {
        console.error("❌ Elemento immagine non trovato!");
        return;
    }

    // Converti l'immagine in Base64
    const canvas = document.createElement("canvas");
    const ctx = canvas.getContext("2d");
    const img = new Image();
    img.crossOrigin = "Anonymous"; // Per evitare problemi di CORS

    img.onload = function () {
        canvas.width = img.width;
        canvas.height = img.height;
        ctx.drawImage(img, 0, 0);
        const base64String = canvas.toDataURL("image/jpeg").split(",")[1]; // Prende solo i dati base64

        // Chiama il metodo Kotlin per l'upload
        Android.uploadProfileImageFromJS(base64String);
    };

    img.onerror = function () {
        console.error("❌ Errore nel caricamento dell'immagine!");
    };

    img.src = imgElement.src;
}

// Callback chiamata da Kotlin al termine dell'upload
function onUploadSuccess() {
    console.log("✅ Immagine del profilo aggiornata con successo!");
    showMessage("Immagine aggiornata con successo!");
}

function onUploadFailed(error) {
    console.error("❌ Errore durante l'upload dell'immagine:", error);
    showMessage("Errore: " + error);
}