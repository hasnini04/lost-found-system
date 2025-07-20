<?php
header('Content-Type: application/json');

// DB connection
$host = "localhost";
$user = "root";
$password = "";
$database = "lost_and_found";

$conn = new mysqli($host, $user, $password, $database);

if ($conn->connect_error) {
    echo json_encode(["status" => "error", "message" => "Database connection failed."]);
    exit;
}

// Get POST data
$reporter_id = isset($_POST['reporter_id']) ? intval($_POST['reporter_id']) : 0;
$found_item_id = isset($_POST['found_item_id']) ? intval($_POST['found_item_id']) : 0;
$item_name = isset($_POST['item_name']) ? $conn->real_escape_string($_POST['item_name']) : '';
$message = isset($_POST['message']) ? $conn->real_escape_string($_POST['message']) : '';
$claim_message = isset($_POST['claim_message']) ? $conn->real_escape_string($_POST['claim_message']) : '';

// Validate required fields
if ($reporter_id === 0 || $found_item_id === 0 || empty($item_name) || empty($claim_message)) {
    echo json_encode(["status" => "error", "message" => "Missing required data."]);
    exit;
}

// Insert into claim table
$sql = "INSERT INTO claim (reporter_id, found_item_id, item_name, message, claim_message, status, created_at) 
        VALUES (?, ?, ?, ?, ?, 'pending', NOW())";

$stmt = $conn->prepare($sql);
$stmt->bind_param("iisss", $reporter_id, $found_item_id, $item_name, $message, $claim_message);

if ($stmt->execute()) {
    echo json_encode(["status" => "success", "message" => "Claim submitted successfully."]);
} else {
    echo json_encode(["status" => "error", "message" => "Insert failed: " . $conn->error]);
}

$stmt->close();
$conn->close();
?>
