<?php
header('Content-Type: application/json');

// Database connection
$host = "localhost";
$user = "root";
$password = "";
$database = "lost_and_found";

$conn = new mysqli($host, $user, $password, $database);

if ($conn->connect_error) {
    echo json_encode(["status" => "error", "message" => "Database connection failed."]);
    exit;
}

// Get reporter_id from GET
$reporter_id = isset($_GET['reporter_id']) ? intval($_GET['reporter_id']) : 0;

if ($reporter_id === 0) {
    echo json_encode(["status" => "error", "message" => "Missing reporter_id."]);
    exit;
}

// Fetch claim requests
$sql = "SELECT c.id AS claim_id, c.item_name, c.message, c.status, c.created_at,
               f.location_found, f.date_found
        FROM claim c
        LEFT JOIN found_items f ON c.found_item_id = f.id
        WHERE c.reporter_id = ?
        ORDER BY c.created_at DESC";

$stmt = $conn->prepare($sql);
$stmt->bind_param("i", $reporter_id);
$stmt->execute();

$result = $stmt->get_result();

$claims = [];

while ($row = $result->fetch_assoc()) {
    $claims[] = $row;
}

echo json_encode([
    "status" => "success",
    "claims" => $claims
]);

$stmt->close();
$conn->close();
?>
