import boto3
import os

s3 = boto3.client(
    "s3",
    endpoint_url="http://minio:9000",
    aws_access_key_id="yft76wefwfa4w6f1FI",
    aws_secret_access_key="yft76wefwfa4w6f1FI",
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
