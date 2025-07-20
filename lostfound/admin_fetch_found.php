<?php
header('Content-Type: application/json');

// DB config
$host = 'localhost';
$db = 'lost_and_found';
$user = 'root';
$pass = '';

// Connect DB
$conn = new mysqli($host, $user, $pass, $db);
if ($conn->connect_error) {
    echo json_encode(["status" => "error", "message" => "DB connection failed"]);
    exit;
}

// Fetch all found items
$query = "SELECT * FROM found_item ORDER BY created_at DESC";
$result = $conn->query($query);

$items = [];
while ($row = $result->fetch_assoc()) {
    $items[] = $row;
}

echo json_encode([
    "status" => "success",
    "data" => $items
]);
?>
