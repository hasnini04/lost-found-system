<?php
header("Content-Type: application/json");

// Database connection
$conn = new mysqli("localhost", "root", "", "lost_and_found");
if ($conn->connect_error) {
    die(json_encode(["status" => "error", "message" => "Database connection failed."]));
}

// Read JSON input from client
$data = json_decode(file_get_contents("php://input"));

// Sanitize and extract input
$title = $conn->real_escape_string($data->title ?? '');
$description = $conn->real_escape_string($data->description ?? '');
$category = $conn->real_escape_string($data->category ?? '');
$location = $conn->real_escape_string($data->location ?? '');
$type = $conn->real_escape_string($data->type ?? 'Lost');
$user_id = intval($data->user_id ?? 0);
$latitude = isset($data->latitude) ? floatval($data->latitude) : null;
$longitude = isset($data->longitude) ? floatval($data->longitude) : null;
$date_reported = date("Y-m-d");

// Input validation
if (empty($title) || empty($description) || empty($category) || empty($location) || $user_id === 0) {
    echo json_encode(["status" => "error", "message" => "All fields are required."]);
    exit;
}

// Prepare and insert data
$stmt = $conn->prepare("INSERT INTO items (title, description, category, location, type, user_id, date_reported, latitude, longitude)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
$stmt->bind_param("sssssisdd", $title, $description, $category, $location, $type, $user_id, $date_reported, $latitude, $longitude);

if ($stmt->execute()) {
    echo json_encode(["status" => "success", "message" => "Item reported successfully."]);
} else {
    echo json_encode(["status" => "error", "message" => "Failed to report item."]);
}

$stmt->close();
$conn->close();
?>
