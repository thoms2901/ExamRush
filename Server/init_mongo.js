db = db.getSiblingDB("mydatabase");

db.images.insertMany([
  { filename: "image1.jpg", url: "uploads/image1.jpg" },
  { filename: "image2.jpg", url: "uploads/image2.jpg" },
  { filename: "image3.jpg", url: "uploads/image3.jpg" },
  { filename: "image4.jpg", url: "uploads/image4.jpg" },
  { filename: "image5.jpg", url: "uploads/image5.jpg" },
  { filename: "image6.jpg", url: "uploads/image6.jpg" },
  { filename: "image7.jpg", url: "uploads/image7.jpg" }
]);

print("✅ Database inizializzato con immagini!");
