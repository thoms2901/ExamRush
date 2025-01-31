from flask import Flask, request, jsonify
from pymongo import MongoClient
import boto3
import os

app = Flask(__name__)

# Configurazioni MongoDB
client = MongoClient(os.getenv("MONGO_URI"))
db = client["mydatabase"]
collection = db["images"]

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



@app.route("/upload", methods=["POST"])
def upload_file():
    file = request.files["image"]
    file_key = file.filename

    # Salva su MinIO
    s3.upload_fileobj(file, BUCKET_NAME, file_key)

    # Salva metadata in MongoDB
    collection.insert_one({"filename": file_key, "url": f"{BUCKET_NAME}/{file_key}"})

    return jsonify({"message": "File caricato con successo!", "file": file_key})

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)



