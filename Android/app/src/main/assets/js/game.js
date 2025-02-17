const maxRetries = 3;  // Numero massimo di tentativi
let attempt = 0;
let quizDataJSON;
let quizData;

while (attempt < maxRetries) {
    try {
        quizDataJSON = Android.getDeck();
        quizData = JSON.parse(quizDataJSON);
        break;  // Se il parsing ha successo, esce dal ciclo
    } catch (error) {
        attempt++;
        showMessage(`Errore nel parsing dei dati del quiz (Tentativo ${attempt}/${maxRetries}): ${error.message}`);

        if (attempt >= maxRetries) {
            throw error;  // Lancia l'errore solo dopo aver esaurito i tentativi
        }
    }
}


const totalTurns = Math.floor(quizData.cards.length);
let currentTurn = 0;
let score = 0;
let timer;
let timeLeft = 30;

const startButton = document.getElementById('start-turn');
const quizContainer = document.getElementById('quiz-container');
const questionText = document.getElementById('question-text');
const answersContainer = document.getElementById('answers-container');
const timerDisplay = document.getElementById('timer');
const scoreDisplay = document.getElementById('score');
const shakeMessage = document.getElementById('shake-message');
const feedbackMessage = document.getElementById('feedback-message');


function startTurn() {
    try {
        if (currentTurn >= totalTurns) {
            showMessage(`Quiz Ended! Final Score: ${score}`);
            try{
                showSummary();
           } catch (error) {
                showMessage(`Errore nello showSummary: ${error.message}`);
           }
            return;
        }

        currentTurn++;
        startButton.style.display = 'none';
        shakeMessage.style.display = 'none';
        quizContainer.style.display = 'block';
        loadQuestion();
    } catch (error) {
        showMessage(`Errore nello startTurn: ${error.message}`);
    }
}

function showSummary() {
    let percentage = (score / totalTurns) * 100;
    let grade = Math.round((score / totalTurns) * 30);
    let resultMessage = percentage >= 60 ? "Hai vinto!" : "Hai perso.";

    quizContainer.innerHTML = `
        <h2>Risultati</h2>
        <p>Risposte corrette: ${score} / ${totalTurns}</p>
        <p>Percentuale: ${percentage.toFixed(2)}%</p>
        <p>Voto: ${grade}/30</p>
        <h3>${resultMessage}</h3>
        <button onclick="goToDecks()">Torna ai Deck</button>
        <button onclick="goToProfile()">Profilo</button>
    `;
    startButton.style.display = 'none';
    shakeMessage.style.display = 'none';
    quizContainer.style.display = 'block';

    // invia update a server
    Android.updateUserStats(grade);
    Android.refreshUserInfo();
}


function goToDecks() {
    try {
        window.location.href = "deck.html";
    } catch (error) {
        showMessage(`Errore nel reindirizzamento ai deck: ${error.message}`);
    }
}

function goToProfile() {
    try {
        window.location.href = "profile.html";
    } catch (error) {
        showMessage(`Errore nel reindirizzamento al profilo: ${error.message}`);
    }
}

function loadQuestion() {
    try {
        const randomIndex = Math.floor(Math.random() * quizData.cards.length);
        const questionData = quizData.cards[randomIndex];

        document.getElementById('question-counter').textContent = `Question ${currentTurn}/${totalTurns}`;
        questionText.textContent = questionData.question;
        answersContainer.innerHTML = '';

        questionData.answers.forEach(answer => {
            const button = document.createElement('button');
            button.textContent = answer;
            button.onclick = () => checkAnswer(answer, questionData.correct_answer);
            answersContainer.appendChild(button);
        });

        startTimer();
    } catch (error) {
        showMessage(`Errore nel caricamento della domanda: ${error.message}`);
    }
}

function startTimer() {
    try {
        timeLeft = 30;
        timerDisplay.textContent = `Time Left: ${timeLeft}s`;
        clearInterval(timer);
        timer = setInterval(() => {
            timeLeft--;
            timerDisplay.textContent = `Time Left: ${timeLeft}s`;

            if (timeLeft === 0) {
                clearInterval(timer);
                showFeedback(false);
            }
        }, 1000);
    } catch (error) {
        showMessage(`Errore nel timer: ${error.message}`);
    }
}

function checkAnswer(selected, correct) {
    try {
        clearInterval(timer);
        let isCorrect = selected === correct;
        if (isCorrect) {
            score++;
            scoreDisplay.textContent = `Score: ${score}`;
        }

        showFeedback(isCorrect);
    } catch (error) {
        showMessage(`Errore nella verifica della risposta: ${error.message}`);
    }
}

function showFeedback(isCorrect) {
    try {
        feedbackMessage.textContent = isCorrect ? "Correct Answer! +1 punto" : "Wrong Answer!";
        feedbackMessage.style.color = isCorrect ? "green" : "red";
        feedbackMessage.style.display = "block";

        setTimeout(() => {
            feedbackMessage.style.display = "none";
            nextTurn();
        }, 2000);
    } catch (error) {
        showMessage(`Errore nella visualizzazione del feedback: ${error.message}`);
    }
}

function nextTurn() {
    if (currentTurn == totalTurns) {
        showMessage(`Quiz Ended! Final Score: ${score}`);
        try{
            showSummary();
       } catch (error) {
            showMessage(`Errore nello showSummary: ${error.message}`);
       }
        return;
    }

    try {
        quizContainer.style.display = 'none';
        startButton.style.display = 'inline';
        shakeMessage.style.display = 'block';
    } catch (error) {
        showMessage(`Errore nel passaggio al turno successivo: ${error.message}`);
    }
}

startButton.addEventListener('click', startTurn);

// Accelerometro
let lastX = 0, lastY = 0, lastZ = 0;
let shakeThreshold = 30;
let lastShakeTime = 0;

window.addEventListener("devicemotion", function(event) {
    try {
        let acceleration = event.accelerationIncludingGravity;
        if (!acceleration) return;

        let deltaX = Math.abs(acceleration.x - lastX);
        let deltaY = Math.abs(acceleration.y - lastY);
        let deltaZ = Math.abs(acceleration.z - lastZ);

        if ((deltaX + deltaY + deltaZ) > shakeThreshold) {
            let now = Date.now();
            if (now - lastShakeTime > 1000) {
                lastShakeTime = now;
                startTurn();
            }
        }

        lastX = acceleration.x;
        lastY = acceleration.y;
        lastZ = acceleration.z;
    } catch (error) {
        showMessage(`Errore nell'uso dell'accelerometro: ${error.message}`);
    }
}, true);
