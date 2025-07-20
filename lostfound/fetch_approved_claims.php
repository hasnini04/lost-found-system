<?php
header('Content-Type: application/json');

// Database connection
$host = 'localhost';
$user = 'root';
$pass = '';
$db   = 'lost_and_found';

$conn = new mysqli($host, $user, $pass, $db);
if ($conn->connect_error) {
    echo json_encode(["error" => "Database connection failed"]);
    exit();
}

// Fetch approved claims
$sql = "SELECT found_item_id FROM claim WHERE status = 'approved'";
$result = $conn->query($sql);

$approved = [];
if ($result && $result->num_rows > 0) {
    while ($row = $result->fetch_assoc()) {
        $approved[] = $row;
    }
}

echo json_encode($approved);
$conn->close();
?>
