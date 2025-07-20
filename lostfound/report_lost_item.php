<?php
header('Content-Type: application/json');

// Database config
$host = "localhost";
$dbname = "lost_and_found";
$username = "root";
$password = "";

$conn = new mysqli($host, $username, $password, $dbname);
if ($conn->connect_error) {
    echo json_encode(["status" => "error", "message" => "Database connection failed"]);
    exit;
}

// Collect inputs
$reporter_id   = isset($_POST['reporter_id']) ? trim($_POST['reporter_id']) : null;
$item_name     = isset($_POST['item_name']) ? trim($_POST['item_name']) : '';
$description   = isset($_POST['description']) ? trim($_POST['description']) : '';
$location_lost = isset($_POST['location_lost']) ? trim($_POST['location_lost']) : '';
$date_lost     = isset($_POST['date_lost']) ? trim($_POST['date_lost']) : '';

if ($item_name === '' || $location_lost === '' || $date_lost === '') {
    echo json_encode(["status" => "error", "message" => "Missing required fields"]);
    exit;
}

// Handle optional image upload
$image_url = null;
if (!empty($_FILES['image']['name']) && is_uploaded_file($_FILES['image']['tmp_name'])) {
    $uploads_dir = __DIR__ . '/uploads/lost_images';
    if (!is_dir($uploads_dir)) {
        mkdir($uploads_dir, 0775, true);
    }

    $tmp_name  = $_FILES['image']['tmp_name'];
    $orig_name = basename($_FILES['image']['name']);
    $ext       = strtolower(pathinfo($orig_name, PATHINFO_EXTENSION));
    $allowed   = ['jpg','jpeg','png','gif','bmp','webp'];

    if (!in_array($ext, $allowed)) {
        echo json_encode(["status" => "error", "message" => "Unsupported image type"]);
        exit;
    }

    $new_name  = 'lost_' . time() . '_' . bin2hex(random_bytes(4)) . '.' . $ext;
    $target_fs = $uploads_dir . '/' . $new_name;
    if (!move_uploaded_file($tmp_name, $target_fs)) {
        echo json_encode(["status" => "error", "message" => "Image upload failed"]);
        exit;
    }

    // DB will store relative path like: uploads/lost_images/filename.png
    $image_url = 'uploads/lost_images/' . $new_name;
}

// Insert into DB
$sql = "INSERT INTO lost_item (reporter_id, item_name, description, location_lost, date_lost, image_url, created_at)
        VALUES (?, ?, ?, ?, ?, ?, NOW())";

$stmt = $conn->prepare($sql);
if (!$stmt) {
    echo json_encode(["status" => "error", "message" => $conn->error]);
    $conn->close();
    exit;
}

$stmt->bind_param("isssss", $reporter_id, $item_name, $description, $location_lost, $date_lost, $image_url);

if ($stmt->execute()) {
    echo json_encode([
        "status"  => "success",
        "message" => "Lost item reported successfully",
        "id"      => $stmt->insert_id,
        "image"   => $image_url
    ]);
} else {
    echo json_encode(["status" => "error", "message" => $stmt->error]);
}

$stmt->close();
$conn->close();
