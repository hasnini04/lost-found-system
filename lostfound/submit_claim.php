<?php
header("Content-Type: application/json");

// DB connection
$host = "localhost";
$dbname = "lost_and_found";
$user = "root";
$pass = "";
$pdo = new PDO("mysql:host=$host;dbname=$dbname", $user, $pass);
$pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

// Get data
$reporter_id = $_POST['reporter_id'] ?? '';
$found_item_id = $_POST['found_item_id'] ?? '';
$claim_message = $_POST['claim_message'] ?? '';

// Validate
if (!$reporter_id || !$found_item_id || !$claim_message) {
    echo json_encode(["status" => "error", "message" => "Missing required fields."]);
    exit;
}

// Insert claim
$stmt = $pdo->prepare("INSERT INTO claim (reporter_id, found_item_id, claim_message) VALUES (?, ?, ?)");
$success = $stmt->execute([$reporter_id, $found_item_id, $claim_message]);

echo json_encode([
    "status" => $success ? "success" : "error",
    "message" => $success ? "Claim submitted." : "Failed to submit claim."
]);
?>
