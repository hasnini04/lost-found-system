<?php
header("Content-Type: application/json");

$host = "localhost";
$user = "root";
$password = "";
$database = "lost_and_found";

$conn = new mysqli($host, $user, $password, $database);
if ($conn->connect_error) {
    echo json_encode(["status" => "error", "message" => "Connection failed"]);
    exit;
}

$claimId = $_POST['claim_id'] ?? '';
$status = $_POST['status'] ?? '';
$pickupNote = $_POST['pickup_note'] ?? '';

if (empty($claimId) || empty($status)) {
    echo json_encode(["status" => "error", "message" => "Missing input"]);
    exit;
}

$sql = "UPDATE claim SET status=?, pickup_note=? WHERE id=?";
$stmt = $conn->prepare($sql);
$stmt->bind_param("ssi", $status, $pickupNote, $claimId);

if ($stmt->execute()) {
    echo json_encode(["status" => "success", "message" => "Claim updated"]);
} else {
    echo json_encode(["status" => "error", "message" => $stmt->error]);
}

$stmt->close();
$conn->close();
?>
