from flask import *
from pymongo import MongoClient
import os

app = Flask(__name__)

# Configurazioni MongoDB
client = MongoClient(os.getenv("MONGO_URI"))
db = client["mydatabase"]
users_collection = db["users"]
decks_collection = db["decks"]
cards_collection = db["cards"]



# Registra un nuovo utente
@app.route("/api/register", methods=["POST"])
def register():
    data = request.get_json()
    users_collection.insert_one(data)
    return jsonify({"message": "Utente registrato con successo!"})


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


# Restituisce tutti i deck disponibili

@app.route("/api/decks", methods=["GET"])
def get_decks():
    # Fetch all decks from the MongoDB collection with specific fields
    decks = list(decks_collection.find(
        {},  # Empty filter to match all documents
        {"_id": 0, "title": 1, "description": 1, "teacher_id": 1}  # Include only these fields
    ))
    return jsonify(decks)



# # Restituisce l'immagine del profilo dell'utente
# @app.route("/api/users/<user_id>/profile-image", methods=["GET"])
# def get_profile_image(user_id):
#     user = users_collection.find_one({"user_id": user_id}, {"_id": 0, "profile_image": 1})
#     return jsonify(user) if user else (jsonify({"error": "Immagine non trovata"}), 404)

# # Carica una nuova immagine del profilo per l'utente
# @app.route("/api/users/<user_id>/profile-image", methods=["POST"])
# def upload_profile_image(user_id):
#     file = request.files["image"]
#     file_key = f"{user_id}/{file.filename}"
#     s3.upload_fileobj(file, BUCKET_NAME, file_key)
#     users_collection.update_one({"user_id": user_id}, {"$set": {"profile_image": file_key}})
#     return jsonify({"message": "Immagine caricata con successo!", "file": file_key})

# # Aggiorna l'immagine del profilo dell'utente
# @app.route("/api/users/<user_id>/profile-image", methods=["PUT"])
# def update_profile_image(user_id):
#     return upload_profile_image(user_id)

# # Elimina l'immagine del profilo dell'utente
# @app.route("/api/users/<user_id>/profile-image", methods=["DELETE"])
# def delete_profile_image(user_id):
#     users_collection.update_one({"user_id": user_id}, {"$unset": {"profile_image": 1}})
#     return jsonify({"message": "Immagine eliminata con successo!"})

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)

