from flask import Flask, request, jsonify
from pymongo import MongoClient
import boto3
import os

app = Flask(__name__)

# Configurazioni MongoDB
client = MongoClient(os.getenv("MONGO_URI"))
db = client["mydatabase"]
users_collection = db["users"]
decks_collection = db["decks"]
cards_collection = db["cards"]

# Configurazione MinIO (S3)
s3 = boto3.client(
    "s3",
    endpoint_url=os.getenv("MINIO_ENDPOINT"),
    aws_access_key_id=os.getenv("MINIO_ACCESS_KEY"),
    aws_secret_access_key=os.getenv("MINIO_SECRET_KEY"),
)

BUCKET_NAME = "uploads"
try:
    s3.create_bucket(Bucket=BUCKET_NAME)
except:
    pass  # Il bucket potrebbe già esistere

# Registra un nuovo utente
@app.route("/api/register", methods=["POST"])
def register():
    data = request.get_json()
    users_collection.insert_one(data)
    return jsonify({"message": "Utente registrato con successo!"})

# Restituisce la lista di tutti i deck disponibili
@app.route("/api/decks", methods=["GET"])
def get_all_decks():
    decks = list(decks_collection.find({}, {"_id": 0}))
    return jsonify(decks)

# Restituisce le informazioni di un deck specifico
@app.route("/api/decks/<deck_id>", methods=["GET"])
def get_deck_by_id(deck_id):
    deck = decks_collection.find_one({"deck_id": deck_id}, {"_id": 0})
    return jsonify(deck) if deck else (jsonify({"error": "Deck non trovato"}), 404)

# Aggiorna il numero di persone che hanno giocato un deck
@app.route("/api/decks/<deck_id>", methods=["PUT"])
def update_deck(deck_id):
    data = request.get_json()
    decks_collection.update_one({"deck_id": deck_id}, {"$inc": {"played": data["number"]}})
    return jsonify({"message": "Deck aggiornato con successo!"})

# Restituisce tutte le carte di un deck
@app.route("/api/decks/<deck_id>/cards", methods=["GET"])
def get_all_cards(deck_id):
    cards = list(cards_collection.find({"deck_id": deck_id}, {"_id": 0}))
    return jsonify(cards)

# Restituisce una carta specifica
@app.route("/api/decks/<deck_id>/cards/<card_id>", methods=["GET"])
def get_card_by_id(deck_id, card_id):
    card = cards_collection.find_one({"deck_id": deck_id, "card_id": card_id}, {"_id": 0})
    return jsonify(card) if card else (jsonify({"error": "Carta non trovata"}), 404)

# Restituisce la risposta di una carta specifica
@app.route("/api/decks/<deck_id>/cards/<card_id>/solution", methods=["GET"])
def check_card_solution(deck_id, card_id):
    card = cards_collection.find_one({"deck_id": deck_id, "card_id": card_id}, {"_id": 0, "solution": 1})
    return jsonify(card) if card else (jsonify({"error": "Soluzione non trovata"}), 404)

# Restituisce le informazioni dell'utente
@app.route("/api/users/<user_id>", methods=["GET"])
def get_user_info(user_id):
    user = users_collection.find_one({"user_id": user_id}, {"_id": 0})
    return jsonify(user) if user else (jsonify({"error": "Utente non trovato"}), 404)

# Aggiorna le informazioni dell'utente
@app.route("/api/users/<user_id>", methods=["PUT"])
def update_user_info(user_id):
    data = request.get_json()
    users_collection.update_one({"user_id": user_id}, {"$set": data})
    return jsonify({"message": "Informazioni aggiornate con successo!"})

# Restituisce le informazioni dell'utente cercandolo per email
@app.route("/api/users", methods=["POST"])
def get_user_by_email():
    data = request.json
    print(data)
    email = data.get("email")
    if not email:
        return jsonify({"error": "Email richiesta"}), 400
    user = users_collection.find_one({"email": email}, {"_id": 0})
    return jsonify(user) if user else (jsonify({"error": "Utente non trovato"}), 404)


# Restituisce l'immagine del profilo dell'utente
@app.route("/api/users/<user_id>/profile-image", methods=["GET"])
def get_profile_image(user_id):
    user = users_collection.find_one({"user_id": user_id}, {"_id": 0, "profile_image": 1})
    return jsonify(user) if user else (jsonify({"error": "Immagine non trovata"}), 404)

# Carica una nuova immagine del profilo per l'utente
@app.route("/api/users/<user_id>/profile-image", methods=["POST"])
def upload_profile_image(user_id):
    file = request.files["image"]
    file_key = f"{user_id}/{file.filename}"
    s3.upload_fileobj(file, BUCKET_NAME, file_key)
    users_collection.update_one({"user_id": user_id}, {"$set": {"profile_image": file_key}})
    return jsonify({"message": "Immagine caricata con successo!", "file": file_key})

# Aggiorna l'immagine del profilo dell'utente
@app.route("/api/users/<user_id>/profile-image", methods=["PUT"])
def update_profile_image(user_id):
    return upload_profile_image(user_id)

# Elimina l'immagine del profilo dell'utente
@app.route("/api/users/<user_id>/profile-image", methods=["DELETE"])
def delete_profile_image(user_id):
    users_collection.update_one({"user_id": user_id}, {"$unset": {"profile_image": 1}})
    return jsonify({"message": "Immagine eliminata con successo!"})

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)
