import boto3
import os

s3 = boto3.client(
    "s3",
    endpoint_url="http://minio:9000",
    aws_access_key_id="miniouifasdiuahduahudsauid",
    aws_secret_access_key="miniouifasdiuahduahudsauid",
)

BUCKET_NAME = "uploads"

# Crea il bucket se non esiste
try:
    s3.create_bucket(Bucket=BUCKET_NAME) 
except:
    pass  # Il bucket potrebbe già esistere

# Carica le immagini di default
img_folder = "/minio_data"

for filename in os.listdir(img_folder):
    file_path = os.path.join(img_folder, filename)
    if os.path.isfile(file_path):
        s3.upload_file(file_path, BUCKET_NAME, filename)
        print(f"✅ Caricata {filename} in MinIO")
