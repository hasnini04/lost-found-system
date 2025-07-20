<?php
header('Content-Type: application/json');

$conn = new mysqli("localhost", "root", "", "lost_and_found");

if ($conn->connect_error) {
    die(json_encode(["error" => "Database connection failed."]));
}

$sql = "SELECT 
            c.id,
            u.name AS user_name,
            u.email,
            fi.item_name,
            fi.status AS item_status,    -- NEW FIELD
            c.claim_message, 
            c.status,
            c.created_at
        FROM claim c
        JOIN user u ON c.reporter_id = u.id
        JOIN found_item fi ON c.found_item_id = fi.id
        ORDER BY c.created_at DESC";

$result = $conn->query($sql);

$claims = [];

if ($result && $result->num_rows > 0) {
    while ($row = $result->fetch_assoc()) {
        $row['claim_message'] = base64_encode($row['claim_message']);  
        $claims[] = $row;
    }
}

echo json_encode($claims);
$conn->close();
?>
