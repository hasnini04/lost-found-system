<?php
header("Content-Type: application/json");

// Database connection
$host = "localhost";
$dbname = "lost_and_found";
$user = "root";
$pass = "";

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $user, $pass);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
} catch (PDOException $e) {
    echo json_encode(["status" => "error", "message" => "DB connection failed: " . $e->getMessage()]);
    exit;
}

// Validate POST data
$reporter_id = isset($_POST['reporter_id']) ? trim($_POST['reporter_id']) : '';
$item_id     = isset($_POST['item_id']) ? trim($_POST['item_id']) : '';
$message     = isset($_POST['message']) ? trim($_POST['message']) : '';

if ($reporter_id === '' || $item_id === '' || $message === '') {
    echo json_encode(["status" => "error", "message" => "Missing required parameters."]);
    exit;
}

try {
    

    $stmt = $pdo->prepare("INSERT INTO messages (reporter_id, item_id, message) VALUES (?, ?, ?)");
    $stmt->execute([$reporter_id, $item_id, $message]);

    echo json_encode(["status" => "success", "message" => "Notification sent successfully."]);
} catch (PDOException $e) {
    echo json_encode(["status" => "error", "message" => "DB error: " . $e->getMessage()]);
}
