from flask import *
import base64
from pymongo import MongoClient
import boto3
import logging
import os
from botocore.exceptions import ClientError
from io import BytesIO


app = Flask(__name__)
app.logger.setLevel(logging.DEBUG)

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


@app.route("/api/register", methods=["POST"])
def register():
    data = request.get_json()

    last_user = users_collection.find_one(sort=[("user_id", -1)])
    new_user_id = (last_user["user_id"] + 1) if last_user else 1

    new_user = {
        "user_id": new_user_id,
        "email": data.get("email", ""),
        "name": data.get("firstName", ""),
        "surname": data.get("lastName", ""),
        "score": 0,
        "role": "student",
        "average_score": 0,
        "played_games": 0,
        "image": ""
    }

    users_collection.insert_one(new_user)

    return jsonify({"message": "Utente registrato con successo!", "user_id": new_user_id})


# Restituisce le informazioni dell'utente
@app.route("/api/users/<user_id>", methods=["GET"])
def get_user_info(user_id):
    user = users_collection.find_one({"user_id": int(user_id)}, {"_id": 0})
    return jsonify(user) if user else (jsonify({"error": "Utente non trovato"}), 404)

@app.route("/api/users/<user_id>", methods=["DELETE"])
def delete_user(user_id):
    logging.debug(f"Richiesta DELETE ricevuta per user_id: {user_id}")

    try:
        user_id = int(user_id)  # Assicura che sia un intero
    except ValueError:
        logging.error(f"Errore: user_id '{user_id}' non è un numero valido.")
        return jsonify({"error": "ID utente non valido"}), 400

    result = users_collection.delete_one({"user_id": user_id})

    if result.deleted_count == 1:
        logging.info(f"Utente con ID {user_id} eliminato con successo.")
        return jsonify({"message": "Utente eliminato con successo!"}), 200
    else:
        logging.warning(f"Utente con ID {user_id} non trovato.")
        return jsonify({"error": "Utente non trovato"}), 404

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

# Restituituisci un deck dal titolo:
@app.route("/api/decks/<deck_id>", methods=["GET"])
def get_deck_by_title(deck_id):
    decoded_bytes = base64.b64decode(deck_id)
    title = decoded_bytes.decode('utf-8')
    deck = decks_collection.find_one({"title": title}, {"_id": 0})
    return jsonify(deck) if deck else (jsonify({"error": "Deck non trovato"}), 404)



@app.route("/api/users/<user_id>/profile-image", methods=["GET"])
def get_profile_image(user_id):
    app.logger.info(f"🔍 [DEBUG] Richiesta immagine per user_id: {user_id}")

    user = users_collection.find_one({"user_id": int(user_id)}, {"_id": 0, "image": 1})
    app.logger.info(f"📄 [DEBUG] Utente trovato nel DB: {user}")

    if user and "image" in user:
        image_entry = db.images.find_one({"filename": user["image"]}, {"_id": 0, "url": 1})
        file_key = image_entry["url"].replace("uploads/", "") if image_entry else "default-profile.jpg"
    else:
        file_key = "default-profile.jpg"

    app.logger.info(f"🖼️ [DEBUG] File richiesto: {file_key}")

    # Lista i file nel bucket per verificare se esiste
    try:
        app.logger.info("[DEBUG] 🔎 File presenti nel bucket:")
        for obj in s3.list_objects_v2(Bucket=BUCKET_NAME, Prefix="").get("Contents", []):
            app.logger.info(f" - {obj['Key']}")
    except ClientError as e:
        app.logger.error(f"⚠️ [DEBUG] Errore nel listare i file: {e}")

    try:
        # Scarica l'immagine in memoria
        image_data = BytesIO()
        s3.download_fileobj(BUCKET_NAME, file_key, image_data)
        image_data.seek(0)

        content_type = "image/jpeg" if file_key.endswith(".jpg") or file_key.endswith(".jpeg") else "image/png"
        app.logger.info(f"✅ [DEBUG] Immagine scaricata correttamente: {file_key}")

        return Response(image_data.read(), content_type=content_type)

    except ClientError as e:
        app.logger.error(f"❌ [DEBUG] Errore nel recuperare l'immagine: {e}")
        return Response(f"Errore nel recuperare l'immagine: {e}", status=500)


@app.route("/api/users/<user_id>/profile-image", methods=["POST"])
def upload_profile_image(user_id):
    try:
        data = request.get_json()

        image_base64 = data.get("image")

        if not image_base64:
            return jsonify({"error": "Immagine non fornita"}), 400

        # Controlla se l'immagine ha un prefisso MIME (es. "data:image/png;base64,")
        if "," in image_base64:
            parts = image_base64.split(",", 1)
            mime_header = parts[0]
            image_base64 = parts[1]

            if "image/png" in mime_header:
                content_type = "image/png"
                extension = "png"
            elif "image/jpeg" in mime_header or "image/jpg" in mime_header:
                content_type = "image/jpeg"
                extension = "jpg"
            else:
                content_type = "image/jpeg"  # Default
                extension = "jpg"
        else:
            content_type = "image/jpeg"  # Default
            extension = "jpg"

        try:
            image_data = base64.b64decode(image_base64)
        except Exception as e:
            return jsonify({"error": f"Errore nella decodifica Base64: {e}"}), 400

        image_filename = f"profile_{user_id}.{extension}"
        image_path = f"{image_filename}"

        # Carica l'immagine su MinIO
        s3.put_object(
            Bucket=BUCKET_NAME,
            Key=image_path,
            Body=BytesIO(image_data),
            ContentType=content_type,
        )

        # Aggiorna il percorso dell'immagine nel database MongoDB
        users_collection.update_one(
            {"user_id": int(user_id)},  
            {"$set": {"image": image_filename}},
            upsert=True
        )
        db.images.update_one(
            {"filename": image_filename},
            {"$set": {"url": f"uploads/{image_path}"}},
            upsert=True
        )

        app.logger.info(f"✅ Immagine caricata su MinIO: {image_path}")

        return jsonify({"success": True, "message": "Immagine salvata", "image_url": image_path}), 200

    except Exception as e:
        app.logger.error(f"❌ Errore nell'upload dell'immagine: {e}")
        return jsonify({"error": str(e)}), 500


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

